/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.service;

import io.camunda.search.clients.CamundaSearchClient;
import io.camunda.service.AuthorizationServices;
import io.camunda.service.CamundaServices;
import io.camunda.service.ClockServices;
import io.camunda.service.DecisionDefinitionServices;
import io.camunda.service.DecisionRequirementsServices;
import io.camunda.service.DocumentServices;
import io.camunda.service.ElementInstanceServices;
import io.camunda.service.FlowNodeInstanceServices;
import io.camunda.service.IncidentServices;
import io.camunda.service.JobServices;
import io.camunda.service.MessageServices;
import io.camunda.service.ProcessInstanceServices;
import io.camunda.service.ResourceServices;
import io.camunda.service.SignalServices;
import io.camunda.service.UserServices;
import io.camunda.service.UserTaskServices;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.gateway.impl.job.ActivateJobsHandler;
import io.camunda.zeebe.gateway.protocol.rest.JobActivationResponse;
import io.camunda.zeebe.gateway.rest.ConditionalOnRestGatewayEnabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnRestGatewayEnabled
public class CamundaServicesConfiguration {

  private final BrokerClient brokerClient;
  private final CamundaSearchClient camundaSearchClient;

  @Autowired
  public CamundaServicesConfiguration(
      final BrokerClient brokerClient, final CamundaSearchClient camundaSearchClient) {
    this.brokerClient = brokerClient;
    this.camundaSearchClient = camundaSearchClient;
  }

  @Bean
  public CamundaServices camundaServices() {
    return new CamundaServices(brokerClient, camundaSearchClient);
  }

  @Bean
  public ProcessInstanceServices processInstanceServices(final CamundaServices camundaServices) {
    return camundaServices.processInstanceServices();
  }

  @Bean
  public UserTaskServices userTaskServices(final CamundaServices camundaServices) {
    return camundaServices.userTaskServices();
  }

  @Bean
  public JobServices<JobActivationResponse> jobServices(
      final CamundaServices camundaServices,
      final ActivateJobsHandler<JobActivationResponse> activateJobsHandler) {
    return camundaServices.jobServices(activateJobsHandler);
  }

  @Bean
  public DecisionDefinitionServices decisionDefinitionServices(
      final CamundaServices camundaServices) {
    return camundaServices.decisionDefinitionServices();
  }

  @Bean
  public DecisionRequirementsServices decisionRequirementsServices(
      final CamundaServices camundaServices) {
    return camundaServices.decisionRequirementsServices();
  }

  @Bean
  public IncidentServices incidentServices(final CamundaServices camundaServices) {
    return camundaServices.incidentServices();
  }

  @Bean
  public FlowNodeInstanceServices flownodeInstanceServices(final CamundaServices camundaServices) {
    return camundaServices.flownodeInstanceServices();
  }

  @Bean
  public UserServices userServices(final CamundaServices camundaServices) {
    return camundaServices.userServices();
  }

  @Bean
  public MessageServices messageServices(final CamundaServices camundaServices) {
    return camundaServices.messageServices();
  }

  @Bean
  public DocumentServices documentServices(final CamundaServices camundaServices) {
    return camundaServices.documentServices();
  }

  @Bean
  public AuthorizationServices authorizationServices(final CamundaServices camundaServices) {
    return camundaServices.authorizationServices();
  }

  @Bean
  public ClockServices clockServices(final CamundaServices camundaServices) {
    return camundaServices.clockServices();
  }

  @Bean
  public ResourceServices resourceServices(final CamundaServices camundaServices) {
    return camundaServices.resourceService();
  }

  @Bean
  public ElementInstanceServices elementServices(final CamundaServices camundaServices) {
    return camundaServices.elementServices();
  }

  @Bean
  public SignalServices signalServices(final CamundaServices camundaServices) {
    return camundaServices.signalServices();
  }
}
