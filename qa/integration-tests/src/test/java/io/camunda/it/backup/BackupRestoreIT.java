/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import feign.FeignException.NotFound;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.response.ProcessInstanceState;
import io.camunda.it.utils.MultiDbConfigurator;
import io.camunda.management.backups.StateCode;
import io.camunda.management.backups.TakeBackupHistoryResponse;
import io.camunda.qa.util.cluster.HistoryBackupClient;
import io.camunda.qa.util.cluster.TestSimpleCamundaApplication;
import io.camunda.search.connect.configuration.DatabaseType;
import io.camunda.security.entity.AuthenticationMethod;
import io.camunda.webapps.backup.BackupStateDto;
import io.camunda.zeebe.broker.system.configuration.BrokerCfg;
import io.camunda.zeebe.broker.system.configuration.backup.BackupStoreCfg.BackupStoreType;
import io.camunda.zeebe.qa.util.actuator.BackupActuator;
import io.camunda.zeebe.qa.util.actuator.ExportingActuator;
import io.camunda.zeebe.qa.util.cluster.TestRestoreApp;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneApplication;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.test.testcontainers.AzuriteContainer;
import io.camunda.zeebe.test.util.testcontainers.ContainerLogsDumper;
import io.camunda.zeebe.test.util.testcontainers.TestSearchContainers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.agrona.CloseHelper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ZeebeIntegration
public class BackupRestoreIT {
  private static final String REPOSITORY_NAME = "test-repository";
  private static final String INDEX_PREFIX = "backup-restore";
  private static final String PROCESS_ID = "backup-process";
  private static final long BACKUP_ID = 3L;
  private static final int PROCESS_INSTANCE_NUMBER = 10;
  protected CamundaClient camundaClient;
  protected ExportingActuator exportingActuator;
  protected BackupActuator backupActuator;

  @TestZeebe(autoStart = false)
  protected TestStandaloneApplication<?> testStandaloneApplication;

  protected BackupDBClient backupDbClient;
  @Container private final AzuriteContainer azuriteContainer = new AzuriteContainer();

  @RegisterExtension
  @SuppressWarnings("unused")
  final ContainerLogsDumper logsWatcher =
      new ContainerLogsDumper(() -> Map.of("azurite", azuriteContainer));

  private String dbUrl;
  // cannot be a @Container because it's initialized in setup()
  private GenericContainer<?> searchContainer;
  private DataGenerator generator;
  private HistoryBackupClient historyBackupClient;

  @AfterEach
  public void tearDown() {
    CloseHelper.quietCloseAll(backupDbClient, camundaClient, generator, searchContainer);
  }

  private void setup(final BackupRestoreTestConfig config) throws Exception {
    testStandaloneApplication =
        new TestSimpleCamundaApplication().withAuthenticationMethod(AuthenticationMethod.BASIC);
    final var configurator = new MultiDbConfigurator(testStandaloneApplication);
    testStandaloneApplication.withBrokerConfig(this::configureZeebeBackupStore);
    searchContainer =
        switch (config.databaseType) {
          case ELASTICSEARCH -> {
            final var container =
                TestSearchContainers.createDefeaultElasticsearchContainer()
                    .withStartupTimeout(Duration.ofMinutes(5))
                    // location of the repository that will be used for snapshots
                    .withEnv("path.repo", "~/");
            container.start();
            dbUrl = "http://" + container.getHttpHostAddress();

            // configure the app
            configurator.configureElasticsearchSupport(dbUrl, INDEX_PREFIX);
            yield container;
          }

          case OPENSEARCH -> {
            final var container =
                TestSearchContainers.createDefaultOpensearchContainer()
                    .withStartupTimeout(Duration.ofMinutes(5))
                    // location of the repository that will be used for snapshots
                    .withEnv("path.repo", "~/");
            container.start();
            dbUrl = container.getHttpHostAddress();
            configurator.configureOpenSearchSupport(dbUrl, INDEX_PREFIX, "admin", "admin");
            yield container;
          }

          default ->
              throw new IllegalArgumentException(
                  "Unsupported database type: " + config.databaseType);
        };
    configurator.getOperateProperties().getBackup().setRepositoryName(REPOSITORY_NAME);
    configurator.getTasklistProperties().getBackup().setRepositoryName(REPOSITORY_NAME);

    testStandaloneApplication.start().awaitCompleteTopology();

    camundaClient = testStandaloneApplication.newClientBuilder().build();
    exportingActuator = ExportingActuator.of(testStandaloneApplication);
    backupActuator = BackupActuator.of(testStandaloneApplication);

    historyBackupClient = HistoryBackupClient.of(testStandaloneApplication);
    backupDbClient = BackupDBClient.create(dbUrl, config.databaseType);
    backupDbClient.createRepository(REPOSITORY_NAME);
    generator = new DataGenerator(camundaClient, PROCESS_ID, Duration.ofSeconds(50));
  }

