/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.process.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeneratorConfiguration {

  private final List<BpmnFeatureType> includeFeatures = new ArrayList<>();
  private final List<BpmnFeatureType> excludeFeatures = new ArrayList<>();
  private int maximumDepth = 3;
  private int maximumBranches = 3;

  public List<BpmnFeatureType> getIncludeFeatures() {
    return includeFeatures;
  }

  public List<BpmnFeatureType> getExcludeFeatures() {
    return excludeFeatures;
  }

  public GeneratorConfiguration withFeatures(final BpmnFeatureType... features) {
    includeFeatures.addAll(Arrays.asList(features));
    return this;
  }

  public GeneratorConfiguration excludeFeatures(final BpmnFeatureType... features) {
    excludeFeatures.addAll(Arrays.asList(features));
    return this;
  }

  public GeneratorConfiguration withMaximumDepth(final int maximumDepth) {
    this.maximumDepth = maximumDepth;
    return this;
  }

  public GeneratorConfiguration withMaximumBranches(final int maximumBranches) {
    this.maximumBranches = maximumBranches;
    return this;
  }

  public int getMaximumBranches() {
    return maximumBranches;
  }

  public int getMaximumDepth() {
    return maximumDepth;
  }
}
