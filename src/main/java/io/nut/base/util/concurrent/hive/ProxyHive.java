/*
 * Copyright (c) 2024-2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
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
    public Hive add(Bee<?>... bees)
    {
        return hive.add(bees);
    }

    @Override
    public <T, R> PipeBee<T, R> pipe(Function<T, R> function)
    {
        return hive.pipe(function);
    }

    @Override
    public <T, R> PipeBee<T, R> pipe(int threads, Function<T, R> function)
    {
        return hive.pipe(threads, function);
    }

    @Override
    public <T, R> PipeBee<T, R> pipe(int threads, int queueSize, Function<T, R> function)
    {
        return hive.pipe(threads, queueSize, function);
    }

    @Override
    public <T> Bee<T> bee(Consumer<T> consumer)
    {
        return hive.bee(consumer);
    }

    @Override
    public <T> Bee<T> bee(int threads, Consumer<T> consumer)
    {
        return hive.bee(threads, consumer);
    }

    @Override
    public <T> Bee<T> bee(int threads, int queueSize, Consumer<T> consumer)
    {
        return hive.bee(threads, queueSize, consumer);
    }

    @Override
    public <E> QueueBee<E> queue(BlockingQueue<E> queue)
    {
        return hive.queue(queue);
    }

    @Override
    public <E> QueueBee<E> queue(int threads, BlockingQueue<E> queue)
    {
        return hive.queue(threads, queue);
    }

    @Override
    public <E> QueueBee<E> queue(int threads, int queueSize, BlockingQueue<E> queue)
    {
        return hive.queue(threads, queueSize, queue);
    }

    @Override
    public <E> ListBee<E> list(List<E> list)
    {
        return hive.list(list);
    }

    @Override
    public <E> ListBee<E> list(int threads, List<E> list)
    {
        return hive.list(threads, list);
    }

    @Override
    public <E> ListBee<E> list(int threads, int queueSize, List<E> list)
    {
        return hive.list(threads, queueSize, list);
    }

    @Override
    public <T> SetBee<T> set(Set<T> set)
    {
        return hive.set(set);
    }

    @Override
    public <T> SetBee<T> set(int threads, Set<T> set)
    {
        return hive.set(threads, set);
    }

    @Override
    public <T> SetBee<T> set(int threads, int queueSize, Set<T> set)
    {
        return hive.set(threads, queueSize, set);
    }

    @Override
    public <T> FilterBee<T> filter(Predicate<T> predicate)
    {
        return hive.filter(predicate);
    }

    @Override
    public <T> FilterBee<T> filter(int threads, Predicate<T> predicate)
    {
        return hive.filter(threads, predicate);
    }

    @Override
    public <T> FilterBee<T> filter(int threads, int queueSize, Predicate<T> predicate)
    {
        return hive.filter(threads, queueSize, predicate);
    }

    @Override
    public <T> BatchBee<T> batch(int maxSize, long maxWaitMillis)
    {
        return hive.batch(maxSize, maxWaitMillis);
    }

    @Override
    public <T> BatchBee<T> batch(int threads, int maxSize, long maxWaitMillis)
    {
        return hive.batch(threads, maxSize, maxWaitMillis);
    }

    @Override
    public <T> BatchBee<T> batch(int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        return hive.batch(threads, queueSize, maxSize, maxWaitMillis);
    }

    @Override
    public <T, R> HivePipeline<T, R> pipeline(Function<T, R> first)
    {
        return hive.pipeline(first);
    }

    @Override
    public <T, R> HivePipeline<T, R> pipeline(int threads, Function<T, R> first)
    {
        return hive.pipeline(threads, first);
    }

    @Override
    public <T, R> HivePipeline<T, R> pipeline(int threads, int queueSize, Function<T, R> first)
    {
        return hive.pipeline(threads, queueSize, first);
    }

    @Override
    public Future<Void> async(Runnable runnable)
    {
        return hive.async(runnable);
    }

    @Override
    public <U> Future<U> async(Supplier<U> supplier)
    {
        return hive.async(supplier);
    }

    @Override
    public Future<Void> lazy(Runnable runnable)
    {
        return hive.lazy(runnable);
    }

    @Override
    public <U> Future<U> lazy(Supplier<U> supplier)
    {
        return hive.lazy(supplier);
    }

}
