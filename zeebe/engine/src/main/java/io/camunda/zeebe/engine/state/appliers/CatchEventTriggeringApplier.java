/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.appliers;

import io.camunda.zeebe.engine.state.TypedEventApplier;
import io.camunda.zeebe.engine.state.mutable.MutableEventScopeInstanceState;
import io.camunda.zeebe.protocol.impl.record.value.events.CatchEventRecord;
import io.camunda.zeebe.protocol.record.intent.CatchEventIntent;

public class CatchEventTriggeringApplier
    implements TypedEventApplier<CatchEventIntent, CatchEventRecord> {

  private final MutableEventScopeInstanceState eventScopeInstanceState;

  public CatchEventTriggeringApplier(final MutableEventScopeInstanceState eventScopeInstanceState) {
    this.eventScopeInstanceState = eventScopeInstanceState;
  }

  @Override
  public void applyState(final long key, final CatchEventRecord value) {
    eventScopeInstanceState.recordCatchEventTriggering(key, value);
  }
}
