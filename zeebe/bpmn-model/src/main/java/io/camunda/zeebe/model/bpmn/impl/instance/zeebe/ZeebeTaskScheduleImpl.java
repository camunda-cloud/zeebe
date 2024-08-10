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
package io.camunda.zeebe.model.bpmn.impl.instance.zeebe;

import io.camunda.zeebe.model.bpmn.impl.BpmnModelConstants;
import io.camunda.zeebe.model.bpmn.impl.ZeebeConstants;
import io.camunda.zeebe.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskSchedule;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder;
import org.camunda.bpm.model.xml.type.attribute.Attribute;

public class ZeebeTaskScheduleImpl extends BpmnModelElementInstanceImpl
    implements ZeebeTaskSchedule {

  private static Attribute<String> dueDateAttribute;
  private static Attribute<String> followUpDateAttribute;

  public ZeebeTaskScheduleImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ZeebeTaskSchedule.class, ZeebeConstants.ELEMENT_SCHEDULE_DEFINITION)
            .namespaceUri(BpmnModelConstants.ZEEBE_NS)
            .instanceProvider(ZeebeTaskScheduleImpl::new);

    dueDateAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_DUE_DATE)
            .namespace(BpmnModelConstants.ZEEBE_NS)
            .build();

    followUpDateAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_FOLLOW_UP_DATE)
            .namespace(BpmnModelConstants.ZEEBE_NS)
            .build();

    typeBuilder.build();
  }

  @Override
  public String getDueDate() {
    return dueDateAttribute.getValue(this);
  }

  @Override
  public void setDueDate(final String dueDate) {
    dueDateAttribute.setValue(this, dueDate);
  }

  @Override
  public String getFollowUpDate() {
    return followUpDateAttribute.getValue(this);
  }

  @Override
  public void setFollowUpDate(final String followUpDate) {
    followUpDateAttribute.setValue(this, followUpDate);
  }
}
