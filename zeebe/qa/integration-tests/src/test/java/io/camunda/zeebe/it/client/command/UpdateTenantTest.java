/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.client.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.zeebe.it.util.ZeebeAssertHelper;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.test.util.junit.AutoCloseResources;
import io.camunda.zeebe.test.util.junit.AutoCloseResources.AutoCloseResource;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ZeebeIntegration
@AutoCloseResources
class UpdateTenantTest {

  private static final String UPDATED_TENANT_NAME = "Updated Tenant Name";
  private static final String TENANT_ID = "tenant-id";

  @TestZeebe
  private final TestStandaloneBroker zeebe = new TestStandaloneBroker().withRecordingExporter(true);

  @AutoCloseResource private CamundaClient client;

  private long tenantKey;

  @BeforeEach
  void initClientAndInstances() {
    client = zeebe.newClientBuilder().defaultRequestTimeout(Duration.ofSeconds(15)).build();
    tenantKey =
        client
            .newCreateTenantCommand()
            .tenantId(TENANT_ID)
            .name("Initial Tenant Name")
            .send()
            .join()
            .getTenantKey();
  }

  @Test
  void shouldUpdateTenantName() {
    // when
    client.newUpdateTenantCommand(tenantKey).name(UPDATED_TENANT_NAME).send().join();

    // then
    ZeebeAssertHelper.assertTenantUpdated(
        TENANT_ID, tenant -> assertThat(tenant.getName()).isEqualTo(UPDATED_TENANT_NAME));
  }

  @Test
  void shouldRejectUpdateIfNameIsNull() {
    // when / then
    assertThatThrownBy(() -> client.newUpdateTenantCommand(tenantKey).name(null).send().join())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name must not be null");
  }

  @Test
  void shouldRejectUpdateIfTenantDoesNotExist() {
    // when / then
    final long notExistingTenantKey = 67677634L;
    assertThatThrownBy(
            () ->
                client
                    .newUpdateTenantCommand(67677634L)
                    .name("Non-Existent Tenant Name")
                    .send()
                    .join())
        .isInstanceOf(ProblemException.class)
        .hasMessageContaining("Failed with code 404: 'Not Found'")
        .hasMessageContaining(
            "Expected to update tenant with key '%d', but no tenant with this key exists."
                .formatted(notExistingTenantKey));
  }
}
