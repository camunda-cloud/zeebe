/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.rdbms;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);

  private DateUtil() {}

  public static OffsetDateTime toOffsetDateTime(final Long timestamp) {
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
  }

  public static OffsetDateTime toOffsetDateTime(final String timestamp) {
    return toOffsetDateTime(timestamp, DateTimeFormatter.ISO_ZONED_DATE_TIME);
  }

  public static OffsetDateTime toOffsetDateTime(
      final String timestamp, final DateTimeFormatter dateTimeFormatter) {
    if (timestamp == null || timestamp.isEmpty()) {
      return null;
    }

    try {
      final ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, dateTimeFormatter);
      return OffsetDateTime.ofInstant(zonedDateTime.toInstant(), ZoneId.systemDefault());
    } catch (final DateTimeParseException e) {
      LOGGER.error(String.format("Cannot parse date from %s - %s", timestamp, e.getMessage()), e);
    }

    return null;
  }

  /**
   * Parses the string to a duration, where it can either be simple a amount of days, or a ISO-8601 duration
   *
   * @param duration ... Number of days or ISO-8601 duration
   * @return Duration
   */
  public static Duration toDuration(final String duration) {
    if (NumberUtils.isCreatable(duration)) {
      return Duration.ofDays(NumberUtils.toLong(duration));
    }

    try {
      return Duration.parse(duration);
    } catch (final DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid duration string (Must be ISO-8601 or just a number of days): " + duration, e);
    }
  }
}
