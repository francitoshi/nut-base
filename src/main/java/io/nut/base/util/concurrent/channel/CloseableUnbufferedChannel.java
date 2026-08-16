/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CloseableUnbufferedChannel<E> extends CloseableChannel<E>
{
    // Identity sentinel, not equality: it doesn't need to be parameterized
    // with E nor associated with any value. It is only compared with == below,
    // so even E itself couldn't "collide" with it even if it were Object,
    // because nobody outside this class holds the reference.
    private static final Object POISON = new Object();

    private final AtomicInteger gets = new AtomicInteger();

    private final SynchronousQueue<Object> queue = new SynchronousQueue<>();

    private volatile boolean closed;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private final Object closeLock = new Object();

    @Override
    public void put(E value) throws InterruptedException
    {
        Objects.requireNonNull(value, "value must not be null");

        if (closed)
        {
            throw new IllegalStateException("closed");
        }

        rwLock.readLock().lock();
        try
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }

            queue.put(value);
        }
        finally
        {
            rwLock.readLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get() throws InterruptedException
    {
        if (closed)
        {
            // The channel is closed, but items already queued before the
            // close() call must still be delivered: close() guarantees no
            // put() remains in transit once it returns, so at this point the
            // queue only holds leftover real values and/or stray POISON
            // sentinels. Drain it non-blockingly instead of discarding it.
            return drainAfterClose();
        }

        gets.incrementAndGet();
        try
        {
            if (closed)
            {
                return drainAfterClose();
            }

            Object item = queue.take();
            return item == POISON ? null : (E) item;
        }
        finally
        {
            gets.decrementAndGet();
        }
    }

    @SuppressWarnings("unchecked")
    private E drainAfterClose()
    {
        Object item = queue.poll();
        return (item == null || item == POISON) ? null : (E) item;
    }

    public boolean close(long timeout, TimeUnit unit) throws InterruptedException
    {
        synchronized (closeLock)
        {
            closed = true;

            // We acquire the writeLock only as a barrier: it confirms that no
            // put() is in the middle of the critical section at this instant.
            // We release it right away: there is no need to hold it, because
            // any put() that resumes after this barrier will re-check `closed`
            // already inside the readLock and throw IllegalStateException on
            // its own. If we did not release it, a put() that was suspended
            // right before requesting the readLock (racing with this close())
            // would block forever on a non-interruptible lock().
            //
            // The barrier must be acquired AFTER closed=true is set: only then
            // are poisons guaranteed to be enqueued after every real value
            // already committed, since no put() can enqueue past this point.
            boolean acquired = rwLock.writeLock().tryLock(timeout, unit);
            if (acquired)
            {
                rwLock.writeLock().unlock();
            }
            // true  -> confirmed that no put() remains in transit (no losses)
            // false -> timeout expired; a put() may still be blocked.
            // Note: closed stays true regardless, so new put()s are already
            // rejected. A later call to close() will retry this barrier and
            // the poisoning loop below; it is idempotent (harmless/cheap) once
            // no more gets() are pending, and honestly keeps returning false
            // while the barrier still cannot be acquired.
            if (!acquired)
            {
                return false;
            }

            for (int i = 1; gets.get() > 0; i++)
            {
                queue.offer(POISON, Math.min(i, 100), TimeUnit.MILLISECONDS);
            }

            return true;
        }
    }

    @Override
    public boolean close()
    {
        try
        {
            return close(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    @Override
    public boolean isClosed()
    {
        return closed;
    }
}
