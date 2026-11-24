/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.TimeUnit;

/**
 * The write side of a channel: writes and closing.
 * <p>
 * Every write is blocking and has a variant with a time limit, and both
 * return a boolean: {@code true} when the value was left in the channel,
 * {@code false} when it was not (interrupted or timed out). A write on a
 * closed channel throws {@link IllegalStateException} in both variants: the
 * {@code false} is never the way a channel says "closed".
 * <p>
 * Only the producer closes a channel: {@link #close()} lives on this
 * interface and not on {@link ChannelReader}, so a consumer cannot close a
 * channel it only reads from.
 *
 * @param <E> the element type
 * @see ChannelReader
 * @see Channel
 */
public interface ChannelWriter<E>
{
    /**
     * Writes a value, blocking until it is delivered.
     *
     * @param value the value to write; must not be {@code null}
     * @return {@code true} if the value was left in the channel;
     *         {@code false} when the write was interrupted
     * @throws IllegalStateException if the channel is closed
     */
    public boolean put(E value);

    /**
     * Writes a value, blocking at most the given time. A non-positive
     * timeout makes an immediate non-blocking attempt.
     *
     * @param value   the value to write; must not be {@code null}
     * @param timeout the maximum time to wait
     * @param unit    the time unit of {@code timeout}
     * @return {@code true} if the value was left in the channel;
     *         {@code false} when the write was interrupted or timed out
     * @throws IllegalStateException if the channel is closed
     */
    public boolean put(E value, long timeout, TimeUnit unit);

    /**
     * Attempts to write a value without blocking.
     * <p>
     * The default implementation delegates to {@link #put(Object, long,
     * TimeUnit)} with a zero timeout, so implementations must treat a zero
     * timeout as a non-blocking attempt. Unlike the mandatory {@code put}s,
     * {@code tryPut} is a best-effort variant: it returns {@code false} when
     * the value was not delivered, whatever the reason (closed channel, no
     * room, interruption); it never throws for a closed channel.
     *
     * @param value the value to write
     * @return {@code true} if the value was delivered; {@code false} otherwise
     */
    public default boolean tryPut(E value)
    {
        return put(value, 0L, TimeUnit.NANOSECONDS);
    }

    /**
     * Closes the channel, signaling that no more elements will be written.
     * <p>
     * After this call returns, any subsequent {@link #put(Object)} throws
     * {@link IllegalStateException}, and {@link ChannelReader#get} drains the
     * remaining elements before returning {@code null}.
     * <p>
     * This method is irrevocable and idempotent: calling it more than once
     * has no additional effect and returns the same value as the first call.
     *
     * @return {@code true} if the close completed (in-flight writes were
     *         given their chance to abort); {@code false} if it could not
     */
    public boolean close();
}
