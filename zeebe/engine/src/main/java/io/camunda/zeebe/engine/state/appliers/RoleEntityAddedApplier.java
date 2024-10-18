/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.appliers;

import io.camunda.zeebe.engine.state.TypedEventApplier;
import io.camunda.zeebe.engine.state.mutable.MutableRoleState;
import io.camunda.zeebe.engine.state.mutable.MutableUserState;
import io.camunda.zeebe.protocol.impl.record.value.authorization.RoleRecord;
import io.camunda.zeebe.protocol.record.intent.RoleIntent;
import io.camunda.zeebe.protocol.record.value.EntityType;

public class RoleEntityAddedApplier implements TypedEventApplier<RoleIntent, RoleRecord> {

  private final MutableRoleState roleState;
  private final MutableUserState userState;

  public RoleEntityAddedApplier(
      final MutableRoleState roleState, final MutableUserState userState) {
    this.roleState = roleState;
    this.userState = userState;
  }

  @Override
  public void applyState(final long key, final RoleRecord value) {
    roleState.addEntity(value);
    if (value.getEntityType() == EntityType.USER) {
      userState.addRole(value.getEntityKey(), value.getRoleKey());
    }
    // todo add entity to mapping state when implemented
  }
}
