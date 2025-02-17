/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.service;

import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskMigrationDbModel;
import io.camunda.db.rdbms.write.queue.ContextType;
import io.camunda.db.rdbms.write.queue.ExecutionQueue;
import io.camunda.db.rdbms.write.queue.QueueItem;
import io.camunda.db.rdbms.write.queue.UpsertMerger;
import java.time.OffsetDateTime;
import java.util.function.Function;

public class UserTaskWriter {

  private final ExecutionQueue executionQueue;

  public UserTaskWriter(final ExecutionQueue executionQueue) {
    this.executionQueue = executionQueue;
  }

  public void create(final UserTaskDbModel userTaskDbModel) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.USER_TASK,
            userTaskDbModel.userTaskKey(),
            "io.camunda.db.rdbms.sql.UserTaskMapper.insert",
            userTaskDbModel));
    if (userTaskDbModel.candidateUsers() != null && !userTaskDbModel.candidateUsers().isEmpty()) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.USER_TASK,
              userTaskDbModel.userTaskKey(),
              "io.camunda.db.rdbms.sql.UserTaskMapper.insertCandidateUsers",
              userTaskDbModel));
    }
    if (userTaskDbModel.candidateGroups() != null && !userTaskDbModel.candidateGroups().isEmpty()) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.USER_TASK,
              userTaskDbModel.userTaskKey(),
              "io.camunda.db.rdbms.sql.UserTaskMapper.insertCandidateGroups",
              userTaskDbModel));
    }
  }

  public void update(final UserTaskDbModel userTaskDbModel) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.USER_TASK,
            userTaskDbModel.userTaskKey(),
            "io.camunda.db.rdbms.sql.UserTaskMapper.update",
            userTaskDbModel));
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.USER_TASK,
            userTaskDbModel.userTaskKey(),
            "io.camunda.db.rdbms.sql.UserTaskMapper.deleteCandidateUsers",
            userTaskDbModel.userTaskKey()));
    if (userTaskDbModel.candidateUsers() != null && !userTaskDbModel.candidateUsers().isEmpty()) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.USER_TASK,
              userTaskDbModel.userTaskKey(),
              "io.camunda.db.rdbms.sql.UserTaskMapper.insertCandidateUsers",
              userTaskDbModel));
    }
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.USER_TASK,
            userTaskDbModel.userTaskKey(),
            "io.camunda.db.rdbms.sql.UserTaskMapper.deleteCandidateGroups",
            userTaskDbModel.userTaskKey()));
    if (userTaskDbModel.candidateGroups() != null && !userTaskDbModel.candidateGroups().isEmpty()) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.USER_TASK,
              userTaskDbModel.userTaskKey(),
              "io.camunda.db.rdbms.sql.UserTaskMapper.insertCandidateGroups",
              userTaskDbModel));
    }
  }

  public void scheduleForHistoryCleanup(
      final Long userTaskKey, final OffsetDateTime historyCleanupDate) {
    final boolean wasMerged =
        mergeToQueue(userTaskKey, b -> b.historyCleanupDate(historyCleanupDate));

    if (!wasMerged) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.USER_TASK,
              userTaskKey,
              "io.camunda.db.rdbms.sql.UserTaskMapper.updateHistoryCleanupDate",
              new UserTaskMapper.UpdateHistoryCleanupDateDto.Builder()
                  .userTaskKey(userTaskKey)
                  .historyCleanupDate(historyCleanupDate)
                  .build()));
    }
  }

  public void migrateToProcess(final UserTaskMigrationDbModel model) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.USER_TASK,
            model.userTaskKey(),
            "io.camunda.db.rdbms.sql.UserTaskMapper.migrateToProcess",
            model));
  }

  private boolean mergeToQueue(
      final long key,
      final Function<UserTaskDbModel.Builder, UserTaskDbModel.Builder> mergeFunction) {
    return executionQueue.tryMergeWithExistingQueueItem(
        new UpsertMerger<>(ContextType.USER_TASK, key, UserTaskDbModel.class, mergeFunction));
  }
}
