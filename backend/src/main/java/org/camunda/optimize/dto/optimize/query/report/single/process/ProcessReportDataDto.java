/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.single.process;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.camunda.optimize.dto.optimize.query.report.Combinable;
import org.camunda.optimize.dto.optimize.query.report.single.SingleReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.parameters.ProcessParametersDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.parameters.ProcessPartDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewDto;
import org.camunda.optimize.service.es.report.command.util.ReportUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Data
public class ProcessReportDataDto extends SingleReportDataDto implements Combinable {

  protected String processDefinitionKey;
  protected String processDefinitionVersion;
  protected List<String> tenantIds = Collections.singletonList(null);
  protected List<ProcessFilterDto> filter = new ArrayList<>();
  protected ProcessViewDto view;
  protected ProcessGroupByDto groupBy;
  protected ProcessVisualization visualization;
  protected ProcessParametersDto parameters = new ProcessParametersDto();

  @JsonIgnore
  @Override
  public String getDefinitionKey() {
    return processDefinitionKey;
  }

  @JsonIgnore
  @Override
  public String getDefinitionVersion() {
    return processDefinitionVersion;
  }

  @JsonIgnore
  @Override
  public String createCommandKey() {
    String viewCommandKey = view == null ? "null" : view.createCommandKey();
    String groupByCommandKey = groupBy == null ? "null" : groupBy.createCommandKey();
    String processPartCommandKey = Optional.ofNullable(getParameters())
      .flatMap(parameters -> Optional.ofNullable(parameters.getProcessPart()))
      .map(ProcessPartDto::createCommandKey)
      .orElse("null");
    String configurationCommandKey = Optional.ofNullable(getConfiguration())
      .map(c -> c.createCommandKey(getView(), getGroupBy()))
      .orElse("null");
    return viewCommandKey + "_" + groupByCommandKey + "_" + processPartCommandKey + "_" + configurationCommandKey;
  }

  @Override
  public boolean isCombinable(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProcessReportDataDto)) {
      return false;
    }
    ProcessReportDataDto that = (ProcessReportDataDto) o;
    return ReportUtil.isCombinable(view, that.view) &&
      ReportUtil.isCombinable(groupBy, that.groupBy) &&
      Objects.equals(visualization, that.visualization);
  }

}
