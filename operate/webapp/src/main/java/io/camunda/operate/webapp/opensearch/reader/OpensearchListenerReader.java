/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.opensearch.reader;

import static io.camunda.operate.store.opensearch.dsl.QueryDSL.*;
import static io.camunda.operate.store.opensearch.dsl.RequestDSL.searchRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.conditions.OpensearchCondition;
import io.camunda.operate.entities.JobEntity;
import io.camunda.operate.entities.ListenerType;
import io.camunda.operate.schema.templates.JobTemplate;
import io.camunda.operate.store.opensearch.client.sync.RichOpenSearchClient;
import io.camunda.operate.webapp.reader.ListenerReader;
import io.camunda.operate.webapp.rest.dto.ListenerDto;
import io.camunda.operate.webapp.rest.dto.ListenerRequestDto;
import io.camunda.operate.webapp.rest.dto.ListenerResponseDto;
import io.camunda.operate.webapp.rest.dto.SortingDto;
import java.util.List;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.search.SearchResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional(OpensearchCondition.class)
@Component
public class OpensearchListenerReader extends OpensearchAbstractReader implements ListenerReader {

  private final JobTemplate jobTemplate;
  private final RichOpenSearchClient richOpenSearchClient;

  private final ObjectMapper objectMapper;

  public OpensearchListenerReader(
      final JobTemplate jobTemplate,
      final RichOpenSearchClient richOpenSearchClient,
      @Qualifier("operateObjectMapper") final ObjectMapper objectMapper) {
    this.jobTemplate = jobTemplate;
    this.richOpenSearchClient = richOpenSearchClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public ListenerResponseDto getListenerExecutions(
      final String processInstanceId, final ListenerRequestDto request) {
    final Query query =
        and(
            term(JobTemplate.PROCESS_INSTANCE_KEY, processInstanceId),
            term(JobTemplate.FLOW_NODE_ID, request.getFlowNodeId()),
            or(
                term(JobTemplate.JOB_KIND, ListenerType.EXECUTION_LISTENER.name()),
                term(JobTemplate.JOB_KIND, ListenerType.TASK_LISTENER.name())));
    final var searchRequest =
        searchRequestBuilder(jobTemplate.getAlias())
            .query(query)
            .sort(
                sortOptions(
                    request.getSorting().getSortBy(),
                    getSortOrder(request.getSorting().getSortOrder())));

    final SearchResult<JobEntity> searchResult =
        richOpenSearchClient.doc().search(searchRequest, JobEntity.class);
    final Long totalHitCount = searchResult.hits().total().value();
    final List<ListenerDto> listenerDtos =
        searchResult.hits().hits().stream()
            .map(
                hit -> {
                  final JobEntity entity = hit.source();
                  return ListenerDto.fromJobEntity(entity);
                })
            .collect(Collectors.toUnmodifiableList());
    return new ListenerResponseDto(listenerDtos, totalHitCount);
  }

  private SortOrder getSortOrder(final String sortOrder) {
    if (sortOrder.equals(SortingDto.SORT_ORDER_DESC_VALUE)) {
      return SortOrder.Desc;
    }
    return SortOrder.Asc;
  }
}
