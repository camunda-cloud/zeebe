/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OptimizeObjectMapper {
  private static final OptimizeJacksonConfig OPTIMIZE_JACKSON_CONFIG = new OptimizeJacksonConfig();
  public static final ObjectMapper OPTIMIZE_MAPPER = OPTIMIZE_JACKSON_CONFIG.objectMapper();
}
