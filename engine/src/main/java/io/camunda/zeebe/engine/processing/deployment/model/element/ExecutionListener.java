/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.deployment.model.element;

import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeExecutionListenerEventType;

public class ExecutionListener {
  private ZeebeExecutionListenerEventType eventType;
  private JobWorkerProperties jobWorkerProperties = new JobWorkerProperties();

  public ZeebeExecutionListenerEventType getEventType() {
    return eventType;
  }

  public void setEventType(final ZeebeExecutionListenerEventType eventType) {
    this.eventType = eventType;
  }

  public JobWorkerProperties getJobWorkerProperties() {
    return jobWorkerProperties;
  }

  public void setJobWorkerProperties(final JobWorkerProperties jobWorkerProperties) {
    this.jobWorkerProperties = jobWorkerProperties;
  }
}
