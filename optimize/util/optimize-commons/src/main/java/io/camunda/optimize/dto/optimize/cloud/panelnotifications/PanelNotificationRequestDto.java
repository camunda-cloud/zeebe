/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.cloud.panelnotifications;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Builder
@Data
public class PanelNotificationRequestDto {

  private final PanelNotificationDataDto notification;

  private PanelNotificationRequestDto(PanelNotificationDataDto notification) {
    this.notification = notification;
  }
}
