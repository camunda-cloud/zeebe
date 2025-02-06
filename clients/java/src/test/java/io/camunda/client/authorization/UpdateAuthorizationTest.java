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
package io.camunda.client.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.client.protocol.rest.AuthorizationRequest;
import io.camunda.client.protocol.rest.OwnerTypeEnum;
import io.camunda.client.protocol.rest.PermissionTypeEnum;
import io.camunda.client.protocol.rest.ResourceTypeEnum;
import io.camunda.client.util.ClientRestTest;
import org.junit.jupiter.api.Test;

public class UpdateAuthorizationTest extends ClientRestTest {

  @Test
  void shouldSendCommand() {
    // when
    client
        .newUpdateAuthorizationCommand(1L)
        .ownerId("ownerId")
        .ownerType(OwnerTypeEnum.USER)
        .resourceId("resourceId")
        .resourceType(ResourceTypeEnum.RESOURCE)
        .permissionTypes(PermissionTypeEnum.CREATE, PermissionTypeEnum.READ)
        .send()
        .join();

    // then
    final AuthorizationRequest request = gatewayService.getLastRequest(AuthorizationRequest.class);
    assertThat(request.getOwnerId()).isEqualTo("ownerId");
    assertThat(request.getOwnerType()).isEqualTo(OwnerTypeEnum.USER);
    assertThat(request.getResourceId()).isEqualTo("resourceId");
    assertThat(request.getResourceType()).isEqualTo(ResourceTypeEnum.RESOURCE);
    assertThat(request.getPermissions())
        .containsExactly(PermissionTypeEnum.CREATE, PermissionTypeEnum.READ);
  }

  @Test
  void shouldRaiseExceptionOnNullOwnerId() {
    // when then
    assertThatThrownBy(
            () ->
                client
                    .newUpdateAuthorizationCommand(1L)
                    .ownerId(null)
                    .ownerType(OwnerTypeEnum.USER)
                    .resourceId("resourceId")
                    .resourceType(ResourceTypeEnum.RESOURCE)
                    .permissionTypes(PermissionTypeEnum.CREATE, PermissionTypeEnum.READ)
                    .send()
                    .join())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ownerId must not be null");
  }

  @Test
  void shouldRaiseExceptionOnEmptyOwnerId() {
    // when then
    assertThatThrownBy(
            () ->
                client
                    .newUpdateAuthorizationCommand(1L)
                    .ownerId("")
                    .ownerType(OwnerTypeEnum.USER)
                    .resourceId("resourceId")
                    .resourceType(ResourceTypeEnum.RESOURCE)
                    .permissionTypes(PermissionTypeEnum.CREATE, PermissionTypeEnum.READ)
                    .send()
                    .join())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ownerId must not be empty");
  }

  @Test
  void shouldRaiseExceptionOnNullOwnerType() {
    // when then
    assertThatThrownBy(
            () ->
                client
                    .newUpdateAuthorizationCommand(1L)
                    .ownerId("ownerId")
                    .ownerType(null)
                    .resourceId("resourceId")
                    .resourceType(ResourceTypeEnum.RESOURCE)
                    .permissionTypes(PermissionTypeEnum.CREATE, PermissionTypeEnum.READ)
                    .send()
                    .join())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ownerType must not be null");
  }

  @Test
  void shouldRaiseExceptionOnNullResourceId() {
    // when then
    assertThatThrownBy(
            () ->
                client
                    .newUpdateAuthorizationCommand(1L)
                    .ownerId("ownerId")
                    .ownerType(OwnerTypeEnum.USER)
                    .resourceId(null)
                    .resourceType(ResourceTypeEnum.RESOURCE)
                    .permissionTypes(PermissionTypeEnum.CREATE, PermissionTypeEnum.READ)
                    .send()
                    .join())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("resourceId must not be null");
  }

  @Test
  void shouldRaiseExceptionOnEmptyResourceId() {
    // when then
    assertThatThrownBy(
            () ->
                client
                    .newUpdateAuthorizationCommand(1L)
                    .ownerId("ownerId")
                    .ownerType(OwnerTypeEnum.USER)
                    .resourceId("")
                    .resourceType(ResourceTypeEnum.RESOURCE)
                    .permissionTypes(PermissionTypeEnum.CREATE, PermissionTypeEnum.READ)
                    .send()
                    .join())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("resourceId must not be empty");
  }

  @Test
  void shouldRaiseExceptionOnNullResourceType() {
    // when then
    assertThatThrownBy(
            () ->
                client
                    .newUpdateAuthorizationCommand(1L)
                    .ownerId("ownerId")
                    .ownerType(OwnerTypeEnum.USER)
                    .resourceId("resourceId")
                    .resourceType(null)
                    .permissionTypes(PermissionTypeEnum.CREATE, PermissionTypeEnum.READ)
                    .send()
                    .join())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("resourceType must not be null");
  }

  @Test
  void shouldRaiseExceptionOnNullPermissions() {
    // when then
    assertThatThrownBy(
            () ->
                client
                    .newUpdateAuthorizationCommand(1L)
                    .ownerId("ownerId")
                    .ownerType(OwnerTypeEnum.USER)
                    .resourceId("resourceId")
                    .resourceType(ResourceTypeEnum.RESOURCE)
                    .permissionTypes(null)
                    .send()
                    .join())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("permissionTypes must not be null");
  }
}
