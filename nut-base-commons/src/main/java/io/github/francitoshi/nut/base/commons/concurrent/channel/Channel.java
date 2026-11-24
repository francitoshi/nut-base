/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for thread-safe communication channels between
 * producers and consumers, the backbone of the package (see
 * {@code doc/channel-rules.txt} for the full design rules).
 *
 * <p>A {@code Channel} combines a write side ({@link ChannelWriter#put}) and
 * a read side ({@link ChannelReader#get}). Every channel is closeable, and
 * only the producer closes it ({@link ChannelWriter#close}). Depending on the
 * capacity policy, a channel can be:
 * <ul>
 *   <li><b>Unbuffered</b> (rendezvous): a {@code put} blocks until a {@code get}
 *       takes the value and vice versa. Suited for direct hand-off / synchronization.</li>
 *   <li><b>Buffered</b>: a fixed number of values are queued, decoupling producer
 *       and consumer timing. Suited for work queues / pipelines.</li>
 *   <li><b>Unlimited</b>: no capacity limit. Suited when the producer may run ahead
 *       without ever blocking (at the cost of unbounded memory).</li>
 *   <li><b>Conflated</b>: only the {@code N} last values are retained; older
 *       pending values are dropped as new ones arrive. Suited for signal /
 *       state-update patterns.</li>
 * </ul>
 *
 * <p><b>Interruption contract.</b> When a channel operation is interrupted
 * while waiting, the operation aborts: {@link #get()} returns {@code null}
 * and {@code put} returns {@code false}, and the channel stays open — an
 * interruption never closes a channel. The interruption request is marked in
 * the channel ({@link #isInterrupted()}), and everything else is processed by
 * the installed {@link Channel.InterruptedExceptionListener}: the listener is
 * the explicit decision point for applying the interruption to the thread.
 * The default listener just logs the interruption and leaves the thread's
 * interrupt flag cleared.
 *
 * <p><b>Iteration contract.</b> Every channel is an {@link Iterable} that,
 * on each call to {@link #iterator()}, creates an iterator specific to that
 * occasion. {@link Iterator#hasNext()} is hooked to {@link #get()}: it
 * returns {@code false} only when {@code get()} yielded {@code null} while
 * the channel is closed, and the value obtained is remembered to be returned
 * by {@link Iterator#next()} regardless of whether it is null or not. A loop
 * over a channel therefore ends only when the channel is closed and drained.
 *
 * <p><b>No select.</b> There is deliberately no Go-style {@code select} here,
 * and it should not be implemented. The benchmarks (see
 * {@code BenchmarkVTChannelTest}) show that waiting with one virtual thread
 * parked in {@link #get()} per channel is orders of magnitude faster than a
 * shared waiter re-scanning the channels after every wake-up: each channel's
 * own lock and condition wake its reader directly, with no listener dispatch,
 * no re-scan of the other channels and no polling fallback. On a Java 21+ JVM,
 * merge or multiplex channels with the thread-per-channel pattern (one
 * {@code get()} loop per source feeding a common downstream channel); on older
 * JVMs, fall back to polling the channels with {@link ChannelReader#tryGet}
 * and {@link ChannelWriter#tryPut}.
 */
public abstract class Channel<E> implements ChannelReader<E>, ChannelWriter<E>, Iterable<E>
{
    private static final Logger LOG = Logger.getLogger(Channel.class.getName());

    private volatile boolean interrupted;

    /**
     * Listener used by {@link #handleInterruptedException}; defaults to
     * {@link #DEFAULT_INTERRUPTED_EXCEPTION_LISTENER}.
     */
    private volatile InterruptedExceptionListener interruptedExceptionListener = DEFAULT_INTERRUPTED_EXCEPTION_LISTENER;

    /**
     * Whether {@link ChannelWriter#close()} has been called. Written by
     * {@link #markClosed()} from the concrete engines and read freely.
     */
    protected volatile boolean closed;

    /**
     * Returns whether this channel has been closed via
     * {@link ChannelWriter#close()}.
     *
     * @return {@code true} if the channel has been closed
     */
    @Override
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Marks the channel as closed and wakes every {@link #join()} waiter.
     * <p>
     * Internal to the concrete engines: their {@code close()} calls it once
     * the close has taken effect on their own engine (under the same lock
     * that stops further puts, so no further value slips in after the
     * transition).
     */
    protected final void markClosed()
    {
        closed = true;
        synchronized (joinLock)
        {
            joinLock.notifyAll();
        }
    }

    /**
     * Blocks the current thread until this channel is closed. If the channel
     * is already closed, returns immediately.
     *
     * @throws InterruptedException if the current thread is interrupted while
     *         waiting
     */
    public void join() throws InterruptedException
    {
        synchronized (joinLock)
        {
            while (!closed)
            {
                joinLock.wait();
            }
        }
    }

    private final Object joinLock = new Object();

    // -----------------------------------------------------------------------
    // Interruption tracking
    // -----------------------------------------------------------------------

    /**
     * Returns whether an {@link InterruptedException} has ever been raised in
     * this channel (by any {@link ChannelReader#get} or {@link ChannelWriter#put}).
     * Once {@code true}, it stays {@code true} for the lifetime of the channel.
     *
     * @return {@code true} if at least one operation was interrupted
     */
    public boolean isInterrupted()
    {
        return interrupted;
    }
    /**
     * Listener that processes the {@link InterruptedException}s raised while a
     * channel operation is waiting.
     * <p>
     * The listener is the explicit decision point for applying the
     * interruption to the thread: the channel marks its own
     * {@link #isInterrupted()} flag and delegates everything else, so a
     * listener that needs the standard contract of a re-marked thread must
     * call {@code Thread.currentThread().interrupt()} itself (see
     * {@link #DEFAULT_INTERRUPTED_EXCEPTION_LISTENER} for the default,
     * leave-no-trace behavior).
     */
    @FunctionalInterface
    public interface InterruptedExceptionListener
    {
        /**
         * Processes an interruption raised while waiting on a channel
         * operation.
         *
         * @param exception the interruption to process
         */
        void processInterruptedException(InterruptedException exception);
    }

    /**
     * Default interruption listener, which logs the interruption and leaves
     * no trace on the thread.
     * <p>
     * With this listener an interrupted {@link ChannelReader#get()} returns
     * {@code null} and an interrupted {@link ChannelWriter#put(Object)}
     * returns {@code false}, exactly as if no interruption had happened: the
     * exception is considered fully processed and nothing else is required
     * but to return. Only {@link #isInterrupted()} keeps a record of it.
     * <p>
     * The interrupt status of the thread is deliberately <em>not</em> marked
     * again: blocking calls clear it when they throw
     * {@link InterruptedException}, and it stays cleared after processing.
     * Marking it here could permanently pollute recycled executor and worker
     * pool threads whose tasks happen to use channels.
     * <p>
     * Applying an interruption by marking the thread afterwards is a decision
     * for the creator of the channel, taken explicitly by installing its own
     * {@link Channel.InterruptedExceptionListener} with
     * {@link #setInterruptedExceptionListener}.
     */
    public static final InterruptedExceptionListener DEFAULT_INTERRUPTED_EXCEPTION_LISTENER = Channel::logInterruptedException;

    /**
     * Logs an interruption, leaving the thread un-marked. The placeholder
     * behind {@link #DEFAULT_INTERRUPTED_EXCEPTION_LISTENER}; when the log
     * entry is unwanted, for example in tests that interrupt on purpose,
     * install {@link #ignoreInterruptedException} instead.
     *
     * @param exception the interruption to log
     */
    public static void logInterruptedException(InterruptedException exception)
    {
        LOG.log(Level.WARNING, "Interrupted while waiting on a channel operation", exception);
    }

    /**
     * Discards an interruption silently: nothing is logged, and like
     * {@link #logInterruptedException} the thread is never re-marked.
     * <p>
     * Useful as the installed listener (see
     * {@link #setInterruptedExceptionListener}) in tests and other contexts
     * that intentionally interrupt channel operations and do not want the
     * warning that the default listener produces.
     *
     * @param exception the interruption to discard
     */
    public static void ignoreInterruptedException(InterruptedException exception)
    {
    }

    /**
     * Returns the listener used when a blocking operation is interrupted.
     *
     * @return the listener; never {@code null}
     */
    public final InterruptedExceptionListener getInterruptedExceptionListener()
    {
        return interruptedExceptionListener;
    }

    /**
     * Sets the listener used when a blocking operation is interrupted.
     * <p>
     * Installing a listener here is the explicit opt-in for how the
     * interruption is applied afterwards. {@link Channel} itself never
     * restores the thread's interrupt flag: with the default listener the
     * interruption is swallowed (processed and no further reaction is
     * required but to return, which keeps recycled executor threads
     * un-marked), and with a custom listener the flag is restored only if
     * that listener marks the thread itself, e.g. with
     * {@code Thread.currentThread().interrupt()}.
     *
     * @param listener the listener to use; must not be {@code null}
     */
    public void setInterruptedExceptionListener(InterruptedExceptionListener listener)
    {
        interruptedExceptionListener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Handles an interruption received during a blocking operation.
     * Marking the channel ({@link #isInterrupted()}) is internal to this
     * family; the rest of the reaction is delegated to the installed
     * {@link Channel.InterruptedExceptionListener}, which decides whether the
     * thread is marked again (by default it is not). Must be invoked by the
     * concrete engines inside their {@code catch (InterruptedException ex)}
     * blocks.
     *
     * @param ex the caught interruption exception
     */
    protected final void handleInterruptedException(InterruptedException ex)
    {
        interrupted = true;
        interruptedExceptionListener.processInterruptedException(ex);
    }

    // -----------------------------------------------------------------------
    // Iteration
    // -----------------------------------------------------------------------

    /**
     * Returns an iterator over the values read from this channel.
     * Each call creates an iterator specific to that occasion.
     *
     * <p>{@link Iterator#hasNext} blocks in {@link #get()}, keeps the value
     * ready for {@link Iterator#next} and returns {@code false} only when
     * {@code get()} yielded {@code null} while {@link #isClosed()} is
     * {@code true}: a for-each loop over a channel ends only when the channel
     * is closed and drained.
     *
     * @return a new {@code Iterator} reading from this channel
     */
    @Override
    public Iterator<E> iterator()
    {
        return new Iterator<E>()
        {
            private E saved;
            private boolean hasSaved;

            @Override
            public boolean hasNext()
            {
                if (hasSaved)
                {
                    return true;
                }
                E value = get();
                if (value == null && closed)
                {
                    return false;
                }
                saved = value;
                hasSaved = true;
                return true;
            }

            @Override
            public E next()
            {
                if (!hasSaved)
                {
                    throw new NoSuchElementException();
                }
                hasSaved = false;
                return saved;
            }
        };
    }

    /**
     * Returns a {@link Spliterator} backed by the iterator created by
     * {@link #iterator()}. Like {@code iterator()}, each call creates a
     * spliterator specific to that occasion.
     *
     * <p>No characteristics are reported: values are delivered blocking one
     * at a time, their encounter order is not guaranteed (conflated channels
     * drop old values, they are not strictly FIFO) and a value may be
     * {@code null} when the underlying {@link #get()} was interrupted.
     *
     * @return a new {@code Spliterator} reading from this channel
     */
    @Override
    public Spliterator<E> spliterator()
    {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }

    // -----------------------------------------------------------------------
    // Factories
    // -----------------------------------------------------------------------

    /** Explicit capacity value requesting an unbounded channel in {@link #of(int)}. */
    public static final int UNLIMITED = -1;

    /**
     * Creates the appropriate channel according to the given capacity:
     * <ul>
     *   <li><code> capacity == 0</code>: unbuffered (rendezvous)</li>
     *   <li><code> capacity &gt; 0</code>: buffered with that capacity</li>
     *   <li><code> capacity == {@link #UNLIMITED}</code>: unbounded capacity</li>
     * </ul>
     * Any other negative value is rejected: a size coming from configuration,
     * input or an arithmetic overflow must not silently turn into an unbounded
     * queue (memory exhaustion).
     *
     * @param capacity the channel capacity, see semantics above
     * @param <T>      the element type
     * @return a channel matching the requested capacity
     * @throws IllegalArgumentException if {@code capacity} is negative and not {@link #UNLIMITED}
     */
    public static <T> Channel<T> of(int capacity)
    {
        if(capacity==0)
        {
            return new UnbufferedChannel<>();
        }
        if(capacity>0)
        {
            return new BufferedChannel<>(capacity);
        }
        if(capacity==UNLIMITED)
        {
            return new UnlimitedChannel<>();
        }
        throw new IllegalArgumentException("capacity must be >= 0 or Channel.UNLIMITED");
    }

    /**
     * Unbuffered channel: {@link ChannelWriter#put} and {@link ChannelReader#get}
     * are synchronized hand in hand, with no buffer, a rendezvous.
     * Use it to synchronize two threads by exchanging a value directly.
     *
     * @param <T> the element type
     * @return an unbuffered rendezvous channel
     */
    public static <T> Channel<T> unbuffered()
    {
        return new UnbufferedChannel<>();
    }

    /**
     * Channel with a fixed-capacity buffer.
     * Use it for work queues / pipelines where the producer may run ahead of the
     * consumer, up to {@code capacity} pending items.
     *
     * @param capacity the number of items the buffer can hold (must be &gt; 0)
     * @param <T>      the element type
     * @return a buffered channel with the given capacity
     * @throws IllegalArgumentException if {@code capacity} is not positive
     */
    public static <T> Channel<T> buffered(int capacity)
    {
        return new BufferedChannel<>(capacity);
    }

    /**
     * Channel with no capacity limit.
     * Use it when the producer must never block, accepting that memory grows
     * with the amount of unconsumed data.
     *
     * @param <T> the element type
     * @return an unbounded channel
     */
    public static <T> Channel<T> unlimited()
    {
        return new UnlimitedChannel<>();
    }

    /**
     * Conflated channel with capacity 1: only the latest value is retained.
     * Equivalent to Kotlin's {@code Channel(CONFLATED)}. When a producer puts
     * a new value before the previous one is consumed, the old value is
     * silently dropped. Ideal for signal / state-update patterns.
     *
     * @param <T> the element type
     * @return a conflated channel of capacity 1
     */
    public static <T> Channel<T> conflated()
    {
        return new ConflatedChannel<>();
    }

    /**
     * Conflated channel that keeps only the {@code capacity} last values: as
     * new values arrive, the oldest pending ones are dropped. Use it when the
     * consumer may fall a few values behind without blocking the producer.
     *
     * @param capacity the number of latest values to keep (must be &gt; 0)
     * @param <T>      the element type
     * @return a conflated channel with the given capacity
     * @throws IllegalArgumentException if {@code capacity} is not positive
     */
    public static <T> Channel<T> conflated(int capacity)
    {
        return new ConflatedChannel<>(capacity);
    }

    // -----------------------------------------------------------------------
    // Fan-out
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link FanoutChannel} that broadcasts every
     * {@link ChannelWriter#put} to all supplied targets.
     *
     * @param <T>      the element type
     * @param channels the downstream destinations; must not be {@code null}
     * @return a new fan-out channel
     */
    @SafeVarargs
    public static <T> FanoutChannel<T> fanout(ChannelWriter<T>... channels)
    {
        return new FanoutChannel<>(channels);
    }
}
