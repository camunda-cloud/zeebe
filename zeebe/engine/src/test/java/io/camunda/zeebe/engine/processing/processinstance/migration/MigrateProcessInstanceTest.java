/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.processinstance.migration;

import static io.camunda.zeebe.engine.processing.processinstance.migration.MigrationTestUtil.extractProcessDefinitionKeyByProcessId;
import static io.camunda.zeebe.protocol.record.Assertions.assertThat;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceMigrationMappingInstruction;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceMigrationIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.test.util.BrokerClassRuleHelper;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class MigrateProcessInstanceTest {

  @ClassRule public static final EngineRule ENGINE = EngineRule.singlePartition();

  @Rule public final TestWatcher watcher = new RecordingExporterTestWatcher();
  @Rule public final BrokerClassRuleHelper helper = new BrokerClassRuleHelper();

  @Test
  public void shouldWriteMigratedEventForProcessInstance() {
    // given
    final String processId1 = helper.getBpmnProcessId();
    final String processId2 = helper.getBpmnProcessId() + "2";

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId1)
                    .startEvent()
                    .serviceTask("A", a -> a.zeebeJobType("A"))
                    .endEvent()
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(processId2)
                    .startEvent()
                    .serviceTask("B", a -> a.zeebeJobType("B"))
                    .endEvent()
                    .done())
            .deploy();
    final long otherProcessDefinitionKey =
        extractProcessDefinitionKeyByProcessId(deployment, processId2);

    final var processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(processId1).create();

    // when
    final var event =
        ENGINE
            .processInstance()
            .withInstanceKey(processInstanceKey)
            .migration()
            .withTargetProcessDefinitionKey(otherProcessDefinitionKey)
            .addMappingInstruction("A", "B")
            .migrate();

    // then
    assertThat(event)
        .hasKey(processInstanceKey)
        .hasRecordType(RecordType.EVENT)
        .hasIntent(ProcessInstanceMigrationIntent.MIGRATED);

    assertThat(event.getValue())
        .hasProcessInstanceKey(processInstanceKey)
        .hasTargetProcessDefinitionKey(otherProcessDefinitionKey)
        .hasMappingInstructions(
            new ProcessInstanceMigrationMappingInstruction()
                .setSourceElementId("A")
                .setTargetElementId("B"));
  }

  @Test
  public void shouldWriteElementMigratedEventForProcessInstance() {
    // given
    final String processId = helper.getBpmnProcessId();
    final String otherProcessId = helper.getBpmnProcessId() + "2";

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId)
                    .startEvent()
                    .serviceTask("A", a -> a.zeebeJobType("A"))
                    .endEvent()
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(otherProcessId)
                    .startEvent()
                    .serviceTask("B", a -> a.zeebeJobType("B"))
                    .endEvent()
                    .done())
            .deploy();
    final long otherProcessDefinitionKey =
        extractProcessDefinitionKeyByProcessId(deployment, otherProcessId);

    final var processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(processId).create();

    // when
    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(otherProcessDefinitionKey)
        .addMappingInstruction("A", "B")
        .migrate();

    // then
    assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .withElementType(BpmnElementType.PROCESS)
                .getFirst()
                .getValue())
        .describedAs("Expect that process definition key changed")
        .hasProcessDefinitionKey(otherProcessDefinitionKey)
        .describedAs("Expect that bpmn process id and element id changed")
        .hasBpmnProcessId(otherProcessId)
        .hasElementId(otherProcessId)
        .describedAs("Expect that version number did not change")
        .hasVersion(1);
  }

  @Test
  public void shouldWriteElementMigratedEventForProcessInstanceToNewVersion() {
    // given
    final String processId = helper.getBpmnProcessId();

    ENGINE
        .deployment()
        .withXmlResource(
            Bpmn.createExecutableProcess(processId)
                .startEvent()
                .serviceTask("A", a -> a.zeebeJobType("A"))
                .endEvent()
                .done())
        .deploy();
    final var secondVersionDeployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId)
                    .startEvent()
                    .serviceTask("A", a -> a.zeebeJobType("A"))
                    .userTask()
                    .endEvent()
                    .done())
            .deploy();

    final long v2ProcessDefinitionKey =
        extractProcessDefinitionKeyByProcessId(secondVersionDeployment, processId);

    final var processInstanceKey =
        ENGINE.processInstance().ofBpmnProcessId(processId).withVersion(1).create();

    // when
    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(v2ProcessDefinitionKey)
        .addMappingInstruction("A", "A")
        .migrate();

    // then
    assertThat(
            RecordingExporter.processInstanceRecords()
                .withProcessInstanceKey(processInstanceKey)
                .withElementType(BpmnElementType.PROCESS)
                .onlyEvents()
                .withIntent(ProcessInstanceIntent.ELEMENT_MIGRATED)
                .getFirst()
                .getValue())
        .describedAs("Expect that process definition key changed")
        .hasProcessDefinitionKey(v2ProcessDefinitionKey)
        .describedAs("Expect that version number changed")
        .hasVersion(2)
        .describedAs("Expect that bpmn process id and element id did not change")
        .hasBpmnProcessId(processId)
        .hasElementId(processId);
  }

  @Test
  public void shouldAdjustMessageCardinalityTrackingWhenMigratedForProcessInstance() {
    final String processId = helper.getBpmnProcessId();
    final String targetProcessId = helper.getBpmnProcessId() + "2";

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId)
                    .startEvent("msg_start")
                    .message("msg1")
                    .serviceTask("A", t -> t.zeebeJobType("A"))
                    .endEvent()
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(targetProcessId)
                    .startEvent("msg_start")
                    .message("msg2")
                    .serviceTask("B", t -> t.zeebeJobType("B"))
                    .endEvent()
                    .done())
            .deploy();

    ENGINE.message().withName("msg1").withCorrelationKey("cardinality").publish();
    final var processInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withElementType(BpmnElementType.START_EVENT)
            .withElementId("msg_start")
            .withBpmnProcessId(processId)
            .getFirst()
            .getValue()
            .getProcessInstanceKey();

    // when
    final long targetProcessDefinitionKey =
        extractProcessDefinitionKeyByProcessId(deployment, targetProcessId);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("A", "B")
        .migrate();

    // then
    ENGINE.message().withName("msg1").withCorrelationKey("cardinality").publish();

    Assertions.assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
                .withElementType(BpmnElementType.PROCESS)
                .withBpmnProcessId(processId)
                .withElementId(processId)
                .skip(1) // skip the first activation
                .exists())
        .isTrue();
  }

  @Test
  public void shouldAdjustMessageCardinalityTrackingWhenMigratedForTargetProcess() {
    final String processId = helper.getBpmnProcessId();
    final String targetProcessId = helper.getBpmnProcessId() + "2";

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId)
                    .startEvent("msg_start")
                    .message("msg1")
                    .serviceTask("A", t -> t.zeebeJobType("A"))
                    .endEvent()
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(targetProcessId)
                    .startEvent("msg_start")
                    .message("msg2")
                    .serviceTask("B", t -> t.zeebeJobType("B"))
                    .endEvent()
                    .done())
            .deploy();

    ENGINE.message().withName("msg1").withCorrelationKey("cardinality").publish();
    final var processInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withElementType(BpmnElementType.START_EVENT)
            .withElementId("msg_start")
            .withBpmnProcessId(processId)
            .getFirst()
            .getValue()
            .getProcessInstanceKey();

    // when
    final long targetProcessDefinitionKey =
        extractProcessDefinitionKeyByProcessId(deployment, targetProcessId);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("A", "B")
        .migrate();

    // then
    final long messagePosition =
        ENGINE
            .message()
            .withName("msg2")
            .withCorrelationKey("cardinality")
            .publish()
            .getSourceRecordPosition();

    // broadcast a record to have something to limit the stream on
    final var limitPosition = ENGINE.signal().withSignalName("dummy").broadcast().getPosition();
    Assertions.assertThat(
            RecordingExporter.records()
                .between(messagePosition, limitPosition)
                .processInstanceRecords()
                .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATED))
        .extracting(r -> r.getValue().getElementId())
        .describedAs("Expect that the target process is not activated")
        .doesNotContain(targetProcessId);

    // complete existing target instance
    ENGINE.job().ofInstance(processInstanceKey).withType("A").complete();

    // start new target instance
    ENGINE.message().withName("msg2").withCorrelationKey("cardinality").publish();

    Assertions.assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
                .withElementType(BpmnElementType.PROCESS)
                .withBpmnProcessId(targetProcessId)
                .exists())
        .isTrue();
  }

  @Test
  public void shouldAdjustMessageCardinalityTrackingWhenMigratedForTargetProcessWithMessageTTL() {
    final String processId = helper.getBpmnProcessId();
    final String targetProcessId = helper.getBpmnProcessId() + "2";

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId)
                    .startEvent("msg_start")
                    .message("msg1")
                    .serviceTask("A", t -> t.zeebeJobType("A"))
                    .endEvent()
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(targetProcessId)
                    .startEvent("msg_start")
                    .message("msg2")
                    .serviceTask("B", t -> t.zeebeJobType("B"))
                    .endEvent()
                    .done())
            .deploy();

    ENGINE.message().withName("msg1").withCorrelationKey("cardinality").publish();
    final var processInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withElementType(BpmnElementType.START_EVENT)
            .withElementId("msg_start")
            .withBpmnProcessId(processId)
            .getFirst()
            .getValue()
            .getProcessInstanceKey();

    // when
    final long targetProcessDefinitionKey =
        extractProcessDefinitionKeyByProcessId(deployment, targetProcessId);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("A", "B")
        .migrate();

    // then
    final long messagePosition =
        ENGINE
            .message()
            .withName("msg2")
            .withCorrelationKey("cardinality")
            .withTimeToLive(Duration.ofMinutes(1)) // create message with TTL
            .publish()
            .getSourceRecordPosition();

    // broadcast a record to have something to limit the stream on
    final var limitPosition = ENGINE.signal().withSignalName("dummy").broadcast().getPosition();
    Assertions.assertThat(
            RecordingExporter.records()
                .between(messagePosition, limitPosition)
                .processInstanceRecords()
                .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATED))
        .extracting(r -> r.getValue().getElementId())
        .describedAs("Expect that the target process is not activated")
        .doesNotContain(targetProcessId);

    // complete existing target instance
    ENGINE.job().ofInstance(processInstanceKey).withType("A").complete();

    Assertions.assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
                .withElementType(BpmnElementType.PROCESS)
                .withBpmnProcessId(targetProcessId)
                .withElementId(targetProcessId)
                .exists())
        .describedAs("Expect that the target process is activated for the message with TTL")
        .isTrue();
  }

  @Test
  public void shouldMigrateWithSameProcessIdWhenProcessInstanceIsCreatedWithMessageStartEvent() {
    // given
    final String processId1 = helper.getBpmnProcessId();
    final String correlationKey = helper.getCorrelationValue();

    ENGINE
        .deployment()
        .withXmlResource(
            Bpmn.createExecutableProcess(processId1)
                .startEvent("msg_start1")
                .message("msg1")
                .serviceTask("task1", t -> t.zeebeJobType("task"))
                .done())
        .deploy();
    ENGINE.message().withName("msg1").withCorrelationKey(correlationKey).publish();
    final var processInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withElementType(BpmnElementType.START_EVENT)
            .withElementId("msg_start1")
            .withBpmnProcessId(processId1)
            .getFirst()
            .getValue()
            .getProcessInstanceKey();

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId1)
                    .startEvent("msg_start")
                    .message("msg2")
                    .serviceTask("task2", t -> t.zeebeJobType("task"))
                    .done())
            .deploy();
    final long targetProcessDefinitionKey =
        MigrationTestUtil.extractProcessDefinitionKeyByProcessId(deployment, processId1);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("task1", "task2")
        .migrate();

    // then
    assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .withElementType(BpmnElementType.PROCESS)
                .getFirst()
                .getValue())
        .describedAs("Expect that process definition key changed")
        .hasProcessDefinitionKey(targetProcessDefinitionKey)
        .describedAs("Expect that bpmn process id and element id is the same")
        .hasBpmnProcessId(processId1)
        .hasElementId(processId1)
        .describedAs("Expect that version number is changed")
        .hasVersion(2);
  }

  @Test
  public void shouldMigrateWhenTargetHasNoMessageStartEvent() {
    // given
    final String processId1 = helper.getBpmnProcessId();
    final String processId2 = helper.getBpmnProcessId() + "2";
    final String correlationKey = helper.getCorrelationValue();

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId1)
                    .startEvent("msg_start")
                    .message("msg")
                    .serviceTask("task1", t -> t.zeebeJobType("task"))
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(processId2)
                    .startEvent("start")
                    .serviceTask("task2", t -> t.zeebeJobType("task"))
                    .done())
            .deploy();
    ENGINE.message().withName("msg").withCorrelationKey(correlationKey).publish();
    final var processInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withElementType(BpmnElementType.START_EVENT)
            .withElementId("msg_start")
            .withBpmnProcessId(processId1)
            .getFirst()
            .getValue()
            .getProcessInstanceKey();

    final long targetProcessDefinitionKey =
        MigrationTestUtil.extractProcessDefinitionKeyByProcessId(deployment, processId2);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("task1", "task2")
        .migrate();

    // then
    assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .withElementType(BpmnElementType.PROCESS)
                .getFirst()
                .getValue())
        .describedAs("Expect that process definition key changed")
        .hasProcessDefinitionKey(targetProcessDefinitionKey)
        .describedAs("Expect that bpmn process id and element id changed")
        .hasBpmnProcessId(processId2)
        .hasElementId(processId2)
        .describedAs("Expect that version number did not change")
        .hasVersion(1);
  }

  @Test
  public void shouldMigrateWhenSourceHasNoMessageStartEvent() {
    // given
    final String processId1 = helper.getBpmnProcessId();
    final String processId2 = helper.getBpmnProcessId() + "2";
    final String correlationKey = helper.getCorrelationValue();

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId1)
                    .startEvent("start")
                    .serviceTask("task1", t -> t.zeebeJobType("task"))
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(processId2)
                    .startEvent("msg_start")
                    .message("msg")
                    .serviceTask("task2", t -> t.zeebeJobType("task"))
                    .done())
            .deploy();

    final var processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(processId1).create();

    ENGINE.message().withName("msg").withCorrelationKey(correlationKey).publish();
    RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
        .withElementType(BpmnElementType.START_EVENT)
        .withElementId("msg_start")
        .withBpmnProcessId(processId2)
        .getFirst()
        .getValue()
        .getProcessInstanceKey();

    final long targetProcessDefinitionKey =
        MigrationTestUtil.extractProcessDefinitionKeyByProcessId(deployment, processId2);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("task1", "task2")
        .migrate();

    // then
    assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .withElementType(BpmnElementType.PROCESS)
                .getFirst()
                .getValue())
        .describedAs("Expect that process definition key changed")
        .hasProcessDefinitionKey(targetProcessDefinitionKey)
        .describedAs("Expect that bpmn process id and element id changed")
        .hasBpmnProcessId(processId2)
        .hasElementId(processId2)
        .describedAs("Expect that version number did not change")
        .hasVersion(1);
  }

  @Test
  public void shouldMigrateWhenSourceProcessInstanceIsCreatedWithoutCorrelationKey() {
    // given
    final String processId1 = helper.getBpmnProcessId();
    final String processId2 = helper.getBpmnProcessId() + "2";
    final String correlationKey = helper.getCorrelationValue();

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId1)
                    .startEvent("msg_start")
                    .message("msg1")
                    .serviceTask("task1", t -> t.zeebeJobType("task"))
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(processId2)
                    .startEvent("msg_start")
                    .message("msg2")
                    .serviceTask("task2", t -> t.zeebeJobType("task"))
                    .done())
            .deploy();
    ENGINE.message().withName("msg1").withCorrelationKey("").publish();
    ENGINE.message().withName("msg2").withCorrelationKey(correlationKey).publish();
    final var processInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withElementType(BpmnElementType.START_EVENT)
            .withElementId("msg_start")
            .withBpmnProcessId(processId1)
            .getFirst()
            .getValue()
            .getProcessInstanceKey();

    RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
        .withElementType(BpmnElementType.START_EVENT)
        .withElementId("msg_start")
        .withBpmnProcessId(processId2)
        .getFirst()
        .getValue()
        .getProcessInstanceKey();

    final long targetProcessDefinitionKey =
        MigrationTestUtil.extractProcessDefinitionKeyByProcessId(deployment, processId2);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("task1", "task2")
        .migrate();

    // then
    assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .withElementType(BpmnElementType.PROCESS)
                .getFirst()
                .getValue())
        .describedAs("Expect that process definition key changed")
        .hasProcessDefinitionKey(targetProcessDefinitionKey)
        .describedAs("Expect that bpmn process id and element id changed")
        .hasBpmnProcessId(processId2)
        .hasElementId(processId2)
        .describedAs("Expect that version number did not change")
        .hasVersion(1);
  }

  @Test
  public void shouldMigrateWhenTargetProcessInstanceIsCreatedWithoutCorrelationKey() {
    // given
    final String processId1 = helper.getBpmnProcessId();
    final String processId2 = helper.getBpmnProcessId() + "2";
    final String correlationKey = helper.getCorrelationValue();

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId1)
                    .startEvent("msg_start")
                    .message("msg1")
                    .serviceTask("task1", t -> t.zeebeJobType("task"))
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(processId2)
                    .startEvent("msg_start")
                    .message("msg2")
                    .serviceTask("task2", t -> t.zeebeJobType("task"))
                    .done())
            .deploy();
    ENGINE.message().withName("msg1").withCorrelationKey(correlationKey).publish();
    ENGINE.message().withName("msg2").withCorrelationKey("").publish();
    final var processInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withElementType(BpmnElementType.START_EVENT)
            .withElementId("msg_start")
            .withBpmnProcessId(processId1)
            .getFirst()
            .getValue()
            .getProcessInstanceKey();

    RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
        .withElementType(BpmnElementType.START_EVENT)
        .withElementId("msg_start")
        .withBpmnProcessId(processId2)
        .getFirst()
        .getValue()
        .getProcessInstanceKey();

    final long targetProcessDefinitionKey =
        MigrationTestUtil.extractProcessDefinitionKeyByProcessId(deployment, processId2);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("task1", "task2")
        .migrate();

    // then
    assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .withElementType(BpmnElementType.PROCESS)
                .getFirst()
                .getValue())
        .describedAs("Expect that process definition key changed")
        .hasProcessDefinitionKey(targetProcessDefinitionKey)
        .describedAs("Expect that bpmn process id and element id changed")
        .hasBpmnProcessId(processId2)
        .hasElementId(processId2)
        .describedAs("Expect that version number did not change")
        .hasVersion(1);
  }

  @Test
  public void shouldMigrateWhenTargetProcessInstanceIsCreatedWithDifferentCorrelationKey() {
    // given
    final String processId1 = helper.getBpmnProcessId();
    final String processId2 = helper.getBpmnProcessId() + "2";
    final String correlationKey1 = helper.getCorrelationValue() + "1";
    final String correlationKey2 = helper.getCorrelationValue() + "2";

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess(processId1)
                    .startEvent("msg_start")
                    .message("msg1")
                    .serviceTask("task1", t -> t.zeebeJobType("task"))
                    .done())
            .withXmlResource(
                Bpmn.createExecutableProcess(processId2)
                    .startEvent("msg_start")
                    .message("msg2")
                    .serviceTask("task2", t -> t.zeebeJobType("task"))
                    .done())
            .deploy();
    ENGINE.message().withName("msg1").withCorrelationKey(correlationKey1).publish();
    ENGINE.message().withName("msg2").withCorrelationKey(correlationKey2).publish();
    final var processInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withElementType(BpmnElementType.START_EVENT)
            .withElementId("msg_start")
            .withBpmnProcessId(processId1)
            .getFirst()
            .getValue()
            .getProcessInstanceKey();

    RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
        .withElementType(BpmnElementType.START_EVENT)
        .withElementId("msg_start")
        .withBpmnProcessId(processId2)
        .getFirst()
        .getValue()
        .getProcessInstanceKey();

    final long targetProcessDefinitionKey =
        MigrationTestUtil.extractProcessDefinitionKeyByProcessId(deployment, processId2);

    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcessDefinitionKey)
        .addMappingInstruction("task1", "task2")
        .migrate();

    // then
    assertThat(
            RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .withElementType(BpmnElementType.PROCESS)
                .getFirst()
                .getValue())
        .describedAs("Expect that process definition key changed")
        .hasProcessDefinitionKey(targetProcessDefinitionKey)
        .describedAs("Expect that bpmn process id and element id changed")
        .hasBpmnProcessId(processId2)
        .hasElementId(processId2)
        .describedAs("Expect that version number did not change")
        .hasVersion(1);
  }
}
