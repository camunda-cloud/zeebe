/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import io.camunda.search.clients.AuthorizationSearchClient;
import io.camunda.search.entities.AuthorizationEntity;
import io.camunda.search.query.AuthorizationQuery;
import io.camunda.search.query.SearchQueryBuilders;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.security.auth.Authentication;
import io.camunda.security.auth.Authorization;
import io.camunda.service.search.core.SearchQueryService;
import io.camunda.service.security.SecurityContextProvider;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.gateway.impl.broker.request.BrokerAuthorizationCreateRequest;
import io.camunda.zeebe.gateway.impl.broker.request.BrokerAuthorizationDeleteRequest;
import io.camunda.zeebe.protocol.impl.record.value.authorization.AuthorizationRecord;
import io.camunda.zeebe.protocol.record.value.AuthorizationOwnerType;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AuthorizationServices
    extends SearchQueryService<AuthorizationServices, AuthorizationQuery, AuthorizationEntity> {

  private final AuthorizationSearchClient authorizationSearchClient;

  public AuthorizationServices(
      final BrokerClient brokerClient,
      final SecurityContextProvider securityContextProvider,
      final AuthorizationSearchClient authorizationSearchClient,
      final Authentication authentication) {
    super(brokerClient, securityContextProvider, authentication);
    this.authorizationSearchClient = authorizationSearchClient;
  }

  @Override
  public AuthorizationServices withAuthentication(final Authentication authentication) {
    return new AuthorizationServices(
        brokerClient, securityContextProvider, authorizationSearchClient, authentication);
  }

  @Override
  public SearchQueryResult<AuthorizationEntity> search(final AuthorizationQuery query) {
    return authorizationSearchClient
        .withSecurityContext(
            securityContextProvider.provideSecurityContext(
                authentication, Authorization.of(a -> a.authorization().read())))
        .searchAuthorizations(query);
  }

  public List<AuthorizationEntity> findAll(final AuthorizationQuery query) {
    return authorizationSearchClient
        .withSecurityContext(
            securityContextProvider.provideSecurityContext(
                authentication, Authorization.of(a -> a.authorization().read())))
        .findAllAuthorizations(query);
  }

  public List<String> getAuthorizedResources(
      final Set<Long> ownerKeys,
      final PermissionType permissionType,
      final AuthorizationResourceType resourceType) {
    final var authorizationQuery =
        SearchQueryBuilders.authorizationSearchQuery(
            fn ->
                fn.filter(
                    f ->
                        f.ownerKeys(ownerKeys.stream().toList())
                            .permissionType(permissionType)
                            .resourceType(resourceType.name())));
    return findAll(authorizationQuery).stream()
        .map(AuthorizationEntity::resourceId)
        .collect(Collectors.toList());
  }

  public List<String> getAuthorizedApplications(final Set<Long> ownerKeys) {
    return getAuthorizedResources(
        ownerKeys, PermissionType.READ, AuthorizationResourceType.APPLICATION);
  }

  public Set<String> fetchAssignedPermissions(
      final Long ownerKey, final AuthorizationResourceType resourceType, final String resourceId) {
    final SearchQueryResult<AuthorizationEntity> result =
        search(
            SearchQueryBuilders.authorizationSearchQuery(
                fn ->
                    fn.filter(
                            f ->
                                f.resourceType(resourceType.name())
                                    .resourceIds(
                                        resourceId != null && !resourceId.isEmpty()
                                            ? resourceId
                                            : null)
                                    .ownerKeys(ownerKey))
                        .page(p -> p.size(1))));
    // TODO logic to fetch indirect authorizations via roles/groups should be added later
    return result.items().stream()
        .flatMap(a -> a.permissions().stream())
        .map(PermissionType::name)
        .collect(Collectors.toSet());
  }

  public CompletableFuture<AuthorizationRecord> createAuthorization(
      final CreateAuthorizationRequest request) {
    final var brokerRequest =
        new BrokerAuthorizationCreateRequest()
            .setOwnerId(request.ownerId())
            .setOwnerType(request.ownerType())
            .setResourceId(request.resourceId())
            .setResourceType(request.resourceType())
            .setPermissions(request.permissions());
    return sendBrokerRequest(brokerRequest);
  }

  public CompletableFuture<AuthorizationRecord> deleteAuthorization(final long authorizationKey) {
    final var brokerRequest = new BrokerAuthorizationDeleteRequest(authorizationKey);
    return sendBrokerRequest(brokerRequest);
  }

  public record CreateAuthorizationRequest(
      String ownerId,
      AuthorizationOwnerType ownerType,
      String resourceId,
      AuthorizationResourceType resourceType,
      Set<PermissionType> permissions) {}
}
