/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.backup.filesystem;

import io.camunda.zeebe.backup.api.BackupIdentifier;
import io.camunda.zeebe.backup.api.NamedFileSet;
import io.camunda.zeebe.backup.common.FileSet;
import io.camunda.zeebe.backup.common.FileSet.NamedFile;
import io.camunda.zeebe.backup.common.NamedFileSetImpl;
import io.camunda.zeebe.util.FileUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

final class FileSetManager {

  // The path format is constructed by basePath/contents/partitionId/checkpointId/nodeId/nameOfFile
  private static final String PATH_FORMAT = "%s/contents/%s/%s/%s/%s/";
  private final String basePath;

  FileSetManager(final String basePath) {
    this.basePath = basePath;
  }

  void save(final BackupIdentifier id, final String fileSetName, final NamedFileSet fileSet) {
    final Path fileSetPath = fileSetPath(id, fileSetName);

    try {
      FileUtil.ensureDirectoryExists(fileSetPath);
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to create backup directory", e);
    }

    for (final var namedFile : fileSet.namedFiles().entrySet()) {
      final var fileName = namedFile.getKey();
      final var filePath = namedFile.getValue();

      final Path targetFilePath = fileSetPath.resolve(fileName);
      final Path sourceFilePath = Paths.get(String.valueOf(filePath));
      try {
        Files.copy(sourceFilePath, targetFilePath);
        FileUtil.flush(fileSetPath);
      } catch (final IOException e) {
        throw new UncheckedIOException("Unable to copy file " + sourceFilePath, e);
      }
    }
  }

  public void delete(final BackupIdentifier id, final String fileSetName) {
    final Path fileSetPath = fileSetPath(id, fileSetName);
    try {
      FileUtil.deleteFolderIfExists(fileSetPath);
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to delete directory " + fileSetPath, e);
    }
  }

  public NamedFileSet restore(
      final BackupIdentifier id,
      final String fileSetName,
      final FileSet fileSet,
      final Path targetFolder) {

    final var pathByName =
        fileSet.files().stream()
            .collect(Collectors.toMap(NamedFile::name, f -> targetFolder.resolve(f.name())));

    final Path fileSetPath = fileSetPath(id, fileSetName);
    for (final var entry : pathByName.entrySet()) {
      final var fileName = entry.getKey();
      final var filePath = entry.getValue();
      final var backupFilePath = fileSetPath.resolve(fileName);

      try {
        Files.copy(backupFilePath, filePath, StandardCopyOption.REPLACE_EXISTING);
      } catch (final IOException e) {
        throw new UncheckedIOException("Unable to restore file " + fileName, e);
      }
    }
    try {
      FileUtil.flushDirectory(fileSetPath);
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to flush directory " + fileSetPath, e);
    }

    return new NamedFileSetImpl(pathByName);
  }

  private Path fileSetPath(final BackupIdentifier id, final String fileSetName) {
    return Path.of(
        PATH_FORMAT.formatted(
            basePath, id.partitionId(), id.checkpointId(), id.nodeId(), fileSetName));
  }
}
