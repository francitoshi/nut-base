/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
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

    public ProxyHive()
    {
        super(null);
    }

    public void setHive(Hive hive)
    {
        this.hive = hive;
    }

    @Override
    public void execute(Runnable task)
    {
        hive.execute(task);
    }

    @Override
    protected void terminated()
    {
        hive.terminated();
    }

    @Override
    public Hive shutdown()
    {
        hive.shutdown();
        return this;
    }

    @Override
    public boolean isShutdown()
    {
        return hive.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        return hive.isTerminated();
    }

    @Override
    public boolean awaitTermination(int millis) throws InterruptedException
    {
        return hive.awaitTermination(millis);
    }

    @Override
    public int getCorePoolSize()
    {
        return hive.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize()
    {
        return hive.getMaximumPoolSize();
    }

    @Override
    public void setCorePoolSize(int i)
    {
        hive.setCorePoolSize(i);
    }

    @Override
    public void setMaximumPoolSize(int i)
    {
        hive.setMaximumPoolSize(i);
    }

    @Override
    public void close()
    {
        hive.close();
    }


    @Override
    public <T, R> PipeBee<T, R> pipe(int threads, int queueSize, Function<T, R> function)
    {
        return hive.pipe(threads, queueSize, function);
    }

    @Override
    public <T> Bee<T> bee(int threads, int queueSize, Consumer<T> consumer)
    {
        return hive.bee(threads, queueSize, consumer);
    }

    @Override
    public <E> Bee<E> queue(int threads, int queueSize, BlockingQueue<E> queue)
    {
        return hive.queue(threads, queueSize, queue);
    }

    @Override
    public <E> Bee<E> list(int threads, int queueSize, List<E> list)
    {
        return hive.list(threads, queueSize, list);
    }

    @Override
    public <T> Bee<T> set(int threads, int queueSize, Set<T> set)
    {
        return hive.set(threads, queueSize, set);
    }

    @Override
    public <T> FilterBee<T> filter(int threads, int queueSize, Predicate<T> predicate)
    {
        return hive.filter(threads, queueSize, predicate);
    }

    @Override
    public <T> BatchBee<T> batch(int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        return hive.batch(threads, queueSize, maxSize, maxWaitMillis);
    }

    @Override
    public <T, R> HivePipeline<T, R> pipeline(int threads, int queueSize, Function<T, R> first)
    {
        return hive.pipeline(threads, queueSize, first);
    }

    @Override
    public <T> Bee<T> sub(String topic, Bee<T> bee)
    {
        return hive.sub(topic, bee);
    }

    @Override
    public <T> Pub<T> pub(String topic)
    {
        return hive.pub(topic);
    }


    @Override
    public ProxyHive spawn(Runnable task)
    {
        hive.spawn(task);
        return this;
    }

    @Override
    public Future<Void> submit(Runnable task)
    {
        return hive.submit(task);
    }

    @Override
    public <U> Future<U> submit(Supplier<U> supplier)
    {
        return hive.submit(supplier);
    }

    @Override
    public <T> void forEach(Iterable<T> iterable, Consumer<? super T> consumer)
    {
        hive.forEach(iterable, consumer);
    }

}
