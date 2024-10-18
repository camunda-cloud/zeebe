/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.user;

import static io.camunda.zeebe.protocol.record.Assertions.assertThat;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.UUID;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class DeleteUserTest {
  @ClassRule public static final EngineRule ENGINE = EngineRule.singlePartition();

  @Rule
  public final RecordingExporterTestWatcher recordingExporterTestWatcher =
      new RecordingExporterTestWatcher();

  @Test
  public void shouldDeleteAUser() {
    final var username = UUID.randomUUID().toString();
    final var userRecord =
        ENGINE
            .user()
            .newUser(username)
            .withName("Foo Bar")
            .withEmail("foo@bar.com")
            .withPassword("password")
            .create();

    final var deletedUser =
        ENGINE
            .user()
            .deleteUser(userRecord.getKey())
            .withUsername(userRecord.getValue().getUsername())
            .withName("Bar Foo")
            .withEmail("foo@bar.blah")
            .withPassword("Foo Bar")
            .delete()
            .getValue();

    assertThat(deletedUser)
        .isNotNull()
        .hasFieldOrPropertyWithValue("username", username)
        .hasFieldOrPropertyWithValue("name", "Bar Foo")
        .hasFieldOrPropertyWithValue("email", "foo@bar.blah")
        .hasFieldOrPropertyWithValue("password", "Foo Bar");
  }

  @Test
  public void shouldRejectTheCommandIfUserDoesNotExist() {
    final var userNotFoundRejection =
        ENGINE
            .user()
            .deleteUser(1234L)
            .withUsername("foobar")
            .withName("Bar Foo")
            .withEmail("foo@bar.blah")
            .withPassword("Foo Bar")
            .expectRejection()
            .delete();

    assertThat(userNotFoundRejection)
        .hasRejectionType(RejectionType.NOT_FOUND)
        .hasRejectionReason(
            "Expected to delete user with key 1234, but a user with this key does not exist");
  }
}
