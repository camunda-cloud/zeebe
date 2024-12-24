/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.providers;

import io.camunda.optimize.dto.optimize.rest.DefinitionExceptionResponseDto;
import io.camunda.optimize.service.LocalizationService;
import io.camunda.optimize.service.exceptions.OptimizeImportDefinitionDoesNotExistException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.springframework.http.MediaType;

@Provider
public class OptimizeImportDefinitionDoesNotExistsExceptionMapper
    implements ExceptionMapper<OptimizeImportDefinitionDoesNotExistException> {

  public static final String ERROR_CODE = "importDefinitionForbidden";
  private static final Logger LOG =
      org.slf4j.LoggerFactory.getLogger(OptimizeImportDefinitionDoesNotExistsExceptionMapper.class);

  private final LocalizationService localizationService;

  public OptimizeImportDefinitionDoesNotExistsExceptionMapper(
      final LocalizationService localizationService) {
    this.localizationService = localizationService;
  }

  @Override
  public Response toResponse(final OptimizeImportDefinitionDoesNotExistException exception) {
    LOG.info("Mapping OptimizeImportDefinitionDoesNotExistException");

    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON_VALUE)
        .entity(getMissingDefinitionResponseDto(exception))
        .build();
  }

  private DefinitionExceptionResponseDto getMissingDefinitionResponseDto(
      final OptimizeImportDefinitionDoesNotExistException exception) {
    final String errorCode = exception.getErrorCode();
    final String errorMessage =
        localizationService.getDefaultLocaleMessageForApiErrorCode(errorCode);
    final String detailedErrorMessage = exception.getMessage();

    return new DefinitionExceptionResponseDto(
        errorCode, errorMessage, detailedErrorMessage, exception.getMissingDefinitions());
  }
}
