/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.optimize.exception.OptimizeIntegrationTestException;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.camunda.optimize.service.util.configuration.DefaultTenant;
import org.camunda.optimize.service.util.configuration.EngineAuthenticationConfiguration;
import org.camunda.optimize.service.util.configuration.EngineConfiguration;
import org.camunda.optimize.test.it.rule.ElasticSearchIntegrationTestRule;
import org.camunda.optimize.test.it.rule.EmbeddedOptimizeRule;
import org.camunda.optimize.test.it.rule.EngineIntegrationRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;

import java.util.HashMap;
import java.util.Map;

import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_DECISION_DEFINITION;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_PROCESS_DEFINITION;
import static org.camunda.optimize.test.it.rule.EmbeddedOptimizeRule.DEFAULT_ENGINE_ALIAS;
import static org.camunda.optimize.test.util.DmnHelper.createSimpleDmnModel;

public class AbstractMultiEngineIT {
  private static final String REST_ENDPOINT = "http://localhost:8080/engine-rest";
  private static final String SECURE_REST_ENDPOINT = "http://localhost:8080/engine-rest-secure";
  public static final String PROCESS_KEY_1 = "TestProcess1";
  public static final String PROCESS_KEY_2 = "TestProcess2";
  public static final String DECISION_KEY_1 = "TestDecision1";
  public static final String DECISION_KEY_2 = "TestDecision2";
  protected final String SECOND_ENGINE_ALIAS = "secondTestEngine";
  public ElasticSearchIntegrationTestRule elasticSearchRule = new ElasticSearchIntegrationTestRule();
  public EmbeddedOptimizeRule embeddedOptimizeRule = new EmbeddedOptimizeRule();

  protected EngineIntegrationRule defaultEngineRule = new EngineIntegrationRule();
  protected EngineIntegrationRule secondEngineRule = new EngineIntegrationRule(
    "multiple-engine/multiple-engine-integration-rules.properties"
  );
  private ConfigurationService configurationService;

  @Rule
  public RuleChain chain = RuleChain
    .outerRule(elasticSearchRule).around(defaultEngineRule).around(embeddedOptimizeRule);

  @Before
  public void init() {
    configurationService = embeddedOptimizeRule.getConfigurationService();
  }

  @After
  public void reset() {
    configurationService.getConfiguredEngines().remove(SECOND_ENGINE_ALIAS);
    embeddedOptimizeRule.reloadConfiguration();
  }

  protected void finishAllUserTasksForAllEngines() {
    defaultEngineRule.finishAllRunningUserTasks();
    secondEngineRule.finishAllRunningUserTasks();
  }

  protected void deployStartAndImportDefinitionForAllEngines(final int definitionResourceType, final String tenantId) {
    switch (definitionResourceType) {
      case RESOURCE_TYPE_PROCESS_DEFINITION:
        deployAndStartSimpleProcessDefinitionForAllEngines(tenantId);
        break;
      case RESOURCE_TYPE_DECISION_DEFINITION:
        deployAndStartDecisionDefinitionForAllEngines(tenantId);
        break;
      default:
        throw new OptimizeIntegrationTestException("Unsupported resourceType: " + definitionResourceType);
    }

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();
  }

  protected void deployAndStartDecisionDefinitionForAllEngines() {
    deployAndStartDecisionDefinitionForAllEngines(null);
  }

  protected void deployAndStartDecisionDefinitionForAllEngines(final String tenantId) {
    defaultEngineRule.deployAndStartDecisionDefinition(createSimpleDmnModel(DECISION_KEY_1), tenantId);
    secondEngineRule.deployAndStartDecisionDefinition(createSimpleDmnModel(DECISION_KEY_2), tenantId);
  }

  protected void deployAndStartSimpleProcessDefinitionForAllEngines() {
    deployAndStartSimpleProcessDefinitionForAllEngines(null);
  }

  protected void deployAndStartSimpleProcessDefinitionForAllEngines(final String tenantId) {
    deployAndStartSimpleProcessDefinitionForAllEngines(PROCESS_KEY_1, PROCESS_KEY_2, tenantId);
  }

