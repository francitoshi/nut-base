/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 *
 * @author franci
 */
public class ProxyActorHub extends ActorHub implements AutoCloseable
{
    private volatile ActorHub actorHub;
    private final List<Actor<?>> pendingActors = new CopyOnWriteArrayList<>();

    public ProxyActorHub()
    {
        super(0, 0, 0, false, false);
    }

    public void setActorHub(ActorHub actorHub)
    {
        this.actorHub = actorHub;
        if (actorHub != null)
        {
            for (Actor<?> actor : pendingActors)
            {
                actorHub.registerActor(actor);
            }
            pendingActors.clear();
        }
    }

    @Override
    public void execute(Runnable task)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            h.execute(task);
        }
        else
        {
            super.execute(task);
        }
    }

    @Override
    protected void terminated()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            h.terminated();
        }
        else
        {
            super.terminated();
        }
    }

    @Override
    public ActorHub shutdown()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            h.shutdown();
        }
        else
        {
            super.shutdown();
        }
        return this;
    }

    @Override
    public boolean isShutdown()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.isShutdown();
        }
        return super.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.isTerminated();
        }
        return super.isTerminated();
    }

    @Override
    public boolean awaitTermination(int millis) throws InterruptedException
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.awaitTermination(millis);
        }
        return super.awaitTermination(millis);
    }

    @Override
    public int getCorePoolSize()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.getCorePoolSize();
        }
        return super.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.getMaximumPoolSize();
        }
        return super.getMaximumPoolSize();
    }

    @Override
    public void setPoolSize(int i)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            h.setPoolSize(i);
        }
        else
        {
            super.setPoolSize(i);
        }
    }

    @Override
    public void close()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            h.close();
        }
        else
        {
            super.close();
        }
    }

    @Override
    public <T, R> PipeActor<T, R> pipe(int threads, int queueSize, Function<T, R> function)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.pipe(threads, queueSize, function);
        }
        return super.pipe(threads, queueSize, function);
    }

    @Override
    public <T> Actor<T> actor(int threads, int queueSize, Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.actor(threads, queueSize, consumer);
        }
        return super.actor(threads, queueSize, consumer);
    }

    @Override
    public <E> Actor<E> queue(int threads, int queueSize, BlockingQueue<E> queue)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.queue(threads, queueSize, queue);
        }
        return super.queue(threads, queueSize, queue);
    }

    @Override
    public <E> Actor<E> list(int threads, int queueSize, List<E> list)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.list(threads, queueSize, list);
        }
        return super.list(threads, queueSize, list);
    }

    @Override
    public <T> Actor<T> set(int threads, int queueSize, Set<T> set)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.set(threads, queueSize, set);
        }
        return super.set(threads, queueSize, set);
    }

    @Override
    public <T> FilterActor<T> filter(int threads, int queueSize, Predicate<T> predicate)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.filter(threads, queueSize, predicate);
        }
        return super.filter(threads, queueSize, predicate);
    }

    @Override
    public <T> BatchActor<T> batch(int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.batch(threads, queueSize, maxSize, maxWaitMillis);
        }
        return super.batch(threads, queueSize, maxSize, maxWaitMillis);
    }

    @Override
    public <T, R> PipelineActor<T, R> pipeline(int threads, int queueSize, Function<T, R> first)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.pipeline(threads, queueSize, first);
        }
        return super.pipeline(threads, queueSize, first);
    }

    @Override
    public <T> Actor<T> sub(String topic, Actor<T> actor)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.sub(topic, actor);
        }
        return super.sub(topic, actor);
    }

    @Override
    public <T> ActorPub<T> pub(String topic)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.pub(topic);
        }
        return super.pub(topic);
    }

    @Override
    public ProxyActorHub spawn(Runnable task)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            h.spawn(task);
        }
        else
        {
            super.spawn(task);
        }
        return this;
    }

    @Override
    public Future<Void> submit(Runnable task)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.submit(task);
        }
        return super.submit(task);
    }

    @Override
    public <U> Future<U> submit(Supplier<U> supplier)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.submit(supplier);
        }
        return super.submit(supplier);
    }

    @Override
    public <T> void forEach(Iterable<T> iterable, Consumer<? super T> consumer)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            h.forEach(iterable, consumer);
        }
        else
        {
            super.forEach(iterable, consumer);
        }
    }

    @Override
    void registerActor(Actor<?> actor)
    {
        ActorHub h = actorHub;
        if (h != null)
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
        if (h != null)
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
        if (h != null)
        {
            return h.actors();
        }
        return pendingActors;
    }

    @Override
    public <T, R> PipeActor<T, R> pipe(Function<T, R> function)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.pipe(function);
        }
        return super.pipe(function);
    }

    @Override
    public <T, R> PipeActor<T, R> pipe(int threads, Function<T, R> function)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.pipe(threads, function);
        }
        return super.pipe(threads, function);
    }

    @Override
    public <T> Actor<T> actor(Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.actor(consumer);
        }
        return super.actor(consumer);
    }

    @Override
    public <T> Actor<T> actor(int threads, Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.actor(threads, consumer);
        }
        return super.actor(threads, consumer);
    }

    @Override
    public <E> Actor<E> queue(BlockingQueue<E> queue)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.queue(queue);
        }
        return super.queue(queue);
    }

    @Override
    public <E> Actor<E> list(List<E> list)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.list(list);
        }
        return super.list(list);
    }

    @Override
    public <E> Actor<E> list(int threads, List<E> list)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.list(threads, list);
        }
        return super.list(threads, list);
    }

    @Override
    public <T> Actor<T> set(Set<T> set)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.set(set);
        }
        return super.set(set);
    }

    @Override
    public <T> Actor<T> set(int threads, Set<T> set)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.set(threads, set);
        }
        return super.set(threads, set);
    }

    @Override
    public <T> FilterActor<T> filter(Predicate<T> predicate)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.filter(predicate);
        }
        return super.filter(predicate);
    }

    @Override
    public <T> FilterActor<T> filter(int threads, Predicate<T> predicate)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.filter(threads, predicate);
        }
        return super.filter(threads, predicate);
    }

    @Override
    public <T, R> PipelineActor<T, R> pipeline(Function<T, R> first)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.pipeline(first);
        }
        return super.pipeline(first);
    }

    @Override
    public <T, R> PipelineActor<T, R> pipeline(int threads, Function<T, R> first)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.pipeline(threads, first);
        }
        return super.pipeline(threads, first);
    }

    @Override
    public <T> BatchActor<T> batch(int maxSize, long maxWaitMillis)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.batch(maxSize, maxWaitMillis);
        }
        return super.batch(maxSize, maxWaitMillis);
    }

    @Override
    public <T> Actor<T> sub(String topic, int threads, Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.sub(topic, threads, consumer);
        }
        return super.sub(topic, threads, consumer);
    }

    @Override
    public <T> Actor<T> sub(String topic, Consumer<T> consumer)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.sub(topic, consumer);
        }
        return super.sub(topic, consumer);
    }

    @Override
    public ActorHub shutdown(boolean onlyWhenEmpty)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.shutdown(onlyWhenEmpty);
        }
        return super.shutdown(onlyWhenEmpty);
    }

    @Override
    public void close(boolean onlyWhenEmpty)
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            h.close(onlyWhenEmpty);
        }
        else
        {
            super.close(onlyWhenEmpty);
        }
    }

    @Override
    AtomicInteger processedCount()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.processedCount();
        }
        return super.processedCount();
    }

    @Override
    public boolean isIdle()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.isIdle();
        }
        return super.isIdle();
    }

    @Override
    public ActorHub waitForIdle()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.waitForIdle();
        }
        return super.waitForIdle();
    }

    @Override
    public ActorHub awaitTermination()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.awaitTermination();
        }
        return super.awaitTermination();
    }

    @Override
    public boolean isSynchronous()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.isSynchronous();
        }
        return super.isSynchronous();
    }

    @Override
    public int getActiveCount()
    {
        ActorHub h = actorHub;
        if (h != null)
        {
            return h.getActiveCount();
        }
        return super.getActiveCount();
    }

}
