/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
public class Hive extends Queen implements AutoCloseable, Executor
{

    /**
     * Protected constructor used by {@link ProxyHive} and subclasses that
     * supply their own pre-built {@link ThreadPoolExecutor}.
     *
     * @param threadPoolExecutor the executor to delegate to, or {@code null}
     *                           for {@link ProxyHive} which sets it lazily
     */
    protected Hive(ThreadPoolExecutor threadPoolExecutor)
    {
        super(threadPoolExecutor);
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
        super(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy);
    }

    /**
     * Full constructor with explicit active-task tracking control.
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
     * @param avoidTracker      if {@code true}, active-task tracking via the
     *                          internal {@link java.util.concurrent.Phaser} is
     *                          disabled, which reduces overhead but makes
     *                          {@link Queen#waitForIdle()} a no-op
     */
    public Hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy, boolean avoidTracker)
    {
        super(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy, avoidTracker);
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
        super(corePoolSize);
    }

    /**
     * Constructs a Hive sized to the number of available CPU cores.
     */
    public Hive()
    {
        super();
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

    @Override
    public Hive spawn(Runnable task)
    {
        super.spawn(task);
        return this;
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
        return pipe(0, 0, function);
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
        return pipe(threads, 0, function);
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
        return bee(0, 0, consumer);
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
        return bee(threads, 0, consumer);
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
        return queue(0, 0, queue);
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
        return queue(threads, 0, queue);
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
        return list(0, 0, list);
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
        return list(threads, 0, list);
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
        return set(0, 0, set);
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
        return set(threads, 0, set);
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
        return filter(0, 0, predicate);
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
        return filter(threads, 0, predicate);
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
        return broadcast(0, 0, targets);
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
        return broadcast(threads, 0, targets);
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
        return pipeline(0, 0, first);
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
        return pipeline(threads, 0, first);
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

    // -------------------------------------------------------------------------
    // BatchBee factory methods
    // -------------------------------------------------------------------------

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
        return batch(0, 0, maxSize, maxWaitMillis);
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
        return batch(threads, 0, maxSize, maxWaitMillis);
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
     * {@inheritDoc}
     *
     * @return this Hive, for fluent chaining
     */
    public Hive shutdown()
    {
        return (Hive) super.shutdown();
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
     * through {@link Bee#accept(Object)}. Subscribers are notified in
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
    
    /**
     * Creates a new {@link Bee} with the given thread count, subscribes it to
     * {@code topic}, and returns it. Convenience shorthand for
     * {@code sub(topic, bee(threads, consumer))}.
     *
     * @param <T>      the message type
     * @param topic    the topic name; must not be {@code null}
     * @param threads  the maximum number of concurrent worker threads for the
     *                 new Bee
     * @param consumer the action performed for each message; must not be
     *                 {@code null}
     * @return the newly created and subscribed Bee
     */
    public <T> Bee<T> sub(String topic, int threads, Consumer<T> consumer)
    {
        return sub(topic, bee(threads, consumer));
    }
    
    /**
     * Creates a new {@link Bee}, subscribes it to {@code topic}, and returns
     * it. Convenience shorthand for {@code sub(topic, bee(consumer))}.
     *
     * @param <T>      the message type
     * @param topic    the topic name; must not be {@code null}
     * @param consumer the action performed for each message; must not be
     *                 {@code null}
     * @return the newly created and subscribed Bee
     */
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

    private static void waitForIdle(Set<Consumer<?>> set, Consumer<?> stage)
    {
        if(set.contains(stage))
        {
            return;
        }
        
        set.add(stage);
        
        if (stage instanceof Bee)
        {
            Bee<?> bee = (Bee<?>) stage;
            bee.waitForIdle();
            for (Consumer<?> target : bee.getLinkedTargets())
            {
                waitForIdle(set, target);
            }
        }
    }
    
    /**
     * Blocks the calling thread until every stage in the given chain is idle
     * and its internal queue is empty. Stages linked via {@link PipeBee},
     * {@link FilterBee}, {@link BatchBee}, and {@link BroadcastBee} are
     * traversed automatically; cycles are handled safely.
     *
     * @param stages the root stage(s) of the chain(s) to wait on; must not be
     *               {@code null}
     */
    public static void waitForIdle(Consumer<?>... stages)
    {
        Set<Consumer<?>> set = new HashSet();
        for(Consumer<?> s : stages)
        {
            waitForIdle(set, s);
        }
    }
    
    private static void shutdown(Set<Consumer<?>> set, boolean onlyWhenEmpty, Consumer<?> stage)
    {
        if(set.contains(stage))
        {
            return;
        }
        
        set.add(stage);
        
        if (stage instanceof Bee)
        {
            Bee<?> bee = (Bee<?>) stage;
            bee.shutdown(onlyWhenEmpty);
            for (Consumer<?> target : bee.getLinkedTargets())
            {
                shutdown(set, onlyWhenEmpty, target);
            }
        }
    }

    /**
     * Initiates a graceful shutdown on every stage in the given chain(s).
     * Stages linked via {@link PipeBee}, {@link FilterBee}, {@link BatchBee},
     * and {@link BroadcastBee} are traversed automatically; cycles are handled
     * safely.
     *
     * @param onlyWhenEmpty if {@code true}, each stage defers its shutdown until
     *                      its queue is empty (see {@link Bee#shutdown(boolean)});
     *                      if {@code false}, shutdown starts immediately
     * @param stages        the root stage(s) of the chain(s) to shut down; must
     *                      not be {@code null}
     */
    public static void shutdown(boolean onlyWhenEmpty, Consumer<?>... stages)
    {
        Set<Consumer<?>> set = new HashSet();
        for(Consumer<?> s : stages)
        {
            shutdown(set, onlyWhenEmpty, s);
        }
    }
    
    private static void awaitTerminationUntilNanos(Set<Consumer<?>> set, Consumer<?> stage, long nanos) throws InterruptedException
    {
        if(set.contains(stage))
        {
            return;
        }
        
        set.add(stage);
        
        if (stage instanceof Bee)
        {
            Bee<?> bee = (Bee<?>) stage;
            bee.awaitTerminationUntilNanos(nanos);
            for (Consumer<?> target : bee.getLinkedTargets())
            {
                awaitTerminationUntilNanos(set, target, nanos);
            }
        }
    }

    /**
     * Blocks the calling thread until every stage in the given chain(s) has
     * terminated or the timeout elapses, whichever comes first. Stages linked
     * via {@link PipeBee}, {@link FilterBee}, {@link BatchBee}, and
     * {@link BroadcastBee} are traversed automatically; cycles are handled
     * safely.
     *
     * @param millis the maximum time to wait in total, in milliseconds
     * @param stages the root stage(s) of the chain(s) to wait on; must not be
     *               {@code null}
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting
     */
    public static void awaitTermination(int millis, Consumer<?>... stages) throws InterruptedException
    {
        long untilNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        Set<Consumer<?>> set = new HashSet();
        for(Consumer<?> s : stages)
        {
            awaitTerminationUntilNanos(set, s, untilNanos);
        }
    }

    /**
     * Shuts down every stage in the given chain(s) and blocks until all of
     * them have terminated. This is a convenience combination of
     * {@link #shutdown(boolean, Consumer[])} followed by
     * {@link #awaitTermination(int, Consumer[])} with an effectively infinite
     * timeout. Stages linked via {@link PipeBee}, {@link FilterBee},
     * {@link BatchBee}, and {@link BroadcastBee} are traversed automatically;
     * cycles are handled safely.
     * <p>
     * If the calling thread is interrupted while waiting, the interruption is
     * logged and the method returns without re-interrupting the thread.
     *
     * @param onlyWhenEmpty if {@code true}, each stage defers its shutdown until
     *                      its queue is empty; if {@code false}, shutdown starts
     *                      immediately
     * @param stages        the root stage(s) of the chain(s) to shut down; must
     *                      not be {@code null}
     */
    public static void shutdownAndAwaitTermination(boolean onlyWhenEmpty, Consumer<?>... stages)
    {
        Objects.requireNonNull(stages, "stages must not be null");

        shutdown(onlyWhenEmpty, stages);

        try
        {
            awaitTermination(Integer.MAX_VALUE, stages);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Hive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
