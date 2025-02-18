/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.entities;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeType;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class FlowNodeTypeTest {

  @ParameterizedTest
  @EnumSource(BpmnElementType.class)
  void shouldMapBpmnElementType(final BpmnElementType bpmnElementType) {
    // when
    final FlowNodeType flowNodeType = FlowNodeType.fromZeebeBpmnElementType(bpmnElementType.name());

    // then
    assertThat(flowNodeType)
        .describedAs("Should have type for BPMN element: %s", bpmnElementType)
        .isNotNull()
        .isNotEqualTo(FlowNodeType.UNKNOWN);
  }
}
