/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.security.configuration;

public class SecurityConfiguration {
  private boolean enabled;

  private AuthorizationsConfiguration authorizations = new AuthorizationsConfiguration();
  private BasicAuthConfiguration basicAuth = new BasicAuthConfiguration();

  private InitializationConfiguration initialization = new InitializationConfiguration();

  private MultiTenancyConfiguration multiTenancy = new MultiTenancyConfiguration();

  public AuthorizationsConfiguration getAuthorizations() {
    return authorizations;
  }

  public void setAuthorizations(final AuthorizationsConfiguration authorizations) {
    this.authorizations = authorizations;
  }

  public InitializationConfiguration getInitialization() {
    return initialization;
  }

  public void setInitialization(final InitializationConfiguration initialization) {
    this.initialization = initialization;
  }

  public MultiTenancyConfiguration getMultiTenancy() {
    return multiTenancy;
  }

  public void setMultiTenancy(final MultiTenancyConfiguration multiTenancy) {
    this.multiTenancy = multiTenancy;
  }

  public BasicAuthConfiguration getBasicAuth() {
    return basicAuth;
  }

  public void setBasicAuth(final BasicAuthConfiguration basicAuth) {
    this.basicAuth = basicAuth;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }
}
