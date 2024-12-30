/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.security.configuration;

public class AuthorizationsConfiguration {

  private static final boolean DEFAULT_AUTHORIZATIONS_ENABLED = false;

  private boolean enabled = DEFAULT_AUTHORIZATIONS_ENABLED;
  private OidcConfiguration oidc = new OidcConfiguration();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public OidcConfiguration getOidc() {
    return oidc;
  }

  public void setOidc(final OidcConfiguration oidc) {
    this.oidc = oidc;
  }
}
