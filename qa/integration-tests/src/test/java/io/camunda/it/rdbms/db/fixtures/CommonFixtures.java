/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db.fixtures;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class CommonFixtures {

  protected static final Random RANDOM = new Random(System.nanoTime());
  protected static final OffsetDateTime NOW = OffsetDateTime.now();
  private static final AtomicLong ID_COUNTER = new AtomicLong();

  public static Long nextKey() {
    return ID_COUNTER.incrementAndGet();
  }

  public static String nextStringId() {
    return UUID.randomUUID().toString();
  }
}
