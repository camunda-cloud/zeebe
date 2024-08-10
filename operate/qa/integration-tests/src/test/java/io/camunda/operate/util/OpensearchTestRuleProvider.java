/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.util;

import static io.camunda.operate.store.opensearch.dsl.RequestDSL.getIndexRequestBuilder;
import static io.camunda.operate.util.ThreadUtil.sleepFor;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.operate.conditions.OpensearchCondition;
import io.camunda.operate.entities.BatchOperationEntity;
import io.camunda.operate.entities.IncidentEntity;
import io.camunda.operate.entities.OperateEntity;
import io.camunda.operate.entities.OperationEntity;
import io.camunda.operate.entities.ProcessEntity;
import io.camunda.operate.entities.VariableEntity;
import io.camunda.operate.entities.dmn.DecisionInstanceEntity;
import io.camunda.operate.entities.dmn.definition.DecisionDefinitionEntity;
import io.camunda.operate.entities.dmn.definition.DecisionRequirementsEntity;
import io.camunda.operate.entities.listview.FlowNodeInstanceForListViewEntity;
import io.camunda.operate.entities.listview.ProcessInstanceForListViewEntity;
import io.camunda.operate.entities.listview.VariableForListViewEntity;
import io.camunda.operate.exceptions.PersistenceException;
import io.camunda.operate.property.OperateOpensearchProperties;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.schema.SchemaManager;
import io.camunda.operate.schema.indices.DecisionIndex;
import io.camunda.operate.schema.indices.DecisionRequirementsIndex;
import io.camunda.operate.schema.indices.ProcessIndex;
import io.camunda.operate.schema.templates.BatchOperationTemplate;
import io.camunda.operate.schema.templates.DecisionInstanceTemplate;
import io.camunda.operate.schema.templates.IncidentTemplate;
import io.camunda.operate.schema.templates.ListViewTemplate;
import io.camunda.operate.schema.templates.OperationTemplate;
import io.camunda.operate.schema.templates.VariableTemplate;
import io.camunda.operate.store.opensearch.client.sync.RichOpenSearchClient;
import io.camunda.operate.zeebe.ImportValueType;
import io.camunda.operate.zeebeimport.RecordsReader;
import io.camunda.operate.zeebeimport.RecordsReaderHolder;
import io.camunda.operate.zeebeimport.ZeebeImporter;
import io.camunda.operate.zeebeimport.ZeebePostImporter;
import io.camunda.operate.zeebeimport.post.PostImportAction;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.runner.Description;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional(OpensearchCondition.class)
@Component
public class OpensearchTestRuleProvider implements SearchTestRuleProvider {

  protected static final Logger LOGGER = LoggerFactory.getLogger(OpensearchTestRuleProvider.class);

  @Autowired protected RichOpenSearchClient richOpenSearchClient;

  @Autowired
  @Qualifier("zeebeOpensearchClient")
  protected OpenSearchClient zeebeOsClient;

  @Autowired protected OperateProperties operateProperties;
  @Autowired protected ZeebeImporter zeebeImporter;
  @Autowired protected ZeebePostImporter zeebePostImporter;
  @Autowired protected RecordsReaderHolder recordsReaderHolder;
  protected boolean failed = false;
  Map<Class<? extends OperateEntity>, String> entityToAliasMap;
  @Autowired private ListViewTemplate listViewTemplate;
  @Autowired private VariableTemplate variableTemplate;
  @Autowired private ProcessIndex processIndex;
  @Autowired private OperationTemplate operationTemplate;
  @Autowired private BatchOperationTemplate batchOperationTemplate;
  @Autowired private IncidentTemplate incidentTemplate;
  @Autowired private DecisionInstanceTemplate decisionInstanceTemplate;
  @Autowired private DecisionRequirementsIndex decisionRequirementsIndex;
  @Autowired private DecisionIndex decisionIndex;
  @Autowired private SchemaManager schemaManager;
  @Autowired private TestImportListener testImportListener;
  private String indexPrefix;

  @Override
  public void failed(Throwable e, Description description) {
    this.failed = true;
  }

  @Override
  public void starting(Description description) {
    if (indexPrefix == null) {
      indexPrefix = TestUtil.createRandomString(10) + "-operate";
    }
    operateProperties.getOpensearch().setIndexPrefix(indexPrefix);
    if (operateProperties.getOpensearch().isCreateSchema()) {
      schemaManager.createSchema();
      assertThat(areIndicesCreatedAfterChecks(indexPrefix, 5, 5 * 60 /*sec*/))
          .describedAs("Opensearch %s (min %d) indices are created", indexPrefix, 5)
          .isTrue();
    }
  }

  @Override
  public void finished(Description description) {
    TestUtil.removeIlmPolicy(richOpenSearchClient);
    final String indexPrefix = operateProperties.getOpensearch().getIndexPrefix();
    TestUtil.removeAllIndices(
        richOpenSearchClient.index(), richOpenSearchClient.template(), indexPrefix);
    operateProperties
        .getOpensearch()
        .setIndexPrefix(OperateOpensearchProperties.DEFAULT_INDEX_PREFIX);
    zeebePostImporter.getPostImportActions().stream().forEach(PostImportAction::clearCache);
    assertMaxOpenScrollContexts(15);
  }

