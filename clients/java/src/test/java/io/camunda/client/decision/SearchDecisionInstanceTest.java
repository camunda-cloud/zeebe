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
package io.camunda.client.decision;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.api.search.response.DecisionDefinitionType;
import io.camunda.client.api.search.response.DecisionInstanceState;
import io.camunda.client.protocol.rest.DateTimeFilterProperty;
import io.camunda.client.protocol.rest.DecisionDefinitionTypeEnum;
import io.camunda.client.protocol.rest.DecisionInstanceFilterRequest;
import io.camunda.client.protocol.rest.DecisionInstanceSearchQueryRequest;
import io.camunda.client.protocol.rest.DecisionInstanceStateEnum;
import io.camunda.client.protocol.rest.LongFilterProperty;
import io.camunda.client.protocol.rest.SortOrderEnum;
import io.camunda.client.util.ClientRestTest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SearchDecisionInstanceTest extends ClientRestTest {

  @Test
  void shouldSearchDecisionInstance() {
    // when
    client.newDecisionInstanceQuery().send().join();

    // then
    final DecisionInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(DecisionInstanceSearchQueryRequest.class);
    assertThat(request.getFilter()).isNull();
  }

  @Test
  void shouldSearchDecisionInstanceWithFullFilters() {
    // when
    client
        .newDecisionInstanceQuery()
        .filter(
            f ->
                f.decisionInstanceKey(1L)
                    .state(DecisionInstanceState.FAILED)
                    .evaluationFailure("ef")
                    .decisionDefinitionType(DecisionDefinitionType.DECISION_TABLE)
                    .processDefinitionKey(2L)
                    .processInstanceKey(3L)
                    .decisionDefinitionKey(4L)
                    .decisionDefinitionId("ddi")
                    .decisionDefinitionName("ddm")
                    .decisionDefinitionVersion(5)
                    .tenantId("t"))
        .send()
        .join();

    // then
    final DecisionInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(DecisionInstanceSearchQueryRequest.class);
    assertThat(request.getFilter().getDecisionInstanceKey().get$Eq()).isEqualTo(1L);
    assertThat(request.getFilter().getState()).isEqualTo(DecisionInstanceStateEnum.FAILED);
    assertThat(request.getFilter().getEvaluationFailure().get$Eq()).isEqualTo("ef");
    assertThat(request.getFilter().getDecisionDefinitionType())
        .isEqualTo(DecisionDefinitionTypeEnum.DECISION_TABLE);
    assertThat(request.getFilter().getProcessDefinitionKey().get$Eq()).isEqualTo(2L);
    assertThat(request.getFilter().getProcessInstanceKey().get$Eq()).isEqualTo(3L);
    assertThat(request.getFilter().getDecisionDefinitionKey().get$Eq()).isEqualTo(4L);
    assertThat(request.getFilter().getDecisionDefinitionId().get$Eq()).isEqualTo("ddi");
    assertThat(request.getFilter().getDecisionDefinitionName().get$Eq()).isEqualTo("ddm");
    assertThat(request.getFilter().getDecisionDefinitionVersion().get$Eq()).isEqualTo(5);
    assertThat(request.getFilter().getTenantId().get$Eq()).isEqualTo("t");
  }

  @Test
  void shouldSearchDecisionInstanceByDecisionDefinitionKeyLongProperty() {
    // when
    client
        .newDecisionInstanceQuery()
        .filter(f -> f.decisionDefinitionKey(b -> b.in(1L, 10L)))
        .send()
        .join();

    // then
    final DecisionInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(DecisionInstanceSearchQueryRequest.class);
    final DecisionInstanceFilterRequest filter = request.getFilter();
    assertThat(filter).isNotNull();
    final LongFilterProperty decisionDefinitionKey = filter.getDecisionDefinitionKey();
    assertThat(decisionDefinitionKey).isNotNull();
    assertThat(decisionDefinitionKey.get$In()).isEqualTo(Arrays.asList(1L, 10L));
  }

  @Test
  void shouldSearchDecisionInstanceByEvaluationDateDateTimeProperty() {
    // when
    final OffsetDateTime now = OffsetDateTime.now();
    client.newDecisionInstanceQuery().filter(f -> f.evaluationDate(b -> b.neq(now))).send().join();

    // then
    final DecisionInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(DecisionInstanceSearchQueryRequest.class);
    final DecisionInstanceFilterRequest filter = request.getFilter();
    assertThat(filter).isNotNull();
    final DateTimeFilterProperty evaluationDate = filter.getEvaluationDate();
    assertThat(evaluationDate).isNotNull();
    assertThat(evaluationDate.get$Neq()).isEqualTo(now.toString());
  }

  @Test
  void shouldSearchDecisionInstanceWithFullSorting() {
    // when
    client
        .newDecisionInstanceQuery()
        .sort(
            s ->
                s.decisionDefinitionKey()
                    .asc()
                    .decisionDefinitionId()
                    .asc()
                    .decisionDefinitionName()
                    .desc()
                    .processInstanceId()
                    .asc()
                    .evaluationDate()
                    .asc()
                    .evaluationFailure()
                    .asc()
                    .decisionDefinitionVersion()
                    .asc()
                    .state()
                    .asc()
                    .processDefinitionKey()
                    .asc()
                    .processInstanceId()
                    .asc()
                    .decisionDefinitionType()
                    .asc()
                    .tenantId()
                    .asc())
        .send()
        .join();

    // then
    final DecisionInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(DecisionInstanceSearchQueryRequest.class);
    assertThat(request.getSort().size()).isEqualTo(12);
    assertThat(request.getSort().get(0).getField()).isEqualTo("decisionDefinitionKey");
    assertThat(request.getSort().get(0).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(1).getField()).isEqualTo("decisionDefinitionId");
    assertThat(request.getSort().get(1).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(2).getField()).isEqualTo("decisionDefinitionName");
    assertThat(request.getSort().get(2).getOrder()).isEqualTo(SortOrderEnum.DESC);
    assertThat(request.getSort().get(3).getField()).isEqualTo("processInstanceId");
    assertThat(request.getSort().get(3).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(4).getField()).isEqualTo("evaluationDate");
    assertThat(request.getSort().get(4).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(5).getField()).isEqualTo("evaluationFailure");
    assertThat(request.getSort().get(5).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(6).getField()).isEqualTo("decisionDefinitionVersion");
    assertThat(request.getSort().get(6).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(7).getField()).isEqualTo("state");
    assertThat(request.getSort().get(7).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(8).getField()).isEqualTo("processDefinitionKey");
    assertThat(request.getSort().get(8).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(9).getField()).isEqualTo("processInstanceId");
    assertThat(request.getSort().get(9).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(10).getField()).isEqualTo("decisionDefinitionType");
    assertThat(request.getSort().get(10).getOrder()).isEqualTo(SortOrderEnum.ASC);
    assertThat(request.getSort().get(11).getField()).isEqualTo("tenantId");
    assertThat(request.getSort().get(11).getOrder()).isEqualTo(SortOrderEnum.ASC);
  }
}
