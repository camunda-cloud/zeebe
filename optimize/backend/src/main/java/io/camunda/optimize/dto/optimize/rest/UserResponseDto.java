/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.rest;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.camunda.optimize.dto.optimize.UserDto;
import java.util.List;
import lombok.Data;

@Data
public class UserResponseDto {

  @JsonUnwrapped private UserDto userDto;
  private List<AuthorizationType> authorizations;

  public UserResponseDto(UserDto userDto, List<AuthorizationType> authorizations) {
    this.userDto = userDto;
    this.authorizations = authorizations;
  }

  public UserResponseDto() {}
}
