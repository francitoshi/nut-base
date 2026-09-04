/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

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
public class ProxyHive extends Hive implements AutoCloseable
{
    private volatile Hive hive;
    private final List<Bee<?>> pendingBees = new CopyOnWriteArrayList<>();

    public ProxyHive()
    {
        super(0, 0, 0, false, false);
    }

    public void setHive(Hive hive)
    {
        this.hive = hive;
        if (hive != null)
        {
            for (Bee<?> bee : pendingBees)
            {
                hive.registerBee(bee);
            }
            pendingBees.clear();
        }
    }

    @Override
    public void execute(Runnable task)
    {
        Hive h = hive;
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
        Hive h = hive;
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
    public Hive shutdown()
    {
        Hive h = hive;
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
        Hive h = hive;
        if (h != null)
        {
            return h.isShutdown();
        }
        return super.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.isTerminated();
        }
        return super.isTerminated();
    }

    @Override
    public boolean awaitTermination(int millis) throws InterruptedException
    {
        Hive h = hive;
        if (h != null)
        {
            return h.awaitTermination(millis);
        }
        return super.awaitTermination(millis);
    }

    @Override
    public int getCorePoolSize()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.getCorePoolSize();
        }
        return super.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.getMaximumPoolSize();
        }
        return super.getMaximumPoolSize();
    }

    @Override
    public void setPoolSize(int i)
    {
        Hive h = hive;
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
        Hive h = hive;
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
    public <T, R> PipeBee<T, R> pipe(int threads, int queueSize, Function<T, R> function)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.pipe(threads, queueSize, function);
        }
        return super.pipe(threads, queueSize, function);
    }

    @Override
    public <T> Bee<T> bee(int threads, int queueSize, Consumer<T> consumer)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.bee(threads, queueSize, consumer);
        }
        return super.bee(threads, queueSize, consumer);
    }

    @Override
    public <E> Bee<E> queue(int threads, int queueSize, BlockingQueue<E> queue)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.queue(threads, queueSize, queue);
        }
        return super.queue(threads, queueSize, queue);
    }

    @Override
    public <E> Bee<E> list(int threads, int queueSize, List<E> list)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.list(threads, queueSize, list);
        }
        return super.list(threads, queueSize, list);
    }

    @Override
    public <T> Bee<T> set(int threads, int queueSize, Set<T> set)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.set(threads, queueSize, set);
        }
        return super.set(threads, queueSize, set);
    }

    @Override
    public <T> FilterBee<T> filter(int threads, int queueSize, Predicate<T> predicate)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.filter(threads, queueSize, predicate);
        }
        return super.filter(threads, queueSize, predicate);
    }

    @Override
    public <T> BatchBee<T> batch(int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.batch(threads, queueSize, maxSize, maxWaitMillis);
        }
        return super.batch(threads, queueSize, maxSize, maxWaitMillis);
    }

    @Override
    public <T, R> HivePipeline<T, R> pipeline(int threads, int queueSize, Function<T, R> first)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.pipeline(threads, queueSize, first);
        }
        return super.pipeline(threads, queueSize, first);
    }

    @Override
    public <T> Bee<T> sub(String topic, Bee<T> bee)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.sub(topic, bee);
        }
        return super.sub(topic, bee);
    }

    @Override
    public <T> Pub<T> pub(String topic)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.pub(topic);
        }
        return super.pub(topic);
    }

    @Override
    public ProxyHive spawn(Runnable task)
    {
        Hive h = hive;
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
        Hive h = hive;
        if (h != null)
        {
            return h.submit(task);
        }
        return super.submit(task);
    }

    @Override
    public <U> Future<U> submit(Supplier<U> supplier)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.submit(supplier);
        }
        return super.submit(supplier);
    }

    @Override
    public <T> void forEach(Iterable<T> iterable, Consumer<? super T> consumer)
    {
        Hive h = hive;
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
    void registerBee(Bee<?> bee)
    {
        Hive h = hive;
        if (h != null)
        {
            h.registerBee(bee);
        }
        else
        {
            pendingBees.add(bee);
        }
    }

    @Override
    void unregisterBee(Bee<?> bee)
    {
        Hive h = hive;
        if (h != null)
        {
            h.unregisterBee(bee);
        }
        else
        {
            pendingBees.remove(bee);
        }
    }

    @Override
    public List<Bee<?>> bees()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.bees();
        }
        return pendingBees;
    }

    @Override
    public <T, R> PipeBee<T, R> pipe(Function<T, R> function)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.pipe(function);
        }
        return super.pipe(function);
    }

    @Override
    public <T, R> PipeBee<T, R> pipe(int threads, Function<T, R> function)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.pipe(threads, function);
        }
        return super.pipe(threads, function);
    }

    @Override
    public <T> Bee<T> bee(Consumer<T> consumer)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.bee(consumer);
        }
        return super.bee(consumer);
    }

    @Override
    public <T> Bee<T> bee(int threads, Consumer<T> consumer)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.bee(threads, consumer);
        }
        return super.bee(threads, consumer);
    }

    @Override
    public <E> Bee<E> queue(BlockingQueue<E> queue)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.queue(queue);
        }
        return super.queue(queue);
    }

    @Override
    public <E> Bee<E> list(List<E> list)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.list(list);
        }
        return super.list(list);
    }

    @Override
    public <E> Bee<E> list(int threads, List<E> list)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.list(threads, list);
        }
        return super.list(threads, list);
    }

    @Override
    public <T> Bee<T> set(Set<T> set)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.set(set);
        }
        return super.set(set);
    }

    @Override
    public <T> Bee<T> set(int threads, Set<T> set)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.set(threads, set);
        }
        return super.set(threads, set);
    }

    @Override
    public <T> FilterBee<T> filter(Predicate<T> predicate)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.filter(predicate);
        }
        return super.filter(predicate);
    }

    @Override
    public <T> FilterBee<T> filter(int threads, Predicate<T> predicate)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.filter(threads, predicate);
        }
        return super.filter(threads, predicate);
    }

    @Override
    public <T, R> HivePipeline<T, R> pipeline(Function<T, R> first)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.pipeline(first);
        }
        return super.pipeline(first);
    }

    @Override
    public <T, R> HivePipeline<T, R> pipeline(int threads, Function<T, R> first)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.pipeline(threads, first);
        }
        return super.pipeline(threads, first);
    }

    @Override
    public <T> BatchBee<T> batch(int maxSize, long maxWaitMillis)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.batch(maxSize, maxWaitMillis);
        }
        return super.batch(maxSize, maxWaitMillis);
    }

    @Override
    public <T> Bee<T> sub(String topic, int threads, Consumer<T> consumer)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.sub(topic, threads, consumer);
        }
        return super.sub(topic, threads, consumer);
    }

    @Override
    public <T> Bee<T> sub(String topic, Consumer<T> consumer)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.sub(topic, consumer);
        }
        return super.sub(topic, consumer);
    }

    @Override
    public Hive shutdown(boolean onlyWhenEmpty)
    {
        Hive h = hive;
        if (h != null)
        {
            return h.shutdown(onlyWhenEmpty);
        }
        return super.shutdown(onlyWhenEmpty);
    }

    @Override
    public void close(boolean onlyWhenEmpty)
    {
        Hive h = hive;
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
        Hive h = hive;
        if (h != null)
        {
            return h.processedCount();
        }
        return super.processedCount();
    }

    @Override
    public boolean isIdle()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.isIdle();
        }
        return super.isIdle();
    }

    @Override
    public Hive waitForIdle()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.waitForIdle();
        }
        return super.waitForIdle();
    }

    @Override
    public Hive awaitTermination()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.awaitTermination();
        }
        return super.awaitTermination();
    }

    @Override
    public boolean isSynchronous()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.isSynchronous();
        }
        return super.isSynchronous();
    }

    @Override
    public int getActiveCount()
    {
        Hive h = hive;
        if (h != null)
        {
            return h.getActiveCount();
        }
        return super.getActiveCount();
    }

}
