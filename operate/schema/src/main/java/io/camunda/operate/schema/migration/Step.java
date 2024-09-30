/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.schema.migration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.OffsetDateTime;
import java.util.Comparator;

/**
 * A step describes a change in one index in a specific version and in which order inside the
 * version.<br>
 * A step stores when it was created and applied.<br>
 * The change is described in content of step.<br>
 * It also provides comparators for SemanticVersion and order comparing.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ProcessorStep.class),
  @JsonSubTypes.Type(value = SetBpmnProcessIdStep.class),
  @JsonSubTypes.Type(value = FillPostImporterQueueStep.class)
})
public interface Step {

  public static final String INDEX_NAME = "indexName",
      CREATED_DATE = "createdDate",
      APPLIED = "applied",
      APPLIED_DATE = "appliedDate",
      VERSION = "version",
      ORDER = "order",
      CONTENT = "content";
  public static final Comparator<Step> SEMANTICVERSION_COMPARATOR =
      new Comparator<Step>() {
        @Override
        public int compare(final Step s1, final Step s2) {
          return SemanticVersion.fromVersion(s1.getVersion())
              .compareTo(SemanticVersion.fromVersion(s2.getVersion()));
        }
      };
  public static final Comparator<Step> ORDER_COMPARATOR =
      new Comparator<Step>() {
        @Override
        public int compare(final Step s1, final Step s2) {
          return s1.getOrder().compareTo(s2.getOrder());
        }
      };
  public static final Comparator<Step> SEMANTICVERSION_ORDER_COMPARATOR =
      new Comparator<Step>() {
        @Override
        public int compare(final Step s1, final Step s2) {
          int result = SEMANTICVERSION_COMPARATOR.compare(s1, s2);
          if (result == 0) {
            result = ORDER_COMPARATOR.compare(s1, s2);
          }
          return result;
        }
      };

  public OffsetDateTime getCreatedDate();

  public Step setCreatedDate(final OffsetDateTime date);

  public OffsetDateTime getAppliedDate();

  public Step setAppliedDate(final OffsetDateTime date);

  public String getVersion();

  public Integer getOrder();

  public boolean isApplied();

  public Step setApplied(final boolean isApplied);

  public String getIndexName();

  public String getContent();

  public String getDescription();
}
