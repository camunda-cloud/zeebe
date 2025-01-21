/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.expression;

import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import java.util.List;
import java.util.Objects;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class EnvVariableEvaluationContext implements ScopedEvaluationContext {
  public static final List<String> ALLOWED_ENVIRONMENT_VARIABLE_PREFIXES =
      List.of("CUSTOM_", "ZEEBE_");

  private final List<String> allowedPrefixes;

  public EnvVariableEvaluationContext() {
    allowedPrefixes = ALLOWED_ENVIRONMENT_VARIABLE_PREFIXES;
  }

  public EnvVariableEvaluationContext(final List<String> allowedPrefixes) {
    this.allowedPrefixes = allowedPrefixes;
  }

  @Override
  public DirectBuffer getVariable(final String variableName) {
    Objects.requireNonNull(variableName);

    if (isAllowed(variableName)) {
      final String value = System.getenv(variableName);

      if (value != null) {
        return asMsgPack(value);
      }
    }

    return null;
  }

  private static UnsafeBuffer asMsgPack(final String value) {
    // @TODO is this the best way to convert to MsgPack?
    return new UnsafeBuffer(MsgPackConverter.convertToMsgPack(String.format("\"%s\"", value)));
  }

  private boolean isAllowed(final String variableName) {
    return allowedPrefixes.isEmpty()
        || allowedPrefixes.stream().anyMatch(prefix -> variableName.startsWith(prefix));
  }
}
