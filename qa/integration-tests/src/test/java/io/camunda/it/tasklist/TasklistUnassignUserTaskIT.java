/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.tasklist;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.application.Profile;
import io.camunda.qa.util.cluster.TestRestTasklistClient;
import io.camunda.qa.util.cluster.TestStandaloneCamunda;
import io.camunda.search.clients.query.SearchQueryBuilders;
import io.camunda.tasklist.schema.templates.TaskTemplate;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.test.util.junit.AutoCloseResources;
import io.camunda.zeebe.test.util.junit.AutoCloseResources.AutoCloseResource;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@AutoCloseResources
@ZeebeIntegration
public class TasklistUnassignUserTaskIT {

  private static final String PROCESS_ID = "foo";
  private static final String PROCESS_ID_WITH_JOB_BASED_USERTASK =
      "PROCESS_WITH_JOB_BASED_USERTASK";

  private static final String TEST_USER_NAME = "bar";
  private static final String TEST_USER_PASSWORD = "bar";

  @AutoCloseResource private static ZeebeClient zeebeClient;
  @AutoCloseResource private static TestRestTasklistClient tasklistRestClient;

  private long userTaskKey;
  private long userTaskKeyWithJobBasedUserTask;

  @TestZeebe
  private TestStandaloneCamunda standaloneCamunda =
      new TestStandaloneCamunda().withAdditionalProfile(Profile.DEFAULT_AUTH_PROFILE);

  @BeforeEach
  public void beforeAll() {
    zeebeClient = standaloneCamunda.newClientBuilder().build();
    tasklistRestClient = standaloneCamunda.newTasklistClient();

    // create user in Operate storage. Operate is the master for security checks
    standaloneCamunda.newOperateClient().createUser(TEST_USER_NAME, TEST_USER_PASSWORD);

    // deploy a process as admin user
    deployResource(zeebeClient, "process/process_with_assigned_user_task.bpmn");
    waitForProcessToBeDeployed(PROCESS_ID);

    // deploy process with a job based user task process
    deployResource(zeebeClient, "process/process_with_assigned_job_based_user_task.bpmn");
    waitForProcessToBeDeployed(PROCESS_ID_WITH_JOB_BASED_USERTASK);

    // create a process instance
    final var processInstanceKey = createProcessInstance(PROCESS_ID);
    userTaskKey = awaitUserTaskBeingAvailable(processInstanceKey);

    // create a process instance with job based user task
    final var processInstanceKeyWithJobBasedUserTask =
        createProcessInstance(PROCESS_ID_WITH_JOB_BASED_USERTASK);
    userTaskKeyWithJobBasedUserTask =
        awaitUserTaskBeingAvailable(processInstanceKeyWithJobBasedUserTask);
  }

  @Test
  public void shouldUnassignUserTask() {
    // when
    final var response =
        tasklistRestClient
            .withAuthentication(TEST_USER_NAME, TEST_USER_PASSWORD)
            .unassignUserTask(userTaskKey);

    // then
    assertThat(response).isNotNull();
    assertThat(response.statusCode()).isEqualTo(200);
    ensureUserTaskIsUnassigned(userTaskKey);
  }

  @Test
  public void shouldUnassignJobBasedUserTask() {
    // when
    final var response =
        tasklistRestClient
            .withAuthentication(TEST_USER_NAME, TEST_USER_PASSWORD)
            .unassignUserTask(userTaskKeyWithJobBasedUserTask);

    // then
    assertThat(response).isNotNull();
    assertThat(response.statusCode()).isEqualTo(200);
    ensureUserTaskIsUnassigned(userTaskKeyWithJobBasedUserTask);
  }

  private void deployResource(final ZeebeClient zeebeClient, final String resource) {
    zeebeClient.newDeployResourceCommand().addResourceFromClasspath(resource).send().join();
  }

  private void waitForProcessToBeDeployed(final String processDefinitionId) {
    Awaitility.await("should deploy process %s and export".formatted(processDefinitionId))
        .atMost(Duration.ofSeconds(15))
        .ignoreExceptions() // Ignore exceptions and continue retrying
        .untilAsserted(
            () -> {
              final var result = tasklistRestClient.searchProcesses(processDefinitionId);
              assertThat(result.hits()).hasSize(1);
            });
  }

  public static long createProcessInstance(final String processDefinitionId) {
    return zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId(processDefinitionId)
        .latestVersion()
        .send()
        .join()
        .getProcessInstanceKey();
  }

  public static long awaitUserTaskBeingAvailable(final long processInstanceKey) {
    final AtomicLong userTaskKey = new AtomicLong();
    Awaitility.await("should create an user task")
        .atMost(Duration.ofSeconds(60))
        .ignoreExceptions() // Ignore exceptions and continue retrying
        .untilAsserted(
            () -> {
              final var result =
                  tasklistRestClient
                      .withAuthentication(TEST_USER_NAME, TEST_USER_PASSWORD)
                      .searchUserTasks(
                          SearchQueryBuilders.term(
                              TaskTemplate.PROCESS_INSTANCE_ID, processInstanceKey));
              assertThat(result.hits()).hasSize(1);
              userTaskKey.set(result.hits().getFirst().source().getKey());
            });
    return userTaskKey.get();
  }

  public static void ensureUserTaskIsUnassigned(final long userTaskKey) {
    final var userTaskQuery = SearchQueryBuilders.term(TaskTemplate.KEY, userTaskKey);
    final var existsAssigneeQuery =
        SearchQueryBuilders.not(
            SearchQueryBuilders.exists(e -> e.field(TaskTemplate.ASSIGNEE)).toSearchQuery());
    final var finalQuery = SearchQueryBuilders.and(existsAssigneeQuery, userTaskQuery);

    Awaitility.await("should unassign the user task")
        .atMost(Duration.ofSeconds(60))
        .ignoreExceptions() // Ignore exceptions and continue retrying
        .untilAsserted(
            () -> {
              final var result = tasklistRestClient.searchUserTasks(finalQuery);
              assertThat(result.totalHits()).isGreaterThanOrEqualTo(1L);
            });
  }
}
