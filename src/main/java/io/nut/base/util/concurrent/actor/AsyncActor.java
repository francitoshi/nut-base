/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.util.concurrent.atomic.AtomicCounterPair;
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
    protected final CloseableChannel<M> channel;
    protected final int threads;
    protected final Semaphore workerSlots;

    /**
     * Messages queued in the channel (first) and messages currently being
     * processed (second), packed into a single word so that moving a message
     * from "queued" to "processing" and detecting that both reached zero are
     * single, race-free atomic operations.
     */
    protected final AtomicCounterPair counters = new AtomicCounterPair();

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
        this.actorHub.registerActor(this);
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
            counters.incrementFirst();
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
                    counters.decrementFirst();
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
     * The permanent worker loop. Subclasses drain the channel and hand back to
     * {@link #workerDone(boolean)} afterwards; the worker stays parked between
     * messages instead of yielding its thread back to the pool, so the Actor
     * always has a reader ready.
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
            counters.addFirstToSecond(1);
            long seq = sequenceCounter.incrementAndGet();
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
                if (counters.decrementSecondAndAllZero())
                {
                    notifyIdle();
                }
            }
        }
    }

    /**
     * The permanent worker drain: reads messages in a loop that blocks
     * indefinitely between messages, so it stays parked as a take-ready reader
     * of (possibly rendezvous) channels for as long as the Actor is registered.
     * Only {@link #closeNow()} (which closes the channel) ends the loop: yielding
     * the thread back to the pool after an idle window is what let a
     * SynchronousQueue hand-off strand, because the just-respawned permanent
     * worker could time out before the matching {@code put} arrived and no other
     * reader would ever come. The permanent worker therefore never abandons the
     * channel between messages; the thread it holds is accounted for by the
     * pool sizing (one core thread per registered non-synchronous Actor).
     */
    protected void drainWhileRegistered()
    {
        M m;
        while ((m = channel.get()) != null)
        {
            counters.addFirstToSecond(1);
            long seq = sequenceCounter.incrementAndGet();
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
                if (counters.decrementSecondAndAllZero())
                {
                    synchronized (lock)
                    {
                        lock.notifyAll();
                        if (shutdownWhenEmpty)
                        {
                            closeNow();
                            return;
                        }
                    }
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
            while (counters.getFirst() > 0)
            {
                drain(0);
            }

            boolean close = false;
            synchronized (lock)
            {
                if (counters.getFirst() > 0)
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
        actorHub.unregisterActor(this);
    }

    // -----------------------------------------------------------------
    // Idle / lifecycle
    // -----------------------------------------------------------------

    @Override
    public int getPendingCount()
    {
        return counters.getFirst() + activeWorkers.get();
    }

    @Override
    public boolean isIdle()
    {
        return counters.isZero();
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
