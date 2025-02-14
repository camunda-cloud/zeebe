/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.application.commons.rdbms.RdbmsConfiguration;
import io.camunda.it.rdbms.db.util.RdbmsTestConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/** This test verifies that the old indexPrefix property is still working. */
@Tag("rdbms")
@DataJdbcTest
@ContextConfiguration(classes = {RdbmsTestConfiguration.class, RdbmsConfiguration.class})
@AutoConfigurationPackage
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "camunda.database.type=rdbms",
      "camunda.database.index-prefix=C8_"
    })
public class TableIndexPrefixIT {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  public void shouldApplyPrefixToAllTables() {
    final var tableNames =
        jdbcTemplate
            .queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")
            .stream()
            .map(row -> row.get("TABLE_NAME").toString())
            .toList();

    for (final String tableName : tableNames) {
      assertThat(tableName).startsWith("C8_");
    }
  }

  @Test
  public void shouldApplyPrefixToAllIndices() {
    final var tableNames =
        jdbcTemplate
            .queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_SCHEMA = 'PUBLIC' AND INDEX_TYPE_NAME != 'PRIMARY KEY'")
            .stream()
            .map(row -> row.get("INDEX_NAME").toString())
            .toList();

    for (final String tableName : tableNames) {
      assertThat(tableName).startsWith("C8_");
    }
  }
}
