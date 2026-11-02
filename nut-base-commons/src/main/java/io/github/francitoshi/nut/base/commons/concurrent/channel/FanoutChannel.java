/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.List;
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
 * <strong>Select support.</strong> Operation listeners registered here are
 * forwarded to every target, so {@link Channel#selectPut} on a fan-out wakes
 * up when any target reports activity (a value was drained or a broadcast
 * completed). {@link #tryPut} is a best-effort broadcast: it offers the value
 * to every target without blocking and returns {@code true} only when all of
 * them accepted it.
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

    /** One forwarding wrapper per registered operation listener per target. */
    private final List<Forwarder> forwarders = new CopyOnWriteArrayList<>();

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
     * to this fan-out from this point forward. The already registered
     * operation listeners are attached to the new target as well.
     *
     * @param target the channel to add; must not be {@code null}
     * @return this fan-out, for fluent chaining
     */
    public FanoutChannel<E> addTarget(ChannelWriter<E> target)
    {
        this.targets.add(Objects.requireNonNull(target, "target must not be null"));
        for (Forwarder forwarder : forwarders)
        {
            target.addOperationListener(forwarder);
        }
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
        boolean removed = this.targets.remove(target);
        if (removed)
        {
            for (Forwarder forwarder : forwarders)
            {
                target.removeOperationListener(forwarder);
            }
        }
        return removed;
    }

    /**
     * Writes {@code value} to every registered target, in registration order.
     * <p>
     * If any target's {@link ChannelWriter#put} blocks (e.g. the target is
     * full and bounded), subsequent targets wait until the previous write
     * completes, providing natural back-pressure from the slowest consumer.
     * <p>
     * If the current thread is interrupted while broadcasting, the interrupted
     * target aborts its {@code put} (see the interruption contract of the
     * concrete channel) and the broadcast returns without completing the
     * remaining destinations.
     *
     * @param value the value to broadcast; may be {@code null} if the
     *              targets accept {@code null} elements
     */
    @Override
    public void put(E value)
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
     */
    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
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

    /**
     * Best-effort non-blocking broadcast: offers {@code value} to every
     * registered target without blocking and returns {@code true} only when
     * all of them accepted it.
     */
    @Override
    public boolean tryPut(E value)
    {
        boolean delivered = true;
        for (ChannelWriter<E> target : targets)
        {
            delivered &= target.tryPut(value);
        }
        return delivered;
    }

    /**
     * Registers a listener that is forwarded to the targets: it is notified
     * whenever any target reports activity (a get, a put or a close).
     *
     * @param listener the listener to register
     */
    @Override
    public void addOperationListener(OperationListener<E> listener)
    {
        Objects.requireNonNull(listener, "listener");
        for (ChannelWriter<E> target : targets)
        {
            Forwarder forwarder = new Forwarder(listener);
            forwarders.add(forwarder);
            target.addOperationListener(forwarder);
        }
    }

    /**
     * Removes a previously registered operation listener.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeOperationListener(OperationListener<E> listener)
    {
        for (int all = forwarders.size(), i = all - 1; i >= 0; i--)
        {
            if (forwarders.get(i).user == listener)
            {
                forwarders.remove(i);
            }
        }
    }

    private final class Forwarder implements OperationListener<E>
    {
        final OperationListener<E> user;

        Forwarder(OperationListener<E> user)
        {
            this.user = user;
        }

        @Override
        public void onOperation(ChannelObservable<E> channel, ChannelObservable.Operation operation, E value)
        {
            user.onOperation(channel, operation, value);
        }
    }
}
