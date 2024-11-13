/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.os;

import static io.camunda.tasklist.schema.v86.indices.AbstractIndexDescriptor.formatAllVersionsIndexNameRegexPattern;
import static io.camunda.tasklist.schema.v86.indices.AbstractIndexDescriptor.formatTasklistIndexPattern;
import static io.camunda.tasklist.util.CollectionUtil.map;

import io.camunda.tasklist.data.conditionals.OpenSearchCondition;
import io.camunda.tasklist.exceptions.TasklistRuntimeException;
import io.camunda.tasklist.property.TasklistProperties;
import io.camunda.tasklist.schema.v86.IndexMapping.IndexMappingProperty;
import io.camunda.tasklist.schema.v86.IndexSchemaValidator;
import io.camunda.tasklist.schema.v86.manager.SchemaManager;
import io.camunda.tasklist.util.IndexSchemaValidatorUtil;
import io.camunda.webapps.schema.descriptors.IndexDescriptor;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(OpenSearchCondition.class)
public class IndexSchemaValidatorOpenSearch extends IndexSchemaValidatorUtil
    implements IndexSchemaValidator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(IndexSchemaValidatorOpenSearch.class);

  @Autowired Set<IndexDescriptor> indexDescriptors;

  @Autowired TasklistProperties tasklistProperties;

  @Autowired RetryOpenSearchClient retryOpenSearchClient;

  @Autowired SchemaManager schemaManager;

  private Set<String> getAllIndexNamesForIndex(final String index) {
    final String indexPattern = formatTasklistIndexPattern(getIndexPrefix());
    LOGGER.debug("Getting all indices for {}", indexPattern);
    final Set<String> indexNames = retryOpenSearchClient.getIndexNames(indexPattern);
    // since we have indices with similar names, we need to additionally filter index names
    // e.g. task and task-variable
    final String patternWithVersion =
        formatAllVersionsIndexNameRegexPattern(getIndexPrefix(), index);
    return indexNames.stream()
        .filter(n -> n.matches(patternWithVersion))
        .collect(Collectors.toSet());
  }

  @Override
  public boolean isHealthCheckEnabled() {
    return tasklistProperties.getOpenSearch().isHealthCheckEnabled();
  }

  @Override
  public boolean hasAnyTasklistIndices() {
    final Set<String> indices =
        retryOpenSearchClient.getIndexNames(
            tasklistProperties.getOpenSearch().getIndexPrefix() + "*");
    return !indices.isEmpty();
  }

  @Override
  public boolean schemaExists() {
    try {
      final Set<String> indices =
          retryOpenSearchClient.getIndexNames(
              tasklistProperties.getOpenSearch().getIndexPrefix() + "*");
      final List<String> allIndexNames =
          map(indexDescriptors, IndexDescriptor::getFullQualifiedName);
      return indices.containsAll(allIndexNames) && validateNumberOfReplicas(allIndexNames);
    } catch (final Exception e) {
      LOGGER.error("Check for existing schema failed", e);
      return false;
    }
  }

  @Override
  public void validateIndexVersions() {
    if (!hasAnyTasklistIndices()) {
      return;
    }
    final Set<String> errors = new HashSet<>();
    indexDescriptors.forEach(
        indexDescriptor -> {
          final Set<String> oldVersions =
              olderVersionsForIndex(indexDescriptor, versionsForIndex(indexDescriptor));
          final Set<String> newerVersions =
              newerVersionsForIndex(indexDescriptor, versionsForIndex(indexDescriptor));
          if (oldVersions.size() > 1) {
            errors.add(
                String.format(
                    "More than one older version for %s (%s) found: %s",
                    indexDescriptor.getIndexName(), indexDescriptor.getVersion(), oldVersions));
          }
          if (!newerVersions.isEmpty()) {
            errors.add(
                String.format(
                    "Newer version(s) for %s (%s) already exists: %s",
                    indexDescriptor.getIndexName(), indexDescriptor.getVersion(), newerVersions));
          }
        });
    if (!errors.isEmpty()) {
      throw new TasklistRuntimeException("Error(s) in index schema: " + String.join(";", errors));
    }
  }

  /**
   * Validates existing indices mappings against schema files defined in codebase.
   *
   * @return newFields map with the new field definitions per index
   * @throws TasklistRuntimeException in case some fields would need to be deleted or have different
   *     settings
   */
  @Override
  public Map<IndexDescriptor, Set<IndexMappingProperty>> validateIndexMappings()
      throws IOException {
    return validateIndexMappings(indexDescriptors);
  }

  @Override
  public Set<String> olderVersionsForIndex(final IndexDescriptor indexDescriptor) {
    final Set<String> versions = getAllIndexNamesForIndex(indexDescriptor.getIndexName());
    return olderVersionsForIndex(indexDescriptor, versions);
  }

  @Override
  public Set<String> newerVersionsForIndex(final IndexDescriptor indexDescriptor) {
    final Set<String> versions = getAllIndexNamesForIndex(indexDescriptor.getIndexName());
    return newerVersionsForIndex(indexDescriptor, versions);
  }

  private Set<String> versionsForIndex(final IndexDescriptor indexDescriptor) {
    final Set<String> allIndexNames = getAllIndexNamesForIndex(indexDescriptor.getIndexName());
    return allIndexNames.stream()
        .map(this::getVersionFromIndexName)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
  }

  public boolean validateNumberOfReplicas(final List<String> indexes) {
    for (final String index : indexes) {
      final IndexSettings response =
          retryOpenSearchClient.getIndexSettingsFor(
              index, RetryOpenSearchClient.NUMBERS_OF_REPLICA);
      if (!response
          .numberOfReplicas()
          .equals(String.valueOf(tasklistProperties.getOpenSearch().getNumberOfReplicas()))) {
        return false;
      }
    }
    return true;
  }
}
