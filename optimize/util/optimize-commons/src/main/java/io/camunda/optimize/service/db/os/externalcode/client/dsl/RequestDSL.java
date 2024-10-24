/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.externalcode.client.dsl;

import static io.camunda.optimize.service.db.os.externalcode.client.sync.OpenSearchDocumentOperations.SCROLL_KEEP_ALIVE_MS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.cluster.PutComponentTemplateRequest;
import org.opensearch.client.opensearch.core.ClearScrollRequest;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.ReindexRequest;
import org.opensearch.client.opensearch.core.ScrollRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.UpdateByQueryRequest;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.reindex.Destination;
import org.opensearch.client.opensearch.core.reindex.Source;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.IndexState;
import org.opensearch.client.opensearch.snapshot.CreateSnapshotRequest;
import org.opensearch.client.opensearch.snapshot.DeleteSnapshotRequest;
import org.opensearch.client.opensearch.snapshot.GetRepositoryRequest;
import org.opensearch.client.opensearch.snapshot.GetSnapshotRequest;

public interface RequestDSL {
  enum QueryType {
    ONLY_RUNTIME,
    ALL
  }

  static CreateIndexRequest.Builder createIndexRequestBuilder(
      final String index, final IndexState patternIndex) {
    return new CreateIndexRequest.Builder()
        .index(index)
        .aliases(patternIndex.aliases())
        .mappings(patternIndex.mappings())
        .settings(
            s ->
                s.index(
                    i ->
                        i.numberOfReplicas(patternIndex.settings().index().numberOfReplicas())
                            .numberOfShards(patternIndex.settings().index().numberOfShards())
                            .analysis(patternIndex.settings().index().analysis())));
  }

  static CreateSnapshotRequest.Builder createSnapshotRequestBuilder(
      final String repository, final String snapshot, final List<String> indices) {
    return new CreateSnapshotRequest.Builder()
        .repository(repository)
        .snapshot(snapshot)
        .indices(indices);
  }

  static DeleteRequest.Builder deleteRequestBuilder(final String index, final String id) {
    return new DeleteRequest.Builder().index(index).id(id).refresh(Refresh.True);
  }

  static DeleteByQueryRequest.Builder deleteByQueryRequestBuilder(final String index) {
    return new DeleteByQueryRequest.Builder().index(List.of(index));
  }

  static DeleteByQueryRequest.Builder deleteByQueryRequestBuilder(final List<String> indexes) {
    return new DeleteByQueryRequest.Builder().index(indexes);
  }

  static UpdateByQueryRequest.Builder updateByQueryRequestBuilder(final List<String> indexes) {
    return new UpdateByQueryRequest.Builder().index(indexes);
  }

  static DeleteSnapshotRequest.Builder deleteSnapshotRequestBuilder(
      final String repositoryName, final String snapshotName) {
    return new DeleteSnapshotRequest.Builder().repository(repositoryName).snapshot(snapshotName);
  }

  static <R> IndexRequest.Builder<R> indexRequestBuilder(final String index) {
    return new IndexRequest.Builder<R>().index(index);
  }

  static GetIndexRequest.Builder getIndexRequestBuilder(final String index) {
    return new GetIndexRequest.Builder().index(index);
  }

  static PutComponentTemplateRequest.Builder componentTemplateRequestBuilder(final String name) {
    return new PutComponentTemplateRequest.Builder().name(name);
  }

  static ReindexRequest.Builder reindexRequestBuilder(
      final String srcIndex, final Query srcQuery, final String dstIndex) {
    return new ReindexRequest.Builder()
        .source(Source.of(b -> b.index(srcIndex).query(srcQuery)))
        .dest(Destination.of(b -> b.index(dstIndex)));
  }

  static ReindexRequest.Builder reindexRequestBuilder(
      final String srcIndex,
      final String dstIndex,
      final String script,
      final Map<String, Object> scriptParams) {
    final var jsonParams =
        scriptParams.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> JsonData.of(e.getValue())));

    return new ReindexRequest.Builder()
        .source(Source.of(b -> b.index(srcIndex)))
        .dest(Destination.of(b -> b.index(dstIndex)))
        .script(b -> b.inline(i -> i.source(script).params(jsonParams)));
  }

  static GetRepositoryRequest.Builder repositoryRequestBuilder(final String name) {
    return new GetRepositoryRequest.Builder().name(name);
  }

  static SearchRequest.Builder searchRequestBuilder(final String... index) {
    return new SearchRequest.Builder().index(List.of(index));
  }

  static GetSnapshotRequest.Builder getSnapshotRequestBuilder(
      final String repository, final String snapshot) {
    return new GetSnapshotRequest.Builder().repository(repository).snapshot(snapshot);
  }

  static <R, A> UpdateRequest.Builder<R, A> updateRequestBuilder(final String index) {
    return new UpdateRequest.Builder<R, A>().index(index);
  }

  static GetRequest.Builder getRequestBuilder(final String index) {
    return new GetRequest.Builder().index(index);
  }

  static GetRequest.Builder getRequest(final String index, final String id) {
    return new GetRequest.Builder().index(index).id(id);
  }

  static ScrollRequest scrollRequest(final String scrollId, final String time) {
    return new ScrollRequest.Builder().scrollId(scrollId).scroll(time(time)).build();
  }

  static ScrollRequest scrollRequest(final String scrollId) {
    return scrollRequest(scrollId, SCROLL_KEEP_ALIVE_MS);
  }

  static ClearScrollRequest clearScrollRequest(final String scrollId) {
    return new ClearScrollRequest.Builder().scrollId(scrollId).build();
  }

  static Time time(final String value) {
    return Time.of(b -> b.time(value));
  }
}
