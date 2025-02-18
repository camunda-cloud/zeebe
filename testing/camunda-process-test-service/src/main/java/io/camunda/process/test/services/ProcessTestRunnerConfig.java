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
package io.camunda.process.test.services;

import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.impl.runner.ProcessTestRunner;
import io.camunda.process.test.impl.runtime.CamundaContainerRuntime;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessTestRunnerConfig {

  @Bean
  public ProcessTestRunner processTestRunner(
      final CamundaContainerRuntime camundaContainerRuntime) {

    camundaContainerRuntime.start();

    CamundaAssert.setAssertionTimeout(Duration.ofSeconds(5));

    return new ProcessTestRunner(
        () -> camundaContainerRuntime,
        runtime -> {
          // reset runtime (e.g. set clock to current time)
        },
        runtime -> {
          // clean runtime (e.g. run purge command)
        });
  }

  public ProcessTestRunner slowProcessTestRunner() {
    // create new runtime for each test case
    return new ProcessTestRunner(
        CamundaContainerRuntime::newDefaultRuntime,
        CamundaContainerRuntime::start,
        this::closeRuntime);
  }

  @Bean
  public CamundaContainerRuntime camundaContainerRuntime() {
    return CamundaContainerRuntime.newBuilder().build();
  }

  public void closeRuntime(final CamundaContainerRuntime runtime) {
    try {
      runtime.close();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
