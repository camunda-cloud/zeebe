/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.sql;

import io.camunda.db.rdbms.read.domain.UserTaskDbQuery;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskMigrationDbModel;
import io.camunda.util.ObjectBuilder;
import java.time.OffsetDateTime;
import java.util.List;

public interface UserTaskMapper {

  void insert(UserTaskDbModel taskDbModel);

  void insertCandidateUsers(UserTaskDbModel taskDbModel);

  void insertCandidateGroups(UserTaskDbModel taskDbModel);

  void update(UserTaskDbModel taskDbModel);

  void deleteCandidateUsers(Long key);

  void deleteCandidateGroups(Long key);

  void migrateToProcess(UserTaskMigrationDbModel dto);

  Long count(UserTaskDbQuery filter);

  List<UserTaskDbModel> search(UserTaskDbQuery filter);

  record UpdateHistoryCleanupDateDto(long userTaskKey, OffsetDateTime historyCleanupDate) {

    public static class Builder implements ObjectBuilder<UpdateHistoryCleanupDateDto> {

      private long userTaskKey;
      private OffsetDateTime historyCleanupDate;

      public UpdateHistoryCleanupDateDto.Builder userTaskKey(final long userTaskKey) {
        this.userTaskKey = userTaskKey;
        return this;
      }

      public UpdateHistoryCleanupDateDto.Builder historyCleanupDate(
          final OffsetDateTime historyCleanupDate) {
        this.historyCleanupDate = historyCleanupDate;
        return this;
      }

      @Override
      public UpdateHistoryCleanupDateDto build() {
        return new UpdateHistoryCleanupDateDto(userTaskKey, historyCleanupDate);
      }
    }
  }
}
