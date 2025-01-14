/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.Process;
import io.camunda.client.api.search.response.ProcessDefinition;
import io.camunda.it.utils.BrokerITInvocationProvider;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BrokerITInvocationProvider.class)
public class ProcessDefinitionQueryTest {

  private static final List<String> PROCESS_DEFINITIONS = List.of(
      "service_tasks_v1.bpmn",
      "service_tasks_v2.bpmn",
      "incident_process_v1.bpmn",
      "manual_process.bpmn",
      "parent_process_v1.bpmn",
      "child_process_v1.bpmn",
      "process_start_form.bpmn",
      "processWithVersionTag.bpmn"
  );

  public List<Process> deployResources(final CamundaClient camundaClient) throws InterruptedException {
    // Deploy form
    deployResource(camundaClient, String.format("form/%s", "form.form"));
    deployResource(camundaClient, String.format("form/%s", "form_v2.form"));

    final var processes = PROCESS_DEFINITIONS.parallelStream()
        .map(process -> deployResource(camundaClient, String.format("process/%s", process)))
        .map(DeploymentEvent::getProcesses)
        .flatMap(List::stream)
        .toList();

    waitForProcessesToBeDeployed(camundaClient, processes);

    return processes;
  }

  @TestTemplate
  void shouldSearchByFromWithLimit(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAll = camundaClient.newProcessDefinitionQuery().send().join();
    final var thirdKey = resultAll.items().get(2).getProcessDefinitionKey();

    final var resultSearchFrom =
        camundaClient.newProcessDefinitionQuery().page(p -> p.limit(2).from(2)).send().join();

    // then
    assertThat(resultSearchFrom.items().size()).isEqualTo(2);
    assertThat(resultSearchFrom.items().stream().findFirst().get().getProcessDefinitionKey())
        .isEqualTo(thirdKey);
  }

  @TestTemplate
  void shouldPaginateWithSortingByProcessDefinitionKey(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAll =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionKey().desc())
            .send()
            .join();