  public void assertMaxOpenScrollContexts(final int maxOpenScrollContexts) {
    assertThat(getOpenScrollcontextSize())
        .describedAs("There are too many open scroll contexts left.")
        .isLessThanOrEqualTo(maxOpenScrollContexts);
  }

  @Override
  public void refreshSearchIndices() {
    refreshZeebeIndices();
    refreshOperateSearchIndices();
  }

  @Override
  public void refreshZeebeIndices() {
    try {
      zeebeOsClient
          .indices()
          .refresh(r -> r.index(operateProperties.getZeebeOpensearch().getPrefix() + "*"));
    } catch (Exception t) {
      LOGGER.error("Could not refresh Zeebe Opensearch indices", t);
    }
  }

  @Override
  public void refreshOperateSearchIndices() {
    try {
      richOpenSearchClient
          .index()
          .refresh(operateProperties.getOpensearch().getIndexPrefix() + "*");
      Thread.sleep(3000); // TODO: Find a way to wait for refresh completion
    } catch (Exception t) {
      LOGGER.error("Could not refresh Operate Opensearch indices", t);
    }
  }

  public void processAllRecordsAndWait(
      Integer maxWaitingRounds, Predicate<Object[]> predicate, Object... arguments) {
    processRecordsAndWaitFor(
        recordsReaderHolder.getAllRecordsReaders(),
        maxWaitingRounds,
        true,
        predicate,
        null,
        arguments);
  }

  public void processAllRecordsAndWait(Predicate<Object[]> predicate, Object... arguments) {
    processAllRecordsAndWait(50, predicate, arguments);
  }

  public void processAllRecordsAndWait(
      Predicate<Object[]> predicate, Supplier<Object> supplier, Object... arguments) {
    processRecordsAndWaitFor(
        recordsReaderHolder.getAllRecordsReaders(), 50, true, predicate, supplier, arguments);
  }

  public void processAllRecordsAndWait(
      boolean runPostImport,
      Predicate<Object[]> predicate,
      Supplier<Object> supplier,
      Object... arguments) {
    processRecordsAndWaitFor(
        recordsReaderHolder.getAllRecordsReaders(),
        50,
        runPostImport,
        predicate,
        supplier,
        arguments);
  }

  public void processRecordsWithTypeAndWait(
      ImportValueType importValueType, Predicate<Object[]> predicate, Object... arguments) {
    processRecordsAndWaitFor(
        getRecordsReaders(importValueType), 50, true, predicate, null, arguments);
  }

  public void processRecordsWithTypeAndWait(
      ImportValueType importValueType,
      boolean runPostImport,
      Predicate<Object[]> predicate,
      Object... arguments) {
    processRecordsAndWaitFor(
        getRecordsReaders(importValueType), 50, runPostImport, predicate, null, arguments);
  }

