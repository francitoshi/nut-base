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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The central coordinator of the Hive concurrency framework.
 * <p>
 * A {@code Hive} wraps a {@link ThreadPoolExecutor} and acts as the shared
 * execution engine for all {@link Bee} stages that are attached to it. Stages
 * are created with the factory methods ({@link #bee}, {@link #pipe},
 * {@link #filter}, {@link #batch}, {@link #queue}, {@link #list},
 * {@link #set}, {@link #broadcast}, {@link #pipeline}) and automatically
 * receive a reference to this Hive, so every {@link Bee#send} call dispatches
 * work to the same underlying thread pool.
 * <p>
 * <strong>Typical usage:</strong>
 * <pre>{@code
 * try (Hive hive = new Hive()) {
 *     Bee<String> printer = hive.bee(System.out::println);
 *     PipeBee<Integer, String> formatter = hive.pipe(i -> "item " + i);
 *     formatter.linkTo(printer);
 *
 *     for (int i = 0; i < 100; i++) formatter.send(i);
 *     Hive.shutdownAndAwaitTermination(true, false, formatter);
 * }
 * }</pre>
 * <p>
 * {@code Hive} implements {@link AutoCloseable}: {@link #close()} calls
 * {@link #shutdown()} and then blocks until the pool terminates. It also
 * implements {@link Executor}, so it can be passed anywhere a plain
 * {@code Executor} is accepted.
 * <p>
 * Static helper methods ({@link #shutdown(Sendable, boolean, boolean)},
 * {@link #awaitTermination(Sendable, boolean, int)},
 * {@link #shutdownAndAwaitTermination(boolean, boolean, Sendable[])}) traverse
 * a chain of linked Bee stages and shut them down collectively, following
 * the links stored by {@link PipeBee}, {@link FilterBee}, {@link BatchBee},
 * and {@link BroadcastBee}.
 */
public class Hive implements AutoCloseable, Executor
{
    /**
     * Number of available processor cores in the system, used as the default
     * pool size when none is specified.
     */
    public static final int CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Default keep-alive time for idle threads, in milliseconds (30 seconds).
     */
    public static final int KEEP_ALIVE_MILLIS = 30_000;

    /**
     * Saturation policy that runs the submitted task in the calling thread when
     * the thread pool and its queue are both full.
     */
    private static final ThreadPoolExecutor.CallerRunsPolicy CALLER_RUNS_POLICY = new ThreadPoolExecutor.CallerRunsPolicy();

    /**
     * Saturation policy that blocks the calling thread until a slot in the
     * thread pool's queue becomes available, providing back-pressure.
     */
    private static final CallerWaitsPolicy CALLER_WAITS_POLICY = new CallerWaitsPolicy();

    /**
     * The underlying thread pool that executes worker tasks submitted by
     * attached {@link Bee} stages.
     */
    private final ThreadPoolExecutor threadPoolExecutor;

    /**
     * Protected constructor used by {@link ProxyHive} and subclasses that
     * supply their own pre-built {@link ThreadPoolExecutor}.
     *
     * @param threadPoolExecutor the executor to delegate to, or {@code null}
     *                           for {@link ProxyHive} which sets it lazily
     */
    protected Hive(ThreadPoolExecutor threadPoolExecutor)
    {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    /**
     * Full constructor.
     *
     * @param corePoolSize      the number of threads kept alive even when idle
     * @param rushPoolSize      the maximum number of threads allowed in the pool
     * @param queueCapacity     the capacity of the task queue; use {@code 0}
     *                          for a {@link SynchronousQueue} (no buffering)
     * @param keepAliveMillis   how long excess idle threads are kept alive
     *                          before being terminated, in milliseconds
     * @param callerWaitsPolicy if {@code true}, a saturated pool blocks the
     *                          caller; if {@code false}, the caller runs the
     *                          task itself
     */
    public Hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        BlockingQueue<Runnable> queue = queueCapacity == 0
                ? new SynchronousQueue<>()
                : new LinkedBlockingQueue<>(queueCapacity);
        this.threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize, rushPoolSize,
                keepAliveMillis, TimeUnit.MILLISECONDS,
                queue,
                callerWaitsPolicy ? CALLER_WAITS_POLICY : CALLER_RUNS_POLICY);
    }

    /**
     * Constructs a Hive with the {@link ThreadPoolExecutor.CallerRunsPolicy}
     * saturation policy.
     *
     * @param corePoolSize    the number of threads kept alive even when idle
     * @param rushPoolSize    the maximum number of threads allowed in the pool
     * @param queueCapacity   the capacity of the task queue (0 = no buffering)
     * @param keepAliveMillis how long excess idle threads survive, in milliseconds
     */
    public Hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis)
    {
        this(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, false);
    }

    /**
     * Constructs a Hive with a symmetric pool of {@code corePoolSize} threads,
     * a bounded queue of the same size, and the default keep-alive time.
     *
     * @param corePoolSize the number of threads and queue slots
     */
    public Hive(int corePoolSize)
    {
        this(corePoolSize, corePoolSize, corePoolSize, KEEP_ALIVE_MILLIS, false);
    }

    /**
     * Constructs a Hive sized to the number of available CPU cores.
     */
    public Hive()
    {
        this(CORES, CORES, CORES, KEEP_ALIVE_MILLIS, false);
    }

    /**
     * Static factory for a Hive with default settings (CPU-core-sized pool).
     * Equivalent to {@code new Hive()}.
     *
     * @return a new default Hive
     */
    public static Hive hive()
    {
        return new Hive();
    }

    /**
     * Static factory for a Hive with a symmetric pool of {@code corePoolSize}
     * threads.
     *
     * @param corePoolSize the number of threads and queue slots
     * @return a new Hive
     */
    public static Hive hive(int corePoolSize)
    {
        return new Hive(corePoolSize);
    }

    /**
     * Static factory with full pool configuration.
     *
     * @param corePoolSize    threads kept alive when idle
     * @param rushPoolSize    maximum threads in the pool
     * @param queueCapacity   task queue capacity (0 = no buffering)
     * @param keepAliveMillis lifetime of excess idle threads, in milliseconds
     * @return a new Hive
     */
    public static Hive hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis)
    {
        return new Hive(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis);
    }

    /**
     * Static factory with full pool configuration and saturation policy choice.
     *
     * @param corePoolSize      threads kept alive when idle
     * @param rushPoolSize      maximum threads in the pool
     * @param queueCapacity     task queue capacity (0 = no buffering)
     * @param keepAliveMillis   lifetime of excess idle threads, in milliseconds
     * @param callerWaitsPolicy {@code true} to block the caller when saturated;
     *                          {@code false} to run the task in the caller
     * @return a new Hive
     */
    public static Hive hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        return new Hive(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy);
    }

    /**
     * Attaches one or more pre-built Bee stages to this Hive, so that messages
     * sent to them are processed on this Hive's thread pool. Useful when a Bee
     * was created without a Hive (e.g. via {@code new BatchBee(...)}) and
     * needs to be wired in later.
     *
     * @param bees the Bee stages to attach; must not be {@code null}
     * @return this Hive, for fluent chaining
     */
    public Hive add(Bee<?>... bees)
    {
        Objects.requireNonNull(bees, "bees must not be null");
        for (Bee<?> item : bees)
        {
            item.setHive(this);
        }
        return this;
    }

    /**
     * Submits a {@link Runnable} to the Hive's thread pool for execution.
     * Implements {@link Executor}.
     *
     * @param task the task to execute; must not be {@code null}
     */
    @Override
    public void execute(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        this.threadPoolExecutor.execute(task);
    }

    /**
     * Creates a new {@link PipeBee}{@code <T,R>} attached to this Hive that
     * applies {@code function} to each received message and forwards the result
     * to the next linked stage. Use {@link PipeBee#linkTo} to wire the output.
     *
     * @param <T>      the input message type
     * @param <R>      the output message type
     * @param function the transformation to apply; must not be {@code null}
     * @return a new PipeBee attached to this Hive
     */
    public <T,R> PipeBee<T,R> pipe(Function<T,R> function)
    {
        return new PipeBee<>(this, function);
    }

    /**
     * Creates a new {@link PipeBee} with the specified thread count.
     *
     * @param <T>      the input message type
     * @param <R>      the output message type
     * @param threads  the maximum number of concurrent worker threads
     * @param function the transformation to apply; must not be {@code null}
     * @return a new PipeBee attached to this Hive
     */
    public <T,R> PipeBee<T,R> pipe(int threads, Function<T,R> function)
    {
        return new PipeBee<>(threads, this, function);
    }

    /**
     * Creates a new {@link PipeBee} with the specified thread count and
     * internal queue size.
     *
     * @param <T>       the input message type
     * @param <R>       the output message type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param function  the transformation to apply; must not be {@code null}
     * @return a new PipeBee attached to this Hive
     */
    public <T,R> PipeBee<T,R> pipe(int threads, int queueSize, Function<T,R> function)
    {
        return new PipeBee<>(threads, this, queueSize, function);
    }

    /**
     * Creates a new terminal {@link Bee}{@code <T>} attached to this Hive whose
     * {@link Bee#receive receive()} method delegates to {@code consumer}. Useful
     * as the last stage of a chain, e.g.:
     * <pre>{@code hive.bee(System.out::println)}</pre>
     *
     * @param <T>      the message type
     * @param consumer the action to perform for each message; must not be
     *                 {@code null}
     * @return a new terminal Bee attached to this Hive
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

    /**
     * Creates a new terminal {@link Bee} with the specified thread count.
     *
     * @param <T>      the message type
     * @param threads  the maximum number of concurrent worker threads
     * @param consumer the action to perform for each message; must not be
     *                 {@code null}
     * @return a new terminal Bee attached to this Hive
     */
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

    /**
     * Creates a new terminal {@link Bee} with the specified thread count and
     * internal queue size.
     *
     * @param <T>       the message type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param consumer  the action to perform for each message; must not be
     *                  {@code null}
     * @return a new terminal Bee attached to this Hive
     */
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
     * Creates a new terminal {@link Bee}{@code <E>} attached to this Hive whose
     * {@link Bee#receive receive()} puts every message into {@code queue}.
     *
     * @param <E>   the element type
     * @param queue the delegate queue; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <E> Bee<E> queue(BlockingQueue<E> queue)
    {
        Objects.requireNonNull(queue, "queue must not be null");
        return new Bee<E>(this)
        {
            @Override
            protected void receive(E m)
            {
                putIntoQueue(queue, m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Bee} with the specified thread count whose
     * {@link Bee#receive receive()} puts every message into {@code queue}.
     *
     * @param <E>     the element type
     * @param threads the maximum number of concurrent worker threads
     * @param queue   the delegate queue; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <E> Bee<E> queue(int threads, BlockingQueue<E> queue)
    {
        Objects.requireNonNull(queue, "queue must not be null");
        return new Bee<E>(threads, this)
        {
            @Override
            protected void receive(E m)
            {
                putIntoQueue(queue, m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Bee} with the specified thread count and
     * internal queue size whose {@link Bee#receive receive()} puts every
     * message into {@code queue}.
     *
     * @param <E>       the element type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param queue     the delegate queue; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <E> Bee<E> queue(int threads, int queueSize, BlockingQueue<E> queue)
    {
        Objects.requireNonNull(queue, "queue must not be null");
        return new Bee<E>(threads, this, queueSize)
        {
            @Override
            protected void receive(E m)
            {
                putIntoQueue(queue, m);
            }
        };
    }

    /**
     * Puts {@code m} into {@code queue}, blocking if necessary. If the
     * calling thread is interrupted while waiting, the interruption is
     * logged and the thread's interrupt status is restored.
     */
    private static <E> void putIntoQueue(BlockingQueue<E> queue, E m)
    {
        try
        {
            queue.put(m);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Hive.class.getName()).log(Level.SEVERE, null, ex);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates a new terminal {@link Bee}{@code <E>} attached to this Hive whose
     * {@link Bee#receive receive()} appends every message to {@code list}.
     * Supply a thread-safe list (e.g.
     * {@code Collections.synchronizedList(new ArrayList<>())}) when the list
     * will be accessed from multiple threads concurrently.
     *
     * @param <E>  the element type
     * @param list the delegate list; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <E> Bee<E> list(List<E> list)
    {
        Objects.requireNonNull(list, "list must not be null");
        return new Bee<E>(this)
        {
            @Override
            protected void receive(E m)
            {
                list.add(m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Bee} with the specified thread count
     * whose {@link Bee#receive receive()} appends every message to
     * {@code list}.
     *
     * @param <E>     the element type
     * @param threads the maximum number of concurrent worker threads
     * @param list    the delegate list; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <E> Bee<E> list(int threads, List<E> list)
    {
        Objects.requireNonNull(list, "list must not be null");
        return new Bee<E>(threads, this)
        {
            @Override
            protected void receive(E m)
            {
                list.add(m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Bee} with the specified thread count and
     * internal queue size whose {@link Bee#receive receive()} appends every
     * message to {@code list}.
     *
     * @param <E>       the element type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param list      the delegate list; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <E> Bee<E> list(int threads, int queueSize, List<E> list)
    {
        Objects.requireNonNull(list, "list must not be null");
        return new Bee<E>(threads, this, queueSize)
        {
            @Override
            protected void receive(E m)
            {
                list.add(m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Bee}{@code <T>} attached to this Hive whose
     * {@link Bee#receive receive()} adds every message to {@code set}.
     * Supply a thread-safe set (e.g.
     * {@code Collections.newSetFromMap(new ConcurrentHashMap<>())}) when the
     * set will be accessed from multiple threads concurrently.
     *
     * @param <T> the element type
     * @param set the delegate set; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <T> Bee<T> set(Set<T> set)
    {
        Objects.requireNonNull(set, "set must not be null");
        return new Bee<T>(this)
        {
            @Override
            protected void receive(T m)
            {
                set.add(m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Bee} with the specified thread count
     * whose {@link Bee#receive receive()} adds every message to {@code set}.
     *
     * @param <T>     the element type
     * @param threads the maximum number of concurrent worker threads
     * @param set     the delegate set; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <T> Bee<T> set(int threads, Set<T> set)
    {
        Objects.requireNonNull(set, "set must not be null");
        return new Bee<T>(threads, this)
        {
            @Override
            protected void receive(T m)
            {
                set.add(m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Bee} with the specified thread count and
     * internal queue size whose {@link Bee#receive receive()} adds every
     * message to {@code set}.
     *
     * @param <T>       the element type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param set       the delegate set; must not be {@code null}
     * @return a new Bee attached to this Hive
     */
    public <T> Bee<T> set(int threads, int queueSize, Set<T> set)
    {
        Objects.requireNonNull(set, "set must not be null");
        return new Bee<T>(threads, this, queueSize)
        {
            @Override
            protected void receive(T m)
            {
                set.add(m);
            }
        };
    }

    /**
     * Creates a new {@link FilterBee}{@code <T>} attached to this Hive that
     * forwards only those messages satisfying {@code predicate} to the next
     * linked stage. Use {@link FilterBee#linkTo} to wire the output.
     *
     * @param <T>       the message type
     * @param predicate the test each message must pass to be forwarded; must
     *                  not be {@code null}
     * @return a new FilterBee attached to this Hive
     */
    public <T> FilterBee<T> filter(Predicate<T> predicate)
    {
        return new FilterBee<>(this, predicate);
    }

    /**
     * Creates a new {@link FilterBee} with the specified thread count.
     *
     * @param <T>       the message type
     * @param threads   the maximum number of concurrent worker threads
     * @param predicate the test each message must pass; must not be {@code null}
     * @return a new FilterBee attached to this Hive
     */
    public <T> FilterBee<T> filter(int threads, Predicate<T> predicate)
    {
        return new FilterBee<>(threads, this, predicate);
    }

    /**
     * Creates a new {@link FilterBee} with the specified thread count and
     * internal queue size.
     *
     * @param <T>       the message type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param predicate the test each message must pass; must not be {@code null}
     * @return a new FilterBee attached to this Hive
     */
    public <T> FilterBee<T> filter(int threads, int queueSize, Predicate<T> predicate)
    {
        return new FilterBee<>(threads, this, queueSize, predicate);
    }

    /**
     * Creates a new {@link BroadcastBee}{@code <T>} attached to this Hive that
     * fans out every received message to all supplied {@code targets}. Additional
     * targets can be added later with {@link BroadcastBee#addTarget}.
     *
     * @param <T>     the message type
     * @param targets zero or more initial downstream stages
     * @return a new BroadcastBee attached to this Hive
     */
    @SafeVarargs
    public final <T> BroadcastBee<T> broadcast(Consumer<T>... targets)
    {
        return new BroadcastBee<>(this, targets);
    }

    /**
     * Creates a new {@link BroadcastBee} with the specified thread count.
     *
     * @param <T>     the message type
     * @param threads the maximum number of concurrent worker threads
     * @param targets zero or more initial downstream stages
     * @return a new BroadcastBee attached to this Hive
     */
    @SafeVarargs
    public final <T> BroadcastBee<T> broadcast(int threads, Consumer<T>... targets)
    {
        return new BroadcastBee<>(threads, this, targets);
    }

    /**
     * Creates a new {@link BroadcastBee} with the specified thread count and
     * internal queue size.
     *
     * @param <T>       the message type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param targets   zero or more initial downstream stages
     * @return a new BroadcastBee attached to this Hive
     */
    @SafeVarargs
    public final <T> BroadcastBee<T> broadcast(int threads, int queueSize, Consumer<T>... targets)
    {
        return new BroadcastBee<>(threads, this, queueSize, targets);
    }

    /**
     * Creates a new {@link BatchBee}{@code <T>} attached to this Hive that
     * accumulates received messages and forwards them as a {@code List<T>} once
     * {@code maxSize} elements are pending or {@code maxWaitMillis} milliseconds
     * have elapsed (pass {@code 0} to disable the time-based flush).
     * Use {@link BatchBee#linkTo} to wire the output.
     *
     * @param <T>          the type of individual messages
     * @param maxSize      the batch size that triggers an immediate flush
     * @param maxWaitMillis the maximum time between flushes, in milliseconds;
     *                     {@code 0} disables periodic flushing
     * @return a new BatchBee attached to this Hive
     */
    public <T> BatchBee<T> batch(int maxSize, long maxWaitMillis)
    {
        return new BatchBee<>(this, maxSize, maxWaitMillis);
    }

    /**
     * Creates a new {@link BatchBee} with the specified thread count.
     *
     * @param <T>           the type of individual messages
     * @param threads       the maximum number of concurrent worker threads
     * @param maxSize       the batch size that triggers an immediate flush
     * @param maxWaitMillis the maximum time between flushes, in milliseconds
     * @return a new BatchBee attached to this Hive
     */
    public <T> BatchBee<T> batch(int threads, int maxSize, long maxWaitMillis)
    {
        return new BatchBee<>(threads, this, maxSize, maxWaitMillis);
    }

    /**
     * Creates a new {@link BatchBee} with the specified thread count and
     * internal queue size.
     *
     * @param <T>           the type of individual messages
     * @param threads       the maximum number of concurrent worker threads
     * @param queueSize     the internal queue capacity
     * @param maxSize       the batch size that triggers an immediate flush
     * @param maxWaitMillis the maximum time between flushes, in milliseconds
     * @return a new BatchBee attached to this Hive
     */
    public <T> BatchBee<T> batch(int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        return new BatchBee<>(threads, this, queueSize, maxSize, maxWaitMillis);
    }

    /**
     * Starts a type-safe, fluent {@link HivePipeline} rooted at this Hive.
     * Additional stages are appended with {@link HivePipeline#then}, and the
     * chain is closed with {@link HivePipeline#sink} or
     * {@link HivePipeline#to}:
     * <pre>{@code
     * Bee<Integer> head = hive.pipeline((Integer i) -> i * 2)
     *                         .then(i -> "item " + i)
     *                         .sink(System.out::println);
     * }</pre>
     *
     * @param <T>   the input type of the first stage
     * @param <R>   the output type of the first stage
     * @param first the function applied by the first stage; must not be
     *              {@code null}
     * @return a new {@link HivePipeline} whose head is the first stage
     */
    public <T,R> HivePipeline<T,R> pipeline(Function<T,R> first)
    {
        PipeBee<T,R> firstStage = this.pipe(first);
        return new HivePipeline<>(this, firstStage, firstStage);
    }

    /**
     * Starts a {@link HivePipeline} with the specified thread count for the
     * first stage.
     *
     * @param <T>     the input type of the first stage
     * @param <R>     the output type of the first stage
     * @param threads the maximum number of concurrent worker threads for the
     *                first stage
     * @param first   the function applied by the first stage
     * @return a new {@link HivePipeline}
     */
    public <T,R> HivePipeline<T,R> pipeline(int threads, Function<T,R> first)
    {
        PipeBee<T,R> firstStage = this.pipe(threads, first);
        return new HivePipeline<>(this, firstStage, firstStage);
    }

    /**
     * Starts a {@link HivePipeline} with the specified thread count and
     * internal queue size for the first stage.
     *
     * @param <T>       the input type of the first stage
     * @param <R>       the output type of the first stage
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity for the first stage
     * @param first     the function applied by the first stage
     * @return a new {@link HivePipeline}
     */
    public <T,R> HivePipeline<T,R> pipeline(int threads, int queueSize, Function<T,R> first)
    {
        PipeBee<T,R> firstStage = this.pipe(threads, queueSize, first);
        return new HivePipeline<>(this, firstStage, firstStage);
    }

    /**
     * Called by the underlying {@link ThreadPoolExecutor} after it has
     * terminated. Subclasses may override to perform post-termination cleanup.
     * The default implementation does nothing.
     */
    protected void terminated()
    {
    }

    /**
     * Initiates a graceful shutdown of the thread pool: previously submitted
     * tasks continue executing, but no new tasks are accepted.
     *
     * @return this Hive, for fluent chaining
     */
    public Hive shutdown()
    {
        this.threadPoolExecutor.shutdown();
        return this;
    }

    /**
     * Returns {@code true} if {@link #shutdown()} has been called on this Hive.
     *
     * @return {@code true} if shutdown has been initiated
     */
    public boolean isShutdown()
    {
        return threadPoolExecutor.isShutdown();
    }

    /**
     * Returns {@code true} if all tasks have completed following a shutdown.
     *
     * @return {@code true} if the thread pool has terminated
     */
    public boolean isTerminated()
    {
        return threadPoolExecutor.isTerminated();
    }

    /**
     * Blocks the calling thread until the thread pool has terminated or the
     * given timeout elapses.
     *
     * @param millis the maximum time to wait, in milliseconds
     * @return {@code true} if the pool terminated within the timeout;
     *         {@code false} otherwise
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting
     */
    public boolean awaitTermination(int millis) throws InterruptedException
    {
        return threadPoolExecutor.awaitTermination(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the core number of threads in the pool.
     *
     * @return the core pool size
     */
    public int getCorePoolSize()
    {
        return threadPoolExecutor.getCorePoolSize();
    }

    /**
     * Returns the maximum allowed number of threads in the pool.
     *
     * @return the maximum pool size
     */
    public int getMaximumPoolSize()
    {
        return threadPoolExecutor.getMaximumPoolSize();
    }

    /**
     * Sets the core number of threads in the pool.
     *
     * @param cps the new core pool size
     */
    public void setCorePoolSize(int cps)
    {
        threadPoolExecutor.setCorePoolSize(cps);
    }

    /**
     * Sets the maximum allowed number of threads in the pool.
     *
     * @param mps the new maximum pool size
     */
    public void setMaximumPoolSize(int mps)
    {
        threadPoolExecutor.setMaximumPoolSize(mps);
    }

    /**
     * Shuts down this Hive and blocks until the thread pool terminates.
     * Implements {@link AutoCloseable} so the Hive can be used in a
     * try-with-resources statement.
     */
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

    /**
     * Submits a {@link Runnable} to this Hive's thread pool for asynchronous
     * execution and returns a {@link Future}{@code <Void>} that completes when
     * the runnable finishes.
     *
     * @param runnable the task to run asynchronously; must not be {@code null}
     * @return a {@code Future<Void>} representing pending completion
     */
    public Future<Void> async(Runnable runnable)
    {
        return CompletableFuture.runAsync(runnable, this.threadPoolExecutor);
    }

    /**
     * Submits a {@link Supplier}{@code <U>} to this Hive's thread pool for
     * asynchronous execution and returns a {@link Future}{@code <U>} that
     * holds the computed result.
     *
     * @param <U>      the result type
     * @param supplier the computation to run asynchronously; must not be
     *                 {@code null}
     * @return a {@code Future<U>} representing pending completion
     */
    public <U> Future<U> async(Supplier<U> supplier)
    {
        return CompletableFuture.supplyAsync(supplier, this.threadPoolExecutor);
    }

    /**
     * Applies {@code consumer} to every element of {@code iterable}, running
     * the invocations on this Hive's thread pool (in parallel, subject to the
     * pool's available threads) and blocking until all of them have finished.
     * <p>
     * Elements are submitted to the pool one by one, as {@code iterable}'s own
     * {@link Iterable#forEach forEach} pulls them, so actual concurrency is
     * bounded by the Hive's configured pool size and saturation policy. If
     * any invocation of {@code consumer} throws an exception, this method
     * waits for all other invocations to complete and then re-throws the
     * first failure wrapped in a {@link CompletionException}, with any
     * further failures added to it as
     * {@linkplain Throwable#addSuppressed(Throwable) suppressed exceptions}.
     *
     * @param <T>      the element type
     * @param iterable the elements to process; must not be {@code null}
     * @param consumer the action to perform for each element; must not be
     *                 {@code null}
     * @throws CompletionException if one or more invocations of
     *                             {@code consumer} threw an exception
     */
    public <T> void forEach(Iterable<T> iterable, Consumer<? super T> consumer)
    {
        Objects.requireNonNull(iterable, "iterable must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        iterable.forEach(item -> futures.add(CompletableFuture.runAsync(() -> consumer.accept(item), this.threadPoolExecutor)));

        AtomicReference<Throwable> first = new AtomicReference<>();
        futures.forEach(future ->
        {
            try
            {
                future.join();
            }
            catch (CompletionException | CancellationException ex)
            {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (!first.compareAndSet(null, cause))
                {
                    first.get().addSuppressed(cause);
                }
            }
        });
        if (first.get() != null)
        {
            throw (first.get() instanceof CompletionException) ? (CompletionException) first.get() : new CompletionException(first.get());
        }
    }

    /**
     * Returns a <em>lazy</em> {@link Future}{@code <Void>} that runs
     * {@code runnable} <em>on the calling thread</em> the first time
     * {@link Future#get()} is invoked, rather than submitting it to the pool
     * immediately. Subsequent {@code get()} calls return the same result.
     *
     * @param runnable the task to run lazily; must not be {@code null}
     * @return a lazy {@code Future<Void>}
     */
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

    /**
     * Returns a <em>lazy</em> {@link Future}{@code <U>} that invokes
     * {@code supplier} <em>on the calling thread</em> the first time
     * {@link Future#get()} is invoked, rather than submitting it to the pool
     * immediately. Subsequent {@code get()} calls return the cached result.
     *
     * @param <U>      the result type
     * @param supplier the computation to run lazily; must not be {@code null}
     * @return a lazy {@code Future<U>}
     */
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

    // -------------------------------------------------------------------------
    // Pub/Sub registry
    // -------------------------------------------------------------------------

    /**
     * Topic → ordered list of subscribers. The list is created on first access
     * and is protected by its own intrinsic lock (see {@link Pub#accept}).
     */
    private final ConcurrentHashMap<String, List<Consumer<?>>> pubSubRegistry = new ConcurrentHashMap<>();

    /**
     * Registers {@code bee} as a subscriber for {@code topic}.
     * <p>
     * After this call, every message published via the {@link Pub} returned by
     * {@link #pub(String)} for the same topic will be delivered to {@code bee}
     * through {@link Co#accept(Object)}. Subscribers are notified in
     * registration order. Registering the same Bee instance more than once for
     * the same topic will result in duplicate deliveries.
     *
     * @param <T>   the message type
     * @param topic the topic name; must not be {@code null}
     * @param bee   the subscriber; must not be {@code null}
     * @return return the same Bee passed as parameter
     */
    public <T> Bee<T> sub(String topic, Bee<T> bee)
    {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(bee,   "bee must not be null");
        List<Consumer<?>> list = pubSubRegistry.computeIfAbsent(topic, k -> new ArrayList<>());
        synchronized (list)
        {
            list.add(bee);
        }
        return bee;
    }
    
    public <T> Bee<T> sub(String topic, int threads, Consumer<T> consumer)
    {
        return sub(topic, bee(threads, consumer));
    }
    
    public <T> Bee<T> sub(String topic, Consumer<T> consumer)
    {
        return sub(topic, bee(consumer));
    }

    /**
     * Returns a {@link Pub}{@code <T>} that publishes messages to all
     * {@link Bee} instances currently (and future) registered for {@code topic}.
     * <p>
     * The returned {@code Pub} holds a live reference to the subscriber list, so
     * Bees subscribed after this call will automatically receive subsequent
     * publishes. Multiple calls with the same topic return publishers backed by
     * the same list.
     *
     * @param <T>   the message type
     * @param topic the topic name; must not be {@code null}
     * @return a publisher for {@code topic}
     */
    @SuppressWarnings("unchecked")
    public <T> Pub<T> pub(String topic)
    {
        Objects.requireNonNull(topic, "topic must not be null");
        List<Consumer<?>> list = pubSubRegistry.computeIfAbsent(topic, k -> new ArrayList<>());
        return new Pub<>((List<Consumer<T>>) (List<?>) list);
    }

    // -------------------------------------------------------------------------
    // Static utility methods for chain-level lifecycle management
    // -------------------------------------------------------------------------

    /**
     * Traverses the chain rooted at {@code stage} and calls
     * {@link Bee#shutdown(boolean)} on each {@link Bee} stage encountered.
     * The traversal follows the {@code next} link of {@link PipeBee},
     * {@link FilterBee}, and {@link BatchBee} stages, and recursively visits
     * every target of a {@link BroadcastBee}. Stages that are {@link Consumer}
     * but not {@link Bee} instances are silently skipped (no shutdown is
     * possible for them).
     *
     * @param stage          the first stage to shut down; may be {@code null}
     *                       (in which case this method is a no-op)
     * @param cascading      reserved for future use; has no effect in the
     *                       current implementation (traversal is always performed)
     * @param onlyWhenEmpty  passed to each {@link Bee#shutdown(boolean)} call;
     *                       {@code true} defers shutdown until the stage's queue
     *                       is empty
     */
    public static void shutdown(Consumer<?> stage, boolean cascading, boolean onlyWhenEmpty)
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
            for (Consumer<?> target : bc.targets)
            {
                shutdown(target, cascading, onlyWhenEmpty);
            }
        }
        else if (stage instanceof Bee)
        {
            // Plain Bee or other Bee subclass: shut it down but don't traverse further.
            ((Bee<?>)stage).shutdown(onlyWhenEmpty);
        }
        // If it's a Sendable but not a Bee, we can't shut it down, so we stop here.
    }

    /**
     * Traverses the chain rooted at {@code stage} and calls
     * {@link Bee#awaitTerminationUntilNanos} on each {@link Bee} stage
     * encountered, using an absolute deadline expressed in nanoseconds. The
     * traversal strategy is the same as {@link #shutdown}.
     *
     * @param stage     the first stage to wait on; may be {@code null}
     * @param cascading reserved for future use
     * @param nanos     the absolute deadline as returned by {@link System#nanoTime()}
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting
     */
    private static void awaitTerminationUntilNanos(Consumer<?> stage, boolean cascading, long nanos) throws InterruptedException
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
            for (Consumer<?> target : bc.targets)
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

    /**
     * Traverses the chain rooted at {@code stage} and waits for each
     * {@link Bee} stage to terminate, giving up once {@code millis}
     * milliseconds have elapsed from the moment this method was called
     * (the deadline is shared across all stages).
     *
     * @param stage     the first stage to wait on; may be {@code null}
     * @param cascading reserved for future use
     * @param millis    the total maximum wait time, in milliseconds
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting
     */
    public static void awaitTermination(Consumer<?> stage, boolean cascading, int millis) throws InterruptedException
    {
        long untilNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        awaitTerminationUntilNanos(stage, cascading, untilNanos);
    }

    /**
     * Convenience method that shuts down all given {@code stages} (and their
     * downstream chains) and then blocks until every one of them has
     * terminated. Shutdown is issued to all stages before waiting, so stages
     * can drain concurrently rather than sequentially.
     *
     * @param cascading     reserved for future use; passed to
     *                      {@link #shutdown(Sendable, boolean, boolean)}
     * @param onlyWhenEmpty if {@code true}, each stage defers its own shutdown
     *                      until its queue is empty
     * @param stages        the head stages of the chains to shut down; must not
     *                      be {@code null}
     */
    public static void shutdownAndAwaitTermination(boolean cascading, boolean onlyWhenEmpty, Consumer<?>... stages)
    {
        Objects.requireNonNull(stages, "stages must not be null");
        for(Consumer<?> item : stages)
        {
            shutdown(item, cascading, onlyWhenEmpty);
        }
        for(Consumer<?> item : stages)
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
