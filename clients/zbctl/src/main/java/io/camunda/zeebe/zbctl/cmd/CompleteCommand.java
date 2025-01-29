/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.zbctl.cmd;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.CompleteUserTaskCommandStep1;
import io.camunda.client.api.response.CompleteUserTaskResponse;
import io.camunda.zeebe.zbctl.cmd.CompleteCommand.UserTaskCommand;
import io.camunda.zeebe.zbctl.converters.JsonInputConverter;
import io.camunda.zeebe.zbctl.converters.JsonInputConverter.JsonInput;
import io.camunda.zeebe.zbctl.mixin.ClientMixin;
import io.camunda.zeebe.zbctl.mixin.OutputMixin;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "complete",
    description = "Complete actions",
    subcommands = {UserTaskCommand.class})
public class CompleteCommand {

  @Command(name = "userTask", description = "Completes a user task defined by the user task key")
  public static class UserTaskCommand implements Callable<Integer> {

    @Mixin private ClientMixin clientMixin;
    @Mixin private OutputMixin outputMixin;

    @Parameters(
        paramLabel = "<user task key>",
        description = "The key of the user task",
        type = Long.class)
    private long userTaskKey;

    @Option(
        names = {"--action"},
        paramLabel = "<action>",
        description = "The action to complete the user task with",
        defaultValue = "")
    private String action;

    @Option(
        names = {"--variables"},
        paramLabel = "<variables>",
        description = "Specify message variables as JSON string or path to JSON file",
        defaultValue = "{}",
        converter = JsonInputConverter.class)
    private JsonInput variables;

    @Override
    public Integer call() throws Exception {
      try (final var client = clientMixin.client()) {
        final var command = prepareCommand(client);

        if (action != null && !action.isEmpty()) {
          command.action(action);
        }

        final Map<String, Object> variablesInput = variables.get();
        if (variablesInput != null && !variablesInput.isEmpty()) {
          command.variables(variablesInput);
        }

        final var response = command.send().join(30, TimeUnit.SECONDS);
        outputMixin.formatter().write(response, CompleteUserTaskResponse.class);
      }

      return ExitCode.OK;
    }

    private CompleteUserTaskCommandStep1 prepareCommand(final CamundaClient client) {
      return client.newUserTaskCompleteCommand(userTaskKey);
    }
  }
}
