/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.reader;

import static io.camunda.optimize.service.db.DatabaseConstants.INSTANT_DASHBOARD_INDEX_NAME;
import static java.lang.String.format;

import io.camunda.optimize.dto.optimize.query.dashboard.InstantDashboardDataDto;
import io.camunda.optimize.service.db.os.OptimizeOpenSearchClient;
import io.camunda.optimize.service.db.reader.InstantDashboardMetadataReader;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.configuration.condition.OpenSearchCondition;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.core.GetResponse;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(OpenSearchCondition.class)
public class InstantDashboardMetadataReaderOS implements InstantDashboardMetadataReader {

  private final OptimizeOpenSearchClient osClient;

  public InstantDashboardMetadataReaderOS(OptimizeOpenSearchClient osClient) {
    this.osClient = osClient;
  }

  @Override
  public Optional<String> getInstantDashboardIdFor(
      final String processDefinitionKey, final String template) throws OptimizeRuntimeException {
    log.debug(
        "Fetching Instant preview dashboard ID for [{}] with template [{}] ",
        processDefinitionKey,
        template);

    InstantDashboardDataDto dashboardDataDto = new InstantDashboardDataDto();
    dashboardDataDto.setTemplateName(template);
    dashboardDataDto.setProcessDefinitionKey(processDefinitionKey);

    final String instantDashboardKey = dashboardDataDto.getInstantDashboardId();

    GetResponse<InstantDashboardDataDto> getResponse =
        osClient.get(
            INSTANT_DASHBOARD_INDEX_NAME,
            instantDashboardKey,
            InstantDashboardDataDto.class,
            format("Could not fetch Instant preview dashboard with key [%s]", instantDashboardKey));

    if (getResponse.found()) {
      return Optional.of(getResponse.source().getDashboardId());
    } else {
      String reason =
          "Could not find dashboard data for key [" + instantDashboardKey + "] in Opensearch.";
      log.error(reason);
      return Optional.empty();
    }
  }
}
