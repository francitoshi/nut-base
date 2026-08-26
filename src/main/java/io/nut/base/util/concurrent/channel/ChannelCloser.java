/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

/**
 * Capability interface for channels that can be closed to signal end-of-data.
 * <p>
 * A closeable channel allows the producer to explicitly indicate that no more
 * elements will be written. Once {@link #close()} is called:
 * <ul>
 *   <li>{@link ChannelWriter#put} throws {@link IllegalStateException}.</li>
 *   <li>{@link ChannelReader#get} drains any remaining buffered elements and
 *       then returns {@code null} (or the timed variant returns {@code null}).</li>
 * </ul>
 * <p>
 * The {@link #join()} method provides a blocking wait until the channel is
 * closed, useful for consumer threads that need to know when the producer
 * has finished.
 * <p>
 * Implementations are typically provided by the {@link CloseableChannel} family
 * ({@link CloseableBufferedChannel}, {@link CloseableUnbufferedChannel},
 * {@link CloseableUnlimitedChannel}, {@link CloseableConflatedChannel}).
 *
 * @see CloseableChannel
 * @see Channel#closeableOf(int)
 */
public interface ChannelCloser
{
    /**
     * Closes the channel, signaling that no more elements will be written.
     * <p>
     * After this call returns, any subsequent {@link ChannelWriter#put} will
     * throw {@link IllegalStateException}, and {@link ChannelReader#get} will
     * drain remaining buffered elements before returning {@code null}.
     * <p>
     * This method is idempotent: calling it more than once has no additional
     * effect and returns the same value as the first call.
     *
     * @return {@code true} if the channel was successfully closed and all
     *         pending readers were unblocked (in-progress reads may have
     *         returned a final value); {@code false} if the close could not
     *         complete within an implementation-defined timeout
     */
    public boolean close();

    /**
     * Returns whether this channel has been closed via {@link #close()}.
     *
     * @return {@code true} if {@link #close()} has been called;
     *         {@code false} otherwise
     */
    public boolean isClosed();

    /**
     * Blocks the current thread until the channel is closed.
     * If the channel is already closed, this method returns immediately.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void join() throws InterruptedException;
}
