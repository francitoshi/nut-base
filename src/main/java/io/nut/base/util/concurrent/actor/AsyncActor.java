/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.util.concurrent.channel.Channel;
import io.nut.base.util.concurrent.channel.CloseableChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract async actor flavor: provides the shared channel, worker
 * infrastructure, and shutdown machine used by {@link SingleActor} and
 * {@link MultiActor}.
 *
 * @param <M> the message type
 */
abstract class AsyncActor<M> extends Actor<M>
{
    /** How long the permanent worker waits before giving its thread back. */
    static final long PERMANENT_WAIT_MILLIS = 400;

    protected final CloseableChannel<M> channel;
    protected final int threads;
    protected final Semaphore workerSlots;
    protected final AtomicInteger pending = new AtomicInteger();
    protected final AtomicInteger processing = new AtomicInteger();
    protected final AtomicLong sequenceCounter = new AtomicLong();
    protected final AtomicInteger activeWorkers = new AtomicInteger();
    protected boolean permanentWorkerStarted;
    protected boolean shutdownWhenEmpty;
    protected final ActorHooks<M> hooks;

    protected AsyncActor(ActorHub actorHub, int threads, int queueSize, ActorHooks<M> hooks)
    {
        super(actorHub);
        this.threads = threads;
        this.hooks = hooks;
        if (queueSize == 0)
        {
            this.channel = Channel.closeableOf(0);
        }
        else
        {
            this.channel = Channel.closeableBuffered(queueSize);
        }
        this.workerSlots = new Semaphore(threads);
        if (actorHub instanceof ActorHub)
        {
            ((ActorHub) actorHub).registerActor(this);
        }
    }

    @Override
    protected void receive(M m)
    {
        // Not called — hooks.receive is used instead.
    }

    @Override
    protected void exception(Exception ex)
    {
        hooks.exception(ex);
    }

    @Override
    int threadDemand()
    {
        return threads;
    }

    // -----------------------------------------------------------------
    // accept
    // -----------------------------------------------------------------

