/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.TimeUnit;

/**
 * Version counter shared between a {@link Select} and the channels it waits on.
 * <p>
 * It carries no data: channels only bump the version when something may have
 * changed (a value became readable, the channel was closed). The select
 * protocol that avoids lost wake-ups is:
 * <ol>
 *   <li>register the signal in every channel,</li>
 *   <li>read {@link #version()},</li>
 *   <li>scan the channels without blocking,</li>
 *   <li>if nothing was found, {@link #awaitChange(long, long)} with the version
 *       read in step 2. If any channel fired in between, the wait returns
 *       immediately.</li>
 * </ol>
 */
final class SelectSignal
{
    private final Object mon = new Object();
    private long version;

    long version()
    {
        synchronized (mon)
        {
            return version;
        }
    }

    void signal()
    {
        synchronized (mon)
        {
            version++;
            mon.notifyAll();
        }
    }

    /**
     * Waits until the version differs from {@code seen}.
     *
     * @param seen  the version read before the last scan
     * @param nanos maximum wait; {@code Long.MAX_VALUE} means no limit
     * @return {@code true} if the version changed, {@code false} on timeout
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    boolean awaitChange(long seen, long nanos) throws InterruptedException
    {
        synchronized (mon)
        {
            long remaining = nanos;
            long last = System.nanoTime();
            while (version == seen)
            {
                if (remaining <= 0)
                {
                    return false;
                }
                if (remaining == Long.MAX_VALUE)
                {
                    mon.wait();
                }
                else
                {
                    TimeUnit.NANOSECONDS.timedWait(mon, remaining);
                    long now = System.nanoTime();
                    remaining -= now - last;
                    last = now;
                }
            }
            return true;
        }
    }
}
