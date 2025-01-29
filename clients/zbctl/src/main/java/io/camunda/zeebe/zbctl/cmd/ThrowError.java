/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.zbctl.cmd;

import io.camunda.zeebe.zbctl.cmd.ThrowError.JobCommand;
import io.camunda.zeebe.zbctl.converters.JsonInputConverter;
import io.camunda.zeebe.zbctl.converters.JsonInputConverter.JsonInput;
import io.camunda.zeebe.zbctl.mixin.ClientMixin;
import io.camunda.zeebe.zbctl.mixin.OutputMixin;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "throwError",
    description = "Throws an error within a resource",
    subcommands = {JobCommand.class})
public class ThrowError {

  @Command(name = "job", description = "Throws an error in a job defined by the job key")
  public static class JobCommand implements Callable<Integer> {

    @Mixin private ClientMixin clientMixin;
    @Mixin private OutputMixin outputMixin;

    @Parameters(paramLabel = "<job key>", description = "The key of the job", type = Long.class)
    private long jobKey;

    @Option(
        names = {"--variables"},
        paramLabel = "<variables>",
        description = "Specify job variables as JSON string or path to JSON file",
        defaultValue = "{}",
        converter = JsonInputConverter.class)
    private JsonInput variables;

    @Option(
        names = {"--errorCode"},
        paramLabel = "<error_code>",
        description = "Specify the code of the error that will be thrown.",
        defaultValue = "-1")
    private String errorCode;

    @Option(
        names = {"--errorMsg"},
        paramLabel = "<error_msg>",
        description = "Specify the message of the error that will be thrown.",
        defaultValue = "")
    private String errorMsg;

    @Override
    public Integer call() throws Exception {
      try (final var client = clientMixin.client()) {
        final var command = client.newThrowErrorCommand(jobKey);

        if (errorCode != null && !errorCode.isEmpty()) {
          final var command2 = command.errorCode(errorCode);

          if (errorMsg != null && !errorMsg.isEmpty()) {
            command2.errorMessage(errorMsg);
          }

          try (final var variablesInput = variables.open()) {
            command2.variables(variablesInput);
          }

          final var response = command2.send().join(30, TimeUnit.SECONDS);
          outputMixin.formatter().write(response, Void.class);
        }
      }

      return ExitCode.OK;
    }
  }
}
