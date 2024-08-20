/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

package io.camunda.application.commons.service;

import io.camunda.application.commons.service.ManagementServicesConfiguration.LicenseKeyProperties;
import io.camunda.service.ManagementServices;
import io.camunda.service.license.CamundaLicense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({LicenseKeyProperties.class})
public class ManagementServicesConfiguration {
  private final LicenseKeyProperties licenseKeyProperties;

  @Autowired
  public ManagementServicesConfiguration(final LicenseKeyProperties licenseKeyProperties) {
    this.licenseKeyProperties = licenseKeyProperties;
  }

  @Bean
  public ManagementServices managementServices() {
    return new ManagementServices(camundaLicense());
  }

  @Bean
  public CamundaLicense camundaLicense() {
    // return new CamundaLicense(licenseKeyProperties.key());
    return new CamundaLicense() {
      @Override
      public synchronized boolean isValid() {
        return true;
      }

      @Override
      public synchronized String getLicenseType() {
        return "self-managed";
      }
    };
  }

  @ConfigurationProperties("camunda.license")
  public record LicenseKeyProperties(String key) {}
}
