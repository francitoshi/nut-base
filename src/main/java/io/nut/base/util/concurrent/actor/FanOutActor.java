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
 *
 * @param <T> the type of messages this FanOutActor receives and forwards
 */
public class FanOutActor<T> implements Consumer<T>, Linkable
{
    protected Actor<T> inner;
    protected final List<Consumer<T>> targets = new CopyOnWriteArrayList<>();

    @SafeVarargs
    public FanOutActor(ActorHub actorHub, int threads, int queueSize, Consumer<T>... targets)
    {
        ActorHooks<T> hooks = new ActorHooks<T>()
        {
            @Override
            public void receive(T m, long seq)
            {
                FanOutActor.this.broadcast(m);
            }

            @Override
            public void terminate()
            {
            }

            @Override
            public void exception(Exception ex)
            {
                // No-op: exceptions from targets are already recorded by
                // the inner flavor via Actor.handleException.
            }
        };
        this.inner = ActorFlavors.create(actorHub, threads, queueSize, hooks);
        addTargets(targets);
    }

    @SafeVarargs
    public FanOutActor(ActorHub actorHub, Consumer<T>... targets)
    {
        this(actorHub, 1, ActorPool.CORES, targets);
    }

    @SafeVarargs
    public FanOutActor(int threads, int queueSize, Consumer<T>... targets)
    {
        this(null, threads, queueSize, targets);
    }

    @SafeVarargs
    public FanOutActor(Consumer<T>... targets)
    {
        this(null, 1, ActorPool.CORES, targets);
    }

    private void addTargets(Consumer<T>[] array)
    {
        for (Consumer<T> target : array)
        {
            addTarget(target);
        }
    }

    public FanOutActor<T> addTarget(Consumer<T> target)
    {
        this.targets.add(Objects.requireNonNull(target, "target must not be null"));
        return this;
    }

    public boolean removeTarget(Consumer<T> target)
    {
        return this.targets.remove(target);
    }

    public List<Consumer<T>> getTargets()
    {
        return Collections.unmodifiableList(targets);
    }

    private void broadcast(T m)
    {
        for (Consumer<T> target : targets)
        {
            target.accept(m);
        }
    }

    @Override
    public void accept(T message)
    {
        inner.accept(message);
    }

    // -----------------------------------------------------------------
    // Linkable
    // -----------------------------------------------------------------

    @Override
    public FanOutActor<T> waitForIdle()
    {
        for (Consumer<T> target : targets)
        {
            if (target instanceof Linkable)
            {
                ((Linkable) target).waitForIdle();
            }
        }
        inner.waitForIdle();
        return this;
    }

    @Override
    public FanOutActor<T> shutdown()
    {
        inner.shutdown();
        return this;
    }

    @Override
    public FanOutActor<T> shutdown(boolean onlyWhenEmpty)
    {
        inner.shutdown(onlyWhenEmpty);
        return this;
    }

    @Override
    public boolean isShutdown()
    {
        return inner.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        return inner.isTerminated();
    }

    @Override
    public boolean isIdle()
    {
        return inner.isIdle();
    }

    @Override
    public ActorLifecycle awaitTermination()
    {
        inner.awaitTermination();
        return this;
    }

    @Override
    public void close()
    {
        inner.close();
    }

    public boolean awaitTermination(int millis)
    {
        return inner.awaitTermination(millis);
    }

    @Override
    public boolean awaitTerminationUntilNanos(long untilNanos)
    {
        return inner.awaitTerminationUntilNanos(untilNanos);
    }

    public Exception getException()
    {
        return inner.getException();
    }

    public FanOutActor<T> dryLogger()
    {
        inner.dryLogger();
        return this;
    }

    public int getPendingCount()
    {
        return inner.getPendingCount();
    }

    public ActorHub getActorHub()
    {
        return inner.getActorHub();
    }

    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        // No defensive copy: {@code targets} is already a CopyOnWriteArrayList,
        // so concurrent readers always observe a consistent snapshot without a
        // per-call allocation. Wrapping it in an unmodifiable view (as
        // {@link #getTargets()} does) blocks mutation by callers at zero copy
        // cost.
        return Collections.<Consumer<?>>unmodifiableList(targets);
    }
}