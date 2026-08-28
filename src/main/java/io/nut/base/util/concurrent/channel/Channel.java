/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

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
 * <p>Use the static factory methods below to build channels. The
 * {@code closeable*} variants return a {@link CloseableChannel} that can be
 * explicitly closed to signal end-of-data.
 *
 * <p>Example:
 * <pre>{@code
 * Channel<String> ch = Channel.of(10);
 * new Thread(() -> {
 *     try { ch.put("hello"); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
 * }).start();
 * String msg = ch.get();   // "hello"
 * }</pre>
 */
public abstract class Channel<E> implements ChannelReader<E>, ChannelWriter<E>
{
    private volatile boolean interrupted;

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
     * Creates the appropriate channel according to the given capacity:
     * <ul>
     *   <li><code> capacity == 0</code>: unbuffered (rendezvous)</li>
     *   <li><code> capacity &gt; 0</code>: buffered with that capacity</li>
     *   <li><code> capacity &lt; 0</code>: unbounded capacity</li>
     * </ul>
     * Use this single entry point when the desired buffering is known only at
     * runtime (e.g. read from configuration).
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
     * Use it when the producer needs to signal "no more data" by closing the
     * channel so consumers can stop waiting.
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
        if(capacity>0)
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
     * @param capacity the number of items the buffer can hold
     * @param <T>      the element type
     * @return a buffered channel with the given capacity
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
     * @param <T> the type of elements read
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

