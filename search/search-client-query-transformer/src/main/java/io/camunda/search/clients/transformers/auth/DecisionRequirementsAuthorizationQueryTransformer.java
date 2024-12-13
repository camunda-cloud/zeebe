/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.clients.transformers.auth;

import static io.camunda.search.clients.query.SearchQueryBuilders.stringTerms;
import static io.camunda.webapps.schema.descriptors.operate.index.DecisionRequirementsIndex.DECISION_REQUIREMENTS_ID;
import static io.camunda.zeebe.protocol.record.value.AuthorizationResourceType.DECISION_REQUIREMENTS_DEFINITION;
import static io.camunda.zeebe.protocol.record.value.PermissionType.READ;

import io.camunda.search.clients.query.SearchQuery;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import java.util.List;

public class DecisionRequirementsAuthorizationQueryTransformer
    implements AuthorizationQueryTransformer {

  @Override
  public SearchQuery toSearchQuery(
      final AuthorizationResourceType resourceType,
      final PermissionType permissionType,
      final List<String> resourceKeys) {
    if (resourceType == DECISION_REQUIREMENTS_DEFINITION && permissionType == READ) {
      return stringTerms(DECISION_REQUIREMENTS_ID, resourceKeys);
    }
    throw new IllegalArgumentException(
        "Unsupported authorizations with resource:%s and permission:%s: "
            .formatted(resourceType, permissionType));
  }
}
