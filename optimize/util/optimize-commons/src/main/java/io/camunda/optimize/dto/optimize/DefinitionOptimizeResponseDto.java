/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.optimize.dto.optimize.datasource.DataSourceDto;
import java.io.Serializable;
import lombok.Data;

@Data
public abstract class DefinitionOptimizeResponseDto implements Serializable, OptimizeDto {

  private String id;
  private String key;
  private String version;
  private String versionTag;
  private String name;
  private DataSourceDto dataSource;
  private String tenantId;
  private boolean deleted;
  @JsonIgnore private DefinitionType type;

  protected DefinitionOptimizeResponseDto(final String id, final DataSourceDto dataSource) {
    this.id = id;
    this.dataSource = dataSource;
  }

  public DefinitionOptimizeResponseDto(
      String id,
      String key,
      String version,
      String versionTag,
      String name,
      DataSourceDto dataSource,
      String tenantId,
      boolean deleted,
      DefinitionType type) {
    this.id = id;
    this.key = key;
    this.version = version;
    this.versionTag = versionTag;
    this.name = name;
    this.dataSource = dataSource;
    this.tenantId = tenantId;
    this.deleted = deleted;
    this.type = type;
  }

  protected DefinitionOptimizeResponseDto() {}

  public static final class Fields {

    public static final String id = "id";
    public static final String key = "key";
    public static final String version = "version";
    public static final String versionTag = "versionTag";
    public static final String name = "name";
    public static final String dataSource = "dataSource";
    public static final String tenantId = "tenantId";
    public static final String deleted = "deleted";
    public static final String type = "type";
  }
}
