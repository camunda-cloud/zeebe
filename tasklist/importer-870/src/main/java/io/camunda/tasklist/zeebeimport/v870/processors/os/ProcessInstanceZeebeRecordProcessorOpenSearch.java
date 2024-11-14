/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.zeebeimport.v870.processors.os;

import static io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.tasklist.CommonUtils;
import io.camunda.tasklist.data.conditionals.OpenSearchCondition;
import io.camunda.tasklist.exceptions.PersistenceException;
import io.camunda.tasklist.util.ConversionUtils;
import io.camunda.tasklist.util.DateUtil;
import io.camunda.tasklist.util.OpenSearchUtil;
import io.camunda.tasklist.v86.entities.FlowNodeInstanceEntity;
import io.camunda.tasklist.v86.entities.FlowNodeType;
import io.camunda.tasklist.v86.entities.ProcessInstanceEntity;
import io.camunda.tasklist.v86.entities.ProcessInstanceState;
import io.camunda.tasklist.v86.entities.listview.ListViewJoinRelation;
import io.camunda.tasklist.v86.entities.listview.ProcessInstanceListViewEntity;
import io.camunda.tasklist.v86.schema.indices.TasklistFlowNodeInstanceIndex;
import io.camunda.tasklist.v86.schema.indices.TasklistProcessInstanceIndex;
import io.camunda.tasklist.v86.schema.templates.TasklistListViewTemplate;
import io.camunda.tasklist.zeebeimport.v870.record.value.ProcessInstanceRecordValueImpl;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(OpenSearchCondition.class)
public class ProcessInstanceZeebeRecordProcessorOpenSearch {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ProcessInstanceZeebeRecordProcessorOpenSearch.class);

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

  @Autowired private TasklistFlowNodeInstanceIndex flowNodeInstanceIndex;

  @Autowired private TasklistProcessInstanceIndex processInstanceIndex;
  @Autowired private TasklistListViewTemplate tasklistListViewTemplate;

  public void processProcessInstanceRecord(
      final Record record, final List<BulkOperation> operations) throws PersistenceException {

    final ProcessInstanceRecordValueImpl recordValue =
        (ProcessInstanceRecordValueImpl) record.getValue();
    if (isVariableScopeType(recordValue) && FLOW_NODE_STATES.contains(record.getIntent().name())) {
      final FlowNodeInstanceEntity flowNodeInstance = createFlowNodeInstance(record);
      operations.add(getFlowNodeInstanceQuery(flowNodeInstance));
      final BulkOperation persistFlowNodeDataToListView =
          persistFlowNodeDataToListView(flowNodeInstance);
      if (persistFlowNodeDataToListView != null) {
        operations.add(persistFlowNodeDataToListView(flowNodeInstance));
      }
    }

    if (isProcessEvent(recordValue)
        && PROCESS_INSTANCE_STATES.contains(record.getIntent().name())) {
      final ProcessInstanceEntity processInstanceEntity = createProcessInstance(record);
      operations.add(getProcessInstanceQuery(processInstanceEntity));
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

  private BulkOperation getFlowNodeInstanceQuery(final FlowNodeInstanceEntity entity) {

    LOGGER.debug("Flow node instance: id {}", entity.getId());

    return new BulkOperation.Builder()
        .index(
            IndexOperation.of(
                io ->
                    io.index(flowNodeInstanceIndex.getFullQualifiedName())
                        .id(entity.getId())
                        .document(CommonUtils.getJsonObjectFromEntity(entity))))
        .build();
  }

  private BulkOperation getProcessInstanceQuery(final ProcessInstanceEntity entity) {

    LOGGER.debug("Process instance: id {}", entity.getId());

    return new BulkOperation.Builder()
        .index(
            IndexOperation.of(
                io ->
                    io.index(processInstanceIndex.getFullQualifiedName())
                        .id(entity.getId())
                        .document(CommonUtils.getJsonObjectFromEntity(entity))))
        .build();
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

  private BulkOperation persistFlowNodeDataToListView(
      final FlowNodeInstanceEntity flowNodeInstance) {
    final ProcessInstanceListViewEntity processInstanceListViewEntity =
        new ProcessInstanceListViewEntity();

    if (flowNodeInstance.getType().equals(FlowNodeType.PROCESS)) {
      processInstanceListViewEntity.setJoin(new ListViewJoinRelation());
      processInstanceListViewEntity.setId(flowNodeInstance.getId());
      processInstanceListViewEntity.setPartitionId(flowNodeInstance.getPartitionId());
      processInstanceListViewEntity.setTenantId(flowNodeInstance.getTenantId());
      processInstanceListViewEntity.getJoin().setName("process");
      return getUpdateRequest(processInstanceListViewEntity);
    } else {
      return null;
    }
  }

  private BulkOperation getUpdateRequest(
      final ProcessInstanceListViewEntity processInstanceListViewEntity) {

    return new BulkOperation.Builder()
        .update(
            up ->
                up.index(tasklistListViewTemplate.getFullQualifiedName())
                    .id(processInstanceListViewEntity.getId())
                    .document(CommonUtils.getJsonObjectFromEntity(processInstanceListViewEntity))
                    .docAsUpsert(true)
                    .retryOnConflict(OpenSearchUtil.UPDATE_RETRY_COUNT))
        .build();
  }
}
