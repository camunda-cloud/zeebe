/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.entities;

import java.time.OffsetDateTime;
import java.util.Objects;

public class ProcessInstanceEntity extends TasklistZeebeEntity<ProcessInstanceEntity> {

  private ProcessInstanceState state;
  private OffsetDateTime endDate;

  public ProcessInstanceState getState() {
    return state;
  }

  public ProcessInstanceEntity setState(final ProcessInstanceState state) {
    this.state = state;
    return this;
  }

  public OffsetDateTime getEndDate() {
    return endDate;
  }

  public ProcessInstanceEntity setEndDate(final OffsetDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final ProcessInstanceEntity that = (ProcessInstanceEntity) o;
    return state == that.state && Objects.equals(endDate, that.endDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), state, endDate);
  }
}
