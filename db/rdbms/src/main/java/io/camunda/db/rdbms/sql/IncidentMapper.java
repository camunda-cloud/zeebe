/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.sql;

import io.camunda.db.rdbms.read.domain.IncidentDbQuery;
import io.camunda.db.rdbms.write.domain.IncidentDbModel;
import io.camunda.search.entities.IncidentEntity;
import io.camunda.util.ObjectBuilder;
import java.time.OffsetDateTime;
import java.util.List;

public interface IncidentMapper {

  void insert(IncidentDbModel incident);

  void updateState(IncidentStateDto dto);

  IncidentEntity findOne(Long incidentKey);

  Long count(IncidentDbQuery filter);

  List<IncidentEntity> search(IncidentDbQuery filter);

  record IncidentStateDto(
      Long incidentKey, IncidentEntity.IncidentState state, String errorMessage) {}

  record UpdateHistoryCleanupDateDto(long incidentKey, OffsetDateTime historyCleanupDate) {

    public static class Builder implements ObjectBuilder<UpdateHistoryCleanupDateDto> {

      private long incidentKey;
      private OffsetDateTime historyCleanupDate;

      public UpdateHistoryCleanupDateDto.Builder incidentKey(final long incidentKey) {
        this.incidentKey = incidentKey;
        return this;
      }

      public UpdateHistoryCleanupDateDto.Builder historyCleanupDate(
          final OffsetDateTime historyCleanupDate) {
        this.historyCleanupDate = historyCleanupDate;
        return this;
      }

      @Override
      public UpdateHistoryCleanupDateDto build() {
        return new UpdateHistoryCleanupDateDto(incidentKey, historyCleanupDate);
      }
    }
  }
}
