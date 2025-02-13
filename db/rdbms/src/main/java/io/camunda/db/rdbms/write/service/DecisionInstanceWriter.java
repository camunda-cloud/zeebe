/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.service;

import io.camunda.db.rdbms.sql.DecisionInstanceMapper;
import io.camunda.db.rdbms.write.domain.DecisionInstanceDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel.ProcessInstanceDbModelBuilder;
import io.camunda.db.rdbms.write.queue.ContextType;
import io.camunda.db.rdbms.write.queue.ExecutionQueue;
import io.camunda.db.rdbms.write.queue.QueueItem;
import io.camunda.db.rdbms.write.queue.UpsertMerger;
import java.time.OffsetDateTime;
import java.util.function.Function;

public class DecisionInstanceWriter {

  private final ExecutionQueue executionQueue;

  public DecisionInstanceWriter(final ExecutionQueue executionQueue) {
    this.executionQueue = executionQueue;
  }

  public void create(final DecisionInstanceDbModel decisionInstance) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.DECISION_INSTANCE,
            decisionInstance.decisionInstanceKey(),
            "io.camunda.db.rdbms.sql.DecisionInstanceMapper.insert",
            decisionInstance));
    if (decisionInstance.evaluatedInputs() != null
        && !decisionInstance.evaluatedInputs().isEmpty()) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.DECISION_INSTANCE,
              decisionInstance.decisionInstanceKey(),
              "io.camunda.db.rdbms.sql.DecisionInstanceMapper.insertInput",
              decisionInstance));
    }
    if (decisionInstance.evaluatedOutputs() != null
        && !decisionInstance.evaluatedOutputs().isEmpty()) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.DECISION_INSTANCE,
              decisionInstance.decisionInstanceKey(),
              "io.camunda.db.rdbms.sql.DecisionInstanceMapper.insertOutput",
              decisionInstance));
    }
  }

  public void scheduleForHistoryCleanup(
      final Long decisionInstanceKey, final OffsetDateTime historyCleanupDate) {
    final boolean wasMerged =
        mergeToQueue(decisionInstanceKey, b -> b.historyCleanupDate(historyCleanupDate));

    if (!wasMerged) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.DECISION_INSTANCE,
              decisionInstanceKey,
              "io.camunda.db.rdbms.sql.DecisionInstanceMapper.updateHistoryCleanupDate",
              new DecisionInstanceMapper.UpdateHistoryCleanupDateDto.Builder()
                  .decisionInstanceKey(decisionInstanceKey)
                  .historyCleanupDate(historyCleanupDate)
                  .build()));
    }
  }

  private boolean mergeToQueue(
      final long key,
      final Function<ProcessInstanceDbModelBuilder, ProcessInstanceDbModelBuilder> mergeFunction) {
    return executionQueue.tryMergeWithExistingQueueItem(
        new UpsertMerger<>(
            ContextType.PROCESS_INSTANCE, key, ProcessInstanceDbModel.class, mergeFunction));
  }
}
