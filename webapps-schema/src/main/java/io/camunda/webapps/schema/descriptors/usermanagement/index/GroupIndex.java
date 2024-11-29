/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.descriptors.usermanagement.index;

import io.camunda.webapps.schema.descriptors.usermanagement.UserManagementIndexDescriptor;
import io.camunda.webapps.schema.entities.usermanagement.EntityJoinRelation.EntityJoinRelationFactory;
import io.camunda.webapps.schema.entities.usermanagement.EntityJoinRelation.IdentityJoinRelationshipType;

public class GroupIndex extends UserManagementIndexDescriptor {

  public static final String INDEX_NAME = "groups";
  public static final String INDEX_VERSION = "8.7.0";

  public static final EntityJoinRelationFactory JOIN_RELATION_FACTORY =
      new EntityJoinRelationFactory(
          IdentityJoinRelationshipType.GROUP, IdentityJoinRelationshipType.MEMBER);

  public GroupIndex(final String indexPrefix, final boolean isElasticsearch) {
    super(indexPrefix, isElasticsearch);
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }

  @Override
  public String getVersion() {
    return INDEX_VERSION;
  }
}
