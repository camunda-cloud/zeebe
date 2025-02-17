/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.util.client;

import io.camunda.zeebe.protocol.impl.record.value.deployment.ResourceRecord;
import io.camunda.zeebe.protocol.record.intent.ResourceIntent;
import io.camunda.zeebe.protocol.record.value.TenantOwned;
import java.util.List;

public class ResourceFetchClient {

  private final CommandWriter writer;
  private final ResourceRecord resourceRecord = new ResourceRecord();
  private List<String> authorizedTenantIds = List.of(TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private int requestStreamId = 1;
  private long requestId = 1L;

  public ResourceFetchClient(final CommandWriter writer) {
    this.writer = writer;
  }

  public ResourceFetchClient withResourceKey(final long resourceKey) {
    resourceRecord.setResourceKey(resourceKey);
    return this;
  }

  public ResourceFetchClient withAuthorizedTenantIds(final String... tenantIds) {
    authorizedTenantIds = List.of(tenantIds);
    return this;
  }

  public ResourceFetchClient withRequestStreamId(final int requestStreamId) {
    this.requestStreamId = requestStreamId;
    return this;
  }

  public ResourceFetchClient withRequestId(final long requestId) {
    this.requestId = requestId;
    return this;
  }

  public void fetch() {
    writer.writeCommand(
        requestStreamId,
        requestId,
        ResourceIntent.FETCH,
        resourceRecord,
        authorizedTenantIds.toArray(new String[0]));
  }
}
