/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.validator;

import static io.camunda.zeebe.gateway.rest.validator.ErrorMessages.ERROR_MESSAGE_EMPTY_ATTRIBUTE;
import static io.camunda.zeebe.gateway.rest.validator.RequestValidator.createProblemDetail;

import io.camunda.zeebe.gateway.protocol.rest.AuthorizationAssignRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ProblemDetail;

public final class AuthorizationRequestValidator {
  public static Optional<ProblemDetail> validateAuthorizationAssignRequest(
      final AuthorizationAssignRequest authorizationAssignRequest) {
    final List<String> violations = new ArrayList<>();
    if (authorizationAssignRequest.getOwnerKey() == null
        || authorizationAssignRequest.getOwnerKey().isBlank()) {
      violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("ownerKey"));
    }

    if (authorizationAssignRequest.getOwnerType() == null) {
      violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("ownerType"));
    }

    if (authorizationAssignRequest.getResourceKey() == null
        || authorizationAssignRequest.getResourceKey().isBlank()) {
      violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("resourceKey"));
    }

    if (authorizationAssignRequest.getResourceType() == null
        || authorizationAssignRequest.getResourceType().isBlank()) {
      violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("resourceType"));
    }

    return createProblemDetail(violations);
  }
}
