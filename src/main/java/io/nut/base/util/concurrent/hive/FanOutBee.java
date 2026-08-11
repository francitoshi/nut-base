/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A pipeline stage that fans out every received message to a set of downstream
 * target stages, allowing the same input to feed multiple independent chains in
 * parallel.
 * <p>
 * Each message delivered to {@link #receive(Object)} is forwarded — unchanged
 * and in order — to every registered {@code Sendable<T>} target by calling
 * {@link Consumer#send send()} on each of them in turn. Because the targets are
 * invoked from the same worker thread, the fan-out itself is sequential; true
 * parallelism is achieved when each target is backed by its own Hive worker.
 * <p>
 * Targets can be supplied at construction time and/or added or removed later
 * with {@link #addTarget(Sendable)} / {@link #removeTarget(Sendable)}. The
 * target list is backed by a {@link CopyOnWriteArrayList}, making concurrent
 * mutation safe without blocking message delivery.
 * <p>
 * <strong>Fan-in</strong> (the inverse pattern, merging several sources into
 * one consumer) needs no dedicated class: any number of producers can simply
 * call {@link Consumer#send send()} on the same downstream {@link Bee}.
 * <p>
 * Example:
 * <pre>{@code
 * FanOutBee<String> bc = hive.broadcast();
 * bc.addTarget(hive.bee(s -> saveToDb(s)));
 * bc.addTarget(hive.bee(s -> publishToKafka(s)));
 * bc.send("hello");  // both targets receive "hello"
 * }</pre>
 *
 * @param <T> the type of messages this FanOutBee receives and forwards
 *            unchanged to every target
 */
public class FanOutBee<T> extends Bee<T>
{
    /**
     * The list of downstream targets. Using {@link CopyOnWriteArrayList} allows
     * {@link #addTarget} and {@link #removeTarget} to be called concurrently
     * with ongoing message delivery without requiring synchronization in
     * {@link #receive(Object)}.
     */
    protected final List<Consumer<T>> targets = new CopyOnWriteArrayList<>();

    /**
     * Full constructor.
     *
     * @param threads   the maximum number of concurrent worker threads
     * @param hive      the Hive thread pool, or {@code null} for synchronous mode
     * @param queueSize the internal queue capacity (0 = default)
     * @param targets   zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutBee(int threads, Hive hive, int queueSize, Consumer<T>... targets)
    {
        super(threads, hive, queueSize);
        addTargets(targets);
    }

    /**
     * Constructs a FanOutBee with the given thread count and Hive, using the
     * default queue size.
     *
     * @param threads the maximum number of concurrent worker threads
     * @param hive    the Hive thread pool, or {@code null} for synchronous mode
     * @param targets zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutBee(int threads, Hive hive, Consumer<T>... targets)
    {
        super(threads, hive);
        addTargets(targets);
    }

    /**
     * Constructs a FanOutBee attached to the given Hive with the default
     * thread count and queue size.
     *
     * @param hive    the Hive thread pool, or {@code null} for synchronous mode
     * @param targets zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutBee(Hive hive, Consumer<T>... targets)
    {
        super(hive);
        addTargets(targets);
    }

    /**
     * Constructs a standalone FanOutBee with the given thread count but no
     * Hive. A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param threads the maximum number of concurrent worker threads
     * @param targets zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutBee(int threads, Consumer<T>... targets)
    {
        super(threads);
        addTargets(targets);
    }

    /**
     * Constructs a standalone FanOutBee with the default thread count and no
     * Hive. A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param targets zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutBee(Consumer<T>... targets)
    {
        super();
        addTargets(targets);
    }

    /**
     * Bulk-adds an array of targets, used by all constructors to initialise the
     * target list.
     *
     * @param array the targets to register; individual elements must not be
     *              {@code null}
     */
    private void addTargets(Consumer<T>[] array)
    {
        for (Consumer<T> target : array)
        {
            addTarget(target);
        }
    }

    /**
     * Registers a new target that will receive every message from this point
     * forward.
     *
     * @param target the downstream stage to add; must not be {@code null}
     * @return this FanOutBee, for fluent chaining of additions
     */
    public FanOutBee<T> addTarget(Consumer<T> target)
    {
        this.targets.add(Objects.requireNonNull(target, "target must not be null"));
        return this;
    }

    /**
     * Removes a previously registered target so that it stops receiving
     * messages.
     *
     * @param target the downstream stage to remove
     * @return {@code true} if the target was present and has been removed;
     *         {@code false} if it was not found
     */
    public boolean removeTarget(Consumer<T> target)
    {
        return this.targets.remove(target);
    }

    /**
     * Returns an unmodifiable snapshot view of the current target list.
     *
     * @return an unmodifiable {@code List} of the registered downstream stages
     */
    public List<Consumer<T>> getTargets()
    {
        return Collections.unmodifiableList(targets);
    }

    /**
     * Forwards {@code m} to every registered target by calling
     * {@link Consumer#send send(m)} on each in turn. Targets added or removed
     * concurrently during this call are handled safely by the underlying
     * {@link CopyOnWriteArrayList}.
     *
     * @param m the message to broadcast
     */
    @Override
    protected void receive(T m)
    {
        for (Consumer<T> target : targets)
        {
            target.accept(m);
        }
    }

    /**
     * {@inheritDoc}
     * Overridden to return the more specific {@code FanOutBee<T>} type for
     * fluent chaining.
     */
    @Override
    public FanOutBee<T> shutdown(boolean onlyWhenEmpty)
    {
        return (FanOutBee<T>) super.shutdown(onlyWhenEmpty);
    }

    @Override
    public Bee<T> waitForIdle()
    {
        for (Consumer<T> target : targets)
        {
            if(target instanceof Bee)
            {
                ((Bee<T>)target).waitForIdle();
            }
        }
        return super.waitForIdle();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return (Collection) targets;
    }
}
