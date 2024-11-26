/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.exporter;

import io.camunda.zeebe.protocol.record.ImmutableRecord;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordValue;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.DecisionIntent;
import io.camunda.zeebe.protocol.record.intent.DecisionRequirementsIntent;
import io.camunda.zeebe.protocol.record.intent.FormIntent;
import io.camunda.zeebe.protocol.record.intent.MappingIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessIntent;
import io.camunda.zeebe.protocol.record.intent.UserIntent;
import io.camunda.zeebe.protocol.record.intent.UserTaskIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ImmutableProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ImmutableUserRecordValue;
import io.camunda.zeebe.protocol.record.value.ImmutableUserTaskRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.UserRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.DecisionRequirementsRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.ImmutableDecisionRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.ImmutableDecisionRequirementsRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.ImmutableProcess;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import io.camunda.zeebe.test.broker.protocol.ProtocolFactory;
import java.time.OffsetDateTime;
import java.util.List;

public class RecordFixtures {

  protected static final ProtocolFactory FACTORY = new ProtocolFactory(System.nanoTime());

  protected static ImmutableRecord<RecordValue> getProcessInstanceStartedRecord(
      final Long position) {
    final io.camunda.zeebe.protocol.record.Record<RecordValue> recordValueRecord =
        FACTORY.generateRecord(ValueType.PROCESS_INSTANCE);
    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATING)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withValue(
            ImmutableProcessInstanceRecordValue.builder()
                .from((ProcessInstanceRecordValue) recordValueRecord.getValue())
                .withBpmnElementType(BpmnElementType.PROCESS)
                .withVersion(1)
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getProcessInstanceCompletedRecord(
      final Long position, final long processInstanceKey) {
    final io.camunda.zeebe.protocol.record.Record<RecordValue> recordValueRecord =
        FACTORY.generateRecord(ValueType.PROCESS_INSTANCE);
    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withKey(processInstanceKey)
        .withValue(
            ImmutableProcessInstanceRecordValue.builder()
                .from((ProcessInstanceRecordValue) recordValueRecord.getValue())
                .withProcessInstanceKey(processInstanceKey)
                .withBpmnElementType(BpmnElementType.PROCESS)
                .withVersion(1)
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getProcessDefinitionCreatedRecord(
      final Long position) {
    final io.camunda.zeebe.protocol.record.Record<RecordValue> recordValueRecord =
        FACTORY.generateRecord(ValueType.PROCESS);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(ProcessIntent.CREATED)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withPartitionId(1)
        .withValue(
            ImmutableProcess.builder()
                .from((Process) recordValueRecord.getValue())
                .withVersion(1)
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getDecisionRequirementsCreatedRecord(
      final Long position) {
    final io.camunda.zeebe.protocol.record.Record<RecordValue> recordValueRecord =
        FACTORY.generateRecord(ValueType.DECISION_REQUIREMENTS);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(DecisionRequirementsIntent.CREATED)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withPartitionId(1)
        .withValue(
            ImmutableDecisionRequirementsRecordValue.builder()
                .from((DecisionRequirementsRecordValue) recordValueRecord.getValue())
                .withDecisionRequirementsVersion(1)
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getDecisionDefinitionCreatedRecord(
      final Long position) {
    final io.camunda.zeebe.protocol.record.Record<RecordValue> recordValueRecord =
        FACTORY.generateRecord(ValueType.DECISION);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(DecisionIntent.CREATED)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withPartitionId(1)
        .withValue(
            ImmutableDecisionRecordValue.builder()
                .from((ImmutableDecisionRecordValue) recordValueRecord.getValue())
                .withVersion(1)
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getFlowNodeActivatingRecord(final Long position) {
    final io.camunda.zeebe.protocol.record.Record<RecordValue> recordValueRecord =
        FACTORY.generateRecord(ValueType.PROCESS_INSTANCE);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATING)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withPartitionId(1)
        .withValue(
            ImmutableProcessInstanceRecordValue.builder()
                .from((ProcessInstanceRecordValue) recordValueRecord.getValue())
                .withVersion(1)
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getFlowNodeCompletedRecord(
      final Long position, final long elementKey) {
    final io.camunda.zeebe.protocol.record.Record<RecordValue> recordValueRecord =
        FACTORY.generateRecord(ValueType.PROCESS_INSTANCE);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withPartitionId(1)
        .withKey(elementKey)
        .withValue(
            ImmutableProcessInstanceRecordValue.builder()
                .from((ProcessInstanceRecordValue) recordValueRecord.getValue())
                .withVersion(1)
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getUserTaskCreatedRecord(final Long position) {
    final Record<RecordValue> recordValueRecord = FACTORY.generateRecord(ValueType.USER_TASK);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(UserTaskIntent.CREATED)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withPartitionId(1)
        .withValue(
            ImmutableUserTaskRecordValue.builder()
                .from((ImmutableUserTaskRecordValue) recordValueRecord.getValue())
                .withCreationTimestamp(OffsetDateTime.now().toEpochSecond())
                .withDueDate(OffsetDateTime.now().toString())
                .withFollowUpDate(OffsetDateTime.now().toString())
                .withProcessDefinitionVersion(1)
                .withCandidateUsersList(List.of("user1", "user2"))
                .withCandidateGroupsList(List.of("group1", "group2"))
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getFormCreatedRecord(final Long position) {
    final Record<RecordValue> recordValueRecord = FACTORY.generateRecord(ValueType.FORM);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(FormIntent.CREATED)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withPartitionId(1)
        .build();
  }

  protected static ImmutableRecord<RecordValue> getUserRecord(
      final Long userKey, final UserIntent intent) {
    final Record<RecordValue> recordValueRecord = FACTORY.generateRecord(ValueType.USER);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(intent)
        .withPosition(1)
        .withTimestamp(System.currentTimeMillis())
        .withKey(userKey)
        .withValue(
            ImmutableUserRecordValue.builder()
                .from((UserRecordValue) recordValueRecord.getValue())
                .withUserKey(userKey)
                .build())
        .build();
  }

  protected static ImmutableRecord<RecordValue> getMappingRecord(
      final Long position, final MappingIntent intent) {
    final Record<RecordValue> recordValueRecord = FACTORY.generateRecord(ValueType.MAPPING);

    return ImmutableRecord.builder()
        .from(recordValueRecord)
        .withIntent(intent)
        .withPosition(position)
        .withTimestamp(System.currentTimeMillis())
        .withPartitionId(1)
        .build();
  }
}
