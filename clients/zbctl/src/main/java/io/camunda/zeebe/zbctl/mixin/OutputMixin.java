/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.zbctl.mixin;

import io.camunda.zeebe.zbctl.serde.JsonOutputFormatter;
import io.camunda.zeebe.zbctl.serde.OutputFormatter;
import io.camunda.zeebe.zbctl.serde.PlaintextOutputFormatter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

public final class OutputMixin {
  private static final OutputStream OUTPUT = System.out;

  @Option(
      names = "--format",
      description =
          "Specifies the output format of commands. Must be one of: [${COMPLETION-CANDIDATES}]",
      defaultValue = "JSON",
      scope = ScopeType.INHERIT)
  private OutputFormat outputFormat;

  public OutputFormatter formatter() {
    return outputFormat.format(writer());
  }

  public static Writer writer() {
    return new OutputStreamWriter(OUTPUT, StandardCharsets.UTF_8);
  }

  public enum OutputFormat {
    JSON(JsonOutputFormatter::new),
    PLAINTEXT(PlaintextOutputFormatter::new);

    private final Function<Writer, OutputFormatter> factory;

    OutputFormat(final Function<Writer, OutputFormatter> factory) {
      this.factory = factory;
    }

    private OutputFormatter format(final Writer writer) {
      return factory.apply(writer);
    }
  }
}
