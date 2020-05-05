/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.single;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.camunda.optimize.dto.optimize.query.report.Combinable;
import org.camunda.optimize.dto.optimize.query.report.ReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.SingleReportConfigurationDto;

import java.util.List;

@AllArgsConstructor
@FieldNameConstants(asEnum = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class SingleReportDataDto implements ReportDataDto, Combinable {

  @Getter
  @Setter
  @Builder.Default
  private SingleReportConfigurationDto configuration = new SingleReportConfigurationDto();

  public abstract String getDefinitionKey();

  public abstract List<String> getDefinitionVersions();

  public abstract String getDefinitionName();

  public abstract List<String> getTenantIds();

}
