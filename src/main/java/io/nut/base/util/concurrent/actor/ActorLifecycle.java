/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Common lifecycle contract shared by {@link Actor}, {@link ActorPool} and
 * {@link ActorHub}.
 * <p>
 * All three follow the same semantics:
 * <ul>
 *   <li>{@link #shutdown()} stops accepting new work immediately, but any
 *       work already submitted is still processed to completion in the
 *       background. This call never blocks.</li>
 *   <li>{@link #shutdown(boolean)} with {@code true} first waits until the
 *       instance is idle ({@link #waitForIdle()}) and only then stops
 *       accepting new work; with {@code false} it behaves exactly like
 *       {@link #shutdown()}.</li>
 *   <li>{@link #waitForIdle()} blocks until there is no work pending or in
 *       progress. This is a point-in-time condition: unless the instance is
 *       also shut down, new work may arrive right afterwards.</li>
 *   <li>{@link #awaitTermination()} blocks until the instance no longer
 *       accepts work <em>and</em> has finished processing everything that was
 *       submitted before it was shut down.</li>
 *   <li>{@link #close()} is the blocking, idempotent, try-with-resources
 *       friendly combination: {@code shutdown(); awaitTermination();}.</li>
 * </ul>
 * <p>
 * Implementations return {@code this} (as a subtype) from the operations that
 * do not produce a value, enabling fluent chaining; a parameterized reference
 * may use the {@code ActorLifecycle} return type declared here.
 */
public interface ActorLifecycle extends AutoCloseable
{
    /**
     * Stops accepting new work. Does not block: work already submitted keeps
     * being processed in the background. Idempotent.
     *
     * @return this instance, for fluent chaining
     */
    ActorLifecycle shutdown();

    /**
     * Stops accepting new work, optionally waiting until the instance is
     * idle first.
     *
     * @param waitForIdleFirst if {@code true}, blocks until
     *                         {@link #waitForIdle()} returns before actually
     *                         closing admission of new work; if
     *                         {@code false}, equivalent to {@link #shutdown()}
     * @return this instance, for fluent chaining
     * @throws InterruptedException if interrupted while waiting for idle
     */
    ActorLifecycle shutdown(boolean waitForIdleFirst) throws InterruptedException;

    /**
     * Blocks the calling thread until this instance has no work pending or
     * in progress.
     *
     * @return this instance, for fluent chaining
     * @throws InterruptedException if interrupted while waiting
     */
    ActorLifecycle waitForIdle() throws InterruptedException;

    /**
     * Blocks the calling thread until this instance no longer accepts new
     * work and has finished processing everything submitted before the
     * shutdown.
     *
     * @return this instance, for fluent chaining
     * @throws InterruptedException if interrupted while waiting
     */
    ActorLifecycle awaitTermination() throws InterruptedException;

    /**
     * @return {@code true} if {@link #shutdown()} (in either form) has been
     *         called
     */
    boolean isShutdown();

    /**
     * @return {@code true} if this instance is shut down and has finished
     *         processing all previously submitted work
     */
    boolean isTerminated();

    /**
     * Blocking, idempotent close: equivalent to calling {@link #shutdown()}
     * followed by {@link #awaitTermination()}. If interrupted while waiting,
     * restores the interrupt flag on the current thread and returns.
     */
    @Override
    void close();
}
