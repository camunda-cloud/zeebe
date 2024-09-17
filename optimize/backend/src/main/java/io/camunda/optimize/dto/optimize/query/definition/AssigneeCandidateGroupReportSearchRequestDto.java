/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.definition;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssigneeCandidateGroupReportSearchRequestDto {

  private String terms;
  @Builder.Default private int limit = 25;
  @NotNull @NotEmpty private List<String> reportIds;

  public AssigneeCandidateGroupReportSearchRequestDto(
      String terms, int limit, @NotNull @NotEmpty List<String> reportIds) {
    this.terms = terms;
    this.limit = limit;
    this.reportIds = reportIds;
  }

  public AssigneeCandidateGroupReportSearchRequestDto() {}

  public Optional<String> getTerms() {
    return Optional.ofNullable(terms);
  }
}