    @Override
    public void accept(M message)
    {
        if (closed)
        {
            throw new IllegalStateException("closed");
        }
        try
        {
            initPermanentWorker();
            pending.incrementAndGet();
            boolean queued = false;
            try
            {
                channel.put(message);
                queued = true;
            }
            finally
            {
                if (!queued)
                {
                    pending.decrementAndGet();
                }
            }

            if (queued && threads > 1)
            {
                synchronized (lock)
                {
                    if (workerSlots.tryAcquire())
                    {
                        try
                        {
                            startWorker();
                        }
                        catch (Exception ex)
                        {
                            workerSlots.release();
                            throw ex;
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            handleException(ex);
        }
    }

    // -----------------------------------------------------------------
    // Worker management
    // -----------------------------------------------------------------

    protected void startWorker()
    {
        activeWorkers.incrementAndGet();
        try
        {
            Executor h = actorHub;
            if (h != null)
            {
                h.execute(this::workerLoop);
            }
            else
            {
                activeWorkers.decrementAndGet();
                workerSlots.release();
            }
        }
        catch (Exception ex)
        {
            activeWorkers.decrementAndGet();
            workerSlots.release();
            throw ex;
        }
    }

    protected void initPermanentWorker()
    {
        synchronized (lock)
        {
            if (permanentWorkerStarted || closed || terminated)
            {
                return;
            }
            permanentWorkerStarted = true;
            if (!workerSlots.tryAcquire())
            {
                permanentWorkerStarted = false;
                return;
            }
            activeWorkers.incrementAndGet();
        }
        try
        {
            actorHub.execute(this::permanentLoop);
        }
        catch (Exception ex)
        {
            synchronized (lock)
            {
                permanentWorkerStarted = false;
                activeWorkers.decrementAndGet();
                workerSlots.release();
            }
            throw ex;
        }
    }

    /**
     * Rush worker loop: drain once, then return.
     */
    protected void workerLoop()
    {
        try
        {
            drain(0);
        }
        finally
        {
            workerDone(false);
        }
    }

    /**
     * The permanent worker loop. Subclasses define the exact drain window;
     * both flavors hand back to {@link #workerDone(boolean)} afterwards so the
     * thread is yielded to the pool once the Actor has been idle for the
     * window.
     */
    protected abstract void permanentLoop();

    // -----------------------------------------------------------------
    // Drain
    // -----------------------------------------------------------------

    protected void drain(long timeoutMillis)
    {
        M m;
        while ((m = channel.get(timeoutMillis, TimeUnit.MILLISECONDS)) != null)
        {
            pending.decrementAndGet();
            long seq = sequenceCounter.incrementAndGet();
            processing.incrementAndGet();
            countProcessed();
            try
            {
                hooks.receive(m, seq);
            }
            catch (Exception ex)
            {
                handleException(ex);
            }
            finally
            {
                if (processing.decrementAndGet() == 0 && pending.get() == 0)
                {
                    notifyIdle();
                }
            }
        }
    }

    /**
     * Wakes {@link #waitForIdle()} waiters as soon as this actor becomes
     * idle, without waiting for the permanent worker to exit its idle window.
     */
    private void notifyIdle()
    {
        synchronized (lock)
        {
            lock.notifyAll();
        }
    }

    // -----------------------------------------------------------------
    // Worker done
    // -----------------------------------------------------------------

    protected void workerDone(boolean permanent)
    {
        while (true)
        {
            while (pending.get() > 0)
            {
                drain(0);
            }

            boolean close = false;
            synchronized (lock)
            {
                if (pending.get() > 0)
                {
                    continue;
                }
                boolean last = activeWorkers.decrementAndGet() == 0;
                if (closed)
                {
                    if (last)
                    {
                        doTerminate();
                    }
                    workerSlots.release();
                    lock.notifyAll();
                    return;
                }
                if (shutdownWhenEmpty)
                {
                    close = true;
                }
                else
                {
                    if (permanent)
                    {
                        permanentWorkerStarted = false;
                    }
                    workerSlots.release();
                    lock.notifyAll();
                    return;
                }
            }

            if (close)
            {
                closeNow();
                if (permanent)
                {
                    permanentWorkerStarted = false;
                }
                synchronized (lock)
                {
                    workerSlots.release();
                    lock.notifyAll();
                }
                return;
            }
        }
    }

    // -----------------------------------------------------------------
    // Close / terminate
    // -----------------------------------------------------------------

    protected void closeNow()
    {
        closed = true;
        channel.close();
        if (activeWorkers.get() == 0)
        {
            drain(0);
            doTerminate();
        }
    }

    protected void doTerminate()
    {
        synchronized (lock)
        {
            if (terminated)
            {
                return;
            }
            terminated = true;
            unregisterFromActorHub();
            try
            {
                hooks.terminate();
            }
            catch (Exception ex)
            {
                handleException(ex);
            }
            lock.notifyAll();
        }
    }

    private void unregisterFromActorHub()
    {
        if (actorHub instanceof ActorHub)
        {
            ((ActorHub) actorHub).unregisterActor(this);
        }
    }

    // -----------------------------------------------------------------
    // Idle / lifecycle
    // -----------------------------------------------------------------

    @Override
    public int getPendingCount()
    {
        return pending.get() + activeWorkers.get();
    }

    @Override
    public boolean isIdle()
    {
        return pending.get() <= 0 && processing.get() == 0;
    }

    @Override
    public Actor<M> waitForIdle()
    {
        synchronized (lock)
        {
            while (!isIdle())
            {
                try
                {
                    lock.wait();
                }
                catch (InterruptedException ex)
                {
                    // Ignored by contract.
                }
            }
        }
        return this;
    }

    @Override
    public Actor<M> shutdown(boolean onlyWhenEmpty)
    {
        synchronized (lock)
        {
            if (!closed && !terminated)
            {
                if (onlyWhenEmpty)
                {
                    if (isIdle())
                    {
                        closeNow();
                    }
                    else
                    {
                        shutdownWhenEmpty = true;
                    }
                }
                else
                {
                    closeNow();
                }
            }
            lock.notifyAll();
        }
        return this;
    }
}
