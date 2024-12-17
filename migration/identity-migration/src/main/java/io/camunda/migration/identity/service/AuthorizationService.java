/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migration.identity.service;

import io.camunda.migration.identity.dto.Role.Permission;
import io.camunda.security.auth.Authentication;
import io.camunda.service.AuthorizationServices;
import io.camunda.service.AuthorizationServices.PatchAuthorizationRequest;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionAction;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationService {
  private final AuthorizationServices authorizationService;

  public AuthorizationService(
      final AuthorizationServices authorizationService,
      final Authentication.Builder servicesAuthenticationBuilder) {
    this.authorizationService =
        authorizationService.withAuthentication(servicesAuthenticationBuilder.build());
  }

  public void patch(final long ownerKey, final List<Permission> oldPermissions) {

    final var newPermissions =
        oldPermissions.stream()
            .flatMap(this::transformToAuthorizations)
            .collect(
                Collectors.groupingBy(
                    ResourceTypePermissionTypeResourceId::resourceType,
                    Collectors.groupingBy(
                        ResourceTypePermissionTypeResourceId::permissionType,
                        Collectors.mapping(
                            ResourceTypePermissionTypeResourceId::resourceId,
                            Collectors.toSet()))));

    final var futures =
        newPermissions.entrySet().stream()
            .map(
                entry ->
                    authorizationService.patchAuthorization(
                        new PatchAuthorizationRequest(
                            ownerKey, PermissionAction.ADD, entry.getKey(), entry.getValue())))
            .toList();
    futures.forEach(CompletableFuture::join);
  }

  private Stream<ResourceTypePermissionTypeResourceId> transformToAuthorizations(
      final Permission permission) {
    if (permission.apiName().toLowerCase().contains("operate")) {
      if (permission.definition().toLowerCase().contains("read")) {
        return Stream.of(
            new ResourceTypePermissionTypeResourceId(
                AuthorizationResourceType.APPLICATION, PermissionType.ACCESS, "operate"),
            new ResourceTypePermissionTypeResourceId(
                AuthorizationResourceType.PROCESS_DEFINITION, PermissionType.READ, "*"),
            new ResourceTypePermissionTypeResourceId(
                AuthorizationResourceType.DECISION_DEFINITION, PermissionType.READ, "*"),
            new ResourceTypePermissionTypeResourceId(
                AuthorizationResourceType.DECISION_REQUIREMENTS_DEFINITION,
                PermissionType.READ,
                "*"));
      } else if (permission.definition().toLowerCase().contains("write")) {
        return Stream.concat(
            Stream.concat(
                Stream.concat(
                    Stream.of(
                        new ResourceTypePermissionTypeResourceId(
                            AuthorizationResourceType.APPLICATION,
                            PermissionType.ACCESS,
                            "operate")),
                    Arrays.stream(PermissionType.values())
                        .map(
                            pt ->
                                new ResourceTypePermissionTypeResourceId(
                                    AuthorizationResourceType.PROCESS_DEFINITION, pt, "*"))),
                Arrays.stream(PermissionType.values())
                    .map(
                        pt ->
                            new ResourceTypePermissionTypeResourceId(
                                AuthorizationResourceType.DECISION_DEFINITION, pt, "*"))),
            Arrays.stream(PermissionType.values())
                .map(
                    pt ->
                        new ResourceTypePermissionTypeResourceId(
                            AuthorizationResourceType.DECISION_REQUIREMENTS_DEFINITION, pt, "*")));
      }
    }
    return new ArrayList<ResourceTypePermissionTypeResourceId>().stream();
  }

  private record ResourceTypePermissionTypeResourceId(
      AuthorizationResourceType resourceType, PermissionType permissionType, String resourceId) {}
}
