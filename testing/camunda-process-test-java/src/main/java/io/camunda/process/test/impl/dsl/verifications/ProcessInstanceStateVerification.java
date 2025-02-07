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
package io.camunda.process.test.impl.dsl.verifications;

import io.camunda.client.api.search.response.ProcessInstanceState;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.impl.dsl.AbstractTestInstruction;
import io.camunda.process.test.impl.dsl.TestVerification;
import io.camunda.process.test.impl.runner.TestContext;

public class ProcessInstanceStateVerification extends AbstractTestInstruction
    implements TestVerification {

  private String processInstanceAlias;
  private ProcessInstanceState state;

  public ProcessInstanceStateVerification() {}

  public ProcessInstanceStateVerification(
      final String processInstanceAlias, final ProcessInstanceState state) {
    this.processInstanceAlias = processInstanceAlias;
    this.state = state;
  }

  public String getProcessInstanceAlias() {
    return processInstanceAlias;
  }

  public void setProcessInstanceAlias(final String processInstanceAlias) {
    this.processInstanceAlias = processInstanceAlias;
  }

  public ProcessInstanceState getState() {
    return state;
  }

  public void setState(final ProcessInstanceState state) {
    this.state = state;
  }

  @Override
  public void verify(
      final TestContext testContext, final CamundaProcessTestContext processTestContext)
      throws AssertionError {

    if (state == ProcessInstanceState.ACTIVE) {
      testContext.assertThatProcessInstance(processInstanceAlias).isActive();

    } else if (state == ProcessInstanceState.COMPLETED) {
      testContext.assertThatProcessInstance(processInstanceAlias).isCompleted();

    } else if (state == ProcessInstanceState.CANCELED) {
      testContext.assertThatProcessInstance(processInstanceAlias).isTerminated();
    }
  }
}
