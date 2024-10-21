/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.optimize.dto.optimize.OptimizeDto;
import io.camunda.optimize.dto.optimize.datasource.IngestedDataSourceDto;
import java.time.OffsetDateTime;

public class TimestampBasedImportIndexDto extends ImportIndexDto<IngestedDataSourceDto>
    implements OptimizeDto {

  protected String esTypeIndexRefersTo;

  public TimestampBasedImportIndexDto(
      final OffsetDateTime lastImportExecutionTimestamp,
      final OffsetDateTime timestampOfLastEntity,
      final String esTypeIndexRefersTo,
      final IngestedDataSourceDto dataSourceDto) {
    super(lastImportExecutionTimestamp, timestampOfLastEntity, dataSourceDto);
    this.esTypeIndexRefersTo = esTypeIndexRefersTo;
  }

  public TimestampBasedImportIndexDto() {}

  @JsonIgnore
  public String getDataSourceName() {
    return dataSource.getName();
  }

  public String getEsTypeIndexRefersTo() {
    return esTypeIndexRefersTo;
  }

  public void setEsTypeIndexRefersTo(final String esTypeIndexRefersTo) {
    this.esTypeIndexRefersTo = esTypeIndexRefersTo;
  }

  @Override
  protected boolean canEqual(final Object other) {
    return other instanceof TimestampBasedImportIndexDto;
  }

  @Override
  public int hashCode() {
    return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(final Object o) {
    return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public String toString() {
    return "TimestampBasedImportIndexDto(esTypeIndexRefersTo=" + getEsTypeIndexRefersTo() + ")";
  }

  public static final class Fields {

    public static final String esTypeIndexRefersTo = "esTypeIndexRefersTo";
  }
}
