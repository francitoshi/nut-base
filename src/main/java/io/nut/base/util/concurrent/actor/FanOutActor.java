/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

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
 * and in order — to every registered {@link Consumer}{@code <T>} target by
 * calling {@link Consumer#accept accept()} on each of them in turn. Because the
 * targets are invoked from the same worker thread, the fan-out itself is
 * sequential; true parallelism is achieved when each target is backed by its
 * own ActorHub worker.
 * <p>
 * Targets can be supplied at construction time and/or added or removed later
 * with {@link #addTarget(Consumer)} / {@link #removeTarget(Consumer)}. The
 * target list is backed by a {@link CopyOnWriteArrayList}, making concurrent
 * mutation safe without blocking message delivery.
 * <p>
 * <strong>Fan-in</strong> (the inverse pattern, merging several sources into
 * one consumer) needs no dedicated class: any number of producers can simply
 * call {@link Consumer#accept accept()} on the same downstream {@link Actor}.
 * <p>
 * Example:
 * <pre>{@code
 * FanOutActor<String> bc = actorHub.broadcast();
 * bc.addTarget(actorHub.actor(s -> saveToDb(s)));
 * bc.addTarget(actorHub.actor(s -> publishToKafka(s)));
 * bc.accept("hello");  // both targets receive "hello"
 * }</pre>
 *
 * @param <T> the type of messages this FanOutActor receives and forwards
 *            unchanged to every target
 */
public class FanOutActor<T> extends Actor<T>
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
     * @param actorHub      the ActorHub thread pool, or {@code null} for synchronous mode
     * @param queueSize the internal queue capacity (0 = default)
     * @param targets   zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutActor(ActorHub actorHub, int threads, int queueSize, Consumer<T>... targets)
    {
        super(actorHub, threads, queueSize);
        addTargets(targets);
    }

    /**
     * Constructs a FanOutActor attached to the given ActorHub with the default
     * thread count and queue size.
     *
     * @param actorHub    the ActorHub thread pool, or {@code null} for synchronous mode
     * @param targets zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutActor(ActorHub actorHub, Consumer<T>... targets)
    {
        super(actorHub);
        addTargets(targets);
    }

    /**
     * Constructs a standalone FanOutActor with the given thread count but no
     * ActorHub. A ActorHub is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param threads the maximum number of concurrent worker threads
     * @param targets zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutActor(int threads, int queueSize, Consumer<T>... targets)
    {
        super(threads, queueSize);
        addTargets(targets);
    }

    /**
     * Constructs a standalone FanOutActor with the default thread count and no
     * ActorHub. A ActorHub is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param targets zero or more initial downstream stages
     */
    @SafeVarargs
    public FanOutActor(Consumer<T>... targets)
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
     * @return this FanOutActor, for fluent chaining of additions
     */
    public FanOutActor<T> addTarget(Consumer<T> target)
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
     * {@link Consumer#accept accept(m)} on each in turn. Targets added or removed
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
     * Overridden to return the more specific {@code FanOutActor<T>} type for
     * fluent chaining.
     */
    @Override
    public FanOutActor<T> shutdown(boolean onlyWhenEmpty)
    {
        return (FanOutActor<T>) super.shutdown(onlyWhenEmpty);
    }

    @Override
    public Actor<T> waitForIdle()
    {
        for (Consumer<T> target : targets)
        {
            if(target instanceof Actor)
            {
                ((Actor<T>)target).waitForIdle();
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
