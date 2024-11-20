/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.clients.transformers.filter;

import static io.camunda.search.clients.query.SearchQueryBuilders.and;
import static io.camunda.search.clients.query.SearchQueryBuilders.dateTimeOperations;
import static io.camunda.search.clients.query.SearchQueryBuilders.intOperations;
import static io.camunda.search.clients.query.SearchQueryBuilders.longOperations;
import static io.camunda.search.clients.query.SearchQueryBuilders.stringOperations;
import static io.camunda.search.clients.query.SearchQueryBuilders.term;
import static io.camunda.webapps.schema.descriptors.IndexDescriptor.TENANT_ID;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.BPMN_PROCESS_ID;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.END_DATE;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.INCIDENT;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.JOIN_RELATION;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.KEY;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.PARENT_FLOW_NODE_INSTANCE_KEY;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.PARENT_PROCESS_INSTANCE_KEY;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.PROCESS_INSTANCE_JOIN_RELATION;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.PROCESS_KEY;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.PROCESS_NAME;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.PROCESS_VERSION;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.PROCESS_VERSION_TAG;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.START_DATE;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.STATE;
import static io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate.TREE_PATH;
import static java.util.Optional.ofNullable;

import io.camunda.search.clients.query.SearchQuery;
import io.camunda.search.filter.ProcessInstanceFilter;
import java.util.ArrayList;
import java.util.List;

public final class ProcessInstanceFilterTransformer
    implements FilterTransformer<ProcessInstanceFilter> {

  @Override
  public SearchQuery toSearchQuery(final ProcessInstanceFilter filter) {
    final var queries = new ArrayList<SearchQuery>();
    ofNullable(getIsProcessInstanceQuery()).ifPresent(queries::add);
    ofNullable(longOperations(KEY, filter.processInstanceKeyOperations()))
        .ifPresent(queries::addAll);
    ofNullable(stringOperations(BPMN_PROCESS_ID, filter.processDefinitionIdOperations()))
        .ifPresent(queries::addAll);
    ofNullable(stringOperations(PROCESS_NAME, filter.processDefinitionNameOperations()))
        .ifPresent(queries::addAll);
    ofNullable(intOperations(PROCESS_VERSION, filter.processDefinitionVersionOperations()))
        .ifPresent(queries::addAll);
    ofNullable(
            stringOperations(PROCESS_VERSION_TAG, filter.processDefinitionVersionTagOperations()))
        .ifPresent(queries::addAll);
    ofNullable(longOperations(PROCESS_KEY, filter.processDefinitionKeyOperations()))
        .ifPresent(queries::addAll);
    ofNullable(
            longOperations(
                PARENT_PROCESS_INSTANCE_KEY, filter.parentProcessInstanceKeyOperations()))
        .ifPresent(queries::addAll);
    ofNullable(
            longOperations(
                PARENT_FLOW_NODE_INSTANCE_KEY, filter.parentFlowNodeInstanceKeyOperations()))
        .ifPresent(queries::addAll);
    ofNullable(stringOperations(TREE_PATH, filter.treePathOperations())).ifPresent(queries::addAll);
    ofNullable(dateTimeOperations(START_DATE, filter.startDateOperations()))
        .ifPresent(queries::addAll);
    ofNullable(dateTimeOperations(END_DATE, filter.endDateOperations())).ifPresent(queries::addAll);
    ofNullable(stringOperations(STATE, filter.stateOperations())).ifPresent(queries::addAll);
    ofNullable(getIncidentQuery(filter.hasIncident())).ifPresent(queries::add);
    ofNullable(stringOperations(TENANT_ID, filter.tenantIdOperations())).ifPresent(queries::addAll);
    return and(queries);
  }

  @Override
  public List<String> toIndices(final ProcessInstanceFilter filter) {
    return List.of("operate-list-view-8.3.0_alias");
  }

  private SearchQuery getIsProcessInstanceQuery() {
    return term(JOIN_RELATION, PROCESS_INSTANCE_JOIN_RELATION);
  }

  private SearchQuery getIncidentQuery(final Boolean hasIncident) {
    if (hasIncident != null) {
      return term(INCIDENT, hasIncident);
    }
    return null;
  }
}
