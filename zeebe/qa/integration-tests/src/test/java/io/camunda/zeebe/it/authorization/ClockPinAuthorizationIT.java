/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.authorization;

import static io.camunda.zeebe.it.util.AuthorizationsUtil.createClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.application.Profile;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.ProblemException;
import io.camunda.zeebe.client.protocol.rest.PermissionTypeEnum;
import io.camunda.zeebe.client.protocol.rest.ResourceTypeEnum;
import io.camunda.zeebe.it.util.AuthorizationsUtil;
import io.camunda.zeebe.it.util.AuthorizationsUtil.Permissions;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.test.util.junit.AutoCloseResources;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@AutoCloseResources
@Testcontainers
@ZeebeIntegration
public class ClockPinAuthorizationIT {
  private static final DockerImageName ELASTIC_IMAGE =
      DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch")
          .withTag(RestClient.class.getPackage().getImplementationVersion());

  @Container
  private static final ElasticsearchContainer CONTAINER =
      new ElasticsearchContainer(ELASTIC_IMAGE)
          // use JVM option files to avoid overwriting default options set by the ES container class
          .withClasspathResourceMapping(
              "elasticsearch-fast-startup.options",
              "/usr/share/elasticsearch/config/jvm.options.d/ elasticsearch-fast-startup.options",
              BindMode.READ_ONLY)
          // can be slow in CI
          .withStartupTimeout(Duration.ofMinutes(5))
          .withEnv("action.auto_create_index", "true")
          .withEnv("xpack.security.enabled", "false")
          .withEnv("xpack.watcher.enabled", "false")
          .withEnv("xpack.ml.enabled", "false")
          .withEnv("action.destructive_requires_name", "false");

  @TestZeebe(autoStart = false)
  private static final TestStandaloneBroker BROKER =
      new TestStandaloneBroker()
          .withRecordingExporter(true)
          .withBrokerConfig(
              b -> b.getExperimental().getEngine().getAuthorizations().setEnableAuthorization(true))
          .withAdditionalProfile(Profile.AUTH_BASIC);

  private static AuthorizationsUtil authUtil;
  private static ZeebeClient defaultUserClient;

  @BeforeAll
  static void beforeAll() {
    BROKER.withCamundaExporter("http://" + CONTAINER.getHttpHostAddress());
    BROKER.start();

    final var defaultUsername = "demo";
    defaultUserClient = createClient(BROKER, defaultUsername, "demo");
    authUtil = new AuthorizationsUtil(BROKER, defaultUserClient, CONTAINER.getHttpHostAddress());

    authUtil.awaitUserExistsInElasticsearch(defaultUsername);
  }

  @Test
  void shouldBeAuthorizedToPinClockWithDefaultUser() {
    // given
    // when
    final var response = defaultUserClient.newClockPinCommand().time(Instant.now()).send().join();

    // The Rest API returns a null future for an empty response
    // We can verify for null, as if we'd be unauthenticated we'd get an exception
    assertThat(response).isNull();
  }

  @Test
  void shouldBeAuthorizedToPinClockWithPermissions() {
    // given
    final var username = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUserWithPermissions(
        username,
        password,
        new Permissions(ResourceTypeEnum.SYSTEM, PermissionTypeEnum.UPDATE, List.of("*")));

    try (final var client = authUtil.createClient(username, password)) {
      // when
      final var response = client.newClockPinCommand().time(Instant.now()).send().join();

      // The Rest API returns a null future for an empty response
      // We can verify for null, as if we'd be unauthenticated we'd get an exception
      assertThat(response).isNull();
    }
  }

  @Test
  void shouldBeUnAuthorizedToPinClockWithPermissions() {
    // given
    final var username = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUser(username, password);

    // when
    try (final var client = authUtil.createClient(username, password)) {
      final var response = client.newClockPinCommand().time(Instant.now()).send();

      // then
      assertThatThrownBy(response::join)
          .isInstanceOf(ProblemException.class)
          .hasMessageContaining("title: UNAUTHORIZED")
          .hasMessageContaining("status: 401")
          .hasMessageContaining("Unauthorized to perform operation 'UPDATE' on resource 'SYSTEM'");
    }
  }
}
