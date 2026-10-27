/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

public class Pauses
{
    private static final Logger LOG = Logger.getLogger(Pauses.class.getName());
    
    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds, subject to the
     * precision and accuracy of system timers and schedulers. The thread does
     * not lose ownership of any monitors.
     *
     * @param millis the length of time to sleep in milliseconds.
     * @return true if thread was interrupted; false otherwise.
     */
    public static boolean sleepMillis(long millis)
    {
        try
        {
            Thread.sleep(millis);
            return false;
        }
        catch (InterruptedException ex)
        {
            Exceptions.severe(LOG, ex);
            return true;
        }
    }

    /**
     * Causes the currently executing thread to sleep (cease execution) for the
     * specified number of milliseconds plus the specified number of
     * nanoseconds, subject to the precision and accuracy of system timers and
     * schedulers. The thread does not lose ownership of any monitors.
     *
     * @param nanos nanoseconds to sleep.
     * @return true if thread was interrupted; false otherwise.
     */
    public static boolean sleepNanos(long nanos)
    {
        if (nanos <= 0)
        {
            return false;
        }

        long millis = nanos / 1_000_000L;
        int remainingNanos = (int) (nanos % 1_000_000L); // siempre en 0..999_999
        
        try
        {
            Thread.sleep(millis, remainingNanos);
            return false;
        }
        catch (InterruptedException ex)
        {
            Exceptions.severe(LOG, ex);
            return true;
        }
    }
    
    /**
     * Parks the current thread until a specified deadline, reliably handling spurious wakeups.
     * <p>
     * This method first calculates the total duration to wait. It then enters a loop that
     * repeatedly parks the thread until the deadline, as measured by {@link System#nanoTime()},
     * is reached. If the thread wakes up prematurely (due to an interrupt or a spurious wakeup),
     * the loop re-evaluates the remaining time and parks again. This ensures that the method
     * does not return before the deadline has passed.
     * <p>
     * The value returned is the initial duration calculated upon the method's entry,
     * providing a consistent report of the intended wait time, regardless of any
     * spurious wakeups that may have occurred.
     *
     * @param deadlineNano The absolute deadline time in nanoseconds, as measured by
     *                     {@link System#nanoTime()}. This is typically calculated as
     *                     {@code System.nanoTime() + desiredDelayInNanos}.
     * @return The initial calculated duration to wait in nanoseconds. This value is computed
     *         once at the beginning of the method and is not affected by how many times the
     *         thread parked. If the deadline had already passed upon method entry, this method
     *         returns the corresponding non-positive duration (i.e., zero or a negative number).
     * @see System#nanoTime()
     * @see java.util.concurrent.locks.LockSupport#parkNanos(long)
     */
    public static long parkUntilNanoTime(long deadlineNano)
    {
        long now = System.nanoTime();
        long remaining = deadlineNano - now;
        final long waitNanos = remaining;
        for(;;)
        {
            if(remaining <= 0)
            {
                return waitNanos;
            }
            // Sleep the rest of the time (may wake up earlier)
            LockSupport.parkNanos(remaining);
            now = System.nanoTime();
            remaining = deadlineNano - now;
        }
    }
   
    /**
     * Suspends the current thread for the specified number of nanoseconds.
     * <p>
     * The thread may wake up earlier than requested due to a call to
     * {@link LockSupport#unpark(Thread)}, a thread interruption, or a spurious
     * wakeup. In all these cases the method returns silently without throwing
     * an exception, so the caller should re-check the wait condition if
     * precision matters.
     *
     * @param nanos maximum time to suspend the thread, in nanoseconds; values
     * &le; 0 cause the method to return immediately.
     */    
    public static void parkNanos(long nanos)
    {
        LockSupport.parkNanos(nanos);
    }

    /**
     * Suspends the current thread for the specified number of milliseconds.
     * <p>
     * Convenience wrapper around {@link #parkNanos(long)} that converts the
     * given millisecond duration to nanoseconds. The same early-wakeup caveats
     * apply: the thread may resume before the timeout elapses due to an
     * {@link LockSupport#unpark(Thread)} call, a thread interruption, or a
     * spurious wakeup.
     *
     * @param millis maximum time to suspend the thread, in milliseconds; values
     * &le; 0 cause the method to return immediately.
     */
    public static void parkMillis(long millis)
    {
        parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
    }
    
}
