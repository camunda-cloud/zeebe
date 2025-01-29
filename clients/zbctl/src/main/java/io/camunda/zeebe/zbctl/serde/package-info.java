/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
@Json.Import(Topology.class)
@Json.Import(BrokerInfo.class)
@Json.Import(PartitionInfo.class)
@Json.Import(PublishMessageResponse.class)
@Json.Import(ProcessInstanceEvent.class)
@Json.Import(ProcessInstanceResult.class)
@Json.Import(DeploymentEvent.class)
@Json.Import(Process.class)
@Json.Import(Decision.class)
@Json.Import(DecisionRequirements.class)
@Json.Import(Form.class)
@Json.Import(ActivatedJob.class)
@Json.Import(Resource.class)
@Json.Import(ProblemDetail.class)
@Json.Import(ReflectConfigGenerator.ClassReflectionConfig.class)
@Json.Import(ReflectConfigGenerator.MethodReflectionConfig.class)
@Json.Import(ActivateJobsResponse.class)
@Json.Import(CreateTenantResponse.class)
package io.camunda.zeebe.zbctl.serde;

import io.avaje.jsonb.Json;
import io.camunda.client.api.response.ActivateJobsResponse;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.BrokerInfo;
import io.camunda.client.api.response.CreateTenantResponse;
import io.camunda.client.api.response.Decision;
import io.camunda.client.api.response.DecisionRequirements;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.Form;
import io.camunda.client.api.response.PartitionInfo;
import io.camunda.client.api.response.Process;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.response.ProcessInstanceResult;
import io.camunda.client.api.response.PublishMessageResponse;
import io.camunda.client.api.response.Resource;
import io.camunda.client.api.response.Topology;
import io.camunda.client.protocol.rest.ProblemDetail;
import io.camunda.zeebe.zbctl.config.ReflectConfigGenerator;
