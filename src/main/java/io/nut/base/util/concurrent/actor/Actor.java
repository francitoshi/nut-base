/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.math.Nums;
import java.util.Collection;
import java.util.Collections;
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
        this.actorHub = actorHub;
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
     * Ordered variant; default delegates to {@link #receive(Object)}.
     */
    protected void receive(M m, long seq)
    {
        receive(m);
    }

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
        if (actorHub != null)
        {
            actorHub.processedCount().increment();
        }
    }

    // -----------------------------------------------------------------
    // Pub/Sub
    // -----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public Actor<M> sub(String topic)
    {
        if (this.actorHub == null)
        {
            throw new IllegalStateException("No ActorHub attached.");
        }
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