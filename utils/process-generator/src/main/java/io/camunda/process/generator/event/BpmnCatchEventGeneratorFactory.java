/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.process.generator.event;

import io.camunda.process.generator.FactoryUtil;
import io.camunda.process.generator.GeneratorContext;
import java.util.List;

public class BpmnCatchEventGeneratorFactory {

  private final GeneratorContext generatorContext;

  private final List<BpmnCatchEventGenerator> generators =
      List.of(new MessageCatchEventGenerator(), new SignalCatchEventGenerator());

  public BpmnCatchEventGeneratorFactory(final GeneratorContext generatorContext) {
    this.generatorContext = generatorContext;
  }

  public BpmnCatchEventGenerator getGenerator() {
    return FactoryUtil.getGenerator(generators, generatorContext);
  }
}
