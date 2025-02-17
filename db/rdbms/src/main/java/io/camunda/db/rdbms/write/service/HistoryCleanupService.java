/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.service;

import io.camunda.db.rdbms.write.RdbmsWriterConfig;
import io.camunda.db.rdbms.write.util.OffsetDateTimeUtil;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryCleanupService {

  private static final Logger LOG = LoggerFactory.getLogger(HistoryCleanupService.class);

  private final Duration defaultHistoryTTL;
  private final Duration minCleanupInterval;
  private final Duration maxCleanupInterval;
  private final int cleanupBatchSize;

  private final ProcessInstanceWriter processInstanceWriter;
  private final IncidentWriter incidentWriter;
  private final FlowNodeInstanceWriter flowNodeInstanceWriter;
  private final UserTaskWriter userTaskWriter;
  private final VariableWriter variableInstanceWriter;
  private final DecisionInstanceWriter decisionInstanceWriter;

  public HistoryCleanupService(
      final RdbmsWriterConfig config,
      final ProcessInstanceWriter processInstanceWriter,
      final IncidentWriter incidentWriter,
      final FlowNodeInstanceWriter flowNodeInstanceWriter,
      final UserTaskWriter userTaskWriter,
      final VariableWriter variableInstanceWriter,
      final DecisionInstanceWriter decisionInstanceWriter) {
    this.defaultHistoryTTL = config.defaultHistoryTTL();
    this.minCleanupInterval = config.minHistoryCleanupInterval();
    this.maxCleanupInterval = config.maxHistoryCleanupInterval();
    this.cleanupBatchSize = config.historyCleanupBatchSize();
    this.processInstanceWriter = processInstanceWriter;
    this.incidentWriter = incidentWriter;
    this.flowNodeInstanceWriter = flowNodeInstanceWriter;
    this.userTaskWriter = userTaskWriter;
    this.variableInstanceWriter = variableInstanceWriter;
    this.decisionInstanceWriter = decisionInstanceWriter;
  }

  public void scheduleProcessForHistoryCleanup(
      final Long processInstanceKey, final OffsetDateTime endDate) {
    final OffsetDateTime historyCleanupDate = calculateHistoryCleanupDate(endDate);

    LOG.debug(
        "Scheduling process instance cleanup for key {} at {}",
        processInstanceKey,
        historyCleanupDate);
    processInstanceWriter.scheduleForHistoryCleanup(processInstanceKey, historyCleanupDate);
    flowNodeInstanceWriter.scheduleForHistoryCleanup(processInstanceKey, historyCleanupDate);
    incidentWriter.scheduleForHistoryCleanup(processInstanceKey, historyCleanupDate);
    userTaskWriter.scheduleForHistoryCleanup(processInstanceKey, historyCleanupDate);
    variableInstanceWriter.scheduleForHistoryCleanup(processInstanceKey, historyCleanupDate);
    decisionInstanceWriter.scheduleForHistoryCleanup(processInstanceKey, historyCleanupDate);
  }

  public OffsetDateTime calculateHistoryCleanupDate(OffsetDateTime endDate) {
    return OffsetDateTimeUtil.addDuration(endDate, this.defaultHistoryTTL.toString());
  }

  public Duration cleanupHistory(int partitionId, OffsetDateTime cleanupDate) {
    LOG.info("Cleanup history for partition {} with TTL before {}", partitionId, cleanupDate);

    processInstanceWriter.cleanupHistory(partitionId, cleanupDate, cleanupBatchSize);
    flowNodeInstanceWriter.cleanupHistory(partitionId, cleanupDate, cleanupBatchSize);
    incidentWriter.cleanupHistory(partitionId, cleanupDate, cleanupBatchSize);
    userTaskWriter.cleanupHistory(partitionId, cleanupDate, cleanupBatchSize);
    variableInstanceWriter.cleanupHistory(partitionId, cleanupDate, cleanupBatchSize);
    decisionInstanceWriter.cleanupHistory(partitionId, cleanupDate, cleanupBatchSize);

    return this.minCleanupInterval;
  }
}
