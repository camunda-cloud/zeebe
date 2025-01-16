/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.exporter.config.ExporterConfiguration.IndexSettings;
import io.camunda.exporter.exceptions.OpensearchExporterException;
import io.camunda.exporter.schema.opensearch.OpensearchEngineClient;
import io.camunda.exporter.utils.SearchDBExtension;
import io.camunda.webapps.schema.descriptors.IndexDescriptor;
import io.camunda.webapps.schema.descriptors.operate.index.ImportPositionIndex;
import io.camunda.webapps.schema.entities.ImportPositionEntity;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.generic.Requests;

public class OpensearchEngineClientIT {

  @RegisterExtension private static SearchDBExtension searchDB = SearchDBExtension.create();

  private static final String TEST_CONTEXT_MARKER = RandomStringUtils.insecure().nextNumeric(10);

  private static OpenSearchClient openSearchClient;
  private static OpensearchEngineClient opensearchEngineClient;

  @BeforeAll
  public static void init() {
    openSearchClient = searchDB.osClient();
    opensearchEngineClient = new OpensearchEngineClient(openSearchClient);
  }

  @Test
  void shouldCreateIndexNormally() throws IOException {
    // given
    final var descriptor =
        SchemaTestUtil.mockIndex(
            "qualified_name-" + TEST_CONTEXT_MARKER,
            "alias-" + TEST_CONTEXT_MARKER,
            "index_name-" + TEST_CONTEXT_MARKER,
            "/mappings.json");

    // when
    final var indexSettings = new IndexSettings();
    opensearchEngineClient.createIndex(descriptor, indexSettings);

    // then
    final var index =
        openSearchClient
            .indices()
            .get(req -> req.index("qualified_name-" + TEST_CONTEXT_MARKER))
            .get("qualified_name-" + TEST_CONTEXT_MARKER);

    SchemaTestUtil.validateMappings(index.mappings(), "/mappings.json");

    assertThat(index.aliases().keySet()).isEqualTo(Set.of("alias-" + TEST_CONTEXT_MARKER));
    assertThat(index.settings().index().numberOfReplicas())
        .isEqualTo(indexSettings.getNumberOfReplicas().toString());
    assertThat(index.settings().index().numberOfShards())
        .isEqualTo(indexSettings.getNumberOfShards().toString());
  }

  @Test
  void shouldCreateIndexTemplate() throws IOException {
    // given
    final var template =
        SchemaTestUtil.mockIndexTemplate(
            "index_name",
            "index_pattern.*",
            "alias",
            List.of(),
            "template_name",
            "/mappings-and-settings.json");

    // when
    final var expectedIndexSettings = new IndexSettings();
    opensearchEngineClient.createIndexTemplate(template, expectedIndexSettings, false);

    // then
    final var createdTemplate =
        openSearchClient
            .indices()
            .getIndexTemplate(req -> req.name("template_name"))
            .indexTemplates();

    assertThat(createdTemplate.size()).isEqualTo(1);

    final var indexSettings =
        createdTemplate
            .getFirst()
            .indexTemplate()
            .template()
            .settings()
            .get("index")
            .toJson()
            .asJsonObject();

    assertThat(indexSettings.getString("number_of_shards"))
        .isEqualTo(expectedIndexSettings.getNumberOfShards().toString());
    assertThat(indexSettings.getString("number_of_replicas"))
        .isEqualTo(expectedIndexSettings.getNumberOfReplicas().toString());
    assertThat(indexSettings.getString("refresh_interval")).isEqualTo("2s");

    SchemaTestUtil.validateMappings(
        createdTemplate.getFirst().indexTemplate().template().mappings(),
        template.getMappingsClasspathFilename());
  }

  @Test
  void shouldNotThrowIfCreatingExistingTemplate() {
    // given
    final var indexTemplate =
        SchemaTestUtil.mockIndexTemplate(
            "index_name",
            "test*",
            "alias",
            Collections.emptyList(),
            "template_name",
            "/mappings.json");

    final var settings = new IndexSettings();
    opensearchEngineClient.createIndexTemplate(indexTemplate, settings, true);

    // when, then
    assertThatNoException()
        .describedAs("Creating an already existing template should not throw")
        .isThrownBy(
            () -> opensearchEngineClient.createIndexTemplate(indexTemplate, settings, true));
  }

