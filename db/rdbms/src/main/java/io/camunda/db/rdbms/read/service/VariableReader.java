/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.read.service;

import io.camunda.db.rdbms.read.domain.VariableModel;
import io.camunda.db.rdbms.sql.VariableMapper;
import io.camunda.db.rdbms.sql.VariableMapper.VariableFilter;
import java.util.List;

public class VariableReader {

  private final VariableMapper variableMapper;

  public VariableReader(final VariableMapper variableMapper) {
    this.variableMapper = variableMapper;
  }

  public VariableModel findOne(final Long key) {
    return variableMapper.findOne(key);
  }

  public List<VariableModel> findByProcessInstanceKey(final Long processInstanceKey) {
    return variableMapper.find(new VariableFilter(processInstanceKey));
  }
}
