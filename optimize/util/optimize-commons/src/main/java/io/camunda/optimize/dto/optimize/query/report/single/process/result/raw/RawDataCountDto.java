/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.result.raw;

import io.camunda.optimize.dto.optimize.query.report.single.RawDataInstanceDto;
import lombok.Data;

@Data
public class RawDataCountDto implements RawDataInstanceDto {

  protected long incidents;
  protected long openIncidents;
  protected long userTasks;

  public RawDataCountDto(long incidents, long openIncidents, long userTasks) {
    this.incidents = incidents;
    this.openIncidents = openIncidents;
    this.userTasks = userTasks;
  }

  public RawDataCountDto() {}

  public enum Fields {
    incidents,
    openIncidents,
    userTasks
  }
}
