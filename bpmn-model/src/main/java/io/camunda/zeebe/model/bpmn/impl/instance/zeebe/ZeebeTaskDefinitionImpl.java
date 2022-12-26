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
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskDefinition;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder;
import org.camunda.bpm.model.xml.type.attribute.Attribute;

public class ZeebeTaskDefinitionImpl extends BpmnModelElementInstanceImpl
    implements ZeebeTaskDefinition {

  protected static Attribute<String> typeAttribute;
  protected static Attribute<String> retriesAttribute;
  protected static Attribute<String> retriesBackoffAttribute;

  public ZeebeTaskDefinitionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getType() {
    return typeAttribute.getValue(this);
  }

  @Override
  public void setType(final String type) {
    typeAttribute.setValue(this, type);
  }

  @Override
  public String getRetries() {
    return retriesAttribute.getValue(this);
  }

  @Override
  public void setRetries(final String retries) {
    retriesAttribute.setValue(this, retries);
  }

  @Override
  public String getRetryBackoff() {
    return retriesBackoffAttribute.getValue(this);
  }

  @Override
  public void setRetryBackoff(final String retryBackoff) {
    retriesBackoffAttribute.setValue(this, retryBackoff);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ZeebeTaskDefinition.class, ZeebeConstants.ELEMENT_TASK_DEFINITION)
            .namespaceUri(BpmnModelConstants.ZEEBE_NS)
            .instanceProvider(ZeebeTaskDefinitionImpl::new);

    typeAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_TYPE)
            .namespace(BpmnModelConstants.ZEEBE_NS)
            .required()
            .build();

    retriesAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_RETRIES)
            .defaultValue(ZeebeTaskDefinition.DEFAULT_RETRIES)
            .namespace(BpmnModelConstants.ZEEBE_NS)
            .build();

    retriesBackoffAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_RETRY_BACKOFF)
            .namespace(BpmnModelConstants.ZEEBE_NS)
            .build();

    typeBuilder.build();
  }
}
