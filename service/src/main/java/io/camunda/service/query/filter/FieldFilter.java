/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service.query.filter;

public class FieldFilter<T> {
  private final Operator operator; // e.g., $eq, $gt, $lt
  private final T value;

  public FieldFilter(final Operator operator, final T value) {
    this.operator = operator;
    this.value = value;
  }

  public Operator getOperator() {
    return operator;
  }

  public T getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "FieldFilter{" + "operator='" + operator + '\'' + ", value=" + value + '}';
  }
}
