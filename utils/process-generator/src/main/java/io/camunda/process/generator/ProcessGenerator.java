/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.process.generator;

import io.camunda.process.generator.BpmnProcessGenerator.GeneratedProcess;
import io.camunda.process.generator.execution.CreateProcessInstanceStep;
import io.camunda.process.generator.template.BpmnTemplateGenerator;
import io.camunda.process.generator.template.BpmnTemplateGeneratorFactory;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.builder.AbstractFlowNodeBuilder;
import io.camunda.zeebe.model.bpmn.impl.BpmnModelConstants;
import io.camunda.zeebe.model.bpmn.instance.Definitions;

public class ProcessGenerator {

  private final String camundaVersion;
  private final BpmnFactories bpmnFactories;

  public ProcessGenerator(final String camundaVersion, final BpmnFactories bpmnFactories) {
    this.camundaVersion = camundaVersion;
    this.bpmnFactories = bpmnFactories;
  }

  public GeneratedProcess generateProcess(final GeneratorContext generatorContext) {
    final String processId = "process_" + generatorContext.getSeed();
    AbstractFlowNodeBuilder<?, ?> processBuilder =
        Bpmn.createExecutableProcess(processId).name(processId).startEvent();
    generatorContext.addExecutionStep(new CreateProcessInstanceStep(processId, processId));

    final BpmnTemplateGeneratorFactory templateGeneratorFactory =
        bpmnFactories.getTemplateGeneratorFactory();

    final var templateLimit = 3;
    for (int i = 0; i < templateLimit; i++) {
      final BpmnTemplateGenerator templateGenerator = templateGeneratorFactory.getMiddleGenerator();
      processBuilder = templateGenerator.addElement(processBuilder, true);
    }

    final BpmnModelInstance process =
        templateGeneratorFactory.getFinalGenerator().addElement(processBuilder, true).done();

    // modify the version so I can open the process in the Camunda Modeler
    final Definitions definitions = process.getDefinitions();
    definitions.setExporterVersion(camundaVersion);
    definitions.setAttributeValueNs(
        BpmnModelConstants.MODELER_NS, "executionPlatformVersion", camundaVersion);

    return new GeneratedProcess(
        process, generatorContext.getExecutionPath(), processId, generatorContext.getSeed());
  }
}
