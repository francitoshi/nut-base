/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.TimeUnit;

public interface ChannelWriter<E> extends ChannelObservable<E>
{
    public void put(E value);

    public boolean put(E value, long timeout, TimeUnit unit);

    /**
     * Attempts to send a value without blocking.
     * <p>
     * This is the primitive used by {@link Channel#selectPut}: the select can
     * test whether a channel is ready by attempting the operation here. The
     * default implementation delegates to {@link #put(Object, long, TimeUnit)}
     * with a zero timeout, so implementations must treat a zero timeout as a
     * non-blocking attempt.
     *
     * @param value the value to send
     * @return {@code true} if the value was delivered; {@code false} otherwise
     */
    public default boolean tryPut(E value)
    {
        return put(value, 0L, TimeUnit.NANOSECONDS);
    }
}
