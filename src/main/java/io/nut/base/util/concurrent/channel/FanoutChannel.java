/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * A write endpoint that broadcasts every {@link #put} to a set of downstream
 * {@link ChannelWriter} destinations.
 * <p>
 * {@code FanoutChannel} implements {@link ChannelWriter} so it can be passed
 * anywhere a single write target is expected, while internally dispatching to
 * multiple destinations. It does <strong>not</strong> manage the lifecycle of
 * its targets — use {@link CloseableFanoutChannel} if closing the fan-out
 * should propagate end-of-data to closeable destinations.
 * <p>
 * <strong>Thread safety:</strong> targets are stored in a
 * {@link CopyOnWriteArrayList}, so {@link #addTarget} and {@link #removeTarget}
 * may be called concurrently with {@link #put}.
 * <p>
 * Example:
 * <pre>{@code
 * Channel<String> dest1 = Channel.buffered(10);
 * Channel<String> dest2 = Channel.buffered(10);
 *
 * FanoutChannel<String> fan = new FanoutChannel<>(dest1, dest2);
 * fan.put("hello");   // broadcast to dest1 and dest2
 * fan.put("world");
 * }</pre>
 *
 * @param <E> the element type
 * @see ChannelWriter
 * @see CloseableFanoutChannel
 */
public class FanoutChannel<E> implements ChannelWriter<E>
{
    final CopyOnWriteArrayList<ChannelWriter<E>> targets = new CopyOnWriteArrayList<>();

    /**
     * Creates a fan-out channel with the given initial destinations.
     *
     * @param targets the downstream channels to broadcast to; individual
     *                elements must not be {@code null}
     */
    @SafeVarargs
    public FanoutChannel(ChannelWriter<E>... targets)
    {
        for (ChannelWriter<E> t : targets)
        {
            addTarget(t);
        }
    }

    /**
     * Registers a new downstream target that will receive every value written
     * to this fan-out from this point forward.
     *
     * @param target the channel to add; must not be {@code null}
     * @return this fan-out, for fluent chaining
     */
    public FanoutChannel<E> addTarget(ChannelWriter<E> target)
    {
        this.targets.add(Objects.requireNonNull(target, "target must not be null"));
        return this;
    }

    /**
     * Removes a previously registered target so that it no longer receives
     * values from this fan-out.
     *
     * @param target the channel to remove
     * @return {@code true} if the target was present and has been removed;
     *         {@code false} if it was not found
     */
    public boolean removeTarget(ChannelWriter<E> target)
    {
        return this.targets.remove(target);
    }

    /**
     * Writes {@code value} to every registered target, in registration order.
     * <p>
     * If any target's {@link ChannelWriter#put} blocks (e.g. the target is
     * full and bounded), subsequent targets wait until the previous write
     * completes, providing natural back-pressure from the slowest consumer.
     *
     * @param value the value to broadcast; may be {@code null} if the
     *              targets accept {@code null} elements
     * @throws InterruptedException if the current thread is interrupted
     *         while writing to any target
     */
    @Override
    public void put(E value) throws InterruptedException
    {
        for (ChannelWriter<E> target : targets)
        {
            target.put(value);
        }
    }

    /**
     * Writes {@code value} to every registered target with a per-target
     * timeout. Returns {@code false} if any target rejects the value (full
     * or closed).
     *
     * @param value   the value to broadcast
     * @param timeout maximum time to wait per target
     * @param unit    the time unit of the timeout
     * @return {@code true} if the value was written to all targets;
     *         {@code false} otherwise
     * @throws InterruptedException if the current thread is interrupted
     */
    @Override
    public boolean put(E value, long timeout, TimeUnit unit) throws InterruptedException
    {
        for (ChannelWriter<E> target : targets)
        {
            if (!target.put(value, timeout, unit))
            {
                return false;
            }
        }
        return true;
    }
}
