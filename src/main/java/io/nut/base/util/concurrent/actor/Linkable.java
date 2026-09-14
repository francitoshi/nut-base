/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Common interface implemented by all stages that participate in chain-level
 * lifecycle management ({@link ActorHub#waitForIdle}, chain traversal, etc.).
 * <p>
 * Both {@link Actor} (engine) and the stage wrappers ({@link PipeActor},
 * {@link FilterActor}, {@link BatchActor}, {@link FanOutActor}) implement this
 * interface so that {@link ActorHub} can traverse arbitrary chains uniformly.
 */
public interface Linkable extends ActorLifecycle
{
    @Override
    Linkable waitForIdle();

    @Override
    Linkable shutdown();

    @Override
    Linkable shutdown(boolean whenIdle);

    /**
     * Returns the downstream consumers linked to this stage.
     *
     * @return an unmodifiable snapshot of downstream targets
     */
    Collection<Consumer<?>> getLinkedTargets();

    /**
     * Blocks until this stage has terminated or the deadline elapses.
     *
     * @param untilNanos the absolute deadline (in nanoseconds)
     * @return {@code true} if terminated within the deadline
     */
    boolean awaitTerminationUntilNanos(long untilNanos);

    /**
     * @return {@code true} if this stage has no pending work
     */
    boolean isIdle();
}
