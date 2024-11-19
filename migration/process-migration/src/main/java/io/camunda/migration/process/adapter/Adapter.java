/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migration.process.adapter;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import io.camunda.exporter.utils.RetryOperation;
import io.camunda.exporter.utils.RetryOperation.RetryPredicate;
import io.camunda.migration.api.MigrationException;
import io.camunda.migration.process.ProcessorStep;
import io.camunda.webapps.schema.descriptors.operate.index.ProcessIndex;
import io.camunda.webapps.schema.entities.operate.ImportPositionEntity;
import io.camunda.webapps.schema.entities.operate.ProcessEntity;
import io.camunda.zeebe.util.ExponentialBackoffRetryDelay;
import io.camunda.zeebe.util.VersionUtil;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface Adapter {

  String PROCESSOR_STEP_ID = VersionUtil.getVersion() + "-1";
  String PROCESSOR_STEP_TYPE = "processorStep";
  String PROCESS_DEFINITION_KEY = "key";
  String STEP_DESCRIPTION = "Process Migration last migrated document";

  String migrate(List<ProcessEntity> records) throws MigrationException;

  List<ProcessEntity> nextBatch(final String processDefinitionKey) throws MigrationException;

  String readLastMigratedEntity() throws MigrationException;

  void writeLastMigratedEntity(final String processDefinitionKey) throws MigrationException;

  Set<ImportPositionEntity> readImportPosition() throws MigrationException;

  void close() throws IOException;

  int getMaxRetries();

  Duration getMinDelay();

  Duration getMaxDelay();

  default Map<String, Object> getUpdateMap(final ProcessEntity entity) {
    final Map<String, Object> updateMap = new HashMap<>();
    updateMap.put(ProcessIndex.IS_PUBLIC, entity.getIsPublic());
    updateMap.put(ProcessIndex.IS_FORM_EMBEDDED, entity.getIsFormEmbedded());

    if (entity.getFormId() != null) {
      updateMap.put(ProcessIndex.FORM_ID, entity.getFormId());
    }
    if (entity.getFormKey() != null) {
      updateMap.put(ProcessIndex.FORM_KEY, entity.getFormKey());
    }
    return updateMap;
  }

  default ProcessorStep upsertProcessorStep(final String processDefinitionKey) {
    final ProcessorStep step = new ProcessorStep();
    step.setContent(processDefinitionKey);
    step.setApplied(true);
    step.setIndexName(ProcessIndex.INDEX_NAME);
    step.setDescription(STEP_DESCRIPTION);
    step.setVersion(VersionUtil.getVersion());
    step.setAppliedDate(OffsetDateTime.now(ZoneId.systemDefault()));
    return step;
  }

  default <T> T doWithRetry(
      final String message, final Callable<T> callable, final RetryPredicate<T> retryPredicate)
      throws Exception {
    final ExponentialBackoffRetryDelay retryDelay =
        new ExponentialBackoffRetryDelay(getMaxDelay(), getMinDelay());
    return RetryOperation.<T>newBuilder()
        .noOfRetry(getMaxRetries())
        .delayInterval(Math.toIntExact(getMinDelay().getSeconds()), TimeUnit.SECONDS)
        .delaySupplier(() -> Math.toIntExact(retryDelay.nextDelay().getSeconds()))
        .retryOn(IOException.class, ElasticsearchException.class)
        .retryPredicate(retryPredicate)
        .retryConsumer(callable::call)
        .message(message)
        .build()
        .retry();
  }
}
