/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.sql;

import io.camunda.db.rdbms.read.domain.DecisionInstanceDbQuery;
import io.camunda.db.rdbms.write.domain.DecisionInstanceDbModel;
import io.camunda.search.entities.DecisionInstanceEntity;
import io.camunda.util.ObjectBuilder;
import java.time.OffsetDateTime;
import java.util.List;

public interface DecisionInstanceMapper {

  void insert(DecisionInstanceDbModel decisionInstance);

  Long count(DecisionInstanceDbQuery filter);

  List<DecisionInstanceEntity> search(DecisionInstanceDbQuery filter);

  List<DecisionInstanceDbModel.EvaluatedInput> loadInputs(List<String> decisionInstanceIds);

  List<DecisionInstanceDbModel.EvaluatedOutput> loadOutputs(List<String> decisionInstanceIds);

  record UpdateHistoryCleanupDateDto(long decisionInstanceKey, OffsetDateTime historyCleanupDate) {

    public static class Builder implements ObjectBuilder<UpdateHistoryCleanupDateDto> {

      private long decisionInstanceKey;
      private OffsetDateTime historyCleanupDate;

      public Builder decisionInstanceKey(final long decisionInstanceKey) {
        this.decisionInstanceKey = decisionInstanceKey;
        return this;
      }

      public Builder historyCleanupDate(final OffsetDateTime historyCleanupDate) {
        this.historyCleanupDate = historyCleanupDate;
        return this;
      }

      @Override
      public UpdateHistoryCleanupDateDto build() {
        return new UpdateHistoryCleanupDateDto(decisionInstanceKey, historyCleanupDate);
      }
    }
  }
}
