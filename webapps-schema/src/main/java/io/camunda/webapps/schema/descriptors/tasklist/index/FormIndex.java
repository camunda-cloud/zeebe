/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.descriptors.tasklist.index;

import io.camunda.webapps.schema.descriptors.backup.Prio4Backup;
import io.camunda.webapps.schema.descriptors.tasklist.TasklistIndexDescriptor;

public class FormIndex extends TasklistIndexDescriptor implements Prio4Backup {

  public static final String INDEX_NAME = "form";
  public static final String INDEX_VERSION = "8.4.0";

  public static final String ID = "id";
  public static final String KEY = "key";
  public static final String BPMN_ID = "bpmnId";
  public static final String SCHEMA = "schema";
  public static final String TENANT_ID = "tenantId";
  public static final String VERSION = "version";
  public static final String IS_DELETED = "isDeleted";

  public FormIndex(final String indexPrefix, final boolean isElasticsearch) {
    super(indexPrefix, isElasticsearch);
  }

  @Override
  public String getFullQualifiedName() {
    return String.format("%s-%s-%s_", getIndexPrefix(), getIndexName(), getVersion());
  }

  @Override
  public String getAlias() {
    return getFullQualifiedName() + "alias";
  }

  @Override
  public String getAllVersionsIndexNameRegexPattern() {
    return String.format("%s-%s-\\d.*", getIndexPrefix(), getIndexName());
  }

  @Override
  public String getVersion() {
    return INDEX_VERSION;
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }
}
