/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface ChannelReader<E> extends ChannelObservable<E>
{
    public E get();

    public E get(long timeout, TimeUnit unit);

    /**
     * Attempts to receive a value without blocking.
     * <p>
     * This is the primitive used by {@link Channel#selectGet}: the select can
     * test whether a channel is ready by attempting the operation here. The
     * default implementation delegates to {@link #get(long, TimeUnit)} with a
     * zero timeout, so implementations must treat a zero timeout as a
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
}
