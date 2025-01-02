/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.authentication.config;

import static org.springframework.security.config.Customizer.withDefaults;

import io.camunda.authentication.CamundaUserDetailsService;
import io.camunda.authentication.filters.OrganizationAuthorizationFilter;
import io.camunda.authentication.filters.TenantRequestAttributeFilter;
import io.camunda.authentication.handler.AuthFailureHandler;
import io.camunda.authentication.handler.CustomMethodSecurityExpressionHandler;
import io.camunda.security.configuration.MultiTenancyConfiguration;
import io.camunda.security.configuration.SecurityConfiguration;
import io.camunda.service.AuthorizationServices;
import io.camunda.service.RoleServices;
import io.camunda.service.TenantServices;
import io.camunda.service.UserServices;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("auth-basic|auth-oidc")
public class WebSecurityConfig {
  public static final String SESSION_COOKIE = "camunda-session";
  public static final String LOGIN_URL = "/login";
  public static final String LOGOUT_URL = "/logout";

  public static final String[] UNAUTHENTICATED_PATHS =
      new String[] {
        "/login",
        "/logout",
        // endpoint for failure forwarding
        "/error",
        // all actuator endpoints
        "/actuator/**",
        // endpoints defined in BrokerHealthRoutes
        "/ready",
        "/health",
        "/startup",
        // deprecated Tasklist v1 Public Endpoints
        "/v1/external/process/**",
        "/new/**",
        // static assets are public
        "/identity/assets/**",
        "/tasklist/assets/**",
        "/tasklist/client-config.js",
        "/operate/static/**",
        "/operate/client-config.js",
      };

  private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

  @Bean
  @ConditionalOnMissingBean(MethodSecurityExpressionHandler.class)
  public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
      final AuthorizationServices authorizationServices) {
    return new CustomMethodSecurityExpressionHandler(authorizationServices);
  }

  @Bean
  @Profile("auth-basic")
  public CamundaUserDetailsService camundaUserDetailsService(
      final UserServices userServices,
      final AuthorizationServices authorizationServices,
      final RoleServices roleServices,
      final TenantServices tenantServices) {
    return new CamundaUserDetailsService(
        userServices, authorizationServices, roleServices, tenantServices);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity) {
    try {
      return httpSecurity.build();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Bean
  @Primary
  @Profile("auth-oidc")
  public HttpSecurity oidcHttpSecurity(
      final HttpSecurity httpSecurity,
      final AuthFailureHandler authFailureHandler,
      final ClientRegistrationRepository clientRegistrationRepository,
      final SecurityConfiguration configuration)
      throws Exception {
    final var security =
        baseHttpSecurity(httpSecurity, authFailureHandler)
            .oauth2ResourceServer(
                oauth2 ->
                    oauth2.jwt(
                        jwtConfigurer ->
                            jwtConfigurer.jwkSetUri(
                                clientRegistrationRepository
                                    .findByRegistrationId("oidcclient")
                                    .getProviderDetails()
                                    .getJwkSetUri())))
            .oauth2Login(oauthLoginConfigurer -> {})
            .logout(
                (logout) ->
                    logout
                        .logoutUrl(LOGOUT_URL)
                        .logoutSuccessHandler(this::genericSuccessHandler)
                        .deleteCookies(SESSION_COOKIE));
    return withOrganizationIdFilter(security, configuration);
  }

  private HttpSecurity withOrganizationIdFilter(
      final HttpSecurity httpSecurity, final SecurityConfiguration configuration) {
    final var organizationId = configuration.getOrganizationId();
    if (organizationId == null) {
      return httpSecurity;
    }
    LOG.info("Requiring organization id {}", organizationId);
    return httpSecurity.addFilterAfter(
        new OrganizationAuthorizationFilter(organizationId),
        SecurityContextHolderAwareRequestFilter.class);
  }

  private void genericSuccessHandler(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final Authentication authentication) {
    response.setStatus(HttpStatus.NO_CONTENT.value());
  }

  @Bean
  @Primary
  @Profile("auth-basic")
  public HttpSecurity localHttpSecurity(
      final HttpSecurity httpSecurity, final AuthFailureHandler authFailureHandler)
      throws Exception {
    LOG.info("Configuring basic auth login");
    return baseHttpSecurity(httpSecurity, authFailureHandler)
        .httpBasic(withDefaults())
        .logout((logout) -> logout.logoutSuccessUrl("/"));
  }

  private HttpSecurity baseHttpSecurity(
      final HttpSecurity httpSecurity, final AuthFailureHandler authFailureHandler) {
    try {
      return httpSecurity
          .authorizeHttpRequests(
              (authorizeHttpRequests) ->
                  authorizeHttpRequests
                      .requestMatchers(UNAUTHENTICATED_PATHS)
                      .permitAll()
                      .anyRequest()
                      .authenticated())
          .headers(
              (headers) ->
                  headers.httpStrictTransportSecurity(
                      (httpStrictTransportSecurity) ->
                          httpStrictTransportSecurity
                              .includeSubDomains(true)
                              .maxAgeInSeconds(63072000)
                              .preload(true)))
          .exceptionHandling(
              (exceptionHandling) -> exceptionHandling.accessDeniedHandler(authFailureHandler))
          .csrf(AbstractHttpConfigurer::disable)
          .cors(AbstractHttpConfigurer::disable)
          .formLogin(AbstractHttpConfigurer::disable)
          .anonymous(AbstractHttpConfigurer::disable);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Bean
  public FilterRegistrationBean<TenantRequestAttributeFilter>
      tenantRequestAttributeFilterRegistration(final MultiTenancyConfiguration configuration) {
    return new FilterRegistrationBean<>(new TenantRequestAttributeFilter(configuration));
  }
}
