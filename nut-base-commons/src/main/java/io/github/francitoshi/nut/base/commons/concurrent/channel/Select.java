/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Go-like {@code select} over the read side of several channels: waits until
 * one of them has a value and returns it, consuming exactly one value.
 *
 * <p><b>Semantics</b>
 * <ul>
 *   <li><b>Fairness:</b> every scan starts at a random channel, so a channel
 *       that is always ready cannot starve the others.</li>
 *   <li><b>Closed channels</b> ({@link ChannelCloser}) are drained first and
 *       then excluded from the set. When all of them are closed and empty the
 *       select ends with {@link SelectResult.Status#CLOSED}. (Go would
 *       return zero values from a closed channel forever; that would be a busy
 *       loop here.)</li>
 *   <li><b>{@code null}</b> returned by the plain {@code T select(...)}
 *       variants means "no value": all closed, timeout, not ready or
 *       interrupted. It is unambiguous because channels never carry
 *       {@code null}. Use the {@code *Indexed} variants to tell them apart.</li>
 *   <li><b>Interruption:</b> following the interruption contract of
 *       {@link Channel}, the interruption is consumed (the flag is not
 *       re-set) and the select returns
 *       {@link SelectResult.Status#INTERRUPTED}. Unlike a single channel
 *       operation, a select does <em>not</em> close any of the channels it was
 *       waiting on.</li>
 *   <li>The same channel may be selected concurrently by several threads;
 *       every value is delivered to exactly one of them.</li>
 * </ul>
 *
 * <p><b>Implementation.</b> Each blocking select registers a
 * {@link SelectSignal} in every channel, then loops: read the signal version,
 * do one non-blocking scan ({@code get(0)}), and, if nothing was found, wait
 * for the version to change. Channels fire the signal after every successful
 * {@code put} and on {@code close()}, so there is no polling on buffered,
 * unlimited and conflated channels.
 *
 * <p><b>Rendezvous channels</b> ({@code UnbufferedChannel},
 * {@code CloseableUnbufferedChannel}, capacity 0): a value is only visible
 * while the writer is parked inside the queue, which happens after it has
 * fired the signal. For those channels (and for a {@link DuplexChannel} whose
 * input is not a {@link Channel}) the select waits in short slices of
 * {@value #POLL_MICROS} microseconds instead of indefinitely. A select can
 * therefore only <em>receive</em> from an unbuffered channel against a
 * writer calling {@code put}; select-send is out of scope here.
 */
public final class Select
{
    private static final int POLL_MICROS = 200;
    private static final long POLL_NANOS = TimeUnit.MICROSECONDS.toNanos(POLL_MICROS);

    private Select()
    {
    }

    // -----------------------------------------------------------------------
    // Blocking
    // -----------------------------------------------------------------------

    /**
     * Blocks until one of the channels yields a value.
     *
     * @return the value, or {@code null} if all channels are closed and
     *         drained or the thread was interrupted
     */
    @SafeVarargs
    public static <T> T select(Channel<T>... channels)
    {
        return selectIndexed(channels).value();
    }

    /**
     * Like {@link #select(Channel[])} but gives up after {@code timeout}
     * (the equivalent of {@code case <-time.After(...)}).
     *
     * @return the value, or {@code null} on timeout, all closed or interrupted
     */
    @SafeVarargs
    public static <T> T select(long timeout, TimeUnit unit, Channel<T>... channels)
    {
        return selectIndexed(timeout, unit, channels).value();
    }

    /** Rich-result variant of {@link #select(Channel[])}. */
    @SafeVarargs
    public static <T> SelectResult<T> selectIndexed(Channel<T>... channels)
    {
        return run(channels, true, false, 0, null);
    }

    /** Rich-result variant of {@link #select(long, TimeUnit, Channel[])}. */
    @SafeVarargs
    public static <T> SelectResult<T> selectIndexed(long timeout, TimeUnit unit, Channel<T>... channels)
    {
        Objects.requireNonNull(unit, "unit must not be null");
        return run(channels, true, true, timeout, unit);
    }

    // -----------------------------------------------------------------------
    // Non-blocking (Go's "default:" case)
    // -----------------------------------------------------------------------

    /**
     * Single non-blocking scan.
     *
     * @return a value if some channel had one ready, otherwise {@code null}
     */
    @SafeVarargs
    public static <T> T trySelect(Channel<T>... channels)
    {
        return trySelectIndexed(channels).value();
    }

    /** Rich-result variant of {@link #trySelect(Channel[])}. */
    @SafeVarargs
    public static <T> SelectResult<T> trySelectIndexed(Channel<T>... channels)
    {
        return run(channels, false, false, 0, null);
    }

    // -----------------------------------------------------------------------
    // Engine
    // -----------------------------------------------------------------------

    private static <T> SelectResult<T> run(Channel<T>[] channels, boolean block, boolean timed, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(channels, "channels must not be null");
        if (channels.length == 0)
        {
            throw new IllegalArgumentException("at least one channel is required");
        }
        for (Channel<T> ch : channels)
        {
            Objects.requireNonNull(ch, "channel must not be null");
        }

        final int n = channels.length;
        final long deadline = timed ? Channel.toDeadline(timeout, unit) : 0;
        final boolean[] exhausted = new boolean[n];
        int open = n;

        final SelectSignal signal = block ? new SelectSignal() : null;
        boolean polling = false;
        try
        {
            if (block)
            {
                // Register BEFORE the first scan so no event can slip through.
                for (Channel<T> ch : channels)
                {
                    if (!ch.register(signal) || ch.needsPolling())
                    {
                        polling = true;
                    }
                }
            }

            for (;;)
            {
                if (Thread.interrupted())
                {
                    return SelectResult.interrupted();
                }

                // Version BEFORE the scan: anything fired after this point
                // makes awaitChange return immediately.
                long seen = block ? signal.version() : 0;

                int start = n == 1 ? 0 : ThreadLocalRandom.current().nextInt(n);
                for (int k = 0; k < n; k++)
                {
                    int i = (start + k) % n;
                    if (exhausted[i])
                    {
                        continue;
                    }
                    Channel<T> ch = channels[i];
                    // Closed state is sampled BEFORE reading: if it was already
                    // closed and the read finds nothing, the channel is drained.
                    boolean wasClosed = ch.isReadClosed();
                    T value = ch.get(0, TimeUnit.NANOSECONDS);
                    if (value != null)
                    {
                        return SelectResult.value(i, value);
                    }
                    if (wasClosed)
                    {
                        exhausted[i] = true;
                        open--;
                    }
                }

                if (open == 0)
                {
                    return SelectResult.closed();
                }
                if (!block)
                {
                    return SelectResult.notReady();
                }

                long wait = Long.MAX_VALUE;
                if (timed)
                {
                    wait = deadline - System.nanoTime();
                    if (wait <= 0)
                    {
                        return SelectResult.timeout();
                    }
                }
                if (polling)
                {
                    wait = Math.min(wait, POLL_NANOS);
                }

                try
                {
                    signal.awaitChange(seen, wait);
                }
                catch (InterruptedException ex)
                {
                    return SelectResult.interrupted();
                }
            }
        }
        finally
        {
            if (block)
            {
                for (Channel<T> ch : channels)
                {
                    ch.unregister(signal);
                }
            }
        }
    }
}
