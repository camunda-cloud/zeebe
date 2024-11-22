/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.backup.repository.elasticsearch;

import io.camunda.webapps.backup.Metadata;
import io.camunda.webapps.backup.repository.SnapshotNameProvider;

public class TestSnapshotProvider implements SnapshotNameProvider {

  @Override
  public String getSnapshotNamePrefix(final long backupId) {
    return "%s%d".formatted(snapshotNamePrefix(), backupId);
  }

  @Override
  public String getSnapshotName(final Metadata metadata) {
    return getSnapshotNamePrefix(metadata.backupId())
        + "_%d_%d".formatted(metadata.partNo(), metadata.partCount());
  }

  @Override
  public String snapshotNamePrefix() {
    return "test_snapshot_";
  }

  @Override
  public Long extractBackupId(final String snapshotName) {
    return Long.valueOf(snapshotName.split("_")[2]);
  }
}
