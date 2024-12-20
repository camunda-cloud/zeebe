/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize;

import static io.camunda.optimize.service.util.configuration.EnvironmentPropertiesConstants.CONTEXT_PATH;

import io.camunda.optimize.rest.HealthRestService;
import io.camunda.optimize.rest.LocalizationRestService;
import io.camunda.optimize.rest.UIConfigurationRestService;
import io.camunda.optimize.rest.security.cloud.CCSaasAuth0WebSecurityConfig;
import io.camunda.optimize.service.util.PanelNotificationConstants;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.tomcat.OptimizeResourceConstants;
import io.camunda.optimize.tomcat.ResponseSecurityHeaderFilter;
import io.camunda.optimize.tomcat.ResponseTimezoneFilter;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OptimizeTomcatConfig {

  public static final String EXTERNAL_SUB_PATH = "/external";
  private static final Logger LOG = LoggerFactory.getLogger(OptimizeTomcatConfig.class);

  private static final String[] COMPRESSED_MIME_TYPES = {
    "application/json", "text/html", "application/x-font-ttf", "image/svg+xml"
  };

  private static final String LOGIN_ENDPOINT = "/login";
  private static final String METRICS_ENDPOINT = "/metrics";
  private static final String URL_BASE = "/#";

  public static final String ALLOWED_URL_EXTENSION =
      String.join(
          "|",
          new String[] {
            URL_BASE,
            LOGIN_ENDPOINT,
            METRICS_ENDPOINT,
            CCSaasAuth0WebSecurityConfig.OAUTH_AUTH_ENDPOINT,
            CCSaasAuth0WebSecurityConfig.OAUTH_REDIRECT_ENDPOINT,
            CCSaasAuth0WebSecurityConfig.AUTH0_JWKS_ENDPOINT,
            CCSaasAuth0WebSecurityConfig.AUTH0_AUTH_ENDPOINT,
            CCSaasAuth0WebSecurityConfig.AUTH0_TOKEN_ENDPOINT,
            CCSaasAuth0WebSecurityConfig.AUTH0_USERINFO_ENDPOINT,
            HealthRestService.READYZ_PATH,
            LocalizationRestService.LOCALIZATION_PATH,
            OptimizeTomcatConfig.EXTERNAL_SUB_PATH,
            OptimizeResourceConstants.REST_API_PATH,
            OptimizeResourceConstants.STATIC_RESOURCE_PATH,
            OptimizeResourceConstants.ACTUATOR_ENDPOINT,
            PanelNotificationConstants.SEND_NOTIFICATION_TO_ALL_ORG_USERS_ENDPOINT,
            UIConfigurationRestService.UI_CONFIGURATION_PATH,
            "/favicon.ico",
            "/index.html"
          });

  private static final String HTTP11_NIO_PROTOCOL = "org.apache.coyote.http11.Http11Nio2Protocol";

  @Autowired private ConfigurationService configurationService;
  @Autowired private Environment environment;

  @Bean
  FilterRegistrationBean<ResponseSecurityHeaderFilter> responseHeadersInjector() {
    LOG.debug("Registering filter 'responseHeadersInjector'...");
    final ResponseSecurityHeaderFilter responseSecurityHeaderFilter =
        new ResponseSecurityHeaderFilter(configurationService);
    final FilterRegistrationBean<ResponseSecurityHeaderFilter> registrationBean =
        new FilterRegistrationBean<>();
    registrationBean.addUrlPatterns("/*");
    registrationBean.setFilter(responseSecurityHeaderFilter);
    return registrationBean;
  }

  @Bean
  FilterRegistrationBean<ResponseTimezoneFilter> responseTimezoneFilter() {
    LOG.debug("Registering filter 'responseTimezoneFilter'...");
    final ResponseTimezoneFilter filter = new ResponseTimezoneFilter();
    final FilterRegistrationBean<ResponseTimezoneFilter> registrationBean =
        new FilterRegistrationBean<>();
    registrationBean.addUrlPatterns("/*");
    registrationBean.setFilter(filter);
    return registrationBean;
  }

  public Optional<String> getContextPath() {
    // If the property is set by env var (the case when starting a new Optimize in ITs), this takes
    // precedence over config
    final Optional<String> contextPath = Optional.ofNullable(environment.getProperty(CONTEXT_PATH));
    if (contextPath.isEmpty()) {
      return configurationService.getContextPath();
    }
    return contextPath;
  }
}
