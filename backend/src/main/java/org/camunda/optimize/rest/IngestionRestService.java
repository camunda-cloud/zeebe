/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.ReportConstants;
import org.camunda.optimize.dto.optimize.query.event.process.EventDto;
import org.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto;
import org.camunda.optimize.dto.optimize.rest.CloudEventRequestDto;
import org.camunda.optimize.service.events.ExternalEventService;
import org.camunda.optimize.service.security.util.LocalDateUtil;
import org.camunda.optimize.service.util.VariableHelper;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.camunda.optimize.service.variable.ExternalVariableService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto.toExternalProcessVariableDtos;
import static org.camunda.optimize.rest.IngestionRestService.INGESTION_PATH;
import static org.camunda.optimize.rest.constants.RestConstants.AUTH_COOKIE_TOKEN_VALUE_PREFIX;

@AllArgsConstructor
@Slf4j
@Path(INGESTION_PATH)
@Component
public class IngestionRestService {
  public static final String INGESTION_PATH = "/ingestion";
  public static final String EVENT_BATCH_SUB_PATH = "/event/batch";
  public static final String VARIABLE_SUB_PATH = "/variable";

  public static final String CONTENT_TYPE_CLOUD_EVENTS_V1_JSON_BATCH = "application/cloudevents-batch+json";
  public static final String QUERY_PARAMETER_ACCESS_TOKEN = "access_token";

  private final ConfigurationService configurationService;
  private final ExternalEventService externalEventService;
  private final ExternalVariableService externalVariableService;

  @POST
  @Path(EVENT_BATCH_SUB_PATH)
  @Consumes({CONTENT_TYPE_CLOUD_EVENTS_V1_JSON_BATCH, MediaType.APPLICATION_JSON})
  @Produces(MediaType.APPLICATION_JSON)
  public void ingestCloudEvents(final @Context ContainerRequestContext requestContext,
                                final @NotNull @Valid @RequestBody ValidList<CloudEventRequestDto> cloudEventDtos) {
    validateAccessToken(requestContext, getEventIngestionAccessToken());
    externalEventService.saveEventBatch(mapToEventDto(cloudEventDtos));
  }

  @POST
  @Path(VARIABLE_SUB_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  public void ingestVariables(final @Context ContainerRequestContext requestContext,
                              final @NotNull @Valid @RequestBody List<ExternalProcessVariableRequestDto> variableDtos) {
    validateAccessToken(requestContext, getVariableIngestionAccessToken());
    validateVariableType(variableDtos);
    externalVariableService.storeExternalProcessVariables(
      toExternalProcessVariableDtos(
        LocalDateUtil.getCurrentDateTime().toInstant().toEpochMilli(),
        variableDtos
      ));
  }

  private List<EventDto> mapToEventDto(final List<CloudEventRequestDto> cloudEventDtos) {
    return cloudEventDtos.stream()
      .map(cloudEventDto -> EventDto.builder()
        .id(cloudEventDto.getId())
        .eventName(cloudEventDto.getType())
        .timestamp(
          cloudEventDto.getTime()
            .orElse(LocalDateUtil.getCurrentDateTime().toInstant())
            .toEpochMilli()
        )
        .traceId(cloudEventDto.getTraceid())
        .group(cloudEventDto.getGroup().orElse(null))
        .source(cloudEventDto.getSource())
        .data(cloudEventDto.getData())
        .build())
      .collect(toList());
  }

  private void validateVariableType(final List<ExternalProcessVariableRequestDto> variables) {
    if (variables.stream().anyMatch(variable -> !VariableHelper.isVariableTypeSupported(variable.getType()))) {
      throw new BadRequestException(String.format(
        "A given variable type is not supported. The type must always be one of: %s",
        ReportConstants.ALL_SUPPORTED_VARIABLE_TYPES
      ));
    }
  }

  private void validateAccessToken(final ContainerRequestContext requestContext, final String expectedAccessToken) {
    final MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    final String queryParameterAccessToken = queryParameters.getFirst(QUERY_PARAMETER_ACCESS_TOKEN);

    if (!expectedAccessToken.equals(extractAuthorizationHeaderToken(requestContext))
      && !expectedAccessToken.equals(queryParameterAccessToken)) {
      throw new NotAuthorizedException("Invalid or no ingestion api secret provided.");
    }
  }

  private String extractAuthorizationHeaderToken(ContainerRequestContext requestContext) {
    return Optional.ofNullable(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION))
      .map(providedValue -> {
        if (providedValue.startsWith(AUTH_COOKIE_TOKEN_VALUE_PREFIX)) {
          return providedValue.replaceFirst(AUTH_COOKIE_TOKEN_VALUE_PREFIX, "");
        }
        return providedValue;
      }).orElse(null);
  }

  private String getEventIngestionAccessToken() {
    return configurationService.getEventIngestionConfiguration().getAccessToken();
  }

  private String getVariableIngestionAccessToken() {
    return configurationService.getVariableIngestionConfiguration().getAccessToken();
  }

  @Data
  private static class ValidList<E> implements List<E> {
    @Delegate
    private List<E> list = new ArrayList<>();
  }

}
