/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {Page, Locator} from '@playwright/test';
import {Paths} from 'modules/Routes';

export class Dashboard {
  private page: Page;
  readonly metricPanel: Locator;

  constructor(page: Page) {
    this.page = page;
    this.metricPanel = page.getByTestId('metric-panel');
  }

  async navigateToDashboard(options?: Parameters<Page['goto']>[1]) {
    await this.page.goto(Paths.dashboard(), options);
  }
}
