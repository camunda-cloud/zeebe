/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.document.store.gcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.camunda.document.api.DocumentCreationRequest;
import io.camunda.document.api.DocumentError;
import io.camunda.document.api.DocumentError.UnknownDocumentError;
import io.camunda.document.api.DocumentLink;
import io.camunda.document.api.DocumentMetadataModel;
import io.camunda.document.api.DocumentReference;
import io.camunda.document.api.DocumentStore;
import io.camunda.zeebe.util.Either;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

public class GcpDocumentStore implements DocumentStore {

  private static final String CONTENT_HASH_METADATA_KEY = "contentHash";

  private final String bucketName;
  private final Storage storage;

  private final ObjectMapper objectMapper;

  private final ExecutorService executor;

  public GcpDocumentStore(final String bucketName) {
    this(bucketName, new ObjectMapper(), Executors.newSingleThreadExecutor());
  }

  public GcpDocumentStore(
      final String bucketName, final ObjectMapper objectMapper, final ExecutorService executor) {
    this.bucketName = bucketName;
    storage = StorageOptions.getDefaultInstance().getService();
    this.objectMapper = objectMapper;
    this.executor = executor;
  }

  public GcpDocumentStore(
      final String bucketName,
      final Storage storage,
      final ObjectMapper objectMapper,
      final ExecutorService executor) {
    this.bucketName = bucketName;
    this.storage = storage;
    this.objectMapper = objectMapper;
    this.executor = executor;
  }

  @Override
  public CompletableFuture<Either<DocumentError, DocumentReference>> createDocument(
      final DocumentCreationRequest request) {
    return CompletableFuture.supplyAsync(() -> createDocumentInternal(request), executor);
  }

  @Override
  public CompletableFuture<Either<DocumentError, InputStream>> getDocument(
      final String documentId) {
    return CompletableFuture.supplyAsync(() -> getDocumentContentInternal(documentId), executor);
  }

  @Override
  public CompletableFuture<Either<DocumentError, Void>> deleteDocument(final String documentId) {
    return CompletableFuture.supplyAsync(() -> deleteDocumentInternal(documentId), executor);
  }

  @Override
  public CompletableFuture<Either<DocumentError, DocumentLink>> createLink(
      final String documentId, final long durationInMillis) {
    return CompletableFuture.supplyAsync(
        () -> createLinkInternal(documentId, durationInMillis), executor);
  }

  @Override
  public CompletableFuture<Either<DocumentError, Void>> verifyContentHash(
      final String documentId, final String contentHash) {
    return CompletableFuture.supplyAsync(
        () -> verifyContentHashInternal(documentId, contentHash), executor);
  }

  private Either<DocumentError, DocumentReference> createDocumentInternal(
      final DocumentCreationRequest request) {
    final String documentId =
        Optional.ofNullable(request.documentId()).orElse(UUID.randomUUID().toString());

    final Blob existingBlob;
    try {
      existingBlob = storage.get(bucketName, documentId);
    } catch (final Exception e) {
      return Either.left(new UnknownDocumentError(e));
    }
    if (existingBlob != null) {
      return Either.left(new DocumentError.DocumentAlreadyExists(documentId));
    }
    final BlobId blobId = BlobId.of(bucketName, documentId);
    final var blobInfoBuilder = BlobInfo.newBuilder(blobId);
    try {
      applyMetadata(blobInfoBuilder, request.metadata());
    } catch (final JsonProcessingException e) {
      return Either.left(
          new DocumentError.InvalidInput("Failed to serialize metadata: " + e.getMessage()));
    }

    final MessageDigest md;
    try {
      md = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);
    } catch (final Exception e) {
      // should never happen
      return Either.left(new UnknownDocumentError(e));
    }
    final DigestInputStream digestStream = new DigestInputStream(request.contentInputStream(), md);

    try {
      storage.createFrom(blobInfoBuilder.build(), digestStream);
    } catch (final Exception e) {
      return Either.left(new UnknownDocumentError(e));
    }

    final var contentHash = HexFormat.of().formatHex(md.digest());

    final var metadataUpdateResult =
        addBlobMetadata(documentId, CONTENT_HASH_METADATA_KEY, contentHash);
    if (metadataUpdateResult.isLeft()) {
      return Either.left(metadataUpdateResult.getLeft());
    }

