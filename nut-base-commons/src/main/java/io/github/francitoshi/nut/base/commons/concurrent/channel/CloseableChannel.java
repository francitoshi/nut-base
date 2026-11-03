/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.Iterator;

/**
 * Contract of the channels that can be closed to signal end-of-data.
 * <p>
 * A {@code CloseableChannel} combines the read side and the write side with
 * the {@link ChannelCloser} capability. The closing state itself
 * ({@code isClosed()}, {@code join()}, the terminating iteration) is shared
 * machinery of the {@link Channel} base class, so each capacity policy does
 * not need to re-implement it: {@code CloseableBufferedChannel} extends
 * {@link BufferedChannel}, {@code CloseableUnbufferedChannel} extends
 * {@link UnbufferedChannel}, and so on, contributing to each of them only the
 * closing behavior that is specific to their engine (how blocked operations
 * are aborted at close time and how the remaining values are drained).
 * <p>
 * Closing is irrevocable and idempotent: {@link #close()} returns the same
 * first-call value and, once closed, further {@code put}s are rejected
 * ({@link IllegalStateException} or {@code false}) and {@code get}s drain the
 * remaining values and then return {@code null}.
 *
 * @param <E> the element type
 * @see Channel#closeableOf(int)
 * @see ChannelCloser
 */
public interface CloseableChannel<E> extends ChannelReader<E>, ChannelWriter<E>, ChannelCloser
{
    /**
     * Returns whether an {@link InterruptedException} has ever been raised in
     * this channel: see {@link Channel#isInterrupted()}.
     *
     * @return {@code true} if at least one operation was interrupted
     */
    boolean isInterrupted();

    /**
     * Returns an iterator over the values read from this closeable channel,
     * which terminates once the channel is closed and drained. Each call
     * creates an iterator specific to that occasion.
     * <p>
     * The dispatch is implemented centrally in {@link Channel}, so no
     * {@code CloseableX} needs to override it; declared here only for
     * documentation symmetry.
     *
     * @return a new {@code Iterator} reading from this channel
     */
    Iterator<E> iterator();
}
