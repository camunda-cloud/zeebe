/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.process.test.controller;

import io.camunda.process.test.dto.CamundaClientConnection;
import io.camunda.process.test.impl.containers.CamundaContainer;
import io.camunda.process.test.impl.dsl.TestSpecification;
import io.camunda.process.test.impl.runner.TestSpecificationResult;
import io.camunda.process.test.impl.runtime.CamundaContainerRuntime;
import io.camunda.process.test.services.ProcessTestService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("process-tests")
public class ProcessTestController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTestController.class);

  private final CamundaContainerRuntime camundaContainerRuntime;
  private final ProcessTestService processTestService;

  public ProcessTestController(
      final CamundaContainerRuntime camundaContainerRuntime,
      final ProcessTestService processTestService) {
    this.camundaContainerRuntime = camundaContainerRuntime;
    this.processTestService = processTestService;
  }

  @PostMapping(
      path = "/execute",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody TestSpecificationResult execute(
      @RequestBody final TestSpecification testSpecification) {

    return processTestService.executeTests(testSpecification);
  }

  @GetMapping(
      path = "/connection",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
  public @ResponseBody CamundaClientConnection getConnection() {

    final CamundaContainer camundaContainer = camundaContainerRuntime.getCamundaContainer();
    return new CamundaClientConnection(
        camundaContainer.getRestApiAddress().toString(),
        camundaContainer.getGrpcApiAddress().toString());
  }

  @PostConstruct
  public void printConnection() {
    final CamundaClientConnection connection = getConnection();
    LOGGER.info(
        "To connect a Camunda client use: REST={}, gRPC={}",
        connection.getRestAddress(),
        connection.getGrpcAddress());
  }
}
