/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.store.elasticsearch;

import static io.camunda.tasklist.util.ElasticsearchUtil.QueryType.ONLY_RUNTIME;
import static io.camunda.tasklist.util.ElasticsearchUtil.fromSearchHit;
import static io.camunda.tasklist.util.ElasticsearchUtil.getRawResponseWithTenantCheck;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.tasklist.data.conditionals.ElasticSearchCondition;
import io.camunda.tasklist.exceptions.NotFoundException;
import io.camunda.tasklist.exceptions.TasklistRuntimeException;
import io.camunda.tasklist.store.FormStore;
import io.camunda.tasklist.tenant.TenantAwareElasticsearchClient;
import io.camunda.tasklist.util.ElasticsearchUtil;
import io.camunda.tasklist.v86.entities.FormEntity;
import io.camunda.tasklist.v86.schema.indices.TasklistFormIndex;
import io.camunda.tasklist.v86.schema.indices.TasklistProcessIndex;
import io.camunda.tasklist.v86.schema.templates.TasklistTaskTemplate;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(ElasticSearchCondition.class)
public class FormStoreElasticSearch implements FormStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(FormStoreElasticSearch.class);

  @Autowired private TasklistFormIndex formIndex;

  @Autowired private TasklistTaskTemplate taskTemplate;

  @Autowired private TasklistProcessIndex processIndex;

  @Autowired private TenantAwareElasticsearchClient tenantAwareClient;

  @Autowired
  @Qualifier("tasklistObjectMapper")
  private ObjectMapper objectMapper;

  @Autowired
  @Qualifier("tasklistEsClient")
  private RestHighLevelClient esClient;

  @Override
  public FormEntity getForm(final String id, final String processDefinitionId, final Long version) {
    final FormEntity formEmbedded =
        version == null ? getFormEmbedded(id, processDefinitionId) : null;
    if (formEmbedded != null) {
      return formEmbedded;
    } else if (isFormAssociatedToTask(id, processDefinitionId)) {
      final var formLinked = getLinkedForm(id, version);
      if (formLinked != null) {
        return formLinked;
      }
    } else if (isFormAssociatedToProcess(id, processDefinitionId)) {
      final var formLinked = getLinkedForm(id, version);
      if (formLinked != null) {
        return formLinked;
      }
    }
    throw new NotFoundException(String.format("form with id %s was not found", id));
  }

  @Override
  public List<String> getFormIdsByProcessDefinitionId(final String processDefinitionId) {
    final SearchRequest searchRequest =
        new SearchRequest(formIndex.getFullQualifiedName())
            .source(
                SearchSourceBuilder.searchSource()
                    .query(termQuery(TasklistFormIndex.PROCESS_DEFINITION_ID, processDefinitionId))
                    .fetchField(TasklistFormIndex.ID));
    try {
      return ElasticsearchUtil.scrollIdsToList(searchRequest, esClient);
    } catch (final IOException e) {
      throw new TasklistRuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public Optional<FormIdView> getHighestVersionFormByKey(final String formKey) {

    final SearchSourceBuilder searchSourceBuilder =
        new SearchSourceBuilder()
            .query(QueryBuilders.termQuery(TasklistFormIndex.ID, formKey))
            .sort(TasklistFormIndex.VERSION, SortOrder.DESC)
            .size(1)
            .fetchSource(
                new String[] {
                  TasklistFormIndex.ID, TasklistFormIndex.BPMN_ID, TasklistFormIndex.VERSION
                },
                null);

    final SearchRequest searchRequest =
        new SearchRequest(formIndex.getFullQualifiedName()).source(searchSourceBuilder);

    try {
      final SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);

      if (searchResponse.getHits().getHits().length > 0) {
        // Extract the source and map it to your FormEntity object
        final Map<String, Object> sourceAsMap =
            searchResponse.getHits().getHits()[0].getSourceAsMap();
        return Optional.of(
            new FormIdView(
                (String) sourceAsMap.get(TasklistFormIndex.ID),
                (String) sourceAsMap.get(TasklistFormIndex.BPMN_ID),
                ((Number) sourceAsMap.get(TasklistFormIndex.VERSION)).longValue()));
      }
    } catch (final IOException e) {
      throw new TasklistRuntimeException(
          String.format("Error retrieving the last version for the formKey: %s", formKey), e);
    }
    return Optional.empty();
  }

  private FormEntity getFormEmbedded(final String id, final String processDefinitionId) {
    try {
      final String formId = String.format("%s_%s", processDefinitionId, id);
      final var formSearchHit =
          getRawResponseWithTenantCheck(formId, formIndex, ONLY_RUNTIME, tenantAwareClient);
      return fromSearchHit(formSearchHit.getSourceAsString(), objectMapper, FormEntity.class);
    } catch (final IOException e) {
      throw new TasklistRuntimeException(e.getMessage(), e);
    } catch (final NotFoundException e) {
      return null;
    }
  }

  private FormEntity getLinkedForm(final String formId, final Long formVersion) {
    final SearchRequest searchRequest = new SearchRequest(formIndex.getFullQualifiedName());
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    boolQuery.must(
        QueryBuilders.boolQuery()
            .should(QueryBuilders.termQuery(TasklistFormIndex.BPMN_ID, formId))
            .should(QueryBuilders.termQuery(TasklistFormIndex.ID, formId))
            .minimumShouldMatch(1));
    if (formVersion != null) {
      // with the version set, you can return the form that was deleted, because of backward
      // compatibility
      boolQuery.must(QueryBuilders.termQuery(TasklistFormIndex.VERSION, formVersion));
    } else {
      // get the latest version where isDeleted is false (highest active version)
      boolQuery.must(QueryBuilders.termQuery(TasklistFormIndex.IS_DELETED, false));
      searchSourceBuilder.sort(TasklistFormIndex.VERSION, SortOrder.DESC);
      searchSourceBuilder.size(1);
    }

    searchSourceBuilder.query(boolQuery);
    searchRequest.source(searchSourceBuilder);

    try {
      final SearchResponse searchResponse = tenantAwareClient.search(searchRequest);

      if (searchResponse.getHits().getHits().length > 0) {
        final Map<String, Object> sourceAsMap =
            searchResponse.getHits().getHits()[0].getSourceAsMap();
        final FormEntity formEntity = new FormEntity();
        formEntity.setBpmnId((String) sourceAsMap.get(TasklistFormIndex.BPMN_ID));
        formEntity.setVersion(((Number) sourceAsMap.get(TasklistFormIndex.VERSION)).longValue());
        formEntity.setEmbedded((Boolean) sourceAsMap.get(TasklistFormIndex.EMBEDDED));
        formEntity.setSchema((String) sourceAsMap.get(TasklistFormIndex.SCHEMA));
        formEntity.setTenantId((String) sourceAsMap.get(TasklistFormIndex.TENANT_ID));
        formEntity.setIsDeleted((Boolean) sourceAsMap.get(TasklistFormIndex.IS_DELETED));
        return formEntity;
      }
    } catch (final IOException e) {
      final String formIdNotFoundMessage =
          String.format("Error retrieving the version for the formId: [%s]", formId);
      throw new TasklistRuntimeException(formIdNotFoundMessage);
    }
    return null;
  }

  private Boolean isFormAssociatedToTask(final String formId, final String processDefinitionId) {
    try {
      final BoolQueryBuilder boolQuery =
          QueryBuilders.boolQuery()
              .must(
                  QueryBuilders.boolQuery()
                      .should(QueryBuilders.matchQuery(TasklistTaskTemplate.FORM_ID, formId))
                      .should(QueryBuilders.matchQuery(TasklistTaskTemplate.FORM_KEY, formId))
                      .minimumShouldMatch(1))
              .must(
                  QueryBuilders.matchQuery(
                      TasklistTaskTemplate.PROCESS_DEFINITION_ID, processDefinitionId));

      final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(boolQuery);

      final SearchRequest searchRequest =
          ElasticsearchUtil.createSearchRequest(taskTemplate, ElasticsearchUtil.QueryType.ALL);
      searchRequest.source(searchSourceBuilder);

      final SearchResponse searchResponse = tenantAwareClient.search(searchRequest);

      return searchResponse.getHits().getTotalHits().value > 0;
    } catch (final IOException e) {
      final String formIdNotFoundMessage =
          String.format("Error retrieving the version for the formId: [%s]", formId);
      throw new TasklistRuntimeException(formIdNotFoundMessage);
    }
  }

  private Boolean isFormAssociatedToProcess(final String formId, final String processDefinitionId) {
    try {
      final BoolQueryBuilder boolQuery =
          QueryBuilders.boolQuery()
              .must(QueryBuilders.matchQuery(TasklistProcessIndex.FORM_ID, formId))
              .must(QueryBuilders.matchQuery(TasklistProcessIndex.ID, processDefinitionId));

      final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(boolQuery);

      final SearchRequest searchRequest =
          ElasticsearchUtil.createSearchRequest(processIndex, ElasticsearchUtil.QueryType.ALL);
      searchRequest.source(searchSourceBuilder);

      final SearchResponse searchResponse = tenantAwareClient.search(searchRequest);

      return searchResponse.getHits().getTotalHits().value > 0;
    } catch (final IOException e) {
      final String formIdNotFoundMessage =
          String.format("Error retrieving the version for the formId: [%s]", formId);
      throw new TasklistRuntimeException(formIdNotFoundMessage);
    }
  }
}
