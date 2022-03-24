/*
 *  Copyright (C) 2020 Temporal Technologies, Inc. All Rights Reserved.
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.internal.testservice;

import io.temporal.workflow.Functions;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.LongSupplier;
import javax.annotation.Nullable;

/**
 * Timer service that automatically forwards current time to the next task time when is not locked
 * through {@link #lockTimeSkipping(String)}.
 */
interface SelfAdvancingTimer {

  /**
   * Schedule a task with a specified delay. The actual wait time is defined by the internal clock
   * that might advance much faster than the wall clock.
   *
   * @return cancellation handle
   */
  Functions.Proc schedule(Duration delay, Runnable task);

  Functions.Proc schedule(Duration delay, Runnable task, String taskInfo);

  Functions.Proc scheduleAt(Instant timestamp, Runnable task, String taskInfo);

  /** Supplier that returns current time of the timer when called. */
  LongSupplier getClock();

  /**
   * Prohibit automatic time skipping until {@link #unlockTimeSkipping(String)} is called. Locks and
   * unlocks are counted. So calling unlock does not guarantee that time is going to be skipped
   * immediately as another lock can be holding it.
   */
  LockHandle lockTimeSkipping(String caller);

  void unlockTimeSkipping(String caller);

  /**
   * Adjust the current time of the timer forward by {@code duration}.
   *
   * <p>This method doesn't respect time skipping lock counter and state.
   *
   * <p>This method returns immediately not waiting for tasks triggering.
   *
   * @param duration the time period to adjust the timer
   * @returns new timer timestamp after skipping
   */
  Instant skip(Duration duration);

  /**
   * Set the current time of the timer to {@code timestamp} if {@code timestamp} is beyond the
   * current timer time, otherwise does nothing
   *
   * <p>This method doesn't respect time skipping lock counter and state.
   *
   * <p>This method returns immediately not waiting for tasks triggering.
   *
   * @param timestamp the timestamp to set the timer to
   */
  void skipTo(Instant timestamp);

  /**
   * Update lock count. The same as calling lockTimeSkipping count number of times for positive
   * count and unlockTimeSkipping for negative count.
   *
   * @param updates to the locks
   */
  void updateLocks(List<RequestContext.TimerLockChange> updates);

  void getDiagnostics(StringBuilder result);

  void shutdown();
}

interface LockHandle {
  void unlock();

  void unlock(@Nullable String caller);
}
