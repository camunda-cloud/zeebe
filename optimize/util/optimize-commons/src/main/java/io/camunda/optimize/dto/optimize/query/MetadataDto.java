/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query;

import io.camunda.optimize.dto.optimize.OptimizeDto;
import java.io.Serializable;
import lombok.Data;

@Data
public class MetadataDto implements OptimizeDto, Serializable {

  private String schemaVersion;
  private String installationId;

  public MetadataDto(String schemaVersion, String installationId) {
    this.schemaVersion = schemaVersion;
    this.installationId = installationId;
  }

  protected MetadataDto() {}

  public enum Fields {
    schemaVersion,
    installationId
  }
}
