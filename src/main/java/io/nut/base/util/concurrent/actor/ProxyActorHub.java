/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An {@link ActorHub} that transparently delegates every operation to a real
 * ActorHub assigned with {@link #setActorHub}. Before the first assignment —
 * and after {@code setActorHub(null)} — it behaves like a plain synchronous
 * ActorHub, while Actors created in that window are kept in
 * {@link #pendingActors} and re-registered into the assigned hub when one is
 * set.
 *
 * @author franci
 */
public class ProxyActorHub extends ActorHub implements AutoCloseable
{
    /**
     * Synchronous hub used as the delegation target while no real ActorHub has
     * been assigned ({@link #setActorHub}). It reproduces the behaviour the
     * inherited synchronous pool of this instance would have had in the
     * unassigned case. {@link #actorHub} starts here; {@code actorHub == fallback}
     * therefore means "no real hub assigned yet".
     */
    private final ActorHub fallback = new ActorHub(0, 0, 0, false, false);

    /** The hub every operation is delegated to; never {@code null}. */
    private volatile ActorHub actorHub = fallback;

    private final List<Actor<?>> pendingActors = new CopyOnWriteArrayList<>();

    public ProxyActorHub()
    {
        super(0, 0, 0, false, false);
    }

    /**
     * Assigns the ActorHub this proxy delegates to from now on. Any Actor this
     * proxy created before the assignment (kept in {@link #pendingActors}) is
     * re-registered into {@code actorHub}. Passing {@code null} unassigns and
     * reverts to the synchronous fallback.
     *
     * @param actorHub the hub to delegate to, or {@code null} to unassign
     */
    public void setActorHub(ActorHub actorHub)
    {
        this.actorHub = actorHub != null ? actorHub : fallback;
        if (actorHub != null)
        {
            for (Actor<?> actor : pendingActors)
            {
                actorHub.registerActor(actor);
            }
            pendingActors.clear();
        }
    }

    // Execution and management operations: forward unconditionally.

    @Override
    public void execute(Runnable task)
    {
        actorHub.execute(task);
    }

    @Override
    protected void terminated()
    {
        actorHub.terminated();
    }

    @Override
    public ActorHub shutdown()
    {
        actorHub.shutdown();
        return this;
    }

    @Override
    public boolean isShutdown()
    {
        return actorHub.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        return actorHub.isTerminated();
    }

    @Override
    public boolean awaitTermination(int millis)
    {
        return actorHub.awaitTermination(millis);
    }

    @Override
    public int getCorePoolSize()
    {
        return actorHub.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize()
    {
        return actorHub.getMaximumPoolSize();
    }

    @Override
    public int getCoreThreads()
    {
        return actorHub.getCoreThreads();
    }

    @Override
    public int getMaxThreads()
    {
        return actorHub.getMaxThreads();
    }

    @Override
    public void setThreads(int coreThreads, int maxThreads)
    {
        actorHub.setThreads(coreThreads, maxThreads);
    }

    @Override
    public void close()
    {
        actorHub.close();
    }

    @Override
    public ProxyActorHub spawn(Runnable task)
    {
        actorHub.spawn(task);
        return this;
    }

    @Override
    public Future<Void> submit(Runnable task)
    {
        return actorHub.submit(task);
    }

    @Override
    public <U> Future<U> submit(Supplier<U> supplier)
    {
        return actorHub.submit(supplier);
    }

    @Override
    public <T> void forEach(Iterable<T> iterable, Consumer<? super T> consumer)
    {
        actorHub.forEach(iterable, consumer);
    }

    @Override
    public ActorHub shutdown(boolean onlyWhenEmpty)
    {
        actorHub.shutdown(onlyWhenEmpty);
        return this;
    }

    @Override
    public void close(boolean onlyWhenEmpty)
    {
        actorHub.close(onlyWhenEmpty);
    }

    @Override
    public boolean isIdle()
    {
        return actorHub.isIdle();
    }

    @Override
    public ActorHub waitForIdle()
    {
        actorHub.waitForIdle();
        return this;
    }

    @Override
    public ActorHub awaitTermination()
    {
        actorHub.awaitTermination();
        return this;
    }

    @Override
    public boolean isSynchronous()
    {
        return actorHub.isSynchronous();
    }

    @Override
    public int getActiveCount()
    {
        return actorHub.getActiveCount();
    }

    // Actor creation: bind the new Actor to the assigned hub, or to this proxy
    // while unassigned so that it stays pending and migrates on setActorHub.
    // The explicit fork is required: a shared helper cannot dispatch to the
    // inherited implementation, and binding to the fallback would detach the
    // new Actors from this proxy's pending-tracking.

    @Override
    public <T, R> PipeActor<T, R> pipe(int threads, int queueSize, Function<T, R> function)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.pipe(threads, queueSize, function);
        }
        return super.pipe(threads, queueSize, function);
    }

    @Override
    public <T> Actor<T> actor(int threads, int queueSize, Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.actor(threads, queueSize, consumer);
        }
        return super.actor(threads, queueSize, consumer);
    }

    @Override
    public <E> Actor<E> queue(int threads, int queueSize, BlockingQueue<E> queue)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.queue(threads, queueSize, queue);
        }
        return super.queue(threads, queueSize, queue);
    }

    @Override
    public <E> Actor<E> list(int threads, int queueSize, List<E> list)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.list(threads, queueSize, list);
        }
        return super.list(threads, queueSize, list);
    }

    @Override
    public <T> Actor<T> set(int threads, int queueSize, Set<T> set)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.set(threads, queueSize, set);
        }
        return super.set(threads, queueSize, set);
    }

    @Override
    public <T> FilterActor<T> filter(int threads, int queueSize, Predicate<T> predicate)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.filter(threads, queueSize, predicate);
        }
        return super.filter(threads, queueSize, predicate);
    }

    @Override
    public <T> BatchActor<T> batch(int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.batch(threads, queueSize, maxSize, maxWaitMillis);
        }
        return super.batch(threads, queueSize, maxSize, maxWaitMillis);
    }

    @Override
    public <T, R> PipelineActor<T, R> pipeline(int threads, int queueSize, Function<T, R> first)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.pipeline(threads, queueSize, first);
        }
        return super.pipeline(threads, queueSize, first);
    }

    @Override
    public <T> Subscription<T> sub(String topic, Actor<T> actor)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.sub(topic, actor);
        }
        return super.sub(topic, actor);
    }

    @Override
    public <T> Subscription<T> sub(String tag, Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.sub(tag, consumer);
        }
        return super.sub(tag, consumer);
    }

    @Override
    public <T> Publisher<T> pub(String topic)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.pub(topic);
        }
        return super.pub(topic);
    }

    @Override
    public <T, R> PipeActor<T, R> pipe(Function<T, R> function)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.pipe(function);
        }
        return super.pipe(function);
    }

    @Override
    public <T, R> PipeActor<T, R> pipe(int threads, Function<T, R> function)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.pipe(threads, function);
        }
        return super.pipe(threads, function);
    }

    @Override
    public <T> Actor<T> actor(Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.actor(consumer);
        }
        return super.actor(consumer);
    }

    @Override
    public <T> Actor<T> actor(int threads, Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.actor(threads, consumer);
        }
        return super.actor(threads, consumer);
    }

    @Override
    public <T> Actor<T> actor(Consumer<T> consumer, Runnable onTerminate, Consumer<Exception> onException)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.actor(consumer, onTerminate, onException);
        }
        return super.actor(consumer, onTerminate, onException);
    }

    @Override
    public <T> Actor<T> actor(int threads, Consumer<T> consumer, Runnable onTerminate, Consumer<Exception> onException)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.actor(threads, consumer, onTerminate, onException);
        }
        return super.actor(threads, consumer, onTerminate, onException);
    }

    @Override
    public <T> Actor<T> actor(int threads, int queueSize, Consumer<T> consumer, Runnable onTerminate, Consumer<Exception> onException)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.actor(threads, queueSize, consumer, onTerminate, onException);
        }
        return super.actor(threads, queueSize, consumer, onTerminate, onException);
    }

    @Override
    public <E> Actor<E> queue(BlockingQueue<E> queue)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.queue(queue);
        }
        return super.queue(queue);
    }

    @Override
    public <E> Actor<E> list(List<E> list)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.list(list);
        }
        return super.list(list);
    }

    @Override
    public <E> Actor<E> list(int threads, List<E> list)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.list(threads, list);
        }
        return super.list(threads, list);
    }

    @Override
    public <T> Actor<T> set(Set<T> set)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.set(set);
        }
        return super.set(set);
    }

    @Override
    public <T> Actor<T> set(int threads, Set<T> set)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.set(threads, set);
        }
        return super.set(threads, set);
    }

    @Override
    public <T> FilterActor<T> filter(Predicate<T> predicate)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.filter(predicate);
        }
        return super.filter(predicate);
    }

    @Override
    public <T> FilterActor<T> filter(int threads, Predicate<T> predicate)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.filter(threads, predicate);
        }
        return super.filter(threads, predicate);
    }

    @Override
    public <T, R> PipelineActor<T, R> pipeline(Function<T, R> first)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.pipeline(first);
        }
        return super.pipeline(first);
    }

    @Override
    public <T, R> PipelineActor<T, R> pipeline(int threads, Function<T, R> first)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.pipeline(threads, first);
        }
        return super.pipeline(threads, first);
    }

    @Override
    public <T> BatchActor<T> batch(int maxSize, long maxWaitMillis)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.batch(maxSize, maxWaitMillis);
        }
        return super.batch(maxSize, maxWaitMillis);
    }

    // Registration tracking: the assigned hub keeps its own actors, otherwise
    // they are accumulated here and migrated on setActorHub.

    @Override
    void registerActor(Actor<?> actor)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            h.registerActor(actor);
        }
        else
        {
            pendingActors.add(actor);
        }
    }

    @Override
    void unregisterActor(Actor<?> actor)
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            h.unregisterActor(actor);
        }
        else
        {
            pendingActors.remove(actor);
        }
    }

    @Override
    public List<Actor<?>> actors()
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.actors();
        }
        return pendingActors;
    }

    @Override
    LongAdder processedCount()
    {
        ActorHub h = actorHub;
        if (h != fallback)
        {
            return h.processedCount();
        }
        return super.processedCount();
    }
}
