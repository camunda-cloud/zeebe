/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize;

import lombok.Data;

@Data
// supposed to be called from entity specific subclasses only
public class AuthorizedEntityDto {

  private RoleType currentUserRole;

  protected AuthorizedEntityDto(RoleType currentUserRole) {
    this.currentUserRole = currentUserRole;
  }

  protected AuthorizedEntityDto() {}
}
