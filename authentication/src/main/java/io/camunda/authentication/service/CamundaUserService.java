/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.authentication.service;

import io.camunda.authentication.AuthProfile;
import io.camunda.authentication.entity.CamundaUserDTO;
import javax.annotation.Nullable;

public interface CamundaUserService {
  AuthProfile getProfile();

  @Nullable
  CamundaUserDTO getCurrentUser();

  String getUserToken();
}
