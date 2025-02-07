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

import io.camunda.client.api.search.response.FlowNodeInstanceState;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.assertions.ElementSelector;
import io.camunda.process.test.api.assertions.ElementSelectors;
import io.camunda.process.test.api.assertions.ProcessInstanceAssert;
import io.camunda.process.test.impl.dsl.AbstractTestInstruction;
import io.camunda.process.test.impl.dsl.TestVerification;
import io.camunda.process.test.impl.runner.TestContext;

public class ElementInstanceStateVerification extends AbstractTestInstruction
    implements TestVerification {

  private String processInstanceAlias;
  private String elementId;
  private FlowNodeInstanceState state;

  public ElementInstanceStateVerification() {}

  public ElementInstanceStateVerification(
      final String processInstanceAlias,
      final String elementId,
      final FlowNodeInstanceState state) {
    this.processInstanceAlias = processInstanceAlias;
    this.elementId = elementId;
    this.state = state;
  }

  public String getProcessInstanceAlias() {
    return processInstanceAlias;
  }

  public void setProcessInstanceAlias(final String processInstanceAlias) {
    this.processInstanceAlias = processInstanceAlias;
  }

  public String getElementId() {
    return elementId;
  }

  public void setElementId(final String elementId) {
    this.elementId = elementId;
  }

  public FlowNodeInstanceState getState() {
    return state;
  }

  public void setState(final FlowNodeInstanceState state) {
    this.state = state;
  }

  @Override
  public void verify(
      final TestContext testContext, final CamundaProcessTestContext processTestContext)
      throws AssertionError {

    final ElementSelector elementSelector = ElementSelectors.byId(elementId);

    final ProcessInstanceAssert processInstanceAssert =
        testContext.assertThatProcessInstance(processInstanceAlias);

    switch (state) {
      case ACTIVE:
        processInstanceAssert.hasActiveElements(elementSelector);
        break;

      case COMPLETED:
        processInstanceAssert.hasCompletedElements(elementSelector);
        break;

      case TERMINATED:
        processInstanceAssert.hasTerminatedElements(elementSelector);
        break;

      default:
    }
  }
}
