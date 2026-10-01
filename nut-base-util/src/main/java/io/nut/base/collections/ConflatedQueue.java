/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe, capacity-1 {@link BlockingQueue} that functions like a Kotlin conflated channel.
 * Insertion operations never block. If the queue is already full, new elements overwrite
 * the existing element (conflating the queue).
 *
 * <p>Retrieval operations block when the queue is empty until an element is available.</p>
 *
 * @param <E> the type of elements held in this queue
 * @author franci
 * @since 1.8
 */
public class ConflatedQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>
{
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private E value;

    /**
     * Constructs an empty ConflatedQueue.
     */
    public ConflatedQueue()
    {
    }

    /**
     * Inserts the specified element into this queue.
     * Since this queue is conflated, this operation never blocks and always returns true,
     * overwriting any existing element.
     *
     * @param e the element to add; must not be null
     * @return {@code true}
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean offer(E e)
    {
        Objects.requireNonNull(e, "element must not be null");
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            value = e;
            notEmpty.signal();
            return true;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Inserts the specified element into this queue, overwriting any existing element.
     * Since this queue is conflated, this operation never blocks.
     *
     * @param e the element to add; must not be null
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public void put(E e)
    {
        offer(e);
    }

    /**
     * Inserts the specified element into this queue.
     * Since this queue is conflated, this operation never blocks, ignores the timeout,
     * and always returns true, overwriting any existing element.
     *
     * @param e the element to add; must not be null
     * @param timeout ignored
     * @param unit ignored
     * @return {@code true}
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit)
    {
        return offer(e);
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public E take() throws InterruptedException
    {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try
        {
            while (value == null)
            {
                notEmpty.await();
            }
            E result = value;
            value = null;
            return result;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return the head of this queue, or {@code null} if the specified waiting time elapses
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException
    {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try
        {
            while (value == null)
            {
                if (nanos <= 0)
                {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            E result = value;
            value = null;
            return result;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes the head of this queue, or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    @Override
    public E poll()
    {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            E result = value;
            value = null;
            return result;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Retrieves, but does not remove, the head of this queue, or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    @Override
    public E peek()
    {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            return value;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Returns the number of elements in this queue (either 0 or 1).
     *
     * @return the size of this queue
     */
    @Override
    public int size()
    {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            return value == null ? 0 : 1;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Returns {@code true} if this queue contains no elements.
     *
     * @return {@code true} if this queue is empty
     */
    @Override
    public boolean isEmpty()
    {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            return value == null;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Returns the remaining capacity of this queue.
     * Returns 1 if empty, or 0 if full.
     *
     * @return the remaining capacity
     */
    @Override
    public int remainingCapacity()
    {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            return value == null ? 1 : 0;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Removes all elements from this queue.
     */
    @Override
    public void clear()
    {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            value = null;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Returns {@code true} if this queue contains the specified element.
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     */
    @Override
    public boolean contains(Object o)
    {
        if (o == null)
        {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            return o.equals(value);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Removes a single instance of the specified element from this queue, if it is present.
     *
     * @param o object to be removed from this queue, if present
     * @return {@code true} if an element was removed as a result of this call
     */
    @Override
    public boolean remove(Object o)
    {
        if (o == null)
        {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            if (o.equals(value))
            {
                value = null;
                return true;
            }
            return false;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Removes all available elements from this queue and adds them to the given collection.
     *
     * @param c the collection to transfer elements into
     * @return the number of elements transferred
     * @throws NullPointerException if the specified collection is null
     * @throws IllegalArgumentException if the specified collection is this queue
     */
    @Override
    public int drainTo(Collection<? super E> c)
    {
        Objects.requireNonNull(c, "collection must not be null");
        if (c == this)
        {
            throw new IllegalArgumentException("cannot drain to self");
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            if (value != null)
            {
                c.add(value);
                value = null;
                return 1;
            }
            return 0;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Removes at most the given number of available elements from this queue and adds them to the given collection.
     *
     * @param c the collection to transfer elements into
     * @param maxElements the maximum number of elements to transfer
     * @return the number of elements transferred
     * @throws NullPointerException if the specified collection is null
     * @throws IllegalArgumentException if the specified collection is this queue
     */
    @Override
    public int drainTo(Collection<? super E> c, int maxElements)
    {
        Objects.requireNonNull(c, "collection must not be null");
        if (c == this)
        {
            throw new IllegalArgumentException("cannot drain to self");
        }
        if (maxElements <= 0)
        {
            return 0;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            if (value != null)
            {
                c.add(value);
                value = null;
                return 1;
            }
            return 0;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Returns an iterator over the elements in this queue.
     * The elements will be returned in order (which consists of at most one element).
     * The iterator is snapshot-based and will not reflect changes to the queue after its creation.
     *
     * @return an iterator over the elements in this queue
     */
    @Override
    public Iterator<E> iterator()
    {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try
        {
            final E snapshot = value;
            return new Iterator<E>()
            {
                private boolean hasNext = (snapshot != null);

                @Override
                public boolean hasNext()
                {
                    return hasNext;
                }

                @Override
                public E next()
                {
                    if (!hasNext)
                    {
                        throw new NoSuchElementException();
                    }
                    hasNext = false;
                    return snapshot;
                }

                @Override
                public void remove()
                {
                    if (snapshot == null)
                    {
                        throw new IllegalStateException();
                    }
                    ConflatedQueue.this.remove(snapshot);
                }
            };
        }
        finally
        {
            lock.unlock();
        }
    }
}
