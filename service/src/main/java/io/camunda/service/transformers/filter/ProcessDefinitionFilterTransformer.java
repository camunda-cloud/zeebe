/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service.transformers.filter;

import static io.camunda.search.clients.query.SearchQueryBuilders.and;
import static io.camunda.search.clients.query.SearchQueryBuilders.intTerms;
import static io.camunda.search.clients.query.SearchQueryBuilders.longTerms;
import static io.camunda.search.clients.query.SearchQueryBuilders.stringTerms;

import io.camunda.search.clients.query.SearchQuery;
import io.camunda.service.search.filter.ProcessDefinitionFilter;
import io.camunda.service.transformers.ServiceTransformers;
import java.util.List;

public class ProcessDefinitionFilterTransformer
    implements FilterTransformer<ProcessDefinitionFilter> {

  private final ServiceTransformers transformers;

  public ProcessDefinitionFilterTransformer(final ServiceTransformers transformers) {
    this.transformers = transformers;
  }

  @Override
  public SearchQuery toSearchQuery(final ProcessDefinitionFilter filter) {
    return and(
        longTerms("key", filter.processDefinitionKeys()),
        stringTerms("name", filter.names()),
        stringTerms("bpmnProcessId", filter.bpmnProcessIds()),
        stringTerms("resourceName", filter.resourceNames()),
        intTerms("version", filter.versions()),
        stringTerms("versionTag", filter.versionTags()),
        stringTerms("tenantId", filter.tenantIds()));
  }

  @Override
  public List<String> toIndices(final ProcessDefinitionFilter filter) {
    return List.of("operate-process-8.3.0_alias");
  }
}
