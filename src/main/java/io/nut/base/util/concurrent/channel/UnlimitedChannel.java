/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Unbounded and non-closeable channel: it lives as long as the JVM lives (or
 * as long as the object itself is referenced). There is no close() operation,
 * so put()/get() are delegated directly to LinkedBlockingQueue without any
 * additional state or synchronization.
 *
 * Since it has no bounded capacity, put() never blocks waiting for room: the
 * value is always enqueued and returns immediately (except for memory
 * exhaustion).
 */
public final class UnlimitedChannel<E> extends Channel<E>
{
    private final LinkedBlockingQueue<E> queue = new LinkedBlockingQueue<>();

    @Override
    public void put(E value) throws InterruptedException
    {
        Objects.requireNonNull(value, "value must not be null");
        queue.put(value);
    }

    @Override
    public E get() throws InterruptedException
    {
        return queue.take();
    }
}
