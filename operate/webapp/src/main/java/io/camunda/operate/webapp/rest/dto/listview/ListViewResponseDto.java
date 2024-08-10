/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.rest.dto.listview;

import java.util.ArrayList;
import java.util.List;

public class ListViewResponseDto {

  private List<ListViewProcessInstanceDto> processInstances = new ArrayList<>();

  private long totalCount;

  public List<ListViewProcessInstanceDto> getProcessInstances() {
    return processInstances;
  }

  public void setProcessInstances(List<ListViewProcessInstanceDto> processInstances) {
    this.processInstances = processInstances;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  @Override
  public int hashCode() {
    int result = processInstances != null ? processInstances.hashCode() : 0;
    result = 31 * result + (int) (totalCount ^ (totalCount >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ListViewResponseDto that = (ListViewResponseDto) o;

    if (totalCount != that.totalCount) {
      return false;
    }
    return processInstances != null
        ? processInstances.equals(that.processInstances)
        : that.processInstances == null;
  }
}
