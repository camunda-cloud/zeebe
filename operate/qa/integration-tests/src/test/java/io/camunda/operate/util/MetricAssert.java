/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class MetricAssert {

  //  public static void assertThatMetricsAreDisabledFrom(MockMvc mockMvc) {
  //    MockHttpServletRequestBuilder request = get(ENDPOINT);
  //    try {
  //      mockMvc.perform(request)
  //          .andExpect(status().is(404));
  //    } catch (Exception e) {
  //      throw new RuntimeException("Exception while asserting:" + e.getMessage(), e);
  //    }
  //  }

  public static void assertThatMetricsFrom(MockMvc mockMvc, Matcher<? super String> matcher) {
    final MockHttpServletRequestBuilder request = get("/actuator/prometheus");
    try {
      mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().string(matcher));
    } catch (Exception e) {
      throw new RuntimeException("Exception while asserting:" + e.getMessage(), e);
    }
  }

  public static final class ValueMatcher extends BaseMatcher {

    private final String metricName;
    private final Predicate<Double> valueMatcher;

    public ValueMatcher(String metricName, Predicate<Double> valueMatcher) {
      this.metricName = metricName.toLowerCase();
      this.valueMatcher = valueMatcher;
    }

    @Override
    public boolean matches(Object o) {
      final Double metricValue = getMetricValue(o);
      if (metricValue != null) {
        return valueMatcher.test(metricValue);
      }
      return false;
    }

    public Double getMetricValue(Object o) {
      final Optional<String> metricString = getMetricString(o);
      if (metricString.isPresent()) {
        final String[] oneMetric = metricString.get().split(" ");
        if (oneMetric.length > 1) {
          return Double.valueOf(oneMetric[1]);
        }
      }
      return null;
    }

    public Optional<String> getMetricString(Object o) {
      final String s = (String) o;
      final String[] strings = s.split("\\n");
      return Arrays.stream(strings)
          .filter(str -> str.toLowerCase().contains(metricName) && !str.startsWith("#"))
          .findFirst();
    }

    @Override
    public void describeTo(Description description) {}
  }
}
