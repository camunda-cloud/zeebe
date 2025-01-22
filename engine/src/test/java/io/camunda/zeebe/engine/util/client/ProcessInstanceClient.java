/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.util.client;

import static io.camunda.zeebe.util.buffer.BufferUtil.wrapString;

import io.camunda.zeebe.engine.util.StreamProcessorRule;
import io.camunda.zeebe.msgpack.property.ArrayProperty;
import io.camunda.zeebe.msgpack.value.StringValue;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceCreationRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ErrorIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceCreationIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.ErrorRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.test.util.MsgPackUtil;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class ProcessInstanceClient {

  private final StreamProcessorRule environmentRule;

  public ProcessInstanceClient(final StreamProcessorRule environmentRule) {
    this.environmentRule = environmentRule;
  }

  public ProcessInstanceCreationClient ofBpmnProcessId(final String bpmnProcessId) {
    return new ProcessInstanceCreationClient(environmentRule, bpmnProcessId);
  }

  public ExistingInstanceClient withInstanceKey(final long processInstanceKey) {
    return new ExistingInstanceClient(environmentRule, processInstanceKey);
  }

  public static class ProcessInstanceCreationClient {

    private static final Function<Long, Record<ProcessInstanceCreationRecordValue>>
        SUCCESS_EXPECTATION =
            (position) ->
                RecordingExporter.processInstanceCreationRecords()
                    .withIntent(ProcessInstanceCreationIntent.CREATED)
                    .withSourceRecordPosition(position)
                    .getFirst();

    private static final Function<Long, Record<ProcessInstanceCreationRecordValue>>
        REJECTION_EXPECTATION =
            (position) ->
                RecordingExporter.processInstanceCreationRecords()
                    .onlyCommandRejections()
                    .withIntent(ProcessInstanceCreationIntent.CREATE)
                    .withSourceRecordPosition(position)
                    .getFirst();

    private final StreamProcessorRule environmentRule;
    private final ProcessInstanceCreationRecord processInstanceCreationRecord;

    private Function<Long, Record<ProcessInstanceCreationRecordValue>> expectation =
        SUCCESS_EXPECTATION;

    public ProcessInstanceCreationClient(
        final StreamProcessorRule environmentRule, final String bpmnProcessId) {
      this.environmentRule = environmentRule;
      processInstanceCreationRecord = new ProcessInstanceCreationRecord();
      processInstanceCreationRecord.setBpmnProcessId(bpmnProcessId);
    }

    public ProcessInstanceCreationClient withVariables(final Map<String, Object> variables) {
      processInstanceCreationRecord.setVariables(MsgPackUtil.asMsgPack(variables));
      return this;
    }

    public ProcessInstanceCreationClient withVariables(final String variables) {
      processInstanceCreationRecord.setVariables(MsgPackUtil.asMsgPack(variables));
      return this;
    }

    public ProcessInstanceCreationClient withVariable(final String key, final Object value) {
      processInstanceCreationRecord.setVariables(MsgPackUtil.asMsgPack(key, value));
      return this;
    }

    public ProcessInstanceCreationWithResultClient withResult() {
      return new ProcessInstanceCreationWithResultClient(
          environmentRule, processInstanceCreationRecord);
    }

    public long create() {
      final long position =
          environmentRule.writeCommand(
              ProcessInstanceCreationIntent.CREATE, processInstanceCreationRecord);

      final var resultingRecord = expectation.apply(position);
      return resultingRecord.getValue().getProcessInstanceKey();
    }

    public long createOnPartition(final int partitionId) {
      final long position =
          environmentRule.writeCommandOnPartition(
              partitionId, ProcessInstanceCreationIntent.CREATE, processInstanceCreationRecord);

      final var resultingRecord = expectation.apply(position);
      return resultingRecord.getValue().getProcessInstanceKey();
    }

    public ProcessInstanceCreationClient expectRejection() {
      expectation = REJECTION_EXPECTATION;
      return this;
    }
  }

  public static class ProcessInstanceCreationWithResultClient {
    private final StreamProcessorRule environmentRule;
    private final ProcessInstanceCreationRecord record;
    private long requestId = 1L;
    private int requestStreamId = 1;

    public ProcessInstanceCreationWithResultClient(
        final StreamProcessorRule environmentRule, final ProcessInstanceCreationRecord record) {
      this.environmentRule = environmentRule;
      this.record = record;
    }

    public ProcessInstanceCreationWithResultClient withFetchVariables(
        final Set<String> fetchVariables) {
      final ArrayProperty<StringValue> variablesToCollect = record.fetchVariables();
      fetchVariables.forEach(variable -> variablesToCollect.add().wrap(wrapString(variable)));
      return this;
    }

    public ProcessInstanceCreationWithResultClient withRequestId(final long requestId) {
      this.requestId = requestId;
      return this;
    }

    public ProcessInstanceCreationWithResultClient withRequestStreamId(final int requestStreamId) {
      this.requestStreamId = requestStreamId;
      return this;
    }

    public long create() {
      final long position =
          environmentRule.writeCommand(
              requestStreamId,
              requestId,
              ProcessInstanceCreationIntent.CREATE_WITH_AWAITING_RESULT,
              record);

      return RecordingExporter.processInstanceCreationRecords()
          .withIntent(ProcessInstanceCreationIntent.CREATED)
          .withSourceRecordPosition(position)
          .getFirst()
          .getValue()
          .getProcessInstanceKey();
    }

    public void asyncCreate() {
      environmentRule.writeCommand(
          requestStreamId,
          requestId,
          ProcessInstanceCreationIntent.CREATE_WITH_AWAITING_RESULT,
          record);
    }
  }

  public static class ExistingInstanceClient {

    public static final Function<Long, Record<ProcessInstanceRecordValue>> SUCCESS_EXPECTATION =
        (processInstanceKey) ->
            RecordingExporter.processInstanceRecords()
                .withRecordKey(processInstanceKey)
                .withIntent(ProcessInstanceIntent.ELEMENT_TERMINATED)
                .withProcessInstanceKey(processInstanceKey)
                .getFirst();

    public static final Function<Long, Record<ProcessInstanceRecordValue>> REJECTION_EXPECTATION =
        (processInstanceKey) ->
            RecordingExporter.processInstanceRecords()
                .onlyCommandRejections()
                .withIntent(ProcessInstanceIntent.CANCEL)
                .withRecordKey(processInstanceKey)
                .withProcessInstanceKey(processInstanceKey)
                .getFirst();

    public static final Function<Long, Record<ErrorRecordValue>> ERROR_EXPECTATION =
        (processInstanceKey) ->
            RecordingExporter.errorRecords().withIntent(ErrorIntent.CREATED).getFirst();

    private static final int DEFAULT_PARTITION = -1;
    private final StreamProcessorRule environmentRule;
    private final long processInstanceKey;

    private int partition = DEFAULT_PARTITION;
    private Function<Long, Record<ProcessInstanceRecordValue>> expectation = SUCCESS_EXPECTATION;

    public ExistingInstanceClient(
        final StreamProcessorRule environmentRule, final long processInstanceKey) {
      this.environmentRule = environmentRule;
      this.processInstanceKey = processInstanceKey;
    }

    public ExistingInstanceClient onPartition(final int partition) {
      this.partition = partition;
      return this;
    }

    public ExistingInstanceClient expectRejection() {
      expectation = REJECTION_EXPECTATION;
      return this;
    }

    public Record<ProcessInstanceRecordValue> cancel() {
      writeCancelCommand();
      return expectation.apply(processInstanceKey);
    }

    public Record<ErrorRecordValue> cancelWithError() {
      writeCancelCommand();
      return ERROR_EXPECTATION.apply(processInstanceKey);
    }

    private void writeCancelCommand() {
      if (partition == DEFAULT_PARTITION) {
        partition =
            RecordingExporter.processInstanceRecords()
                .withProcessInstanceKey(processInstanceKey)
                .getFirst()
                .getPartitionId();
      }

      environmentRule.writeCommandOnPartition(
          partition,
          processInstanceKey,
          ProcessInstanceIntent.CANCEL,
          new ProcessInstanceRecord().setProcessInstanceKey(processInstanceKey));
    }
  }
}
