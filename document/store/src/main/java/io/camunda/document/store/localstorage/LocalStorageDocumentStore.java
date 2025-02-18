/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.document.store.localstorage;

<<<<<<< HEAD
=======
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;

>>>>>>> 2c5476f5 (feat: add LocalStorage document store implementation)
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.document.api.DocumentContent;
import io.camunda.document.api.DocumentCreationRequest;
import io.camunda.document.api.DocumentError;
import io.camunda.document.api.DocumentError.DocumentAlreadyExists;
import io.camunda.document.api.DocumentError.DocumentHashMismatch;
import io.camunda.document.api.DocumentError.DocumentNotFound;
import io.camunda.document.api.DocumentError.OperationNotSupported;
import io.camunda.document.api.DocumentError.UnknownDocumentError;
import io.camunda.document.api.DocumentLink;
import io.camunda.document.api.DocumentMetadataModel;
import io.camunda.document.api.DocumentReference;
import io.camunda.document.api.DocumentStore;
import io.camunda.document.store.DocumentHashProcessor;
import io.camunda.document.store.DocumentHashProcessor.HashResult;
import io.camunda.zeebe.util.Either;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class LocalStorageDocumentStore implements DocumentStore {

  public static final String METADATA_SUFFIX = "-metadata";
  private final Path storagePath;
  private final FileHandler fileHandler;
  private final ExecutorService executor;
  private final ObjectMapper mapper;

  public LocalStorageDocumentStore(
      final Path storagePath,
      final FileHandler fileHandler,
      final ObjectMapper mapper,
      final ExecutorService executor) {
    this.storagePath = storagePath;
    this.fileHandler = fileHandler;
    this.mapper = mapper;
    this.executor = executor;
  }

  @Override
  public CompletableFuture<Either<DocumentError, DocumentReference>> createDocument(
      final DocumentCreationRequest request) {
    return CompletableFuture.supplyAsync(() -> createDocumentInternal(request), executor);
  }

  @Override
  public CompletableFuture<Either<DocumentError, DocumentContent>> getDocument(
      final String documentId) {
<<<<<<< HEAD
    return CompletableFuture.supplyAsync(() -> getDocumentInternal(documentId), executor);
=======
    return CompletableFuture.supplyAsync(() -> getDocumentInternal(documentId));
>>>>>>> 2c5476f5 (feat: add LocalStorage document store implementation)
  }

  @Override
  public CompletableFuture<Either<DocumentError, Void>> deleteDocument(final String documentId) {
<<<<<<< HEAD
    return CompletableFuture.supplyAsync(() -> deleteDocumentInternal(documentId), executor);
=======
    return CompletableFuture.supplyAsync(() -> deleteDocumentInternal(documentId));
>>>>>>> 2c5476f5 (feat: add LocalStorage document store implementation)
  }

  @Override
  public CompletableFuture<Either<DocumentError, DocumentLink>> createLink(
      final String documentId, final long durationInMillis) {
    return CompletableFuture.completedFuture(
        Either.left(
            new OperationNotSupported(
                "The local storage document store does not support creating links")));
  }

  @Override
  public CompletableFuture<Either<DocumentError, Void>> verifyContentHash(
      final String documentId, final String contentHash) {
<<<<<<< HEAD
    return CompletableFuture.supplyAsync(
        () -> verifyContentHashInternal(documentId, contentHash), executor);
=======
    return CompletableFuture.supplyAsync(() -> verifyContentHashInternal(documentId, contentHash));
>>>>>>> 2c5476f5 (feat: add LocalStorage document store implementation)
  }

  private Either<DocumentError, DocumentReference> createDocumentInternal(
      final DocumentCreationRequest request) {
    final String documentId = getDocumentId(request);
    final Path documentFilePath = storagePath.resolve(documentId);
    final Path documentMetaDataFilePath = storagePath.resolve(documentId + METADATA_SUFFIX);

    if (fileHandler.fileExists(documentFilePath)
        || fileHandler.fileExists(documentMetaDataFilePath)) {
      return Either.left(new DocumentAlreadyExists(documentId));
    }

<<<<<<< HEAD
    final HashResult hashResult = DocumentHashProcessor.hash(request.contentInputStream());
=======
    final HashResult hashResult = DocumentHashProcessor.hash(request.contentInputStream(), SHA_256);
>>>>>>> 2c5476f5 (feat: add LocalStorage document store implementation)

    try (final InputStream stream = hashResult.inputStream();
        final InputStream metadataStream =
            new ByteArrayInputStream(mapper.writeValueAsBytes(request.metadata()))) {
      fileHandler.createFile(stream, documentFilePath);
      fileHandler.createFile(metadataStream, documentMetaDataFilePath);
    } catch (final IOException e) {
      return Either.left(new UnknownDocumentError(e));
    }
    return Either.right(
        new DocumentReference(request.documentId(), hashResult.contentHash(), request.metadata()));
  }

  private Either<DocumentError, DocumentContent> getDocumentInternal(final String documentId) {
    final Path documentPath = storagePath.resolve(documentId);
    final Path documentMetadataPath = storagePath.resolve(documentId + METADATA_SUFFIX);

    if (!fileHandler.fileExists(documentPath) || !fileHandler.fileExists(documentMetadataPath)) {
      return Either.left(new DocumentNotFound(documentId));
    }

    try {
      final InputStream inputStream = fileHandler.getInputStream(documentPath);
      final DocumentMetadataModel metadataModel =
          mapper.readValue(documentMetadataPath.toFile(), DocumentMetadataModel.class);

      return Either.right(new DocumentContent(inputStream, metadataModel.contentType()));
    } catch (final IOException e) {
      return Either.left(
          new UnknownDocumentError(
              "Error occurred while retrieving document ID: " + documentId, e));
    }
  }

  private Either<DocumentError, Void> deleteDocumentInternal(final String documentId) {
    final Path documentPath = storagePath.resolve(documentId);
    final Path documentMetadataPath = storagePath.resolve(documentId + METADATA_SUFFIX);

    try {
      fileHandler.delete(documentPath);
      fileHandler.delete(documentMetadataPath);
    } catch (final IOException e) {
      return Either.left(new UnknownDocumentError(e));
    }
    return Either.right(null);
  }

  private Either<DocumentError, Void> verifyContentHashInternal(
      final String documentId, final String contentHash) {
    final Path documentPath = storagePath.resolve(documentId);

    if (!fileHandler.fileExists(documentPath)) {
      return Either.left(new DocumentNotFound(documentId));
    }

    try (final InputStream inputStream = fileHandler.getInputStream(documentPath)) {
<<<<<<< HEAD
      final var result = DocumentHashProcessor.hash(inputStream);
=======
      final var result = DocumentHashProcessor.hash(inputStream, SHA_256);
>>>>>>> 2c5476f5 (feat: add LocalStorage document store implementation)
      if (!result.contentHash().equals(contentHash)) {
        return Either.left(new DocumentHashMismatch(documentId, contentHash));
      }
    } catch (final IOException e) {
      return Either.left(new UnknownDocumentError(e));
    }
    return Either.right(null);
  }

  private static String getDocumentId(final DocumentCreationRequest request) {
    return request.documentId() == null ? UUID.randomUUID().toString() : request.documentId();
  }
}
