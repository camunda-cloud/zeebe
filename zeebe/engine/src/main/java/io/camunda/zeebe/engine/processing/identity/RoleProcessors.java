/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.identity;

import io.camunda.zeebe.engine.processing.distribution.CommandDistributionBehavior;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecordProcessors;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.engine.state.immutable.RoleState;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.RoleIntent;
import io.camunda.zeebe.stream.api.state.KeyGenerator;

public class RoleProcessors {
  public static void addRoleProcessors(
      final TypedRecordProcessors typedRecordProcessors,
      final RoleState roleState,
      final AuthorizationCheckBehavior authCheckBehavior,
      final KeyGenerator keyGenerator,
      final Writers writers,
      final CommandDistributionBehavior commandDistributionBehavior) {
    typedRecordProcessors.onCommand(
        ValueType.ROLE,
        RoleIntent.CREATE,
        new RoleCreateProcessor(
            roleState, authCheckBehavior, keyGenerator, writers, commandDistributionBehavior));
    typedRecordProcessors.onCommand(
        ValueType.ROLE,
        RoleIntent.UPDATE,
        new RoleUpdateProcessor(
            roleState, keyGenerator, authCheckBehavior, writers, commandDistributionBehavior));
  }
}