  public static Stream<BackupRestoreTestConfig> sources() {
    final var backupRestoreConfigs = new ArrayList<BackupRestoreTestConfig>();
    for (final var db : List.of(DatabaseType.ELASTICSEARCH, DatabaseType.OPENSEARCH)) {
      backupRestoreConfigs.add(new BackupRestoreTestConfig(db, "bucket"));
    }
    return backupRestoreConfigs.stream();
  }

  @ParameterizedTest
  @MethodSource(value = {"sources"})
  public void shouldBackupAndRestoreToPreviousState(final BackupRestoreTestConfig config)
      throws Exception {
    // given
    setup(config);

    testStandaloneApplication.awaitCompleteTopology();
    // generate completed processes
    generator.generateCompletedProcesses(PROCESS_INSTANCE_NUMBER);

    // generate some processes, but do not complete them,
    // we will complete them after the restore
    generator.generateUncompletedProcesses(PROCESS_INSTANCE_NUMBER);

    // BACKUP PROCEDURE
    // Zeebe is soft-paused
    exportingActuator.softPause();

    final var snapshots = takeHistoryBackup();
    takeZeebeBackup();
    exportingActuator.resume();

    // when
    // if we stop all apps and restart elasticsearch
    testStandaloneApplication.stop();

    backupDbClient.deleteAllIndices(INDEX_PREFIX);
    Awaitility.await().untilAsserted(() -> assertThat(backupDbClient.cat()).isEmpty());

    // RESTORE PROCEDURE
    backupDbClient.restore(REPOSITORY_NAME, snapshots);
    restoreZeebe();

    testStandaloneApplication.start();

    generator.verifyAllExported(ProcessInstanceState.ACTIVE);
    // complete the processes that were not terminated before stopping the apps
    generator.completeProcesses(PROCESS_INSTANCE_NUMBER);

    // then
    generator.verifyAllExported(ProcessInstanceState.COMPLETED);
  }

  private void configureZeebeBackupStore(final BrokerCfg cfg) {
    final var backup = cfg.getData().getBackup();
    final var azure = backup.getAzure();

    backup.setStore(BackupStoreType.AZURE);
    azure.setBasePath(azuriteContainer.getContainerName());
    azure.setConnectionString(azuriteContainer.getConnectString());
  }

  private void restoreZeebe() {
    try (final var restoreApp =
        new TestRestoreApp(testStandaloneApplication.brokerConfig()).withBackupId(BACKUP_ID)) {

      assertThatNoException().isThrownBy(restoreApp::start);
    }
  }

  private List<String> takeHistoryBackup() {
    final var takeResponse = historyBackupClient.takeBackup(BACKUP_ID);
    assertThat(takeResponse)
        .extracting(TakeBackupHistoryResponse::getScheduledSnapshots)
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .isNotEmpty();
    System.out.println("ciao");
    final var snapshots = takeResponse.getScheduledSnapshots();

    Awaitility.await("Webapps Backup completed")
        .atMost(Duration.ofSeconds(60))
        .untilAsserted(
            () -> {
              try {
                final var backupResponse = historyBackupClient.getBackup(BACKUP_ID);
                assertThat(backupResponse.getState()).isEqualTo(BackupStateDto.COMPLETED);
                assertThat(backupResponse.getDetails())
                    .allMatch(d -> d.getState().equals("SUCCESS"));
              } catch (final NotFound e) {
                throw new AssertionError("Backup not found:", e);
              }
            });
    return snapshots;
  }

  private void takeZeebeBackup() {
    backupActuator.take(BACKUP_ID);
    Awaitility.await("Zeebe backup completed")
        .untilAsserted(
            () -> {
              try {
                final var status = backupActuator.status(BACKUP_ID);
                assertThat(status.getState()).isEqualTo(StateCode.COMPLETED);
                assertThat(status.getDetails())
                    .allSatisfy(d -> assertThat(d.getState()).isEqualTo(StateCode.COMPLETED));
              } catch (final Exception e) {
                throw new AssertionError("Backup not found", e);
              }
            });
  }

  public record BackupRestoreTestConfig(DatabaseType databaseType, String bucket) {}
}
