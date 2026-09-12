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
 *   <li>{@link #shutdown()} stops accepting new work, but any work already
 *       submitted is still processed to completion in the background.</li>
 *   <li>{@link #shutdown(boolean)} with {@code true} closes admission of new
 *       work only once the instance is idle, so work already submitted is
 *       processed to completion first; with {@code false} it behaves exactly
 *       like {@link #shutdown()}.</li>
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
 * The shutdown methods ({@link #shutdown()} and {@link #shutdown(boolean)})
 * are non-blocking by convention but not by contract. An implementation is
 * strongly encouraged to return immediately and complete the shutdown in the
 * background, yet it <em>may</em> wait as long as necessary to honour the
 * guarantee that work submitted before the shutdown is never abandoned. For
 * example, {@link ActorHub#shutdown(boolean)} waits until every linked stage
 * of its graph is quiescent before closing, so that in-flight forwards
 * between stages are not lost. In all cases the methods are idempotent: the
 * second call behaves like the first.
 * </p>
 * <p>
 * Implementations return {@code this} (as a subtype) from the operations that
 * do not produce a value, enabling fluent chaining; a parameterized reference
 * may use the {@code ActorLifecycle} return type declared here.
 */
public interface ActorLifecycle extends AutoCloseable
{
/**
     * Stops accepting new work. Work already submitted is still processed to
     * completion in the background. Idempotent.
     * <p>
     * Non-blocking by convention: implementations return as soon as possible
     * and may wait only when necessary to guarantee that previously submitted
     * work is not abandoned.
     *
     * @return this instance, for fluent chaining
     */
    ActorLifecycle shutdown();

    /**
     * Stops accepting new work. With {@code true} admission of new work closes
     * only once this instance is idle; with {@code false} it closes
     * immediately. Work submitted before the shutdown is never abandoned.
     * Idempotent.
     * <p>
     * Non-blocking by convention: implementations return as soon as possible
     * and may wait only when necessary to guarantee that previously submitted
     * work is not abandoned.
     *
     * @param whenIdle if {@code true}, close admission of new work once this
     *                 instance is idle; if {@code false}, close admission
     *                 immediately
     * @return this instance, for fluent chaining
     */
    ActorLifecycle shutdown(boolean whenIdle);

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
