/*
 * Copyright 2016-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.raft.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LeaderMetrics extends RaftMetrics {
  private static final String FOLLOWER_LABEL = "follower";

  private final MeterRegistry meterRegistry;
  private final Map<String, Timer> appendLatency;
  private final Map<String, Counter> appendDataRate;
  private final Map<String, Counter> appendRate;
  private final Counter commitRate;
  private final AtomicLong nonCommittedEntriesValue;
  private final Map<String, AtomicLong> nonReplicatedEntries;

  public LeaderMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    this.meterRegistry = meterRegistry;
    appendLatency = new HashMap<>();
    appendDataRate = new HashMap<>();
    appendRate = new HashMap<>();
    nonReplicatedEntries = new HashMap<>();

    commitRate =
        Counter.builder(LeaderMetricsDoc.COMMIT_RATE.getName())
            .description(LeaderMetricsDoc.COMMIT_RATE.getDescription())
            .tags(PARTITION_GROUP_NAME_LABEL, partitionGroupName)
            .register(meterRegistry);

    nonCommittedEntriesValue = new AtomicLong(0L);
    Gauge.builder(LeaderMetricsDoc.NON_COMMITTED_ENTRIES.getName(), nonCommittedEntriesValue::get)
        .description(LeaderMetricsDoc.NON_COMMITTED_ENTRIES.getDescription())
        .tags(PARTITION_GROUP_NAME_LABEL, partitionGroupName)
        .register(meterRegistry);
  }

  public void appendComplete(final long latencyms, final String memberId) {
    getAppendLatency(memberId).record(latencyms, TimeUnit.MILLISECONDS);
  }

  public void observeAppend(
      final String memberId, final int appendedEntries, final int appendedBytes) {
    getAppendRate(memberId).increment(appendedEntries);
    getAppendDataRate(memberId).increment(appendedBytes / 1024f);
  }

  public void observeCommit() {
    commitRate.increment();
  }

  public void observeNonCommittedEntries(final long remainingEntries) {
    nonCommittedEntriesValue.set(remainingEntries);
  }

  public void observeRemainingEntries(final String memberId, final long remainingEntries) {
    getNonReplicatedEntries(memberId).set(remainingEntries);
  }

  private Timer getAppendLatency(final String memberId) {
    return appendLatency.computeIfAbsent(
        memberId,
        id ->
            Timer.builder(LeaderMetricsDoc.APPEND_ENTRIES_LATENCY.getName())
                .description(LeaderMetricsDoc.APPEND_ENTRIES_LATENCY.getDescription())
                .serviceLevelObjectives(LeaderMetricsDoc.APPEND_ENTRIES_LATENCY.getTimerSLOs())
                .tags(FOLLOWER_LABEL, memberId, PARTITION_GROUP_NAME_LABEL, partitionGroupName)
                .register(meterRegistry));
  }

  private Counter getAppendDataRate(final String memberId) {
    return appendDataRate.computeIfAbsent(
        memberId,
        id ->
            Counter.builder(LeaderMetricsDoc.APPEND_DATA_RATE.getName())
                .description(LeaderMetricsDoc.APPEND_DATA_RATE.getDescription())
                .tags(FOLLOWER_LABEL, id, PARTITION_GROUP_NAME_LABEL, partitionGroupName)
                .register(meterRegistry));
  }

  private Counter getAppendRate(final String memberId) {
    return appendRate.computeIfAbsent(
        memberId,
        id ->
            Counter.builder(LeaderMetricsDoc.APPEND_RATE.getName())
                .description(LeaderMetricsDoc.APPEND_RATE.getDescription())
                .tags(FOLLOWER_LABEL, id, PARTITION_GROUP_NAME_LABEL, partitionGroupName)
                .register(meterRegistry));
  }

  private AtomicLong getNonReplicatedEntries(final String memberId) {
    var inMap = nonReplicatedEntries.get(memberId);
    if (inMap == null) {
      inMap = new AtomicLong(0L);
      // add new gauge for this entry
      Gauge.builder(LeaderMetricsDoc.NON_REPLICATED_ENTRIES.getName(), inMap::get)
          .description(LeaderMetricsDoc.NON_REPLICATED_ENTRIES.getDescription())
          .tags(FOLLOWER_LABEL, memberId, PARTITION_GROUP_NAME_LABEL, partitionGroupName)
          .register(meterRegistry);
      nonReplicatedEntries.put(memberId, inMap);
    }
    return inMap;
  }
}
