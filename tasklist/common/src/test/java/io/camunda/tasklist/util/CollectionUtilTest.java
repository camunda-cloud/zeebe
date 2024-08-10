/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.camunda.tasklist.exceptions.TasklistRuntimeException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CollectionUtilTest {

  @Test
  void testAsMapOneEntry() {
    final Map<String, Object> result = CollectionUtil.asMap("key1", "value1");
    assertThat(result).hasSize(1);
    assertThat(result).containsEntry("key1", "value1");
  }

  @Test
  void testAsMapManyEntries() {
    final Map<String, Object> result =
        CollectionUtil.asMap("key1", "value1", "key2", "value2", "key3", "value3");
    assertThat(result).hasSize(3);
    assertThat(result).containsEntry("key2", "value2");
    assertThat(result).containsEntry("key3", "value3");
  }

  @Test
  void testAsMapException() {
    assertThatExceptionOfType(TasklistRuntimeException.class)
        .isThrownBy(() -> CollectionUtil.asMap((Object[]) null));
    assertThatExceptionOfType(TasklistRuntimeException.class)
        .isThrownBy(() -> CollectionUtil.asMap("key1"));
    assertThatExceptionOfType(TasklistRuntimeException.class)
        .isThrownBy(() -> CollectionUtil.asMap("key1", "value1", "key2"));
  }

  @Test
  void testFromTo() {
    assertThat(CollectionUtil.fromTo(0, 0)).contains(0);
    assertThat(CollectionUtil.fromTo(0, -1)).isEmpty();
    assertThat(CollectionUtil.fromTo(-1, 0)).contains(-1, 0);
    assertThat(CollectionUtil.fromTo(1, 5)).contains(1, 2, 3, 4, 5);
  }

  @Test
  void testWithoutNulls() {
    final List<Object> ids = Arrays.asList("id-1", null, "id3", null, null, "id5");
    assertThat(CollectionUtil.withoutNulls(ids)).containsExactly("id-1", "id3", "id5");
  }

  @Test
  void testToSafeListOfStrings() {
    final List<Object> ids = Arrays.asList("id-1", null, "id3", null, null, "id5");
    assertThat(CollectionUtil.withoutNulls(ids)).containsExactly("id-1", "id3", "id5");
  }

  @Test
  void testSplitAndGetSublist() {
    List<Integer> partitions = Arrays.asList(1, 2, 3, 4, 5, 6);
    assertThat(CollectionUtil.splitAndGetSublist(partitions, 2, 1))
        .containsExactlyInAnyOrder(4, 5, 6);

    partitions = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
    assertThat(CollectionUtil.splitAndGetSublist(partitions, 3, 0))
        .containsExactlyInAnyOrder(1, 2, 3);
    assertThat(CollectionUtil.splitAndGetSublist(partitions, 3, 1))
        .containsExactlyInAnyOrder(4, 5, 6);
    assertThat(CollectionUtil.splitAndGetSublist(partitions, 3, 2)).containsExactlyInAnyOrder(7, 8);

    partitions = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
    assertThat(CollectionUtil.splitAndGetSublist(partitions, 3, 4)).isEmpty();
  }
}