  protected void deployAndStartSimpleProcessDefinitionForAllEngines(final String key1,
                                                                    final String key2,
                                                                    final String tenantId) {
    deployAndStartProcessOnDefaultEngine(key1, tenantId);
    deployAndStartProcessOnSecondEngine(key2, tenantId);
  }

  protected void deployAndStartProcessOnSecondEngine(final String key2, final String tenantId) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aStringVariable", "foo");
    secondEngineRule.deployAndStartProcessWithVariables(
      Bpmn.createExecutableProcess(key2)
        .startEvent()
        .endEvent()
        .done(),
      variables,
      tenantId
    );
  }

  protected void deployAndStartProcessOnDefaultEngine(final String key1, final String tenantId) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aStringVariable", "foo");
    defaultEngineRule.deployAndStartProcessWithVariables(
      Bpmn.createExecutableProcess(key1)
        .startEvent()
        .endEvent()
        .done(),
      variables,
      tenantId
    );
  }

  protected void deployAndStartUserTaskProcessForAllEngines() {
    defaultEngineRule.deployAndStartProcess(
      Bpmn.createExecutableProcess(PROCESS_KEY_1)
        .startEvent()
        .userTask()
        .endEvent()
        .done()
    );
    secondEngineRule.deployAndStartProcess(
      Bpmn.createExecutableProcess(PROCESS_KEY_2)
        .startEvent()
        .userTask()
        .endEvent()
        .done()
    );
  }

  protected void addSecondEngineToConfiguration() {
    String anotherEngine = "anotherEngine";
    addEngineToConfiguration(anotherEngine);
    embeddedOptimizeRule.reloadConfiguration();
  }

  protected void addSecureEngineToConfiguration() {
    addEngineToConfiguration("anotherEngine", SECURE_REST_ENDPOINT, true, "admin", "admin");
  }

  protected void addEngineToConfiguration(String engineName) {
    addEngineToConfiguration(engineName, REST_ENDPOINT, false, "", "");
  }

  protected void addNonExistingSecondEngineToConfiguration() {
    addEngineToConfiguration("notExistingEngine", "http://localhost:9999/engine-rest", false, "", "");
    embeddedOptimizeRule.reloadConfiguration();
  }

  protected void addEngineToConfiguration(String engineName, String restEndpoint, boolean withAuthentication,
                                          String username, String password) {
    EngineAuthenticationConfiguration engineAuthenticationConfiguration = constructEngineAuthenticationConfiguration(
      withAuthentication,
      username,
      password
    );

    EngineConfiguration anotherEngineConfig = new EngineConfiguration();
    anotherEngineConfig.setName(engineName);
    anotherEngineConfig.setRest(restEndpoint);
    anotherEngineConfig.setAuthentication(engineAuthenticationConfiguration);
    configurationService
      .getConfiguredEngines()
      .put(SECOND_ENGINE_ALIAS, anotherEngineConfig);
  }

  protected EngineAuthenticationConfiguration constructEngineAuthenticationConfiguration(boolean withAuthentication,
                                                                                         String username,
                                                                                         String password) {
    EngineAuthenticationConfiguration engineAuthenticationConfiguration = new EngineAuthenticationConfiguration();
    engineAuthenticationConfiguration.setEnabled(withAuthentication);
    engineAuthenticationConfiguration.setPassword(password);
    engineAuthenticationConfiguration.setUser(username);
    return engineAuthenticationConfiguration;
  }

  protected void setDefaultEngineDefaultTenant(final DefaultTenant defaultTenant) {
    embeddedOptimizeRule.getConfigurationService()
      .getConfiguredEngines()
      .get(DEFAULT_ENGINE_ALIAS)
      .setDefaultTenant(defaultTenant);
  }

  protected void setSecondEngineDefaultTenant(final DefaultTenant defaultTenant) {
    embeddedOptimizeRule.getConfigurationService()
      .getConfiguredEngines()
      .get(SECOND_ENGINE_ALIAS)
      .setDefaultTenant(defaultTenant);
  }
}
