/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.exporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.elastic.clients.elasticsearch._types.Refresh;
import io.camunda.zeebe.exporter.TestClient.ComponentTemplatesDto.ComponentTemplateWrapper;
import io.camunda.zeebe.exporter.TestClient.IndexTemplatesDto.IndexTemplateWrapper;
import io.camunda.zeebe.exporter.dto.Template;
import io.camunda.zeebe.protocol.record.ValueType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.agrona.CloseHelper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Execution(ExecutionMode.CONCURRENT)
final class ElasticClientIT {
  // configuring a superuser will allow us to create more users, which will let us test
  // authentication
  @Container
  private static final ElasticsearchContainer CONTAINER =
      TestSupport.createDefaultContainer()
          .withEnv("xpack.license.self_generated.type", "trial")
          .withEnv("xpack.security.enabled", "true")
          .withEnv("xpack.security.authc.anonymous.username", "anon")
          .withEnv("xpack.security.authc.anonymous.roles", "superuser")
          .withEnv("xpack.security.authc.anonymous.authz_exception", "true");

  private final ElasticsearchExporterConfiguration config =
      new ElasticsearchExporterConfiguration();
  private final TemplateReader templateReader = new TemplateReader(config.index);
  private final IndexRouter indexRouter = new IndexRouter(config.index);

  private TestClient testClient;
  private ElasticClient client;
  private ArrayList<String> bulkRequest;

  @BeforeEach
  public void beforeEach() {
    // as all tests use the same endpoint, we need a per-test unique prefix
    config.index.prefix = UUID.randomUUID() + "-test-record";
    config.url = CONTAINER.getHttpHostAddress();

    bulkRequest = new ArrayList<>();
    // set the metrics directly to avoid having to index a record to create them
    client = new ElasticClient(config, bulkRequest).setMetrics(new ElasticsearchMetrics(1));
    testClient = new TestClient(config, indexRouter);
  }

  @AfterEach
  void afterEach() {
    CloseHelper.quietCloseAll(testClient, client);
  }

  @Test
  void shouldThrowExceptionIfFailToFlushBulk() {
    // bypass indexing to add some invalid JSON
    bulkRequest.add("{\"index\": {\"_index\": \"something\"}}\n{some");

    // when/then
    assertThatThrownBy(client::flush)
        .isInstanceOf(ElasticsearchExporterException.class)
        .hasMessageContaining(
            "Failed to flush 1 item(s) of bulk request [type: mapper_parsing_exception, reason: failed to parse]");
  }

  @Test
  void shouldPutIndexTemplate() {
    // given
    final var valueType = ValueType.VARIABLE;
    final String indexTemplateName = indexRouter.indexPrefixForValueType(valueType);
    final String indexTemplateAlias = indexRouter.aliasNameForValueType(valueType);
    final Template expectedTemplate =
        templateReader.readIndexTemplate(
            valueType, indexRouter.searchPatternForValueType(valueType), indexTemplateAlias);

    // required since all index templates are composed with it
    client.putComponentTemplate();

    // when
    client.putIndexTemplate(valueType);

    // then
    final var templateWrapper = testClient.getIndexTemplate(valueType);
    assertThat(templateWrapper)
        .as("should have created template for value type %s", valueType)
        .isPresent()
        .get()
        .extracting(IndexTemplateWrapper::name)
        .isEqualTo(indexTemplateName);

    final var template = templateWrapper.get().template();
    assertIndexTemplate(template, expectedTemplate);
  }

  @Test
  void shouldPutComponentTemplate() {
    // given
    final Template expectedTemplate = templateReader.readComponentTemplate();

    // when
    client.putComponentTemplate();

    // then
    final var templateWrapper = testClient.getComponentTemplate();
    assertThat(templateWrapper)
        .as("should have created component template")
        .isPresent()
        .get()
        .extracting(ComponentTemplateWrapper::name)
        .isEqualTo(config.index.prefix);

    final var template = templateWrapper.get().template();
    assertIndexTemplate(template, expectedTemplate);
  }

  @Test
  void shouldAuthenticateWithBasicAuth() throws IOException {
    // given
    testClient
        .getEsClient()
        .security()
        .putUser(
            b -> b.username("user").password("password").refresh(Refresh.True).roles("superuser"));
    config.getAuthentication().setUsername("user");
    config.getAuthentication().setPassword("password");

    // when
    // force recreating the client
    final var authenticatedClient = new ElasticClient(config, bulkRequest);
    authenticatedClient.putComponentTemplate();

    // then
    assertThat(testClient.getComponentTemplate()).isPresent();
  }

  private void assertIndexTemplate(final Template actualTemplate, final Template expectedTemplate) {
    assertThat(actualTemplate.patterns()).isEqualTo(expectedTemplate.patterns());
    assertThat(actualTemplate.composedOf()).isEqualTo(expectedTemplate.composedOf());
    assertThat(actualTemplate.priority()).isEqualTo(expectedTemplate.priority());
    assertThat(actualTemplate.version()).isEqualTo(expectedTemplate.version());
    assertThat(actualTemplate.template().aliases())
        .isEqualTo(expectedTemplate.template().aliases());
    assertThat(actualTemplate.template().mappings())
        .isEqualTo(expectedTemplate.template().mappings());

    // cannot compare settings because we never get flat settings, instead we get { index : {
    // number_of_shards: 1, queries: { cache : { enabled : false } } } }
    // so instead we decompose how we compare the settings. I've tried with flat_settings parameter
    // but that doesn't seem to be doing anything
    assertThat(actualTemplate.template().settings())
        .as("should contain a map of index settings")
        .extractingByKey("index")
        .isInstanceOf(Map.class)
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
        .containsEntry("number_of_shards", "1")
        .containsEntry("number_of_replicas", "0")
        .containsEntry("queries", Map.of("cache", Map.of("enabled", "false")));
  }
}
