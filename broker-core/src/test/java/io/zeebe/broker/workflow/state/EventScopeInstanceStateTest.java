/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.workflow.state;

import static io.zeebe.util.buffer.BufferUtil.wrapString;
import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.broker.logstreams.state.ZeebeState;
import io.zeebe.broker.util.ZeebeStateRule;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class EventScopeInstanceStateTest {

  @Rule public ZeebeStateRule stateRule = new ZeebeStateRule();

  private EventScopeInstanceState state;

  @Before
  public void setUp() {
    final ZeebeState zeebeState = stateRule.getZeebeState();
    state = zeebeState.getWorkflowState().getEventScopeInstanceState();
  }

  @Test
  public void shouldCreateInterruptingEventScopeInstance() {
    // given
    final long key = 123;

    // when
    state.createInstance(key, true);

    // then
    final EventScopeInstance instance = state.getInstance(key);
    assertThat(instance.isAccepting()).isTrue();
    assertThat(instance.isInterrupting()).isTrue();
  }

  @Test
  public void shouldCreateNonInterruptingEventScopeInstance() {
    // given
    final long key = 123;

    // when
    state.createInstance(key, false);

    // then
    final EventScopeInstance instance = state.getInstance(key);
    assertThat(instance.isAccepting()).isTrue();
    assertThat(instance.isInterrupting()).isFalse();
  }

  @Test
  public void shouldTriggerInterruptingEventScopeInstance() {
    // given
    final long key = 123;
    final long position = 456;
    final EventTrigger eventTrigger = createEventTrigger();

    state.createInstance(key, true);

    // when
    final boolean triggered = triggerEvent(key, position, eventTrigger);

    // then
    assertThat(triggered).isTrue();

    assertThat(state.pollEventTrigger(key)).isEqualTo(eventTrigger);

    final EventScopeInstance instance = state.getInstance(key);
    assertThat(instance.isAccepting()).isFalse();
  }

  @Test
  public void shouldTriggerInterruptingEventScopeInstanceOnlyOnce() {
    // given
    final long key = 123;
    final long position1 = 456;
    final EventTrigger eventTrigger1 = createEventTrigger();
    final long position2 = 789;
    final EventTrigger eventTrigger2 = createEventTrigger();

    state.createInstance(key, true);

    // when
    final boolean triggered1 = triggerEvent(key, position1, eventTrigger1);
    final boolean triggered2 = triggerEvent(key, position2, eventTrigger2);

    // then
    assertThat(triggered1).isTrue();
    assertThat(triggered2).isFalse();

    assertThat(state.pollEventTrigger(key)).isEqualTo(eventTrigger1);
    assertThat(state.pollEventTrigger(key)).isNull();

    final EventScopeInstance instance = state.getInstance(key);
    assertThat(instance.isAccepting()).isFalse();
  }

  @Test
  public void shouldTriggerNonInterruptingEventScopeInstanceMultipleTimes() {
    // given
    final long key = 123;
    final long position1 = 456;
    final EventTrigger eventTrigger1 = createEventTrigger();
    final long position2 = 789;
    final EventTrigger eventTrigger2 = createEventTrigger();

    state.createInstance(key, false);

    // when
    final boolean triggered1 = triggerEvent(key, position1, eventTrigger1);
    final boolean triggered2 = triggerEvent(key, position2, eventTrigger2);

    // then
    assertThat(triggered1).isTrue();
    assertThat(triggered2).isTrue();

    assertThat(state.pollEventTrigger(key)).isEqualTo(eventTrigger1);
    assertThat(state.pollEventTrigger(key)).isEqualTo(eventTrigger2);
    assertThat(state.pollEventTrigger(key)).isNull();

    final EventScopeInstance instance = state.getInstance(key);
    assertThat(instance.isAccepting()).isTrue();
  }

  @Test
  public void shouldNotTriggerOnNonExistingEventScope() {
    // given
    final EventTrigger eventTrigger = createEventTrigger();

    // when
    final boolean triggered = triggerEvent(123, 456, eventTrigger);

    // then
    assertThat(triggered).isFalse();
  }

  @Test
  public void shouldPeekEventTrigger() {
    // given
    final long key = 123;
    final EventTrigger eventTrigger = createEventTrigger();

    // when
    state.createInstance(key, false);
    triggerEvent(key, 1, eventTrigger);

    // then
    assertThat(state.peekEventTrigger(key)).isEqualTo(eventTrigger);
    assertThat(state.peekEventTrigger(key)).isEqualTo(eventTrigger);
  }

  @Test
  public void shouldPollEventTrigger() {
    // given
    final long key = 123;
    final EventTrigger eventTrigger1 = createEventTrigger();
    final EventTrigger eventTrigger2 = createEventTrigger();

    // when
    state.createInstance(key, false);
    triggerEvent(key, 1, eventTrigger1);
    triggerEvent(key, 2, eventTrigger2);

    // then
    assertThat(state.pollEventTrigger(key)).isEqualTo(eventTrigger1);
    assertThat(state.pollEventTrigger(key)).isEqualTo(eventTrigger2);
    assertThat(state.pollEventTrigger(key)).isNull();
  }

  @Test
  public void shouldDeleteTrigger() {
    // given
    final long key = 123;
    final long position = 456;

    state.createInstance(key, false);
    triggerEvent(key, position, createEventTrigger());

    // when
    state.deleteTrigger(key, position);

    // then
    assertThat(state.pollEventTrigger(key)).isNull();
  }

  @Test
  public void shouldDeleteEventScopeAndTriggers() {
    // given
    final long key = 123;

    state.createInstance(key, false);
    triggerEvent(123, 1, createEventTrigger());
    triggerEvent(123, 2, createEventTrigger());
    triggerEvent(123, 3, createEventTrigger());

    // when
    state.deleteInstance(key);

    // then
    assertThat(state.getInstance(key)).isNull();
    assertThat(state.peekEventTrigger(key)).isNull();
  }

  @Test
  public void shouldNotTriggerOnDeletedEventScope() {
    // given
    final long key = 123;
    final EventTrigger eventTrigger = createEventTrigger();

    state.createInstance(key, true);
    state.deleteInstance(key);

    // when
    final boolean triggered = triggerEvent(key, 456, eventTrigger);

    // then
    assertThat(triggered).isFalse();
  }

  private boolean triggerEvent(long eventScopeKey, long position, EventTrigger eventTrigger) {
    return state.triggerEvent(
        eventScopeKey, position, eventTrigger.getElementId(), eventTrigger.getPayload());
  }

  private EventTrigger createEventTrigger() {
    return createEventTrigger(randomString(), randomString());
  }

  private EventTrigger createEventTrigger(String elementId, String payload) {
    return new EventTrigger().setElementId(wrapString(elementId)).setPayload(wrapString(payload));
  }

  private String randomString() {
    return UUID.randomUUID().toString();
  }
}
