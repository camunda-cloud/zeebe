/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest;

import static io.camunda.zeebe.util.buffer.BufferUtil.bufferAsString;

import io.camunda.service.DocumentServices.DocumentReferenceResponse;
import io.camunda.zeebe.gateway.impl.job.JobActivationResult;
import io.camunda.zeebe.gateway.protocol.rest.ActivatedJob;
import io.camunda.zeebe.gateway.protocol.rest.DocumentMetadata;
import io.camunda.zeebe.gateway.protocol.rest.DocumentReference;
import io.camunda.zeebe.gateway.protocol.rest.JobActivationResponse;
import io.camunda.zeebe.gateway.protocol.rest.MessageCorrelationResponse;
import io.camunda.zeebe.msgpack.value.LongValue;
import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.impl.record.value.message.MessageCorrelationRecord;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseMapper {

  public static JobActivationResult<JobActivationResponse> toActivateJobsResponse(
      final io.camunda.zeebe.gateway.impl.job.JobActivationResponse activationResponse) {
    final Iterator<LongValue> jobKeys = activationResponse.brokerResponse().jobKeys().iterator();
    final Iterator<JobRecord> jobs = activationResponse.brokerResponse().jobs().iterator();

    final JobActivationResponse response = new JobActivationResponse();

    while (jobKeys.hasNext() && jobs.hasNext()) {
      final LongValue jobKey = jobKeys.next();
      final JobRecord job = jobs.next();
      final ActivatedJob activatedJob = toActivatedJob(jobKey.getValue(), job);

      response.addJobsItem(activatedJob);
    }

    return new RestJobActivationResult(response);
  }

  private static ActivatedJob toActivatedJob(final long jobKey, final JobRecord job) {
    return new ActivatedJob()
        .key(jobKey)
        .type(job.getType())
        .bpmnProcessId(job.getBpmnProcessId())
        .elementId(job.getElementId())
        .processInstanceKey(job.getProcessInstanceKey())
        .processDefinitionVersion(job.getProcessDefinitionVersion())
        .processDefinitionKey(job.getProcessDefinitionKey())
        .elementInstanceKey(job.getElementInstanceKey())
        .worker(bufferAsString(job.getWorkerBuffer()))
        .retries(job.getRetries())
        .deadline(job.getDeadline())
        .variables(job.getVariables())
        .customHeaders(job.getCustomHeadersObjectMap())
        .tenantId(job.getTenantId());
  }

  public static ResponseEntity<Object> toMessageCorrelationResponse(
      final MessageCorrelationRecord brokerResponse) {
    final var response =
        new MessageCorrelationResponse()
            .key(brokerResponse.getMessageKey())
            .tenantId(brokerResponse.getTenantId())
            .processInstanceKey(brokerResponse.getProcessInstanceKey());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  public static ResponseEntity<Object> toDocumentReference(
      final DocumentReferenceResponse response) {
    final var internalMetadata = response.metadata();
    final var externalMetadata =
        new DocumentMetadata()
            .expiresAt(
                Optional.ofNullable(internalMetadata.expiresAt())
                    .map(Object::toString)
                    .orElse(null))
            .fileName(internalMetadata.fileName())
            .contentType(internalMetadata.contentType());
    Optional.ofNullable(internalMetadata.additionalProperties())
        .ifPresent(map -> map.forEach(externalMetadata::putAdditionalProperty));
    final var reference =
        new DocumentReference()
            .documentId(response.documentId())
            .storeId(response.storeId())
            .metadata(externalMetadata);
    return new ResponseEntity<>(reference, HttpStatus.CREATED);
  }

  static class RestJobActivationResult implements JobActivationResult<JobActivationResponse> {

    private final JobActivationResponse response;

    RestJobActivationResult(final JobActivationResponse response) {
      this.response = response;
    }

    @Override
    public int getJobsCount() {
      return response.getJobs().size();
    }

    @Override
    public List<ActivatedJob> getJobs() {
      return response.getJobs().stream()
          .map(j -> new ActivatedJob(j.getKey(), j.getRetries()))
          .toList();
    }

    @Override
    public JobActivationResponse getActivateJobsResponse() {
      return response;
    }

    @Override
    public List<ActivatedJob> getJobsToDefer() {
      return Collections.emptyList();
    }
  }
}
