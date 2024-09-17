/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import io.camunda.optimize.dto.optimize.query.TokenDto;
import io.camunda.optimize.rest.cloud.CloudSaasMetaInfoService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Path("/token")
@Component
@Slf4j
public class TokenRestService {

  private final Optional<CloudSaasMetaInfoService> cloudSaasMetaInfoService;

  public TokenRestService(Optional<CloudSaasMetaInfoService> cloudSaasMetaInfoService) {
    this.cloudSaasMetaInfoService = cloudSaasMetaInfoService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public TokenDto getCurrentToken() {
    return cloudSaasMetaInfoService
        .map(
            saasMetaInfoService ->
                new TokenDto(saasMetaInfoService.getCurrentUserServiceToken().orElse(null)))
        .orElseGet(() -> new TokenDto(null));
  }
}
