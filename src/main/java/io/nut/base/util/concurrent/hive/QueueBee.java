/*
 * Copyright (c) 2026 francitoshi@gmail.com
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A terminal pipeline stage that doubles as a {@link BlockingQueue}: every
 * message received via the {@link Bee} machinery is put into a delegate
 * {@link BlockingQueue}{@code <E>} supplied at construction time, and every
 * {@code BlockingQueue} method is delegated to that same queue.
 * <p>
 * This makes it possible to use a {@code QueueBee} as the end-point of a
 * {@link PipeBee} / {@link FilterBee} chain (e.g. with {@link PipeBee#linkTo})
 * while still consuming the produced elements as a plain {@code BlockingQueue}
 * from any other thread — for example with a blocking {@code take()} in a
 * consumer loop:
 * <pre>{@code
 * BlockingQueue<String> q = new LinkedBlockingQueue<>();
 * QueueBee<String>      qb = hive.queue(q);
 * pipe.linkTo(qb);
 *
 * // in another thread:
 * while (true) {
 *     String item = q.take();
 *     process(item);
 * }
 * }</pre>
 * <p>
 * {@link #receive(Object)} calls {@link BlockingQueue#put put()} on the
 * delegate, blocking the worker thread if the queue is at capacity. This
 * provides natural back-pressure: when the consumer is slow, the producer's
 * worker thread stalls until space is available, slowing the whole pipeline.
 * <p>
 * Note that the {@code BlockingQueue} itself must be thread-safe (all standard
 * implementations from {@code java.util.concurrent} are), but the caller is
 * responsible for choosing a queue with the appropriate capacity and ordering
 * semantics.
 *
 * @param <E> the type of elements held by the delegate queue
 */
public class QueueBee<E> extends Bee<E> implements BlockingQueue<E>
{
    private final BlockingQueue<E> delegate;

    /**
     * Full constructor.
     *
     * @param threads   the maximum number of concurrent worker threads
     * @param hive      the Hive thread pool, or {@code null} for synchronous mode
     * @param queueSize the internal Bee queue capacity (0 = default); distinct
     *                  from the delegate {@code BlockingQueue}'s capacity
     * @param delegate  the {@code BlockingQueue} that stores received elements;
     *                  must not be {@code null}
     */
    public QueueBee(int threads, Hive hive, int queueSize, BlockingQueue<E> delegate)
    {
        super(threads, hive, queueSize);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a QueueBee with the given thread count and Hive, using the
     * default internal queue size.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param hive     the Hive thread pool, or {@code null} for synchronous mode
     * @param delegate the delegate queue; must not be {@code null}
     */
    public QueueBee(int threads, Hive hive, BlockingQueue<E> delegate)
    {
        super(threads, hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a QueueBee attached to the given Hive with the default thread
     * count and internal queue size.
     *
     * @param hive     the Hive thread pool, or {@code null} for synchronous mode
     * @param delegate the delegate queue; must not be {@code null}
     */
    public QueueBee(Hive hive, BlockingQueue<E> delegate)
    {
        super(hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a standalone QueueBee with the given thread count but no Hive.
     * A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param delegate the delegate queue; must not be {@code null}
     */
    public QueueBee(int threads, BlockingQueue<E> delegate)
    {
        super(threads);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a standalone QueueBee with the default thread count and no
     * Hive. A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param delegate the delegate queue; must not be {@code null}
     */
    public QueueBee(BlockingQueue<E> delegate)
    {
        super();
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Receives a message from the Bee pipeline and puts it into the delegate
     * queue, blocking the calling worker thread if the queue is currently at
     * capacity. This implements natural back-pressure: a slow consumer slows the
     * entire upstream pipeline.
     *
     * @param e the message to insert into the delegate queue
     * @throws RuntimeException wrapping {@link InterruptedException} if the
     *                          worker thread is interrupted while waiting for
     *                          space in the delegate queue; the thread's
     *                          interrupted flag is also restored
     */
    @Override
    protected void receive(E e)
    {
        try
        {
            this.delegate.put(e);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    // =========================================================== Collection

    /** {@inheritDoc} */
    @Override
    public int size()
    {
        return delegate.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(Object o)
    {
        return delegate.contains(o);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<E> iterator()
    {
        return delegate.iterator();
    }

    /** {@inheritDoc} */
    @Override
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T[] toArray(T[] a)
    {
        return delegate.toArray(a);
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(E e)
    {
        return delegate.add(e);
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Object o)
    {
        return delegate.remove(o);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        return delegate.containsAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        return delegate.addAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        return delegate.removeAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        return delegate.retainAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public void clear()
    {
        delegate.clear();
    }

    // ================================================================= Queue

    /** {@inheritDoc} */
    @Override
    public boolean offer(E e)
    {
        return delegate.offer(e);
    }

    /** {@inheritDoc} */
    @Override
    public E remove()
    {
        return delegate.remove();
    }

    /** {@inheritDoc} */
    @Override
    public E poll()
    {
        return delegate.poll();
    }

    /** {@inheritDoc} */
    @Override
    public E element()
    {
        return delegate.element();
    }

    /** {@inheritDoc} */
    @Override
    public E peek()
    {
        return delegate.peek();
    }

    // ========================================================= BlockingQueue

    /** {@inheritDoc} */
    @Override
    public void put(E e) throws InterruptedException
    {
        delegate.put(e);
    }

    /** {@inheritDoc} */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
    {
        return delegate.offer(e, timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public E take() throws InterruptedException
    {
        return delegate.take();
    }

    /** {@inheritDoc} */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException
    {
        return delegate.poll(timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public int remainingCapacity()
    {
        return delegate.remainingCapacity();
    }

    /** {@inheritDoc} */
    @Override
    public int drainTo(Collection<? super E> c)
    {
        return delegate.drainTo(c);
    }

    /** {@inheritDoc} */
    @Override
    public int drainTo(Collection<? super E> c, int maxElements)
    {
        return delegate.drainTo(c, maxElements);
    }
}
