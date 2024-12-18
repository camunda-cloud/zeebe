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
import io.camunda.client.ZeebeClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.protocol.rest.PermissionTypeEnum;
import io.camunda.client.protocol.rest.ResourceTypeEnum;
import io.camunda.zeebe.it.util.AuthorizationsUtil;
import io.camunda.zeebe.it.util.AuthorizationsUtil.Permissions;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.DeploymentIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.test.util.junit.AutoCloseResources;
import io.camunda.zeebe.test.util.junit.AutoCloseResources.AutoCloseResource;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.testcontainers.TestSearchContainers;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@AutoCloseResources
@Testcontainers
@ZeebeIntegration
public class MessageCorrelationCorrelateAuthorizationIT {

  public static final String INTERMEDIATE_MSG_NAME = "intermediateMsg";
  public static final String START_MSG_NAME = "startMsg";
  public static final String CORRELATION_KEY_VARIABLE = "correlationKey";

  @Container
  private static final ElasticsearchContainer CONTAINER =
      TestSearchContainers.createDefeaultElasticsearchContainer();

  private static final String PROCESS_ID = "processId";
  private static AuthorizationsUtil authUtil;
  @AutoCloseResource private static ZeebeClient defaultUserClient;

  @TestZeebe(autoStart = false)
  private TestStandaloneBroker broker =
      new TestStandaloneBroker()
          .withRecordingExporter(true)
          .withSecurityConfig(c -> c.getAuthorizations().setEnabled(true))
          .withAdditionalProfile(Profile.AUTH_BASIC);

