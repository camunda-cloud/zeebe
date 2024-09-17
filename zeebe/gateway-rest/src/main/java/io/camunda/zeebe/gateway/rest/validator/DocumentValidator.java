/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.validator;

import static io.camunda.zeebe.gateway.rest.validator.RequestValidator.createProblemDetail;
import static io.camunda.zeebe.gateway.rest.validator.RequestValidator.validateDate;

import io.camunda.zeebe.gateway.protocol.rest.DocumentLinkRequest;
import io.camunda.zeebe.gateway.protocol.rest.DocumentMetadata;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.http.ProblemDetail;

public class DocumentValidator {

  public static Optional<ProblemDetail> validateDocumentMetadata(final DocumentMetadata metadata) {

    final var violations = new ArrayList<String>();

    if (metadata == null) {
      return Optional.empty();
    }

    if (metadata.getFileName() != null && metadata.getFileName().isBlank()) {
      violations.add("The file name must not be empty, if present");
    }

    if (metadata.getContentType() != null && metadata.getContentType().isBlank()) {
      violations.add("The content type must not be empty, if present");
    }

    if (metadata.getExpiresAt() != null) {
      validateDate(metadata.getExpiresAt(), "expiresAt", violations);
    }
    return createProblemDetail(violations);
  }

  public static Optional<ProblemDetail> validateDocumentLinkParams(
      final DocumentLinkRequest request) {
    final var violations = new ArrayList<String>();

    if (request == null) {
      return Optional.empty();
    }

    if (request.getExpiresAt() != null) {
      validateDate(request.getExpiresAt(), "expiresAt", violations);
      final var now = System.currentTimeMillis();
      final var expiresAtDate =
          ZonedDateTime.parse(request.getExpiresAt()).toInstant().toEpochMilli();
      if (expiresAtDate < now) {
        violations.add("The expiration date must be in the future");
      }
    }

    return createProblemDetail(violations);
  }
}
