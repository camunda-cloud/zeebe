/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

  // Used to delay construction of UserService bean
  @Autowired BeanFactory beanFactory;

  @Override
  protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
      Authentication authentication, MethodInvocation invocation) {
    final CustomSecurityExpressionRoot root = new CustomSecurityExpressionRoot(authentication);
    root.setUserService(getUserService());
    root.setThis(invocation.getThis());
    root.setPermissionEvaluator(getPermissionEvaluator());
    root.setTrustResolver(getTrustResolver());
    root.setRoleHierarchy(getRoleHierarchy());
    root.setDefaultRolePrefix(getDefaultRolePrefix());
    return root;
  }

  private UserService<? extends Authentication> getUserService() {
    return beanFactory.getBean(UserService.class);
  }
}
