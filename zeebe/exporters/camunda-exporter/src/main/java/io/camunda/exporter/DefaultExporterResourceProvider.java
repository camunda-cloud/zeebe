/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter;

import io.camunda.webapps.schema.descriptors.IndexDescriptor;
import io.camunda.webapps.schema.descriptors.IndexTemplateDescriptor;
import java.util.List;

/**
 * This is the class where teams should make their components such as handlers, and index/index
 * template descriptors available
 */
public class DefaultExporterResourceProvider implements ExporterResourceProvider {

  @Override
  public List<IndexDescriptor> getIndexDescriptors() {
    return List.of();
  }

  @Override
  public List<IndexTemplateDescriptor> getIndexTemplateDescriptors() {
    return List.of();
  }
}
