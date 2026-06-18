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

import io.nut.base.util.concurrent.CallerWaitsPolicy;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Hive implements AutoCloseable, Executor
{

    /**
     * Number of available processor cores in the system
     */
    public static final int CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Default keep-alive time for idle threads (30 seconds)
     */
    public static final int KEEP_ALIVE_MILLIS = 30_000;

    /**
     * Policy that executes tasks in the calling thread when the pool is
     * saturated
     */
    private static final ThreadPoolExecutor.CallerRunsPolicy CALLER_RUNS_POLICY = new ThreadPoolExecutor.CallerRunsPolicy();

    /**
     * Policy that makes the calling thread wait when the pool is saturated
     */
    private static final CallerWaitsPolicy CALLER_WAITS_POLICY = new CallerWaitsPolicy();

    /**
     * The underlying thread pool executor that manages thread execution
     */
    private final ThreadPoolExecutor threadPoolExecutor;

    protected Hive(ThreadPoolExecutor threadPoolExecutor)
    {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public Hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        BlockingQueue<Runnable> queue = queueCapacity == 0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(queueCapacity);
        this.threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, rushPoolSize, keepAliveMillis, TimeUnit.MILLISECONDS, queue, callerWaitsPolicy ? CALLER_WAITS_POLICY : CALLER_RUNS_POLICY);
    }

    public Hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis)
    {
        this(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, false);
    }

    public Hive(int corePoolSize)
    {
        this(corePoolSize, corePoolSize, corePoolSize, KEEP_ALIVE_MILLIS, false);
    }

    public Hive()
    {
        this(CORES, CORES, CORES, KEEP_ALIVE_MILLIS, false);
    }

    /**
     * Convenience static factory for a Hive with default settings,
     * e.g. {@code Hive hive = Hive.hive();}
     */
    public static Hive hive()
    {
        return new Hive();
    }

    public static Hive hive(int corePoolSize)
    {
        return new Hive(corePoolSize);
    }

    public static Hive hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis)
    {
        return new Hive(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis);
    }

    public static Hive hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        return new Hive(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy);
    }

    public Hive add(Bee<?>... bees)
    {
        Objects.requireNonNull(bees, "bees must not be null");
        for (Bee<?> item : bees)
        {
            item.setHive(this);
        }
        return this;
    }

    @Override
    public void execute(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        this.threadPoolExecutor.execute(task);
    }

    /**
     * Creates a new Pipe&lt;T,R&gt; already attached to this Hive, so that
     * sending a message to it is processed asynchronously on this Hive's
     * thread pool. Use {@link PipeBee#linkTo} to chain it to the next stage.
     */
    public <T,R> PipeBee<T,R> pipe(Function<T,R> function)
    {
        return new PipeBee<>(this, function);
    }
    public <T,R> PipeBee<T,R> pipe(int threads, Function<T,R> function)
    {
        return new PipeBee<>(threads, this, function);
    }
    public <T,R> PipeBee<T,R> pipe(int threads, int queueSize, Function<T,R> function)
    {
        return new PipeBee<>(threads, this, queueSize, function);
    }

    /**
     * Creates a new terminal Bee&lt;T&gt; already attached to this Hive,
     * whose receive() simply delegates to the given Consumer. Useful to
     * close a chain of Pipes, e.g. {@code hive.bee(x -> System.out.println(x))}.
     */
    public <T> Bee<T> bee(Consumer<T> consumer)
    {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new Bee<T>(this)
        {
            @Override
            protected void receive(T m)
            {
                consumer.accept(m);
            }
        };
    }
    public <T> Bee<T> bee(int threads, Consumer<T> consumer)
    {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new Bee<T>(threads, this)
        {
            @Override
            protected void receive(T m)
            {
                consumer.accept(m);
            }
        };
    }
    public <T> Bee<T> bee(int threads, int queueSize, Consumer<T> consumer)
    {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new Bee<T>(threads, this, queueSize)
        {
            @Override
            protected void receive(T m)
            {
                consumer.accept(m);
            }
        };
    }

    /**
     * Creates a new QueueBee&lt;E&gt; already attached to this Hive, whose
     * receive() puts every message into the given BlockingQueue. Useful to
     * close a chain of Pipes into a queue that another thread can drain
     * with take()/poll(), e.g. {@code hive.queue(new LinkedBlockingQueue<>())}.
     */
    public <E> QueueBee<E> queue(BlockingQueue<E> queue)
    {
        Objects.requireNonNull(queue, "queue must not be null");
        return new QueueBee<>(this, queue);
    }
    public <E> QueueBee<E> queue(int threads, BlockingQueue<E> queue)
    {
        Objects.requireNonNull(queue, "queue must not be null");
        return new QueueBee<>(threads, this, queue);
    }
    public <E> QueueBee<E> queue(int threads, int queueSize, BlockingQueue<E> queue)
    {
        Objects.requireNonNull(queue, "queue must not be null");
        return new QueueBee<>(threads, this, queueSize, queue);
    }

    /**
     * Creates a new ListBee&lt;E&gt; already attached to this Hive, whose
     * receive() appends every message to the given List, e.g.
     * {@code hive.list(Collections.synchronizedList(new ArrayList<>()))}.
     */
    public <E> ListBee<E> list(List<E> list)
    {
        Objects.requireNonNull(list, "list must not be null");
        return new ListBee<>(this, list);
    }
    public <E> ListBee<E> list(int threads, List<E> list)
    {
        Objects.requireNonNull(list, "list must not be null");
        return new ListBee<>(threads, this, list);
    }
    public <E> ListBee<E> list(int threads, int queueSize, List<E> list)
    {
        Objects.requireNonNull(list, "list must not be null");
        return new ListBee<>(threads, this, queueSize, list);
    }

    /**
     * Creates a new SetBee&lt;T&gt; already attached to this Hive, whose
     * receive() adds every message to the given Set, e.g.
     * {@code hive.set(Collections.newSetFromMap(new ConcurrentHashMap<>()))}.
     */
    public <T> SetBee<T> set(Set<T> set)
    {
        Objects.requireNonNull(set, "set must not be null");
        return new SetBee<>(this, set);
    }
    public <T> SetBee<T> set(int threads, Set<T> set)
    {
        Objects.requireNonNull(set, "set must not be null");
        return new SetBee<>(threads, this, set);
    }
    public <T> SetBee<T> set(int threads, int queueSize, Set<T> set)
    {
        Objects.requireNonNull(set, "set must not be null");
        return new SetBee<>(threads, this, queueSize, set);
    }

    /**
     * Creates a new FilterBee&lt;T&gt; already attached to this Hive, whose
     * receive() only forwards messages that satisfy the given Predicate.
     * Use {@link FilterBee#linkTo} to chain it to the next stage.
     */
    public <T> FilterBee<T> filter(Predicate<T> predicate)
    {
        return new FilterBee<>(this, predicate);
    }
    public <T> FilterBee<T> filter(int threads, Predicate<T> predicate)
    {
        return new FilterBee<>(threads, this, predicate);
    }
    public <T> FilterBee<T> filter(int threads, int queueSize, Predicate<T> predicate)
    {
        return new FilterBee<>(threads, this, queueSize, predicate);
    }

    /**
     * Creates a new BroadcastBee&lt;T&gt; already attached to this Hive,
     * whose receive() fans out every message to all the given targets.
     * More targets can be added later with {@link BroadcastBee#addTarget}.
     */
    @SafeVarargs
    public final <T> BroadcastBee<T> broadcast(Sendable<T>... targets)
    {
        return new BroadcastBee<>(this, targets);
    }
    @SafeVarargs
    public final <T> BroadcastBee<T> broadcast(int threads, Sendable<T>... targets)
    {
        return new BroadcastBee<>(threads, this, targets);
    }
    @SafeVarargs
    public final <T> BroadcastBee<T> broadcast(int threads, int queueSize, Sendable<T>... targets)
    {
        return new BroadcastBee<>(threads, this, queueSize, targets);
    }

    /**
     * Creates a new BatchBee&lt;T&gt; already attached to this Hive, whose
     * receive() accumulates messages and forwards them, as a List&lt;T&gt;,
     * once {@code maxSize} elements are reached or {@code maxWaitMillis}
     * have elapsed since the last flush (pass 0 to disable the time-based
     * flush). Use {@link BatchBee#linkTo} to chain it to the next stage.
     */
    public <T> BatchBee<T> batch(int maxSize, long maxWaitMillis)
    {
        return new BatchBee<>(this, maxSize, maxWaitMillis);
    }
    public <T> BatchBee<T> batch(int threads, int maxSize, long maxWaitMillis)
    {
        return new BatchBee<>(threads, this, maxSize, maxWaitMillis);
    }
    public <T> BatchBee<T> batch(int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        return new BatchBee<>(threads, this, queueSize, maxSize, maxWaitMillis);
    }

    /**
     * Starts a type-safe, fluent {@link HivePipeline} attached to this
     * Hive, e.g. {@code hive.pipeline(f1).then(f2).then(f3).sink(consumer)},
     * which auto-chains the intermediate {@code PipeBee} stages so they
     * don't need to be linked by hand with {@link PipeBee#linkTo}.
     */
    public <T,R> HivePipeline<T,R> pipeline(Function<T,R> first)
    {
        PipeBee<T,R> firstStage = this.pipe(first);
        return new HivePipeline<>(this, firstStage, firstStage);
    }
    public <T,R> HivePipeline<T,R> pipeline(int threads, Function<T,R> first)
    {
        PipeBee<T,R> firstStage = this.pipe(threads, first);
        return new HivePipeline<>(this, firstStage, firstStage);
    }
    public <T,R> HivePipeline<T,R> pipeline(int threads, int queueSize, Function<T,R> first)
    {
        PipeBee<T,R> firstStage = this.pipe(threads, queueSize, first);
        return new HivePipeline<>(this, firstStage, firstStage);
    }

    protected void terminated()
    {
    }

    public Hive shutdown()
    {
        this.threadPoolExecutor.shutdown();
        return this;
    }

    public boolean isShutdown()
    {
        return threadPoolExecutor.isShutdown();
    }

    public boolean isTerminated()
    {
        return threadPoolExecutor.isTerminated();
    }

    public boolean awaitTermination(int millis) throws InterruptedException
    {
        return threadPoolExecutor.awaitTermination(millis, TimeUnit.MILLISECONDS);
    }
    
    public int getCorePoolSize()
    {
        return threadPoolExecutor.getCorePoolSize();
    }

    public int getMaximumPoolSize()
    {
        return threadPoolExecutor.getMaximumPoolSize();
    }

    public void setCorePoolSize(int cps)
    {
        threadPoolExecutor.setCorePoolSize(cps);
    }

    public void setMaximumPoolSize(int mps)
    {
        threadPoolExecutor.setMaximumPoolSize(mps);
    }

    @Override
    public void close()
    {
        try
        {
            this.shutdown();
            this.awaitTermination(Integer.MAX_VALUE);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Hive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Future<Void> async(Runnable runnable)
    {
        return CompletableFuture.runAsync(runnable, this.threadPoolExecutor);
    }

    public <U> Future<U> async(Supplier<U> supplier)
    {
        return CompletableFuture.supplyAsync(supplier, this.threadPoolExecutor);
    }

    public Future<Void> lazy(Runnable runnable)
    {
        return new FutureTask<Void>(runnable, null)
        {
            @Override
            public Void get() throws InterruptedException, ExecutionException
            {
                run();
                return super.get();
            }
            @Override
            public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
                run(); // Run the process only when the result is requested
                return super.get(timeout, unit);
            }
        };
    }

    public <U> Future<U> lazy(Supplier<U> supplier)
    {
        // We created a FutureTask that involves the supplier.
        // We use supplier::get to adapt it to the Callable interface that FutureTask requires.
        return new FutureTask<U>(supplier::get)
        {
            @Override
            public U get() throws InterruptedException, ExecutionException
            {
                run(); //Run the process only when the result is requested
                return super.get();
            }

            @Override
            public U get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
                run(); // Run the process only when the result is requested
                return super.get(timeout, unit);
            }
        };
    }

    // STATIC UTILITY METHODS
    
    public static void shutdown(Sendable<?> stage, boolean cascading, boolean onlyWhenEmpty)
    {
        if (stage instanceof PipeBee)
        {
            PipeBee<?,?> pipe = (PipeBee<?,?>) stage;
            pipe.shutdown(onlyWhenEmpty);
            shutdown(pipe.getNext(), cascading, onlyWhenEmpty);
        }
        else if (stage instanceof FilterBee)
        {
            FilterBee<?> filter = (FilterBee<?>) stage;
            filter.shutdown(onlyWhenEmpty);
            shutdown(filter.getNext(), cascading, onlyWhenEmpty);
        }
        else if (stage instanceof BatchBee)
        {
            BatchBee<?> batch = (BatchBee<?>) stage;
            batch.shutdown(onlyWhenEmpty);
            shutdown(batch.getNext(), cascading, onlyWhenEmpty);
        }
        else if (stage instanceof BroadcastBee)
        {
            BroadcastBee<?> bc = (BroadcastBee<?>) stage;
            bc.shutdown(onlyWhenEmpty);
            for (Sendable<?> target : bc.targets)
            {
                shutdown(target, cascading, onlyWhenEmpty);
            }
        }
        else if (stage instanceof Bee)
        {
            // Plain Bee or other Bee subclass: shut it down, but don't traverse further
            ((Bee<?>)stage).shutdown(onlyWhenEmpty);
        }
        // If it's a Sendable but not a Bee, we can't shut it down, so we stop here
    }

    private static void awaitTerminationUntilNanos(Sendable<?> stage, boolean cascading, long nanos) throws InterruptedException
    {
        if (stage instanceof PipeBee)
        {
            PipeBee<?,?> pipe = (PipeBee<?,?>) stage;
            pipe.awaitTerminationUntilNanos(nanos);
            awaitTerminationUntilNanos(pipe.getNext(), cascading, nanos);
        }
        else if (stage instanceof FilterBee)
        {
            FilterBee<?> filter = (FilterBee<?>) stage;
            filter.awaitTerminationUntilNanos(nanos);
            awaitTerminationUntilNanos(filter.getNext(), cascading, nanos);
        }
        else if (stage instanceof BatchBee)
        {
            BatchBee<?> batch = (BatchBee<?>) stage;
            batch.awaitTerminationUntilNanos(nanos);
            awaitTerminationUntilNanos(batch.getNext(), cascading, nanos);
        }
        else if (stage instanceof BroadcastBee)
        {
            BroadcastBee<?> bc = (BroadcastBee<?>) stage;
            bc.awaitTerminationUntilNanos(nanos);
            for (Sendable<?> target : bc.targets)
            {
                awaitTerminationUntilNanos(target, cascading, nanos);
            }
        }
        else if (stage instanceof Bee)
        {
            // Plain Bee or other Bee subclass: shut it down, but don't traverse further
            ((Bee<?>) stage).awaitTerminationUntilNanos(nanos);
        }
        // If it's a Sendable but not a Bee, we can't shut it down, so we stop here
    }
    
    public static void awaitTermination(Sendable<?> stage, boolean cascading, int millis) throws InterruptedException
    {
        long untilNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        awaitTerminationUntilNanos(stage, cascading, untilNanos);
    }
    
    public static void shutdownAndAwaitTermination(boolean cascading, boolean onlyWhenEmpty, Sendable<?> ...stages)
    {
        Objects.requireNonNull(stages, "stages must not be null");
        for(Sendable<?> item : stages)
        {
            shutdown(item, cascading, onlyWhenEmpty);
        }
        for(Sendable<?> item : stages)
        {
            try
            {
                awaitTermination(item, cascading, Integer.MAX_VALUE);
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(Hive.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}


