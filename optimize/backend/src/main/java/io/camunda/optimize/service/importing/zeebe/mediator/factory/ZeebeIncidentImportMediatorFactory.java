/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.zeebe.mediator.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.datasource.ZeebeDataSourceDto;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.reader.ProcessDefinitionReader;
import io.camunda.optimize.service.db.writer.ProcessInstanceWriter;
import io.camunda.optimize.service.importing.ImportIndexHandlerRegistry;
import io.camunda.optimize.service.importing.ImportMediator;
import io.camunda.optimize.service.importing.engine.service.zeebe.ZeebeIncidentImportService;
import io.camunda.optimize.service.importing.zeebe.db.ZeebeIncidentFetcher;
import io.camunda.optimize.service.importing.zeebe.mediator.ZeebeIncidentImportMediator;
import io.camunda.optimize.service.util.BackoffCalculator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ZeebeIncidentImportMediatorFactory extends AbstractZeebeImportMediatorFactory {

  private final ProcessInstanceWriter zeebeProcessInstanceWriter;
  private final ProcessDefinitionReader processDefinitionReader;

  public ZeebeIncidentImportMediatorFactory(
      final BeanFactory beanFactory,
      final ImportIndexHandlerRegistry importIndexHandlerRegistry,
      final ConfigurationService configurationService,
      final ProcessInstanceWriter zeebeProcessInstanceWriter,
      final ProcessDefinitionReader processDefinitionReader,
      final @Qualifier("optimizeObjectMapper") ObjectMapper objectMapper,
      final DatabaseClient databaseClient) {
    super(
        beanFactory,
        importIndexHandlerRegistry,
        configurationService,
        objectMapper,
        databaseClient);
    this.zeebeProcessInstanceWriter = zeebeProcessInstanceWriter;
    this.processDefinitionReader = processDefinitionReader;
  }

  @Override
  public List<ImportMediator> createMediators(final ZeebeDataSourceDto zeebeDataSourceDto) {
    return Collections.singletonList(
        new ZeebeIncidentImportMediator(
            importIndexHandlerRegistry.getZeebeIncidentImportIndexHandler(
                zeebeDataSourceDto.getPartitionId()),
            beanFactory.getBean(
                ZeebeIncidentFetcher.class,
                zeebeDataSourceDto.getPartitionId(),
                databaseClient,
                objectMapper,
                configurationService),
            new ZeebeIncidentImportService(
                configurationService,
                zeebeProcessInstanceWriter,
                zeebeDataSourceDto.getPartitionId(),
                processDefinitionReader,
                databaseClient),
            configurationService,
            new BackoffCalculator(configurationService)));
  }
}
