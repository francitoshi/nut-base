/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The fundamental building block of the Hive concurrency framework.
 * A {@code Bee<M>} is an asynchronous message-processing stage: it accepts
 * messages via {@link #send(Object)}, queues them internally, and dispatches
 * them to {@link #receive(Object)} on a bounded set of worker threads supplied
 * by an attached {@link Hive}.
 * <p>
 * When no {@link Hive} is attached (the {@code hive} parameter is {@code null}),
 * the {@link #send(Object)} call executes {@link #receive(Object)} synchronously
 * in the calling thread instead, making the Bee usable in simple, non-concurrent
 * contexts without any configuration.
 * <p>
 * <strong>Lifecycle</strong>: a Bee starts in the {@code RUNNING} state and
 * accepts messages freely. Calling {@link #shutdown()} moves it to
 * {@code SHUTDOWN}, where it stops accepting new messages but finishes
 * processing any already queued. Once the queue is drained and all worker
 * threads have returned, {@link #terminate()} is called and the state becomes
 * {@code TERMINATED}. Callers can block on this final state with
 * {@link #awaitTermination(int)}.
 * <p>
 * <strong>Concurrency</strong>: the number of concurrent workers is bounded by
 * a {@link Semaphore} whose initial permit count equals {@code threads} (or the
 * number of available CPU cores when {@code threads == 0}). Each worker drains
 * as many messages from the queue as possible before releasing its permit, so
 * throughput scales naturally without spawning one thread per message.
 * <p>
 * Subclasses must implement {@link #receive(Object)} and may optionally override
 * {@link #terminate()} (called once, after the last message, on shutdown) and
 * {@link #exception(Exception)} (called on every unhandled exception from
 * {@link #receive(Object)} or the lifecycle hooks).
 *
 * @param <M> the type of messages this Bee processes
 */
public abstract class Bee<M> implements Consumer<M>
{
    /** Internal state: accepting and processing messages. */
    private static final int RUNNING    = 0;
    /** Internal state: no longer accepting new messages, but draining the queue. */
    private static final int SHUTDOWN   = 1;
    /** Internal state: queue drained, {@link #terminate()} has completed. */
    private static final int TERMINATED = 2;

    /** Default internal queue capacity when none is specified. */
    private static final int QUEUE_SIZE = Short.MAX_VALUE;

    private final Object lock = new Object();
    private volatile int status = RUNNING;

    private volatile boolean allowLogger = true;
    private volatile boolean shutdownWhenEmpty = false;
    private volatile Executor hive;
    private final int threads;
    private final Semaphore semaphore;
    private final BlockingQueue<M> queue;

    /**
     * The last exception that occurred during message processing or lifecycle
     * operations. May be {@code null} if no exception has been raised yet.
     */
    private volatile Exception ex;


    /**
     * Full constructor.
     *
     * @param threads   the maximum number of concurrent worker threads; if
     *                  {@code 0}, defaults to {@link Runtime#availableProcessors()}
     * @param hive      the {@link Hive} whose thread pool executes the workers,
     *                  or {@code null} to process messages synchronously in the
     *                  calling thread
     * @param queueSize the internal queue capacity; if {@code 0}, uses the
     *                  default capacity ({@value #QUEUE_SIZE})
     * @throws IllegalArgumentException if {@code threads < 0} or
     *                                  {@code queueSize < 0}
     */
    public Bee(int threads, Hive hive, int queueSize)
    {
        if (threads < 0)
        {
            throw new IllegalArgumentException("threads < 0");
        }
        if (queueSize < 0)
        {
            throw new IllegalArgumentException("queueSize < 0");
        }
        this.threads = threads != 0 ? threads : Hive.CORES;
        this.hive = hive;
        this.queue = new LinkedBlockingQueue<>(queueSize != 0 ? queueSize : QUEUE_SIZE);
        this.semaphore = new Semaphore(this.threads);
    }

    /**
     * Constructs a Bee with the given thread count and Hive, using the default
     * queue size.
     *
     * @param threads the maximum number of concurrent worker threads
     * @param hive    the Hive thread pool, or {@code null} for synchronous mode
     */
    public Bee(int threads, Hive hive)
    {
        this(threads, hive, QUEUE_SIZE);
    }

    /**
     * Convenience constructor for a Bee attached to a Hive from the start,
     * using the default thread count and queue size. This is the constructor
     * used by {@link Hive#bee(java.util.function.Consumer)} and
     * {@link PipeBee#PipeBee(Hive, java.util.function.Function)} to create
     * stages that are bound to a Hive at creation time.
     *
     * @param hive the Hive thread pool, or {@code null} for synchronous mode
     */
    public Bee(Hive hive)
    {
        this(0, hive, QUEUE_SIZE);
    }

    /**
     * Constructs a standalone Bee with the given thread count but no Hive.
     * A Hive can be attached later with {@link #setHive(Hive)}.
     *
     * @param threads the maximum number of concurrent worker threads
     */
    public Bee(int threads)
    {
        this(threads, null, QUEUE_SIZE);
    }

    /**
     * Constructs a standalone Bee with default thread count and no Hive.
     * A Hive can be attached later with {@link #setHive(Hive)}.
     */
    public Bee()
    {
        this(0, null, QUEUE_SIZE);
    }

    /**
     * Suppresses the internal {@link Logger} so that exceptions from
     * {@link #receive(Object)} and lifecycle hooks are silently swallowed
     * rather than written to the log. Useful in tests or when the caller
     * handles errors via {@link #getException()}.
     *
     * @return this Bee, for fluent chaining
     */
    public Bee<M> dryLogger()
    {
        this.allowLogger = false;
        return this;
    }

    /**
     * Returns the last exception that was thrown during message processing
     * or a lifecycle hook, or {@code null} if none has occurred yet.
     *
     * @return the last unhandled exception, or {@code null}
     */
    public Exception getException()
    {
        return ex;
    }

    /**
     * Called by the worker thread for each message dequeued from the internal
     * queue. Subclasses must implement the actual message-processing logic here.
     * <p>
     * This method is invoked on the Hive's thread pool (or on the calling
     * thread in synchronous mode). It must not call {@link #send(Object)} on
     * itself, as that would re-enqueue the message.
     *
     * @param m the message to process; never {@code null} under normal operation
     */
    protected abstract void receive(M m);

    /**
     * Called once, after the internal queue is fully drained and all worker
     * threads have completed, as the final step of the shutdown sequence.
     * Subclasses may override this to release resources, flush buffers, etc.
     * The default implementation does nothing.
     */
    protected void terminate()
    {
    }

    /**
     * Called whenever an unhandled exception escapes from {@link #receive(Object)}
     * or from this Bee's lifecycle hooks. The exception is also stored in
     * {@link #getException()} and, unless suppressed by {@link #dryLogger()},
     * logged at {@code SEVERE} level.
     * <p>
     * Subclasses may override this to add custom error-handling logic.
     * The default implementation does nothing.
     *
     * @param ex the exception that was thrown
     */
    protected void exception(Exception ex)
    {
    }

    /**
     * Sends a message to this Bee for processing.
     * <p>
     * If a Hive is attached, the message is placed on the internal queue and a
     * worker task is submitted to the Hive's thread pool (if a permit is
     * available). If no Hive is attached, {@link #receive(Object)} is called
     * directly in the calling thread.
     * <p>
     * Messages sent after {@link #shutdown()} has been called cause an
     * {@link IllegalStateException} which is passed to {@link #exception(Exception)}
     * and, unless suppressed by {@link #dryLogger()}, logged at {@code SEVERE} level.
     *
     * @param message the message to deliver
     * @throws IllegalStateException if this Bee has already been shut down
     */
    @Override
    public void accept(M message)
    {
        try
        {
            if(this.status!=RUNNING)
            {
                throw new IllegalStateException("status!=RUNNING");
                //666 return false;
            }

            if(this.hive!=null)
            {
                this.queue.put(message);
                // Submit task if permits available
                if (this.semaphore.availablePermits() > 0)
                {
                    this.hive.execute(receiveTask);
                }
            }
            else
            {
                this.receive(message);
            }
            //666 return true;
        }
        catch (Exception ex)
        {
            this.ex = ex;
            if(allowLogger)
            {
                Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.send()", ex);
            }
            exception(ex);
            //666 return false;
        }
    }

    /**
     * Runnable submitted to the Hive's thread pool to drain messages from the
     * internal queue. Acquires a semaphore permit so that at most {@code threads}
     * workers run concurrently. Each worker drains as many messages as are
     * available before releasing its permit.
     */
    private final Runnable receiveTask = new Runnable()
    {
        @Override
        public void run()
        {
            if(!semaphore.tryAcquire())
            {
                return;
            }
            try
            {
                M m;
                while ((m = queue.poll()) != null)
                {
                    try
                    {
                        receive(m);
                    }
                    catch (Exception ex)
                    {
                        Bee.this.ex = ex;
                        if(allowLogger)
                        {
                            Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.receiveTask.run()", ex);
                        }
                        exception(ex);
                    }
                }
            }
            finally
            {
                semaphore.release();
                if (!queue.isEmpty() && semaphore.availablePermits() > 0)
                {
                    try
                    {
                        Executor h = hive;
                        if (h != null)
                        {
                            h.execute(receiveTask);
                        }
                    }
                    catch (Exception ignored) {}
                }
                synchronized(lock)
                {
                    if(shutdownWhenEmpty && semaphore.availablePermits() == threads && queue.isEmpty())
                    {
                        shutdown(false);
                    }
                    lock.notifyAll();
                }
            }
        }
    };

    /**
     * Runnable submitted to the Hive's thread pool to perform the final
     * shutdown handshake: drains any remaining messages, acquires all semaphore
     * permits (to wait for in-flight workers), then calls {@link #terminate()}
     * once the queue is empty.
     */
    private final Runnable shutdownTask = new Runnable()
    {
        @Override
        public void run()
        {
            // Last chance for messages in the queue to be received.
            receiveTask.run();

            semaphore.acquireUninterruptibly(threads);
            try
            {
                synchronized(lock)
                {
                    while(status==SHUTDOWN)
                    {
                        if(queue.isEmpty())
                        {
                            status=TERMINATED;
                            terminate();
                            break;
                        }
                        semaphore.release(threads); 
                        try
                        {
                            Executor h = hive;
                            if (h != null)
                            {
                                h.execute(receiveTask);
                            }
                            lock.wait();
                        }
                        finally
                        {
                            semaphore.acquireUninterruptibly(threads); // Re-adquirir                        
                        }
                    }
                    lock.notifyAll();
                }
            }
            catch (InterruptedException ex)
            {
                Bee.this.ex = ex;
                if (allowLogger)
                {
                    Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.shutdownTask.run()", ex);
                }
                exception(ex);
            }
            finally
            {
                semaphore.release(threads);
            }
        }
    };

    /**
     * Blocks the calling thread until all worker threads are idle and the
     * internal queue is empty, then returns. Does not change the Bee's state;
     * the Bee continues accepting messages after this call.
     *
     * @return this Bee, for fluent chaining
     */
    public Bee<M> waitForIdle()
    {
        synchronized(lock)
        {
            try
            {
                while(semaphore.availablePermits() < threads || !queue.isEmpty())
                {
                    lock.wait();
                }
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return this;
    }

    /**
     * Initiates a graceful shutdown: the Bee stops accepting new messages but
     * finishes processing those already in the queue. Equivalent to
     * {@code shutdown(false)}.
     *
     * @return this Bee, for fluent chaining
     */
    public Bee<M> shutdown()
    {
        return shutdown(false);
    }

    /**
     * Initiates a graceful shutdown.
     *
     * @param onlyWhenEmpty if {@code true}, the shutdown is deferred until the
     *                      internal queue is empty and all workers are idle
     *                      (i.e. the Bee shuts itself down automatically once it
     *                      runs out of work); if {@code false}, the shutdown
     *                      sequence starts immediately
     * @return this Bee, for fluent chaining
     */
    public Bee<M> shutdown(boolean onlyWhenEmpty)
    {
        synchronized(lock)
        {
            if (onlyWhenEmpty)
            {
                this.shutdownWhenEmpty = true;
                if (semaphore.availablePermits() == threads && queue.isEmpty())
                {
                    shutdown(false);
                }
                return this;
            }
            if(this.status==RUNNING)
            {
                this.status = SHUTDOWN;
                if(this.hive!=null)
                {
                    this.hive.execute(shutdownTask);
                }
                else if(queue.isEmpty()) 
                {
                    status = TERMINATED;
                    terminate();
                }
            }
        }
        return this;
    }

    /**
     * Returns {@code true} if {@link #shutdown()} has been called (regardless
     * of whether the Bee has finished processing).
     *
     * @return {@code true} once shutdown has been initiated
     */
    public boolean isShutdown()
    {
        return this.status!=RUNNING;
    }

    /**
     * Returns {@code true} if the Bee has fully terminated: shutdown was called,
     * the queue was drained, and {@link #terminate()} has completed.
     *
     * @return {@code true} when the Bee is in the {@code TERMINATED} state
     */
    public boolean isTerminated()
    {
        return this.status==TERMINATED;
    }

    /**
     * Blocks the calling thread until the Bee is terminated or the given
     * timeout elapses.
     *
     * @param millis the maximum time to wait, in milliseconds
     * @return {@code true} if the Bee terminated within the timeout;
     *         {@code false} if the timeout elapsed first
     */
    public boolean awaitTermination(int millis)
    {
        try
        {
            long untilNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
            return awaitTerminationUntilNanos(untilNanos);
        }
        catch (InterruptedException ex)
        {
            Bee.this.ex = ex;
            if(allowLogger)
            {
                Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.awaitTermination()", ex);
            }
            exception(ex);
            return false;
        }
    }

    /**
     * Blocks until the Bee is terminated or the absolute deadline (in nanoseconds,
     * as returned by {@link System#nanoTime()}) is reached. Used internally by
     * {@link Hive#awaitTermination(Sendable, boolean, int)}.
     *
     * @param untilNanos the deadline as an absolute nanosecond timestamp
     * @return {@code true} if the Bee terminated before the deadline
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting
     */
    protected boolean awaitTerminationUntilNanos(long untilNanos) throws InterruptedException
    {
        boolean rc = false;
        synchronized(lock)
        {
            long now;
            while(!(rc=isTerminated()) && (now=System.nanoTime())<untilNanos)
            {
                long remaining = untilNanos - now;
                lock.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            }
            return rc;
        }
    }

    /**
     * Attaches (or replaces) the {@link Hive} thread pool used to execute
     * worker tasks. Can be called before the first {@link #send(Object)} to
     * configure a Bee that was constructed without a Hive.
     *
     * @param hive the Hive to use for asynchronous message dispatch
     */
    public void setHive(Hive hive)
    {
        this.hive = hive;
    }

    /**
     * Subscribes this Bee to {@code topic} on the attached {@link Hive}.
     * <p>
     * After this call, every message published via {@link Hive#pub(String)}
     * for the same topic will be delivered to this Bee through
     * {@link #accept(Object)}. The Bee must have been constructed with a
     * non-{@code null} Hive (or one must have been attached via
     * {@link #setHive(Hive)}) before calling this method.
     *
     * @param topic the topic name; must not be {@code null}
     * @return this Bee, for fluent chaining
     * @throws IllegalStateException if no Hive has been attached to this Bee
     */
    @SuppressWarnings("unchecked")
    public Bee<M> sub(String topic)
    {
        if (!(this.hive instanceof Hive))
        {
            throw new IllegalStateException("No Hive attached; call setHive(Hive) first or use a Hive-aware constructor.");
        }
        ((Hive) this.hive).sub(topic, this);
        return this;
    }
}
