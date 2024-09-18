/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.persistence;

import io.camunda.optimize.dto.optimize.OptimizeDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class BusinessKeyDto implements OptimizeDto {

  private String processInstanceId;
  private String businessKey;

  public BusinessKeyDto(String processInstanceId, String businessKey) {
    this.processInstanceId = processInstanceId;
    this.businessKey = businessKey;
  }

  public BusinessKeyDto() {}

  public static final class Fields {

    public static final String processInstanceId = "processInstanceId";
    public static final String businessKey = "businessKey";
  }
}
