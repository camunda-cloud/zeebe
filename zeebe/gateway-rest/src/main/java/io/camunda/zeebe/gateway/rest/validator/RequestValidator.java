/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.validator;

import static io.camunda.zeebe.gateway.rest.validator.ErrorMessages.ERROR_MESSAGE_DATE_PARSING;
import static io.camunda.zeebe.gateway.rest.validator.ErrorMessages.ERROR_MESSAGE_INVALID_ATTRIBUTE_VALUE;
import static io.camunda.zeebe.protocol.record.RejectionType.INVALID_ARGUMENT;

import io.camunda.zeebe.gateway.protocol.rest.Changeset;
import io.camunda.zeebe.gateway.rest.RestErrorMapper;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class RequestValidator {

  public static Optional<ProblemDetail> createProblemDetail(final List<String> violations) {
    final String problems = String.join(". ", violations) + ".";
    return violations.isEmpty()
        ? Optional.empty()
        : Optional.of(
            RestErrorMapper.createProblemDetail(
                HttpStatus.BAD_REQUEST, problems, INVALID_ARGUMENT.name()));
  }

  public static void validateDate(
      final String dateString, final String attributeName, final List<String> violations) {
    if (dateString != null && !dateString.isEmpty()) {
      try {
        OffsetDateTime.parse(dateString);
      } catch (final DateTimeParseException ex) {
        violations.add(ERROR_MESSAGE_DATE_PARSING.formatted(attributeName, dateString));
      }
    }
  }

  public static boolean isEmpty(final Changeset changeset) {
    return changeset == null
        || (changeset.getFollowUpDate() == null
            && changeset.getDueDate() == null
            && changeset.getCandidateGroups() == null
            && changeset.getCandidateUsers() == null
            && changeset.getPriority() == null);
  }

  public static Optional<ProblemDetail> validate(final Consumer<List<String>> customValidation) {
    final List<String> violations = new ArrayList<>();

    customValidation.accept(violations);

    return createProblemDetail(violations);
  }

  public static void validateOperationReference(
      final Long operationReference, final List<String> violations) {
    if (operationReference != null && operationReference < 1) {
      violations.add(
          ERROR_MESSAGE_INVALID_ATTRIBUTE_VALUE.formatted(
              "operationReference", operationReference, "> 0"));
    }
  }
}
