/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.user;

import static io.camunda.zeebe.util.buffer.BufferUtil.wrapString;

import io.camunda.zeebe.db.ColumnFamily;
import io.camunda.zeebe.db.DbKey;
import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.DbForeignKey;
import io.camunda.zeebe.db.impl.DbLong;
import io.camunda.zeebe.db.impl.DbString;
import io.camunda.zeebe.engine.state.immutable.UserState;
import io.camunda.zeebe.engine.state.mutable.MutableUserState;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.impl.record.value.user.UserRecord;
import java.util.List;
import java.util.Optional;
import org.agrona.DirectBuffer;

public class DbUserState implements UserState, MutableUserState {

  private final PersistedUser persistedUser = new PersistedUser();

  private final DbString username;
  private final DbLong userKey;
  private final DbForeignKey<DbLong> fkUserKey;
  private final ColumnFamily<DbString, DbForeignKey<DbLong>> userKeyByUsernameColumnFamily;
  private final ColumnFamily<DbKey, PersistedUser> userByUserKeyColumnFamily;

  public DbUserState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final TransactionContext transactionContext) {
    username = new DbString();
    userKey = new DbLong();
    fkUserKey = new DbForeignKey<>(userKey, ZbColumnFamilies.USERS);
    userKeyByUsernameColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.USER_KEY_BY_USERNAME, transactionContext, username, fkUserKey);
    userByUserKeyColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.USERS, transactionContext, userKey, new PersistedUser());
  }

  @Override
  public void create(final UserRecord user) {
    username.wrapBuffer(user.getUsernameBuffer());
    userKey.wrapLong(user.getUserKey());
    persistedUser.setUser(user);

    userByUserKeyColumnFamily.insert(userKey, persistedUser);
    userKeyByUsernameColumnFamily.insert(username, fkUserKey);
  }

  @Override
  public void update(final UserRecord user) {
    username.wrapBuffer(user.getUsernameBuffer());
    final var key = userKeyByUsernameColumnFamily.get(username);
    persistedUser.setUser(user);

    userByUserKeyColumnFamily.update(key, persistedUser);
  }

  @Override
  public void delete(final long userKey) {
    this.userKey.wrapLong(userKey);
    userByUserKeyColumnFamily.deleteExisting(this.userKey);
  }

  @Override
  public void addRole(final long userKey, final String roleKey) {
    this.userKey.wrapLong(userKey);
    final var persistedUser = userByUserKeyColumnFamily.get(this.userKey);
    persistedUser.addRoleKey(roleKey);
    userByUserKeyColumnFamily.update(this.userKey, persistedUser);
  }

  @Override
  public void removeRole(final long userKey, final long roleKey) {
    this.userKey.wrapLong(userKey);
    final var persistedUser = userByUserKeyColumnFamily.get(this.userKey);
    final List<String> roleKeys = persistedUser.getRoleKeysList();
    roleKeys.remove(roleKey);
    persistedUser.setRoleKeysList(roleKeys);
    userByUserKeyColumnFamily.update(this.userKey, persistedUser);
  }

  @Override
  public void addTenantId(final long userKey, final String tenantId) {
    this.userKey.wrapLong(userKey);
    final var persistedUser = userByUserKeyColumnFamily.get(this.userKey);
    persistedUser.addTenantId(tenantId);
    userByUserKeyColumnFamily.update(this.userKey, persistedUser);
  }

  @Override
  public void removeTenant(final long userKey, final String tenantId) {
    this.userKey.wrapLong(userKey);
    final var persistedUser = userByUserKeyColumnFamily.get(this.userKey);
    final List<String> tenantIds = persistedUser.getTenantIdsList();
    tenantIds.remove(tenantId);
    persistedUser.setTenantIdsList(tenantIds);
    userByUserKeyColumnFamily.update(this.userKey, persistedUser);
  }

  @Override
  public void addGroup(final long userKey, final String groupKey) {
    this.userKey.wrapLong(userKey);
    final var persistedUser = userByUserKeyColumnFamily.get(this.userKey);
    persistedUser.addGroupKey(groupKey);
    userByUserKeyColumnFamily.update(this.userKey, persistedUser);
  }

  @Override
  public void removeGroup(final long userKey, final long groupKey) {
    this.userKey.wrapLong(userKey);
    final var persistedUser = userByUserKeyColumnFamily.get(this.userKey);
    final List<String> groupKeys = persistedUser.getGroupKeysList();
    groupKeys.remove(groupKey);
    persistedUser.setGroupKeysList(groupKeys);
    userByUserKeyColumnFamily.update(this.userKey, persistedUser);
  }

  @Override
  public Optional<PersistedUser> getUser(final DirectBuffer username) {
    this.username.wrapBuffer(username);
    final var key = userKeyByUsernameColumnFamily.get(this.username);

    if (key == null) {
      return Optional.empty();
    }

    return getUser(key.inner().getValue());
  }

  @Override
  public Optional<PersistedUser> getUser(final String username) {
    return getUser(wrapString(username));
  }

  @Override
  public Optional<PersistedUser> getUser(final long userKey) {
    this.userKey.wrapLong(userKey);
    final var persistedUser = userByUserKeyColumnFamily.get(this.userKey);

    if (persistedUser == null) {
      return Optional.empty();
    }
    return Optional.of(persistedUser.copy());
  }

  @Override
  public List<String> getTenantIds(final long userKey) {
    this.userKey.wrapLong(userKey);
    final var persistedUser = userByUserKeyColumnFamily.get(this.userKey);

    if (persistedUser == null) {
      return List.of();
    }
    return persistedUser.getTenantIdsList();
  }
}
