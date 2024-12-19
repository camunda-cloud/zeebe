/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.spring.client.jobhandling.parameter;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;

public class VariablesAsTypeResolver implements ParameterResolver {
  private final Class<?> variablesType;

  public VariablesAsTypeResolver(final Class<?> variablesType) {
    this.variablesType = variablesType;
  }

  @Override
  public Object resolve(final JobClient jobClient, final ActivatedJob job) {
    return job.getVariablesAsType(variablesType);
  }
}
