/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.tasks.batchoperations;

import io.camunda.exporter.tasks.BackgroundTask;
import io.camunda.exporter.tasks.batchoperations.BatchOperationUpdateRepository.DocumentUpdate;
import io.camunda.webapps.schema.descriptors.operate.template.BatchOperationTemplate;
import io.camunda.webapps.schema.entities.operation.BatchOperationEntity;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public class BatchOperationUpdateTask implements BackgroundTask {

  private static final int NO_UPDATES = 0;
  private final BatchOperationUpdateRepository batchOperationUpdateRepository;

  private final Logger logger;

  public BatchOperationUpdateTask(
      final BatchOperationUpdateRepository batchOperationUpdateRepository, final Logger logger) {
    this.batchOperationUpdateRepository = batchOperationUpdateRepository;
    this.logger = logger;
  }

  @Override
  public CompletionStage<Integer> execute() {

    final var batchOperations = batchOperationUpdateRepository.getNotFinishedBatchOperations();

    if (batchOperations.size() > 0) {

      final var finishedSingleOperationsCount =
          batchOperationUpdateRepository.getFinishedOperationsCount(
              batchOperations.stream()
                  .map(BatchOperationEntity::getId)
                  .collect(Collectors.toList()));

      final var documentUpdates =
          finishedSingleOperationsCount.stream()
              .map(
                  d ->
                      new DocumentUpdate(
                          d.batchOperationId(),
                          Map.of(
                              BatchOperationTemplate.COMPLETED_OPERATIONS_COUNT,
                              d.finishedOperationsCount())))
              .toList();

      if (documentUpdates.size() > 0) {
        final Integer updatesCount = batchOperationUpdateRepository.bulkUpdate(documentUpdates);
        logger.trace(
            "Updated {} batch operations with the following completedOperationsCount {}",
            updatesCount,
            documentUpdates);
        return CompletableFuture.completedFuture(updatesCount);
      } // else return 0
    }

    return CompletableFuture.completedFuture(NO_UPDATES);
  }
}
