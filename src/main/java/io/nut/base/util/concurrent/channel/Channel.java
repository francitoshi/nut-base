/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;

import io.nut.base.math.Nums;
import io.nut.base.util.tuple.Tuple2;

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
 * channel ({@link #isInterrupted()}) but it is not signaled again in the
 * thread, because it has already been processed by the operation.
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
 */
public abstract class Channel<E> implements ChannelReader<E>, ChannelWriter<E>, Iterable<E>
{
    private volatile boolean interrupted;

    /**
     * Returns an iterator over the values read from this channel.
     * Each call creates an iterator specific to that occasion.
     *
     * <p>This base implementation gives a non-closeable iteration:
     * {@link Iterator#hasNext} always returns {@code true} and
     * {@link Iterator#next} is hooked directly to {@link #get()}, blocking
     * until the next value is available.
     *
     * <p>Closeable channels ({@link CloseableChannel}) override this method to
     * stop iteration once the channel has been closed.
     *
     * @return a new {@code Iterator} reading from this channel
     */
    @Override
    public Iterator<E> iterator()
    {
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

    /**
     * Handles an interruption received during a blocking operation.
     * The interruption request is marked in the channel but it is not
     * signaled again in the thread, because it has already been processed
     * by the interrupted operation. Must be invoked by the subclasses inside
     * their {@code catch (InterruptedException ex)} blocks.
     *
     * @param ex the caught interruption exception
     */
    protected void handleInterruptedException(InterruptedException ex)
    {
        markInterrupted();
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

    /**
     * Computes the point in time, in {@link System#nanoTime()} space, that is
     * {@code timeout} away from now, saturating instead of overflowing.
     * <p>
     * Both the unit-to-nanos conversion and the addition are clamped so that an
     * absurdly large timeout behaves as an almost infinite wait rather than
     * wrapping around and expiring immediately.
     *
     * @param timeout the wait duration
     * @param unit    the time unit of the timeout
     * @return the saturated deadline in the same space as {@link System#nanoTime()}
     */
    static long toDeadline(long timeout, TimeUnit unit)
    {
        return Nums.saturatedAdd(System.nanoTime(), toNanosSaturated(timeout, unit));
    }

    private static long toNanosSaturated(long timeout, TimeUnit unit)
    {
        if (timeout == 0 || unit == TimeUnit.NANOSECONDS)
        {
            return unit.toNanos(timeout);
        }
        long scaleNanos = unit.toNanos(1);
        long maxTimeout = Long.MAX_VALUE / scaleNanos;
        if (timeout > maxTimeout)
        {
            return Long.MAX_VALUE;
        }
        if (timeout < -maxTimeout)
        {
            return Long.MIN_VALUE;
        }
        return unit.toNanos(timeout);
    }

    /**
     * Creates the appropriate channel according to the given capacity:
     * <ul>
     *   <li><code> capacity == 0</code>: unbuffered (rendezvous)</li>
     *   <li><code> capacity &gt; 0</code>: buffered with that capacity</li>
     *   <li><code> capacity &lt; 0</code>: unbounded capacity</li>
     * </ul>
     *
     * @param capacity the channel capacity, see semantics above
     * @param <T>      the element type
     * @return a channel matching the requested capacity
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
        return new UnlimitedChannel<>();
    }

    /**
     * Same as {@link #of(int)} but returns a {@link CloseableChannel}.
     *
     * @param capacity the channel capacity, see {@link #of(int)}
     * @param <T>      the element type
     * @return a closeable channel matching the requested capacity
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
        return new CloseableUnlimitedChannel<>();
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
