/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.entities;

import java.time.OffsetDateTime;
import java.util.Objects;

public class MetricEntity extends OperateEntity {
  private String event;
  private String value;
  private OffsetDateTime eventTime;

  private String tenantId;

  public MetricEntity() {
    super();
  }

  public MetricEntity(String event, String value, OffsetDateTime eventTime) {
    this.event = event;
    this.value = value;
    this.eventTime = eventTime;
  }

  public String getEvent() {
    return event;
  }

  public MetricEntity setEvent(String event) {
    this.event = event;
    return this;
  }

  public String getValue() {
    return value;
  }

  public MetricEntity setValue(String value) {
    this.value = value;
    return this;
  }

  public OffsetDateTime getEventTime() {
    return eventTime;
  }

  public MetricEntity setEventTime(OffsetDateTime eventTime) {
    this.eventTime = eventTime;
    return this;
  }

  public String getTenantId() {
    return tenantId;
  }

  public MetricEntity setTenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final MetricEntity that = (MetricEntity) o;
    return Objects.equals(event, that.event)
        && Objects.equals(value, that.value)
        && Objects.equals(eventTime, that.eventTime)
        && Objects.equals(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), event, value, eventTime, tenantId);
  }
}
