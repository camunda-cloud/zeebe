/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.qa.util;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public abstract class VariablesUtil {

  public static final String VAR_SUFFIX = "9999999999";

  public static String createBigVarsWithSuffix(final int size) {
    return createBigVarsWithSuffix(null, size, null);
  }

  public static String createBigVarsWithSuffix(final int size, final String varSuffix) {
    return createBigVarsWithSuffix(null, size, varSuffix);
  }

  public static String createBigVarsWithSuffix(
      final String varNamePrefix, final int size, final String varSuffix) {
    final StringBuffer vars = new StringBuffer("{");
    for (int i = 0; i < 3; i++) {
      if (vars.length() > 1) {
        vars.append(",\n");
      }
      vars.append("\"")
          .append(varNamePrefix == null ? "" : varNamePrefix + "_")
          .append("var")
          .append(i)
          .append("\": \"")
          .append(createBigVariable(size))
          .append(varSuffix == null ? VAR_SUFFIX : varSuffix)
          .append("\"");
    }
    vars.append("}");
    return vars.toString();
  }

  public static String createBigVariable(int size) {
    final Random random = new Random();
    final StringBuffer sb = new StringBuffer();
    for (int i = 0; i < size; i++) {
      sb.append(random.nextInt(9));
    }
    return sb.toString();
  }

  public static String createALotOfVarsPayload() {
    final Map<String, String> payload = new HashMap<>();
    payload.put("var1", "value1");
    final int numberOfVars = 600;
    IntStream.range(0, numberOfVars)
        .forEach(
            value -> {
              payload.put("many_vars_" + value, "value_" + value);
            });
    return new Gson().toJson(payload);
  }
}
