/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.camunda.zeebe.engine.state.distribution.DistributionQueue;
import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.AuthorizationIntent;
import io.camunda.zeebe.protocol.record.intent.CommandDistributionIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationOwnerType;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.CommandDistributionRecordValue;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class UpdateAuthorizationMultipartitionTest {

  private static final int PARTITION_COUNT = 3;

  @Rule public final EngineRule engine = EngineRule.multiplePartition(PARTITION_COUNT);
  @Rule public final TestWatcher recordingExporterTestWatcher = new RecordingExporterTestWatcher();

  @Test
  public void shouldTestLifecycle() {
    // when
    final var key =
        engine
            .authorization()
            .newAuthorization()
            // TODO: remove with https://github.com/camunda/camunda/issues/26883
            .withOwnerKey(1L)
            .withOwnerId("ownerId")
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceId("resourceId")
            .withResourceType(AuthorizationResourceType.RESOURCE)
            .withPermissions(PermissionType.CREATE)
            .create()
            .getKey();

    engine.authorization().updateAuthorization(key).update();

    RecordingExporter.commandDistributionRecords(CommandDistributionIntent.FINISHED)
        .withDistributionIntent(AuthorizationIntent.CREATE)
        .await();

    // then
    assertThat(
            RecordingExporter.records()
                .withPartitionId(1)
                .limitByCount(r -> r.getIntent().equals(CommandDistributionIntent.FINISHED), 2)
                .filter(
                    record ->
                        record.getValueType() == ValueType.AUTHORIZATION
                            || (record.getValueType() == ValueType.COMMAND_DISTRIBUTION
                                && ((CommandDistributionRecordValue) record.getValue()).getIntent()
                                    == AuthorizationIntent.UPDATE)))
        .extracting(
            io.camunda.zeebe.protocol.record.Record::getIntent,
            io.camunda.zeebe.protocol.record.Record::getRecordType,
            r ->
                // We want to verify the partition id where the deletion was distributing to and
                // where it was completed. Since only the CommandDistribution records have a
                // value that contains the partition id, we use the partition id the record was
                // written on for the other records.
                r.getValue() instanceof CommandDistributionRecordValue
                    ? ((CommandDistributionRecordValue) r.getValue()).getPartitionId()
                    : r.getPartitionId())
        .containsSubsequence(
            tuple(AuthorizationIntent.UPDATE, RecordType.COMMAND, 1),
            tuple(AuthorizationIntent.UPDATED, RecordType.EVENT, 1),
            tuple(CommandDistributionIntent.STARTED, RecordType.EVENT, 1))
        .containsSubsequence(
            tuple(CommandDistributionIntent.DISTRIBUTING, RecordType.EVENT, 2),
            tuple(CommandDistributionIntent.ACKNOWLEDGE, RecordType.COMMAND, 2),
            tuple(CommandDistributionIntent.ACKNOWLEDGED, RecordType.EVENT, 2))
        .containsSubsequence(
            tuple(CommandDistributionIntent.DISTRIBUTING, RecordType.EVENT, 3),
            tuple(CommandDistributionIntent.ACKNOWLEDGE, RecordType.COMMAND, 3),
            tuple(CommandDistributionIntent.ACKNOWLEDGED, RecordType.EVENT, 3))
        .endsWith(tuple(CommandDistributionIntent.FINISHED, RecordType.EVENT, 1));

    for (int partitionId = 2; partitionId < PARTITION_COUNT; partitionId++) {
      assertThat(
              RecordingExporter.records()
                  .withPartitionId(partitionId)
                  .limit(r -> r.getIntent().equals(AuthorizationIntent.UPDATED))
                  .collect(Collectors.toList()))
          .extracting(Record::getIntent)
          .endsWith(AuthorizationIntent.UPDATE, AuthorizationIntent.UPDATED);
    }
  }

  @Test
  public void shouldDistributeInIdentityQueue() {
    // when
    final var key =
        engine
            .authorization()
            .newAuthorization()
            // TODO: remove with https://github.com/camunda/camunda/issues/26883
            .withOwnerKey(1L)
            .withOwnerId("ownerId")
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceId("resourceId")
            .withResourceType(AuthorizationResourceType.RESOURCE)
            .withPermissions(PermissionType.CREATE)
            .create()
            .getKey();

    engine.authorization().updateAuthorization(key).update();

    // then
    assertThat(
            RecordingExporter.commandDistributionRecords()
                .limitByCount(r -> r.getIntent().equals(CommandDistributionIntent.FINISHED), 2)
                .withIntent(CommandDistributionIntent.ENQUEUED))
        .extracting(r -> r.getValue().getQueueId())
        .containsOnly(DistributionQueue.IDENTITY.getQueueId());
  }

  @Test
  public void distributionShouldNotOvertakeOtherCommandsInSameQueue() {
    // given the user creation distribution is intercepted
    for (int partitionId = 2; partitionId <= PARTITION_COUNT; partitionId++) {
      interceptAuthorizationCreateForPartition(partitionId);
    }
    final var key =
        engine
            .authorization()
            .newAuthorization()
            // TODO: remove with https://github.com/camunda/camunda/issues/26883
            .withOwnerKey(1L)
            .withOwnerId("ownerId")
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceId("resourceId")
            .withResourceType(AuthorizationResourceType.RESOURCE)
            .withPermissions(PermissionType.CREATE)
            .create()
            .getKey();

    engine.authorization().updateAuthorization(key).update();

    // Increase time to trigger a redistribution
    engine.increaseTime(Duration.ofMinutes(1));

    // then
    assertThat(
            RecordingExporter.commandDistributionRecords(CommandDistributionIntent.FINISHED)
                .limit(2))
        .extracting(r -> r.getValue().getValueType(), r -> r.getValue().getIntent())
        .containsExactly(
            tuple(ValueType.AUTHORIZATION, AuthorizationIntent.CREATE),
            tuple(ValueType.AUTHORIZATION, AuthorizationIntent.UPDATE));
  }

  private void interceptAuthorizationCreateForPartition(final int partitionId) {
    final var hasInterceptedPartition = new AtomicBoolean(false);
    engine.interceptInterPartitionCommands(
        (receiverPartitionId, valueType, intent, recordKey, command) -> {
          if (hasInterceptedPartition.get()) {
            return true;
          }
          hasInterceptedPartition.set(true);
          return !(receiverPartitionId == partitionId && intent == AuthorizationIntent.CREATE);
        });
  }
}
