/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query;

import io.camunda.optimize.dto.optimize.query.entity.EntityType;
import lombok.Data;

@Data
public class EntityIdResponseDto {

  private String id;
  private EntityType entityType;

  public EntityIdResponseDto(String id, EntityType entityType) {
    this.id = id;
    this.entityType = entityType;
  }

  protected EntityIdResponseDto() {}
}
