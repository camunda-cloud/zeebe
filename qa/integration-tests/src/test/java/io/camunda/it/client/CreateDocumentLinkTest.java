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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@ZeebeIntegration
public class CreateDocumentLinkTest {

  private static final String DOCUMENT_CONTENT = "test";

<<<<<<< HEAD
  private static CamundaClient camundaClient;
=======
  private static ZeebeClient zeebeClient;
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
  private static DocumentReferenceResponse documentReference;

  @TestZeebe(initMethod = "initTestStandaloneCamunda")
  private static TestStandaloneCamunda testStandaloneCamunda;

  @SuppressWarnings("unused")
  static void initTestStandaloneCamunda() {
<<<<<<< HEAD
    testStandaloneCamunda = new TestStandaloneCamunda().withUnauthenticatedAccess();
=======
    testStandaloneCamunda = new TestStandaloneCamunda();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
  }

  @BeforeAll
  public static void beforeAll() {
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
  public void shouldReturnBadRequestWhenDocumentStoreDoesNotExist() {
    // given
    final var storeId = "invalid-document-store-id";
<<<<<<< HEAD
    camundaClient = testStandaloneCamunda.newClientBuilder().build();
=======
    zeebeClient = testStandaloneCamunda.newClientBuilder().build();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)

    // when
    final var exception =
        assertThrowsExactly(
            ProblemException.class,
            () ->
<<<<<<< HEAD
                camundaClient
=======
                zeebeClient
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
                    .newCreateDocumentLinkCommand(documentReference)
                    .storeId(storeId)
                    .send()
                    .join());

    // then
    assertThat(exception.getMessage()).startsWith("Failed with code 400");
    assertThat(exception.details().getStatus()).isEqualTo(400);
    assertThat(exception.details().getDetail())
        .contains("Document store with id 'invalid-document-store-id' does not exist");
  }

  @Test
  public void shouldReturnMethodNotAllowedWhenStoreIsInMemory() {
    // given
    final var storeId = "in-memory";
    final var documentId = documentReference.getDocumentId();
<<<<<<< HEAD
    camundaClient = testStandaloneCamunda.newClientBuilder().build();
=======
    zeebeClient = testStandaloneCamunda.newClientBuilder().build();
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)

    // when
    final var exception =
        assertThrowsExactly(
            ProblemException.class,
            () ->
<<<<<<< HEAD
                camundaClient
=======
                zeebeClient
>>>>>>> 26923896 (feat: add Create and Get document command to zeebe client)
                    .newCreateDocumentLinkCommand(documentId)
                    .storeId(storeId)
                    .contentHash(documentReference.getContentHash())
                    .send()
                    .join());

    // then
    assertThat(exception.getMessage()).startsWith("Failed with code 405");
    assertThat(exception.details().getStatus()).isEqualTo(405);
    assertThat(exception.details().getDetail())
        .contains("The in-memory document store does not support creating links");
  }
}
