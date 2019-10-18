/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service;

import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.GroupDto;
import org.camunda.optimize.dto.optimize.IdentityDto;
import org.camunda.optimize.dto.optimize.UserDto;
import org.camunda.optimize.rest.engine.AuthorizedIdentitiesResult;
import org.camunda.optimize.rest.engine.EngineContext;
import org.camunda.optimize.rest.engine.EngineContextFactory;
import org.camunda.optimize.service.util.configuration.ConfigurationReloadable;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.camunda.optimize.service.util.configuration.engine.UserSyncConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SyncedIdentityCacheService implements ConfigurationReloadable {

  private SearchableIdentityCache activeIdentityCache;

  private final ConfigurationService configurationService;
  private final EngineContextFactory engineContextFactory;

  private ThreadPoolTaskScheduler taskScheduler;
  private ScheduledFuture<?> scheduledTrigger;

  public SyncedIdentityCacheService(final ConfigurationService configurationService,
                                    final EngineContextFactory engineContextFactory) {
    this.configurationService = configurationService;
    this.engineContextFactory = engineContextFactory;
    this.activeIdentityCache = new SearchableIdentityCache();
  }

  @Override
  public void reloadConfiguration(final ApplicationContext context) {
    resetCache();
  }

  @PostConstruct
  public void init() {
    log.info("Initializing user sync.");
    final UserSyncConfiguration userSyncConfiguration = getUserSyncConfiguration();
    userSyncConfiguration.validate();
    if (userSyncConfiguration.isEnabled()) {
      startSchedulingUserSync();
    }
  }

  public synchronized void startSchedulingUserSync() {
    log.info("Scheduling User Sync");
    if (this.taskScheduler == null) {
      this.taskScheduler = new ThreadPoolTaskScheduler();
      this.taskScheduler.initialize();
      this.taskScheduler.submit(this::synchronizeIdentities);
    }
    if (this.scheduledTrigger == null) {
      this.scheduledTrigger = this.taskScheduler.schedule(this::synchronizeIdentities, getCronTrigger());
    }
  }

  @PreDestroy
  public synchronized void stopSchedulingUserSync() {
    log.info("Stop scheduling user sync.");
    if (scheduledTrigger != null) {
      this.scheduledTrigger.cancel(true);
      this.scheduledTrigger = null;
    }
    if (this.taskScheduler != null) {
      this.taskScheduler.destroy();
      this.taskScheduler = null;
    }
  }

  public boolean isScheduledToRun() {
    return this.scheduledTrigger != null;
  }

  public synchronized void synchronizeIdentities() {
    final SearchableIdentityCache newIdentityCache = new SearchableIdentityCache();
    engineContextFactory.getConfiguredEngines()
      .forEach(engineContext -> populateAllAuthorizedIdentitiesForEngineToCache(engineContext, newIdentityCache));
    replaceActiveCache(newIdentityCache);
  }

  public void addIdentity(final IdentityDto identity) {
    activeIdentityCache.addIdentity(identity);
  }

  public Optional<UserDto> getUserIdentityById(final String id) {
    return activeIdentityCache.getUserIdentityById(id);
  }

  public Optional<GroupDto> getGroupIdentityById(final String id) {
    return activeIdentityCache.getGroupIdentityById(id);
  }

  public List<IdentityDto> searchIdentities(final String terms, final int resultLimit) {
    return activeIdentityCache.searchIdentities(terms, resultLimit);
  }

  private synchronized void replaceActiveCache(final SearchableIdentityCache newIdentityCache) {
    final SearchableIdentityCache previousIdentityCache = activeIdentityCache;
    this.activeIdentityCache = newIdentityCache;
    previousIdentityCache.close();
  }

  private void populateAllAuthorizedIdentitiesForEngineToCache(final EngineContext engineContext,
                                                               final SearchableIdentityCache identityCache) {
    final AuthorizedIdentitiesResult authorizedIdentities = engineContext.getApplicationAuthorizedIdentities();

    if (authorizedIdentities.isGlobalOptimizeGrant()) {
      populateGlobalAccessGrantedIdentitiesToCache(engineContext, identityCache, authorizedIdentities);
    } else {
      populateGrantedIdentitiesToCache(engineContext, identityCache, authorizedIdentities);
    }
  }

  private void populateGlobalAccessGrantedIdentitiesToCache(final EngineContext engineContext,
                                                            final SearchableIdentityCache identityCache,
                                                            final AuthorizedIdentitiesResult authorizedIdentities) {
    // add all non revoked groups
    consumeIdentitiesInBatches(
      identityCache::addIdentities,
      engineContext::fetchPageOfGroups,
      groupDto -> !authorizedIdentities.getRevokedGroupIds().contains(groupDto.getId())
        && !identityCache.getGroupIdentityById(groupDto.getId()).isPresent()
    );

    // collect all members of revoked groups to exclude them when adding all other users
    final Set<String> userIdsOfRevokedGroupMembers = authorizedIdentities.getRevokedGroupIds().stream()
      .flatMap(revokedGroupId -> {
        final Set<IdentityDto> currentGroupIdentities = new HashSet<>();
        consumeIdentitiesInBatches(
          currentGroupIdentities::addAll,
          (pageStartIndex, pageLimit) -> engineContext.fetchPageOfUsers(pageStartIndex, pageLimit, revokedGroupId),
          userDto -> true
        );
        return currentGroupIdentities.stream();
      })
      .map(IdentityDto::getId)
      .collect(Collectors.toSet());

    // then iterate all users... that's the price you pay for global access
    consumeIdentitiesInBatches(
      identityCache::addIdentities,
      engineContext::fetchPageOfUsers,
      userDto ->
        // add them if explicitly granted access
        authorizedIdentities.getGrantedUserIds().contains(userDto.getId())
          // @formatter:off
          // or if they are not member of a revoked access group nor revoked by user id
          || (
            !userIdsOfRevokedGroupMembers.contains(userDto.getId())
              && !authorizedIdentities.getRevokedUserIds().contains(userDto.getId())
          )
          // @formatter:on
    );
  }

  private void populateGrantedIdentitiesToCache(final EngineContext engineContext,
                                                final SearchableIdentityCache identityCache,
                                                final AuthorizedIdentitiesResult authorizedIdentities) {
    if (authorizedIdentities.getGrantedGroupIds().size() > 0) {
      // add all granted groups (as group grant wins over group revoke)
      // https://docs.camunda.org/manual/7.11/user-guide/process-engine/authorization-service/#authorization-precedence
      final Set<String> grantedGroupIdsNotYetImported = authorizedIdentities.getGrantedGroupIds().stream()
        .filter(userId -> !identityCache.getGroupIdentityById(userId).isPresent())
        .collect(Collectors.toSet());
      Iterables.partition(grantedGroupIdsNotYetImported, getUserSyncConfiguration().getMaxPageSize())
        .forEach(userIdBatch -> identityCache.addIdentities(engineContext.getGroupsById(userIdBatch)));

      // add all members of the authorized groups (as group grants win over group revoke) except explicit revoked users
      authorizedIdentities.getGrantedGroupIds()
        .forEach(groupId -> {
          consumeIdentitiesInBatches(
            identityCache::addIdentities,
            (pageStartIndex, pageLimit) -> engineContext.fetchPageOfUsers(pageStartIndex, pageLimit, groupId),
            userDto -> !authorizedIdentities.getRevokedUserIds().contains(userDto.getId())
          );
        });
    }

    // finally add explicitly granted users, not yet in the cache already
    final Set<String> grantedUserIdsNotYetImported = authorizedIdentities.getGrantedUserIds().stream()
      .filter(userId -> !identityCache.getUserIdentityById(userId).isPresent())
      .collect(Collectors.toSet());
    Iterables.partition(grantedUserIdsNotYetImported, getUserSyncConfiguration().getMaxPageSize())
      .forEach(userIdBatch -> identityCache.addIdentities(engineContext.getUsersById(userIdBatch)));
  }

  private <T extends IdentityDto> void consumeIdentitiesInBatches(final Consumer<List<IdentityDto>> identityBatchConsumer,
                                                                  final GetIdentityPageMethod<T> getIdentityPage,
                                                                  final Predicate<T> identityFilter) {
    final int maxPageSize = getUserSyncConfiguration().getMaxPageSize();
    int currentIndex = 0;
    List<T> currentPage;
    do {
      currentPage = getIdentityPage.getPageOfIdentities(currentIndex, maxPageSize);
      currentIndex += currentPage.size();
      final List<IdentityDto> identities = currentPage.stream()
        .filter(identityFilter)
        .collect(Collectors.toList());
      identityBatchConsumer.accept(identities);
    } while (currentPage.size() >= maxPageSize);
  }

  private synchronized void resetCache() {
    if (activeIdentityCache != null) {
      activeIdentityCache.close();
      activeIdentityCache = new SearchableIdentityCache();
    }
  }

  private UserSyncConfiguration getUserSyncConfiguration() {
    return this.configurationService.getUserSyncConfiguration();
  }

  private CronTrigger getCronTrigger() {
    return new CronTrigger(getUserSyncConfiguration().getCronTrigger());
  }

  @FunctionalInterface
  private interface GetIdentityPageMethod<T extends IdentityDto> {
    List<T> getPageOfIdentities(int pageStartIndex, int pageLimit);
  }
}
