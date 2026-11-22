/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import io.github.francitoshi.nut.base.commons.concurrent.atomic.AtomicCounterPair;
import io.github.francitoshi.nut.base.commons.concurrent.channel.Channel;
import io.github.francitoshi.nut.base.commons.concurrent.channel.CloseableChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    protected final AtomicInteger activeWorkers = new AtomicInteger();
    /**
     * Volatile so the {@code accept()} hot path can check it without acquiring
     * {@code lock}: it is only ever written while holding {@code lock}, and a
     * stale read (either direction) is harmless — the slow path under the lock
     * re-checks it and a worker that just exited re-arms it there.
     */
    protected volatile boolean permanentWorkerStarted;
    protected boolean shutdownWhenEmpty;

    /**
     * Slice between drain passes in {@link #workerDone(boolean)} while a
     * message is still in transit (its producer holds a counter entry but the
     * channel does not hold the message yet). A non-blocking pass cannot see
     * such messages, so the previous hot poll loop burned a core for as long
     * as the producer stayed descheduled; this tiny timed slice parks the
     * worker instead, which costs nothing when nothing arrives in time and
     * keeps the message latency bounded well below a scheduling quantum.
     */
    private static final long IN_TRANSIT_SLICE_MILLIS = 2;

    protected AsyncActor(ActorHub actorHub, int threads, int queueSize)
    {
        super(actorHub);
        this.threads = threads;
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
        // The permanent worker is started before the message is accounted
        // for, so a failure here cannot strand it: nothing was delivered yet,
        // and the failure propagates to the caller (the message would
        // otherwise be silently dropped while the caller believes it was
        // accepted).
        try
        {
            initPermanentWorker();
        }
        catch (RuntimeException ex)
        {
            handleException(ex);
            throw ex;
        }
        counters.incrementFirst();
        boolean queued = false;
        try
        {
            channel.put(message);
            queued = true;
        }
        catch (RuntimeException ex)
        {
            // Not delivered: the actor or its channel was closed concurrently,
            // or the put was aborted by the close. The failure is recorded and
            // surfaced to the caller instead of letting it believe the message
            // was accepted.
            handleException(ex);
            throw ex;
        }
        finally
        {
            if (!queued)
            {
                counters.decrementFirst();
            }
        }

        if (threads > 1)
        {
            // The rush-worker start does not take the actor lock: the
            // semaphore already bounds the total worker count at `threads`,
            // and every other field touched here is either atomic
            // (activeWorkers, workerSlots) or written under the lock
            // elsewhere. Producers therefore never serialise against
            // workerDone()/notifyIdle() on the message path.
            //
            // The start is also lazy: `threads` is a ceiling, not a
            // guarantee. The permanent worker is the safety net that always
            // processes messages, so an extra worker is only worth its pool
            // hand-off when work is actually backing up: `first > 1` means
            // at least one queued message besides ours. When the permanent
            // worker keeps up with the arrival rate, no rush worker is ever
            // started — which also avoids the double wake-up of the eager
            // design (channel put wakes the permanent worker, execute()
            // wakes a rush worker, one of the two wake-ups is wasted on a
            // worker that finds the channel already drained). A spurious
            // or missed start is harmless: a spurious one drains and exits,
            // a missed one only delays a message until the permanent worker
            // or a later rush takes it.
            if (counters.getFirst() > 1 && workerSlots.tryAcquire())
            {
                try
                {
                    startWorker();
                }
                catch (Exception ex)
                {
                    workerSlots.release();
                    // The message is safely queued: the permanent worker
                    // drains it. A failing rush start is recorded and
                    // logged, but must NOT surface as a failed accept —
                    // the caller would believe the message was rejected
                    // and a retry would deliver it twice.
                    handleException(ex);
                }
            }
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
        // Fast path: the permanent worker is already running. The volatile
        // read replaces what used to be a lock acquisition per message; the
        // slow path below re-checks the same condition under the lock, so a
        // racy stale read can only cost one redundant slow-path entry.
        if (permanentWorkerStarted)
        {
            return;
        }
        lock.lock();
        try
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
        finally
        {
            lock.unlock();
        }
        try
        {
            // The permanent worker lives as long as this actor: run it outside
            // the hub's idle tracking so waitForIdle()/isIdle() do not consider
            // an idle actor to be busy forever.
            actorHub.executeUntracked(this::permanentLoop);
        }
        catch (Exception ex)
        {
            lock.lock();
            try
            {
                permanentWorkerStarted = false;
                activeWorkers.decrementAndGet();
                workerSlots.release();
            }
            finally
            {
                lock.unlock();
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
            countProcessed();
            try
            {
                receive(m);
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
            countProcessed();
            try
            {
                receive(m);
            }
            catch (Exception ex)
            {
                handleException(ex);
            }
            finally
            {
                if (counters.decrementSecondAndAllZero())
                {
                    lock.lock();
                    try
                    {
                        condition.signalAll();
                        if (shutdownWhenEmpty)
                        {
                            closeNow();
                            return;
                        }
                    }
                    finally
                    {
                        lock.unlock();
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
        lock.lock();
        try
        {
            condition.signalAll();
        }
        finally
        {
            lock.unlock();
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
                if (counters.getFirst() > 0)
                {
                    // Messages still in transit: a producer already holds its
                    // counter entry but has not reached the channel yet (or
                    // its rendezvous put is waiting for this short window to
                    // hand the value over). The previous non-blocking pass
                    // could not see such a message, so re-polling it in a
                    // tight loop burned CPU for as long as the producer
                    // stayed slow (preemption, scheduling, GC pause). A tiny
                    // timed hand-off parks the worker instead of spinning,
                    // which costs nothing when nothing arrives.
                    drain(IN_TRANSIT_SLICE_MILLIS);
                }
            }

            // A blocked get()/put() that is interrupted makes the closeable
            // channels close themselves, without going through this actor's
            // own close path, so the actor's closed flag is still false here.
            // Without this hook the workers would simply exit and the actor
            // would be left open forever: never terminated (awaitTermination
            // hangs) with no workers, and every later accept() failing
            // against the closed channel with the exception silently
            // swallowed by handleException.
            closeIfClosedExternally();

            boolean close = false;
            lock.lock();
            try
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
                    condition.signalAll();
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
                    condition.signalAll();
                    return;
                }
            }
            finally
            {
                lock.unlock();
            }

            if (close)
            {
                closeNow();
                if (permanent)
                {
                    permanentWorkerStarted = false;
                }
                lock.lock();
                try
                {
                    workerSlots.release();
                    condition.signalAll();
                }
                finally
                {
                    lock.unlock();
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

    /**
     * Drives this actor through its normal close path when the channel was
     * closed from outside this actor's own close paths. The closeable
     * channels abort a blocked {@code get()}/{@code put()} when their thread
     * is interrupted, and that abort closes the channel itself; the actor's
     * {@code closed} flag stays false, so the state is unambiguous:
     * {@code channel closed while the actor is open} can only come from such
     * an external close.
     * <p>
     * The flag is claimed under the lock so exactly one caller proceeds;
     * {@link #closeNow()} then runs outside the lock (as in
     * {@link #workerDone(boolean)}) and is idempotent against the actor's own
     * shutdown paths. Messages already buffered in a closed channel are still
     * drained by the readers (the closeable channels yield buffered elements
     * after the close), so no queued work is lost.
     */
    private void closeIfClosedExternally()
    {
        if (closed || terminated || !channel.isClosed())
        {
            return;
        }
        lock.lock();
        try
        {
            if (closed || terminated)
            {
                return;
            }
            closed = true;
        }
        finally
        {
            lock.unlock();
        }
        closeNow();
    }

    protected void doTerminate()
    {
        lock.lock();
        try
        {
            if (terminated)
            {
                return;
            }
            terminated = true;
            unregisterFromActorHub();
            try
            {
                terminate();
            }
            catch (Exception ex)
            {
                handleException(ex);
            }
            condition.signalAll();
        }
        finally
        {
            lock.unlock();
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
        lock.lock();
        try
        {
            while (!isIdle())
            {
                try
                {
                    condition.await();
                }
                catch (InterruptedException ex)
                {
                    // Ignored by contract.
                }
            }
        }
        finally
        {
            lock.unlock();
        }
        return this;
    }

    @Override
    public Actor<M> shutdown(boolean onlyWhenEmpty)
    {
        lock.lock();
        try
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
            condition.signalAll();
        }
        finally
        {
            lock.unlock();
        }
        return this;
    }
}
