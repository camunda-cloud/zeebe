/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.zeebeimport.processors;

import static io.camunda.operate.zeebeimport.util.ImportUtil.tenantOrDefault;
import static io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent.*;

import io.camunda.operate.entities.FlowNodeInstanceEntity;
import io.camunda.operate.entities.FlowNodeState;
import io.camunda.operate.entities.FlowNodeType;
import io.camunda.operate.exceptions.PersistenceException;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.schema.templates.FlowNodeInstanceTemplate;
import io.camunda.operate.store.BatchRequest;
import io.camunda.operate.store.FlowNodeStore;
import io.camunda.operate.util.ConversionUtils;
import io.camunda.operate.util.DateUtil;
import io.camunda.operate.zeebe.PartitionHolder;
import io.camunda.operate.zeebeimport.cache.FlowNodeInstanceRecord;
import io.camunda.operate.zeebeimport.cache.FlowNodeInstanceTreePathCache;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FlowNodeInstanceZeebeRecordProcessor {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FlowNodeInstanceZeebeRecordProcessor.class);

  private static final Set<String> AI_FINISH_STATES =
      Set.of(ELEMENT_COMPLETED.name(), ELEMENT_TERMINATED.name());
  private static final Set<String> AI_START_STATES = Set.of(ELEMENT_ACTIVATING.name());
  // treePath by flowNodeInstanceKey caches
  private final FlowNodeInstanceTreePathCache treePathCache;
  private final FlowNodeStore flowNodeStore;
  private final FlowNodeInstanceTemplate flowNodeInstanceTemplate;

  public FlowNodeInstanceZeebeRecordProcessor(
      final FlowNodeStore flowNodeStore,
      final FlowNodeInstanceTemplate flowNodeInstanceTemplate,
      final OperateProperties operateProperties,
      final PartitionHolder partitionHolder) {
    this.flowNodeStore = flowNodeStore;
    this.flowNodeInstanceTemplate = flowNodeInstanceTemplate;
    final var flowNodeTreeCacheSize = operateProperties.getImporter().getFlowNodeTreeCacheSize();
    final var partitionIds = partitionHolder.getPartitionIds();
    treePathCache =
        new FlowNodeInstanceTreePathCache(
            partitionIds, flowNodeTreeCacheSize, flowNodeStore::findParentTreePathFor);
  }

  public void processIncidentRecord(final Record record, final BatchRequest batchRequest)
      throws PersistenceException {
    final String intentStr = record.getIntent().name();
    final IncidentRecordValue recordValue = (IncidentRecordValue) record.getValue();

    // update activity instance
    final FlowNodeInstanceEntity entity =
        new FlowNodeInstanceEntity()
            .setId(ConversionUtils.toStringOrNull(recordValue.getElementInstanceKey()))
            .setKey(recordValue.getElementInstanceKey())
            .setPartitionId(record.getPartitionId())
            .setFlowNodeId(recordValue.getElementId())
            .setProcessInstanceKey(recordValue.getProcessInstanceKey())
            .setProcessDefinitionKey(recordValue.getProcessDefinitionKey())
            .setBpmnProcessId(recordValue.getBpmnProcessId())
            .setTenantId(tenantOrDefault(recordValue.getTenantId()));
    if (intentStr.equals(IncidentIntent.CREATED.name())) {
      entity.setIncidentKey(record.getKey());
    } else if (intentStr.equals(IncidentIntent.RESOLVED.name())) {
      entity.setIncidentKey(null);
    }

    LOGGER.debug("Flow node instance: id {}", entity.getId());
    final Map<String, Object> updateFields = new HashMap<>();
    updateFields.put(FlowNodeInstanceTemplate.INCIDENT_KEY, entity.getIncidentKey());
    batchRequest.upsert(
        flowNodeInstanceTemplate.getFullQualifiedName(), entity.getId(), entity, updateFields);
  }

  public void processProcessInstanceRecord(
      final Map<Long, List<Record<ProcessInstanceRecordValue>>> records,
      final List<Long> flowNodeInstanceKeysOrdered,
      final BatchRequest batchRequest)
      throws PersistenceException {

    for (final Long key : flowNodeInstanceKeysOrdered) {
      final List<Record<ProcessInstanceRecordValue>> wiRecords = records.get(key);
      FlowNodeInstanceEntity fniEntity = null;
      for (final Record<ProcessInstanceRecordValue> record : wiRecords) {

        if (shouldProcessProcessInstanceRecord(record)) {
          fniEntity = updateFlowNodeInstance(record, fniEntity);
        }
      }
      if (fniEntity != null) {
        LOGGER.debug("Flow node instance: id {}", fniEntity.getId());
        if (canOptimizeFlowNodeInstanceIndexing(fniEntity)) {
          batchRequest.add(flowNodeInstanceTemplate.getFullQualifiedName(), fniEntity);
        } else {
          final Map<String, Object> updateFields = new HashMap<>();
          updateFields.put(FlowNodeInstanceTemplate.ID, fniEntity.getId());
          updateFields.put(FlowNodeInstanceTemplate.PARTITION_ID, fniEntity.getPartitionId());
          updateFields.put(FlowNodeInstanceTemplate.TYPE, fniEntity.getType());
          updateFields.put(FlowNodeInstanceTemplate.STATE, fniEntity.getState());
          updateFields.put(FlowNodeInstanceTemplate.TREE_PATH, fniEntity.getTreePath());
          updateFields.put(FlowNodeInstanceTemplate.FLOW_NODE_ID, fniEntity.getFlowNodeId());
          updateFields.put(
              FlowNodeInstanceTemplate.PROCESS_DEFINITION_KEY, fniEntity.getProcessDefinitionKey());
          updateFields.put(FlowNodeInstanceTemplate.BPMN_PROCESS_ID, fniEntity.getBpmnProcessId());
          updateFields.put(FlowNodeInstanceTemplate.LEVEL, fniEntity.getLevel());
          if (fniEntity.getStartDate() != null) {
            updateFields.put(FlowNodeInstanceTemplate.START_DATE, fniEntity.getStartDate());
          }
          if (fniEntity.getEndDate() != null) {
            updateFields.put(FlowNodeInstanceTemplate.END_DATE, fniEntity.getEndDate());
          }
          if (fniEntity.getPosition() != null) {
            updateFields.put(FlowNodeInstanceTemplate.POSITION, fniEntity.getPosition());
          }
          batchRequest.upsert(
              flowNodeInstanceTemplate.getFullQualifiedName(),
              fniEntity.getId(),
              fniEntity,
              updateFields);
        }
      }
    }
  }

  private boolean shouldProcessProcessInstanceRecord(
      final Record<ProcessInstanceRecordValue> processInstanceRecord) {
    final var processInstanceRecordValue = processInstanceRecord.getValue();
    final var intent = processInstanceRecord.getIntent().name();
    return !isProcessEvent(processInstanceRecordValue)
        && (AI_START_STATES.contains(intent)
            || AI_FINISH_STATES.contains(intent)
            || ELEMENT_MIGRATED.name().equals(intent));
  }

  private static FlowNodeInstanceRecord toFNIRecord(
      final Record<?> record, final ProcessInstanceRecordValue recordValue) {
    return new FlowNodeInstanceRecord(
        record.getPartitionId(),
        record.getKey(),
        recordValue.getFlowScopeKey(),
        recordValue.getProcessInstanceKey());
  }

  private FlowNodeInstanceEntity updateFlowNodeInstance(
      final Record<ProcessInstanceRecordValue> record, FlowNodeInstanceEntity entity) {
    if (entity == null) {
      entity = new FlowNodeInstanceEntity();
    }

    final var recordValue = record.getValue();
    final var intentStr = record.getIntent().name();

    entity.setKey(record.getKey());
    entity.setId(ConversionUtils.toStringOrNull(record.getKey()));
    entity.setPartitionId(record.getPartitionId());
    entity.setFlowNodeId(recordValue.getElementId());
    entity.setProcessInstanceKey(recordValue.getProcessInstanceKey());
    entity.setProcessDefinitionKey(recordValue.getProcessDefinitionKey());
    entity.setBpmnProcessId(recordValue.getBpmnProcessId());
    entity.setTenantId(tenantOrDefault(recordValue.getTenantId()));

    if (entity.getTreePath() == null) {
      final String parentTreePath = treePathCache.resolveTreePath(toFNIRecord(record, recordValue));
      entity.setTreePath(
          String.join("/", parentTreePath, ConversionUtils.toStringOrNull(record.getKey())));
      entity.setLevel(parentTreePath.split("/").length);
    }

    if (AI_FINISH_STATES.contains(intentStr)) {
      if (intentStr.equals(ELEMENT_TERMINATED.name())) {
        entity.setState(FlowNodeState.TERMINATED);
      } else {
        entity.setState(FlowNodeState.COMPLETED);
      }
      entity.setEndDate(DateUtil.toOffsetDateTime(Instant.ofEpochMilli(record.getTimestamp())));
    } else {
      entity.setState(FlowNodeState.ACTIVE);
      if (AI_START_STATES.contains(intentStr)) {
        entity.setStartDate(DateUtil.toOffsetDateTime(Instant.ofEpochMilli(record.getTimestamp())));
        entity.setPosition(record.getPosition());
      }
    }

    entity.setType(
        FlowNodeType.fromZeebeBpmnElementType(
            recordValue.getBpmnElementType() == null
                ? null
                : recordValue.getBpmnElementType().name()));

    return entity;
  }

  private boolean canOptimizeFlowNodeInstanceIndexing(final FlowNodeInstanceEntity entity) {
    final var startDate = entity.getStartDate();
    final var endDate = entity.getEndDate();

    if (startDate != null && endDate != null) {
      // When the activating and completed/terminated events
      // for a flow node instance is part of the same batch
      // to import, then we can try to optimize the request
      // by submitting an IndexRequest instead of a UpdateRequest.
      // In such case, the following is assumed:
      // * When the duration between start and end time is lower than
      //   (or equal to) 2 seconds, then it can "safely" be assumed
      //   that there was no incident in between.
      // * The 2s duration is chosen arbitrarily. However, it should
      //   not be too short but not too long to avoid any negative
      //   side effects with incidents.
      final var duration = Duration.between(startDate, endDate);
      return duration.getSeconds() <= 2L;
    }

    return false;
  }

  private boolean isProcessEvent(final ProcessInstanceRecordValue recordValue) {
    return isOfType(recordValue, BpmnElementType.PROCESS);
  }

  private boolean isOfType(
      final ProcessInstanceRecordValue recordValue, final BpmnElementType type) {
    final BpmnElementType bpmnElementType = recordValue.getBpmnElementType();
    if (bpmnElementType == null) {
      return false;
    }
    return bpmnElementType.equals(type);
  }
}
