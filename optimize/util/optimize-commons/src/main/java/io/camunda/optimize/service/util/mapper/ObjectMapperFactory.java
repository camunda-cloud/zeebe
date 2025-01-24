/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.camunda.optimize.dto.optimize.DefinitionOptimizeResponseDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionEntity;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.dto.optimize.rest.AuthorizedReportDefinitionResponseDto;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.ConfigurationServiceBuilder;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class ObjectMapperFactory {

  private static final ObjectMapperFactory FACTORY =
      new ObjectMapperFactory(
          new OptimizeDateTimeFormatterFactory().getObject(),
          ConfigurationServiceBuilder.createDefaultConfiguration());
  public static final ObjectMapper OPTIMIZE_MAPPER = FACTORY.createOptimizeMapper();
  public static final ObjectMapper OPTIMIZE_MAPPER_UNKNOWN_FAIL_DISABLED =
      FACTORY.createOptimizeMapper(false);

  private final DateTimeFormatter optimizeDateTimeFormatter;
  private final ConfigurationService configurationService;

  public ObjectMapperFactory(
      final DateTimeFormatter optimizeDateTimeFormatter,
      final ConfigurationService configurationService) {
    this.optimizeDateTimeFormatter = optimizeDateTimeFormatter;
    this.configurationService = configurationService;
  }

  @Qualifier("optimizeObjectMapper")
  @Bean
  public ObjectMapper createOptimizeMapper() {
    return buildObjectMapper(optimizeDateTimeFormatter, true);
  }

  public ObjectMapper createOptimizeMapper(final boolean unknownPropsEnable) {
    return buildObjectMapper(optimizeDateTimeFormatter, unknownPropsEnable);
  }

  @Qualifier("engineMapper")
  @Bean
  public ObjectMapper createEngineMapper() {
    return buildObjectMapper(createEngineDateTimeFormatter(), true);
  }

  private DateTimeFormatter createEngineDateTimeFormatter() {
    return DateTimeFormatter.ofPattern(configurationService.getEngineDateFormat());
  }

  private ObjectMapper buildObjectMapper(
      final DateTimeFormatter deserializationDateTimeFormatter,
      final boolean unknownPropsFailEnable) {
    final JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(
        OffsetDateTime.class, new CustomOffsetDateTimeSerializer(optimizeDateTimeFormatter));
    javaTimeModule.addSerializer(
        Date.class, new DateSerializer(false, new StdDateFormat().withColonInTimeZone(false)));
    javaTimeModule.addDeserializer(
        OffsetDateTime.class,
        new CustomOffsetDateTimeDeserializer(deserializationDateTimeFormatter));

    final Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder =
        Jackson2ObjectMapperBuilder.json()
            .modules(new Jdk8Module(), javaTimeModule)
            .featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY,
                SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)
            .featuresToEnable(
                JsonParser.Feature.ALLOW_COMMENTS,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

    if (unknownPropsFailEnable) {
      jackson2ObjectMapperBuilder.featuresToDisable(
          DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    final ObjectMapper mapper = jackson2ObjectMapperBuilder.build();

    final SimpleModule module = new SimpleModule();
    module.addDeserializer(
        DefinitionOptimizeResponseDto.class, new CustomDefinitionDeserializer(mapper));
    module.addDeserializer(
        ReportDefinitionDto.class, new CustomReportDefinitionDeserializer(mapper));
    module.addDeserializer(
        AuthorizedReportDefinitionResponseDto.class,
        new CustomAuthorizedReportDefinitionDeserializer(mapper));
    module.addDeserializer(CollectionEntity.class, new CustomCollectionEntityDeserializer(mapper));
    mapper.registerModule(module);

    return mapper;
  }
}
