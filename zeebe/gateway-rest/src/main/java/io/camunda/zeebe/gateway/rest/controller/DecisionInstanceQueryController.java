/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.controller;

import static io.camunda.zeebe.gateway.rest.Loggers.REST_LOGGER;

import io.camunda.search.query.DecisionInstanceQuery;
import io.camunda.service.DecisionInstanceServices;
import io.camunda.zeebe.gateway.protocol.rest.DecisionInstanceSearchQueryRequest;
import io.camunda.zeebe.gateway.protocol.rest.DecisionInstanceSearchQueryResponse;
import io.camunda.zeebe.gateway.rest.RestErrorMapper;
import io.camunda.zeebe.gateway.rest.SearchQueryRequestMapper;
import io.camunda.zeebe.gateway.rest.SearchQueryResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@CamundaRestQueryController
@RequestMapping("/v2/decision-instances")
public class DecisionInstanceQueryController {

  @Autowired private DecisionInstanceServices decisionInstanceServices;

  @PostMapping(
      path = "/search",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DecisionInstanceSearchQueryResponse> searchDecisionInstances(
      @RequestBody(required = false) final DecisionInstanceSearchQueryRequest query) {
    return SearchQueryRequestMapper.toDecisionInstanceQuery(query)
        .fold(RestErrorMapper::mapProblemToResponse, this::search);
  }

  private ResponseEntity<DecisionInstanceSearchQueryResponse> search(
      final DecisionInstanceQuery query) {
    try {
      final var decisionInstances = decisionInstanceServices.search(query);
      return ResponseEntity.ok(
          SearchQueryResponseMapper.toDecisionInstanceSearchQueryResponse(decisionInstances));
    } catch (final Exception e) {
      REST_LOGGER.warn("An exception occurred in searchDecisionInstances.", e);
      final var problemDetail =
          RestErrorMapper.createProblemDetail(
              HttpStatus.INTERNAL_SERVER_ERROR,
              e.getMessage(),
              "Failed to execute Decision Instance Search Query.");
      return RestErrorMapper.mapProblemToResponse(problemDetail);
    }
  }
}
