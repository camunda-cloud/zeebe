/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
@Json.Import(Topology.class)
@Json.Import(BrokerInfo.class)
@Json.Import(PartitionInfo.class)
@Json.Import(PublishMessageResponse.class)
package io.camunda.zeebe.zbctl.serde;

import io.avaje.jsonb.Json;
import io.camunda.zeebe.client.api.response.BrokerInfo;
import io.camunda.zeebe.client.api.response.PartitionInfo;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.client.api.response.Topology;
