/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.sql;

import io.camunda.db.rdbms.read.domain.ProcessInstanceDbQuery;
import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.util.ObjectBuilder;
import java.time.OffsetDateTime;
import java.util.List;

public interface ProcessInstanceMapper {

  void insert(ProcessInstanceDbModel processInstance);

  void updateStateAndEndDate(EndProcessInstanceDto dto);

  void incrementIncidentCount(Long processInstanceKey);

  void decrementIncidentCount(Long processInstanceKey);

  ProcessInstanceEntity findOne(Long processInstanceKey);

  Long count(ProcessInstanceDbQuery filter);

  List<ProcessInstanceEntity> search(ProcessInstanceDbQuery filter);

  record EndProcessInstanceDto(
      long processInstanceKey,
      ProcessInstanceEntity.ProcessInstanceState state,
      OffsetDateTime endDate) {}

  record UpdateHistoryCleanupDateDto(long processInstanceKey, OffsetDateTime historyCleanupDate) {

    public static class Builder implements ObjectBuilder<UpdateHistoryCleanupDateDto> {

      private long processInstanceKey;
      private OffsetDateTime historyCleanupDate;

      public UpdateHistoryCleanupDateDto.Builder processInstanceKey(final long processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
        return this;
      }

      public UpdateHistoryCleanupDateDto.Builder historyCleanupDate(
          final OffsetDateTime historyCleanupDate) {
        this.historyCleanupDate = historyCleanupDate;
        return this;
      }

      @Override
      public UpdateHistoryCleanupDateDto build() {
        return new UpdateHistoryCleanupDateDto(processInstanceKey, historyCleanupDate);
      }
    }
  }
}
