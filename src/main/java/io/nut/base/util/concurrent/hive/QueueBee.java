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
 * A Bee&lt;E&gt; that is, at the same time, a BlockingQueue&lt;E&gt;: every
 * message it receives via {@link #receive(Object)} is put into a delegate
 * {@code BlockingQueue<E>} supplied at construction time, and every
 * BlockingQueue method is forwarded ("delegated") to that same queue.
 * <p>
 * This makes it possible to use a QueueBee as the terminal stage of a
 * Bee/Pipe chain (e.g. with {@link PipeBee#linkTo}) while still being able
 * to consume the produced elements as a plain BlockingQueue from any other
 * thread, for instance with a blocking {@code take()} in a consumer loop.
 *
 * @param <E> the type of elements held by the delegate queue
 */
public class QueueBee<E> extends Bee<E> implements BlockingQueue<E>
{
    private final BlockingQueue<E> delegate;

    public QueueBee(int threads, Hive hive, int queueSize, BlockingQueue<E> delegate)
    {
        super(threads, hive, queueSize);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public QueueBee(int threads, Hive hive, BlockingQueue<E> delegate)
    {
        super(threads, hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public QueueBee(Hive hive, BlockingQueue<E> delegate)
    {
        super(hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public QueueBee(int threads, BlockingQueue<E> delegate)
    {
        super(threads);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public QueueBee(BlockingQueue<E> delegate)
    {
        super();
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Receives a message and puts it into the delegate queue, blocking
     * the calling/worker thread if the queue is currently at capacity.
     *
     * @param e the message/element to insert into the delegate queue
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

    //=========================================================== Collection
    @Override
    public int size()
    {
        return delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return delegate.contains(o);
    }

    @Override
    public Iterator<E> iterator()
    {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(E e)
    {
        return delegate.add(e);
    }

    @Override
    public boolean remove(Object o)
    {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        return delegate.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return delegate.retainAll(c);
    }

    @Override
    public void clear()
    {
        delegate.clear();
    }

    //================================================================= Queue
    @Override
    public boolean offer(E e)
    {
        return delegate.offer(e);
    }

    @Override
    public E remove()
    {
        return delegate.remove();
    }

    @Override
    public E poll()
    {
        return delegate.poll();
    }

    @Override
    public E element()
    {
        return delegate.element();
    }

    @Override
    public E peek()
    {
        return delegate.peek();
    }

    //========================================================= BlockingQueue
    @Override
    public void put(E e) throws InterruptedException
    {
        delegate.put(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
    {
        return delegate.offer(e, timeout, unit);
    }

    @Override
    public E take() throws InterruptedException
    {
        return delegate.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException
    {
        return delegate.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity()
    {
        return delegate.remainingCapacity();
    }

    @Override
    public int drainTo(Collection<? super E> c)
    {
        return delegate.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements)
    {
        return delegate.drainTo(c, maxElements);
    }
}
