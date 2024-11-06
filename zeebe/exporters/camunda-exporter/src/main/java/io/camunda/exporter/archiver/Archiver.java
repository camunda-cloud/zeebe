/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.archiver;

import io.camunda.exporter.ExporterResourceProvider;
import io.camunda.exporter.archiver.ArchiverRepository.NoopArchiverRepository;
import io.camunda.exporter.config.ConnectionTypes;
import io.camunda.exporter.config.ExporterConfiguration;
import io.camunda.exporter.config.ExporterConfiguration.ArchiverConfiguration;
import io.camunda.exporter.metrics.CamundaExporterMetrics;
import io.camunda.search.connect.es.ElasticsearchConnector;
import io.camunda.webapps.schema.descriptors.operate.ProcessInstanceDependant;
import io.camunda.webapps.schema.descriptors.operate.template.BatchOperationTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate;
import io.camunda.webapps.schema.descriptors.tasklist.template.TaskTemplate;
import io.camunda.zeebe.util.CloseableSilently;
import io.camunda.zeebe.util.VisibleForTesting;
import io.camunda.zeebe.util.error.FatalErrorHandler;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import javax.annotation.WillCloseWhenClosed;
import org.agrona.CloseHelper;
import org.slf4j.Logger;

public final class Archiver implements CloseableSilently {
  private final int partitionId;
  private final ArchiverRepository repository;
  private final Logger logger;
  private final ScheduledExecutorService executor;

  @VisibleForTesting
  Archiver(
      final int partitionId,
      final @WillCloseWhenClosed ArchiverRepository repository,
      final Logger logger,
      final @WillCloseWhenClosed ScheduledExecutorService executor) {
    this.partitionId = partitionId;
    this.repository = Objects.requireNonNull(repository, "must specify a repository");
    this.logger = Objects.requireNonNull(logger, "must specify a logger");
    this.executor = Objects.requireNonNull(executor, "must specify an executor");
  }

  @Override
  public void close() {
    // Close executor first before anything else; this will ensure any callbacks are not triggered
    // in case we close any underlying resource (e.g. repository) and would want to perform
    // unnecessary error handling in any of these callbacks
    //
    // avoid calling executor.close, which will await 1d (!) until termination
    // we also don't need to wait for the jobs to fully finish, as we should be able to handle
    // partial jobs (e.g. node crash/restart)
    executor.shutdownNow();
    // TODO: any closed resources used in the jobs should handle cases where it's been closed
    CloseHelper.close(
        error ->
            logger.warn("Failed to close archiver repository for partition {}", partitionId, error),
        repository);
  }

  private void start(final ArchiverConfiguration config, final ArchiverJob... jobs) {
    logger.debug("Starting {} archiver jobs", jobs.length);
    for (final ArchiverJob job : jobs) {
      executor.submit(
          new ReschedulingTask(
              job, config.getRolloverBatchSize(), config.getDelayBetweenRuns(), executor, logger));
    }
  }

  public static Archiver create(
      final int partitionId,
      final String exporterId,
      final ExporterConfiguration config,
      final ExporterResourceProvider resourceProvider,
      final CamundaExporterMetrics metrics,
      final Logger logger) {
    final var threadFactory =
        Thread.ofPlatform()
            .name("exporter-" + exporterId + "-p" + partitionId + "-bg-", 0)
            .uncaughtExceptionHandler(FatalErrorHandler.uncaughtExceptionHandler(logger))
            .factory();
    final var executor = defaultExecutor(threadFactory);
    final var repository =
        createRepository(config, resourceProvider, partitionId, executor, metrics, logger);
    final var archiver = new Archiver(partitionId, repository, logger, executor);
    final var processInstanceJob =
        createProcessInstanceJob(metrics, logger, resourceProvider, repository, executor);
    final var batchOperationJob =
        createBatchOperationJob(metrics, logger, resourceProvider, repository, executor);

    archiver.start(config.getArchiver(), processInstanceJob, batchOperationJob);
    return archiver;
  }

  private static ProcessInstancesArchiverJob createProcessInstanceJob(
      final CamundaExporterMetrics metrics,
      final Logger logger,
      final ExporterResourceProvider resourceProvider,
      final ArchiverRepository repository,
      final ScheduledThreadPoolExecutor executor) {
    final var dependantTemplates = new ArrayList<ProcessInstanceDependant>();
    resourceProvider.getIndexTemplateDescriptors().stream()
        .filter(ProcessInstanceDependant.class::isInstance)
        .map(ProcessInstanceDependant.class::cast)
        .forEach(dependantTemplates::add);

    // add a special case just for TaskTemplate, which has 2 kinds of documents in the same
    // index
    final var taskTemplate = resourceProvider.getIndexTemplateDescriptor(TaskTemplate.class);
    dependantTemplates.add(
        new ProcessInstanceDependantAdapter(taskTemplate.getFullQualifiedName(), TaskTemplate.ID));

    return new ProcessInstancesArchiverJob(
        repository,
        resourceProvider.getIndexTemplateDescriptor(ListViewTemplate.class),
        dependantTemplates,
        metrics,
        logger,
        executor);
  }

  private static BatchOperationArchiverJob createBatchOperationJob(
      final CamundaExporterMetrics metrics,
      final Logger logger,
      final ExporterResourceProvider resourceProvider,
      final ArchiverRepository repository,
      final ScheduledThreadPoolExecutor executor) {

    return new BatchOperationArchiverJob(
        repository,
        resourceProvider.getIndexTemplateDescriptor(BatchOperationTemplate.class),
        metrics,
        logger,
        executor);
  }

  private static ScheduledThreadPoolExecutor defaultExecutor(final ThreadFactory threadFactory) {
    // TODO: set size to 2 in case we need to do batch operations
    final var executor = new ScheduledThreadPoolExecutor(1, threadFactory);
    executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    executor.setRemoveOnCancelPolicy(true);

    return executor;
  }

  private static ArchiverRepository createRepository(
      final ExporterConfiguration config,
      final ExporterResourceProvider resourceProvider,
      final int partitionId,
      final Executor executor,
      final CamundaExporterMetrics metrics,
      final Logger logger) {
    final var listViewTemplate =
        resourceProvider.getIndexTemplateDescriptor(ListViewTemplate.class);
    final var batchOperationTemplate =
        resourceProvider.getIndexTemplateDescriptor(BatchOperationTemplate.class);
    return switch (ConnectionTypes.from(config.getConnect().getType())) {
      case ELASTICSEARCH -> {
        final var connector = new ElasticsearchConnector(config.getConnect());
        yield new ElasticsearchRepository(
            partitionId,
            config.getArchiver(),
            config.getRetention(),
            listViewTemplate.getFullQualifiedName(),
            batchOperationTemplate.getFullQualifiedName(),
            connector.createAsyncClient(),
            executor,
            metrics,
            logger);
      }
      default -> new NoopArchiverRepository();
    };
  }

  private record ProcessInstanceDependantAdapter(String name, String field)
      implements ProcessInstanceDependant {

    @Override
    public String getFullQualifiedName() {
      return name;
    }

    @Override
    public String getProcessInstanceDependantField() {
      return field;
    }
  }
}
