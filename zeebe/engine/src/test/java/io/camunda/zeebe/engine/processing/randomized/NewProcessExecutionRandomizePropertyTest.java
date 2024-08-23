/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.randomized;

import io.camunda.process.generator.BpmnFeatureType;
import io.camunda.process.generator.BpmnProcessGenerator;
import io.camunda.process.generator.BpmnProcessGenerator.GeneratedProcess;
import io.camunda.process.generator.GeneratorConfiguration;
import io.camunda.process.generator.execution.ProcessExecutionStep;
import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NewProcessExecutionRandomizePropertyTest {

  public static final int AMOUNT_OF_PROCESSES = 1000;
  private static final BpmnProcessGenerator BPMN_GENERATOR =
      new BpmnProcessGenerator(
          new GeneratorConfiguration()
              .withFeatures(BpmnFeatureType.EMBEDDED_SUBPROCESS, BpmnFeatureType.BOUNDARY_EVENT)
              .excludeFeatures(BpmnFeatureType.COMPENSATION_EVENT));
  @Rule public final EngineRule engineRule = EngineRule.singlePartition();
  @Parameter public GeneratedProcess generatedProcess;

  @Rule
  public TestWatcher failedTestPrinter =
      new FailedRandomProcessTestPrinter(this::getGeneratedProcess);

  private GeneratedProcess getGeneratedProcess() {
    return generatedProcess;
  }

  @Test
  public void shouldExecuteProcessToEnd() {
    // given - generate and deploy a process
    final var engineRuleProcessExecutor = new EngineRuleProcessExecutor(engineRule);
    engineRule.deployment().withXmlResource(generatedProcess.process()).deploy();

    // when - executing all execution steps
    System.out.println("----------");
    for (final ProcessExecutionStep executionStep : generatedProcess.executionPath()) {
      System.out.println(executionStep.description());
      engineRuleProcessExecutor.execute(executionStep);
    }
    System.out.println("----------");

    // then - process should be completed
    RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_COMPLETED)
        .withElementType(BpmnElementType.PROCESS)
        .withBpmnProcessId(generatedProcess.processId())
        .await();
  }

  @Parameters(name = "{0}")
  public static Collection<GeneratedProcess> processes() {
    return IntStream.range(0, AMOUNT_OF_PROCESSES)
        .mapToObj(i -> BPMN_GENERATOR.generateProcess(ThreadLocalRandom.current().nextLong()))
        .toList();
    //    return List.of(BPMN_GENERATOR.generateProcess(123L));
  }
}
