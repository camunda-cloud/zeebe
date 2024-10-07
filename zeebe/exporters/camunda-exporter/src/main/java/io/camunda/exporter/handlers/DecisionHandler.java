/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.handlers;

import static io.camunda.webapps.schema.entities.AbstractExporterEntity.DEFAULT_TENANT_IDENTIFIER;

import io.camunda.exporter.exceptions.PersistenceException;
import io.camunda.exporter.store.BatchRequest;
import io.camunda.webapps.schema.descriptors.operate.DecisionIndex;
import io.camunda.webapps.schema.entities.operate.dmn.definition.DecisionDefinitionEntity;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.ProcessIntent;
import io.camunda.zeebe.protocol.record.value.deployment.DecisionRecordValue;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecisionHandler
    implements ExportHandler<DecisionDefinitionEntity, DecisionRecordValue> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DecisionHandler.class);

  private static final Set<String> STATES = Set.of(ProcessIntent.CREATED.name());

  public DecisionHandler() {}

  @Override
  public ValueType getHandledValueType() {
    return ValueType.DECISION;
  }

  @Override
  public Class<DecisionDefinitionEntity> getEntityType() {
    return DecisionDefinitionEntity.class;
  }

  @Override
  public boolean handlesRecord(final Record<DecisionRecordValue> record) {
    final String intentStr = record.getIntent().name();
    return STATES.contains(intentStr);
  }

  @Override
  public List<String> generateIds(final Record<DecisionRecordValue> record) {
    return List.of(String.valueOf(record.getValue().getDecisionKey()));
  }

  @Override
  public DecisionDefinitionEntity createNewEntity(final String id) {
    return new DecisionDefinitionEntity().setId(id);
  }

  @Override
  public void updateEntity(
      final Record<DecisionRecordValue> record, final DecisionDefinitionEntity entity) {
    final DecisionRecordValue decision = record.getValue();
    entity
        .setId(String.valueOf(decision.getDecisionKey()))
        .setKey(decision.getDecisionKey())
        .setName(decision.getDecisionName())
        .setVersion(decision.getVersion())
        .setDecisionId(decision.getDecisionId())
        .setDecisionRequirementsId(decision.getDecisionRequirementsId())
        .setDecisionRequirementsKey(decision.getDecisionRequirementsKey())
        .setTenantId(DEFAULT_TENANT_IDENTIFIER); // TODO
  }

  @Override
  public void flush(final DecisionDefinitionEntity entity, final BatchRequest batchRequest)
      throws PersistenceException {
    batchRequest.addWithId(getIndexName(), toStringOrNull(entity.getKey()), entity);
  }

  private String getIndexName() {
    return new DecisionIndex("operate", true).getFullQualifiedName();
  }

  public static String toStringOrNull(final Object object) {
    return object == null ? null : object.toString();
  }
}
