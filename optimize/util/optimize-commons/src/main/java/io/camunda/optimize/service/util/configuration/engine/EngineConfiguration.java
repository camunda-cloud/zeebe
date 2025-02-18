/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util.configuration.engine;

public class EngineConfiguration {

  private boolean importEnabled = false;

  public EngineConfiguration(final boolean importEnabled) {
    this.importEnabled = importEnabled;
  }

  protected EngineConfiguration() {}

  public boolean isImportEnabled() {
    return importEnabled;
  }

  public void setImportEnabled(final boolean importEnabled) {
    this.importEnabled = importEnabled;
  }

  protected boolean canEqual(final Object other) {
    return other instanceof EngineConfiguration;
  }

  @Override
  public int hashCode() {
    return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(final Object o) {
    return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public String toString() {
    return "EngineConfiguration(importEnabled=" + isImportEnabled() + ")";
  }

  private static boolean defaultImportEnabled() {
    return false;
  }

  public static EngineConfigurationBuilder builder() {
    return new EngineConfigurationBuilder();
  }

  public static class EngineConfigurationBuilder {

    private boolean importEnabledValue;
    private boolean importEnabledSet;

    EngineConfigurationBuilder() {}

    public EngineConfigurationBuilder importEnabled(final boolean importEnabled) {
      importEnabledValue = importEnabled;
      importEnabledSet = true;
      return this;
    }

    public EngineConfiguration build() {
      boolean importEnabledValue = this.importEnabledValue;
      if (!importEnabledSet) {
        importEnabledValue = EngineConfiguration.defaultImportEnabled();
      }
      return new EngineConfiguration(importEnabledValue);
    }

    @Override
    public String toString() {
      return "EngineConfiguration.EngineConfigurationBuilder(importEnabledValue="
          + importEnabledValue
          + ")";
    }
  }
}
