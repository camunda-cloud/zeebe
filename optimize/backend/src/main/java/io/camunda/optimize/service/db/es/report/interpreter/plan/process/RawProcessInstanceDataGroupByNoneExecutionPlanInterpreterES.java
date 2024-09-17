/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.interpreter.plan.process;

import static io.camunda.optimize.service.db.report.plan.process.ProcessExecutionPlan.PROCESS_RAW_PROCESS_INSTANCE_DATA_GROUP_BY_NONE;

import io.camunda.optimize.dto.optimize.query.report.CommandEvaluationResult;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.service.db.es.OptimizeElasticsearchClient;
import io.camunda.optimize.service.db.es.filter.ProcessQueryFilterEnhancerES;
import io.camunda.optimize.service.db.es.report.interpreter.groupby.process.ProcessGroupByInterpreterFacadeES;
import io.camunda.optimize.service.db.es.report.interpreter.view.process.ProcessViewInterpreterFacadeES;
import io.camunda.optimize.service.db.reader.ProcessDefinitionReader;
import io.camunda.optimize.service.db.report.ExecutionContext;
import io.camunda.optimize.service.db.report.interpreter.plan.process.RawProcessInstanceDataGroupByNoneExecutionPlanInterpreter;
import io.camunda.optimize.service.db.report.plan.process.ProcessExecutionPlan;
import io.camunda.optimize.service.util.configuration.condition.ElasticSearchCondition;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Conditional(ElasticSearchCondition.class)
public class RawProcessInstanceDataGroupByNoneExecutionPlanInterpreterES
    extends AbstractProcessExecutionPlanInterpreterES
    implements RawProcessInstanceDataGroupByNoneExecutionPlanInterpreter {
  @Getter private final ProcessGroupByInterpreterFacadeES groupByInterpreter;
  @Getter private final ProcessViewInterpreterFacadeES viewInterpreter;
  @Getter private final OptimizeElasticsearchClient esClient;
  @Getter private final ProcessDefinitionReader processDefinitionReader;
  @Getter private final ProcessQueryFilterEnhancerES queryFilterEnhancer;

  @Override
  public Set<ProcessExecutionPlan> getSupportedExecutionPlans() {
    return Set.of(PROCESS_RAW_PROCESS_INSTANCE_DATA_GROUP_BY_NONE);
  }

  @Override
  public CommandEvaluationResult<Object> interpret(
      ExecutionContext<ProcessReportDataDto, ProcessExecutionPlan> executionContext) {
    final CommandEvaluationResult<Object> commandResult = super.interpret(executionContext);
    addNewVariablesAndDtoFieldsToTableColumnConfig(executionContext, commandResult);
    return commandResult;
  }
}
