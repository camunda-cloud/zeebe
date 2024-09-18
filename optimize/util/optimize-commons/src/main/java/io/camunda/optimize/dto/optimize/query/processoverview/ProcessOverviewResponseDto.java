/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.processoverview;

import java.util.List;
import lombok.Data;

@Data
public class ProcessOverviewResponseDto {

  private String processDefinitionName;
  private String processDefinitionKey;
  private ProcessOwnerResponseDto owner;
  private ProcessDigestResponseDto digest;
  private List<KpiResultDto> kpis;

  public ProcessOverviewResponseDto(
      String processDefinitionName,
      String processDefinitionKey,
      ProcessOwnerResponseDto owner,
      ProcessDigestResponseDto digest,
      List<KpiResultDto> kpis) {
    this.processDefinitionName = processDefinitionName;
    this.processDefinitionKey = processDefinitionKey;
    this.owner = owner;
    this.digest = digest;
    this.kpis = kpis;
  }

  public ProcessOverviewResponseDto() {}

  public static final class Fields {

    public static final String processDefinitionName = "processDefinitionName";
    public static final String processDefinitionKey = "processDefinitionKey";
    public static final String owner = "owner";
    public static final String digest = "digest";
    public static final String kpis = "kpis";
  }
}
