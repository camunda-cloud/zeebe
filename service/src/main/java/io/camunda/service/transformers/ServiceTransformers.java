/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service.transformers;

import io.camunda.search.clients.core.SearchQueryRequest;
import io.camunda.search.clients.query.SearchQuery;
import io.camunda.service.search.filter.AuthorizationFilter;
import io.camunda.service.search.filter.ComparableValueFilter;
import io.camunda.service.search.filter.DateValueFilter;
import io.camunda.service.search.filter.DecisionDefinitionFilter;
import io.camunda.service.search.filter.DecisionInstanceFilter;
import io.camunda.service.search.filter.DecisionRequirementsFilter;
import io.camunda.service.search.filter.FilterBase;
import io.camunda.service.search.filter.FlowNodeInstanceFilter;
import io.camunda.service.search.filter.IncidentFilter;
import io.camunda.service.search.filter.ProcessDefinitionFilter;
import io.camunda.service.search.filter.ProcessInstanceFilter;
import io.camunda.service.search.filter.UserFilter;
import io.camunda.service.search.filter.UserTaskFilter;
import io.camunda.service.search.filter.VariableFilter;
import io.camunda.service.search.filter.VariableValueFilter;
import io.camunda.service.search.query.AuthorizationQuery;
import io.camunda.service.search.query.DecisionDefinitionQuery;
import io.camunda.service.search.query.DecisionInstanceQuery;
import io.camunda.service.search.query.DecisionRequirementsQuery;
import io.camunda.service.search.query.FlowNodeInstanceQuery;
import io.camunda.service.search.query.IncidentQuery;
import io.camunda.service.search.query.ProcessDefinitionQuery;
import io.camunda.service.search.query.ProcessInstanceQuery;
import io.camunda.service.search.query.SearchQueryResult;
import io.camunda.service.search.query.TypedSearchQuery;
import io.camunda.service.search.query.UserQuery;
import io.camunda.service.search.query.UserTaskQuery;
import io.camunda.service.search.query.VariableQuery;
import io.camunda.service.search.result.QueryResultConfig;
import io.camunda.service.search.sort.FlowNodeInstanceSort;
import io.camunda.service.search.sort.SortOption;
import io.camunda.service.security.auth.Authentication;
import io.camunda.service.transformers.filter.AuthenticationTransformer;
import io.camunda.service.transformers.filter.AuthorizationFilterTransformer;
import io.camunda.service.transformers.filter.ComparableValueFilterTransformer;
import io.camunda.service.transformers.filter.DateValueFilterTransformer;
import io.camunda.service.transformers.filter.DecisionDefinitionFilterTransformer;
import io.camunda.service.transformers.filter.DecisionInstanceFilterTransformer;
import io.camunda.service.transformers.filter.DecisionRequirementsFilterTransformer;
import io.camunda.service.transformers.filter.FilterTransformer;
import io.camunda.service.transformers.filter.FlownodeInstanceFilterTransformer;
import io.camunda.service.transformers.filter.IncidentFilterTransformer;
import io.camunda.service.transformers.filter.ProcessDefinitionFilterTransformer;
import io.camunda.service.transformers.filter.ProcessInstanceFilterTransformer;
import io.camunda.service.transformers.filter.UserFilterTransformer;
import io.camunda.service.transformers.filter.UserTaskFilterTransformer;
import io.camunda.service.transformers.filter.VariableFilterTransformer;
import io.camunda.service.transformers.filter.VariableValueFilterTransformer;
import io.camunda.service.transformers.query.SearchQueryResultTransformer;
import io.camunda.service.transformers.query.TypedSearchQueryTransformer;
import io.camunda.service.transformers.result.ResultConfigTransformer;
import io.camunda.service.transformers.sort.FieldSortingTransformer;
import java.util.HashMap;
import java.util.Map;

public final class ServiceTransformers {

  private final Map<Class<?>, ServiceTransformer<?, ?>> transformers = new HashMap<>();

  private ServiceTransformers() {}

  public static ServiceTransformers newInstance() {
    final var serviceTransformers = new ServiceTransformers();
    initializeTransformers(serviceTransformers);
    return serviceTransformers;
  }

  public <F extends FilterBase, S extends SortOption>
      TypedSearchQueryTransformer<F, S> getTypedSearchQueryTransformer(final Class<?> cls) {
    final ServiceTransformer<TypedSearchQuery<F, S>, SearchQueryRequest> transformer =
        getTransformer(cls);
    return (TypedSearchQueryTransformer<F, S>) transformer;
  }

  public <F extends FilterBase> FilterTransformer<F> getFilterTransformer(final Class<?> cls) {
    final ServiceTransformer<F, SearchQuery> transformer = getTransformer(cls);
    return (FilterTransformer<F>) transformer;
  }

  public <T, R> ServiceTransformer<T, R> getTransformer(final Class<?> cls) {
    return (ServiceTransformer<T, R>) transformers.get(cls);
  }

  private void put(final Class<?> cls, final ServiceTransformer<?, ?> mapper) {
    transformers.put(cls, mapper);
  }

  public static void initializeTransformers(final ServiceTransformers mappers) {
    // query -> request
    mappers.put(ProcessInstanceQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(UserTaskQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(VariableQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(DecisionDefinitionQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(DecisionRequirementsQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(DecisionInstanceQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(UserQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(AuthorizationQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(IncidentQuery.class, new TypedSearchQueryTransformer<>(mappers));
    mappers.put(
        FlowNodeInstanceQuery.class,
        new TypedSearchQueryTransformer<FlowNodeInstanceFilter, FlowNodeInstanceSort>(mappers));
    mappers.put(ProcessDefinitionQuery.class, new TypedSearchQueryTransformer<>(mappers));
    // search query response -> search query result
    mappers.put(SearchQueryResult.class, new SearchQueryResultTransformer());

    // sorting -> search sort options
    mappers.put(FieldSortingTransformer.class, new FieldSortingTransformer());

    // filters -> search query
    mappers.put(Authentication.class, new AuthenticationTransformer());
    mappers.put(ProcessInstanceFilter.class, new ProcessInstanceFilterTransformer(mappers));
    mappers.put(UserTaskFilter.class, new UserTaskFilterTransformer(mappers));
    mappers.put(VariableValueFilter.class, new VariableValueFilterTransformer());
    mappers.put(DateValueFilter.class, new DateValueFilterTransformer());
    mappers.put(
        VariableFilter.class,
        new VariableFilterTransformer(mappers, new VariableValueFilterTransformer()));
    mappers.put(DecisionDefinitionFilter.class, new DecisionDefinitionFilterTransformer());
    mappers.put(DecisionRequirementsFilter.class, new DecisionRequirementsFilterTransformer());
    mappers.put(DecisionInstanceFilter.class, new DecisionInstanceFilterTransformer(mappers));
    mappers.put(UserFilter.class, new UserFilterTransformer());
    mappers.put(AuthorizationFilter.class, new AuthorizationFilterTransformer());
    mappers.put(ComparableValueFilter.class, new ComparableValueFilterTransformer());
    mappers.put(FlowNodeInstanceFilter.class, new FlownodeInstanceFilterTransformer());
    mappers.put(IncidentFilter.class, new IncidentFilterTransformer(mappers));
    mappers.put(ProcessDefinitionFilter.class, new ProcessDefinitionFilterTransformer(mappers));

    // result config -> source config
    mappers.put(QueryResultConfig.class, new ResultConfigTransformer());
  }
}
