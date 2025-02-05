/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

<<<<<<< HEAD
import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.response.DocumentReferenceResponse;
import io.camunda.qa.util.cluster.TestStandaloneCamunda;
=======
import io.camunda.qa.util.cluster.TestStandaloneCamunda;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.ProblemException;
import io.camunda.zeebe.client.api.response.DocumentReferenceResponse;
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ZeebeIntegration
public class DeleteDocumentTest {

  private static final String DOCUMENT_CONTENT = "test";

<<<<<<< HEAD
  private static CamundaClient camundaClient;
=======
  private static ZeebeClient zeebeClient;
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)

  @TestZeebe(initMethod = "initTestStandaloneCamunda")
  private static TestStandaloneCamunda testStandaloneCamunda;

  private DocumentReferenceResponse documentReference;

  @SuppressWarnings("unused")
  static void initTestStandaloneCamunda() {
<<<<<<< HEAD
    testStandaloneCamunda = new TestStandaloneCamunda().withUnauthenticatedAccess();
=======
    testStandaloneCamunda = new TestStandaloneCamunda();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
  }

  @BeforeEach
  public void beforeEach() {
<<<<<<< HEAD
    camundaClient = testStandaloneCamunda.newClientBuilder().build();
    documentReference =
        camundaClient.newCreateDocumentCommand().content(DOCUMENT_CONTENT).send().join();
=======
    zeebeClient = testStandaloneCamunda.newClientBuilder().build();
    documentReference =
        zeebeClient.newCreateDocumentCommand().content(DOCUMENT_CONTENT).send().join();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
  }

  @Test
  public void shouldWorkWithDocumentId() {
    // given
    final var documentId = documentReference.getDocumentId();
<<<<<<< HEAD
    camundaClient = testStandaloneCamunda.newClientBuilder().build();

    // when
    camundaClient.newDeleteDocumentCommand(documentId).send().join();
=======
    zeebeClient = testStandaloneCamunda.newClientBuilder().build();

    // when
    zeebeClient.newDeleteDocumentCommand(documentId).send().join();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)

    // then
    assertDocumentIsDeleted(documentId);
  }

  @Test
  public void shouldWorkWithDocumentIdAndStoreId() {
    // given
    final var documentId = documentReference.getDocumentId();
    final var storeId = documentReference.getStoreId();
<<<<<<< HEAD
    camundaClient = testStandaloneCamunda.newClientBuilder().build();

    // when
    camundaClient.newDeleteDocumentCommand(documentId).storeId(storeId).send().join();
=======
    zeebeClient = testStandaloneCamunda.newClientBuilder().build();

    // when
    zeebeClient.newDeleteDocumentCommand(documentId).storeId(storeId).send().join();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)

    // then
    assertDocumentIsDeleted(documentId);
  }

  @Test
  public void shouldWorkWithDocumentReference() {
    // given
<<<<<<< HEAD
    camundaClient = testStandaloneCamunda.newClientBuilder().build();

    // when
    camundaClient.newDeleteDocumentCommand(documentReference).send().join();
=======
    zeebeClient = testStandaloneCamunda.newClientBuilder().build();

    // when
    zeebeClient.newDeleteDocumentCommand(documentReference).send().join();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)

    // then
    assertDocumentIsDeleted(documentReference.getDocumentId());
  }

  @Test
  public void shouldReturnNotFoundIfDocumentDoesNotExist() {
    // given
    final var documentId = "non-existing-document";
<<<<<<< HEAD
    camundaClient = testStandaloneCamunda.newClientBuilder().build();
=======
    zeebeClient = testStandaloneCamunda.newClientBuilder().build();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)

    // when
    final var exception =
        assertThrowsExactly(
            ProblemException.class,
            () -> {
<<<<<<< HEAD
              camundaClient.newDeleteDocumentCommand(documentId).send().join();
=======
              zeebeClient.newDeleteDocumentCommand(documentId).send().join();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
            });

    // then
    assertThat(exception.details().getStatus()).isEqualTo(404);
    assertThat(exception.details().getDetail())
        .isEqualTo("Document with id 'non-existing-document' not found");
  }

  @Test
  public void shouldReturnBadRequestForNonExistingStoreId() {
    // given
<<<<<<< HEAD
    camundaClient = testStandaloneCamunda.newClientBuilder().build();
=======
    zeebeClient = testStandaloneCamunda.newClientBuilder().build();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
    final var documentContent = "test";
    final var storeId = "non-existing";

    // when
    final var command =
<<<<<<< HEAD
        camundaClient.newCreateDocumentCommand().content(documentContent).storeId(storeId).send();
=======
        zeebeClient.newCreateDocumentCommand().content(documentContent).storeId(storeId).send();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)

    // then
    final var exception = assertThrowsExactly(ProblemException.class, command::join);
    assertThat(exception.getMessage()).startsWith("Failed with code 400");
    assertThat(exception.details()).isNotNull();
    assertThat(exception.details().getStatus()).isEqualTo(400);
    assertThat(exception.details().getDetail())
        .isEqualTo("Document store with id 'non-existing' does not exist");
  }

  private void assertDocumentIsDeleted(final String documentId) {
    final var exception =
        assertThrowsExactly(
            ProblemException.class,
<<<<<<< HEAD
            () -> camundaClient.newDocumentContentGetRequest(documentId).send().join());
=======
            () -> zeebeClient.newDocumentContentGetRequest(documentId).send().join());
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
    assertThat(exception.details().getStatus()).isEqualTo(404);
  }
}
