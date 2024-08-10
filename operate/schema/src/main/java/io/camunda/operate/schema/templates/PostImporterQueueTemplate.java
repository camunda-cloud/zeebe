/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.schema.templates;

import io.camunda.operate.schema.backup.Prio3Backup;
import org.springframework.stereotype.Component;

@Component
public class PostImporterQueueTemplate extends AbstractTemplateDescriptor
    implements ProcessInstanceDependant, Prio3Backup {

  public static final String INDEX_NAME = "post-importer-queue";

  public static final String ID = "id";
  public static final String KEY = "key";
  public static final String PROCESS_INSTANCE_KEY = "processInstanceKey";
  public static final String ACTION_TYPE = "actionType";
  public static final String CREATION_TIME = "creationTime";
  public static final String INTENT = "intent";
  public static final String PARTITION_ID = "partitionId";

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }

  @Override
  public String getVersion() {
    return "8.3.0";
  }
}
