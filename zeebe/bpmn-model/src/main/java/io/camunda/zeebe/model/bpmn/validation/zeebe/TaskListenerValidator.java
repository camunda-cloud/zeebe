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
package io.camunda.zeebe.model.bpmn.validation.zeebe;

import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskListener;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public class TaskListenerValidator implements ModelElementValidator<ZeebeTaskListener> {

  @Override
  public Class<ZeebeTaskListener> getElementType() {
    return ZeebeTaskListener.class;
  }

  @Override
  public void validate(
      final ZeebeTaskListener zeebeTaskListener,
      final ValidationResultCollector validationResultCollector) {
    final String errorMessage =
        "Task listeners are not yet supported. Java BPMN-modeling API for task"
            + " listeners was introduced with version 8.6, but the support for listener execution will "
            + "be added in the upcoming versions.";
    validationResultCollector.addError(0, errorMessage);
  }
}
