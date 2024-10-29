/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.user;

import io.camunda.zeebe.db.DbValue;
import io.camunda.zeebe.msgpack.UnpackedObject;
import io.camunda.zeebe.msgpack.property.ArrayProperty;
import io.camunda.zeebe.msgpack.property.ObjectProperty;
import io.camunda.zeebe.msgpack.value.LongValue;
import io.camunda.zeebe.msgpack.value.StringValue;
import io.camunda.zeebe.protocol.impl.record.value.user.UserRecord;
import io.camunda.zeebe.protocol.record.value.UserType;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class PersistedUser extends UnpackedObject implements DbValue {

  private final ObjectProperty<UserRecord> userProp =
      new ObjectProperty<>("user", new UserRecord());
  private final ArrayProperty<LongValue> roleKeysProp =
      new ArrayProperty<>("roleKeys", LongValue::new);
  private final ArrayProperty<StringValue> tenantIdsProp =
      new ArrayProperty<>("tenantIds", StringValue::new);

  public PersistedUser() {
    super(1);
    declareProperty(userProp);
  }

  public PersistedUser copy() {
    final var copy = new PersistedUser();
    copy.setUser(getUser());
    copy.setRoleKeysList(getRoleKeysList());
    copy.setTenantIdsList(getTenantIdsList());
    return copy;
  }

  public UserRecord getUser() {
    return userProp.getValue();
  }

  public void setUser(final UserRecord record) {
    userProp.getValue().wrap(record);
  }

  public long getUserKey() {
    return getUser().getUserKey();
  }

  public String getUsername() {
    return getUser().getUsername();
  }

  public String getName() {
    return getUser().getName();
  }

  public String getEmail() {
    return getUser().getEmail();
  }

  public String getPassword() {
    return getUser().getPassword();
  }

  public UserType getUserType() {
    return getUser().getUserType();
  }

  public List<Long> getRoleKeysList() {
    return StreamSupport.stream(roleKeysProp.spliterator(), false)
        .map(LongValue::getValue)
        .collect(Collectors.toList());
  }

  public PersistedUser setRoleKeysList(final List<Long> roleKeys) {
    roleKeysProp.reset();
    roleKeys.forEach(roleKey -> roleKeysProp.add().setValue(roleKey));
    return this;
  }

  public PersistedUser addRoleKey(final long roleKey) {
    roleKeysProp.add().setValue(roleKey);
    return this;
  }

  public List<String> getTenantIdsList() {
    return StreamSupport.stream(tenantIdsProp.spliterator(), false)
        .map(StringValue::toString)
        .collect(Collectors.toList());
  }

  public PersistedUser setTenantIdsList(final List<String> tenantIds) {
    tenantIdsProp.reset();
    tenantIds.forEach(
        tenantId -> {
          final DirectBuffer buffer = new UnsafeBuffer(tenantId.getBytes());
          tenantIdsProp.add().wrap(buffer);
        });
    return this;
  }

  public PersistedUser addTenantId(final String tenantId) {
    final DirectBuffer buffer = new UnsafeBuffer(tenantId.getBytes());
    tenantIdsProp.add().wrap(buffer);
    return this;
  }
}
