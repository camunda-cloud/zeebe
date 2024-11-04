/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.camunda.exporter.cache.CachedProcessEntity;
import io.camunda.exporter.cache.TestProcessCache;
import io.camunda.exporter.store.BatchRequest;
import io.camunda.exporter.utils.XMLUtil;
import io.camunda.webapps.schema.entities.operate.ProcessEntity;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.ProcessIntent;
import io.camunda.zeebe.protocol.record.value.deployment.ImmutableProcess;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import io.camunda.zeebe.test.broker.protocol.ProtocolFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ProcessHandlerTest {
  private final ProtocolFactory factory = new ProtocolFactory();
  private final String indexName = "test-process";
  private final TestProcessCache processCache = new TestProcessCache();
  private final ProcessHandler underTest =
      new ProcessHandler(indexName, new XMLUtil(), processCache);

  @Test
  void testGetHandledValueType() {
    assertThat(underTest.getHandledValueType()).isEqualTo(ValueType.PROCESS);
  }

  @Test
  void testGetEntityType() {
    assertThat(underTest.getEntityType()).isEqualTo(ProcessEntity.class);
  }

  @Test
  void shouldHandleRecord() {
    // given
    final Record<Process> processRecord =
        factory.generateRecord(ValueType.PROCESS, r -> r.withIntent(ProcessIntent.CREATED));

    // when - then
    assertThat(underTest.handlesRecord(processRecord)).isTrue();
  }

  @Test
  void shouldGenerateIds() {
    // given
    final long expectedId = 123;
    final Process processRecordValue =
        ImmutableProcess.builder()
            .from(factory.generateObject(ImmutableProcess.class))
            .withProcessDefinitionKey(expectedId)
            .build();

    final Record<Process> processRecord =
        factory.generateRecord(
            ValueType.PROCESS,
            r -> r.withIntent(ProcessIntent.CREATED).withValue(processRecordValue));

    // when
    final var idList = underTest.generateIds(processRecord);

    // then
    assertThat(idList).containsExactly(String.valueOf(expectedId));
  }

  @Test
  void shouldCreateNewEntity() {
    // when
    final long expectedId = 123;
    final var result = underTest.createNewEntity(String.valueOf(expectedId));

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(String.valueOf(expectedId));
    assertThat(result.getKey()).isEqualTo(expectedId);
  }

  @Test
  void shouldAddEntityOnFlush() {
    // given
    final ProcessEntity inputEntity = new ProcessEntity().setId("111");
    final BatchRequest mockRequest = mock(BatchRequest.class);

    // when
    underTest.flush(inputEntity, mockRequest);

    // then
    verify(mockRequest, times(1)).add(indexName, inputEntity);
  }

  @Test
  void shouldUpdateEntityFromRecord() throws IOException {
    // given

    final long expectedId = 123;
    final var resource = getClass().getClassLoader().getResource("process/test-process.bpmn");
    assertThat(resource).isNotNull();
    final ImmutableProcess processRecordValue =
        ImmutableProcess.builder()
            .from(factory.generateObject(ImmutableProcess.class))
            .withProcessDefinitionKey(expectedId)
            .withBpmnProcessId("testProcessId")
            .withResource(Files.readAllBytes(Path.of(resource.getPath())))
            .build();

    final Record<Process> processRecord =
        factory.generateRecord(
            ValueType.DECISION,
            r -> r.withIntent(ProcessIntent.CREATED).withValue(processRecordValue));

    // when
    final ProcessEntity processEntity = new ProcessEntity();
    underTest.updateEntity(processRecord, processEntity);

    // then
    assertThat(processEntity.getId()).isEqualTo(String.valueOf(expectedId));
    assertThat(processEntity.getKey()).isEqualTo(expectedId);
    assertThat(processEntity.getName()).isEqualTo("testProcessName");
    assertThat(processEntity.getBpmnProcessId()).isEqualTo("testProcessId");
    assertThat(processEntity.getVersionTag()).isEqualTo("processTag");
    assertThat(processEntity.getVersion()).isEqualTo(processRecordValue.getVersion());
    assertThat(processEntity.getResourceName()).isEqualTo(processRecordValue.getResourceName());
    assertThat(processEntity.getBpmnXml())
        .isEqualTo(new String(processRecordValue.getResource(), StandardCharsets.UTF_8));
    assertThat(processEntity.getTenantId()).isEqualTo(processRecordValue.getTenantId());
    assertThat(processEntity.getIsPublic()).isFalse();
    assertThat(processEntity.getFormId()).isNull();
  }

  @Test
  void shouldUpdateEntityFromRecordWithForm() throws IOException {
    // given

    final long expectedId = 123;
    final var resource = getClass().getClassLoader().getResource("process/form-process.bpmn");
    assertThat(resource).isNotNull();
    final ImmutableProcess processRecordValue =
        ImmutableProcess.builder()
            .from(factory.generateObject(ImmutableProcess.class))
            .withProcessDefinitionKey(expectedId)
            .withBpmnProcessId("testProcessId")
            .withResource(Files.readAllBytes(Path.of(resource.getPath())))
            .build();

    final Record<Process> processRecord =
        factory.generateRecord(
            ValueType.DECISION,
            r -> r.withIntent(ProcessIntent.CREATED).withValue(processRecordValue));

    // when
    final ProcessEntity processEntity = new ProcessEntity();
    underTest.updateEntity(processRecord, processEntity);

    // then
    assertThat(processEntity.getId()).isEqualTo(String.valueOf(expectedId));
    assertThat(processEntity.getKey()).isEqualTo(expectedId);
    assertThat(processEntity.getName()).isEqualTo("testProcessName");
    assertThat(processEntity.getBpmnProcessId()).isEqualTo("testProcessId");
    assertThat(processEntity.getVersionTag()).isEqualTo("processTag");
    assertThat(processEntity.getVersion()).isEqualTo(processRecordValue.getVersion());
    assertThat(processEntity.getResourceName()).isEqualTo(processRecordValue.getResourceName());
    assertThat(processEntity.getBpmnXml())
        .isEqualTo(new String(processRecordValue.getResource(), StandardCharsets.UTF_8));
    assertThat(processEntity.getTenantId()).isEqualTo(processRecordValue.getTenantId());
    assertThat(processEntity.getIsPublic()).isTrue();
    assertThat(processEntity.getFormId()).isNotNull();
  }

  @Test
  void shouldUpdateProcessCache() throws IOException {
    // given
    final long expectedId = 123;
    final var resource = getClass().getClassLoader().getResource("process/test-process.bpmn");
    assertThat(resource).isNotNull();
    final ImmutableProcess processRecordValue =
        ImmutableProcess.builder()
            .from(factory.generateObject(ImmutableProcess.class))
            .withProcessDefinitionKey(expectedId)
            .withBpmnProcessId("testProcessId")
            .withResource(Files.readAllBytes(Path.of(resource.getPath())))
            .build();

    final Record<Process> processRecord =
        factory.generateRecord(
            ValueType.DECISION,
            r -> r.withIntent(ProcessIntent.CREATED).withValue(processRecordValue));

    // when
    final ProcessEntity processEntity = new ProcessEntity();
    underTest.updateEntity(processRecord, processEntity);

    // then
    assertThat(processCache.get(processRecord.getValue().getProcessDefinitionKey()))
        .isPresent()
        .get()
        .extracting(CachedProcessEntity::name, CachedProcessEntity::versionTag)
        .containsExactly("testProcessName", "processTag");
  }
}
