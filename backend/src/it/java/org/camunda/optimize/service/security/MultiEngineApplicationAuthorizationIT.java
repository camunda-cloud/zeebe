/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.security;

import org.camunda.optimize.service.AbstractMultiEngineIT;
import org.camunda.optimize.test.engine.AuthorizationClient;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.camunda.optimize.service.security.EngineAuthenticationProvider.INVALID_CREDENTIALS_ERROR_MESSAGE;
import static org.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class MultiEngineApplicationAuthorizationIT extends AbstractMultiEngineIT {

  public AuthorizationClient defaultAuthorizationClient = new AuthorizationClient(defaultEngineRule);
  public AuthorizationClient secondAuthorizationClient = new AuthorizationClient(secondEngineRule);

  @Test
  public void authorizedByAtLeastOneEngine() {
    // given
    addSecondEngineToConfiguration();
    defaultAuthorizationClient.addKermitUserAndGrantAccessToOptimize();
    defaultEngineRule.addUser("gonzo", "gonzo");
    secondEngineRule.addUser(KERMIT_USER, KERMIT_USER);
    secondEngineRule.grantUserOptimizeAccess("gonzo");

    // when
    Response response = embeddedOptimizeRule.authenticateUserRequest(KERMIT_USER, KERMIT_USER);

    // then
    assertThat(response.getStatus(), is(200));
    String responseEntity = response.readEntity(String.class);
    assertThat(responseEntity, is(notNullValue()));

    response = embeddedOptimizeRule.authenticateUserRequest("gonzo", "gonzo");

    // then
    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void authorizedByOneEngineEvenIfOtherEngineIsDown() {
    // given
    addNonExistingSecondEngineToConfiguration();
    defaultAuthorizationClient.addKermitUserAndGrantAccessToOptimize();

    // when
    Response response = embeddedOptimizeRule.authenticateUserRequest(KERMIT_USER, KERMIT_USER);

    // then
    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void authorizedByOneEngineEvenIfCredentialsAreWrongForOtherEngine() {
    // given
    addSecondEngineToConfiguration();
    defaultEngineRule.addUser(KERMIT_USER, "123");
    defaultEngineRule.grantUserOptimizeAccess(KERMIT_USER);
    secondAuthorizationClient.addKermitUserAndGrantAccessToOptimize();

    // when
    Response response1 = embeddedOptimizeRule.authenticateUserRequest(KERMIT_USER, "123");
    secondEngineRule.unlockUser(KERMIT_USER);
    Response response2 = embeddedOptimizeRule.authenticateUserRequest(KERMIT_USER, KERMIT_USER);

    // then
    assertThat(response1.getStatus(), is(200));
    assertThat(response2.getStatus(), is(200));
  }

  @Test
  public void rejectWrongPasswordForAllEngines() {
    // given
    addSecondEngineToConfiguration();
    defaultAuthorizationClient.addKermitUserAndGrantAccessToOptimize();
    secondAuthorizationClient.addKermitUserAndGrantAccessToOptimize();

    // when
    Response response = embeddedOptimizeRule.authenticateUserRequest(KERMIT_USER, "wrongPassword");

    // then
    assertThat(response.getStatus(), is(401));
    String responseEntity = response.readEntity(String.class);
    assertThat(responseEntity, containsString(INVALID_CREDENTIALS_ERROR_MESSAGE));
  }

}
