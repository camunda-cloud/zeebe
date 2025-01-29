/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {t} from 'translation';

export function formatLink(id: string, type: string) {
  return `${type}/${id}/`;
}

export function formatType(entityType: string, reportType: string) {
  switch (entityType) {
    case 'collection':
      return t('common.collection.label');
    case 'dashboard':
      return t('dashboard.label');
    case 'report':
      if (reportType === 'process') {
        return t('home.types.process');
      }
      if (reportType === 'decision') {
        return t('home.types.decision');
      }
      return t('report.label');
    default:
      return t('home.types.unknown');
  }
}

export function formatSubEntities({dashboard, report}: {dashboard?: number; report?: number}) {
  let string = '';
  if (dashboard) {
    string += dashboard + ' ';
    string += t('dashboard.' + (dashboard > 1 ? 'label-plural' : 'label'));
    if (report) {
      string += ', ';
    }
  }
  if (report) {
    string += report + ' ';
    string += t('report.' + (report > 1 ? 'label-plural' : 'label'));
  }

  return string;
}

export function formatRole(role: string) {
  return t('home.roles.' + role);
}
