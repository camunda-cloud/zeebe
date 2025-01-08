/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.immutable;

import io.camunda.zeebe.engine.state.group.PersistedGroup;
import io.camunda.zeebe.protocol.record.value.EntityType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GroupState {

  Optional<PersistedGroup> get(String groupId);

  Optional<String> getGroupKeyByName(String groupName);

  Optional<EntityType> getEntityType(String groupId, long entityKey);

  Map<EntityType, List<Long>> getEntitiesByType(String groupId);

  List<String> getGroupIdsForEntity(final String entity);
}
