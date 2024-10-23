/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.descriptors;

public abstract class AbstractIndexDescriptor implements IndexDescriptor {

  public static final String SCHEMA_FOLDER_OPENSEARCH = "/schema/opensearch/create";
  public static final String SCHEMA_FOLDER_ELASTICSEARCH = "/schema/elasticsearch/create";
  public static final String SCHEMA_CREATE_INDEX_JSON_OPENSEARCH =
      SCHEMA_FOLDER_OPENSEARCH + "/index/%s-%s.json";
  public static final String SCHEMA_CREATE_INDEX_JSON_ELASTICSEARCH =
      SCHEMA_FOLDER_ELASTICSEARCH + "/index/%s-%s.json";

  protected String indexPrefix;
  protected boolean isElasticsearch;

  public AbstractIndexDescriptor(final String indexPrefix, final boolean isElasticsearch) {
    this.indexPrefix = indexPrefix;
    this.isElasticsearch = isElasticsearch;
  }

  @Override
  public String getFullQualifiedName() {
    return String.format("%s-%s-%s_", getPrefixAndComponentName(), getIndexName(), getVersion());
  }

  @Override
  public String getAlias() {
    return getFullQualifiedName() + "alias";
  }

  @Override
  public String getMappingsClasspathFilename() {
    return isElasticsearch
        ? String.format(SCHEMA_CREATE_INDEX_JSON_ELASTICSEARCH, getComponentName(), getIndexName())
        : String.format(SCHEMA_CREATE_INDEX_JSON_OPENSEARCH, getComponentName(), getIndexName());
  }

  @Override
  public String getAllVersionsIndexNameRegexPattern() {
    return String.format("%s-%s-\\d.*", getPrefixAndComponentName(), getIndexName());
  }

  @Override
  public String getVersion() {
    return "1.0.0";
  }

  public String getIndexPrefix() {
    return indexPrefix;
  }

  private String getPrefixAndComponentName() {
    // Cannot start index with "-" so must not append "-" for empty prefix
    String prefix = "";
    if (getIndexPrefix() != null && !getIndexPrefix().isBlank()) {
      prefix = getIndexPrefix() + "-";
    }
    // Legacy descriptors have the same index prefix as component name this avoids duplication.
    return prefix.contains(getComponentName()) ? prefix : prefix + getComponentName();
  }

  public abstract String getComponentName();
}
