/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service.search.core;

import io.camunda.search.clients.CamundaSearchClient;
import io.camunda.service.ApiServices;
import io.camunda.service.search.query.SearchQueryBase;
import io.camunda.service.search.query.SearchQueryResult;
import io.camunda.service.security.auth.Authentication;
import io.camunda.service.transformers.ServiceTransformers;
import io.camunda.zeebe.broker.client.api.BrokerClient;

public abstract class SearchQueryService<T extends ApiServices<T>, Q extends SearchQueryBase, D>
    extends ApiServices<T> {

  protected final SearchClientBasedQueryExecutor executor;

  protected SearchQueryService(
      final BrokerClient brokerClient,
      final CamundaSearchClient searchClient,
      final ServiceTransformers transformers,
      final Authentication authentication,
      final boolean withResourceAuthorization) {
    super(brokerClient, searchClient, transformers, authentication);
    executor = initiateExecutor(withResourceAuthorization);
  }

  private SearchClientBasedQueryExecutor initiateExecutor(final boolean withResourceAuthorization) {
    var executor = new SearchClientBasedQueryExecutor(searchClient, transformers, authentication);
    if (withResourceAuthorization) {
      executor = executor.withResourceAuthorization();
    }
    return executor;
  }

  public abstract SearchQueryResult<D> search(final Q query);
}
