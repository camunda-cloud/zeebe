/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.job;

import io.camunda.zeebe.engine.metrics.JobMetrics;
import io.camunda.zeebe.engine.processing.common.EventHandle;
import io.camunda.zeebe.engine.processing.identity.AuthorizationCheckBehavior;
import io.camunda.zeebe.engine.processing.streamprocessor.CommandProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedCommandWriter;
import io.camunda.zeebe.engine.state.immutable.ElementInstanceState;
import io.camunda.zeebe.engine.state.immutable.JobState;
import io.camunda.zeebe.engine.state.immutable.ProcessingState;
import io.camunda.zeebe.engine.state.immutable.UserTaskState;
import io.camunda.zeebe.engine.state.instance.ElementInstance;
import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.UserTaskIntent;
import io.camunda.zeebe.stream.api.records.TypedRecord;

public final class JobCompleteProcessor implements CommandProcessor<JobRecord> {

  private final JobState jobState;
  private final UserTaskState userTaskState;
  private final ElementInstanceState elementInstanceState;
  private final DefaultJobCommandPreconditionGuard defaultProcessor;
  private final JobMetrics jobMetrics;
  private final EventHandle eventHandle;

  public JobCompleteProcessor(
      final ProcessingState state,
      final JobMetrics jobMetrics,
      final EventHandle eventHandle,
      final AuthorizationCheckBehavior authCheckBehavior) {
    jobState = state.getJobState();
    userTaskState = state.getUserTaskState();
    elementInstanceState = state.getElementInstanceState();
    defaultProcessor =
        new DefaultJobCommandPreconditionGuard(
            "complete", jobState, this::acceptCommand, authCheckBehavior);
    this.jobMetrics = jobMetrics;
    this.eventHandle = eventHandle;
  }

  @Override
  public boolean onCommand(
      final TypedRecord<JobRecord> command, final CommandControl<JobRecord> commandControl) {
    return defaultProcessor.onCommand(command, commandControl);
  }

  @Override
  public void afterAccept(
      final TypedCommandWriter commandWriter,
      final StateWriter stateWriter,
      final long key,
      final Intent intent,
      final JobRecord value) {

    final var elementInstanceKey = value.getElementInstanceKey();

    final var elementInstance = elementInstanceState.getInstance(elementInstanceKey);

    if (elementInstance == null) {
      return;
    }

    switch (value.getJobKind()) {
      case EXECUTION_LISTENER:
        {
          // to store the variable for merge, to handle concurrent commands
          eventHandle.triggeringProcessEvent(value);

          commandWriter.appendFollowUpCommand(
              elementInstanceKey,
              ProcessInstanceIntent.COMPLETE_EXECUTION_LISTENER,
              elementInstance.getValue());
          return;
        }
      case TASK_LISTENER:
        {
          // to store the variable for merge, to handle concurrent commands
          eventHandle.triggeringProcessEvent(value);

          /*
           We retrieve the intermediate user task state rather than the regular user task record
           because the intermediate state captures the exact data provided during the original
           user task command (e.g., COMPLETE, ASSIGN). This data includes variables, actions,
           and other command-related details that may not yet be reflected in the persisted user
           task record, that can be accessed via `userTaskState.getUserTask`.

           When task listeners are involved, it's essential to preserve this original state
           until all task listeners have been executed. Retrieving the intermediate state here
           ensures that the finalization of the user task command uses the correct, unmodified
           data as originally intended by the user. Once all task listeners have been processed
           and the original user task command is finalized, the intermediate state is cleared.
          */
          final var userTask =
              userTaskState.getIntermediateState(elementInstance.getUserTaskKey()).getRecord();
          commandWriter.appendFollowUpCommand(
              userTask.getUserTaskKey(), UserTaskIntent.COMPLETE_TASK_LISTENER, userTask);
          return;
        }
      default:
        {
          final long scopeKey = elementInstance.getValue().getFlowScopeKey();
          final ElementInstance scopeInstance = elementInstanceState.getInstance(scopeKey);

          if (scopeInstance != null && scopeInstance.isActive()) {
            eventHandle.triggeringProcessEvent(value);
            commandWriter.appendFollowUpCommand(
                elementInstanceKey,
                ProcessInstanceIntent.COMPLETE_ELEMENT,
                elementInstance.getValue());
          }
        }
    }
  }

  private void acceptCommand(
      final TypedRecord<JobRecord> command,
      final CommandControl<JobRecord> commandControl,
      final JobRecord job) {
    job.setVariables(command.getValue().getVariablesBuffer());

    commandControl.accept(JobIntent.COMPLETED, job);
    jobMetrics.jobCompleted(job.getType(), job.getJobKind());
  }
}