    final var documentReference =
        new DocumentReference(documentId, contentHash, request.metadata());
    return Either.right(documentReference);
  }

  private Either<DocumentError, InputStream> getDocumentContentInternal(final String documentId) {
    try {
      final Blob blob = storage.get(bucketName, documentId);
      if (blob == null) {
        return Either.left(new DocumentError.DocumentNotFound(documentId));
      }
      final var inputStream = Channels.newInputStream(blob.reader());
      return Either.right(inputStream);
    } catch (final Exception e) {
      return Either.left(new UnknownDocumentError(e));
    }
  }

  private Either<DocumentError, Void> deleteDocumentInternal(final String documentId) {
    try {
      final boolean result = storage.delete(bucketName, documentId);
      if (!result) {
        return Either.left(new DocumentError.DocumentNotFound(documentId));
      }
      return Either.right(null);
    } catch (final Exception e) {
      return Either.left(new UnknownDocumentError(e));
    }
  }

  private Either<DocumentError, DocumentLink> createLinkInternal(
      final String documentId, final long durationInMillis) {
    try {
      final Blob blob = storage.get(bucketName, documentId);
      if (blob == null) {
        return Either.left(new DocumentError.DocumentNotFound(documentId));
      }
      final var link = blob.signUrl(durationInMillis, TimeUnit.MILLISECONDS);
      return Either.right(
          new DocumentLink(
              link.toString(), OffsetDateTime.now().plus(Duration.ofMillis(durationInMillis))));
    } catch (final Exception e) {
      return Either.left(new UnknownDocumentError(e));
    }
  }

  private Either<DocumentError, Void> addBlobMetadata(
      final String documentId, final String key, final String value) {
    try {
      final Blob blob = storage.get(bucketName, documentId);
      if (blob == null) {
        return Either.left(new DocumentError.DocumentNotFound(documentId));
      }
      final var blobInfoBuilder = BlobInfo.newBuilder(blob.getBlobId());
      final var metadata = Optional.ofNullable(blob.getMetadata()).orElse(new HashMap<>());
      metadata.put(key, value);
      blobInfoBuilder.setMetadata(metadata);
      storage.update(blobInfoBuilder.build());
      return Either.right(null);
    } catch (final Exception e) {
      return Either.left(new UnknownDocumentError(e));
    }
  }

  private Either<DocumentError, Void> verifyContentHashInternal(
      final String documentId, final String contentHashToVerify) {
    try {
      final Blob blob = storage.get(bucketName, documentId);
      if (blob == null) {
        return Either.left(new DocumentError.DocumentNotFound(documentId));
      }
      final var metadata = blob.getMetadata();
      if (metadata == null) {
        return Either.left(new DocumentError.InvalidInput("No metadata found for document"));
      }
      final var storedContentHash = metadata.get(CONTENT_HASH_METADATA_KEY);
      if (storedContentHash == null) {
        return Either.left(new DocumentError.InvalidInput("No content hash found for document"));
      }
      if (!storedContentHash.equals(contentHashToVerify)) {
        return Either.left(new DocumentError.DocumentHashMismatch(documentId, contentHashToVerify));
      }
      return Either.right(null);
    } catch (final Exception e) {
      return Either.left(new UnknownDocumentError(e));
    }
  }

  private void applyMetadata(
      final BlobInfo.Builder blobInfoBuilder, final DocumentMetadataModel metadata)
      throws JsonProcessingException {
    if (metadata == null) {
      return;
    }
    if (metadata.contentType() != null && !metadata.contentType().isEmpty()) {
      blobInfoBuilder.setContentType(metadata.contentType());
    }
    if (metadata.expiresAt() != null) {
      blobInfoBuilder.setCustomTimeOffsetDateTime(OffsetDateTime.from(metadata.expiresAt()));
    }
    if (metadata.fileName() != null && !metadata.fileName().isEmpty()) {
      blobInfoBuilder.setContentDisposition("attachment; filename=" + metadata.fileName());
    } else {
      blobInfoBuilder.setContentDisposition("attachment");
    }
    if (metadata.customProperties() != null && !metadata.customProperties().isEmpty()) {
      final Map<String, String> blobMetadata = new HashMap<>();
      final var valueAsString = objectMapper.writeValueAsString(metadata.customProperties());
      metadata.customProperties().forEach((key, value) -> blobMetadata.put(key, valueAsString));
      blobInfoBuilder.setMetadata(blobMetadata);
    }
  }
}
