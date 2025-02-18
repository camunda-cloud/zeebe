/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.client.flownodeinstance;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.protocol.rest.FlowNodeInstanceFilter;
import io.camunda.client.protocol.rest.FlowNodeInstanceResult;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class FlowNodeInstanceTypeTest {

  @ParameterizedTest
  @EnumSource(BpmnElementType.class)
  void shouldHaveInstanceFilterType(final BpmnElementType bpmnElementType) {
    // when
    final FlowNodeInstanceFilter.TypeEnum filterType =
        FlowNodeInstanceFilter.TypeEnum.fromValue(bpmnElementType.name());

    // then
    assertThat(filterType)
        .describedAs("Should have type for BPMN element: %s", bpmnElementType)
        .isNotNull()
        .isNotEqualTo(FlowNodeInstanceFilter.TypeEnum.UNKNOWN_DEFAULT_OPEN_API);
  }

  @ParameterizedTest
  @EnumSource(BpmnElementType.class)
  void shouldHaveInstanceResultType(final BpmnElementType bpmnElementType) {
    // when
    final FlowNodeInstanceResult.TypeEnum resultType =
        FlowNodeInstanceResult.TypeEnum.fromValue(bpmnElementType.name());

    // then
    assertThat(resultType)
        .describedAs("Should have type for BPMN element: %s", bpmnElementType)
        .isNotNull()
        .isNotEqualTo(FlowNodeInstanceResult.TypeEnum.UNKNOWN_DEFAULT_OPEN_API);
  }
}
