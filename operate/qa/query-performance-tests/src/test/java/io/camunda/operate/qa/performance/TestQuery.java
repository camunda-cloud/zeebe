/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.qa.performance;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpMethod;

public class TestQuery {

  private String title;

  private String url;

  private HttpMethod method;

  private String pathParams;

  private JsonNode body;

  private String ignore;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public void setMethod(HttpMethod method) {
    this.method = method;
  }

  public String getPathParams() {
    return pathParams;
  }

  public void setPathParams(String pathParams) {
    this.pathParams = pathParams;
  }

  @JsonRawValue
  public String getBody() {
    // default raw value: null or "[]"
    return body == null ? null : body.toString();
  }

  public void setBody(JsonNode node) {
    this.body = node;
  }

  public String getIgnore() {
    return ignore;
  }

  public void setIgnore(String ignore) {
    this.ignore = ignore;
  }

  @Override
  public String toString() {
    return title;
  }
}
