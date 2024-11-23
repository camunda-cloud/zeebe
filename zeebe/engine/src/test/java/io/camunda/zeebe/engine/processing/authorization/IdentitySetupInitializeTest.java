/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.authorization;

import static io.camunda.zeebe.engine.processing.identity.AuthorizationCheckBehavior.WILDCARD_PERMISSION;
import static io.camunda.zeebe.protocol.record.Assertions.assertThat;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.impl.record.value.authorization.RoleRecord;
import io.camunda.zeebe.protocol.impl.record.value.user.UserRecord;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.AuthorizationIntent;
import io.camunda.zeebe.protocol.record.intent.IdentitySetupIntent;
import io.camunda.zeebe.protocol.record.intent.RoleIntent;
import io.camunda.zeebe.protocol.record.intent.UserIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationRecordValue;
import io.camunda.zeebe.protocol.record.value.AuthorizationRecordValue.PermissionValue;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.EntityType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class IdentitySetupInitializeTest {

  @Rule public final EngineRule engine = EngineRule.singlePartition();
  @Rule public final TestWatcher recordingExporterTestWatcher = new RecordingExporterTestWatcher();

  @Test
  public void shouldCreateRoleAndUser() {
    // given
    final var roleKey = 1;
    final var roleName = "roleName";
    final var role = new RoleRecord().setRoleKey(roleKey).setName(roleName);
    final var userKey = 2;
    final var username = "username";
    final var userName = "userName";
    final var password = "password";
    final var mail = "e@mail.com";
    final var user =
        new UserRecord()
            .setUserKey(userKey)
            .setUsername(username)
            .setName(userName)
            .setPassword(password)
            .setEmail(mail);

    // when
    engine.identitySetup().initialize().withRole(role).withUser(user).initialize();

    // then
    assertThat(RecordingExporter.roleRecords(RoleIntent.CREATED).getFirst().getValue())
        .hasRoleKey(roleKey)
        .hasName(roleName);
    assertThat(RecordingExporter.userRecords(UserIntent.CREATED).getFirst().getValue())
        .hasUserKey(userKey)
        .hasUsername(username)
        .hasName(userName)
        .hasPassword(password)
        .hasEmail(mail);
    assertUserIsAssignedToRole(roleKey, userKey);
    assertThatAllPermissionsAreAddedToRole(roleKey);
  }

  @Test
  public void shouldNotCreateUserIfAlreadyExists() {
    // given
    final var roleKey = 1;
    final var roleName = "roleName";
    final var role = new RoleRecord().setRoleKey(roleKey).setName(roleName);
    final var username = "username";
    final var userName = "userName";
    final var password = "password";
    final var mail = "e@mail.com";
    final var user =
        new UserRecord()
            .setUserKey(2)
            .setUsername(username)
            .setName(userName)
            .setPassword(password)
            .setEmail(mail);
    final var userKey =
        engine
            .user()
            .newUser(username)
            .withName(userName)
            .withPassword(password)
            .withEmail(mail)
            .create()
            .getKey();

    // when
    final var initializeRecord =
        engine.identitySetup().initialize().withRole(role).withUser(user).initialize();

    // then
    assertUserIsNotCreated(initializeRecord.getSourceRecordPosition());
    assertUserIsAssignedToRole(roleKey, userKey);
  }

  @Test
  public void shouldNotCreateRoleIfAlreadyExists() {
    // given
    final var roleName = "roleName";
    final var role = new RoleRecord().setRoleKey(1).setName(roleName);
    final var userKey = 2;
    final var username = "username";
    final var userName = "userName";
    final var password = "password";
    final var mail = "e@mail.com";
    final var user =
        new UserRecord()
            .setUserKey(userKey)
            .setUsername(username)
            .setName(userName)
            .setPassword(password)
            .setEmail(mail);
    final var roleKey = engine.role().newRole(roleName).create().getKey();

    // when
    final var initializeRecord =
        engine.identitySetup().initialize().withRole(role).withUser(user).initialize();

    // then
    assertRoleIsNotCreated(initializeRecord.getSourceRecordPosition());
    assertUserIsAssignedToRole(roleKey, userKey);
    Assertions.assertThat(
            RecordingExporter.records()
                .limit(r -> r.getIntent() == IdentitySetupIntent.INITIALIZED)
                .authorizationRecords()
                .asList())
        .describedAs("No permissions should be added. The role should not be modified.")
        .isEmpty();
  }

  @Test
  public void shouldAssignUserToRoleIfBothAlreadyExist() {
    // given
    final var roleName = "roleName";
    final var role = new RoleRecord().setRoleKey(1).setName(roleName);
    final var username = "username";
    final var userName = "userName";
    final var password = "password";
    final var mail = "e@mail.com";
    final var user =
        new UserRecord()
            .setUserKey(2)
            .setUsername(username)
            .setName(userName)
            .setPassword(password)
            .setEmail(mail);
    final var roleKey = engine.role().newRole(roleName).create().getKey();
    final var userKey =
        engine
            .user()
            .newUser(username)
            .withName(userName)
            .withPassword(password)
            .withEmail(mail)
            .create()
            .getKey();

    // when
    final var initializeRecord =
        engine.identitySetup().initialize().withRole(role).withUser(user).initialize();

    // then
    assertRoleIsNotCreated(initializeRecord.getSourceRecordPosition());
    assertUserIsNotCreated(initializeRecord.getSourceRecordPosition());
    assertUserIsAssignedToRole(roleKey, userKey);
  }

  private static void assertUserIsAssignedToRole(final long roleKey, final long userKey) {
    assertThat(RecordingExporter.roleRecords(RoleIntent.ENTITY_ADDED).getFirst().getValue())
        .hasRoleKey(roleKey)
        .hasEntityKey(userKey);
  }

  @Test
  public void shouldNotAssignUserToRoleIfAlreadyAssigned() {
    // given
    final var roleName = "roleName";
    final var role = new RoleRecord().setRoleKey(1).setName(roleName);
    final var username = "username";
    final var userName = "userName";
    final var password = "password";
    final var mail = "e@mail.com";
    final var user =
        new UserRecord()
            .setUserKey(2)
            .setUsername(username)
            .setName(userName)
            .setPassword(password)
            .setEmail(mail);
    final var roleKey = engine.role().newRole(roleName).create().getKey();
    final var userKey =
        engine
            .user()
            .newUser(username)
            .withName(userName)
            .withPassword(password)
            .withEmail(mail)
            .create()
            .getKey();
    engine.role().addEntity(roleKey).withEntityKey(userKey).withEntityType(EntityType.USER).add();

    // when
    final var initializeRecord =
        engine.identitySetup().initialize().withRole(role).withUser(user).initialize();

    // then
    assertRoleIsNotCreated(initializeRecord.getSourceRecordPosition());
    assertUserIsNotCreated(initializeRecord.getSourceRecordPosition());
    assertNoAssignmentIsCreated(initializeRecord.getSourceRecordPosition());
  }

  private static void assertThatAllPermissionsAreAddedToRole(final long roleKey) {
    final var addedPermissions =
        RecordingExporter.authorizationRecords(AuthorizationIntent.PERMISSION_ADDED)
            .withOwnerKey(roleKey)
            .limit(AuthorizationResourceType.values().length)
            .map(Record::getValue)
            .toList();
    Assertions.assertThat(addedPermissions)
        .describedAs("Added permissions for all resource types")
        .extracting(AuthorizationRecordValue::getResourceType)
        .containsExactly(AuthorizationResourceType.values());

    final List<Tuple> expectedPermissions = new ArrayList<>();
    for (final PermissionType value : PermissionType.values()) {
      expectedPermissions.add(Tuple.tuple(value, Set.of(WILDCARD_PERMISSION)));
    }

    Assertions.assertThat(addedPermissions)
        .describedAs("Added permissions for all resource types")
        .flatMap(AuthorizationRecordValue::getPermissions)
        .extracting(PermissionValue::getPermissionType, PermissionValue::getResourceIds)
        .containsOnly(expectedPermissions.toArray(new Tuple[0]));
  }

  private static void assertUserIsNotCreated(final long initializePosition) {
    Assertions.assertThat(
            RecordingExporter.records()
                .after(initializePosition)
                .limit(r -> r.getIntent() == IdentitySetupIntent.INITIALIZED)
                .userRecords()
                .withIntent(UserIntent.CREATED)
                .toList())
        .isEmpty();
  }

  private static void assertRoleIsNotCreated(final long initializePosition) {
    Assertions.assertThat(
            RecordingExporter.records()
                .after(initializePosition)
                .limit(r -> r.getIntent() == IdentitySetupIntent.INITIALIZED)
                .roleRecords()
                .withIntent(RoleIntent.CREATED)
                .toList())
        .isEmpty();
  }

  private static void assertNoAssignmentIsCreated(final long initializePosition) {
    Assertions.assertThat(
            RecordingExporter.records()
                .after(initializePosition)
                .limit(r -> r.getIntent() == IdentitySetupIntent.INITIALIZED)
                .roleRecords()
                .withIntent(RoleIntent.ENTITY_ADDED)
                .toList())
        .isEmpty();
  }
}
