/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.job;

import io.camunda.zeebe.engine.processing.job.behaviour.JobUpdateBehaviour;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecordProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedRejectionWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedResponseWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.stream.api.records.TypedRecord;

public class JobUpdateTimeoutProcessor implements TypedRecordProcessor<JobRecord> {

  private final JobUpdateBehaviour jobUpdateBehaviour;
  private final TypedRejectionWriter rejectionWriter;
  private final TypedResponseWriter responseWriter;

  public JobUpdateTimeoutProcessor(
      final JobUpdateBehaviour jobUpdateBehaviour, final Writers writers) {
    this.jobUpdateBehaviour = jobUpdateBehaviour;
    rejectionWriter = writers.rejection();
    responseWriter = writers.response();
  }

  @Override
  public void processRecord(final TypedRecord<JobRecord> command) {
    final long jobKey = command.getKey();
    jobUpdateBehaviour
        .getJob(jobKey, command)
        .ifRightOrLeft(
            job ->
                jobUpdateBehaviour
                    .updateJobTimeout(jobKey, job, command)
                    .ifPresentOrElse(
                        errorMessage -> {
                          rejectionWriter.appendRejection(
                              command, RejectionType.INVALID_STATE, errorMessage);
                          responseWriter.writeRejectionOnCommand(
                              command, RejectionType.INVALID_STATE, errorMessage);
                        },
                        () ->
                            responseWriter.writeEventOnCommand(
                                jobKey, JobIntent.TIMEOUT_UPDATED, job, command)),
            errorMessage -> {
              responseWriter.writeRejectionOnCommand(
                  command, RejectionType.NOT_FOUND, errorMessage);
            });
  }
}
