/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.system.partitions.impl;

import static java.util.Objects.requireNonNull;

import io.atomix.raft.RaftServer.Role;
import io.camunda.zeebe.broker.Loggers;
import io.camunda.zeebe.broker.system.partitions.PartitionTransition;
import io.camunda.zeebe.broker.system.partitions.PartitionTransitionContext;
import io.camunda.zeebe.broker.system.partitions.PartitionTransitionStep;
import io.camunda.zeebe.util.sched.ConcurrencyControl;
import io.camunda.zeebe.util.sched.future.ActorFuture;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

public final class NewPartitionTransitionImpl implements PartitionTransition {
  private static final int INACTIVE_TERM = -1;
  private static final Logger LOG = Loggers.SYSTEM_LOGGER;

  private final List<PartitionTransitionStep> steps;
  private PartitionTransitionContext context;
  private ConcurrencyControl concurrencyControl;
  private PartitionTransitionProcess lastTransition;
  private ActorFuture<Void> currentTransitionFuture;

  public NewPartitionTransitionImpl(
      final List<PartitionTransitionStep> steps, final PartitionTransitionContext context) {
    this.steps = new ArrayList<>(requireNonNull(steps));
    this.context = requireNonNull(context);
  }

  @Override
  public ActorFuture<Void> toFollower(final long term) {
    return transitionTo(term, Role.FOLLOWER);
  }

  @Override
  public ActorFuture<Void> toLeader(final long term) {
    return transitionTo(term, Role.LEADER);
  }

  @Override
  public ActorFuture<Void> toInactive() {
    return transitionTo(INACTIVE_TERM, Role.INACTIVE);
  }

  @Override
  public void setConcurrencyControl(final ConcurrencyControl concurrencyControl) {
    this.concurrencyControl = requireNonNull(concurrencyControl);
  }

  @Override
  public void updateTransitionContext(final PartitionTransitionContext transitionContext) {
    context = transitionContext;
  }

  public ActorFuture<Void> transitionTo(final long term, final Role role) {
    LOG.info("Transition to {} on term {} requested.", role, term);

    // notify steps immediately that a transition is coming; steps are encouraged to cancel any
    // ongoing activity at this point in time
    steps.forEach(step -> step.onNewRaftRole(context, role));

    final ActorFuture<Void> nextTransitionFuture = concurrencyControl.createFuture();
    final var nextTransition =
        new PartitionTransitionProcess(steps, concurrencyControl, context, term, role);
    nextTransitionFuture.onComplete((v, e) -> lastTransition = nextTransition);

    final var ongoingTransitionFuture =
        currentTransitionFuture == null
            ? concurrencyControl.createCompletedFuture()
            : currentTransitionFuture;

    // For safety reasons we have to immediately replace the current transition future with the next
    // transition future, such that we make sure that we enqueue all transitions after another.
    //
    // This means we will always add the next transition to the tail, this can become a chain of
    // futures if we have many transitions after another. Ideally we would execute after the current
    // transition only the last enqueue one. This should be implemented as soon we have time for it.
    currentTransitionFuture = nextTransitionFuture;

    ongoingTransitionFuture.onComplete(
        (nothing, error) -> {
          context.setCurrentTerm(term);
          context.setCurrentRole(role);

          if (lastTransition == null) {
            nextTransition.start(nextTransitionFuture);
          } else {
            final var cleanupFuture = lastTransition.cleanup(term, role);
            cleanupFuture.onComplete(
                (ok, e) -> {
                  if (error != null) {
                    LOG.error("Error during transition clean up: {}", error.getMessage(), error);
                    LOG.info("Aborting transition to {} on term {} due to error.", role, term);
                    nextTransitionFuture.completeExceptionally(error);
                  } else {
                    nextTransition.start(nextTransitionFuture);
                  }
                });
          }
        });
    return nextTransitionFuture;
  }
}
