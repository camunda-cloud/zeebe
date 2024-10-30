/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.interceptors.impl;

import io.camunda.identity.sdk.Identity;
import io.camunda.identity.sdk.IdentityConfiguration;
import io.camunda.identity.sdk.authentication.exception.TokenVerificationException;
import io.camunda.identity.sdk.tenants.dto.Tenant;
import io.camunda.zeebe.gateway.impl.configuration.GatewayCfg;
import io.camunda.zeebe.gateway.impl.configuration.IdentityCfg;
import io.camunda.zeebe.gateway.impl.configuration.IdentityRequestCfg;
import io.camunda.zeebe.gateway.impl.configuration.MultiTenancyCfg;
import io.camunda.zeebe.gateway.impl.identity.IdentityTenantService;
import io.camunda.zeebe.gateway.interceptors.InterceptorUtil;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IdentityInterceptor implements ServerInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdentityInterceptor.class);
  private static final Metadata.Key<String> AUTH_KEY =
      Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
  private final Identity identity;
  private final IdentityTenantService tenantService;
  private final MultiTenancyCfg multiTenancy;

  public IdentityInterceptor(final IdentityCfg config, final GatewayCfg gatewayCfg) {
    this(
        createIdentity(config),
        gatewayCfg.getMultiTenancy(),
        gatewayCfg.getExperimental().getIdentityRequest());
  }

  public IdentityInterceptor(
      final IdentityConfiguration configuration, final GatewayCfg gatewayCfg) {
    this(
        new Identity(configuration),
        gatewayCfg.getMultiTenancy(),
        gatewayCfg.getExperimental().getIdentityRequest());
  }

  public IdentityInterceptor(
      final Identity identity,
      final MultiTenancyCfg multiTenancy,
      final IdentityRequestCfg identityRequestCfg) {
    this.identity = identity;
    this.multiTenancy = multiTenancy;
    tenantService = new IdentityTenantService(identity, identityRequestCfg);
  }

  private static Identity createIdentity(final IdentityCfg config) {
    return new Identity(
        new IdentityConfiguration.Builder()
            .withIssuerBackendUrl(config.getIssuerBackendUrl())
            .withAudience(config.getAudience())
            .withType(config.getType().name())
            .withBaseUrl(config.getBaseUrl())
            .build());
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      final ServerCall<ReqT, RespT> call,
      final Metadata headers,
      final ServerCallHandler<ReqT, RespT> next) {
    final var methodDescriptor = call.getMethodDescriptor();

    final var authorization = headers.get(AUTH_KEY);
    if (authorization == null) {
      LOGGER.debug(
          "Denying call {} as no token was provided", methodDescriptor.getFullMethodName());
      return deny(
          call,
          Status.UNAUTHENTICATED.augmentDescription(
              "Expected bearer token at header with key [%s], but found nothing"
                  .formatted(AUTH_KEY.name())));
    }

    final String token = authorization.replaceFirst("^Bearer ", "");
    try {
      identity.authentication().verifyToken(token);
    } catch (final TokenVerificationException e) {
      LOGGER.debug(
          "Denying call {} as the token could not be verified successfully. Error message: {}",
          methodDescriptor.getFullMethodName(),
          e.getMessage(),
          e);

      return deny(
          call,
          Status.UNAUTHENTICATED
              .augmentDescription("Failed to parse bearer token, see cause for details")
              .withCause(e));
    }

    if (!multiTenancy.isEnabled()) {
      return next.startCall(call, headers);
    }

    try {
      final List<String> authorizedTenants =
          tenantService.getTenantsForToken(token).stream().map(Tenant::getTenantId).toList();
      final var context = InterceptorUtil.setAuthorizedTenants(authorizedTenants);
      return Contexts.interceptCall(context, call, headers, next);

    } catch (final RejectedExecutionException ree) {
      return denyTenantCallAndLog(call, Status.UNAVAILABLE, ree);
    } catch (final RuntimeException | ExecutionException e) {
      return denyTenantCallAndLog(call, Status.UNAUTHENTICATED, e);
    }
  }

  private <ReqT> ServerCall.Listener<ReqT> denyTenantCallAndLog(
      final ServerCall<ReqT, ?> call, final Status status, final Throwable ex) {
    LOGGER.debug(
        "Denying call {} as the authorized tenants could not be retrieved from Identity. Error message: {}",
        call.getMethodDescriptor().getFullMethodName(),
        ex.getMessage());

    return deny(
        call,
        status
            .augmentDescription(
                "Expected Identity to provide authorized tenants, see cause for details")
            .withCause(ex));
  }

  private <ReqT> ServerCall.Listener<ReqT> deny(
      final ServerCall<ReqT, ?> call, final Status status) {
    call.close(status, new Metadata());
    return new ServerCall.Listener<>() {};
  }
}
