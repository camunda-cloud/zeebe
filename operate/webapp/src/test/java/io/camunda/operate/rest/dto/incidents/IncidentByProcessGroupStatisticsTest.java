/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.rest.dto.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.operate.webapp.rest.dto.incidents.IncidentsByProcessGroupStatisticsDto;
import org.junit.jupiter.api.Test;

public class IncidentByProcessGroupStatisticsTest {

  @Test
  public void testComparatorSameInstances() {
    final IncidentsByProcessGroupStatisticsDto moreIncidents = newWithInstancesAndIncidents(5, 3);
    final IncidentsByProcessGroupStatisticsDto lesserIncidents = newWithInstancesAndIncidents(5, 2);
    assertIsBefore(moreIncidents, lesserIncidents);
  }

  @Test
  public void testComparatorDifferentInstancesAndIncidents() {
    final IncidentsByProcessGroupStatisticsDto moreIncidents =
        newWithInstancesAndIncidents(1314 + 845, 845);
    final IncidentsByProcessGroupStatisticsDto lessIncidents =
        newWithInstancesAndIncidents(1351 + 831, 831);
    assertIsBefore(moreIncidents, lessIncidents);
  }

  @Test
  public void testComparatorZeroIncidents() {
    final IncidentsByProcessGroupStatisticsDto moreInstances = newWithInstancesAndIncidents(172, 0);
    final IncidentsByProcessGroupStatisticsDto lessInstances = newWithInstancesAndIncidents(114, 0);
    assertIsBefore(moreInstances, lessInstances);
  }

  @Test
  public void testComparatorSameIncidentsAndInstances() {
    final IncidentsByProcessGroupStatisticsDto onlyOtherBPMN1 =
        newWithInstancesAndIncidents(172, 0);
    onlyOtherBPMN1.setBpmnProcessId("1");
    final IncidentsByProcessGroupStatisticsDto onlyOtherBPMN2 =
        newWithInstancesAndIncidents(172, 0);
    onlyOtherBPMN2.setBpmnProcessId("2");
    assertIsBefore(onlyOtherBPMN1, onlyOtherBPMN2);
  }

  protected IncidentsByProcessGroupStatisticsDto newWithInstancesAndIncidents(
      final int instances, final int incidents) {
    final IncidentsByProcessGroupStatisticsDto newObject =
        new IncidentsByProcessGroupStatisticsDto();
    newObject.setActiveInstancesCount(Long.valueOf(instances));
    newObject.setInstancesWithActiveIncidentsCount(incidents);
    return newObject;
  }

  protected void assertIsBefore(
      final IncidentsByProcessGroupStatisticsDto first,
      final IncidentsByProcessGroupStatisticsDto second) {
    assertThat(IncidentsByProcessGroupStatisticsDto.COMPARATOR.compare(first, second))
        .isLessThan(0);
  }
}
