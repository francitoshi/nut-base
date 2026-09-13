/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

/**
 * Closeable unbuffered (rendezvous) channel.
 * <p>
 * {@link #close()} marks the channel as closed, unblocks all pending readers
 * (they return {@code null}) and aborts any {@link #put} in progress: a
 * blocked {@code put} returns without delivering its value and the timed
 * variant returns {@code false}.
 */
public final class CloseableUnbufferedChannel<E> extends CloseableBufferedChannel<E>
{
    public CloseableUnbufferedChannel()
    {
        super(0);
    }
}
