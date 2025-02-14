/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.adapter.juel;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.el.JuelExpression;
import org.camunda.bpm.engine.impl.el.JuelExpressionManager;
import org.camunda.bpm.impl.juel.jakarta.el.ELContext;
import org.camunda.bpm.impl.juel.jakarta.el.ELException;
import org.camunda.bpm.impl.juel.jakarta.el.MethodNotFoundException;
import org.camunda.bpm.impl.juel.jakarta.el.PropertyNotFoundException;
import org.camunda.bpm.impl.juel.jakarta.el.ValueExpression;

public class EnginelessJuelExpression extends JuelExpression {

  public EnginelessJuelExpression(
      ValueExpression valueExpression,
      JuelExpressionManager expressionManager,
      String expressionText) {
    super(valueExpression, expressionManager, expressionText);
  }

  @Override
  public Object getValue(VariableScope variableScope) {
    final ELContext elContext = expressionManager.getElContext(variableScope);
    try {
      return valueExpression.getValue(elContext);
    } catch (PropertyNotFoundException pnfe) {
      throw new ProcessEngineException(
          "Unknown property used in expression: "
              + expressionText
              + ". Cause: "
              + pnfe.getMessage(),
          pnfe);
    } catch (MethodNotFoundException mnfe) {
      throw new ProcessEngineException(
          "Unknown method used in expression: " + expressionText + ". Cause: " + mnfe.getMessage(),
          mnfe);
    } catch (ELException ele) {
      throw new ProcessEngineException(
          "EL Error while evaluating expression: "
              + expressionText
              + ". Cause: "
              + ele.getMessage(),
          ele);
    } catch (Exception e) {
      throw new ProcessEngineException(
          "Error while evaluating expression: " + expressionText + ". Cause: " + e.getMessage(), e);
    }
  }
}
