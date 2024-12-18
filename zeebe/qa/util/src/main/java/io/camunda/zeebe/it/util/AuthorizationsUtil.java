/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.util;

import static io.camunda.security.configuration.InitializationConfiguration.DEFAULT_USER_PASSWORD;
import static io.camunda.security.configuration.InitializationConfiguration.DEFAULT_USER_USERNAME;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CredentialsProvider;
import io.camunda.client.ZeebeClient;
import io.camunda.client.protocol.rest.PermissionTypeEnum;
import io.camunda.client.protocol.rest.ResourceTypeEnum;
import io.camunda.webapps.schema.descriptors.usermanagement.index.UserIndex;
import io.camunda.zeebe.qa.util.cluster.TestGateway;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.awaitility.Awaitility;

public class AuthorizationsUtil {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  private final TestGateway<?> gateway;
  private final ZeebeClient client;
  private final String elasticsearchUrl;

  public AuthorizationsUtil(
      final TestGateway<?> gateway, final ZeebeClient client, final String elasticsearchUrl) {
    this.gateway = gateway;
    this.client = client;
    this.elasticsearchUrl = elasticsearchUrl;
  }

  public static AuthorizationsUtil create(
      final TestGateway<?> gateway, final String elasticsearchUrl) {
    final var authorizationUtil =
        new AuthorizationsUtil(
            gateway,
            createClient(gateway, DEFAULT_USER_USERNAME, DEFAULT_USER_PASSWORD),
            elasticsearchUrl);
    authorizationUtil.awaitUserExistsInElasticsearch(DEFAULT_USER_USERNAME);
    return authorizationUtil;
  }

  public long createUser(final String username, final String password) {
    return createUserWithPermissions(username, password);
  }

  public long createUserWithPermissions(
      final String username, final String password, final Permissions... permissions) {
    final var userCreateResponse =
        client
            .newUserCreateCommand()
            .username(username)
            .password(password)
            .name("name")
            .email("foo@bar.com")
            .send()
            .join();

    for (final Permissions permission : permissions) {
      client
          .newAddPermissionsCommand(userCreateResponse.getUserKey())
          .resourceType(permission.resourceType())
          .permission(permission.permissionType())
          .resourceIds(permission.resourceIds())
          .send()
          .join();
    }

    awaitUserExistsInElasticsearch(username);
    return userCreateResponse.getUserKey();
  }

  public ZeebeClient createClient(final String username, final String password) {
    return createClient(gateway, username, password);
  }

  public ZeebeClient createUserAndClient(
      final String username, final String password, final Permissions... permissions) {
    createUserWithPermissions(username, password, permissions);
    return createClient(gateway, username, password);
  }

  public static ZeebeClient createClient(
      final TestGateway<?> gateway, final String username, final String password) {
    return gateway
        .newClientBuilder()
        .preferRestOverGrpc(true)
        .defaultRequestTimeout(Duration.ofSeconds(15))
        .credentialsProvider(
            new CredentialsProvider() {
              @Override
              public void applyCredentials(final CredentialsApplier applier) {
                applier.put(
                    "Authorization",
                    "Basic %s"
                        .formatted(
                            Base64.getEncoder()
                                .encodeToString("%s:%s".formatted(username, password).getBytes())));
              }

              @Override
              public boolean shouldRetryRequest(final StatusCode statusCode) {
                return false;
              }
            })
        .build();
  }

  public void awaitUserExistsInElasticsearch(final String username) {
    final HttpRequest request;
    try {
      request =
          HttpRequest.newBuilder()
              .POST(
                  BodyPublishers.ofString(
                      """
                  {
                    "query": {
                      "match": {
                        "username": "%s"
                      }
                    }
                  }"""
                          .formatted(username)))
              .uri(
                  new URI(
                      "http://%s/%s/_count/"
                          .formatted(
                              elasticsearchUrl, new UserIndex("", true).getFullQualifiedName())))
              .header("Content-Type", "application/json")
              .build();
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }

    Awaitility.await()
        .atMost(Duration.ofSeconds(15))
        .until(
            () -> {
              final var response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
              final var userExistsResponse =
                  OBJECT_MAPPER.readValue(response.body(), UserExistsResponse.class);
              return userExistsResponse.count > 0;
            });
  }

  public ZeebeClient getDefaultClient() {
    return client;
  }

  public record Permissions(
      ResourceTypeEnum resourceType, PermissionTypeEnum permissionType, List<String> resourceIds) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record UserExistsResponse(int count) {}
}