  @Test
  void shouldPutMappingCorrectly() throws IOException {
    // given
    final var descriptor =
        SchemaTestUtil.mockIndex("qualified_name", "alias", "index_name", "/mappings.json");
    opensearchEngineClient.createIndex(descriptor, new IndexSettings());

    final Set<IndexMappingProperty> newProperties = new HashSet<>();
    newProperties.add(new IndexMappingProperty("email", Map.of("type", "keyword")));
    newProperties.add(new IndexMappingProperty("age", Map.of("type", "integer")));

    // when
    opensearchEngineClient.putMapping(descriptor, newProperties);

    // then
    final var indices =
        openSearchClient
            .indices()
            .get(req -> req.index(descriptor.getFullQualifiedName()))
            .result();

    assertThat(indices.size()).isEqualTo(1);
    final var properties = indices.get(descriptor.getFullQualifiedName()).mappings().properties();

    assertThat(properties.get("email").isKeyword()).isTrue();
    assertThat(properties.get("age").isInteger()).isTrue();
  }

  @Test
  void shouldRetrieveAllIndexMappingsWithImplementationAgnosticReturnType() {
    // given
    final var index =
        SchemaTestUtil.mockIndex(
            "index_qualified_name_" + TEST_CONTEXT_MARKER,
            "alias_" + TEST_CONTEXT_MARKER,
            "index_name_" + TEST_CONTEXT_MARKER,
            "/mappings-complex-property.json");

    opensearchEngineClient.createIndex(index, new IndexSettings());

    // when
    final var mappings =
        opensearchEngineClient.getMappings(
            "index_qualified_name_" + TEST_CONTEXT_MARKER, MappingSource.INDEX);

    // then
    assertThat(mappings.size()).isEqualTo(1);
    assertThat(mappings.get("index_qualified_name_" + TEST_CONTEXT_MARKER).dynamic())
        .isEqualTo("strict");

    assertThat(mappings.get("index_qualified_name_" + TEST_CONTEXT_MARKER).properties())
        .containsExactlyInAnyOrder(
            new IndexMappingProperty.Builder()
                .name("hello")
                .typeDefinition(
                    Map.of("type", "text", "index", false, "eager_global_ordinals", true))
                .build(),
            new IndexMappingProperty.Builder()
                .name("world")
                .typeDefinition(Map.of("type", "keyword"))
                .build());
  }

  @Test
  void shouldRetrieveAllIndexTemplateMappingsWithImplementationAgnosticReturnType() {
    // given
    final var template =
        SchemaTestUtil.mockIndexTemplate(
            "index_name_" + TEST_CONTEXT_MARKER,
            "index_pattern_" + TEST_CONTEXT_MARKER + ".*",
            "alias_" + TEST_CONTEXT_MARKER,
            List.of(),
            "template_name_" + TEST_CONTEXT_MARKER,
            "/mappings-complex-property.json");

    opensearchEngineClient.createIndexTemplate(template, new IndexSettings(), true);

    // when
    final var templateMappings =
        opensearchEngineClient.getMappings(
            "template_name_" + TEST_CONTEXT_MARKER, MappingSource.INDEX_TEMPLATE);

    // then
    assertThat(templateMappings.size()).isEqualTo(1);
    assertThat(templateMappings.get("template_name_" + TEST_CONTEXT_MARKER).properties())
        .containsExactlyInAnyOrder(
            new IndexMappingProperty.Builder()
                .name("hello")
                .typeDefinition(
                    Map.of("type", "text", "index", false, "eager_global_ordinals", true))
                .build(),
            new IndexMappingProperty.Builder()
                .name("world")
                .typeDefinition(Map.of("type", "keyword"))
                .build());
  }

  @Test
  void shouldNotThrowErrorIfRetrievingMappingsWhereOnlySubsetOfIndicesExist() {
    // given
    final var index =
        SchemaTestUtil.mockIndex("index_qualified_name", "alias", "index_name", "/mappings.json");

    opensearchEngineClient.createIndex(index, new IndexSettings());

    // when, then
    assertThatNoException()
        .isThrownBy(
            () ->
                opensearchEngineClient.getMappings(
                    index.getFullQualifiedName() + "*,foo*", MappingSource.INDEX));
  }

  @Test
  void shouldUpdateSettingsWithPutSettingsRequest() throws IOException {
    // given
    final var index =
        SchemaTestUtil.mockIndex("index_name", "alias", "index_name", "/mappings.json");

    opensearchEngineClient.createIndex(index, new IndexSettings());

    // when
    final Map<String, String> newSettings = Map.of("index.refresh_interval", "5s");
    opensearchEngineClient.putSettings(List.of(index), newSettings);

    // then
    final var indices = openSearchClient.indices().get(req -> req.index("index_name"));

    assertThat(indices.result().size()).isEqualTo(1);
    assertThat(indices.result().get("index_name").settings().index().refreshInterval().time())
        .isEqualTo("5s");
  }