    final var firstPage =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionKey().desc())
            .page(p -> p.limit(1))
            .send()
            .join();
    final var secondPage =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionKey().desc())
            .page(p -> p.limit(1).searchAfter(firstPage.page().lastSortValues()))
            .send()
            .join();

    // then
    assertThat(firstPage.items().size()).isEqualTo(1);
    assertThat(firstPage.items().getFirst().getProcessDefinitionKey())
        .isEqualTo(resultAll.items().get(0).getProcessDefinitionKey());
    assertThat(secondPage.items().size()).isEqualTo(1);
    assertThat(secondPage.items().getFirst().getProcessDefinitionKey())
        .isEqualTo(resultAll.items().get(1).getProcessDefinitionKey());
  }

  @TestTemplate
  void shouldPaginateWithSortingByProcessDefinitionId(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAll =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().desc())
            .send()
            .join();

    final var firstPage =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().desc())
            .page(p -> p.limit(2))
            .send()
            .join();
    final var secondPage =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().desc())
            .page(p -> p.limit(1).searchAfter(firstPage.page().lastSortValues()))
            .send()
            .join();

    // then
    assertThat(firstPage.items().size()).isEqualTo(2);
    assertThat(firstPage.items().getFirst().getProcessDefinitionKey())
        .isEqualTo(resultAll.items().get(0).getProcessDefinitionKey());
    assertThat(firstPage.items().getLast().getProcessDefinitionKey())
        .isEqualTo(resultAll.items().get(1).getProcessDefinitionKey());
    assertThat(secondPage.items().size()).isEqualTo(1);
    assertThat(secondPage.items().getFirst().getProcessDefinitionKey())
        .isEqualTo(resultAll.items().get(2).getProcessDefinitionKey());
  }

  @TestTemplate
  void shouldGetPreviousPageWithSortingByProcessDefinitionId(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var firstPage =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().desc())
            .page(p -> p.limit(2))
            .send()
            .join();
    final var secondPage =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().desc())
            .page(p -> p.limit(1).searchAfter(firstPage.page().lastSortValues()))
            .send()
            .join();
    final var firstPageAgain =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().desc())
            .page(p -> p.limit(2).searchBefore(secondPage.page().firstSortValues()))
            .send()
            .join();

    // then
    assertThat(firstPageAgain.items()).isEqualTo(firstPage.items());
  }

  @TestTemplate
  void shouldThrownExceptionIfProcessDefinitionNotFoundByKey(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final long invalidProcessDefinitionKey = 0xC00L;

    // when / then
    final var exception =
        assertThrowsExactly(
            ProblemException.class,
            () ->
                camundaClient
                    .newProcessDefinitionGetRequest(invalidProcessDefinitionKey)
                    .send()
                    .join());
    assertThat(exception.getMessage()).startsWith("Failed with code 404");
    assertThat(exception.details()).isNotNull();
    assertThat(exception.details().getTitle()).isEqualTo("NOT_FOUND");
    assertThat(exception.details().getStatus()).isEqualTo(404);
    assertThat(exception.details().getDetail())
        .isEqualTo("Process definition with key 3072 not found");
  }

  @TestTemplate
  void shouldGetProcessDefinitionByKey(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    final var processes = deployResources(camundaClient);

    final var processDefinitionId = "service_tasks_v1";
    final var processEvent =
        processes.stream()
            .filter(p -> Objects.equals(processDefinitionId, p.getBpmnProcessId()))
            .findFirst()
            .orElseThrow();
    final var processDefinitionKey = processEvent.getProcessDefinitionKey();

    // when
    final var result =
        camundaClient.newProcessDefinitionGetRequest(processDefinitionKey).send().join();

    // then
    assertThat(result).isNotNull();
    assertThat(result.getProcessDefinitionKey()).isEqualTo(processDefinitionKey);
    assertThat(result.getProcessDefinitionId()).isEqualTo(processDefinitionId);
    assertThat(result.getName()).isEqualTo("Service tasks v1");
    assertThat(result.getVersion()).isEqualTo(1);
    assertThat(result.getResourceName()).isEqualTo("process/service_tasks_v1.bpmn");
    assertThat(result.getTenantId()).isEqualTo("<default>");
    assertThat(result.getVersionTag()).isNull();
  }

  @TestTemplate
  void shouldRetrieveAllProcessDefinitionsByDefault(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    final var processes = deployResources(camundaClient);

    final var expectedProcessDefinitionIds =
        processes.stream().map(Process::getBpmnProcessId).toList();

    // when
    final var result = camundaClient.newProcessDefinitionQuery().send().join();

    // then
    assertThat(result.items().size()).isEqualTo(expectedProcessDefinitionIds.size());
    assertThat(result.items().stream().map(ProcessDefinition::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrderElementsOf(expectedProcessDefinitionIds);
  }

  @TestTemplate
  void shouldRetrieveProcessDefinitionsByProcessDefinitionId(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final var processDefinitionId = "service_tasks_v1";

    // when
    final var result =
        camundaClient
            .newProcessDefinitionQuery()
            .filter(f -> f.processDefinitionId(processDefinitionId))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().getFirst().getProcessDefinitionId()).isEqualTo(processDefinitionId);
  }

  @TestTemplate
  void shouldRetrieveProcessDefinitionsByName(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final var name = "Service tasks v1";

    // when
    final var result =
        camundaClient.newProcessDefinitionQuery().filter(f -> f.name(name)).send().join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().getFirst().getName()).isEqualTo(name);
  }

  @TestTemplate
  void shouldRetrieveProcessDefinitionsByVersion(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final var version = 1;

    // when
    final var result =
        camundaClient.newProcessDefinitionQuery().filter(f -> f.version(version)).send().join();

    // then
    assertThat(result.items().size()).isEqualTo(8);
    assertThat(result.items().getFirst().getVersion()).isEqualTo(version);
  }

  @TestTemplate
  void shouldRetrieveProcessDefinitionsByResourceName(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final var resourceName = "process/service_tasks_v1.bpmn";

    // when
    final var result =
        camundaClient
            .newProcessDefinitionQuery()
            .filter(f -> f.resourceName(resourceName))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().getFirst().getResourceName()).isEqualTo(resourceName);
  }

  @TestTemplate
  void shouldRetrieveProcessDefinitionsByTenantId(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final var tenantId = "<default>";

    // when
    final var result =
        camundaClient.newProcessDefinitionQuery().filter(f -> f.tenantId(tenantId)).send().join();

    // then
    assertThat(result.items().size()).isEqualTo(8);
    assertThat(result.items().getFirst().getTenantId()).isEqualTo(tenantId);
  }

  @TestTemplate
  void shouldRetrieveProcessDefinitionsByNullVersionTag(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final String versionTag = null;

    // when
    final var result =
        camundaClient
            .newProcessDefinitionQuery()
            .filter(f -> f.versionTag(versionTag))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(8);
    assertThat(result.items().getFirst().getVersionTag()).isEqualTo(versionTag);
  }

  @TestTemplate
  void shouldRetrieveProcessDefinitionsByVersionTag(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final String versionTag = "1.1.0";

    // when
    final var result =
        camundaClient
            .newProcessDefinitionQuery()
            .filter(f -> f.versionTag(versionTag))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().getFirst().getVersionTag()).isEqualTo(versionTag);
  }

  @TestTemplate
  void shouldRetrieveProcessDefinitionsWithReverseSorting(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    final var processes = deployResources(camundaClient);

    final var expectedProcessDefinitionIds =
        processes.stream()
            .map(Process::getBpmnProcessId)
            .sorted(Comparator.reverseOrder())
            .toList();

    // when
    final var result =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().desc())
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(8);
    assertThat(result.items().stream().map(ProcessDefinition::getProcessDefinitionId).toList())
        .containsExactlyElementsOf(expectedProcessDefinitionIds);
  }

  @TestTemplate
  void shouldSortProcessDefinitionsByKey(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAsc =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionKey().asc())
            .send()
            .join();
    final var resultDesc =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionKey().desc())
            .send()
            .join();

    final var all =
        resultAsc.items().stream().map(ProcessDefinition::getProcessDefinitionKey).toList();
    final var sortedAsc = all.stream().sorted(Comparator.naturalOrder()).toList();
    final var sortedDesc = all.stream().sorted(Comparator.reverseOrder()).toList();

    // then
    assertThat(resultAsc.items().stream().map(ProcessDefinition::getProcessDefinitionKey).toList())
        .containsExactlyElementsOf(sortedAsc);
    assertThat(resultDesc.items().stream().map(ProcessDefinition::getProcessDefinitionKey).toList())
        .containsExactlyElementsOf(sortedDesc);
  }

  @TestTemplate
  void shouldSortProcessDefinitionsByProcessDefinitionId(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAsc =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().asc())
            .send()
            .join();
    final var resultDesc =
        camundaClient
            .newProcessDefinitionQuery()
            .sort(s -> s.processDefinitionId().desc())
            .send()
            .join();

    final var all =
        resultAsc.items().stream().map(ProcessDefinition::getProcessDefinitionId).toList();
    final var sortedAsc = all.stream().sorted(Comparator.naturalOrder()).toList();
    final var sortedDesc = all.stream().sorted(Comparator.reverseOrder()).toList();

    // then
    assertThat(resultAsc.items().stream().map(ProcessDefinition::getProcessDefinitionId).toList())
        .containsExactlyElementsOf(sortedAsc);
    assertThat(resultDesc.items().stream().map(ProcessDefinition::getProcessDefinitionId).toList())
        .containsExactlyElementsOf(sortedDesc);
  }

  @TestTemplate
  void shouldSortProcessDefinitionsByName(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAsc =
        camundaClient.newProcessDefinitionQuery().sort(s -> s.name().asc()).send().join();
    final var resultDesc =
        camundaClient.newProcessDefinitionQuery().sort(s -> s.name().desc()).send().join();

    final var all = resultAsc.items().stream().map(ProcessDefinition::getName).toList();
    final var sortedAsc = all.stream().sorted(Comparator.naturalOrder()).toList();
    final var sortedDesc = all.stream().sorted(Comparator.reverseOrder()).toList();

    // then
    assertThat(resultAsc.items().stream().map(ProcessDefinition::getName).toList())
        .containsExactlyElementsOf(sortedAsc);
    assertThat(resultDesc.items().stream().map(ProcessDefinition::getName).toList())
        .containsExactlyElementsOf(sortedDesc);
  }

  @TestTemplate
  void shouldSortProcessDefinitionsByProcessResourceName(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAsc =
        camundaClient.newProcessDefinitionQuery().sort(s -> s.resourceName().asc()).send().join();
    final var resultDesc =
        camundaClient.newProcessDefinitionQuery().sort(s -> s.resourceName().desc()).send().join();

    final var all = resultAsc.items().stream().map(ProcessDefinition::getResourceName).toList();
    final var sortedAsc = all.stream().sorted(Comparator.naturalOrder()).toList();
    final var sortedDesc = all.stream().sorted(Comparator.reverseOrder()).toList();

    // then
    assertThat(resultAsc.items().stream().map(ProcessDefinition::getResourceName).toList())
        .containsExactlyElementsOf(sortedAsc);
    assertThat(resultDesc.items().stream().map(ProcessDefinition::getResourceName).toList())
        .containsExactlyElementsOf(sortedDesc);
  }

  @TestTemplate
  void shouldSortProcessDefinitionsByVersion(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAsc =
        camundaClient.newProcessDefinitionQuery().sort(s -> s.version().asc()).send().join();
    final var resultDesc =
        camundaClient.newProcessDefinitionQuery().sort(s -> s.version().desc()).send().join();

    final var all = resultAsc.items().stream().map(ProcessDefinition::getVersion).toList();
    final var sortedAsc = all.stream().sorted(Comparator.naturalOrder()).toList();
    final var sortedDesc = all.stream().sorted(Comparator.reverseOrder()).toList();

    // then
    assertThat(resultAsc.items().stream().map(ProcessDefinition::getVersion).toList())
        .containsExactlyElementsOf(sortedAsc);
    assertThat(resultDesc.items().stream().map(ProcessDefinition::getVersion).toList())
        .containsExactlyElementsOf(sortedDesc);
  }

  @TestTemplate
  void shouldSortProcessDefinitionsByTenantId(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    // when
    final var resultAsc =
        camundaClient.newProcessDefinitionQuery().sort(s -> s.tenantId().asc()).send().join();
    final var resultDesc =
        camundaClient.newProcessDefinitionQuery().sort(s -> s.tenantId().desc()).send().join();

    final var all = resultAsc.items().stream().map(ProcessDefinition::getTenantId).toList();
    final var sortedAsc = all.stream().sorted(Comparator.naturalOrder()).toList();
    final var sortedDesc = all.stream().sorted(Comparator.reverseOrder()).toList();

    // then
    assertThat(resultAsc.items().stream().map(ProcessDefinition::getTenantId).toList())
        .containsExactlyElementsOf(sortedAsc);
    assertThat(resultDesc.items().stream().map(ProcessDefinition::getTenantId).toList())
        .containsExactlyElementsOf(sortedDesc);
  }

  @TestTemplate
  public void shouldValidatePagination(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final var result =
        camundaClient.newProcessDefinitionQuery().page(p -> p.limit(2)).send().join();
    assertThat(result.items().size()).isEqualTo(2);
    final var key = result.items().getFirst().getProcessDefinitionKey();
    // apply searchAfter
    final var resultAfter =
        camundaClient
            .newProcessDefinitionQuery()
            .page(p -> p.searchAfter(Collections.singletonList(key)))
            .send()
            .join();

    assertThat(resultAfter.items().size()).isEqualTo(7);
    final var keyAfter = resultAfter.items().getFirst().getProcessDefinitionKey();
    // apply searchBefore
    final var resultBefore =
        camundaClient
            .newProcessDefinitionQuery()
            .page(p -> p.searchBefore(Collections.singletonList(keyAfter)))
            .send()
            .join();
    assertThat(result.items().size()).isEqualTo(2);
    assertThat(resultBefore.items().getFirst().getProcessDefinitionKey()).isEqualTo(key);
  }

  @TestTemplate
  public void shouldValidateGetProcessForm(final TestStandaloneBroker testBroker)
      throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployResources(camundaClient);

    final var resultProcess =
        camundaClient
            .newProcessDefinitionQuery()
            .filter(f -> f.name("Process With Form"))
            .send()
            .join();

    final var processDefinitionKey =
        resultProcess.items().stream().findFirst().get().getProcessDefinitionKey();

    final var resultForm =
        camundaClient.newProcessDefinitionGetFormRequest(processDefinitionKey).send().join();

    assertThat(resultForm.getFormId()).isEqualTo("test");
  }

  private static DeploymentEvent deployResource(
      final CamundaClient camundaClient, final String resourceName) {
    return camundaClient
        .newDeployResourceCommand()
        .addResourceFromClasspath(resourceName)
        .send()
        .join();
  }

  private static void waitForProcessesToBeDeployed(final CamundaClient camundaClient, final List<Process> processes)
      throws InterruptedException {
    Awaitility.await("should deploy processes and import in Operate")
        .atMost(Duration.ofMinutes(3))
        .ignoreExceptions() // Ignore exceptions and continue retrying
        .untilAsserted(
            () -> {
              final var result = camundaClient.newProcessDefinitionQuery().send().join();
              assertThat(result.items().size()).isEqualTo(processes.size());

              final var processDefinitionKey =
                  camundaClient
                      .newProcessDefinitionQuery()
                      .filter(f -> f.name("Process With Form"))
                      .send()
                      .join()
                      .items()
                      .get(0)
                      .getProcessDefinitionKey();

              final var resultForm =
                  camundaClient
                      .newProcessDefinitionGetFormRequest(processDefinitionKey)
                      .send()
                      .join();

              assertThat(resultForm.getFormId().equals("test"));
            });
  }
}
