/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.management;

import io.camunda.operate.schema.IndexSchemaValidator;
import io.camunda.operate.schema.SchemaManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IndicesCheck {

  @Autowired private IndexSchemaValidator indexSchemaValidator;

  @Autowired private SchemaManager schemaManager;

  public boolean indicesArePresent() {
    return indexSchemaValidator.schemaExists();
  }

  public boolean isHealthy() {
    return schemaManager.isHealthy();
  }
}
