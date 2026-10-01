/*
 *  DuplexQueue.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A {@link Queue} that separates read and write operations across two underlying queues.
 * All retrieval operations are delegated to the {@code read} queue and all insertion
 * operations to the {@code write} queue, enabling unidirectional data flow between
 * two independent queues through a single interface.
 *
 * @param <E> the type of elements held in this queue
 */
public class DuplexQueue<E> implements Queue<E>
{
    private final Queue<E> read;
    private final Queue<E> write;

    /**
     * Constructs a {@code DuplexQueue} backed by the given read and write queues.
     *
     * @param read  the queue used for all retrieval operations; must not be {@code null}
     * @param write the queue used for all insertion operations; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public DuplexQueue(Queue<E> read, Queue<E> write)
    {
        this.read = Objects.requireNonNull(read, "read");
        this.write = Objects.requireNonNull(write, "write");
    }

    /** Removes and returns the head of the read queue. */
    @Override
    public E remove()
    {
        return read.remove();
    }

    /** Retrieves and removes the head of the read queue, or returns {@code null} if empty. */
    @Override
    public E poll()
    {
        return read.poll();
    }

    /** Retrieves, without removing, the head of the read queue. */
    @Override
    public E element()
    {
        return read.element();
    }

    /** Retrieves, without removing, the head of the read queue, or returns {@code null} if empty. */
    @Override
    public E peek()
    {
        return read.peek();
    }

    /** Inserts the element into the write queue, throwing on capacity violation. */
    @Override
    public boolean add(E e)
    {
        return write.add(e);
    }

    /** Inserts the element into the write queue, returning {@code false} on capacity violation. */
    @Override
    public boolean offer(E e)
    {
        return write.offer(e);
    }

    /** Returns the number of elements in the read queue. */
    @Override
    public int size()
    {
        return read.size();
    }

    /** Returns {@code true} if the read queue contains no elements. */
    @Override
    public boolean isEmpty()
    {
        return read.isEmpty();
    }

    /** Returns {@code true} if the read queue contains the specified element. */
    @Override
    public boolean contains(Object o)
    {
        return read.contains(o);
    }

    /** Returns an iterator over the elements in the read queue. */
    @Override
    public Iterator<E> iterator()
    {
        return read.iterator();
    }

    /**
     * Adds all elements of the given collection to the read queue.
     *
     * @param clctn the collection whose elements are to be added
     * @return {@code true} if the read queue changed as a result
     */
    @Override
    public boolean addAll(Collection<? extends E> clctn)
    {
        return read.addAll(clctn);
    }

    /** Returns an array containing all elements in the read queue. */
    @Override
    public Object[] toArray()
    {
        return read.toArray();
    }

    /** Returns an array containing all elements in the read queue, using the provided array. */
    @Override
    public <T> T[] toArray(T[] ts)
    {
        return read.toArray(ts);
    }

    /** Removes a single instance of the specified element from the read queue. */
    @Override
    public boolean remove(Object o)
    {
        return read.remove(o);
    }

    /** Returns {@code true} if the read queue contains all elements in the given collection. */
    @Override
    public boolean containsAll(Collection<?> clctn)
    {
        return read.containsAll(clctn);
    }

    /** Removes from the read queue all elements contained in the given collection. */
    @Override
    public boolean removeAll(Collection<?> clctn)
    {
        return read.removeAll(clctn);
    }

    /** Removes from the read queue all elements that satisfy the given predicate. */
    @Override
    public boolean removeIf(Predicate<? super E> prdct)
    {
        return read.removeIf(prdct);
    }

    /** Retains only the elements in the read queue that are contained in the given collection. */
    @Override
    public boolean retainAll(Collection<?> clctn)
    {
        return read.retainAll(clctn);
    }

    /** Removes all elements from the read queue. */
    @Override
    public void clear()
    {
        read.clear();
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.read);
        hash = 97 * hash + Objects.hashCode(this.write);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final DuplexQueue<?> other = (DuplexQueue<?>) obj;
        if (!Objects.equals(this.read, other.read))
        {
            return false;
        }
        return Objects.equals(this.write, other.write);
    }

    /** Returns a {@link Spliterator} over the elements in the read queue. */
    @Override
    public Spliterator<E> spliterator()
    {
        return read.spliterator();
    }

    /** Returns a sequential {@link Stream} over the elements in the read queue. */
    @Override
    public Stream<E> stream()
    {
        return read.stream();
    }

    /** Returns a parallel {@link Stream} over the elements in the read queue. */
    @Override
    public Stream<E> parallelStream()
    {
        return read.parallelStream();
    }
}
