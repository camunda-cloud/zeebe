/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {waitFor} from 'modules/testing-library';
import {mockDecisionInstances} from 'modules/mocks/mockDecisionInstances';
import {decisionInstancesStore} from './decisionInstances';
import {mockFetchDecisionInstances} from 'modules/mocks/api/decisionInstances/fetchDecisionInstances';

describe('decisionInstancesStore', () => {
  afterEach(() => {
    decisionInstancesStore.reset();
  });

  it('should get decision instances', async () => {
    mockFetchDecisionInstances().withSuccess(mockDecisionInstances);

    expect(decisionInstancesStore.state.status).toBe('initial');

    decisionInstancesStore.fetchDecisionInstancesFromFilters();

    await waitFor(() =>
      expect(decisionInstancesStore.state.status).toBe('fetched'),
    );
    expect(decisionInstancesStore.state.decisionInstances).toEqual(
      mockDecisionInstances.decisionInstances,
    );
  });

  it('should fail when getting decision instances', async () => {
    mockFetchDecisionInstances().withServerError();

    expect(decisionInstancesStore.state.status).toBe('initial');

    decisionInstancesStore.fetchDecisionInstancesFromFilters();

    await waitFor(() =>
      expect(decisionInstancesStore.state.status).toBe('error'),
    );
  });
});