  @BeforeEach
  void beforeEach() {
    broker.withCamundaExporter("http://" + CONTAINER.getHttpHostAddress());
    broker.start();

    final var defaultUsername = "demo";
    defaultUserClient = createClient(broker, defaultUsername, "demo");
    authUtil = new AuthorizationsUtil(broker, defaultUserClient, CONTAINER.getHttpHostAddress());

    authUtil.awaitUserExistsInElasticsearch(defaultUsername);
    defaultUserClient
        .newDeployResourceCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess(PROCESS_ID)
                .startEvent()
                .intermediateCatchEvent()
                .message(
                    m ->
                        m.name(INTERMEDIATE_MSG_NAME)
                            .zeebeCorrelationKeyExpression(CORRELATION_KEY_VARIABLE))
                .endEvent()
                .moveToProcess(PROCESS_ID)
                .startEvent()
                .message(m -> m.name(START_MSG_NAME))
                .done(),
            "process.xml")
        .send()
        .join();
  }

  @Test
  void shouldBeAuthorizedToCorrelateMessageToIntermediateEventWithDefaultUser() {
    // given
    final var correlationKey = UUID.randomUUID().toString();
    final var processInstance = createProcessInstance(correlationKey);

    // when
    final var response =
        defaultUserClient
            .newCorrelateMessageCommand()
            .messageName(INTERMEDIATE_MSG_NAME)
            .correlationKey(correlationKey)
            .send()
            .join();

    // then
    assertThat(response.getProcessInstanceKey()).isEqualTo(processInstance.getProcessInstanceKey());
  }

  @Test
  void shouldBeAuthorizedToCorrelateMessageToIntermediateEventWithUser() {
    // given
    final var correlationKey = UUID.randomUUID().toString();
    final var processInstance = createProcessInstance(correlationKey);
    final var username = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUserWithPermissions(
        username,
        password,
        new Permissions(
            ResourceTypeEnum.PROCESS_DEFINITION,
            PermissionTypeEnum.UPDATE_PROCESS_INSTANCE,
            List.of(PROCESS_ID)));

    try (final var client = authUtil.createClient(username, password)) {
      // when
      final var response =
          client
              .newCorrelateMessageCommand()
              .messageName(INTERMEDIATE_MSG_NAME)
              .correlationKey(correlationKey)
              .send()
              .join();

      // then
      assertThat(response.getProcessInstanceKey())
          .isEqualTo(processInstance.getProcessInstanceKey());
    }
  }

  @Test
  void shouldBeUnauthorizedToCorrelateMessageToIntermediateEventIfNoPermissions() {
    // given
    final var correlationKey = UUID.randomUUID().toString();
    createProcessInstance(correlationKey);
    final var username = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUser(username, password);

    try (final var client = authUtil.createClient(username, password)) {

      // when
      final var response =
          client
              .newCorrelateMessageCommand()
              .messageName(INTERMEDIATE_MSG_NAME)
              .correlationKey(correlationKey)
              .send();

      // then
      assertThatThrownBy(response::join)
          .isInstanceOf(ProblemException.class)
          .hasMessageContaining("title: FORBIDDEN")
          .hasMessageContaining("status: 403")
          .hasMessageContaining(
              "Insufficient permissions to perform operation 'UPDATE_PROCESS_INSTANCE' on resource 'PROCESS_DEFINITION', required resource identifiers are one of '[*, %s]'",
              PROCESS_ID);
    }
  }

  @Test
  void shouldBeAuthorizedToCorrelateMessageToStartEventWithDefaultUser() {
    // when
    final var response =
        defaultUserClient
            .newCorrelateMessageCommand()
            .messageName(START_MSG_NAME)
            .withoutCorrelationKey()
            .send()
            .join();

    // then
    assertThat(response.getProcessInstanceKey()).isPositive();
  }

  @Test
  void shouldBeAuthorizedToCorrelateMessageToStartEventWithUser() {
    // given
    final var username = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUserWithPermissions(
        username,
        password,
        new Permissions(
            ResourceTypeEnum.PROCESS_DEFINITION,
            PermissionTypeEnum.CREATE_PROCESS_INSTANCE,
            List.of(PROCESS_ID)));

    try (final var client = authUtil.createClient(username, password)) {
      // when
      final var response =
          client
              .newCorrelateMessageCommand()
              .messageName(START_MSG_NAME)
              .withoutCorrelationKey()
              .send()
              .join();

      // then
      assertThat(response.getProcessInstanceKey()).isPositive();
    }
  }

  @Test
  void shouldBeUnauthorizedToCorrelateMessageToStartEventIfNoPermissions() {
    // given
    final var username = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUser(username, password);

    try (final var client = authUtil.createClient(username, password)) {
      // when
      final var response =
          client
              .newCorrelateMessageCommand()
              .messageName(START_MSG_NAME)
              .withoutCorrelationKey()
              .send();

      // then
      assertThatThrownBy(response::join)
          .isInstanceOf(ProblemException.class)
          .hasMessageContaining("title: FORBIDDEN")
          .hasMessageContaining("status: 403")
          .hasMessageContaining(
              "Insufficient permissions to perform operation 'CREATE_PROCESS_INSTANCE' on resource 'PROCESS_DEFINITION', required resource identifiers are one of '[*, %s]'",
              PROCESS_ID);
    }
  }

  @Test
  void shouldNotCorrelateAnyMessageIfUnauthorizedForOne() {
    // given
    final var username = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUserWithPermissions(
        username,
        password,
        new Permissions(
            ResourceTypeEnum.PROCESS_DEFINITION,
            PermissionTypeEnum.CREATE_PROCESS_INSTANCE,
            List.of(PROCESS_ID)));
    final var unauthorizedProcessId = "unauthorizedProcessId";
    final var resourceName = "unauthorizedProcess.xml";
    final var deploymentKey =
        defaultUserClient
            .newDeployResourceCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess(unauthorizedProcessId)
                    .startEvent()
                    .message(m -> m.name(START_MSG_NAME))
                    .endEvent()
                    .done(),
                resourceName)
            .send()
            .join()
            .getKey();

    try (final var client = authUtil.createClient(username, password)) {
      // when
      final var response =
          client
              .newCorrelateMessageCommand()
              .messageName(START_MSG_NAME)
              .withoutCorrelationKey()
              .send();

      // then
      assertThatThrownBy(response::join)
          .isInstanceOf(ProblemException.class)
          .hasMessageContaining("title: FORBIDDEN")
          .hasMessageContaining("status: 403")
          .hasMessageContaining(
              "Insufficient permissions to perform operation 'CREATE_PROCESS_INSTANCE' on resource 'PROCESS_DEFINITION', required resource identifiers are one of '[*, %s]'",
              unauthorizedProcessId);

      final var deploymentPosition =
          RecordingExporter.deploymentRecords(DeploymentIntent.CREATED)
              .withRecordKey(deploymentKey)
              .getFirst()
              .getPosition();
      assertThat(
              RecordingExporter.records()
                  .after(deploymentPosition)
                  .limit(r -> r.getRejectionType() == RejectionType.FORBIDDEN)
                  .processInstanceRecords()
                  .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATING)
                  .withBpmnProcessId(unauthorizedProcessId)
                  .exists())
          .isFalse();
    }
  }

  private ProcessInstanceEvent createProcessInstance(final String correlationKey) {
    return defaultUserClient
        .newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_ID)
        .latestVersion()
        .variables(Map.of(CORRELATION_KEY_VARIABLE, correlationKey))
        .send()
        .join();
  }
}
