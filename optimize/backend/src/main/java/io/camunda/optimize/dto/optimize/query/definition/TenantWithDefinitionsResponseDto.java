/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.definition;

import io.camunda.optimize.dto.optimize.SimpleDefinitionDto;
import java.util.List;
import lombok.Data;
import lombok.NonNull;

@Data
public class TenantWithDefinitionsResponseDto {

  private String id;
  private String name;
  @NonNull private List<SimpleDefinitionDto> definitions;

  public TenantWithDefinitionsResponseDto(
      String id, String name, @NonNull List<SimpleDefinitionDto> definitions) {
    this.id = id;
    this.name = name;
    this.definitions = definitions;
  }

  protected TenantWithDefinitionsResponseDto() {}
}
