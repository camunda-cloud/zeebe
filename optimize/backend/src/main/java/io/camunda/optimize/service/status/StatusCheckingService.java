/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.status;

import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import org.springframework.stereotype.Component;

@Component
public abstract class StatusCheckingService {

  protected final OptimizeIndexNameService optimizeIndexNameService;

  public StatusCheckingService(OptimizeIndexNameService optimizeIndexNameService) {
    this.optimizeIndexNameService = optimizeIndexNameService;
  }

  public abstract boolean isConnectedToDatabase();
}
