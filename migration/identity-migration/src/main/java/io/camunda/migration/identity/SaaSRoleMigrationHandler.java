/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migration.identity;

import io.camunda.migration.identity.console.ConsoleClient;
import io.camunda.migration.identity.dto.Role;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(ConsoleClient.class)
public class SaaSRoleMigrationHandler extends RoleMigrationHandler {

  @Override
  protected List<Role> fetchBatch() {
    return List.of();
  }

  @Override
  protected void process(final List<Role> batch) {}
}
