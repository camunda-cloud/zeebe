/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.identity;

import static io.camunda.zeebe.engine.processing.identity.PermissionsBehavior.AUTHORIZATION_DOES_NOT_EXIST_ERROR_MESSAGE_UPDATE;

import io.camunda.zeebe.engine.processing.distribution.CommandDistributionBehavior;
import io.camunda.zeebe.engine.processing.streamprocessor.DistributedTypedRecordProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedRejectionWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedResponseWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.engine.state.authorization.PersistedAuthorization;
import io.camunda.zeebe.engine.state.distribution.DistributionQueue;
import io.camunda.zeebe.engine.state.immutable.ProcessingState;
import io.camunda.zeebe.protocol.impl.record.value.authorization.AuthorizationRecord;
import io.camunda.zeebe.protocol.record.intent.AuthorizationIntent;
import io.camunda.zeebe.stream.api.records.TypedRecord;
import io.camunda.zeebe.stream.api.state.KeyGenerator;

public class AuthorizationUpdateProcessor
    implements DistributedTypedRecordProcessor<AuthorizationRecord> {

  private final KeyGenerator keyGenerator;
  private final CommandDistributionBehavior distributionBehavior;
  private final StateWriter stateWriter;
  private final TypedResponseWriter responseWriter;
  private final TypedRejectionWriter rejectionWriter;
  private final PermissionsBehavior permissionsBehavior;

  public AuthorizationUpdateProcessor(
      final Writers writers,
      final KeyGenerator keyGenerator,
      final ProcessingState processingState,
      final CommandDistributionBehavior distributionBehavior,
      final AuthorizationCheckBehavior authCheckBehavior) {
    this.keyGenerator = keyGenerator;
    this.distributionBehavior = distributionBehavior;
    stateWriter = writers.state();
    responseWriter = writers.response();
    rejectionWriter = writers.rejection();
    permissionsBehavior = new PermissionsBehavior(processingState, authCheckBehavior);
  }

  @Override
  public void processNewCommand(final TypedRecord<AuthorizationRecord> command) {
    permissionsBehavior
        .isAuthorized(command)
        .flatMap(
            authorizationRecord ->
                permissionsBehavior.authorizationExists(
                    authorizationRecord, AUTHORIZATION_DOES_NOT_EXIST_ERROR_MESSAGE_UPDATE))
        .map(
            persistedAuthorization ->
                overlayAuthorization(command.getValue(), persistedAuthorization))
        .flatMap(
            record ->
                permissionsBehavior.hasValidPermissionTypes(
                    command.getValue(),
                    command.getValue().getAuthorizationPermissions(),
                    record.getResourceType(),
                    "Expected to update authorization with permission types '%s' and resource type '%s', but these permissions are not supported. Supported permission types are: '%s'"))
        .ifRightOrLeft(
            authorizationRecord -> writeEventAndDistribute(command, authorizationRecord),
            (rejection) -> {
              rejectionWriter.appendRejection(command, rejection.type(), rejection.reason());
              responseWriter.writeRejectionOnCommand(command, rejection.type(), rejection.reason());
            });
  }

  @Override
  public void processDistributedCommand(final TypedRecord<AuthorizationRecord> command) {
    permissionsBehavior
        .authorizationExists(command.getValue(), AUTHORIZATION_DOES_NOT_EXIST_ERROR_MESSAGE_UPDATE)
        .ifRightOrLeft(
            ignored ->
                stateWriter.appendFollowUpEvent(
                    command.getKey(), AuthorizationIntent.UPDATED, command.getValue()),
            rejection ->
                rejectionWriter.appendRejection(command, rejection.type(), rejection.reason()));

    distributionBehavior.acknowledgeCommand(command);
  }

  private void writeEventAndDistribute(
      final TypedRecord<AuthorizationRecord> command,
      final AuthorizationRecord authorizationRecord) {
    final long key = keyGenerator.nextKey();
    stateWriter.appendFollowUpEvent(key, AuthorizationIntent.UPDATED, authorizationRecord);
    distributionBehavior
        .withKey(key)
        .inQueue(DistributionQueue.IDENTITY.getQueueId())
        .distribute(command);
    responseWriter.writeEventOnCommand(
        key, AuthorizationIntent.UPDATED, authorizationRecord, command);
  }

  // Create the new record with the persisted values if not changed by the user
  private AuthorizationRecord overlayAuthorization(
      final AuthorizationRecord newAuthorization,
      final PersistedAuthorization persistedAuthorization) {
    final var changeset = newAuthorization.getChangedAttributes();
    if (!changeset.contains(AuthorizationRecord.OWNER_ID)) {
      newAuthorization.setOwnerId(persistedAuthorization.getOwnerId());
    }
    if (!changeset.contains(AuthorizationRecord.OWNER_TYPE)) {
      newAuthorization.setOwnerType(persistedAuthorization.getOwnerType());
    }
    if (!changeset.contains(AuthorizationRecord.RESOURCE_ID)) {
      newAuthorization.setResourceId(persistedAuthorization.getResourceId());
    }
    if (!changeset.contains(AuthorizationRecord.RESOURCE_TYPE)) {
      newAuthorization.setResourceType(persistedAuthorization.getResourceType());
    }
    if (!changeset.contains(AuthorizationRecord.PERMISSIONS)) {
      newAuthorization.setAuthorizationPermissions(persistedAuthorization.getPermissions());
    }
    return newAuthorization;
  }
}
