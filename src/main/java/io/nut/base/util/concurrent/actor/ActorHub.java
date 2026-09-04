/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.math.Nums;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The central coordinator of the ActorHub concurrency framework.
 * <p>
 * A {@code ActorHub} wraps a {@link ThreadPoolExecutor} and acts as the shared
 * execution engine for all {@link Actor} stages that are attached to it. Stages
 * are created with the factory methods ({@link #actor}, {@link #pipe},
 * {@link #filter}, {@link #batch}, {@link #queue}, {@link #list},
 * {@link #set}, {@link #broadcast}, {@link #pipeline}) and automatically
 * receive a reference to this ActorHub, so every {@link Actor#accept(Object)} call
 * dispatches work to the same underlying thread pool.
 * <p>
 * <strong>Typical usage:</strong>
 * <pre>{@code
 * try (ActorHub actorHub = new ActorHub()) {
 *     Actor<String> printer = actorHub.actor(System.out::println);
 *     PipeActor<Integer, String> formatter = actorHub.pipe(i -> "item " + i);
 *     formatter.linkTo(printer);
 *
 *     for (int i = 0; i < 100; i++) formatter.accept(i);
 *     actorHub.close(true);
 * }
 * }</pre>
 * <p>
 * {@code ActorHub} implements {@link AutoCloseable}: {@link #close()} calls
 * {@link #shutdown()} and then blocks until the pool terminates. It also
 * implements {@link Executor}, so it can be passed anywhere a plain
 * {@code Executor} is accepted.
 * <p>
 * As with {@link ActorPool}, the pool is configured with a single
 * {@code corePoolSize} that also acts as the initial maximum, and idle core
 * threads are allowed to time out, so the number of threads scales from
 * {@code 0} up to {@code corePoolSize}. When more (non-synchronous) Actors are
 * registered than {@code corePoolSize}, the pool's core and maximum sizes
 * automatically grow to keep one thread available per Actor.
 * <p>
 * {@link #shutdown(boolean)} and {@link #close(boolean)}
 * shut down every Actor registered with this ActorHub; the static
 * {@link #awaitTermination(int, Consumer[])} follows the links stored by
 * {@link PipeActor}, {@link FilterActor}, {@link BatchActor}, and {@link FanOutActor},
 * blocking until a chain of linked stages has terminated.
 */
public class ActorHub extends ActorPool implements AutoCloseable, Executor
{
    public static ActorHub SYNCHRONOUS = new ActorHub(null);
    
    /** Active non-synchronous Actors attached to this ActorHub, for coordinated tasks. */
    private final List<Actor<?>> actors = new CopyOnWriteArrayList<>();

    /** O(1) count of registered Actors, used to size the pool as Actors come and go. */
    private final AtomicInteger beeCount = new AtomicInteger();

    /** Shared count of messages processed by all Actors attached to this ActorHub. */
    private final AtomicInteger processedCount = new AtomicInteger();

    /** Guards the deferred-shutdown handshake between {@link #shutdown(boolean)}
     *  and {@link #unregisterActor(Actor)}. */
    private final Object shutdownLock = new Object();

    /**
     * {@code true} once {@link #shutdown(boolean)} has been called with
     * {@code onlyWhenEmpty == true} but the pool shutdown is still pending
     * because at least one non-synchronous Actor has yet to drain and terminate.
     */
    private boolean shutdownWhenEmpty;

    /**
     * The core pool size as configured at construction. Both the core and the
     * maximum grow together beyond this value, to one thread per registered
     * non-synchronous Actor (see {@link #adjustPoolToActors}).
     */
    private final int initialCorePoolSize;

    /**
     * Protected constructor used by {@link ProxyActorHub} and subclasses that
     * supply their own pre-built {@link ThreadPoolExecutor}.
     *
     * @param threadPoolExecutor the executor to delegate to, or {@code null}
     *                           for {@link ProxyActorHub} which sets it lazily
     */
    protected ActorHub(ThreadPoolExecutor threadPoolExecutor)
    {
        super(threadPoolExecutor);
        this.initialCorePoolSize = 0;
    }

    /**
     * Full constructor with explicit active-task tracking control.
     *
     * @param corePoolSize      the maximum number of concurrent worker threads;
     *                          {@code 0} selects the synchronous mode (no pool)
     * @param queueCapacity     the capacity of the task queue; use {@code 0}
     *                          for a {@link SynchronousQueue} (no buffering)
     * @param keepAliveMillis   the keep-alive time for idle threads, in
     *                          milliseconds; the pool grows from {@code 0} up
     *                          to {@code corePoolSize} threads (or more, when
     *                          more Actors demand them) as load demands
     * @param callerWaitsPolicy if {@code true}, a saturated pool blocks the
     *                          caller; if {@code false}, the caller runs the
     *                          task itself
     * @param avoidTracker      if {@code true}, active-task tracking via the
     *                          internal {@link java.util.concurrent.Phaser} is
     *                          disabled, which reduces overhead but makes
     *                          {@link ActorPool#waitForIdle()} a no-op
     */
    public ActorHub(int corePoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy, boolean avoidTracker)
    {
        super(corePoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy, avoidTracker);
        this.initialCorePoolSize = corePoolSize;
    }

    /**
     * Full constructor.
     *
     * @param corePoolSize      the maximum number of concurrent worker threads;
     *                          {@code 0} selects the synchronous mode (no pool)
     * @param queueCapacity     the capacity of the task queue; use {@code 0}
     *                          for a {@link SynchronousQueue} (no buffering)
     * @param keepAliveMillis   the keep-alive time for idle threads, in
     *                          milliseconds
     * @param callerWaitsPolicy if {@code true}, a saturated pool blocks the
     *                          caller; if {@code false}, the caller runs the
     *                          task itself
     */
    public ActorHub(int corePoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        this(corePoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy, DEFAULT_AVOID_TRACKER);
    }

    /**
     * Constructs an ActorHub with the {@link ThreadPoolExecutor.CallerRunsPolicy}
     * saturation policy.
     *
     * @param corePoolSize    the maximum number of concurrent worker threads;
     *                        {@code 0} selects the synchronous mode (no pool)
     * @param queueCapacity   the capacity of the task queue (0 = no buffering)
     * @param keepAliveMillis the keep-alive time for idle threads, in milliseconds
     */
    public ActorHub(int corePoolSize, int queueCapacity, int keepAliveMillis)
    {
        this(corePoolSize, queueCapacity, keepAliveMillis, DEFAULT_CALLER_WAITS_POLICY, DEFAULT_AVOID_TRACKER);
    }

    /**
     * Constructs an ActorHub with a symmetric pool of {@code corePoolSize} threads,
     * a task queue of the same capacity, and the default keep-alive time.
     * When the queue is full, saturated tasks run in the caller under the
     * {@link ThreadPoolExecutor.CallerRunsPolicy CallerRunsPolicy}.
     *
     * @param corePoolSize the number of threads and queue slots
     */
    public ActorHub(int corePoolSize)
    {
        this(corePoolSize, corePoolSize, DEFAULT_KEEP_ALIVE_MILLIS, DEFAULT_CALLER_WAITS_POLICY, DEFAULT_AVOID_TRACKER);
    }

    /**
     * Constructs an ActorHub sized to the number of available CPU cores.
     */
    public ActorHub()
    {
        this(CORES, CORES, DEFAULT_KEEP_ALIVE_MILLIS, DEFAULT_CALLER_WAITS_POLICY, DEFAULT_AVOID_TRACKER);
    }

    /**
     * Static factory for an ActorHub with default settings (CPU-core-sized pool).
     * Equivalent to {@code new ActorHub()}.
     *
     * @return a new default ActorHub
     */
    public static ActorHub actorHub()
    {
        return new ActorHub();
    }

    /**
     * Static factory for an ActorHub with a symmetric pool of {@code corePoolSize}
     * threads.
     *
     * @param corePoolSize the number of threads and queue slots
     * @return a new ActorHub
     */
    public static ActorHub actorHub(int corePoolSize)
    {
        return new ActorHub(corePoolSize);
    }

    /**
     * Static factory with full pool configuration.
     *
     * @param corePoolSize    the maximum number of concurrent worker threads
     * @param queueCapacity   task queue capacity (0 = no buffering)
     * @param keepAliveMillis keep-alive time for idle threads, in milliseconds
     * @return a new ActorHub
     */
    public static ActorHub actorHub(int corePoolSize, int queueCapacity, int keepAliveMillis)
    {
        return new ActorHub(corePoolSize, queueCapacity, keepAliveMillis);
    }

    /**
     * Static factory with full pool configuration and saturation policy choice.
     *
     * @param corePoolSize      the maximum number of concurrent worker threads
     * @param queueCapacity     task queue capacity (0 = no buffering)
     * @param keepAliveMillis   keep-alive time for idle threads, in milliseconds
     * @param callerWaitsPolicy {@code true} to block the caller when saturated;
     *                          {@code false} to run the task in the caller
     * @return a new ActorHub
     */
    public static ActorHub actorHub(int corePoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        return new ActorHub(corePoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy);
    }

    @Override
    public ActorHub spawn(Runnable task)
    {
        super.spawn(task);
        return this;
    }   
    
    /**
     * Returns the ActorHub-wide counter of messages processed by its Actors. Actors
     * attached to this ActorHub hold a direct reference to it, so a graceful
     * {@code shutdown(true)} can detect, across {@link #waitForIdle()} passes,
     * whether any Actor is still forwarding messages.
     *
     * @return the shared processed-message counter
     */
    AtomicInteger processedCount()
    {
        return processedCount;
    }

    /**
     * Registers a non-synchronous Actor attached to this ActorHub so its lifecycle
     * can be tracked. Called by {@link Actor} on construction.
     *
     * @param actor the Actor to register
     */
    void registerActor(Actor<?> actor)
    {
        actors.add(actor);
        beeCount.incrementAndGet();
        adjustPoolToActors();
    }

    /**
     * Removes an Actor from this ActorHub's tracking list. Called by {@link Actor} when
     * it terminates (on shutdown).
     *
     * @param actor the Actor to remove
     */
    void unregisterActor(Actor<?> actor)
    {
        if (actors.remove(actor))
        {
            beeCount.decrementAndGet();
            adjustPoolToActors();
            maybeShutdownPool();
        }
    }

    /**
     * Adjusts the pool's core and maximum sizes together to match the number
     * of registered Actors. Both are set to {@code max(initialCorePoolSize,
     * beeCount)}, so the pool stays symmetric (core == maximum, as with
     * {@link ActorPool}) and simply grows one thread per registered Actor beyond
     * the initial core size. Sizing scales down again as Actors terminate.
     * <p>
     * ThreadPoolExecutor requires {@code maximumPoolSize &ge; corePoolSize};
     * since core and maximum are always set to the same value this invariant
     * is trivially satisfied.
     */
    private void adjustPoolToActors()
    {
        if (isSynchronous())
        {
            return;
        }
        int size = Math.max(initialCorePoolSize, beeCount.get());
        setPoolSize(size);
    }

    /**
     * Returns the list of active non-synchronous Actors attached to this ActorHub.
     * Actors are removed from this list once they terminate (on shutdown), and
     * synchronous Actors (constructed with {@code threads == 0}) are never added.
     *
     * @return the list of active Actors attached to this ActorHub
     */
    public List<Actor<?>> actors()
    {
        return actors;
    }

    /**
     * Creates a new {@link PipeActor}{@code <T,R>} attached to this ActorHub that
     * applies {@code function} to each received message and forwards the result
     * to the next linked stage. Use {@link PipeActor#linkTo} to wire the output.
     *
     * @param <T>      the input message type
     * @param <R>      the output message type
     * @param function the transformation to apply; must not be {@code null}
     * @return a new PipeActor attached to this ActorHub
     */
    public <T,R> PipeActor<T,R> pipe(Function<T,R> function)
    {
        return pipe(1, 0, function);
    }

    /**
     * Creates a new {@link PipeActor} with the specified thread count.
     *
     * @param <T>      the input message type
     * @param <R>      the output message type
     * @param threads  the maximum number of concurrent worker threads
     * @param function the transformation to apply; must not be {@code null}
     * @return a new PipeActor attached to this ActorHub
     */
    public <T,R> PipeActor<T,R> pipe(int threads, Function<T,R> function)
    {
        return pipe(threads, 0, function);
    }

    /**
     * Creates a new {@link PipeActor} with the specified thread count and
     * internal queue size.
     *
     * @param <T>       the input message type
     * @param <R>       the output message type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param function  the transformation to apply; must not be {@code null}
     * @return a new PipeActor attached to this ActorHub
     */
    public <T,R> PipeActor<T,R> pipe(int threads, int queueSize, Function<T,R> function)
    {
        return new PipeActor<>(this, threads, queueSize, function);
    }

    /**
     * Creates a new terminal {@link Actor}{@code <T>} attached to this ActorHub whose
     * {@link Actor#receive receive()} method delegates to {@code consumer}. Useful
     * as the last stage of a chain, e.g.:
     * <pre>{@code actorHub.actor(System.out::println)}</pre>
     *
     * @param <T>      the message type
     * @param consumer the action to perform for each message; must not be
     *                 {@code null}
     * @return a new terminal Actor attached to this ActorHub
     */
    public <T> Actor<T> actor(Consumer<T> consumer)
    {
        return actor(1, 0, consumer);
    }

    /**
     * Creates a new terminal {@link Actor} with the specified thread count.
     *
     * @param <T>      the message type
     * @param threads  the maximum number of concurrent worker threads
     * @param consumer the action to perform for each message; must not be
     *                 {@code null}
     * @return a new terminal Actor attached to this ActorHub
     */
    public <T> Actor<T> actor(int threads, Consumer<T> consumer)
    {
        return actor(threads, 0, consumer);
    }

    /**
     * Creates a new terminal {@link Actor} with the specified thread count and
     * internal queue size.
     *
     * @param <T>       the message type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param consumer  the action to perform for each message; must not be
     *                  {@code null}
     * @return a new terminal Actor attached to this ActorHub
     */
    public <T> Actor<T> actor(int threads, int queueSize, Consumer<T> consumer)
    {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new Actor<T>(this, threads, queueSize)
        {
            @Override
            protected void receive(T m)
            {
                consumer.accept(m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Actor}{@code <E>} attached to this ActorHub whose
     * {@link Actor#receive receive()} puts every message into {@code queue}.
     *
     * @param <E>   the element type
     * @param queue the delegate queue; must not be {@code null}
     * @return a new Actor attached to this ActorHub
     */
    public <E> Actor<E> queue(BlockingQueue<E> queue)
    {
        return queue(1, 0, queue);
    }

    /**
     * Creates a new terminal {@link Actor} with the specified thread count and
     * internal queue size whose {@link Actor#receive receive()} puts every
     * message into {@code queue}.
     *
     * @param <E>       the element type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param queue     the delegate queue; must not be {@code null}
     * @return a new Actor attached to this ActorHub
     */
    public <E> Actor<E> queue(int threads, int queueSize, BlockingQueue<E> queue)
    {
        Objects.requireNonNull(queue, "queue must not be null");
        return new Actor<E>(this, threads, queueSize)
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
            Logger.getLogger(ActorHub.class.getName()).log(Level.SEVERE, null, ex);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates a new terminal {@link Actor}{@code <E>} attached to this ActorHub whose
     * {@link Actor#receive receive()} appends every message to {@code list}.
     * Supply a thread-safe list (e.g.
     * {@code Collections.synchronizedList(new ArrayList<>())}) when the list
     * will be accessed from multiple threads concurrently.
     *
     * @param <E>  the element type
     * @param list the delegate list; must not be {@code null}
     * @return a new Actor attached to this ActorHub
     */
    public <E> Actor<E> list(List<E> list)
    {
        return list(1, 0, list);
    }

    /**
     * Creates a new terminal {@link Actor} with the specified thread count
     * whose {@link Actor#receive receive()} appends every message to
     * {@code list}.
     *
     * @param <E>     the element type
     * @param threads the maximum number of concurrent worker threads
     * @param list    the delegate list; must not be {@code null}
     * @return a new Actor attached to this ActorHub
     */
    public <E> Actor<E> list(int threads, List<E> list)
    {
        return list(threads, 0, list);
    }

    /**
     * Creates a new terminal {@link Actor} with the specified thread count and
     * internal queue size whose {@link Actor#receive receive()} appends every
     * message to {@code list}.
     *
     * @param <E>       the element type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param list      the delegate list; must not be {@code null}
     * @return a new Actor attached to this ActorHub
     */
    public <E> Actor<E> list(int threads, int queueSize, List<E> list)
    {
        Objects.requireNonNull(list, "list must not be null");
        return new Actor<E>(this, threads, queueSize)
        {
            @Override
            protected void receive(E m)
            {
                list.add(m);
            }
        };
    }

    /**
     * Creates a new terminal {@link Actor}{@code <T>} attached to this ActorHub whose
     * {@link Actor#receive receive()} adds every message to {@code set}.
     * Supply a thread-safe set (e.g.
     * {@code Collections.newSetFromMap(new ConcurrentHashMap<>())}) when the
     * set will be accessed from multiple threads concurrently.
     *
     * @param <T> the element type
     * @param set the delegate set; must not be {@code null}
     * @return a new Actor attached to this ActorHub
     */
    public <T> Actor<T> set(Set<T> set)
    {
        return set(1, 0, set);
    }

    /**
     * Creates a new terminal {@link Actor} with the specified thread count
     * whose {@link Actor#receive receive()} adds every message to {@code set}.
     *
     * @param <T>     the element type
     * @param threads the maximum number of concurrent worker threads
     * @param set     the delegate set; must not be {@code null}
     * @return a new Actor attached to this ActorHub
     */
    public <T> Actor<T> set(int threads, Set<T> set)
    {
        return set(threads, 0, set);
    }

    /**
     * Creates a new terminal {@link Actor} with the specified thread count and
     * internal queue size whose {@link Actor#receive receive()} adds every
     * message to {@code set}.
     *
     * @param <T>       the element type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param set       the delegate set; must not be {@code null}
     * @return a new Actor attached to this ActorHub
     */
    public <T> Actor<T> set(int threads, int queueSize, Set<T> set)
    {
        Objects.requireNonNull(set, "set must not be null");
        return new Actor<T>(this, threads, queueSize)
        {
            @Override
            protected void receive(T m)
            {
                set.add(m);
            }
        };
    }

    /**
     * Creates a new {@link FilterActor}{@code <T>} attached to this ActorHub that
     * forwards only those messages satisfying {@code predicate} to the next
     * linked stage. Use {@link FilterActor#linkTo} to wire the output.
     *
     * @param <T>       the message type
     * @param predicate the test each message must pass to be forwarded; must
     *                  not be {@code null}
     * @return a new FilterActor attached to this ActorHub
     */
    public <T> FilterActor<T> filter(Predicate<T> predicate)
    {
        return filter(1, 0, predicate);
    }

    /**
     * Creates a new {@link FilterActor} with the specified thread count.
     *
     * @param <T>       the message type
     * @param threads   the maximum number of concurrent worker threads
     * @param predicate the test each message must pass; must not be {@code null}
     * @return a new FilterActor attached to this ActorHub
     */
    public <T> FilterActor<T> filter(int threads, Predicate<T> predicate)
    {
        return filter(threads, 0, predicate);
    }

    /**
     * Creates a new {@link FilterActor} with the specified thread count and
     * internal queue size.
     *
     * @param <T>       the message type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param predicate the test each message must pass; must not be {@code null}
     * @return a new FilterActor attached to this ActorHub
     */
    public <T> FilterActor<T> filter(int threads, int queueSize, Predicate<T> predicate)
    {
        return new FilterActor<>(this, threads, queueSize, predicate);
    }

    /**
     * Creates a new {@link FanOutActor}{@code <T>} attached to this ActorHub that
     * fans out every received message to all supplied {@code targets}. Additional
     * targets can be added later with {@link FanOutActor#addTarget}.
     *
     * @param <T>     the message type
     * @param targets zero or more initial downstream stages
     * @return a new FanOutActor attached to this ActorHub
     */
    @SafeVarargs
    public final <T> FanOutActor<T> broadcast(Consumer<T>... targets)
    {
        return broadcast(1, 0, targets);
    }

    /**
     * Creates a new {@link FanOutActor} with the specified thread count.
     *
     * @param <T>     the message type
     * @param threads the maximum number of concurrent worker threads
     * @param targets zero or more initial downstream stages
     * @return a new FanOutActor attached to this ActorHub
     */
    @SafeVarargs
    public final <T> FanOutActor<T> broadcast(int threads, Consumer<T>... targets)
    {
        return broadcast(threads, 0, targets);
    }

    /**
     * Creates a new {@link FanOutActor} with the specified thread count and
     * internal queue size.
     *
     * @param <T>       the message type
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity
     * @param targets   zero or more initial downstream stages
     * @return a new FanOutActor attached to this ActorHub
     */
    @SafeVarargs
    public final <T> FanOutActor<T> broadcast(int threads, int queueSize, Consumer<T>... targets)
    {
        return new FanOutActor<>(this, threads, queueSize, targets);
    }

    /**
     * Starts a type-safe, fluent {@link PipelineActor} rooted at this ActorHub.
     * Additional stages are appended with {@link PipelineActor#then}, and the
     * chain is closed with {@link PipelineActor#sink} or
     * {@link PipelineActor#to}:
     * <pre>{@code
     * Actor<Integer> head = actorHub.pipeline((Integer i) -> i * 2)
     *                         .then(i -> "item " + i)
     *                         .sink(System.out::println);
     * }</pre>
     *
     * @param <T>   the input type of the first stage
     * @param <R>   the output type of the first stage
     * @param first the function applied by the first stage; must not be
     *              {@code null}
     * @return a new {@link PipelineActor} whose head is the first stage
     */
    public <T,R> PipelineActor<T,R> pipeline(Function<T,R> first)
    {
        return pipeline(1, 0, first);
    }

    /**
     * Starts a {@link PipelineActor} with the specified thread count for the
     * first stage.
     *
     * @param <T>     the input type of the first stage
     * @param <R>     the output type of the first stage
     * @param threads the maximum number of concurrent worker threads for the
     *                first stage
     * @param first   the function applied by the first stage
     * @return a new {@link PipelineActor}
     */
    public <T,R> PipelineActor<T,R> pipeline(int threads, Function<T,R> first)
    {
        return pipeline(threads, 0, first);
    }

    /**
     * Starts a {@link PipelineActor} with the specified thread count and
     * internal queue size for the first stage.
     *
     * @param <T>       the input type of the first stage
     * @param <R>       the output type of the first stage
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity for the first stage
     * @param first     the function applied by the first stage
     * @return a new {@link PipelineActor}
     */
    public <T,R> PipelineActor<T,R> pipeline(int threads, int queueSize, Function<T,R> first)
    {
        PipeActor<T,R> firstStage = this.pipe(threads, queueSize, first);
        return new PipelineActor<>(this, firstStage, firstStage);
    }

    // -------------------------------------------------------------------------
    // BatchActor factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@link BatchActor}{@code <T>} attached to this ActorHub that
     * accumulates received messages and forwards them as a {@code List<T>} once
     * {@code maxSize} elements are pending or {@code maxWaitMillis} milliseconds
     * have elapsed (pass {@code 0} to disable the time-based flush).
     * Use {@link BatchActor#linkTo} to wire the output.
     *
     * @param <T>          the type of individual messages
     * @param maxSize      the batch size that triggers an immediate flush
     * @param maxWaitMillis the maximum time between flushes, in milliseconds;
     *                     {@code 0} disables periodic flushing
     * @return a new BatchActor attached to this ActorHub
     */
    public <T> BatchActor<T> batch(int maxSize, long maxWaitMillis)
    {
        return batch(1, 0, maxSize, maxWaitMillis);
    }

    /**
     * Creates a new {@link BatchActor} with the specified thread count and
     * internal queue size.
     *
     * @param <T>           the type of individual messages
     * @param threads       the maximum number of concurrent worker threads
     * @param queueSize     the internal queue capacity
     * @param maxSize       the batch size that triggers an immediate flush
     * @param maxWaitMillis the maximum time between flushes, in milliseconds
     * @return a new BatchActor attached to this ActorHub
     */
    public <T> BatchActor<T> batch(int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        return new BatchActor<>(this, threads, queueSize, maxSize, maxWaitMillis);
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
     * Registers {@code actor} as a subscriber for {@code topic}.
     * <p>
     * After this call, every message published via the {@link Pub} returned by
     * {@link #pub(String)} for the same topic will be delivered to {@code actor}
     * through {@link Actor#accept(Object)}. Subscribers are notified in
     * registration order. Registering the same Actor instance more than once for
     * the same topic will result in duplicate deliveries.
     *
     * @param <T>   the message type
     * @param topic the topic name; must not be {@code null}
     * @param actor   the subscriber; must not be {@code null}
     * @return return the same Actor passed as parameter
     */
    public <T> Actor<T> sub(String topic, Actor<T> actor)
    {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(actor,   "actor must not be null");
        List<Consumer<?>> list = pubSubRegistry.computeIfAbsent(topic, k -> new ArrayList<>());
        synchronized (list)
        {
            list.add(actor);
        }
        return actor;
    }
    
    /**
     * Creates a new {@link Actor} with the given thread count, subscribes it to
     * {@code topic}, and returns it. Convenience shorthand for
     * {@code sub(topic, actor(threads, consumer))}.
     *
     * @param <T>      the message type
     * @param topic    the topic name; must not be {@code null}
     * @param threads  the maximum number of concurrent worker threads for the
     *                 new Actor
     * @param consumer the action performed for each message; must not be
     *                 {@code null}
     * @return the newly created and subscribed Actor
     */
    public <T> Actor<T> sub(String topic, int threads, Consumer<T> consumer)
    {
        return sub(topic, actor(threads, consumer));
    }
    
    /**
     * Creates a new {@link Actor}, subscribes it to {@code topic}, and returns
     * it. Convenience shorthand for {@code sub(topic, actor(consumer))}.
     *
     * @param <T>      the message type
     * @param topic    the topic name; must not be {@code null}
     * @param consumer the action performed for each message; must not be
     *                 {@code null}
     * @return the newly created and subscribed Actor
     */
    public <T> Actor<T> sub(String topic, Consumer<T> consumer)
    {
        return sub(topic, actor(consumer));
    }

    /**
     * Returns a {@link Pub}{@code <T>} that publishes messages to all
     * {@link Actor} instances currently (and future) registered for {@code topic}.
     * <p>
     * The returned {@code Pub} holds a live reference to the subscriber list, so
     * Actors subscribed after this call will automatically receive subsequent
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
        
        if (stage instanceof Actor)
        {
            Actor<?> actor = (Actor<?>) stage;
            actor.waitForIdle();
            for (Consumer<?> target : actor.getLinkedTargets())
            {
                waitForIdle(set, target);
            }
        }
    }
    
    /**
     * Blocks the calling thread until every stage in the given chain is idle
     * and its internal queue is empty. Stages linked via {@link PipeActor},
     * {@link FilterActor}, {@link BatchActor}, and {@link FanOutActor} are
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

    /**
     * {@inheritDoc}
     * <p>
     * Shuts down every Actor registered with this ActorHub first, then shuts down the
     * underlying thread pool.
     *
     * @return this ActorHub, for fluent chaining
     */
    public ActorHub shutdown()
    {
        return shutdown(false);
    }

    /**
     * Initiates a graceful shutdown on every Actor registered with this ActorHub;
     * synchronous Actors ({@code threads == 0}) are not registered and are
     * unaffected.
     * <p>
     * When {@code onlyWhenEmpty} is {@code true} the ActorHub first drains the
     * whole graph: it waits until every registered Actor is idle, repeating the
     * pass whenever the ActorHub-wide processed-message counter advances (so
     * in-flight forwards between linked stages are not lost), and only then
     * closes every Actor and the underlying thread pool. When {@code false} the
     * shutdown starts immediately.
     *
     * @param onlyWhenEmpty if {@code true}, the ActorHub drains until the whole
     *                      graph is quiescent before shutting down; if
     *                      {@code false}, shutdown starts immediately
     * @return this ActorHub, for fluent chaining
     */
    public ActorHub shutdown(boolean onlyWhenEmpty)
    {
        if (onlyWhenEmpty)
        {
            synchronized (shutdownLock)
            {
                if (shutdownWhenEmpty)
                {
                    return this;
                }
                shutdownWhenEmpty = true;
            }
            // Graceful drain: wait until every Actor is idle, repeating the pass
            // whenever the ActorHub-wide processed-message counter advanced during
            // the wait. A Actor that is still forwarding to later stages raises
            // the counter of a peer Actor, so the repeated pass catches in-flight
            // forwards; only when the counter stops moving is the whole graph
            // quiescent and safe to close.
            long last = processedCount.get();
            while (true)
            {
                for (Actor<?> actor : actors)
                {
                    actor.waitForIdle();
                }
                long now = processedCount.get();
                if (now == last)
                {
                    break;
                }
                last = now;
            }
        }
        for (Actor<?> actor : actors)
        {
            actor.shutdown(false);
        }
        shutdownPoolOrDefer();
        return this;
    }

    /**
     * Shuts down the underlying thread pool unless a graceful
     * ({@code onlyWhenEmpty == true}) shutdown is still pending Actors that have
     * yet to drain. When such Actors are still registered the pool is left open
     * so their workers can complete, and {@link #maybeShutdownPool()} is called
     * again from {@link #unregisterActor(Actor)} once the last one terminates.
     */
    private void shutdownPoolOrDefer()
    {
        synchronized (shutdownLock)
        {
            if (shutdownWhenEmpty && beeCount.get() > 0)
            {
                return;
            }
            shutdownWhenEmpty = false;
            if (!isShutdown())
            {
                super.shutdown();
            }
        }
    }

    /**
     * Shuts down the underlying pool if a graceful shutdown has been requested
     * and this was the last registered Actor to terminate.
     */
    private void maybeShutdownPool()
    {
        synchronized (shutdownLock)
        {
            if (shutdownWhenEmpty && beeCount.get() == 0)
            {
                shutdownWhenEmpty = false;
                if (!isShutdown())
                {
                    super.shutdown();
                }
            }
        }
    }
    
    private static void awaitTerminationUntilNanos(Set<Consumer<?>> set, Consumer<?> stage, long nanos) throws InterruptedException
    {
        if(set.contains(stage))
        {
            return;
        }
        
        set.add(stage);
        
        if (stage instanceof Actor)
        {
            Actor<?> actor = (Actor<?>) stage;
            actor.awaitTerminationUntilNanos(nanos);
            for (Consumer<?> target : actor.getLinkedTargets())
            {
                awaitTerminationUntilNanos(set, target, nanos);
            }
        }
    }

    /**
     * Blocks the calling thread until every stage in the given chain(s) has
     * terminated or the timeout elapses, whichever comes first. Stages linked
     * via {@link PipeActor}, {@link FilterActor}, {@link BatchActor}, and
     * {@link FanOutActor} are traversed automatically; cycles are handled
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
        long untilNanos = Nums.saturatedAdd(System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(millis));
        Set<Consumer<?>> set = new HashSet();
        for(Consumer<?> s : stages)
        {
            awaitTerminationUntilNanos(set, s, untilNanos);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} only while the pool is idle <em>and</em> every Actor
     * registered with this ActorHub is idle (see {@link Actor#isIdle()}).
     */
    @Override
    public boolean isIdle()
    {
        if (!super.isIdle())
        {
            return false;
        }
        for (Actor<?> actor : actors)
        {
            if (!actor.isIdle())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Blocks until the pool is idle <em>and</em> every Actor registered with this
     * ActorHub is idle.
     */
    @Override
    public ActorHub waitForIdle()
    {
        super.waitForIdle();
        for (Actor<?> actor : actors)
        {
            actor.waitForIdle();
        }
        return this;
    }

    /**
     * Blocks until every Actor registered with this ActorHub has terminated (closed,
     * drained, and unregistered), then returns.
     * <p>
     * If the calling thread is interrupted while waiting, the interrupt status
     * is restored and the interruption is logged at {@link Level#SEVERE}; the
     * method then returns without waiting for termination to complete.
     */
    public ActorHub awaitTermination()
    {
        try
        {
            awaitTermination(Integer.MAX_VALUE, actors.toArray(new Consumer<?>[0]));
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            Logger.getLogger(ActorHub.class.getName()).log(Level.SEVERE, null, ex);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Also blocks until every Actor registered with this ActorHub has terminated, and
     * is equivalent to {@link #close(boolean) close(false)}.
     */
    @Override
    public void close()
    {
        close(false);
    }

    /**
     * Shuts down this ActorHub and blocks until every registered Actor has
     * terminated and the pool has shut down.
     *
     * @param onlyWhenEmpty if {@code true}, each Actor defers its shutdown until
     *                      its queue is empty; if {@code false}, shutdown starts
     *                      immediately
     */
    public void close(boolean onlyWhenEmpty)
    {
        try
        {
            shutdown(onlyWhenEmpty);
            awaitTermination(Integer.MAX_VALUE, actors.toArray(new Consumer<?>[0]));
            awaitTermination(Integer.MAX_VALUE);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            Logger.getLogger(ActorHub.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
