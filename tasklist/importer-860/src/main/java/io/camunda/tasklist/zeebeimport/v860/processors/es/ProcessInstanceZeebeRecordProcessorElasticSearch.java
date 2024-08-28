/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.zeebeimport.v860.processors.es;

import static io.camunda.tasklist.util.ElasticsearchUtil.UPDATE_RETRY_COUNT;
import static io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent.ELEMENT_ACTIVATING;
import static io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent.ELEMENT_COMPLETED;
import static io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent.ELEMENT_TERMINATED;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.tasklist.entities.FlowNodeInstanceEntity;
import io.camunda.tasklist.entities.FlowNodeType;
import io.camunda.tasklist.entities.ProcessInstanceEntity;
import io.camunda.tasklist.entities.ProcessInstanceState;
import io.camunda.tasklist.entities.TaskVariableSnapshotEntity;
import io.camunda.tasklist.exceptions.PersistenceException;
import io.camunda.tasklist.schema.indices.FlowNodeInstanceIndex;
import io.camunda.tasklist.schema.indices.ProcessInstanceIndex;
import io.camunda.tasklist.schema.templates.TasklistTaskVariableSnapshotTemplate;
import io.camunda.tasklist.util.ConversionUtils;
import io.camunda.tasklist.util.DateUtil;
import io.camunda.tasklist.zeebeimport.v860.record.value.ProcessInstanceRecordValueImpl;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ProcessInstanceZeebeRecordProcessorElasticSearch {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ProcessInstanceZeebeRecordProcessorElasticSearch.class);

  private static final Set<String> FLOW_NODE_STATES = new HashSet<>();
  private static final Set<String> PROCESS_INSTANCE_STATES = new HashSet<>();

  private static final List<BpmnElementType> VARIABLE_SCOPE_TYPES =
      Arrays.asList(
          BpmnElementType.PROCESS,
          BpmnElementType.SUB_PROCESS,
          BpmnElementType.EVENT_SUB_PROCESS,
          BpmnElementType.SERVICE_TASK,
          BpmnElementType.USER_TASK,
          BpmnElementType.MULTI_INSTANCE_BODY);

  static {
    FLOW_NODE_STATES.add(ELEMENT_ACTIVATING.name());
    PROCESS_INSTANCE_STATES.add(ELEMENT_COMPLETED.name());
    PROCESS_INSTANCE_STATES.add(ELEMENT_TERMINATED.name());
  }

  @Autowired
  @Qualifier("tasklistObjectMapper")
  private ObjectMapper objectMapper;

  @Autowired private FlowNodeInstanceIndex flowNodeInstanceIndex;

  @Autowired private ProcessInstanceIndex processInstanceIndex;

  @Autowired private TasklistTaskVariableSnapshotTemplate taskVariableSnapshotTemplate;

  public void processProcessInstanceRecord(final Record record, final BulkRequest bulkRequest)
      throws PersistenceException {

    final ProcessInstanceRecordValueImpl recordValue =
        (ProcessInstanceRecordValueImpl) record.getValue();
    if (isVariableScopeType(recordValue) && FLOW_NODE_STATES.contains(record.getIntent().name())) {
      final FlowNodeInstanceEntity flowNodeInstance = createFlowNodeInstance(record);
      bulkRequest.add(getFlowNodeInstanceQuery(flowNodeInstance));

      final UpdateRequest processUserTaskAndProcessListView = persistSnapshot(flowNodeInstance);

      if (processUserTaskAndProcessListView != null) {
        bulkRequest.add(persistSnapshot(flowNodeInstance));
      }
    }

    if (isProcessEvent(recordValue)
        && PROCESS_INSTANCE_STATES.contains(record.getIntent().name())) {
      final ProcessInstanceEntity processInstanceEntity = createProcessInstance(record);
      bulkRequest.add(getProcessInstanceQuery(processInstanceEntity));
    }
  }

  private ProcessInstanceEntity createProcessInstance(final Record record) {
    final ProcessInstanceEntity entity = new ProcessInstanceEntity();
    entity.setId(ConversionUtils.toStringOrNull(record.getKey()));
    entity.setKey(record.getKey());
    entity.setPartitionId(record.getPartitionId());
    if (ELEMENT_COMPLETED.name().equals(record.getIntent().name())) {
      entity.setState(ProcessInstanceState.COMPLETED);
    } else if (ELEMENT_TERMINATED.name().equals(record.getIntent().name())) {
      entity.setState(ProcessInstanceState.CANCELED);
    }
    entity.setEndDate(DateUtil.toOffsetDateTime(Instant.ofEpochMilli(record.getTimestamp())));
    return entity;
  }

  private FlowNodeInstanceEntity createFlowNodeInstance(final Record record) {
    final ProcessInstanceRecordValueImpl recordValue =
        (ProcessInstanceRecordValueImpl) record.getValue();
    final FlowNodeInstanceEntity entity = new FlowNodeInstanceEntity();
    entity.setId(ConversionUtils.toStringOrNull(record.getKey()));
    entity.setKey(record.getKey());
    entity.setPartitionId(record.getPartitionId());
    entity.setProcessInstanceId(String.valueOf(recordValue.getProcessInstanceKey()));
    entity.setParentFlowNodeId(String.valueOf(recordValue.getFlowScopeKey()));
    entity.setType(
        FlowNodeType.fromZeebeBpmnElementType(
            recordValue.getBpmnElementType() == null
                ? null
                : recordValue.getBpmnElementType().name()));
    entity.setPosition(record.getPosition());
    return entity;
  }

  private IndexRequest getFlowNodeInstanceQuery(final FlowNodeInstanceEntity entity)
      throws PersistenceException {
    try {
      LOGGER.debug("Flow node instance: id {}", entity.getId());

      return new IndexRequest(flowNodeInstanceIndex.getFullQualifiedName())
          .id(entity.getId())
          .source(objectMapper.writeValueAsString(entity), XContentType.JSON);
    } catch (final IOException e) {
      throw new PersistenceException(
          String.format(
              "Error preparing the query to index flow node instance [%s]", entity.getId()),
          e);
    }
  }

  private IndexRequest getProcessInstanceQuery(final ProcessInstanceEntity entity)
      throws PersistenceException {
    try {
      LOGGER.debug("Process instance: id {}", entity.getId());

      return new IndexRequest()
          .index(processInstanceIndex.getFullQualifiedName())
          .id(entity.getId())
          .source(objectMapper.writeValueAsString(entity), XContentType.JSON);
    } catch (final IOException e) {
      throw new PersistenceException(
          String.format(
              "Error preparing the query to index process instance [%s]w", entity.getId()),
          e);
    }
  }

  private boolean isVariableScopeType(final ProcessInstanceRecordValueImpl recordValue) {
    final BpmnElementType bpmnElementType = recordValue.getBpmnElementType();
    if (bpmnElementType == null) {
      return false;
    }
    return VARIABLE_SCOPE_TYPES.contains(bpmnElementType);
  }

  private boolean isProcessEvent(final ProcessInstanceRecordValueImpl recordValue) {
    return isOfType(recordValue, BpmnElementType.PROCESS);
  }

  private boolean isOfType(
      final ProcessInstanceRecordValueImpl recordValue, final BpmnElementType type) {
    final BpmnElementType bpmnElementType = recordValue.getBpmnElementType();
    if (bpmnElementType == null) {
      return false;
    }
    return bpmnElementType.equals(type);
  }

  private UpdateRequest persistSnapshot(final FlowNodeInstanceEntity flowNodeInstance)
      throws PersistenceException {
    final TaskVariableSnapshotEntity entity = new TaskVariableSnapshotEntity();

    final Map<String, Object> joinField = new HashMap<>();
    if (flowNodeInstance.getType().equals(FlowNodeType.PROCESS)) {
      entity.setId(flowNodeInstance.getId());
      joinField.put("name", "process");
      entity.setJoin(joinField);
      entity.setDataType(String.valueOf(FlowNodeType.PROCESS));
      return getUpdateRequest(flowNodeInstance, entity, null);
    } else if (flowNodeInstance.getType().equals(FlowNodeType.SUB_PROCESS)) {
      entity.setId(flowNodeInstance.getId());
      joinField.put("name", "task");
      joinField.put("parent", flowNodeInstance.getProcessInstanceId());
      entity.setDataType(String.valueOf(FlowNodeType.SUB_PROCESS));
      entity.setJoin(joinField);
      return getUpdateRequest(flowNodeInstance, entity, flowNodeInstance.getProcessInstanceId());
    } else {
      return null;
    }
  }

  private UpdateRequest getUpdateRequest(
      final FlowNodeInstanceEntity flowNodeInstance,
      final TaskVariableSnapshotEntity entity,
      final String routingKey)
      throws PersistenceException {
    try {
      final Map<String, Object> jsonMap =
          objectMapper.readValue(objectMapper.writeValueAsString(entity), HashMap.class);
      final UpdateRequest updateRequest =
          new UpdateRequest()
              .index(taskVariableSnapshotTemplate.getFullQualifiedName())
              .id(entity.getId())
              .routing(routingKey)
              .upsert(objectMapper.writeValueAsString(entity), XContentType.JSON)
              .doc(jsonMap)
              .retryOnConflict(UPDATE_RETRY_COUNT);

      if (flowNodeInstance.getType().equals(FlowNodeType.USER_TASK)) {
        updateRequest.routing(flowNodeInstance.getProcessInstanceId());
      }

      return updateRequest;
    } catch (final IOException e) {
      throw new PersistenceException("Error preparing the query to upsert snapshot entity", e);
    }
  }
}
