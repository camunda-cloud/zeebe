/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.webapp.security.identity;

import static io.camunda.tasklist.webapp.security.TasklistProfileService.IDENTITY_AUTH_PROFILE;
import static io.camunda.tasklist.webapp.security.TasklistURIs.IDENTITY_CALLBACK_URI;

import io.camunda.identity.sdk.Identity;
import io.camunda.identity.sdk.authentication.Tokens;
import io.camunda.identity.sdk.authentication.dto.AuthCodeDto;
import io.camunda.identity.sdk.exception.IdentityException;
import io.camunda.tasklist.property.TasklistProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.UUID;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Profile(IDENTITY_AUTH_PROFILE)
public class IdentityService {

  private static final int DELAY_IN_MILLISECONDS = 500;
  private static final int MAX_ATTEMPTS = 10;

  @Autowired private Identity identity;

  @Autowired private TasklistProperties tasklistProperties;

  public String getRedirectUrl(final HttpServletRequest req) {
    return identity
        .authentication()
        .authorizeUriBuilder(getRedirectURI(req, IDENTITY_CALLBACK_URI))
        .build()
        .toString();
  }

  public void logout() {
    final IdentityAuthentication authentication =
        (IdentityAuthentication) SecurityContextHolder.getContext().getAuthentication();
    identity.authentication().revokeToken(authentication.getTokens().getRefreshToken());
  }

  public static <T> T requestWithRetry(final CheckedSupplier<T> operation) {
    final RetryPolicy<T> retryPolicy =
        new RetryPolicy<T>()
            .handle(IdentityException.class)
            .withDelay(Duration.ofMillis(DELAY_IN_MILLISECONDS))
            .withMaxAttempts(MAX_ATTEMPTS);
    return Failsafe.with(retryPolicy).get(operation);
  }

  public String getRedirectURI(final HttpServletRequest req, final String redirectTo) {
    final String fixedRedirectRootUrl = tasklistProperties.getIdentity().getRedirectRootUrl();

    String redirectRootUri;
    if (StringUtils.isNotBlank(fixedRedirectRootUrl)) {
      redirectRootUri = fixedRedirectRootUrl;
    } else {
      redirectRootUri = req.getScheme() + "://" + req.getServerName();
      if ((req.getScheme().equals("http") && req.getServerPort() != 80)
          || (req.getScheme().equals("https") && req.getServerPort() != 443)) {
        redirectRootUri += ":" + req.getServerPort();
      }
    }

    final String result;
    if (contextPathIsUUID(req.getContextPath())) {
      final String clusterId = req.getContextPath().replace("/", "");
      result = redirectRootUri + /* req.getContextPath()+ */ redirectTo + "?uuid=" + clusterId;
    } else {
      result = redirectRootUri + req.getContextPath() + redirectTo;
    }
    return result;
  }

  private boolean contextPathIsUUID(String contextPath) {
    try {
      UUID.fromString(contextPath.replace("/", ""));
      return true;
    } catch (Exception e) {
      // Assume it isn't a UUID
      return false;
    }
  }

  public IdentityAuthentication getAuthenticationFor(
      final HttpServletRequest req, final AuthCodeDto authCodeDto) {
    final Tokens tokens =
        requestWithRetry(
            () ->
                identity
                    .authentication()
                    .exchangeAuthCode(authCodeDto, getRedirectURI(req, IDENTITY_CALLBACK_URI)));
    final IdentityAuthentication authentication = new IdentityAuthentication();
    authentication.authenticate(tokens);
    return authentication;
  }
}
