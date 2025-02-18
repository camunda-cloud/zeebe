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
package io.camunda.process.test.impl.dsl.actions;

import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.impl.dsl.AbstractTestInstruction;
import io.camunda.process.test.impl.dsl.TestAction;
import io.camunda.process.test.impl.runner.TestContext;

public class CreateProcessInstanceAction extends AbstractTestInstruction implements TestAction {

  private String processId;
  private String variables;
  private String processInstanceAlias;

  public CreateProcessInstanceAction() {}

  public CreateProcessInstanceAction(
      final String processId, final String variables, final String processInstanceAlias) {
    this.processId = processId;
    this.variables = variables;
    this.processInstanceAlias = processInstanceAlias;
  }

  public String getProcessInstanceAlias() {
    return processInstanceAlias;
  }

  public void setProcessInstanceAlias(final String processInstanceAlias) {
    this.processInstanceAlias = processInstanceAlias;
  }

  public String getVariables() {
    return variables;
  }

  public void setVariables(final String variables) {
    this.variables = variables;
  }

  public String getProcessId() {
    return processId;
  }

  public void setProcessId(final String processId) {
    this.processId = processId;
  }

  @Override
  public void execute(
      final TestContext testContext, final CamundaProcessTestContext processTestContext) {
    final ProcessInstanceEvent processInstance =
        testContext
            .getCamundaClient()
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .send()
            .join();

    final long processInstanceKey = processInstance.getProcessInstanceKey();
    testContext.addProcessInstance(processInstanceAlias, processInstanceKey);
  }
}
