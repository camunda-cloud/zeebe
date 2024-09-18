/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.alert;

import lombok.Data;

@Data
public class AlertInterval {

  private int value;
  private AlertIntervalUnit unit;

  public AlertInterval(int value, AlertIntervalUnit unit) {
    this.value = value;
    this.unit = unit;
  }

  public AlertInterval() {}

  public static final class Fields {

    public static final String value = "value";
    public static final String unit = "unit";
  }
}
