/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {isDurationHeatmap, isProcessInstanceDuration} from './service';

it('should correctly check for duration heatmap', () => {
  expect(
    isDurationHeatmap({
      view: {entity: 'flowNode', properties: ['duration']},
      visualization: 'heat',
      definitions: [
        {
          key: 'test',
          versions: ['test'],
        },
      ],
    })
  ).toBeTruthy();
});

it('should work for user task reports', () => {
  expect(
    isDurationHeatmap({
      view: {entity: 'userTask', properties: ['duration']},
      visualization: 'heat',
      definitions: [
        {
          key: 'test',
          versions: ['test'],
        },
      ],
    })
  ).toBeTruthy();

  expect(
    isDurationHeatmap({
      view: {entity: 'userTask', properties: ['frequency']},
      visualization: 'heat',
      definitions: [
        {
          key: 'test',
          versions: ['test'],
        },
      ],
    })
  ).toBeFalsy();
});

it('should correclty check for process instance duration reports', () => {
  expect(
    isProcessInstanceDuration({view: {entity: 'processInstance', properties: ['duration']}})
  ).toBeTruthy();
});
