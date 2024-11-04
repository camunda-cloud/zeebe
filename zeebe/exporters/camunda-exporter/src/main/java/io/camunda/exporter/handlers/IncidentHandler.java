/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.handlers;

import static io.camunda.webapps.schema.descriptors.operate.template.IncidentTemplate.*;

import io.camunda.exporter.store.BatchRequest;
import io.camunda.exporter.utils.ExporterUtil;
import io.camunda.webapps.schema.entities.operate.ErrorType;
import io.camunda.webapps.schema.entities.operate.IncidentEntity;
import io.camunda.webapps.schema.entities.operate.IncidentState;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncidentHandler implements ExportHandler<IncidentEntity, IncidentRecordValue> {

  private static final Logger LOGGER = LoggerFactory.getLogger(IncidentHandler.class);
  private final Map<String, Record<IncidentRecordValue>> recordsMap = new HashMap<>();
  private final String indexName;
  private final boolean concurrencyMode;

  public IncidentHandler(final String indexName, final boolean concurrencyMode) {
    this.indexName = indexName;
    this.concurrencyMode = concurrencyMode;
  }

  @Override
  public ValueType getHandledValueType() {
    return ValueType.INCIDENT;
  }

  @Override
  public Class<IncidentEntity> getEntityType() {
    return IncidentEntity.class;
  }

  @Override
  public boolean handlesRecord(final Record<IncidentRecordValue> record) {
    final String intentStr = record.getIntent().name();
    return !intentStr.equals(IncidentIntent.RESOLVED.toString());
  }

  @Override
  public List<String> generateIds(final Record<IncidentRecordValue> record) {
    return List.of(ExporterUtil.toStringOrNull(record.getKey()));
  }

  @Override
  public IncidentEntity createNewEntity(final String id) {
    return new IncidentEntity().setId(id);
  }

  @Override
  public void updateEntity(final Record<IncidentRecordValue> record, final IncidentEntity entity) {
    final IncidentRecordValue recordValue = record.getValue();
    final long incidentKey = record.getKey();
    entity
        .setId(ExporterUtil.toStringOrNull(incidentKey))
        .setKey(incidentKey)
        .setPartitionId(record.getPartitionId())
        .setPosition(record.getPosition());
    if (recordValue.getJobKey() > 0) {
      entity.setJobKey(recordValue.getJobKey());
    }
    if (recordValue.getProcessInstanceKey() > 0) {
      entity.setProcessInstanceKey(recordValue.getProcessInstanceKey());
    }
    if (recordValue.getProcessDefinitionKey() > 0) {
      entity.setProcessDefinitionKey(recordValue.getProcessDefinitionKey());
    }
    entity.setBpmnProcessId(recordValue.getBpmnProcessId());
    final String errorMessage = ExporterUtil.trimWhitespace(recordValue.getErrorMessage());
    entity
        .setErrorMessage(errorMessage)
        .setErrorType(
            ErrorType.fromZeebeErrorType(
                recordValue.getErrorType() == null ? null : recordValue.getErrorType().name()))
        .setFlowNodeId(recordValue.getElementId());
    if (recordValue.getElementInstanceKey() > 0) {
      entity.setFlowNodeInstanceKey(recordValue.getElementInstanceKey());
    }
    entity
        .setState(IncidentState.PENDING)
        .setCreationTime(
            OffsetDateTime.ofInstant(Instant.ofEpochMilli(record.getTimestamp()), ZoneOffset.UTC))
        .setTenantId(ExporterUtil.tenantOrDefault(recordValue.getTenantId()));

    recordsMap.put(entity.getId(), record);
  }

  @Override
  public void flush(final IncidentEntity entity, final BatchRequest batchRequest) {
    final String id = entity.getId();
    final Record<IncidentRecordValue> record = recordsMap.get(id);
    final String intentStr = (record == null) ? null : record.getIntent().name();
    if (intentStr == null) {
      LOGGER.warn("Intent is null for incident: id {}", id);
    }
    final Map<String, Object> updateFields = getUpdateFieldsMapByIntent(intentStr, entity);
    updateFields.put(POSITION, entity.getPosition());
    if (concurrencyMode) {
      batchRequest.upsertWithScript(
          indexName, String.valueOf(entity.getKey()), entity, getScript(), updateFields);
    } else {
      batchRequest.upsert(indexName, String.valueOf(entity.getKey()), entity, updateFields);
    }
  }

  @Override
  public String getIndexName() {
    return indexName;
  }

  private static Map<String, Object> getUpdateFieldsMapByIntent(
      final String intent, final IncidentEntity incidentEntity) {
    final Map<String, Object> updateFields = new HashMap<>();
    if (Objects.equals(intent, IncidentIntent.MIGRATED.name())) {
      updateFields.put(BPMN_PROCESS_ID, incidentEntity.getBpmnProcessId());
      updateFields.put(PROCESS_DEFINITION_KEY, incidentEntity.getProcessDefinitionKey());
      updateFields.put(FLOW_NODE_ID, incidentEntity.getFlowNodeId());
    }
    return updateFields;
  }

  private static String getScript() {
    return String.format(
        "if (ctx._source.%s == null || ctx._source.%s < params.%s) { "
            + "ctx._source.%s = params.%s; " // position
            + "if (params.%s != null) {"
            + "   ctx._source.%s = params.%s; " // PROCESS_DEFINITION_KEY
            + "   ctx._source.%s = params.%s; " // BPMN_PROCESS_ID
            + "   ctx._source.%s = params.%s; " // FLOW_NODE_ID
            + "}"
            + "}",
        POSITION,
        POSITION,
        POSITION,
        POSITION,
        POSITION,
        PROCESS_DEFINITION_KEY,
        PROCESS_DEFINITION_KEY,
        PROCESS_DEFINITION_KEY,
        BPMN_PROCESS_ID,
        BPMN_PROCESS_ID,
        FLOW_NODE_ID,
        FLOW_NODE_ID);
  }
}
