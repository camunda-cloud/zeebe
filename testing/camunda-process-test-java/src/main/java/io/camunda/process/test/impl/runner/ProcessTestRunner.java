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

import io.camunda.client.CamundaClient;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.impl.assertions.CamundaDataSource;
import io.camunda.process.test.impl.dsl.TestAction;
import io.camunda.process.test.impl.dsl.TestCase;
import io.camunda.process.test.impl.dsl.TestInstruction;
import io.camunda.process.test.impl.dsl.TestResource;
import io.camunda.process.test.impl.dsl.TestSpecification;
import io.camunda.process.test.impl.dsl.TestVerification;
import io.camunda.process.test.impl.extension.CamundaProcessTestContextImpl;
import io.camunda.process.test.impl.runtime.CamundaContainerRuntime;
import io.camunda.process.test.impl.testresult.CamundaProcessTestResultCollector;
import io.camunda.process.test.impl.testresult.ProcessTestResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessTestRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTestRunner.class);

  private final Supplier<CamundaContainerRuntime> runtimeSupplier;
  private final Consumer<CamundaContainerRuntime> beforeEachCallback;
  private final Consumer<CamundaContainerRuntime> afterEachCallback;

  public ProcessTestRunner(
      final Supplier<CamundaContainerRuntime> runtimeSupplier,
      final Consumer<CamundaContainerRuntime> beforeEachCallback,
      final Consumer<CamundaContainerRuntime> afterEachCallback) {
    this.runtimeSupplier = runtimeSupplier;
    this.beforeEachCallback = beforeEachCallback;
    this.afterEachCallback = afterEachCallback;
  }

  public TestSpecificationResult run(final TestSpecification testSpecification) {

    final Instant startTime = Instant.now();

    final List<TestCaseResult> testResults =
        testSpecification.getTestCases().stream()
            .map(testCase -> runTestCase(testCase, testSpecification.getTestResources()))
            .collect(Collectors.toList());

    final int passedTestCases =
        (int) testResults.stream().filter(TestCaseResult::isSuccess).count();
    final int totalTestCases = testResults.size();
    final Duration totalTestDuration = Duration.between(startTime, Instant.now());

    return new TestSpecificationResult(
        passedTestCases, totalTestCases, totalTestDuration, testResults);
  }

  private TestCaseResult runTestCase(
      final TestCase testCase, final List<TestResource> testResources) {

    final Instant startTime = Instant.now();

    final TestCaseResult testCaseResult = new TestCaseResult();
    testCaseResult.setName(testCase.getName());
    testCaseResult.setSuccess(true);

    try {
      // 1: bootstrap
      final CamundaContainerRuntime containerRuntime = runtimeSupplier.get();
      beforeEachCallback.accept(containerRuntime);

      final List<CamundaClient> createdClients = new ArrayList<>();

      final CamundaProcessTestContext camundaProcessTestContext =
          new CamundaProcessTestContextImpl(
              containerRuntime.getCamundaContainer(),
              containerRuntime.getConnectorsContainer(),
              createdClients::add);

      final CamundaDataSource camundaDataSource =
          new CamundaDataSource(
              containerRuntime.getCamundaContainer().getRestApiAddress().toString());

      final CamundaProcessTestResultCollector processTestResultCollector =
          new CamundaProcessTestResultCollector(camundaDataSource);

      final CamundaClient camundaClient = camundaProcessTestContext.createClient();

      final TestContext testContext = new TestContext(camundaDataSource, camundaClient);

      // 2: deploy resources
      try {
        testResources.forEach(
            testResource ->
                camundaClient
                    .newDeployResourceCommand()
                    .addResourceBytes(testResource.getResource(), testResource.getName())
                    .send()
                    .join());
      } catch (final Exception e) {
        testCaseResult.setSuccess(false);
        testCaseResult.setFailureMessage("Failed to deploy test resources: " + e.getMessage());
        return testCaseResult;
      }

      // 3: apply instructions
      final VerificationResult verificationResult =
          runInstructions(testCase.getInstructions(), testContext, camundaProcessTestContext);

      final Duration testDuration = Duration.between(startTime, Instant.now());
      testCaseResult.setTestDuration(testDuration);

      // 4: collect output
      final ProcessTestResult processTestResult = processTestResultCollector.collect();

      // 5: shutdown
      createdClients.forEach(CamundaClient::close);
      afterEachCallback.accept(containerRuntime);

      if (!verificationResult.isSuccessful()) {
        testCaseResult.setSuccess(false);
        testCaseResult.setFailedInstruction(verificationResult.getInstruction());
        testCaseResult.setFailureMessage(verificationResult.getFailureMessage());
        testCaseResult.setTestOutput(processTestResult.getProcessInstanceTestResults());
      }

    } catch (final Exception e) {
      testCaseResult.setSuccess(false);
      testCaseResult.setFailureMessage("Failed to run test case: " + e.getMessage());

      LOGGER.warn("Failed to run test case: {}.", testCase.getName(), e);
    }

    return testCaseResult;
  }

  private VerificationResult runInstructions(
      final List<TestInstruction> instructions,
      final TestContext testContext,
      final CamundaProcessTestContext processTestContext) {

    for (final TestInstruction instruction : instructions) {
      if (instruction instanceof TestAction) {
        final TestAction action = (TestAction) instruction;

        try {
          action.execute(testContext, processTestContext);
        } catch (final Exception e) {
          return new VerificationResult(instruction, false, e.getMessage());
        }

      } else if (instruction instanceof TestVerification) {
        final TestVerification verification = (TestVerification) instruction;

        try {
          verification.verify(testContext, processTestContext);

        } catch (final AssertionError e) {
          return new VerificationResult(instruction, false, e.getMessage());
        }
      }
    }

    return new VerificationResult(null, true, "<success>");
  }
}
