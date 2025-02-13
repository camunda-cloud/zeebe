/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.sql;

import io.camunda.db.rdbms.read.domain.FlowNodeInstanceDbQuery;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.util.ObjectBuilder;
import java.time.OffsetDateTime;
import java.util.List;

public interface FlowNodeInstanceMapper {

  void insert(FlowNodeInstanceDbModel flowNode);

  void updateStateAndEndDate(EndFlowNodeDto dto);

  void updateIncident(UpdateIncidentDto dto);

  Long count(FlowNodeInstanceDbQuery filter);

  List<FlowNodeInstanceEntity> search(FlowNodeInstanceDbQuery filter);

  record EndFlowNodeDto(
      long flowNodeInstanceKey,
      FlowNodeInstanceEntity.FlowNodeState state,
      OffsetDateTime endDate) {}

  record UpdateIncidentDto(long flowNodeInstanceKey, Long incidentKey) {}

  record UpdateHistoryCleanupDateDto(long flowNodeInstanceKey, OffsetDateTime historyCleanupDate) {

    public static class Builder implements ObjectBuilder<UpdateHistoryCleanupDateDto> {

      private long flowNodeInstanceKey;
      private OffsetDateTime historyCleanupDate;

      public UpdateHistoryCleanupDateDto.Builder flowNodeInstanceKey(
          final long flowNodeInstanceKey) {
        this.flowNodeInstanceKey = flowNodeInstanceKey;
        return this;
      }

      public UpdateHistoryCleanupDateDto.Builder historyCleanupDate(
          final OffsetDateTime historyCleanupDate) {
        this.historyCleanupDate = historyCleanupDate;
        return this;
      }

      @Override
      public UpdateHistoryCleanupDateDto build() {
        return new UpdateHistoryCleanupDateDto(flowNodeInstanceKey, historyCleanupDate);
      }
    }
  }
}
