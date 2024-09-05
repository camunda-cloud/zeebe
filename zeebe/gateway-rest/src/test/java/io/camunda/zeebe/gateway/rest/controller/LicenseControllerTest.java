/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.service.ManagementServices;
import io.camunda.service.license.LicenseType;
import io.camunda.zeebe.gateway.rest.RestControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

@WebMvcTest(value = LicenseController.class, properties = "camunda.rest.query.enabled=true")
public class LicenseControllerTest extends RestControllerTest {

  static final String LICENSE_URL = "/v2/license";

  static final String EXPECTED_LICENSE_RESPONSE =
      """
      {
          "validLicense": true,
          "licenseType": "saas"
      }""";

  @MockBean ManagementServices managementServices;

  @Test
  void shouldReturnProperSaaSResponse() {
    // given
    when(managementServices.isCamundaLicenseValid()).thenReturn(true);
    when(managementServices.getCamundaLicenseType()).thenReturn(LicenseType.SAAS);

    // when / then
    webClient
        .get()
        .uri(LICENSE_URL)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json(EXPECTED_LICENSE_RESPONSE);

    verify(managementServices).isCamundaLicenseValid();
    verify(managementServices).getCamundaLicenseType();
  }
}
