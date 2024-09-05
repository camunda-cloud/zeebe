/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.identity;

import io.camunda.zeebe.engine.processing.distribution.CommandDistributionBehavior;
import io.camunda.zeebe.engine.processing.streamprocessor.DistributedTypedRecordProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedRejectionWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedResponseWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.engine.state.immutable.AuthorizationState;
import io.camunda.zeebe.engine.state.immutable.ProcessingState;
import io.camunda.zeebe.protocol.impl.record.value.authorization.AuthorizationRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.AuthorizationIntent;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.stream.api.records.TypedRecord;
import io.camunda.zeebe.stream.api.state.KeyGenerator;

public class AuthorizationCreateProcessor
    implements DistributedTypedRecordProcessor<AuthorizationRecord> {
  private final AuthorizationState authorizationState;
  private final StateWriter stateWriter;
  private final KeyGenerator keyGenerator;
  private final TypedRejectionWriter rejectionWriter;
  private final TypedResponseWriter responseWriter;
  private final CommandDistributionBehavior distributionBehavior;

  public AuthorizationCreateProcessor(
      final KeyGenerator keyGenerator,
      final ProcessingState processingState,
      final Writers writers,
      final CommandDistributionBehavior distributionBehavior) {
    authorizationState = processingState.getAuthorizationState();
    stateWriter = writers.state();
    this.keyGenerator = keyGenerator;
    rejectionWriter = writers.rejection();
    responseWriter = writers.response();
    this.distributionBehavior = distributionBehavior;
  }

  @Override
  public void processNewCommand(final TypedRecord<AuthorizationRecord> command) {
    final var authorizationToCreate = command.getValue();

    final var authorization =
        authorizationState.getResourceIdentifiers(
            authorizationToCreate.getOwnerKey(),
            authorizationToCreate.getOwnerType(),
            authorizationToCreate.getResourceType(),
            PermissionType.CREATE);

    if (authorization != null) {
      final var rejectionMessage =
          "Expected to create authorization with owner key: %s, but an authorization with these values already exists"
              .formatted(authorizationToCreate.getOwnerKey());
      rejectionWriter.appendRejection(command, RejectionType.ALREADY_EXISTS, rejectionMessage);
      responseWriter.writeRejectionOnCommand(
          command, RejectionType.ALREADY_EXISTS, rejectionMessage);
      return;
    }

    final var key = keyGenerator.nextKey();

    stateWriter.appendFollowUpEvent(key, AuthorizationIntent.CREATED, authorizationToCreate);
    distributionBehavior.withKey(key).distribute(command);
    responseWriter.writeEventOnCommand(
        key, AuthorizationIntent.CREATED, authorizationToCreate, command);
  }

  @Override
  public void processDistributedCommand(final TypedRecord<AuthorizationRecord> command) {
    stateWriter.appendFollowUpEvent(
        command.getKey(), AuthorizationIntent.CREATED, command.getValue());

    distributionBehavior.acknowledgeCommand(command);
  }
}
