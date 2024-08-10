/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service.zeebe;

import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
import io.camunda.optimize.dto.optimize.datasource.ZeebeDataSourceDto;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.reader.ProcessDefinitionReader;
import io.camunda.optimize.service.db.writer.ZeebeProcessInstanceWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.service.ImportService;
import io.camunda.optimize.service.importing.job.ZeebeProcessInstanceDatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ZeebeProcessInstanceSubEntityImportService<T> implements ImportService<T> {

  protected final DatabaseImportJobExecutor databaseImportJobExecutor;
  private final ZeebeProcessInstanceWriter processInstanceWriter;
  protected final ConfigurationService configurationService;
  protected final ProcessDefinitionReader processDefinitionReader;
  protected final int partitionId;
  private final DatabaseClient databaseClient;
  private final String sourceExportIndex;

  protected ZeebeProcessInstanceSubEntityImportService(
      final ConfigurationService configurationService,
      final ZeebeProcessInstanceWriter processInstanceWriter,
      final int partitionId,
      final ProcessDefinitionReader processDefinitionReader,
      final DatabaseClient databaseClient,
      final String sourceExportIndex) {
    this.databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.processInstanceWriter = processInstanceWriter;
    this.partitionId = partitionId;
    this.configurationService = configurationService;
    this.processDefinitionReader = processDefinitionReader;
    this.databaseClient = databaseClient;
    this.sourceExportIndex = sourceExportIndex;
  }

  abstract List<ProcessInstanceDto> filterAndMapZeebeRecordsToOptimizeEntities(List<T> records);

  @Override
  public void executeImport(final List<T> zeebeRecords, final Runnable importCompleteCallback) {
    boolean newDataIsAvailable = !zeebeRecords.isEmpty();
    if (newDataIsAvailable) {
      final List<ProcessInstanceDto> newOptimizeEntities =
          filterAndMapZeebeRecordsToOptimizeEntities(zeebeRecords);
      final DatabaseImportJob<ProcessInstanceDto> databaseImportJob =
          createDatabaseImportJob(newOptimizeEntities, importCompleteCallback);
      addDatabaseImportJobToQueue(databaseImportJob);
    }
  }

  @Override
  public DatabaseImportJobExecutor getDatabaseImportJobExecutor() {
    return databaseImportJobExecutor;
  }

  protected ProcessInstanceDto createSkeletonProcessInstance(
      final String processDefinitionKey,
      final Long processInstanceId,
      final Long processDefinitionId,
      final String tenantId) {
    final ProcessInstanceDto processInstanceDto = new ProcessInstanceDto();
    processInstanceDto.setProcessDefinitionKey(processDefinitionKey);
    processInstanceDto.setProcessInstanceId(String.valueOf(processInstanceId));
    processInstanceDto.setProcessDefinitionId(String.valueOf(processDefinitionId));
    processInstanceDto.setTenantId(tenantId);
    processInstanceDto.setDataSource(
        new ZeebeDataSourceDto(configurationService.getConfiguredZeebe().getName(), partitionId));
    return processInstanceDto;
  }

  private void addDatabaseImportJobToQueue(
      DatabaseImportJob<ProcessInstanceDto> databaseImportJob) {
    databaseImportJobExecutor.executeImportJob(databaseImportJob);
  }

  private DatabaseImportJob<ProcessInstanceDto> createDatabaseImportJob(
      final List<ProcessInstanceDto> processInstanceDtos, final Runnable importCompleteCallback) {
    ZeebeProcessInstanceDatabaseImportJob processInstanceImportJob =
        new ZeebeProcessInstanceDatabaseImportJob(
            processInstanceWriter,
            configurationService,
            importCompleteCallback,
            sourceExportIndex,
            databaseClient);
    processInstanceImportJob.setEntitiesToImport(processInstanceDtos);
    return processInstanceImportJob;
  }
}
