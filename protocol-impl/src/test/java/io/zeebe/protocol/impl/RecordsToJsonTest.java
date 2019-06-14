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
package io.zeebe.protocol.impl;

import static io.zeebe.util.buffer.BufferUtil.wrapArray;
import static io.zeebe.util.buffer.BufferUtil.wrapString;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.exporter.api.record.value.deployment.ResourceType;
import io.zeebe.protocol.BpmnElementType;
import io.zeebe.protocol.ErrorType;
import io.zeebe.protocol.VariableDocumentUpdateSemantic;
import io.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.zeebe.protocol.impl.record.UnifiedRecordValue;
import io.zeebe.protocol.impl.record.value.deployment.DeploymentRecord;
import io.zeebe.protocol.impl.record.value.error.ErrorRecord;
import io.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import io.zeebe.protocol.impl.record.value.job.JobBatchRecord;
import io.zeebe.protocol.impl.record.value.job.JobRecord;
import io.zeebe.protocol.impl.record.value.message.MessageRecord;
import io.zeebe.protocol.impl.record.value.message.MessageStartEventSubscriptionRecord;
import io.zeebe.protocol.impl.record.value.message.MessageSubscriptionRecord;
import io.zeebe.protocol.impl.record.value.message.WorkflowInstanceSubscriptionRecord;
import io.zeebe.protocol.impl.record.value.timer.TimerRecord;
import io.zeebe.protocol.impl.record.value.variable.VariableDocumentRecord;
import io.zeebe.protocol.impl.record.value.variable.VariableRecord;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceCreationRecord;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import jdk.nashorn.internal.parser.JSONParser;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RecordsToJsonTest {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(Feature.ALLOW_SINGLE_QUOTES, true);

  private static final String VARIABLES_JSON = "{'foo':'bar'}";
  private static final DirectBuffer VARIABLES_MSGPACK =
      new UnsafeBuffer(MsgPackConverter.convertToMsgPack(VARIABLES_JSON));

  private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException("test");

  private static final String STACK_TRACE;

  static {
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter pw = new PrintWriter(stringWriter);
    RUNTIME_EXCEPTION.printStackTrace(pw);

    STACK_TRACE = stringWriter.toString();
  }

  @Parameters(name = "{index}: {0}")
  public static Object[][] records() {
    final Object[][] contents = {
      /////////////////////////////////////////////////////////////////////////////////////////////
      //////////////////////////////////// DeploymentRecord ///////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      new Object[] {
        "DeploymentRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final String resourceName = "resource";
              final ResourceType resourceType = ResourceType.BPMN_XML;
              final DirectBuffer resource = wrapString("contents");
              final String bpmnProcessId = "testProcess";
              final long workflowKey = 123;
              final int workflowVersion = 12;
              final DeploymentRecord record = new DeploymentRecord();
              record
                  .resources()
                  .add()
                  .setResourceName(wrapString(resourceName))
                  .setResourceType(resourceType)
                  .setResource(resource);
              record
                  .workflows()
                  .add()
                  .setBpmnProcessId(wrapString(bpmnProcessId))
                  .setKey(workflowKey)
                  .setResourceName(wrapString(resourceName))
                  .setVersion(workflowVersion);
              return record;
            },
        "{'resources':[{'resourceType':'BPMN_XML','resourceName':'resource','resource':'Y29udGVudHM='}],'deployedWorkflows':[{'bpmnProcessId':'testProcess','version':12,'workflowKey':123,'resourceName':'resource'}]}"
      },
      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////////// ErrorRecord ///////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      new Object[] {
        "ErrorRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final ErrorRecord record = new ErrorRecord();
              record.initErrorRecord(RUNTIME_EXCEPTION, 123);
              record.setWorkflowInstanceKey(4321);
              return record;
            },
        "{'exceptionMessage':'test','stacktrace':"
            + JSONParser.quote(STACK_TRACE)
            + ",'errorEventPosition':123,'workflowInstanceKey':4321}"
      },
      /////////////////////////////////////////////////////////////////////////////////////////////
      //////////////////////////////////// IncidentRecord /////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      new Object[] {
        "IncidentRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final long elementInstanceKey = 34;
              final long workflowKey = 134;
              final long workflowInstanceKey = 10;
              final String elementId = "activity";
              final String bpmnProcessId = "process";
              final String errorMessage = "error";
              final ErrorType errorType = ErrorType.IO_MAPPING_ERROR;
              final long jobKey = 123;

              final IncidentRecord record =
                  new IncidentRecord()
                      .setElementInstanceKey(elementInstanceKey)
                      .setWorkflowKey(workflowKey)
                      .setWorkflowInstanceKey(workflowInstanceKey)
                      .setElementId(wrapString(elementId))
                      .setBpmnProcessId(wrapString(bpmnProcessId))
                      .setErrorMessage(errorMessage)
                      .setErrorType(errorType)
                      .setJobKey(jobKey)
                      .setVariableScopeKey(elementInstanceKey);
              return record;
            },
        "{'errorType':'IO_MAPPING_ERROR','errorMessage':'error','bpmnProcessId':'process','workflowKey':134,'workflowInstanceKey':10,'elementId':'activity','elementInstanceKey':34,'jobKey':123,'variableScopeKey':34}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////// JobBatchRecord ////////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "JobBatchRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final int amount = 1;
              final long timeout = 2L;
              final String type = "type";
              final String worker = "worker";

              final JobBatchRecord record =
                  new JobBatchRecord()
                      .setMaxJobsToActivate(amount)
                      .setTimeout(timeout)
                      .setType(type)
                      .setWorker(worker)
                      .setTruncated(true);

              record.jobKeys().add().setValue(3L);
              final JobRecord jobRecord = record.jobs().add();

              final String bpmnProcessId = "test-process";
              final int workflowKey = 13;
              final int workflowDefinitionVersion = 12;
              final int workflowInstanceKey = 1234;
              final String activityId = "activity";
              final int activityInstanceKey = 123;

              jobRecord
                  .setWorker(wrapString(worker))
                  .setType(wrapString(type))
                  .setVariables(VARIABLES_MSGPACK)
                  .setRetries(3)
                  .setErrorMessage("failed message")
                  .setDeadline(1000L);

              jobRecord
                  .getJobHeaders()
                  .setBpmnProcessId(wrapString(bpmnProcessId))
                  .setWorkflowKey(workflowKey)
                  .setWorkflowDefinitionVersion(workflowDefinitionVersion)
                  .setWorkflowInstanceKey(workflowInstanceKey)
                  .setElementId(wrapString(activityId))
                  .setElementInstanceKey(activityInstanceKey);

              return record;
            },
        "{'maxJobsToActivate':1,'type':'type','worker':'worker','truncated':true,'jobKeys':[3],'jobs':[{'headers':{'bpmnProcessId':'test-process','workflowKey':13,'workflowDefinitionVersion':12,'workflowInstanceKey':1234,'elementId':'activity','elementInstanceKey':123},'type':'type','worker':'worker','variables':'{\"foo\":\"bar\"}','retries':3,'errorMessage':'failed message','customHeaders':{},'deadline':1000}],'timeout':2}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////////// JobRecord /////////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "JobRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final String worker = "myWorker";
              final String type = "myType";
              final int retries = 12;
              final int deadline = 13;

              final String bpmnProcessId = "test-process";
              final int workflowKey = 13;
              final int workflowDefinitionVersion = 12;
              final int workflowInstanceKey = 1234;
              final String elementId = "activity";
              final int activityInstanceKey = 123;

              final Map<String, Object> customHeaders =
                  Collections.singletonMap("workerVersion", 42);

              final JobRecord record =
                  new JobRecord()
                      .setWorker(wrapString(worker))
                      .setType(wrapString(type))
                      .setVariables(VARIABLES_MSGPACK)
                      .setRetries(retries)
                      .setDeadline(deadline)
                      .setErrorMessage("failed message");
              record
                  .getJobHeaders()
                  .setBpmnProcessId(wrapString(bpmnProcessId))
                  .setWorkflowKey(workflowKey)
                  .setWorkflowDefinitionVersion(workflowDefinitionVersion)
                  .setWorkflowInstanceKey(workflowInstanceKey)
                  .setElementId(wrapString(elementId))
                  .setElementInstanceKey(activityInstanceKey);

              record.setCustomHeaders(wrapArray(MsgPackConverter.convertToMsgPack(customHeaders)));
              return record;
            },
        "{'headers':{'bpmnProcessId':'test-process','workflowKey':13,'workflowDefinitionVersion':12,'workflowInstanceKey':1234,'elementId':'activity','elementInstanceKey':123},'worker':'myWorker','type':'myType','variables':'{\"foo\":\"bar\"}','retries':12,'errorMessage':'failed message','customHeaders':{'workerVersion':42},'deadline':13}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////// MessageRecord /////////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "MessageRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final String correlationKey = "test-key";
              final String messageName = "test-message";
              final long timeToLive = 12;
              final String messageId = "test-id";

              final MessageRecord record =
                  new MessageRecord()
                      .setCorrelationKey(wrapString(correlationKey))
                      .setName(wrapString(messageName))
                      .setVariables(VARIABLES_MSGPACK)
                      .setTimeToLive(timeToLive)
                      .setMessageId(wrapString(messageId));
              return record;
            },
        "{'timeToLive':12,'correlationKey':'test-key','variables':'{\"foo\":\"bar\"}','messageId':'test-id','name':'test-message'}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////// MessageStartEventSubscriptionRecord ///////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "MessageStartEventSubscriptionRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final String messageName = "name";
              final String startEventId = "startEvent";
              final int workflowKey = 22334;

              final MessageStartEventSubscriptionRecord record =
                  new MessageStartEventSubscriptionRecord()
                      .setMessageName(wrapString(messageName))
                      .setStartEventId(wrapString(startEventId))
                      .setWorkflowKey(workflowKey);

              return record;
            },
        "{'workflowKey':22334,'messageName':'name','startEventId':'startEvent'}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////// MessageSubscriptionRecord /////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "MessageSubscriptionRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final long activityInstanceKey = 1L;
              final String messageName = "name";
              final long workflowInstanceKey = 1L;
              final String correlationKey = "key";
              final long messageKey = 1L;

              final MessageSubscriptionRecord record =
                  new MessageSubscriptionRecord()
                      .setElementInstanceKey(activityInstanceKey)
                      .setMessageKey(messageKey)
                      .setMessageName(wrapString(messageName))
                      .setWorkflowInstanceKey(workflowInstanceKey)
                      .setCorrelationKey(wrapString(correlationKey));
              return record;
            },
        "{'workflowInstanceKey':1,'elementInstanceKey':1,'messageName':'name','correlationKey':'key'}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      //////////////////////////// WorkflowInstanceSubscriptionRecord /////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "WorkflowInstanceSubscriptionRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final long activityInstanceKey = 123;
              final String messageName = "test-message";
              final int subscriptionPartitionId = 2;
              final int messageKey = 3;
              final long workflowInstanceKey = 1345;

              final WorkflowInstanceSubscriptionRecord record =
                  new WorkflowInstanceSubscriptionRecord()
                      .setElementInstanceKey(activityInstanceKey)
                      .setMessageName(wrapString(messageName))
                      .setMessageKey(messageKey)
                      .setSubscriptionPartitionId(subscriptionPartitionId)
                      .setWorkflowInstanceKey(workflowInstanceKey)
                      .setVariables(VARIABLES_MSGPACK);

              return record;
            },
        "{'elementInstanceKey':123,'messageName':'test-message','workflowInstanceKey':1345,'variables':'{\"foo\":\"bar\"}'}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      /////////////////////////////////// TimerRecord /////////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "TimerRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final int workflowKey = 13;
              final int workflowInstanceKey = 1234;
              final int dueDate = 1234;
              final int elementInstanceKey = 567;
              final String handlerNodeId = "node1";
              final int repetitions = 3;

              final TimerRecord record =
                  new TimerRecord()
                      .setDueDate(dueDate)
                      .setElementInstanceKey(elementInstanceKey)
                      .setHandlerNodeId(wrapString(handlerNodeId))
                      .setRepetitions(repetitions)
                      .setWorkflowInstanceKey(workflowInstanceKey)
                      .setWorkflowKey(workflowKey);

              return record;
            },
        "{'elementInstanceKey':567,'workflowInstanceKey':1234,'dueDate':1234,'handlerFlowNodeId':'node1','repetitions':3,'workflowKey':13}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////// VariableRecord ////////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "VariableRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final String name = "x";
              final String value = "1";
              final long scopeKey = 3;
              final long workflowInstanceKey = 2;
              final long workflowKey = 4;

              final VariableRecord record =
                  new VariableRecord()
                      .setName(wrapString(name))
                      .setValue(new UnsafeBuffer(MsgPackConverter.convertToMsgPack(value)))
                      .setScopeKey(scopeKey)
                      .setWorkflowInstanceKey(workflowInstanceKey)
                      .setWorkflowKey(workflowKey);
              return record;
            },
        "{'scopeKey':3,'workflowInstanceKey':2,'workflowKey':4,'name':'x','value':'1'}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////// VariableDocumentRecord ////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "VariableDocumentRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final String value = "{'foo':1}";
              final long scopeKey = 3;

              final VariableDocumentRecord record =
                  new VariableDocumentRecord()
                      .setUpdateSemantics(VariableDocumentUpdateSemantic.LOCAL)
                      .setDocument(new UnsafeBuffer(MsgPackConverter.convertToMsgPack(value)))
                      .setScopeKey(scopeKey);

              return record;
            },
        "{'updateSemantics':'LOCAL','document':{'foo':1},'scopeKey':3}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////// WorkflowInstanceCreationRecord ////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "WorkflowInstanceCreationRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final String processId = "process";
              final long key = 1L;
              final int version = 1;
              final long instanceKey = 2L;

              final WorkflowInstanceCreationRecord record =
                  new WorkflowInstanceCreationRecord()
                      .setBpmnProcessId(processId)
                      .setKey(key)
                      .setVersion(version)
                      .setVariables(
                          new UnsafeBuffer(
                              MsgPackConverter.convertToMsgPack("{'foo':'bar','baz':'boz'}")))
                      .setInstanceKey(instanceKey);

              return record;
            },
        "{'variables':{'foo':'bar','baz':'boz'},'bpmnProcessId':'process','key':1,'version':1,'instanceKey':2}"
      },

      /////////////////////////////////////////////////////////////////////////////////////////////
      ///////////////////////////////// WorkflowInstanceRecord ////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////////////////////
      {
        "WorkflowInstanceRecord",
        (Supplier<UnifiedRecordValue>)
            () -> {
              final String bpmnProcessId = "test-process";
              final int workflowKey = 13;
              final int version = 12;
              final int workflowInstanceKey = 1234;
              final String elementId = "activity";
              final int flowScopeKey = 123;
              final BpmnElementType bpmnElementType = BpmnElementType.SERVICE_TASK;

              final WorkflowInstanceRecord record =
                  new WorkflowInstanceRecord()
                      .setElementId(elementId)
                      .setBpmnElementType(bpmnElementType)
                      .setBpmnProcessId(wrapString(bpmnProcessId))
                      .setVersion(version)
                      .setWorkflowKey(workflowKey)
                      .setWorkflowInstanceKey(workflowInstanceKey)
                      .setFlowScopeKey(flowScopeKey);
              return record;
            },
        "{'bpmnProcessId':'test-process','version':12,'workflowKey':13,'workflowInstanceKey':1234,'elementId':'activity','flowScopeKey':123,'bpmnElementType':'SERVICE_TASK'}"
      },
    };
    return contents;
  }

  @Parameter public String testName;

  @Parameter(1)
  public Supplier<UnifiedRecordValue> actualRecordSupplier;

  @Parameter(2)
  public String expectedJson;

  @Test
  public void shouldConvertDeploymentRecordToJson() {
    // given

    // when
    final String json = actualRecordSupplier.get().toJson();

    // then
    assertThat(json(json)).isEqualTo(json(expectedJson));
  }

  private static JsonNode json(String jsonString) {
    try {
      return OBJECT_MAPPER.readTree(jsonString);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
