/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.authorization;

import io.camunda.zeebe.db.ColumnFamily;
import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.DbCompositeKey;
import io.camunda.zeebe.db.impl.DbString;
import io.camunda.zeebe.engine.state.immutable.AuthorizationState;
import io.camunda.zeebe.engine.state.mutable.MutableAuthorizationState;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.impl.record.value.authorization.AuthorizationRecord;
import io.camunda.zeebe.protocol.record.value.AuthorizationOwnerType;

public class DbAuthorizationState implements AuthorizationState, MutableAuthorizationState {
  private final PersistedAuthorization persistedAuthorization = new PersistedAuthorization();

  private final PersistedPermissions persistedPermissions = new PersistedPermissions();

  private final DbString ownerKey;
  private final DbString ownerType;
  private final DbString resourceKey;
  private final DbString resourceType;
  private final DbCompositeKey<DbString, DbString> resourceCompositeKey;
  private final DbCompositeKey<
          DbCompositeKey<DbString, DbString>, DbCompositeKey<DbString, DbString>>
      ownerAndResourceCompositeKey;
  // owner key + owner type + resource key + resource type -> permission
  private final ColumnFamily<
          DbCompositeKey<DbCompositeKey<DbString, DbString>, DbCompositeKey<DbString, DbString>>,
          PersistedPermissions>
      ownerAuthorizationColumnFamily;

  public DbAuthorizationState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final TransactionContext transactionContext) {
    ownerKey = new DbString();
    ownerType = new DbString();
    resourceKey = new DbString();
    resourceType = new DbString();
    resourceCompositeKey = new DbCompositeKey<>(resourceKey, resourceType);
    ownerAndResourceCompositeKey =
        new DbCompositeKey<>(new DbCompositeKey<>(ownerKey, ownerType), resourceCompositeKey);

    ownerAuthorizationColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.AUTHORIZATIONS_BY_USERNAME_AND_PERMISSION,
            transactionContext,
            ownerAndResourceCompositeKey,
            persistedPermissions);
  }

  @Override
  public void createAuthorization(final AuthorizationRecord authorizationRecord) {
    persistedAuthorization.setAuthorization(authorizationRecord);

    ownerKey.wrapString(authorizationRecord.getOwnerKey());
    ownerType.wrapString(authorizationRecord.getOwnerType().name());
    resourceKey.wrapString(authorizationRecord.getResourceKey());
    resourceType.wrapString(authorizationRecord.getResourceType());
    persistedPermissions.setPermissions(authorizationRecord.getPermissions());
    ownerAuthorizationColumnFamily.insert(ownerAndResourceCompositeKey, persistedPermissions);
  }

  @Override
  public PersistedPermissions getPermissions(
      final String ownerKey,
      final AuthorizationOwnerType ownerType,
      final String resourceKey,
      final String resourceType) {
    this.ownerKey.wrapString(ownerKey);
    this.ownerType.wrapString(ownerType.name());
    this.resourceKey.wrapString(resourceKey);
    this.resourceType.wrapString(resourceType);

    final var persistedPermissions =
        ownerAuthorizationColumnFamily.get(ownerAndResourceCompositeKey);

    return persistedPermissions == null ? null : persistedPermissions.copy();
  }
}
