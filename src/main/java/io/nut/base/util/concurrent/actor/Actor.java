/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.math.Nums;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base for all actor flavors ({@link SynchronousActor},
 * {@link SingleActor}, {@link MultiActor}).
 * <p>
 * Provides common lifecycle state, exception handling, and the
 * {@link Linkable} / {@link ActorLifecycle} contract.
 *
 * @param <M> the type of messages this actor processes
 */
public abstract class Actor<M> implements Consumer<M>, Linkable
{
    private static final Logger LOG = Logger.getLogger(Actor.class.getName());

    // All lifecycle / scheduling decisions are made under this monitor.
    protected final Object lock = new Object();

    /** {@code true} once the internal channel has been closed. */
    protected volatile boolean closed;

    /** {@code true} after {@link #terminate()} has run. */
    protected boolean terminated;

    protected volatile boolean allowLogger = true;
    protected volatile Exception ex;

    protected final ActorHub actorHub;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    protected Actor(ActorHub actorHub)
    {
        this.actorHub = actorHub != null ? actorHub : ActorHub.SYNCHRONOUS;
    }

    // -----------------------------------------------------------------
    // Static factory
    // -----------------------------------------------------------------

    /**
     * Creates the appropriate actor flavor for the requested thread count and
     * hub configuration, wiring the given callbacks into its message handling.
     * <p>
     * Selects {@link SynchronousActor} when {@code threads == 0}, the hub is
     * {@code null}, or the hub is synchronous; {@link SingleActor} when
     * {@code threads == 1}; {@link MultiActor} when {@code threads >= 2}.
     * <p>
     * The returned actor is a concrete flavor whose {@link #receive} delegates
     * to {@code onMessage}, {@link #terminate} to {@code onTerminate} (when
     * not {@code null}), and {@link #exception} to {@code onException} (when
     * not {@code null}).
     *
     * @param <M>       the message type
     * @param hub       the ActorHub, or {@code null} for synchronous
     * @param threads   the requested thread count
     * @param queueSize the channel queue capacity (0 = rendezvous)
     * @param onMessage the action to perform for each message; must not be
     *                  {@code null}
     * @param onTerminate the action to perform once after the channel is
     *                  closed and drained, or {@code null} for no-op
     * @param onException the action to perform when an unhandled exception
     *                  escapes from receive, or {@code null} for no-op
     * @return a new actor of the selected flavor
     * @throws IllegalArgumentException if {@code threads} or {@code queueSize}
     *                  are negative
     */
    public static <M> Actor<M> create(ActorHub hub, int threads, int queueSize, Consumer<M> onMessage, Runnable onTerminate, Consumer<Exception> onException)
    {
        Objects.requireNonNull(onMessage, "onMessage must not be null");
        if (threads < 0)
        {
            throw new IllegalArgumentException("threads must not be negative");
        }
        if (queueSize < 0)
        {
            throw new IllegalArgumentException("queueSize must not be negative");
        }
        if (threads == 0 || hub == null || hub.isSynchronous())
        {
            return new SynchronousActor<M>(hub)
            {
                @Override
                protected void receive(M m)
                {
                    onMessage.accept(m);
                }

                @Override
                protected void terminate()
                {
                    if (onTerminate != null)
                    {
                        onTerminate.run();
                    }
                }

                @Override
                protected void exception(Exception ex)
                {
                    if (onException != null)
                    {
                        onException.accept(ex);
                    }
                }
            };
        }
        if (threads == 1)
        {
            return new SingleActor<M>(hub, queueSize)
            {
                @Override
                protected void receive(M m)
                {
                    onMessage.accept(m);
                }

                @Override
                protected void terminate()
                {
                    if (onTerminate != null)
                    {
                        onTerminate.run();
                    }
                }

                @Override
                protected void exception(Exception ex)
                {
                    if (onException != null)
                    {
                        onException.accept(ex);
                    }
                }
            };
        }
        return new MultiActor<M>(hub, threads, queueSize)
        {
            @Override
            protected void receive(M m)
            {
                onMessage.accept(m);
            }

            @Override
            protected void terminate()
            {
                if (onTerminate != null)
                {
                    onTerminate.run();
                }
            }

            @Override
            protected void exception(Exception ex)
            {
                if (onException != null)
                {
                    onException.accept(ex);
                }
            }
        };
    }

    // -----------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------

    public Actor<M> dryLogger()
    {
        this.allowLogger = false;
        return this;
    }

    public Exception getException()
    {
        return ex;
    }

    public ActorHub getActorHub()
    {
        return actorHub;
    }

    // -----------------------------------------------------------------
    // Message API
    // -----------------------------------------------------------------

    /**
     * Sends a message to this actor for processing.
     *
     * @param message the message to deliver
     */
    @Override
    public abstract void accept(M message);

    /**
     * Called once for each message delivered to this actor.
     */
    protected abstract void receive(M m);

    /**
     * Called once after the channel is closed and drained.
     */
    protected void terminate()
    {
    }

    /**
     * Called when an unhandled exception escapes from receive.
     */
    protected void exception(Exception ex)
    {
    }

    // -----------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------

    protected void handleException(Exception ex)
    {
        this.ex = ex;
        if (allowLogger)
        {
            LOG.log(Level.SEVERE, "Actor", ex);
        }
        exception(ex);
    }

    protected void countProcessed()
    {
        actorHub.processedCount().increment();
    }

    // -----------------------------------------------------------------
    // Pub/Sub
    // -----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public Actor<M> sub(String topic)
    {
        this.actorHub.sub(topic, this);
        return this;
    }

    // -----------------------------------------------------------------
    // Idle / lifecycle
    // -----------------------------------------------------------------

    public int getPendingCount()
    {
        return 0;
    }

    public boolean isIdle()
    {
        return true;
    }

    @Override
    public Actor<M> waitForIdle()
    {
        return this;
    }

    @Override
    public Actor<M> shutdown()
    {
        return shutdown(false);
    }

    @Override
    public abstract Actor<M> shutdown(boolean onlyWhenEmpty);

    @Override
    public void close()
    {
        close(false);
    }

    public void close(boolean onlyWhenEmpty)
    {
        shutdown(onlyWhenEmpty);
        awaitTermination(Integer.MAX_VALUE);
    }

    public boolean awaitTermination(int millis)
    {
        long deadline = Nums.saturatedAdd(System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(millis));
        return awaitTerminationUntilNanos(deadline);
    }

    @Override
    public Actor<M> awaitTermination()
    {
        awaitTermination(Integer.MAX_VALUE);
        return this;
    }

    @Override
    public boolean awaitTerminationUntilNanos(long untilNanos)
    {
        synchronized (lock)
        {
            while (!terminated)
            {
                long remaining = untilNanos - System.nanoTime();
                if (remaining <= 0)
                {
                    return false;
                }
                try
                {
                    lock.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                }
                catch (InterruptedException ex)
                {
                    // Ignored by contract.
                }
            }
            return true;
        }
    }

    /**
     * Returns the number of worker threads this actor needs from the pool
     * (used by {@link ActorHub} for pool sizing). Synchronous actors report 0.
     */
    int threadDemand()
    {
        return 0;
    }

    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown()
    {
        return closed;
    }

    @Override
    public boolean isTerminated()
    {
        return terminated;
    }
}