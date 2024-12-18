/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.exporter;

import io.camunda.client.ZeebeClient;
import io.camunda.client.api.command.DeployResourceCommandStep1;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.builder.UserTaskBuilder;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import org.awaitility.Awaitility;

public class ExporterTestUtil {

  public static final String RESOURCE_MANUAL_PROCESS = "process/manual_process.bpmn";
  public static final String PROCESS_ID_MANUAL_PROCESS = "manual_process";

  @SafeVarargs
  public static String createAndDeployUserTaskProcess(
      final ZeebeClient zeebeClient,
      final String processId,
      final String flowNodeBpmnId,
      final Consumer<UserTaskBuilder>... taskModifiers) {
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess(processId)
            .startEvent("start")
            .userTask(
                flowNodeBpmnId,
                task -> Arrays.stream(taskModifiers).forEach(modifier -> modifier.accept(task)))
            .endEvent()
            .done();
    return deployProcess(zeebeClient, process, processId);
  }

  @SafeVarargs
  public static String createAndDeployUserTaskProcess(
      final ZeebeClient zeebeClient,
      final String processId,
      final String flowNodeBpmnId,
      final String tenantId,
      final Consumer<UserTaskBuilder>... taskModifiers) {
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess(processId)
            .startEvent("start")
            .userTask(
                flowNodeBpmnId,
                task -> Arrays.stream(taskModifiers).forEach(modifier -> modifier.accept(task)))
            .endEvent()
            .done();
    return deployProcess(zeebeClient, process, processId, tenantId);
  }

  public static String deployProcess(
      final ZeebeClient client,
      final BpmnModelInstance processModel,
      final String resourceName,
      final String tenantId) {
    final DeployResourceCommandStep1.DeployResourceCommandStep2 deployProcessCommandStep1 =
        client
            .newDeployResourceCommand()
            .addProcessModel(processModel, resourceName + ".bpmn")
            .tenantId(tenantId);
    final DeploymentEvent deploymentEvent = deployProcessCommandStep1.send().join();
    return String.valueOf(deploymentEvent.getProcesses().getFirst().getProcessDefinitionKey());
  }

  public static String deployProcess(
      final ZeebeClient client, final BpmnModelInstance processModel, final String resourceName) {
    final DeployResourceCommandStep1.DeployResourceCommandStep2 deployProcessCommandStep1 =
        client.newDeployResourceCommand().addProcessModel(processModel, resourceName + ".bpmn");
    final DeploymentEvent deploymentEvent = deployProcessCommandStep1.send().join();
    return String.valueOf(deploymentEvent.getProcesses().getFirst().getProcessDefinitionKey());
  }

  public static long startProcessInstance(final ZeebeClient client, final String processId) {
    return client
        .newCreateInstanceCommand()
        .bpmnProcessId(processId)
        .latestVersion()
        .send()
        .join()
        .getProcessInstanceKey();
  }

  public static String startProcessInstanceWithStringReturn(final ZeebeClient client, final String processId) {
    return String.valueOf(startProcessInstance(client, processId));
  }

  public static String startProcessInstanceWithStringReturn(
      final ZeebeClient client, final String processId, final String tenantId) {
    return String.valueOf(
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .tenantId(tenantId)
            .send()
            .join()
            .getProcessInstanceKey());
  }

  public static String startProcessInstanceWithStringReturn(
      final ZeebeClient client,
      final String processId,
      final String tenantId,
      final Map<String, Object> variables) {
    return String.valueOf(
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .tenantId(tenantId)
            .variables(variables)
            .send()
            .join()
            .getProcessInstanceKey());
  }

  public static String startProcessInstanceWithStringReturn(
      final ZeebeClient client, final String processId, final Map<String, Object> variables) {
    return String.valueOf(
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .send()
            .join()
            .getProcessInstanceKey());
  }

  public static void waitForProcessTasks(
      final ZeebeClient client, final String processInstanceKey) {

    Awaitility.await()
        .ignoreExceptions()
        .timeout(Duration.ofSeconds(30))
        .until(
            () ->
                !client
                    .newUserTaskQuery()
                    .filter(f -> f.processInstanceKey(Long.valueOf(processInstanceKey)))
                    .send()
                    .join()
                    .items()
                    .isEmpty());
  }
}
