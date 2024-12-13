/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import io.camunda.search.clients.TenantSearchClient;
import io.camunda.search.entities.TenantEntity;
import io.camunda.search.page.SearchQueryPage;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.search.query.TenantQuery;
import io.camunda.security.auth.Authentication;
import io.camunda.security.auth.Authorization;
import io.camunda.service.exception.ForbiddenException;
import io.camunda.service.search.core.SearchQueryService;
import io.camunda.service.security.SecurityContextProvider;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.gateway.impl.broker.request.BrokerTenantEntityRequest;
import io.camunda.zeebe.gateway.impl.broker.request.tenant.BrokerTenantCreateRequest;
import io.camunda.zeebe.gateway.impl.broker.request.tenant.BrokerTenantDeleteRequest;
import io.camunda.zeebe.gateway.impl.broker.request.tenant.BrokerTenantUpdateRequest;
import io.camunda.zeebe.protocol.impl.record.value.tenant.TenantRecord;
import io.camunda.zeebe.protocol.record.value.EntityType;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class TenantServices extends SearchQueryService<TenantServices, TenantQuery, TenantEntity> {

  private final TenantSearchClient tenantSearchClient;

  public TenantServices(
      final BrokerClient brokerClient,
      final SecurityContextProvider securityContextProvider,
      final TenantSearchClient tenantSearchClient,
      final Authentication authentication) {
    super(brokerClient, securityContextProvider, authentication);
    this.tenantSearchClient = tenantSearchClient;
  }

  @Override
  public SearchQueryResult<TenantEntity> search(final TenantQuery query) {
    return tenantSearchClient
        .withSecurityContext(
            securityContextProvider.provideSecurityContext(
                authentication, Authorization.of(a -> a.tenant().read())))
        .searchTenants(query);
  }

  @Override
  public TenantServices withAuthentication(final Authentication authentication) {
    return new TenantServices(
        brokerClient, securityContextProvider, tenantSearchClient, authentication);
  }

  public CompletableFuture<TenantRecord> createTenant(final TenantDTO request) {
    return sendBrokerRequest(
        new BrokerTenantCreateRequest().setTenantId(request.tenantId()).setName(request.name()));
  }

  public CompletableFuture<TenantRecord> updateTenant(final TenantDTO request) {
    return sendBrokerRequest(new BrokerTenantUpdateRequest(request.key()).setName(request.name()));
  }

  public CompletableFuture<TenantRecord> deleteTenant(final long key) {
    return sendBrokerRequest(new BrokerTenantDeleteRequest(key));
  }

  public TenantEntity getByKey(final Long tenantKey) {
    final var tenantQuery = TenantQuery.of(q -> q.filter(f -> f.key(tenantKey)));
    final var result =
        tenantSearchClient
            .withSecurityContext(securityContextProvider.provideSecurityContext(authentication))
            .searchTenants(tenantQuery);
    final var tenantEntity = getSingleResultOrThrow(result, tenantKey, "Tenant");
    final var authorization = Authorization.of(a -> a.tenant().read());
    if (!securityContextProvider.isAuthorized(
        tenantEntity.tenantId(), authentication, authorization)) {
      throw new ForbiddenException(authorization);
    }
    return tenantEntity;
  }

  public CompletableFuture<?> addMember(
      final Long tenantKey, final EntityType entityType, final long entityKey) {
    return sendBrokerRequest(
        BrokerTenantEntityRequest.createAddRequest()
            .setTenantKey(tenantKey)
            .setEntity(entityType, entityKey));
  }

  public CompletableFuture<?> removeMember(
      final Long tenantKey, final EntityType entityType, final long entityKey) {
    return sendBrokerRequest(
        BrokerTenantEntityRequest.createRemoveRequest()
            .setTenantKey(tenantKey)
            .setEntity(entityType, entityKey));
  }

  public Collection<TenantEntity> getTenantsByMemberKey(final long memberKey) {
    return search(
            TenantQuery.of(
                queryBuilder ->
                    queryBuilder
                        .filter(filterBuilder -> filterBuilder.memberKey(memberKey))
                        // FIXME: we don't have an easy way to fetch all results, so we use a large
                        //        limit – 10k tenants ought to be enough for anybody …
                        .page(SearchQueryPage.of(b -> b.size(10_000)))))
        .items();
  }

  public record TenantDTO(Long key, String tenantId, String name) {}
}
