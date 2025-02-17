/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.sql.typehandler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.StringTypeHandler;

public class WildcardTransformingStringTypeHandler extends StringTypeHandler {

  // Regular expression to match unescaped wildcards
  public static final String REGEX_ASTERISK = "(?<!\\\\)\\*";
  public static final String REGEX_QUESTION_MARK = "(?<!\\\\)\\?";
  public static final String REGEX_PERCENT = "(?<!\\\\)%";
  public static final String REGEX_UNDERSCORE = "(?<!\\\\)_";
  public static final Pattern PATTERN_ASTERISK = Pattern.compile(REGEX_ASTERISK);
  public static final Pattern PATTERN_QUESTION_MARK = Pattern.compile(REGEX_QUESTION_MARK);
  public static final Pattern PATTERN_PERCENT = Pattern.compile(REGEX_PERCENT);
  public static final Pattern PATTERN_UNDERSCORE = Pattern.compile(REGEX_UNDERSCORE);

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps, final int i, final String parameter, final JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, transformElasticsearchToSql(parameter));
  }

  private static String replace(
      final Pattern pattern, final String input, final String replacement) {
    final var matcher = pattern.matcher(input);
    final var builder = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(builder, replacement);
    }
    matcher.appendTail(builder);
    return builder.toString();
  }

  public static String transformElasticsearchToSql(String exp) {
    // Escape unescaped %
    exp = replace(PATTERN_PERCENT, exp, "\\\\%");
    // Escape unescaped _
    exp = replace(PATTERN_UNDERSCORE, exp, "\\\\_");
    // Replace unescaped * with %
    exp = replace(PATTERN_ASTERISK, exp, "%");
    // Replace unescaped ? with _
    exp = replace(PATTERN_QUESTION_MARK, exp, "_");

    return exp;
  }
}
