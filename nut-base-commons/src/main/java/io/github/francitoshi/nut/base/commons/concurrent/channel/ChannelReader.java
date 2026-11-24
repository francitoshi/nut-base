/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The read side of a channel: reads and nothing else.
 * <p>
 * Every read is blocking and has a variant with a time limit. An
 * {@link InterruptedException} raised while a read waits makes the read yield
 * {@code null} without consuming anything (see the interruption rules in
 * {@code doc/channel-rules.txt}); a channel that is closed and drained also
 * yields {@code null}, so {@link #isClosed()} resolves the ambiguity.
 *
 * @param <E> the element type
 * @see ChannelWriter
 * @see Channel
 */
public interface ChannelReader<E>
{
    /**
     * Reads a value, blocking until one is available. On a closed channel it
     * drains the remaining values and then returns {@code null}.
     *
     * @return the value, or {@code null} when the channel is closed and
     *         drained, or when the read was interrupted
     */
    public E get();

    /**
     * Reads a value, blocking at most the given time. A non-positive timeout
     * makes an immediate non-blocking attempt.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of {@code timeout}
     * @return the value, or {@code null} when nothing arrived in time, when
     *         the channel is closed and drained, or when the read was
     *         interrupted
     */
    public E get(long timeout, TimeUnit unit);

    /**
     * Attempts to receive a value without blocking.
     * <p>
     * The default implementation delegates to {@link #get(long, TimeUnit)}
     * with a zero timeout, so implementations must treat a zero timeout as a
     * non-blocking attempt.
     *
     * @return the received value, or an empty {@link Optional} if no value is
     *         available; implementations must not use {@code null} as a
     *         channel element if they rely on the default implementation
     */
    public default Optional<E> tryGet()
    {
        return Optional.ofNullable(get(0L, TimeUnit.NANOSECONDS));
    }

    /**
     * Returns whether this channel has been closed via
     * {@link ChannelWriter#close()}.
     * <p>
     * A {@code null} from {@link #get()} is ambiguous between "the read was
     * interrupted" and "the channel is closed and drained": this method
     * resolves it.
     *
     * @return {@code true} if the channel has been closed;
     *         {@code false} otherwise
     */
    public boolean isClosed();
}
