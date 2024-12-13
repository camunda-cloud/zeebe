/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.backup;

import io.camunda.webapps.backup.BackupRepository;
import io.camunda.webapps.backup.BackupService;
import io.camunda.webapps.backup.BackupServiceImpl;
import io.camunda.webapps.backup.repository.BackupRepositoryProps;
import io.camunda.webapps.profiles.ProfileOperateTasklist;
import io.camunda.webapps.schema.descriptors.backup.Prio1Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio2Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio3Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio4Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio5Backup;
import io.camunda.webapps.schema.descriptors.backup.Prio6Backup;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ProfileOperateTasklist
public class HistoryBackupComponent {
  private static final Logger LOG = LoggerFactory.getLogger(HistoryBackupComponent.class);

  private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
  private final List<Prio1Backup> prio1BackupIndices;
  private final List<Prio2Backup> prio2BackupTemplates;
  private final List<Prio3Backup> prio3BackupTemplates;
  private final List<Prio4Backup> prio4BackupTemplates;
  private final List<Prio5Backup> prio5BackupIndices;
  private final BackupRepositoryProps backupRepositoryProps;
  private final BackupRepository backupRepository;
  private final List<Prio6Backup> prio6BackupIndices;

  public HistoryBackupComponent(
      @Qualifier("backupThreadPoolExecutor") final ThreadPoolTaskExecutor threadPoolTaskExecutor,
      final List<Prio1Backup> prio1BackupIndices,
      final List<Prio2Backup> prio2BackupTemplates,
      final List<Prio3Backup> prio3BackupTemplates,
      final List<Prio4Backup> prio4BackupTemplates,
      final List<Prio5Backup> prio5BackupIndices,
      final List<Prio6Backup> prio6BackupIndices,
      final BackupRepositoryProps backupRepositoryProps,
      final BackupRepository backupRepository) {
    LOG.debug("Prio1BackupIndices are {}", prio1BackupIndices);
    LOG.debug("Prio2BackupTemplates are {}", prio2BackupTemplates);
    LOG.debug("Prio3BackupTemplates are {}", prio3BackupTemplates);
    LOG.debug("Prio4BackupIndices are {}", prio4BackupTemplates);
    LOG.debug("Prio5BackupIndices are {}", prio5BackupIndices);
    LOG.debug("Prio6BackupIndices are {}", prio6BackupIndices);
    this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    this.prio1BackupIndices = prio1BackupIndices;
    this.prio2BackupTemplates = prio2BackupTemplates;
    this.prio3BackupTemplates = prio3BackupTemplates;
    this.prio4BackupTemplates = prio4BackupTemplates;
    this.prio5BackupIndices = prio5BackupIndices;
    this.prio6BackupIndices = prio6BackupIndices;
    this.backupRepositoryProps = backupRepositoryProps;
    this.backupRepository = backupRepository;
  }

  @Bean
  public BackupService backupService() {
    return new BackupServiceImpl(
        threadPoolTaskExecutor,
        prio1BackupIndices,
        prio2BackupTemplates,
        prio3BackupTemplates,
        prio4BackupTemplates,
        prio5BackupIndices,
        prio6BackupIndices,
        backupRepositoryProps,
        backupRepository);
  }
}
