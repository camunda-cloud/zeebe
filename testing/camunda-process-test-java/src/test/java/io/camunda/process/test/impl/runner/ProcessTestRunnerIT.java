/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.process.test.impl.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.api.search.response.ProcessInstanceState;
import io.camunda.process.test.impl.dsl.TestCase;
import io.camunda.process.test.impl.dsl.TestResource;
import io.camunda.process.test.impl.dsl.TestSpecification;
import io.camunda.process.test.impl.dsl.actions.CompleteJobAction;
import io.camunda.process.test.impl.dsl.actions.CreateProcessInstanceAction;
import io.camunda.process.test.impl.dsl.verifications.ProcessInstanceStateVerification;
import io.camunda.process.test.impl.runtime.CamundaContainerRuntime;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessTestRunnerIT {

  private final ObjectMapper objectMapper =
      new ObjectMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

  private ProcessTestRunner processTestRunner;

  @BeforeEach
  void createProcessTestRunner() {
    processTestRunner =
        new ProcessTestRunner(
            () -> CamundaContainerRuntime.newBuilder().build(),
            CamundaContainerRuntime::start,
            camundaContainerRuntime -> {
              try {
                camundaContainerRuntime.close();
              } catch (final Exception e) {
                e.printStackTrace();
              }
            });
  }

  @Test
  void shouldRunTestSpecification() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask("A", task -> task.zeebeJobType("A"))
            .endEvent()
            .done();

    final List<TestResource> testResources =
        Arrays.asList(
            new TestResource(
                "process.bpmn", Bpmn.convertToString(process).getBytes(StandardCharsets.UTF_8)));

    final TestCase testCase1 =
        new TestCase(
            "case-1",
            Arrays.asList(
                new CreateProcessInstanceAction("process", "{}", "process-instance"),
                new CompleteJobAction("A", "{}"),
                new ProcessInstanceStateVerification(
                    "process-instance", ProcessInstanceState.COMPLETED)));

    final TestCase testCase2 =
        new TestCase(
            "case-2",
            Arrays.asList(
                new CreateProcessInstanceAction("process", "{}", "process-instance"),
                new ProcessInstanceStateVerification(
                    "process-instance", ProcessInstanceState.COMPLETED)));

    final TestSpecification testSpecification =
        new TestSpecification(testResources, Arrays.asList(testCase1, testCase2));

    // when
    final TestSpecificationResult result = processTestRunner.run(testSpecification);

    // then
    assertThat(result.getPassesTestCases()).isEqualTo(1);
    assertThat(result.getTotalTestCases()).isEqualTo(2);
    assertThat(result.getTotalTestDuration()).isGreaterThan(Duration.ZERO);

    assertThat(result.getTestResults())
        .hasSize(2)
        .extracting(TestCaseResult::getName, TestCaseResult::isSuccess)
        .containsSequence(tuple("case-1", true), tuple("case-2", false));

    final TestCaseResult failedTestCase = result.getTestResults().get(1);
    assertThat(failedTestCase.isSuccess()).isFalse();
    assertThat(failedTestCase.getFailedInstruction()).isNotNull();
    assertThat(failedTestCase.getFailureMessage())
        .isNotEmpty()
        .contains("should be completed but was active");
    assertThat(failedTestCase.getTestDuration()).isGreaterThan(Duration.ZERO);
    assertThat(failedTestCase.getTestOutput()).hasSize(1);
  }

  @Test
  void shouldRunSerializedTestSpecification() throws IOException {
    //
    final InputStream serializedTestSpecification =
        getClass().getResourceAsStream("/test-specification/test-spec-1.txt");
    assertThat(serializedTestSpecification).describedAs("Test resource not found").isNotNull();

    final TestSpecification testSpecification = parse(serializedTestSpecification);

    // when
    final TestSpecificationResult result = processTestRunner.run(testSpecification);

    // then
    assertThat(result.getTestResults())
        .hasSize(1)
        .extracting(TestCaseResult::getName, TestCaseResult::isSuccess)
        .containsSequence(tuple("case-1", true));
  }

  private TestSpecification parse(final InputStream testSpecification) throws IOException {
    return objectMapper.readValue(testSpecification, TestSpecification.class);
  }
}
