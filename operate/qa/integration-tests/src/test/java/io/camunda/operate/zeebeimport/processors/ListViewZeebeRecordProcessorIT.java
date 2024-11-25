/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.zeebeimport.processors;

import static io.camunda.operate.util.TestUtil.createFlowNodeInstance;
import static io.camunda.operate.util.TestUtil.createProcessInstance;
import static io.camunda.operate.util.TestUtil.createVariableForListView;
import static io.camunda.operate.util.ZeebeRecordTestUtil.createZeebeRecordFromFni;
import static io.camunda.operate.util.ZeebeRecordTestUtil.createZeebeRecordFromPi;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.ACTIVITIES_JOIN_RELATION;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.PROCESS_INSTANCE_JOIN_RELATION;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.VARIABLES_JOIN_RELATION;
import static io.camunda.webapps.schema.entities.operate.listview.ProcessInstanceState.ACTIVE;
import static io.camunda.zeebe.protocol.record.intent.IncidentIntent.CREATED;
import static io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent.ELEMENT_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.operate.cache.ProcessCache;
import io.camunda.operate.exceptions.PersistenceException;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.store.BatchRequest;
import io.camunda.operate.util.j5templates.OperateSearchAbstractIT;
import io.camunda.operate.zeebe.PartitionHolder;
import io.camunda.operate.zeebeimport.ImportBatch;
import io.camunda.operate.zeebeimport.ImportPositionHolder;
import io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate;
import io.camunda.webapps.schema.entities.operate.FlowNodeState;
import io.camunda.webapps.schema.entities.operate.listview.FlowNodeInstanceForListViewEntity;
import io.camunda.webapps.schema.entities.operate.listview.ProcessInstanceForListViewEntity;
import io.camunda.webapps.schema.entities.operate.listview.VariableForListViewEntity;
import io.camunda.zeebe.protocol.record.ImmutableRecord;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.intent.VariableIntent;
import io.camunda.zeebe.protocol.record.value.ImmutableIncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.ImmutableJobRecordValue;
import io.camunda.zeebe.protocol.record.value.ImmutableVariableRecordValue;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ListViewZeebeRecordProcessorIT extends OperateSearchAbstractIT {

  private final int newVersion = 111;
  private final String newBpmnProcessId = "newBpmnProcessId";
  private final long newProcessDefinitionKey = 111;
  private final String newProcessName = "New process name";
  private final String errorMessage = "Error message";
  @Autowired private ListViewTemplate listViewTemplate;
  @Autowired private ListViewZeebeRecordProcessor listViewZeebeRecordProcessor;
  @Autowired private BeanFactory beanFactory;
  @MockBean private PartitionHolder partitionHolder;
  @MockBean private ProcessCache processCache;
  @Autowired private ImportPositionHolder importPositionHolder;
  @Autowired private OperateProperties operateProperties;
  private boolean concurrencyModeBefore;

  @Override
  protected void runAdditionalBeforeAllSetup() throws Exception {
    when(partitionHolder.getPartitionIds()).thenReturn(List.of(1));
    concurrencyModeBefore = importPositionHolder.getConcurrencyMode();
    importPositionHolder.setConcurrencyMode(true);
  }

  @Override
  @AfterAll
  public void afterAllTeardown() {
    importPositionHolder.setConcurrencyMode(concurrencyModeBefore);
    super.afterAllTeardown();
  }

  @Test
  public void shouldNotOverrideProcessInstanceFields() throws IOException, PersistenceException {
    // having
    // process instance entity with position = 2
    final long oldPosition = 2L;
    final ProcessInstanceForListViewEntity pi = createProcessInstance().setPosition(oldPosition);
    testSearchRepository.createOrUpdateDocumentFromObject(
        listViewTemplate.getFullQualifiedName(), pi.getId(), pi);

    // when
    // importing Zeebe record with smaller position
    when(processCache.getProcessNameOrDefaultValue(eq(newProcessDefinitionKey), anyString()))
        .thenReturn(newProcessName);
    final long newPosition = 1L;
    final Record<ProcessInstanceRecordValue> zeebeRecord =
        createZeebeRecordFromPi(
            pi,
            b -> b.withPosition(newPosition).withIntent(ELEMENT_COMPLETED),
            b ->
                b.withVersion(newVersion)
                    .withBpmnProcessId(newBpmnProcessId)
                    .withProcessDefinitionKey(newProcessDefinitionKey));
    importProcessInstanceZeebeRecord(zeebeRecord);

    // then
    // process instance fields are updated
    final ProcessInstanceForListViewEntity updatedPI = findProcessInstanceByKey(pi.getKey());
    // old values
    assertThat(updatedPI.getProcessInstanceKey()).isEqualTo(pi.getProcessInstanceKey());
    assertThat(updatedPI.getTenantId()).isEqualTo(pi.getTenantId());
    assertThat(updatedPI.getKey()).isEqualTo(pi.getKey());
    assertThat(updatedPI.getTenantId()).isEqualTo(pi.getTenantId());
    assertThat(updatedPI.getStartDate()).isNotNull();
    // old values
    assertThat(updatedPI.getProcessName()).isEqualTo(pi.getProcessName());
    assertThat(updatedPI.getProcessDefinitionKey()).isEqualTo(pi.getProcessDefinitionKey());
    assertThat(updatedPI.getProcessVersion()).isEqualTo(pi.getProcessVersion());
    assertThat(updatedPI.getState()).isEqualTo(ACTIVE);
    assertThat(updatedPI.getEndDate()).isNull();
    assertThat(updatedPI.getPosition()).isEqualTo(oldPosition);
  }

  @Test
  public void shouldNotOverrideIncidentErrorMsg() throws IOException, PersistenceException {
    // having
    // flow node instance entity with position = 2
    final long processInstanceKey = 111L;
    final FlowNodeInstanceForListViewEntity fni =
        createFlowNodeInstance(processInstanceKey, FlowNodeState.ACTIVE).setPositionIncident(2L);
    testSearchRepository.createOrUpdateDocumentFromObject(
        listViewTemplate.getFullQualifiedName(),
        fni.getId(),
        fni,
        String.valueOf(processInstanceKey));

    // when
    // importing Zeebe record with smaller position
    final long newPosition = 1L;
    final Record<IncidentRecordValue> zeebeRecord =
        (Record)
            ImmutableRecord.builder()
                .withKey(112L)
                .withPosition(newPosition)
                .withIntent(CREATED)
                .withValue(
                    ImmutableIncidentRecordValue.builder()
                        .withElementInstanceKey(fni.getKey())
                        .withErrorMessage(errorMessage)
                        .build())
                .build();
    importIncidentZeebeRecord(zeebeRecord);

    // then
    // incident fields are not updated
    final FlowNodeInstanceForListViewEntity updatedFni = findFlowNodeInstanceByKey(fni.getKey());
    // old values
    assertThat(updatedFni.getKey()).isEqualTo(fni.getKey());
    assertThat(updatedFni.getErrorMessage()).isNull();
    assertThat(updatedFni.getPositionIncident()).isEqualTo(fni.getPositionIncident());
  }

  @Test
  public void shouldNotOverrideVariableFields() throws IOException, PersistenceException {
    // having
    // process instance entity with position = 2
    final long processInstanceKey = 111L;
    final VariableForListViewEntity var =
        createVariableForListView(processInstanceKey).setPosition(2L);
    testSearchRepository.createOrUpdateDocumentFromObject(
        listViewTemplate.getFullQualifiedName(),
        var.getId(),
        var,
        String.valueOf(processInstanceKey));

    // when
    // importing Zeebe record with bigger position
    final long newPosition = 1L;
    final String newValue = "newValue";
    final Record<VariableRecordValue> zeebeRecord =
        (Record)
            ImmutableRecord.builder()
                .withKey(113L)
                .withPosition(newPosition)
                .withIntent(VariableIntent.UPDATED)
                .withValue(
                    ImmutableVariableRecordValue.builder()
                        .withName(var.getVarName())
                        .withValue(newValue)
                        .withScopeKey(var.getScopeKey())
                        .withProcessInstanceKey(processInstanceKey)
                        .build())
                .build();
    importVariableZeebeRecord(zeebeRecord);

    // then
    // variable fields are not updated
    final VariableForListViewEntity updatedVar = variableById(var.getId());
    // old values
    assertThat(updatedVar.getId()).isEqualTo(var.getId());
    assertThat(updatedVar.getVarName()).isEqualTo(var.getVarName());
    assertThat(updatedVar.getVarValue()).isEqualTo(var.getVarValue());
  }

  @Test
  public void shouldNotOverrideFlowNodeInstanceFields() throws IOException, PersistenceException {
    // having
    // flow node instance entity with position = 2
    final long processInstanceKey = 222L;
    final FlowNodeInstanceForListViewEntity fni =
        createFlowNodeInstance(processInstanceKey, FlowNodeState.ACTIVE).setPosition(2L);
    testSearchRepository.createOrUpdateDocumentFromObject(
        listViewTemplate.getFullQualifiedName(),
        fni.getId(),
        fni,
        String.valueOf(processInstanceKey));

    // when
    // importing Zeebe record with bigger position
    final long newPosition = 1L;
    final Record<ProcessInstanceRecordValue> zeebeRecord =
        createZeebeRecordFromFni(
            fni, b -> b.withPosition(newPosition).withIntent(ELEMENT_COMPLETED), null);
    importProcessInstanceZeebeRecord(zeebeRecord);

    // then
    // incident fields are updated
    final FlowNodeInstanceForListViewEntity updatedFni = findFlowNodeInstanceByKey(fni.getKey());
    // old values
    assertThat(updatedFni.getKey()).isEqualTo(fni.getKey());
    assertThat(updatedFni.getActivityState()).isEqualTo(FlowNodeState.ACTIVE);
    assertThat(updatedFni.getPosition()).isEqualTo(2L);
  }

  @Test
  public void shouldNotOverrideJobFailedWithRetriesField()
      throws IOException, PersistenceException {
    // having
    // flow node instance entity with position = 2
    final long processInstanceKey = 333L;
    final FlowNodeInstanceForListViewEntity fni =
        createFlowNodeInstance(processInstanceKey, FlowNodeState.ACTIVE).setPositionJob(2L);
    testSearchRepository.createOrUpdateDocumentFromObject(
        listViewTemplate.getFullQualifiedName(),
        fni.getId(),
        fni,
        String.valueOf(processInstanceKey));

    // when
    // importing Zeebe record with bigger position
    final long newPosition = 1L;
    final Record<JobRecordValue> zeebeRecord =
        (Record)
            ImmutableRecord.builder()
                .withKey(115L)
                .withPosition(newPosition)
                .withIntent(JobIntent.FAILED)
                .withValue(
                    ImmutableJobRecordValue.builder()
                        .withElementInstanceKey(fni.getKey())
                        .withProcessInstanceKey(processInstanceKey)
                        .withRetries(1)
                        .build())
                .build();
    importJobZeebeRecord(zeebeRecord);

    // then
    // incident fields are updated
    final FlowNodeInstanceForListViewEntity updatedFni = findFlowNodeInstanceByKey(fni.getKey());
    // old values
    assertThat(updatedFni.getKey()).isEqualTo(fni.getKey());
    assertThat(updatedFni.isJobFailedWithRetriesLeft()).isEqualTo(false);
    assertThat(updatedFni.getPositionJob()).isEqualTo(2L);
  }

  @NotNull
  private ProcessInstanceForListViewEntity findProcessInstanceByKey(final long key)
      throws IOException {
    final List<ProcessInstanceForListViewEntity> entities =
        testSearchRepository.searchJoinRelation(
            listViewTemplate.getFullQualifiedName(),
            PROCESS_INSTANCE_JOIN_RELATION,
            ProcessInstanceForListViewEntity.class,
            10);
    final Optional<ProcessInstanceForListViewEntity> first =
        entities.stream().filter(p -> p.getKey() == key).findFirst();
    assertThat(first.isPresent()).isTrue();
    return first.get();
  }

  @NotNull
  private FlowNodeInstanceForListViewEntity findFlowNodeInstanceByKey(final long key)
      throws IOException {
    final List<FlowNodeInstanceForListViewEntity> entities =
        testSearchRepository.searchJoinRelation(
            listViewTemplate.getFullQualifiedName(),
            ACTIVITIES_JOIN_RELATION,
            FlowNodeInstanceForListViewEntity.class,
            10);
    final Optional<FlowNodeInstanceForListViewEntity> first =
        entities.stream().filter(p -> p.getKey() == key).findFirst();
    assertThat(first.isPresent()).isTrue();
    return first.get();
  }

  @NotNull
  private VariableForListViewEntity variableById(final String id) throws IOException {
    final List<VariableForListViewEntity> entities =
        testSearchRepository.searchJoinRelation(
            listViewTemplate.getFullQualifiedName(),
            VARIABLES_JOIN_RELATION,
            VariableForListViewEntity.class,
            10);
    final Optional<VariableForListViewEntity> first =
        entities.stream().filter(p -> p.getId().equals(id)).findFirst();
    assertThat(first.isPresent()).isTrue();
    return first.get();
  }

  private void importProcessInstanceZeebeRecord(
      final Record<ProcessInstanceRecordValue> zeebeRecord) throws PersistenceException {
    final BatchRequest batchRequest = beanFactory.getBean(BatchRequest.class);
    listViewZeebeRecordProcessor.processProcessInstanceRecord(
        (Map) Map.of(zeebeRecord.getKey(), List.of(zeebeRecord)),
        batchRequest,
        mock(ImportBatch.class),
        true);
    batchRequest.execute();
    searchContainerManager.refreshIndices(listViewTemplate.getFullQualifiedName());
  }

  private void importIncidentZeebeRecord(final Record<IncidentRecordValue> zeebeRecord)
      throws PersistenceException {
    final BatchRequest batchRequest = beanFactory.getBean(BatchRequest.class);
    listViewZeebeRecordProcessor.processIncidentRecord(zeebeRecord, batchRequest, true);
    batchRequest.execute();
    searchContainerManager.refreshIndices(listViewTemplate.getFullQualifiedName());
  }

  private void importVariableZeebeRecord(final Record<VariableRecordValue> zeebeRecord)
      throws PersistenceException {
    final BatchRequest batchRequest = beanFactory.getBean(BatchRequest.class);
    listViewZeebeRecordProcessor.processVariableRecords(
        (Map) Map.of(zeebeRecord.getKey(), List.of(zeebeRecord)), batchRequest, true);
    batchRequest.execute();
    searchContainerManager.refreshIndices(listViewTemplate.getFullQualifiedName());
  }

  private void importJobZeebeRecord(final Record<JobRecordValue> zeebeRecord)
      throws PersistenceException {
    final BatchRequest batchRequest = beanFactory.getBean(BatchRequest.class);
    listViewZeebeRecordProcessor.processJobRecords(
        (Map) Map.of(zeebeRecord.getKey(), List.of(zeebeRecord)), batchRequest, true);
    batchRequest.execute();
    searchContainerManager.refreshIndices(listViewTemplate.getFullQualifiedName());
  }
}
