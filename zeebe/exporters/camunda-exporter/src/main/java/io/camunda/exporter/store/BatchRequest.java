/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.store;

import io.camunda.exporter.exceptions.PersistenceException;
import io.camunda.webapps.schema.entities.ExporterEntity;
import java.util.Map;
import java.util.function.Function;

/** A {@link BatchRequest} contains updates to one or more {@link ExporterEntity} */
@SuppressWarnings("rawtypes")
public interface BatchRequest {

  BatchRequest add(String index, ExporterEntity entity);

  BatchRequest addWithId(String index, String id, ExporterEntity entity);

  BatchRequest addWithRouting(String index, ExporterEntity entity, String routing);

  BatchRequest upsert(
      String index, String id, ExporterEntity entity, Map<String, Object> updateFields);

  BatchRequest upsertWithRouting(
      String index,
      String id,
      ExporterEntity entity,
      Map<String, Object> updateFields,
      String routing);

  BatchRequest upsertWithScript(
      String index,
      String id,
      ExporterEntity entity,
      String script,
      Map<String, Object> parameters);

  BatchRequest upsertWithScriptAndRouting(
      String index,
      String id,
      ExporterEntity entity,
      String script,
      Map<String, Object> parameters,
      String routing);

  BatchRequest update(String index, String id, Map<String, Object> updateFields);

  BatchRequest update(String index, String id, ExporterEntity entity) throws PersistenceException;

  BatchRequest updateWithScript(
      String index, String id, String script, Map<String, Object> parameters);

  BatchRequest delete(String index, String id);

  BatchRequest deleteWithRouting(String index, String id, String routing);

  /**
   * Applies all updates in this batch.
   *
   * @throws PersistenceException if an error occurs during the execution
   */
  void execute() throws PersistenceException;

  void executeWithRefresh() throws PersistenceException;

  /**
   * Adds an error handler to the batch request. The error handler is called when an error occurs
   *
   * @param index the index name that this specific error handler should be used to handle errors
   *     for.
   * @param errorHandler the error handler to call when an error occurs
   */
  void onError(String index, Function<String, Void> errorHandler);
}
