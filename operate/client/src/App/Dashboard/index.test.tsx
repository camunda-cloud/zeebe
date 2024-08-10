/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import React from 'react';
import {render, screen} from 'modules/testing-library';
import {MemoryRouter} from 'react-router-dom';
import {PAGE_TITLE} from 'modules/constants';
import {statisticsStore} from 'modules/stores/statistics';
import {Dashboard} from './index';
import {mockIncidentsByError} from './IncidentsByError/index.setup';
import {mockWithSingleVersion} from './InstancesByProcess/index.setup';
import {statistics} from 'modules/mocks/statistics';
import {mockFetchProcessCoreStatistics} from 'modules/mocks/api/processInstances/fetchProcessCoreStatistics';
import {mockFetchIncidentsByError} from 'modules/mocks/api/incidents/fetchIncidentsByError';
import {mockFetchProcessInstancesByName} from 'modules/mocks/api/incidents/fetchProcessInstancesByName';

type Props = {
  children?: React.ReactNode;
};

const Wrapper = ({children}: Props) => {
  return <MemoryRouter>{children}</MemoryRouter>;
};

describe('Dashboard', () => {
  beforeEach(() => {
    statisticsStore.reset();
  });

  it('should render', async () => {
    mockFetchProcessCoreStatistics().withSuccess(statistics);
    mockFetchIncidentsByError().withSuccess(mockIncidentsByError);
    mockFetchProcessInstancesByName().withSuccess(mockWithSingleVersion);

    render(<Dashboard />, {wrapper: Wrapper});

    expect(
      await screen.findByText('1087 Running Process Instances in total'),
    ).toBeInTheDocument();

    expect(document.title).toBe(PAGE_TITLE.DASHBOARD);
    expect(screen.getByText('Operate Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Process Instances by Name')).toBeInTheDocument();
    expect(
      screen.getByText('Process Incidents by Error Message'),
    ).toBeInTheDocument();
  });

  it('should render empty state (no instances)', async () => {
    mockFetchProcessCoreStatistics().withSuccess(statistics);
    mockFetchIncidentsByError().withSuccess(mockIncidentsByError);
    mockFetchProcessInstancesByName().withSuccess([]);

    render(<Dashboard />, {wrapper: Wrapper});

    expect(
      await screen.findByText('Start by deploying a process'),
    ).toBeInTheDocument();
    expect(
      screen.queryByText('Process Incidents by Error Message'),
    ).not.toBeInTheDocument();
  });

  it('should render empty state (no incidents)', async () => {
    mockFetchProcessCoreStatistics().withSuccess(statistics);
    mockFetchIncidentsByError().withSuccess([]);
    mockFetchProcessInstancesByName().withSuccess(mockWithSingleVersion);

    render(<Dashboard />, {wrapper: Wrapper});

    expect(
      await screen.findByText('Your processes are healthy'),
    ).toBeInTheDocument();
    expect(screen.getByText('Process Instances by Name')).toBeInTheDocument();
    expect(
      screen.getByText('Process Incidents by Error Message'),
    ).toBeInTheDocument();
  });
});
