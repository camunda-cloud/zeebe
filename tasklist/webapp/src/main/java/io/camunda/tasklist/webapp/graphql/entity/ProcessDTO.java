/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.webapp.graphql.entity;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import io.camunda.tasklist.entities.ProcessEntity;
import io.camunda.tasklist.util.CollectionUtil;

public class ProcessDTO {

  @GraphQLField @GraphQLNonNull private String id;

  @GraphQLField private String name;

  @GraphQLField private String processDefinitionId;

  private String[] sortValues;

  private boolean startedByForm;

  private String formKey;

  private String formId;

  private Boolean isFormEmbedded;

  @GraphQLField private Integer version;

  public static ProcessDTO createFrom(ProcessEntity processEntity) {
    return createFrom(processEntity, null);
  }

  public static ProcessDTO createFrom(ProcessEntity processEntity, Object[] sortValues) {
    final ProcessDTO processDTO =
        new ProcessDTO()
            .setId(processEntity.getId())
            .setName(processEntity.getName())
            .setProcessDefinitionId(processEntity.getBpmnProcessId())
            .setVersion(processEntity.getVersion())
            .setStartedByForm(processEntity.isStartedByForm())
            .setFormKey(processEntity.getFormKey())
            .setFormId(processEntity.getFormId())
            .setFormEmbedded(processEntity.getIsFormEmbedded());

    if (sortValues != null) {
      processDTO.setSortValues(CollectionUtil.toArrayOfStrings(sortValues));
    }
    return processDTO;
  }

  public String getId() {
    return id;
  }

  public ProcessDTO setId(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public ProcessDTO setName(String name) {
    this.name = name;
    return this;
  }

  public String[] getSortValues() {
    return sortValues;
  }

  public ProcessDTO setSortValues(String[] sortValues) {
    this.sortValues = sortValues;
    return this;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public ProcessDTO setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  public Integer getVersion() {
    return version;
  }

  public ProcessDTO setVersion(Integer version) {
    this.version = version;
    return this;
  }

  public boolean isStartedByForm() {
    return startedByForm;
  }

  public ProcessDTO setStartedByForm(boolean startedByForm) {
    this.startedByForm = startedByForm;
    return this;
  }

  public String getFormKey() {
    return formKey;
  }

  public ProcessDTO setFormKey(String formKey) {
    this.formKey = formKey;
    return this;
  }

  public Boolean getFormEmbedded() {
    return isFormEmbedded;
  }

  public ProcessDTO setFormEmbedded(Boolean formEmbedded) {
    isFormEmbedded = formEmbedded;
    return this;
  }

  public String getFormId() {
    return formId;
  }

  public ProcessDTO setFormId(String formId) {
    this.formId = formId;
    return this;
  }
}
