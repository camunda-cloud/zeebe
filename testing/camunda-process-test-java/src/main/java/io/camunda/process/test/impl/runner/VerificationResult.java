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
package io.camunda.process.test.impl.runner;

import io.camunda.process.test.impl.dsl.TestInstruction;

public class VerificationResult {

  private TestInstruction instruction;
  private boolean isSuccessful;
  private String failureMessage;

  public VerificationResult() {}

  public VerificationResult(
      final TestInstruction instruction, final boolean isSuccessful, final String failureMessage) {
    this.instruction = instruction;
    this.isSuccessful = isSuccessful;
    this.failureMessage = failureMessage;
  }

  public boolean isSuccessful() {
    return isSuccessful;
  }

  public void setSuccessful(final boolean successful) {
    isSuccessful = successful;
  }

  public String getFailureMessage() {
    return failureMessage;
  }

  public void setFailureMessage(final String failureMessage) {
    this.failureMessage = failureMessage;
  }

  public TestInstruction getInstruction() {
    return instruction;
  }

  public void setInstruction(final TestInstruction instruction) {
    this.instruction = instruction;
  }
}
