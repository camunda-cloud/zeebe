/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.util.client;

import io.camunda.zeebe.protocol.impl.record.value.authorization.AuthorizationRecord;
import io.camunda.zeebe.protocol.impl.record.value.authorization.Permission;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.AuthorizationIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationOwnerType;
import io.camunda.zeebe.protocol.record.value.AuthorizationRecordValue;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionAction;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import java.util.function.Function;

public final class AuthorizationClient {

  private final CommandWriter writer;

  public AuthorizationClient(final CommandWriter writer) {
    this.writer = writer;
  }

  public AuthorizationCreationClient newAuthorization() {
    return new AuthorizationCreationClient(writer);
  }

  public static class AuthorizationCreationClient {

    private static final Function<Long, Record<AuthorizationRecordValue>> SUCCESS_SUPPLIER =
        (position) ->
            RecordingExporter.authorizationRecords()
                .withIntent(AuthorizationIntent.CREATED)
                .withSourceRecordPosition(position)
                .getFirst();
    private static final Function<Long, Record<AuthorizationRecordValue>> REJECTION_SUPPLIER =
        (position) ->
            RecordingExporter.authorizationRecords()
                .onlyCommandRejections()
                .withIntent(AuthorizationIntent.CREATE)
                .withSourceRecordPosition(position)
                .getFirst();
    private final CommandWriter writer;
    private final AuthorizationRecord authorizationCreationRecord;
    private Function<Long, Record<AuthorizationRecordValue>> expectation = SUCCESS_SUPPLIER;

    public AuthorizationCreationClient(final CommandWriter writer) {
      this.writer = writer;
      authorizationCreationRecord = new AuthorizationRecord();
    }

    public AuthorizationCreationClient withOwnerKey(final Long ownerKey) {
      authorizationCreationRecord.setOwnerKey(ownerKey);
      return this;
    }

    public AuthorizationCreationClient withAction(final PermissionAction action) {
      authorizationCreationRecord.setAction(action);
      return this;
    }

    public AuthorizationCreationClient withOwnerType(final AuthorizationOwnerType ownerType) {
      authorizationCreationRecord.setOwnerType(ownerType);
      return this;
    }

    public AuthorizationCreationClient withResourceType(
        final AuthorizationResourceType resourceType) {
      authorizationCreationRecord.setResourceType(resourceType);
      return this;
    }

    public AuthorizationCreationClient withPermission(
        final PermissionType permissionType, final String resourceId) {
      authorizationCreationRecord.addPermission(
          new Permission().setPermissionType(permissionType).addResourceId(resourceId));
      return this;
    }

    public AuthorizationCreationClient withPermission(final Permission permission) {
      authorizationCreationRecord.addPermission(permission);
      return this;
    }

    public Record<AuthorizationRecordValue> create() {
      final long position =
          writer.writeCommand(AuthorizationIntent.CREATE, authorizationCreationRecord);
      return expectation.apply(position);
    }

    public AuthorizationCreationClient expectRejection() {
      expectation = REJECTION_SUPPLIER;
      return this;
    }
  }
}
