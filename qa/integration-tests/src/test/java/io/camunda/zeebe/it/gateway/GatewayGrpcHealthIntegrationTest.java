/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.it.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.camunda.zeebe.test.util.testcontainers.ZeebeTestContainerDefaults;
import io.zeebe.containers.ZeebeGatewayContainer;
import io.zeebe.containers.ZeebePort;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class GatewayGrpcHealthIntegrationTest {

  private final Network network = Network.newNetwork();

  @Container
  private final ZeebeGatewayContainer zeebeGatewayContainer =
      new ZeebeGatewayContainer(ZeebeTestContainerDefaults.defaultTestImage())
          .withoutTopologyCheck()
          .withNetwork(network);

  private final GenericContainer<?> grpcHealthProbeContainer =
      new GenericContainer<>(
              new ImageFromDockerfile()
                  .withDockerfileFromBuilder(
                      builder ->
                          builder
                              .from("alpine:3.14")
                              .run(
                                  "set -ex "
                                      + "    && apk add --no-cache curl "
                                      + "    && curl -fSL https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/v0.4.5/grpc_health_probe-linux-amd64 -o grpc_health_probe "
                                      + "    && chmod +x grpc_health_probe")
                              .entryPoint("while true; do date; sleep 5; done")
                              .build()))
          .waitingFor(
              new AbstractWaitStrategy() {
                @Override
                protected void waitUntilReady() {
                  await()
                      .until(
                          () ->
                              waitStrategyTarget.getCurrentContainerInfo().getState().getRunning());
                }
              })
          .dependsOn(zeebeGatewayContainer)
          .withNetwork(network);

  @Test
  void shouldReturnServingWhenGatewayIsStarted() throws IOException, InterruptedException {
    try (grpcHealthProbeContainer) {
      grpcHealthProbeContainer.start();
      final ExecResult execResult =
          grpcHealthProbeContainer.execInContainer(
              "./grpc_health_probe",
              "--addr",
              zeebeGatewayContainer.getInternalAddress(ZeebePort.GATEWAY.getPort()));
      assertThat(execResult.getExitCode()).isEqualTo(0);
      assertThat(execResult.getStderr()).contains("status: SERVING");
    }
  }
}
