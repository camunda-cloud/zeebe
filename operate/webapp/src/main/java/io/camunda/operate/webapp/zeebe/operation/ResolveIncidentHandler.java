/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.zeebe.operation;

import static io.camunda.operate.entities.OperationType.RESOLVE_INCIDENT;

import io.camunda.operate.entities.ErrorType;
import io.camunda.operate.entities.IncidentEntity;
import io.camunda.operate.entities.OperationEntity;
import io.camunda.operate.entities.OperationType;
import io.camunda.operate.webapp.reader.IncidentReader;
import io.camunda.operate.webapp.rest.exception.NotFoundException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Resolve the incident. */
@Component
public class ResolveIncidentHandler extends AbstractOperationHandler implements OperationHandler {

  @Autowired private IncidentReader incidentReader;

  @Override
  public void handleWithException(final OperationEntity operation) throws Exception {

    if (operation.getIncidentKey() == null) {
      failOperation(operation, "Incident key must be defined.");
      return;
    }

    final IncidentEntity incident;
    try {
      incident = incidentReader.getIncidentById(operation.getIncidentKey());
    } catch (final NotFoundException ex) {
      failOperation(operation, "No appropriate incidents found: " + ex.getMessage());
      return;
    }

    final ErrorType errorType = incident.getErrorType();
    if (errorType != null && errorType.isResolvedViaRetries()) {
      zeebeClient.newUpdateRetriesCommand(incident.getJobKey()).retries(1).send().join();
    }
    zeebeClient.newResolveIncidentCommand(incident.getKey()).send().join();
    // mark operation as sent
    markAsSent(operation);
  }

  @Override
  public Set<OperationType> getTypes() {
    return Set.of(RESOLVE_INCIDENT);
  }
}
