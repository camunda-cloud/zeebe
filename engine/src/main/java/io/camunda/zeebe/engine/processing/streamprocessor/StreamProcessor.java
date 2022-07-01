/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.streamprocessor;

public interface StreamProcessor extends StreamProcessorLifecycleAware {

  void init(ProcessingContext context);

  void apply(TypedRecord typedEvent);

  ProcessingResult process(TypedRecord typedCommand);

  ProcessingResult onProcessingError(
      Throwable processingException, TypedRecord typedCommand, long position);
}
