/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.backup;

import static io.camunda.application.commons.backup.ConfigValidation.allMatch;
import static io.camunda.application.commons.backup.ConfigValidation.skipEmptyOptional;

import io.camunda.operate.conditions.DatabaseInfo;
import io.camunda.operate.conditions.DatabaseType;
import io.camunda.operate.property.OperateProperties;
import io.camunda.tasklist.property.TasklistProperties;
import io.camunda.webapps.profiles.ProfileOperateTasklist;
import io.camunda.webapps.schema.descriptors.backup.BackupPriorities;
import io.camunda.webapps.schema.descriptors.backup.Prio1Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio2Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio3Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio4Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio5Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio6Backup;
import io.camunda.webapps.schema.descriptors.operate.index.DecisionIndex;
import io.camunda.webapps.schema.descriptors.operate.index.DecisionRequirementsIndex;
import io.camunda.webapps.schema.descriptors.operate.index.ImportPositionIndex;
import io.camunda.webapps.schema.descriptors.operate.index.MetricIndex;
import io.camunda.webapps.schema.descriptors.operate.index.ProcessIndex;
import io.camunda.webapps.schema.descriptors.operate.template.BatchOperationTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.DecisionInstanceTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.EventTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.FlowNodeInstanceTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.IncidentTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.JobTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.MessageTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.OperationTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.PostImporterQueueTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.SequenceFlowTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.VariableTemplate;
import io.camunda.webapps.schema.descriptors.tasklist.index.FormIndex;
import io.camunda.webapps.schema.descriptors.tasklist.index.TasklistImportPositionIndex;
import io.camunda.webapps.schema.descriptors.tasklist.index.TasklistMetricIndex;
import io.camunda.webapps.schema.descriptors.tasklist.template.DraftTaskVariableTemplate;
import io.camunda.webapps.schema.descriptors.tasklist.template.SnapshotTaskVariableTemplate;
import io.camunda.webapps.schema.descriptors.tasklist.template.TaskTemplate;
import io.camunda.webapps.schema.descriptors.usermanagement.index.AuthorizationIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.GroupIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.MappingIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.PersistentWebSessionIndexDescriptor;
import io.camunda.webapps.schema.descriptors.usermanagement.index.RoleIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.TenantIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.UserIndex;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ProfileOperateTasklist
public class BackupPriorityConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(BackupPriorityConfiguration.class);
  private static final String NO_CONFIG_ERROR_MESSAGE =
      "Expected operate or tasklist to be configured, but none of them is.";

  final OperateProperties operateProperties;
  final TasklistProperties tasklistProperties;
  final String[] profiles;

  public BackupPriorityConfiguration(
      @Autowired(required = false) final OperateProperties operateProperties,
      @Autowired(required = false) final TasklistProperties tasklistProperties,
      @Autowired final Environment environment) {
    profiles = environment.getActiveProfiles();
    if (environment.matchesProfiles("operate")) {
      this.operateProperties = operateProperties;
    } else {
      this.operateProperties = null;
    }
    if (environment.matchesProfiles("tasklist")) {
      this.tasklistProperties = tasklistProperties;
    } else {
      this.tasklistProperties = null;
    }
  }

  private <A> Function<Map<String, A>, String> differentConfigFor(final String field) {
    return values ->
        String.format(
            "Expected %s to be configured with the same value in operate and tasklist. Got %s. Active profiles: %s",
            field, values, Arrays.asList(profiles));
  }

  @Bean
  public BackupPriorities backupPriorities() {
    final var indexPrefix =
        allMatch(
                NO_CONFIG_ERROR_MESSAGE,
                differentConfigFor("indexPrefix"),
                Map.of(
                    "operate",
                    Optional.ofNullable(operateProperties).map(OperateProperties::getIndexPrefix),
                    "tasklist",
                    Optional.ofNullable(tasklistProperties)
                        .map(TasklistProperties::getIndexPrefix)),
                skipEmptyOptional())
            .get();
    final var isElasticsearch =
        allMatch(
                NO_CONFIG_ERROR_MESSAGE,
                differentConfigFor("database.type"),
                Map.of(
                    "operate",
                    Optional.ofNullable(operateProperties)
                        .map(ignored -> DatabaseInfo.isCurrent(DatabaseType.Elasticsearch)),
                    "tasklist",
                    Optional.ofNullable(tasklistProperties)
                        .map(prop -> prop.getDatabase().equals(TasklistProperties.ELASTIC_SEARCH))),
                skipEmptyOptional())
            .get();
    final List<Prio1Backup> prio1 =
        List.of(
            // OPERATE
            new ImportPositionIndex(indexPrefix, isElasticsearch),
            // TASKLIST
            new TasklistImportPositionIndex(indexPrefix, isElasticsearch));

    final List<Prio2Backup> prio2 =
        List.of(
            // OPERATE
            new ListViewTemplate(indexPrefix, isElasticsearch),
            // TASKLIST
            new TaskTemplate(indexPrefix, isElasticsearch));

    final List<Prio3Backup> prio3 =
        List.of(
            // OPERATE
            new BatchOperationTemplate(indexPrefix, isElasticsearch),
            new OperationTemplate(indexPrefix, isElasticsearch));

    final List<Prio4Backup> prio4 =
        List.of(
            // OPERATE
            new DecisionIndex(indexPrefix, isElasticsearch),
            new DecisionInstanceTemplate(indexPrefix, isElasticsearch),
            new EventTemplate(indexPrefix, isElasticsearch),
            new FlowNodeInstanceTemplate(indexPrefix, isElasticsearch),
            new IncidentTemplate(indexPrefix, isElasticsearch),
            new JobTemplate(indexPrefix, isElasticsearch),
            new MessageTemplate(indexPrefix, isElasticsearch),
            new PostImporterQueueTemplate(indexPrefix, isElasticsearch),
            new SequenceFlowTemplate(indexPrefix, isElasticsearch),
            new VariableTemplate(indexPrefix, isElasticsearch),
            // TASKLIST
            new DraftTaskVariableTemplate(indexPrefix, isElasticsearch),
            new SnapshotTaskVariableTemplate(indexPrefix, isElasticsearch));

    final List<Prio5Backup> prio5 =
        List.of(
            // OPERATE
            new DecisionRequirementsIndex(indexPrefix, isElasticsearch),
            new MetricIndex(indexPrefix, isElasticsearch),
            new ProcessIndex(indexPrefix, isElasticsearch),
            // TASKLIST
            new FormIndex(indexPrefix, isElasticsearch),
            new TasklistMetricIndex(indexPrefix, isElasticsearch),
            // USER MANAGEMENT
            new AuthorizationIndex(indexPrefix, isElasticsearch),
            new GroupIndex(indexPrefix, isElasticsearch),
            new MappingIndex(indexPrefix, isElasticsearch),
            new PersistentWebSessionIndexDescriptor(indexPrefix, isElasticsearch),
            new RoleIndex(indexPrefix, isElasticsearch),
            new TenantIndex(indexPrefix, isElasticsearch),
            new UserIndex(indexPrefix, isElasticsearch));

    // OPTIMIZE static indices
    final List<Prio6Backup> prio6 = List.of();
    LOG.debug("Prio1 are {}", prio1);
    LOG.debug("Prio2 are {}", prio2);
    LOG.debug("Prio3 are {}", prio3);
    LOG.debug("Prio4 are {}", prio4);
    LOG.debug("Prio5 are {}", prio5);
    LOG.debug("Prio6 are {}", prio6);
    return new BackupPriorities(prio1, prio2, prio3, prio4, prio5, prio6);
  }
}
