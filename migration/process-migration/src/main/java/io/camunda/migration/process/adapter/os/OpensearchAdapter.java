/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migration.process.adapter.os;

import io.camunda.migration.api.MigrationException;
import io.camunda.migration.process.MigrationRepositoryIndex;
import io.camunda.migration.process.ProcessMigrationProperties;
import io.camunda.migration.process.ProcessorStep;
import io.camunda.migration.process.adapter.Adapter;
import io.camunda.search.connect.configuration.ConnectConfiguration;
import io.camunda.search.connect.os.OpensearchConnector;
import io.camunda.webapps.schema.descriptors.operate.index.ProcessIndex;
import io.camunda.webapps.schema.entities.operate.ProcessEntity;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpensearchAdapter implements Adapter {

  private static final Logger LOG = LoggerFactory.getLogger(OpensearchAdapter.class);
  private final ProcessMigrationProperties properties;
  private final OpenSearchClient client;
  private final MigrationRepositoryIndex migrationRepositoryIndex;
  private final ProcessIndex processIndex;

  public OpensearchAdapter(
      final ProcessMigrationProperties properties,
      final ConnectConfiguration connectConfiguration) {
    this.properties = properties;
    migrationRepositoryIndex =
        new MigrationRepositoryIndex(connectConfiguration.getIndexPrefix(), false);
    processIndex = new ProcessIndex(connectConfiguration.getIndexPrefix(), false);
    client = new OpensearchConnector(connectConfiguration).createClient();
  }

  @Override
  public String migrate(final List<ProcessEntity> entities) throws MigrationException {
    final BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
    final var idList = entities.stream().map(ProcessEntity::getId).toList();
    entities.forEach(e -> migrateEntity(e, bulkRequest));

    final BulkResponse response;
    try {
      response =
          doWithRetry(
              "Migrate entities %s".formatted(idList),
              () -> client.bulk(bulkRequest.build()),
              (res) -> res == null || res.errors() || res.items().isEmpty());
    } catch (final Exception e) {
      throw new MigrationException("Failed to migrate entities %s".formatted(idList), e);
    }
    return lastUpdatedProcessDefinition(response.items());
  }

  @Override
  public List<ProcessEntity> nextBatch(final String processDefinitionKey) {
    final SearchRequest request =
        new SearchRequest.Builder()
            .index(processIndex.getFullQualifiedName())
            .size(properties.getBatchSize())
            .sort(s -> s.field(f -> f.field(PROCESS_DEFINITION_KEY).order(SortOrder.Asc)))
            .query(
                q ->
                    q.range(
                        r ->
                            r.field(PROCESS_DEFINITION_KEY)
                                .gt(
                                    JsonData.of(
                                        processDefinitionKey == null ? "" : processDefinitionKey))))
            .build();

    final SearchResponse<ProcessEntity> searchResponse;
    try {
      searchResponse =
          doWithRetry(
              "Fetching next process batch",
              () -> client.search(request, ProcessEntity.class),
              res -> res.timedOut() || Boolean.TRUE.equals(res.terminatedEarly()));
    } catch (final Exception e) {
      throw new MigrationException("Failed to fetch next processes batch", e);
    }
    return searchResponse.hits().hits().stream().map(Hit::source).toList();
  }

  @Override
  public String readLastMigratedEntity() throws MigrationException {
    final SearchRequest request =
        new SearchRequest.Builder()
            .index(migrationRepositoryIndex.getFullQualifiedName())
            .size(1)
            .query(
                q ->
                    q.bool(
                        b ->
                            b.must(
                                    m ->
                                        m.match(
                                            t ->
                                                t.field(MigrationRepositoryIndex.TYPE)
                                                    .query(FieldValue.of(PROCESSOR_STEP_TYPE))))
                                .must(
                                    m ->
                                        m.term(
                                            t ->
                                                t.field(MigrationRepositoryIndex.ID)
                                                    .value(FieldValue.of(PROCESSOR_STEP_ID))))))
            .build();

    final SearchResponse<ProcessorStep> searchResponse;

    try {
      searchResponse =
          doWithRetry(
              "Fetching last migrated process",
              () -> client.search(request, ProcessorStep.class),
              res -> res.timedOut() || Boolean.TRUE.equals(res.terminatedEarly()));
    } catch (final Exception e) {
      throw new MigrationException("Failed to fetch last migrated process", e);
    }

    return searchResponse.hits().hits().stream()
        .map(Hit::source)
        .filter(Objects::nonNull)
        .map(ProcessorStep::getContent)
        .findFirst()
        .orElse(null);
  }

  @Override
  public void writeLastMigratedEntity(final String processDefinitionKey) throws MigrationException {
    final UpdateRequest<ProcessorStep, ProcessorStep> updateRequest =
        new UpdateRequest.Builder<ProcessorStep, ProcessorStep>()
            .index(migrationRepositoryIndex.getFullQualifiedName())
            .id(PROCESSOR_STEP_ID)
            .docAsUpsert(true)
            .doc(upsertProcessorStep(processDefinitionKey))
            .build();

    try {
      doWithRetry(
          "Update last migrated process",
          () -> client.update(updateRequest, ProcessorStep.class),
          res -> res.result() == null);
    } catch (final Exception e) {
      throw new MigrationException("Failed to update migrated process", e);
    }
  }

  @Override
  public void close() throws IOException {
    client._transport().close();
  }

  @Override
  public int getMaxRetries() {
    return properties.getMaxRetries();
  }

  @Override
  public int getMinDelayInSeconds() {
    return properties.getMinRetryDelayInSeconds();
  }

  @Override
  public int getMaxDelayInSeconds() {
    return properties.getMaxRetryDelayInSeconds();
  }

  private void migrateEntity(final ProcessEntity entity, final BulkRequest.Builder bulkRequest) {

    bulkRequest.operations(
        op ->
            op.update(
                e ->
                    e.index(processIndex.getFullQualifiedName())
                        .id(entity.getId())
                        .document(getUpdateMap(entity))));
  }

  private String lastUpdatedProcessDefinition(final List<BulkResponseItem> items) {
    final var sorted = items.stream().sorted(Comparator.comparing(BulkResponseItem::id)).toList();
    for (int i = 0; i < sorted.size(); i++) {
      if (sorted.get(i).error() != null) {
        return i > 0
            ? Objects.requireNonNull(sorted.get(i - 1).id())
            : Objects.requireNonNull(sorted.get(i).id());
      }
    }

    return Objects.requireNonNull(sorted.getLast().id());
  }
}