  public void processRecordsAndWaitFor(
      Collection<RecordsReader> readers,
      Integer maxWaitingRounds,
      boolean runPostImport,
      Predicate<Object[]> predicate,
      Supplier<Object> supplier,
      Object... arguments) {
    int waitingRound = 0;
    final int maxRounds = maxWaitingRounds;
    boolean found = predicate.test(arguments);
    final long start = System.currentTimeMillis();
    while (!found && waitingRound < maxRounds) {
      testImportListener.resetCounters();
      try {
        if (supplier != null) {
          supplier.get();
        }
        refreshSearchIndices();
        zeebeImporter.performOneRoundOfImportFor(readers);
        refreshOperateSearchIndices();
        if (runPostImport) {
          runPostImportActions();
        }

      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
      int waitForImports = 0;
      // Wait for imports max 30 sec (60 * 500 ms)
      while (testImportListener.getImportedCount() < testImportListener.getScheduledCount()
          && waitForImports < 60) {
        waitForImports++;
        try {
          sleepFor(2000);
          zeebeImporter.performOneRoundOfImportFor(readers);
          refreshOperateSearchIndices();
          if (runPostImport) {
            runPostImportActions();
          }

        } catch (Exception e) {
          waitingRound = 0;
          testImportListener.resetCounters();
          LOGGER.error(e.getMessage(), e);
        }
        LOGGER.debug(
            " {} of {} imports processed",
            testImportListener.getImportedCount(),
            testImportListener.getScheduledCount());
      }
      refreshOperateSearchIndices();
      found = predicate.test(arguments);
      if (!found) {
        sleepFor(2000);
        waitingRound++;
      }
    }
    final long finishedTime = System.currentTimeMillis() - start;

    if (found) {
      LOGGER.debug("Conditions met in round {} ({} ms).", waitingRound, finishedTime);
    } else {
      LOGGER.debug("Conditions not met after {} rounds ({} ms).", waitingRound, finishedTime);
      //      throw new TestPrerequisitesFailedException("Conditions not met.");
    }
  }

  public void runPostImportActions() {
    if (zeebePostImporter.getPostImportActions().size() == 0) {
      zeebePostImporter.initPostImporters();
    }
    for (PostImportAction action : zeebePostImporter.getPostImportActions()) {
      try {
        action.performOneRound();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public boolean areIndicesCreatedAfterChecks(
      String indexPrefix, int minCountOfIndices, int maxChecks) {
    boolean areCreated = false;
    int checks = 0;
    while (!areCreated && checks <= maxChecks) {
      checks++;
      try {
        areCreated = areIndicesCreated(indexPrefix, minCountOfIndices);
      } catch (Exception t) {
        LOGGER.error(
            "Opensearch indices (min {}) are not created yet. Waiting {}/{}",
            minCountOfIndices,
            checks,
            maxChecks);
        sleepFor(200);
      }
    }
    LOGGER.debug("Opensearch indices are created after {} checks", checks);
    return areCreated;
  }

  public List<RecordsReader> getRecordsReaders(ImportValueType importValueType) {
    return recordsReaderHolder.getAllRecordsReaders().stream()
        .filter(rr -> rr.getImportValueType().equals(importValueType))
        .collect(Collectors.toList());
  }

  public void persistNew(OperateEntity... entitiesToPersist) {
    try {
      persistOperateEntitiesNew(Arrays.asList(entitiesToPersist));
    } catch (PersistenceException e) {
      LOGGER.error("Unable to persist entities: " + e.getMessage(), e);
      throw new RuntimeException(e);
    }
    refreshSearchIndices();
  }

  public void persistOperateEntitiesNew(List<? extends OperateEntity> operateEntities)
      throws PersistenceException {
    final var batchRequest = richOpenSearchClient.batch().newBatchRequest();

    for (final OperateEntity entity : operateEntities) {
      final String alias = getEntityToAliasMap().get(entity.getClass());
      if (alias == null) {
        throw new RuntimeException("Index not configured for " + entity.getClass().getName());
      }
      if (entity instanceof FlowNodeInstanceForListViewEntity flowNodeInstanceForListViewEntity) {
        batchRequest.addWithRouting(
            alias, entity, flowNodeInstanceForListViewEntity.getProcessInstanceKey().toString());
      } else if (entity instanceof VariableForListViewEntity variableForListViewEntity) {
        batchRequest.addWithRouting(
            alias, entity, variableForListViewEntity.getProcessInstanceKey().toString());
      } else {
        batchRequest.add(alias, entity);
      }
    }
    batchRequest.executeWithRefresh();
  }

  public Map<Class<? extends OperateEntity>, String> getEntityToAliasMap() {
    if (entityToAliasMap == null) {
      entityToAliasMap = new HashMap<>();
      entityToAliasMap.put(ProcessEntity.class, processIndex.getFullQualifiedName());
      entityToAliasMap.put(IncidentEntity.class, incidentTemplate.getFullQualifiedName());
      entityToAliasMap.put(
          ProcessInstanceForListViewEntity.class, listViewTemplate.getFullQualifiedName());
      entityToAliasMap.put(
          FlowNodeInstanceForListViewEntity.class, listViewTemplate.getFullQualifiedName());
      entityToAliasMap.put(
          VariableForListViewEntity.class, listViewTemplate.getFullQualifiedName());
      entityToAliasMap.put(VariableEntity.class, variableTemplate.getFullQualifiedName());
      entityToAliasMap.put(OperationEntity.class, operationTemplate.getFullQualifiedName());
      entityToAliasMap.put(
          BatchOperationEntity.class, batchOperationTemplate.getFullQualifiedName());
      entityToAliasMap.put(
          DecisionInstanceEntity.class, decisionInstanceTemplate.getFullQualifiedName());
      entityToAliasMap.put(
          DecisionRequirementsEntity.class, decisionRequirementsIndex.getFullQualifiedName());
      entityToAliasMap.put(DecisionDefinitionEntity.class, decisionIndex.getFullQualifiedName());
    }
    return entityToAliasMap;
  }

  public int getOpenScrollcontextSize() {
    try {
      return richOpenSearchClient.cluster().totalOpenContexts();
    } catch (Exception e) {
      LOGGER.error("Failed to retrieve open contexts from opensearch! Returning 0.", e);
      return 0;
    }
  }

  public void setIndexPrefix(String indexPrefix) {
    this.indexPrefix = indexPrefix;
  }

  @Override
  public boolean indexExists(String index) {
    return richOpenSearchClient.index().indexExists(index);
  }

  private boolean areIndicesCreated(String indexPrefix, int minCountOfIndices) throws IOException {
    final var indexRequestBuilder =
        getIndexRequestBuilder(indexPrefix + "*")
            .ignoreUnavailable(true)
            .allowNoIndices(false)
            .expandWildcards(ExpandWildcard.Open);

    final GetIndexResponse response = richOpenSearchClient.index().get(indexRequestBuilder);

    final var result = response.result();
    return result.size() >= minCountOfIndices;
  }
}
