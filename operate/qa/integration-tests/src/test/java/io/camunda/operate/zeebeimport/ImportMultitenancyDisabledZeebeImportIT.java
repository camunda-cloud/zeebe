/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.zeebeimport;

import static io.camunda.operate.qa.util.RestAPITestUtil.createGetAllProcessInstancesRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.camunda.operate.entities.FlowNodeInstanceEntity;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.util.OperateZeebeAbstractIT;
import io.camunda.operate.util.TestApplication;
import io.camunda.operate.util.ZeebeTestUtil;
import io.camunda.operate.webapp.reader.ListViewReader;
import io.camunda.operate.webapp.rest.dto.listview.ListViewProcessInstanceDto;
import io.camunda.operate.webapp.rest.dto.listview.ListViewRequestDto;
import io.camunda.operate.webapp.rest.dto.listview.ListViewResponseDto;
import io.camunda.operate.webapp.security.tenant.TenantService;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {TestApplication.class},
    properties = {
      OperateProperties.PREFIX + ".importer.startLoadingDataOnStartup = false",
      OperateProperties.PREFIX + ".archiver.rolloverEnabled = false",
      "spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER",
      OperateProperties.PREFIX + ".multiTenancy.enabled = false"
    })
public class ImportMultitenancyDisabledZeebeImportIT extends OperateZeebeAbstractIT {

  @Autowired private ListViewReader listViewReader;

  private final String defaultTenantId = "<default>";

  @Test
  public void testDefaultTenantIsAssignedAndImported() {
    doReturn(TenantService.AuthenticatedTenants.assignedTenants(List.of(defaultTenantId)))
        .when(tenantService)
        .getAuthenticatedTenants();

    // having
    final String processId = "demoProcess";
    final Long processDefinitionKey = deployProcess("demoProcess_v_1.bpmn");

    // when
    final Long processInstanceKey =
        ZeebeTestUtil.startProcessInstance(zeebeClient, processId, "{\"a\": \"b\"}");
    searchTestRule.processAllRecordsAndWait(flowNodeIsActiveCheck, processInstanceKey, "taskA");

    // then
    // assert process instance
    final ListViewProcessInstanceDto pi = getSingleProcessInstanceForListView();
    assertThat(pi.getTenantId()).isEqualTo(defaultTenantId);
    // assert flow node instances
    final List<FlowNodeInstanceEntity> allFlowNodeInstances =
        tester.getAllFlowNodeInstances(processInstanceKey);
    assertThat(allFlowNodeInstances).extracting("tenantId").containsOnly(defaultTenantId);
    // assert variables

  }

  private ListViewProcessInstanceDto getSingleProcessInstanceForListView(
      final ListViewRequestDto request) {
    final ListViewResponseDto listViewResponse = listViewReader.queryProcessInstances(request);
    assertThat(listViewResponse.getTotalCount()).isEqualTo(1);
    assertThat(listViewResponse.getProcessInstances()).hasSize(1);
    return listViewResponse.getProcessInstances().get(0);
  }

  private ListViewProcessInstanceDto getSingleProcessInstanceForListView() {
    return getSingleProcessInstanceForListView(createGetAllProcessInstancesRequest());
  }
}
