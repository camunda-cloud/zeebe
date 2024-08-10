/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.property;

import io.camunda.tasklist.exceptions.TasklistRuntimeException;
import org.elasticsearch.index.reindex.AbstractBulkByScrollRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ConfigurationProperties(TasklistProperties.PREFIX + ".migration")
public class MigrationProperties {

  private static final int DEFAULT_REINDEX_BATCH_SIZE = 5_000;
  private static final int DEFAULT_THREADS_COUNT = 5;

  private boolean migrationEnabled = true;
  private boolean deleteSrcSchema = true;
  private int threadsCount = DEFAULT_THREADS_COUNT;

  // Depends of the size of documents
  //   big documents => batch size lower
  private int reindexBatchSize = DEFAULT_REINDEX_BATCH_SIZE;
  // AUTO=0 means 1 slice per shard
  private int slices = AbstractBulkByScrollRequest.AUTO_SLICES;

  public boolean isMigrationEnabled() {
    return migrationEnabled;
  }

  public MigrationProperties setMigrationEnabled(boolean migrationEnabled) {
    this.migrationEnabled = migrationEnabled;
    return this;
  }

  public MigrationProperties setDeleteSrcSchema(boolean deleteSrcSchema) {
    this.deleteSrcSchema = deleteSrcSchema;
    return this;
  }

  public boolean isDeleteSrcSchema() {
    return deleteSrcSchema;
  }

  public int getReindexBatchSize() {
    return reindexBatchSize;
  }

  public MigrationProperties setReindexBatchSize(int reindexBatchSize) {
    if (reindexBatchSize < 1 || reindexBatchSize > 10_000) {
      throw new TasklistRuntimeException(
          String.format(
              "Reindex batch size must be between 1 and 10000. Given was %d", reindexBatchSize));
    }
    this.reindexBatchSize = reindexBatchSize;
    return this;
  }

  public int getSlices() {
    return slices;
  }

  public MigrationProperties setSlices(int slices) {
    if (slices < 0) {
      throw new TasklistRuntimeException(
          String.format("Slices must be positive. Given was %d", slices));
    }
    this.slices = slices;
    return this;
  }

  public int getThreadsCount() {
    return threadsCount;
  }

  public void setThreadsCount(int threadsCount) {
    this.threadsCount = threadsCount;
  }
}
