/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.filter;

import io.camunda.util.ObjectBuilder;
import io.camunda.zeebe.protocol.record.value.PermissionType;

public record AuthorizationFilter(
    Long ownerKey,
    String ownerType,
    String resourceKey,
    String resourceType,
    PermissionType permissionType)
    implements FilterBase {
  public static final class Builder implements ObjectBuilder<AuthorizationFilter> {
    private Long ownerKey;
    private String ownerType;
    private String resourceKey;
    private String resourceType;
    private PermissionType permissionType;

    public Builder ownerKey(final Long value) {
      ownerKey = value;
      return this;
    }

    public Builder ownerType(final String value) {
      ownerType = value;
      return this;
    }

    public Builder resourceKey(final String value) {
      resourceKey = value;
      return this;
    }

    public Builder resourceType(final String value) {
      resourceType = value;
      return this;
    }

    public Builder permissionType(final PermissionType value) {
      permissionType = value;
      return this;
    }

    @Override
    public AuthorizationFilter build() {
      return new AuthorizationFilter(
          ownerKey, ownerType, resourceKey, resourceType, permissionType);
    }
  }
}
