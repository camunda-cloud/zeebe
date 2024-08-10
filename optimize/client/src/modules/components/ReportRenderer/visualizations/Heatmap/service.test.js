/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {calculateTargetValueHeat, getConfig} from './service';

jest.mock('heatmap.js', () => {});
jest.mock('services', () => ({
  formatters: {convertToMilliseconds: (value) => value},
}));

describe('calculateTargetValueHeat', () => {
  it('should return the relative difference between actual and target value', () => {
    expect(calculateTargetValueHeat({a: 10}, {a: {value: 5, unit: 'millis'}})).toEqual({a: 1});
  });

  it('should return null for an element that is below target value', () => {
    expect(calculateTargetValueHeat({a: 2}, {a: {value: 5, unit: 'millis'}})).toEqual({a: null});
  });
});

it('should construct rawdata report with the target value as a filter', () => {
  const configuration = {
    heatmapTargetValue: {
      values: {
        flowNodeA: {
          value: 1234,
          unit: 'days',
        },
      },
    },
  };

  expect(
    getConfig(
      {
        configuration,
        definitions: [
          {
            key: '1',
            versions: ['1'],
            tenantIds: ['tenantA'],
          },
        ],
        filter: [{type: 'test'}],
      },
      'flowNodeA'
    )
  ).toMatchSnapshot();
});
