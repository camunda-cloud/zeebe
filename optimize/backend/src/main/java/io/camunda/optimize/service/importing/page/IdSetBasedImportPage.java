/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.page;

import java.util.Set;

public class IdSetBasedImportPage implements ImportPage {

  private Set<String> ids;

  public Set<String> getIds() {
    return ids;
  }

  public void setIds(Set<String> ids) {
    this.ids = ids;
  }
}
