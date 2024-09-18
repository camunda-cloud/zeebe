/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize;

import static io.camunda.optimize.service.util.importing.ZeebeConstants.FLOW_NODE_TYPE_USER_TASK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.optimize.dto.optimize.datasource.DataSourceDto;
import io.camunda.optimize.dto.optimize.persistence.incident.IncidentDto;
import io.camunda.optimize.dto.optimize.query.process.FlowNodeInstanceDto;
import io.camunda.optimize.dto.optimize.query.variable.SimpleProcessVariableDto;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ProcessInstanceDto implements OptimizeDto {

  private String processDefinitionKey;
  private String processDefinitionVersion;
  private String processDefinitionId;
  private String processInstanceId;
  private String businessKey;
  private OffsetDateTime startDate;
  private OffsetDateTime endDate;
  private Long duration; // duration in ms
  private String state;
  @Builder.Default private List<FlowNodeInstanceDto> flowNodeInstances = new ArrayList<>();
  @Builder.Default private List<SimpleProcessVariableDto> variables = new ArrayList<>();
  @Builder.Default private List<IncidentDto> incidents = new ArrayList<>();
  private DataSourceDto dataSource;
  private String tenantId;

  protected ProcessInstanceDto(
      String processDefinitionKey,
      String processDefinitionVersion,
      String processDefinitionId,
      String processInstanceId,
      String businessKey,
      OffsetDateTime startDate,
      OffsetDateTime endDate,
      Long duration,
      String state,
      List<FlowNodeInstanceDto> flowNodeInstances,
      List<SimpleProcessVariableDto> variables,
      List<IncidentDto> incidents,
      DataSourceDto dataSource,
      String tenantId) {
    this.processDefinitionKey = processDefinitionKey;
    this.processDefinitionVersion = processDefinitionVersion;
    this.processDefinitionId = processDefinitionId;
    this.processInstanceId = processInstanceId;
    this.businessKey = businessKey;
    this.startDate = startDate;
    this.endDate = endDate;
    this.duration = duration;
    this.state = state;
    this.flowNodeInstances = flowNodeInstances;
    this.variables = variables;
    this.incidents = incidents;
    this.dataSource = dataSource;
    this.tenantId = tenantId;
  }

  public ProcessInstanceDto() {}

  @JsonIgnore
  public List<FlowNodeInstanceDto> getUserTasks() {
    return flowNodeInstances.stream()
        .filter(flowNode -> FLOW_NODE_TYPE_USER_TASK.equalsIgnoreCase(flowNode.getFlowNodeType()))
        .toList();
  }

  public static final class Fields {

    public static final String processDefinitionKey = "processDefinitionKey";
    public static final String processDefinitionVersion = "processDefinitionVersion";
    public static final String processDefinitionId = "processDefinitionId";
    public static final String processInstanceId = "processInstanceId";
    public static final String businessKey = "businessKey";
    public static final String startDate = "startDate";
    public static final String endDate = "endDate";
    public static final String duration = "duration";
    public static final String state = "state";
    public static final String flowNodeInstances = "flowNodeInstances";
    public static final String variables = "variables";
    public static final String incidents = "incidents";
    public static final String dataSource = "dataSource";
    public static final String tenantId = "tenantId";
  }
}
