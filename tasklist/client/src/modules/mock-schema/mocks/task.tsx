/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import type {Task} from 'modules/types';
import {currentUser} from 'modules/mock-schema/mocks/current-user';
import {DEFAULT_TENANT_ID} from 'modules/constants/multiTenancy';

function* getUniqueId(): Generator<number> {
  let id = 0;

  while (true) {
    yield id++;
  }
}

const unassignedTask = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: null,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: null,
  priority: 50,
  taskState: 'CREATED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: null,
  formVersion: null,
  formId: null,
  isFormEmbedded: null,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: ['accounting candidate'],
  candidateUsers: ['jane candidate'],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

const unassignedTaskWithForm = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: null,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: null,
  priority: 50,
  taskState: 'CREATED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: 'camunda-forms:bpmn:form-0',
  formVersion: null,
  formId: null,
  isFormEmbedded: true,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: [],
  candidateUsers: [],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

const unassignedTaskWithFormDeployed = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: null,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: null,
  priority: 50,
  taskState: 'CREATED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: '234234432',
  formVersion: 2,
  formId: 'form-deployed-id',
  isFormEmbedded: false,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: [],
  candidateUsers: [],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

const completedTask = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: currentUser.userId,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: '2025-01-01T00:00:00.000Z',
  priority: 50,
  taskState: 'COMPLETED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: null,
  formVersion: null,
  formId: null,
  isFormEmbedded: null,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: [],
  candidateUsers: [],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

const completedTaskWithForm = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: currentUser.userId,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: null,
  priority: 50,
  taskState: 'COMPLETED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: 'camunda-forms:bpmn:form-0',
  formId: null,
  formVersion: null,
  isFormEmbedded: true,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: [],
  candidateUsers: [],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

const completedTaskWithFormDeployed = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: currentUser.userId,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: null,
  priority: 50,
  taskState: 'COMPLETED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: '234324324',
  formId: 'form-deployed-id',
  formVersion: 1,
  isFormEmbedded: false,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: [],
  candidateUsers: [],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

const assignedTask = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: currentUser.userId,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: null,
  priority: 50,
  taskState: 'CREATED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: null,
  formId: null,
  formVersion: null,
  isFormEmbedded: null,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: [],
  candidateUsers: [],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

const assignedTaskWithForm = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: currentUser.userId,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: null,
  priority: 50,
  taskState: 'CREATED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: 'camunda-forms:bpmn:form-0',
  formId: null,
  formVersion: null,
  isFormEmbedded: true,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: [],
  candidateUsers: [],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

const assignedTaskWithFormDeployed = (
  id: string = getUniqueId().next().value.toString(),
): Task => ({
  id,
  name: 'My Task',
  processName: 'Nice Process',
  assignee: currentUser.userId,
  creationDate: '2024-01-01T00:00:00.000Z',
  completionDate: null,
  priority: 50,
  taskState: 'CREATED',
  isFirst: false,
  sortValues: ['1', '2'],
  formKey: '324234234342',
  formId: 'form-deployed-id',
  formVersion: 1,
  isFormEmbedded: false,
  taskDefinitionId: 'task-0',
  processDefinitionKey: 'process',
  processInstanceKey: '123',
  followUpDate: null,
  dueDate: null,
  candidateGroups: [],
  candidateUsers: [],
  tenantId: DEFAULT_TENANT_ID,
  context: 'My Task',
  risk: 'LOW',
});

export {
  unassignedTask,
  completedTask,
  assignedTask,
  unassignedTaskWithForm,
  unassignedTaskWithFormDeployed,
  assignedTaskWithForm,
  assignedTaskWithFormDeployed,
  completedTaskWithForm,
  completedTaskWithFormDeployed,
};
