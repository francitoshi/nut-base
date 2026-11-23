/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.francitoshi.nut.base.commons.tuple.Tuple2;

/**
 * Abstract base class for thread-safe communication channels between
 * producers and consumers.
 *
 * <p>A {@code Channel} combines a write side ({@link ChannelWriter#put}) and a
 * read side ({@link ChannelReader#get}). Depending on the capacity, a channel
 * can be:
 * <ul>
 *   <li><b>Unbuffered</b> (rendezvous): a {@code put} blocks until a {@code get}
 *       takes the value and vice versa. Suited for direct hand-off / synchronization.</li>
 *   <li><b>Buffered</b>: a fixed number of values are queued, decoupling producer
 *       and consumer timing. Suited for work queues / pipelines.</li>
 *   <li><b>Unlimited</b>: no capacity limit. Suited when the producer may run ahead
 *       without ever blocking (at the cost of unbounded memory).</li>
 * </ul>
 *
 * <p><b>Interruption contract.</b> When a channel receives an interruption
 * request during a blocking operation, the operation aborts: {@link #get()}
 * returns {@code null} and {@link ChannelWriter#put} returns without
 * delivering its value. If the channel is closeable, the aborting operation
 * additionally requests a shutdown via {@link ChannelCloser#close()} (see
 * {@link #closeIfCloseable()}). The interruption request is marked in the
 * channel ({@link #isInterrupted()}), and everything else is processed by the
 * installed {@link Channel.InterruptedExceptionListener}: the listener is the
 * explicit decision point for applying the interruption to the thread. The
 * default listener just logs the interruption and leaves the thread's
 * interrupt flag cleared.
 *
 * <p><b>Iteration contract.</b> Every channel is an {@link Iterable} that,
 * on each call to {@link #iterator()}, creates an iterator specific to that
 * occasion. In non-closeable channels {@link Iterator#hasNext()} always
 * returns {@code true} and {@link Iterator#next()} is hooked directly to
 * {@link #get()}. In closeable channels ({@link CloseableChannel})
 * {@link Iterator#hasNext()} is hooked to {@link #get()}: it returns
 * {@code false} only when {@code get()} yielded {@code null} while the
 * channel is closed, and the value obtained is remembered to be returned by
 * {@link Iterator#next()} regardless of whether it is null or not.
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
     * Returns an iterator over the values read from this channel.
     * Each call creates an iterator specific to that occasion.
     *
     * <p>This base implementation gives a non-closeable iteration:
     * {@link Iterator#hasNext} always returns {@code true} and
     * {@link Iterator#next} is hooked directly to {@link #get()}, blocking
     * until the next value is available.
     *
     * <p>Closeable channels ({@link CloseableChannel}) stop iteration once the
     * channel has been closed: this dispatch is centralized here, so the
     * {@code CloseableX} subclasses do not need to override {@code iterator()}.
     *
     * @return a new {@code Iterator} reading from this channel
     */
    @Override
    public Iterator<E> iterator()
    {
        if (this instanceof ChannelCloser)
        {
            return terminatingIterator();
        }
        return new Iterator<E>()
        {
            @Override
            public boolean hasNext()
            {
                return true;
            }

            @Override
            public E next()
            {
                return get();
            }
        };
    }

    /**
     * Iterator that stops once the read side is closed: {@link Iterator#hasNext}
     * blocks in {@link #get()}, keeps the value ready for {@link Iterator#next}
     * and returns {@code false} only when {@code get()} yielded {@code null}
     * while {@link #isReadClosed()} is {@code true}.
     *
     * <p>Shared by {@link CloseableChannel} and by {@link DuplexChannel}s whose
     * input is closeable, so that a {@code for-each} over them terminates
     * instead of spinning forever on {@code null}s.
     */
    final Iterator<E> terminatingIterator()
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
                if (value == null && isReadClosed())
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
     * overwrite, they are not FIFO) and, in non-closeable channels, a value
     * may be {@code null} when the underlying {@link #get()} was interrupted.
     *
     * @return a new {@code Spliterator} reading from this channel
     */
    @Override
    public Spliterator<E> spliterator()
    {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }

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

    final void markInterrupted()
    {
        interrupted = true;
    }

    // -----------------------------------------------------------------------
    // Close state (only meaningful on CloseableChannel subclasses)
    // -----------------------------------------------------------------------

    /**
     * Whether {@link ChannelCloser#close()} has been called. Written by
     * {@link #markClosed()} through the closeable implementations and read
     * freely; on non-closeable channels it is never set, so
     * {@link #isClosed()} stays {@code false} for their whole lifetime.
     */
    protected volatile boolean closed;

    /**
     * Returns whether this channel has been closed via
     * {@link ChannelCloser#close()}. On non-closeable channels it always
     * returns {@code false} ({@code close()} only exists on the
     * {@link CloseableChannel} family).
     *
     * @return {@code true} if a closeable channel has been closed
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Marks the channel as closed and wakes every {@link #join()} waiter.
     * <p>
     * Internal to the {@link CloseableChannel} implementations: their
     * {@code close()} calls it once the close has taken effect on their own
     * engine (under the same lock that stops further puts, so no further
     * value slips in after the transition).
     */
    protected final void markClosed()
    {
        closed = true;
        wakeJoiners();
    }

    /**
     * Blocks the current thread until this closeable channel is closed. If
     * the channel is already closed, returns immediately.
     *
     * @throws IllegalStateException if the channel cannot ever be closed
     *         (it is not part of the {@link CloseableChannel} family)
     * @throws InterruptedException if the current thread is interrupted while
     *         waiting
     */
    public void join() throws InterruptedException
    {
        if (!(this instanceof ChannelCloser))
        {
            throw new IllegalStateException("not a closeable channel");
        }
        synchronized (joinLock)
        {
            while (!closed)
            {
                joinLock.wait();
            }
        }
    }

    /** Wakes the threads waiting in {@link #join()}; called by {@link #markClosed()}. */
    private final void wakeJoiners()
    {
        synchronized (joinLock)
        {
            joinLock.notifyAll();
        }
    }

    private final Object joinLock = new Object();

    /** Whether the read side of this channel is closed. */
    boolean isReadClosed()
    {
        return closed;
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
     * returns without delivering its value, exactly as if no interruption
     * had happened: the exception is considered fully processed and nothing
     * else is required but to return. Only {@link #isInterrupted()} keeps a
     * record of it.
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
     * subclasses inside their {@code catch (InterruptedException ex)} blocks.
     *
     * @param ex the caught interruption exception
     */
    protected final void handleInterruptedException(InterruptedException ex)
    {
        markInterrupted();
        interruptedExceptionListener.processInterruptedException(ex);
    }

    /**
     * Requests the shutdown of the channel if it implements {@link ChannelCloser}.
     * Does nothing on non-closeable channels. Subclasses must invoke it once
     * they have released the locks / counters of the interrupted operation.
     */
    protected final void closeIfCloseable()
    {
        if (this instanceof ChannelCloser)
        {
            ((ChannelCloser) this).close();
        }
    }

    /** Explicit capacity value requesting an unbounded channel in {@link #of(int)} and {@link #closeableOf(int)}. */
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
     * Same as {@link #of(int)} but returns a {@link CloseableChannel}.
     *
     * @param capacity the channel capacity, see {@link #of(int)}
     * @param <T>      the element type
     * @return a closeable channel matching the requested capacity
     * @throws IllegalArgumentException if {@code capacity} is negative and not {@link #UNLIMITED}
     */
    public static <T> CloseableChannel<T> closeableOf(int capacity)
    {
        if(capacity==0)
        {
            return new CloseableUnbufferedChannel<>();
        }
        if (capacity>0)
        {
            return new CloseableBufferedChannel<>(capacity);
        }
        if (capacity==UNLIMITED)
        {
            return new CloseableUnlimitedChannel<>();
        }
        throw new IllegalArgumentException("capacity must be >= 0 or Channel.UNLIMITED");
    }

    /**
     * Actor-mailbox flavour of {@link #closeableOf(int)}: the producer's own
     * interruption does not abort its {@code put} and does not close the
     * channel — the wait for room (or for the rendezvous hand-off) survives
     * the interrupt, so {@code accept()} can promise to wait permanently.
     * Reader interruptions keep the regular abort-and-close semantics.
     *
     * @param capacity the channel capacity, see {@link #of(int)}; only 0 and
     *                 positive values are supported by the mailbox flavour
     * @param <T>      the element type
     * @return a closeable mailbox channel matching the requested capacity
     * @throws IllegalArgumentException if {@code capacity} is negative (the
     *                                  mailbox flavour has no unbounded variant
     *                                  — a mailbox that cannot fill cannot
     *                                  exercise backpressure)
     */
    public static <T> CloseableChannel<T> closeableMailboxOf(int capacity)
    {
        if (capacity == 0)
        {
            return new CloseableUnbufferedChannel<>(true);
        }
        if (capacity > 0)
        {
            return new CloseableBufferedChannel<>(capacity, true);
        }
        throw new IllegalArgumentException("capacity must be >= 0 for a mailbox channel");
    }

    /**
     * Actor-mailbox flavour of {@link #closeableBuffered(int)} — see
     * {@link #closeableMailboxOf(int)}.
     *
     * @param capacity the buffer capacity; must be positive
     * @param <T>      the element type
     * @return a closeable mailbox channel with the given capacity
     * @throws IllegalArgumentException if {@code capacity <= 0}
     */
    public static <T> CloseableChannel<T> closeableMailboxBuffered(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        return new CloseableBufferedChannel<>(capacity, true);
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

    /** Closeable variant of {@link #unbuffered()}. Use it when the rendezvous must
     *  be able to be ended by closing the channel. */
    public static <T> CloseableChannel<T> closeableUnbuffered()
    {
        return new CloseableUnbufferedChannel<>();
    }

    /** Closeable variant of {@link #buffered(int)}. Use it for a closable work queue. */
    public static <T> CloseableChannel<T> closeableBuffered(int capacity)
    {
        return new CloseableBufferedChannel<>(capacity);
    }

    /** Closeable variant of {@link #unlimited()}. Use it for an unbounded, closable channel. */
    public static <T> CloseableChannel<T> closeableUnlimited()
    {
        return new CloseableUnlimitedChannel<>();
    }

    /**
     * Conflated channel: only the latest value is retained. Equivalent to
     * Kotlin's {@code Channel(CONFLATED)}. When a producer puts a new value
     * before the previous one is consumed, the old value is silently
     * overwritten. Ideal for signal / state-update patterns.
     *
     * @param <T> the element type
     * @return a conflated channel
     */
    public static <T> Channel<T> conflated()
    {
        return new ConflatedChannel<>();
    }

    /** Closeable variant of {@link #conflated()}. */
    public static <T> CloseableChannel<T> closeableConflated()
    {
        return new CloseableConflatedChannel<>();
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

    /**
     * Creates a {@link CloseableFanoutChannel} that broadcasts every
     * {@link ChannelWriter#put} to all supplied targets and can be closed
     * to propagate end-of-data to closeable targets.
     *
     * @param <T>      the element type
     * @param channels the downstream destinations; must not be {@code null}
     * @return a new closeable fan-out channel
     * @see CloseableFanoutChannel
     */
    @SafeVarargs
    public static <T> CloseableFanoutChannel<T> closeableFanout(ChannelWriter<T>... channels)
    {
        return new CloseableFanoutChannel<>(channels);
    }

    /**
     * Indicates whether this channel is bidirectional, i.e. elements sent with
     * {@link ChannelWriter#put} are not read back with {@link ChannelReader#get}
     * but delivered to another peer that in turn sends the elements obtained
     * through its own {@code get()}.
     *
     * @return {@code true} if the channel is duplex, {@code false} otherwise
     */
    public boolean isDuplex()
    {
        return false;
    }

    /**
     * Creates a duplex channel where write and read operations are disconnected.
     * Writes are forwarded to the {@code out} writer, and reads are retrieved from the {@code in} reader.
     *
     * @param in  the channel reader to delegate reads to
     * @param out the channel writer to delegate writes to
     * @param <T> the type of elements read from the input reader and written to the output writer
     * @return a duplex channel delegating to {@code out} and {@code in}
     */
    public static <T> DuplexChannel<T> duplex(ChannelReader<T> in, ChannelWriter<T> out)
    {
        return new DuplexChannel<>(in, out);
    }

    /**
     * Creates a pair of cross-connected duplex channels: elements put into the
     * first channel are read from the second, and vice versa.
     *
     * @param a  one end of the connection
     * @param b  the other end of the connection
     * @param <T> the type of elements exchanged
     * @return a pair of duplex channels, each reading what the other writes
     * @throws IllegalArgumentException if either channel is already duplex
     */
    public static <T> Tuple2<DuplexChannel<T>, DuplexChannel<T>> duplexPair(Channel<T> a, Channel<T> b)
    {
        if (a.isDuplex() || b.isDuplex())
        {
            throw new IllegalArgumentException("duplex channels cannot be connected as peers");
        }
        return new Tuple2<>(new DuplexChannel<>(a, b), new DuplexChannel<>(b, a));
    }
}