  @Test
  @DisabledIfSystemProperty(
      named = SearchDBExtension.IT_OPENSEARCH_AWS_INSTANCE_URL_PROPERTY,
      matches = "^(?=\\s*\\S).*$",
      disabledReason = "Excluding from AWS OS IT CI - policies not allowed for shared DBs")
  void shouldCreateIndexLifeCyclePolicy() throws IOException {
    // given, when
    opensearchEngineClient.putIndexLifeCyclePolicy("policy_name", "20d");

    // then
    final var req =
        Requests.builder().method("GET").endpoint("/_plugins/_ism/policies/policy_name").build();
    try (final var response = openSearchClient.generic().execute(req)) {
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(
              new ObjectMapper()
                  .readTree(response.getBody().get().body())
                  .get("policy")
                  .get("states")
                  .get(0)
                  .get("transitions")
                  .get(0)
                  .get("conditions")
                  .get("min_index_age")
                  .asText())
          .isEqualTo("20d");
    }
  }

  @Test
  void shouldFailIfIndexStateManagementPolicyInvalid() {
    // given, when, then
    assertThatThrownBy(
            () -> opensearchEngineClient.putIndexLifeCyclePolicy("policy_name", "test123"))
        .isInstanceOf(OpensearchExporterException.class)
        .hasMessageContaining(
            "Creating index state management policy [policy_name] with min_deletion_age [test123] failed.");
  }

  @Nested
  class ImportersCompleted {
    final String indexPrefix = "";
    final int partitionId = 1;
    final IndexDescriptor importPositionIndex = new ImportPositionIndex(indexPrefix, true);

    @BeforeEach
    void setup() throws IOException {
      openSearchClient
          .indices()
          .delete(r -> r.index(importPositionIndex.getFullQualifiedName()).ignoreUnavailable(true));
      opensearchEngineClient.createIndex(importPositionIndex, new IndexSettings());
    }

    @Test
    void shouldReturnRecordReadersCompletedIfAllReadersCompletedFieldIsTrue() throws IOException {
      // given, when
      openSearchClient.bulk(createImportPositionDocuments(partitionId, importPositionIndex));
      openSearchClient.indices().refresh();

      // then
      final var importersCompleted =
          opensearchEngineClient.importersCompleted(partitionId, List.of(importPositionIndex));
      assertThat(importersCompleted).isEqualTo(true);
    }

    @Test
    void shouldReturnRecordReadersNotCompletedIfSomeReadersCompletedFieldIsFalse()
        throws IOException {
      openSearchClient.bulk(createImportPositionDocuments(partitionId, importPositionIndex));

      final var decisionEntity =
          new ImportPositionEntity().setPartitionId(partitionId).setAliasName("decision");

      final var updateRequest =
          new UpdateRequest.Builder<ImportPositionEntity, Map<String, Boolean>>()
              .id(decisionEntity.getId())
              .index(importPositionIndex.getFullQualifiedName())
              .doc(Map.of("completed", false))
              .build();

      openSearchClient.update(updateRequest, ImportPositionEntity.class);

      openSearchClient.indices().refresh();

      final var importersCompleted =
          opensearchEngineClient.importersCompleted(partitionId, List.of(importPositionIndex));
      assertThat(importersCompleted).isEqualTo(false);
    }

    @Test
    void shouldReturnImportersCompletedForFreshInstall() {
      final var importersCompleted =
          opensearchEngineClient.importersCompleted(partitionId, List.of(importPositionIndex));
      assertThat(importersCompleted).isEqualTo(true);
    }

    private BulkRequest createImportPositionDocuments(
        final int partitionId, final IndexDescriptor importPositionIndex) {
      final BulkRequest.Builder br = new BulkRequest.Builder();
      Stream.of("process-instance", "decision", "job")
          .map(
              type ->
                  new ImportPositionEntity()
                      .setCompleted(true)
                      .setPartitionId(partitionId)
                      .setAliasName(type))
          .forEach(
              entity -> {
                br.operations(
                    op ->
                        op.index(
                            i ->
                                i.index(importPositionIndex.getFullQualifiedName())
                                    .id(entity.getId())
                                    .document(entity)));
              });

      return br.build();
    }
  }
}
