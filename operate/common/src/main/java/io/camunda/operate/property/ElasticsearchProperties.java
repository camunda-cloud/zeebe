/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.property;

import static io.camunda.operate.util.ConversionUtils.stringIsEmpty;

import io.camunda.search.connect.plugin.PluginConfiguration;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class ElasticsearchProperties {
  public static final String ELS_DATE_FORMAT_DEFAULT = "date_time";

  public static final int BULK_REQUEST_MAX_SIZE_IN_BYTES_DEFAULT = 1024 * 1024 * 90; // 90 MB

  private String clusterName; // = "elasticsearch";

  @Deprecated private String host; // = "localhost";

  @Deprecated private int port = -1; // 9200;

  private String dateFormat; // = OperateDateTimeFormatter.DATE_FORMAT_DEFAULT;

  private String elsDateFormat; // = ELS_DATE_FORMAT_DEFAULT;

  private int batchSize = -1; // = 200;

  private Integer socketTimeout;
  private Integer connectTimeout;

  private Boolean createSchema; // = true;

  /** Indicates whether operate does a proper health check for ES clusters. */
  private Boolean healthCheckEnabled; // = true;

  private String url;
  private String username;
  private String password;

  private int bulkRequestMaxSizeInBytes = -1; // = BULK_REQUEST_MAX_SIZE_IN_BYTES_DEFAULT;

  @NestedConfigurationProperty private SslProperties ssl;

  private List<PluginConfiguration> interceptorPlugins;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(final String clusterName) {
    this.clusterName = clusterName;
  }

  @Deprecated
  public String getHost() {
    return getFromURIorDefault(URI::getHost, host);
  }

  @Deprecated
  public void setHost(final String host) {
    this.host = host;
  }

  @Deprecated
  public int getPort() {
    return getFromURIorDefault(URI::getPort, port);
  }

  @Deprecated
  public void setPort(final int port) {
    this.port = port;
  }

  private <T> T getFromURIorDefault(final Function<URI, T> valueFromURI, final T defaultValue) {
    if (!stringIsEmpty(url)) {
      try {
        return valueFromURI.apply(new URI(url));
      } catch (final URISyntaxException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(final String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getElsDateFormat() {
    return elsDateFormat;
  }

  public void setElsDateFormat(final String elsDateFormat) {
    this.elsDateFormat = elsDateFormat;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(final int batchSize) {
    this.batchSize = batchSize;
  }

  public Boolean isCreateSchema() {
    return createSchema;
  }

  public void setCreateSchema(final boolean createSchema) {
    this.createSchema = createSchema;
  }

  public Boolean isHealthCheckEnabled() {
    return healthCheckEnabled;
  }

  public void setHealthCheckEnabled(final boolean healthCheckEnabled) {
    this.healthCheckEnabled = healthCheckEnabled;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getUrl() {
    if (getHost() != null && getPort() > 1) {
      if (stringIsEmpty(url)) {
        return String.format("http://%s:%d", getHost(), getPort());
      } else {
        return url;
      }
    } else {
      return null;
    }
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public Integer getSocketTimeout() {
    return socketTimeout;
  }

  public void setSocketTimeout(final Integer socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  public Integer getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(final Integer connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public SslProperties getSsl() {
    return ssl;
  }

  public void setSsl(final SslProperties ssl) {
    this.ssl = ssl;
  }

  public long getBulkRequestMaxSizeInBytes() {
    return bulkRequestMaxSizeInBytes;
  }

  public void setBulkRequestMaxSizeInBytes(final int bulkRequestMaxSizeInBytes) {
    this.bulkRequestMaxSizeInBytes = bulkRequestMaxSizeInBytes;
  }

  public List<PluginConfiguration> getInterceptorPlugins() {
    return interceptorPlugins;
  }

  public void setInterceptorPlugins(final List<PluginConfiguration> interceptorPlugins) {
    this.interceptorPlugins = interceptorPlugins;
  }
}
