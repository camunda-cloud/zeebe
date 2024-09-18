/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es;

import static io.camunda.optimize.service.util.mapper.ObjectMapperFactory.OPTIMIZE_MAPPER;

import co.elastic.clients.json.jackson.JacksonJsonProvider;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import io.camunda.optimize.service.db.es.schema.ElasticSearchSchemaManager;
import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import io.camunda.optimize.service.util.BackoffCalculator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.condition.ElasticSearchCondition;
import io.camunda.search.connect.plugin.PluginRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Conditional(ElasticSearchCondition.class)
public class OptimizeElasticsearchClientConfiguration {

  private final ConfigurationService configurationService;
  private final OptimizeIndexNameService optimizeIndexNameService;
  private final ElasticSearchSchemaManager elasticSearchSchemaManager;
  private final PluginRepository pluginRepository = new PluginRepository();

  public OptimizeElasticsearchClientConfiguration(
      ConfigurationService configurationService,
      OptimizeIndexNameService optimizeIndexNameService,
      ElasticSearchSchemaManager elasticSearchSchemaManager) {
    this.configurationService = configurationService;
    this.optimizeIndexNameService = optimizeIndexNameService;
    this.elasticSearchSchemaManager = elasticSearchSchemaManager;
  }

  @Bean
  public OptimizeElasticsearchClient optimizeElasticsearchClient(
      final BackoffCalculator backoffCalculator) {
    return createOptimizeElasticsearchClient(backoffCalculator);
  }

  @Bean
  public JacksonJsonProvider jacksonJsonProvider() {
    return new JacksonJsonProvider(new JacksonJsonpMapper(OPTIMIZE_MAPPER));
  }

  @SneakyThrows
  public OptimizeElasticsearchClient createOptimizeElasticsearchClient(
      final BackoffCalculator backoffCalculator) {
    return OptimizeElasticsearchClientFactory.create(
        configurationService,
        optimizeIndexNameService,
        elasticSearchSchemaManager,
        backoffCalculator,
        pluginRepository);
  }
}
