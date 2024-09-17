/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.alert;

import io.camunda.optimize.dto.optimize.query.alert.AlertDefinitionDto;
import lombok.Data;

@Data
public class AlertNotificationDto {

  private final AlertDefinitionDto alert;
  private final Double currentValue;
  private final AlertNotificationType type;
  private final String alertMessage;
  private final String reportLink;

  public AlertNotificationDto(
      AlertDefinitionDto alert,
      Double currentValue,
      AlertNotificationType type,
      String alertMessage,
      String reportLink) {
    this.alert = alert;
    this.currentValue = currentValue;
    this.type = type;
    this.alertMessage = alertMessage;
    this.reportLink = reportLink;
  }
}
