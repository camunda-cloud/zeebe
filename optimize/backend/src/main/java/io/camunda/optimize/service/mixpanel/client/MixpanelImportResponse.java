/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.mixpanel.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class MixpanelImportResponse {

  @JsonProperty("error")
  private String error;

  @JsonProperty("num_records_imported")
  private int numberOfRecordsImported;

  public MixpanelImportResponse() {}

  @JsonIgnore
  public boolean isSuccessful() {
    return StringUtils.isEmpty(this.error);
  }
}
