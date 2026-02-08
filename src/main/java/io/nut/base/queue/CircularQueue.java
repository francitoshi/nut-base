/*
 *  CircularQueue.java
 *
 *  Copyright (c) 2025-2026 francitoshi@gmail.com
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
package io.nut.base.queue;

// Claude Sonnet 4.5

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A fixed-size circular queue (ring buffer) implementation for generic types.
 * <p>
 * This structure operates with a fixed capacity. When elements are pushed into a full queue,
 * the oldest element (head) is automatically removed/overwritten to make room for the new element.
 * <p>
 * This implementation is backed by an {@link ArrayList} acting as a fixed-size buffer.
 * <p>
 * <b>Note:</b> This implementation is not thread-safe.
 *
 * @param <E> the type of elements held in this queue.
 */
public class CircularQueue<E>
{
    private final List<E> buffer;
    private final int capacity;
    private int head;
    private int tail;
    private int size;

    /**
     * Constructs a new CircularQueue with the specified capacity.
     *
     * @param capacity the maximum number of elements the queue can hold.
     * @throws IllegalArgumentException if the capacity is less than or equal to 0.
     */
    public CircularQueue(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be positive, but was: " + capacity);
        }
        this.capacity = capacity;
        this.buffer = new ArrayList<>(capacity);
        // Initialize with nulls to fill the underlying array structure
        for (int i = 0; i < capacity; i++)
        {
            this.buffer.add(null);
        }
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public CircularQueue(E[] data)
    {
        Objects.requireNonNull(data, "data cannot be null");
        if (data.length <= 0)
        {
            throw new IllegalArgumentException("data cannot be empty");
        }
        this.capacity = data.length;
        this.buffer = Arrays.asList(data);
        this.head = 0;
        this.tail = 0;
        this.size = data.length;
    }

    /**
     * Adds a value to the end of the queue.
     * <p>
     * If the queue is currently at maximum capacity, the oldest element (at the head)
     * is overwritten/removed to accommodate the new value.
     *
     * @param value the element to add.
     * @return the element that was overwritten if the queue was full, otherwise {@code null}.
     */
    public E push(E value)
    {
        E removed = null;
        if (size == capacity)
        {
            removed = buffer.get(head);
            head = (head + 1) % capacity;
            size--;
        }
        buffer.set(tail, value);
        tail = (tail + 1) % capacity;
        size++;
        return removed;
    }

    public void pushAll(E[] value)
    {
        for(E v : value)
        {
            push(v);
        }
    }

    /**
     * Removes and returns the element at the head of the queue.
     *
     * @return the oldest element in the queue, or {@code null} if the queue is empty.
     */
    public E pop()
    {
        if (size == 0)
        {
            return null;
        }
        E value = buffer.get(head);
        buffer.set(head, null); // Helps GC
        head = (head + 1) % capacity;
        size--;
        return value;
    }
    
    /**
     * Retrieves the element at a specific index relative to the head of the queue.
     * <p>
     * Index 0 corresponds to the head (oldest element).
     *
     * @param n the relative index of the element to retrieve.
     * @return the element at the specified index, or {@code null} if the index is out of bounds (n < 0 or n >= size).
     */
    public E get(int n)
    {
        if (n < 0 || n >= size)
        {
            return null;
        }
        return buffer.get((head + n) % capacity);
    }

    /**
     * Performs the given action for each element in the queue.
     * Elements are processed in order from head (oldest) to tail (newest).
     *
     * @param consumer the action to perform on each element.
     */
    public void foreach(Consumer<E> consumer)
    {
        for (int i = 0; i < size; i++)
        {
            consumer.accept(buffer.get((head + i) % capacity));
        }
    }

    public List<E> list()
    {
        List<E> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            result.add(buffer.get((head + i) % capacity));
        }
        return result;
    }

    /**
     * Returns an array containing all of the elements in this queue in proper sequence
     * (from oldest to newest).
     * <p>
     * If the queue fits in the specified array (i.e., the array length equals the queue size),
     * it is returned therein. Otherwise, a new array is allocated with the runtime type of
     * the specified array and the exact size of this queue.
     *
     * @param a the array into which the elements of the queue are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose.
     * @return an array containing the elements of the queue.
     */
    public E[] array(E[] a)
    {
        if (a.length != size)
        {
            a = Arrays.copyOf(a, size);
        }
        for (int i = 0; i < size; i++)
        {
            a[i] = buffer.get((head + i) % capacity);
        }
        return a;
    }

    /**
     * Returns the number of elements currently in the queue.
     *
     * @return the current size.
     */
    public int size()
    {
        return size;
    }

    public boolean isEmpty()
    {
        return size==0;
    }

    /**
     * Finds the minimum value currently in the queue based on the natural ordering of the elements.
     *
     * @return the smallest element, or {@code null} if the queue is empty.
     * @throws ClassCastException if the elements in the queue are not {@link Comparable}.
     */
    @SuppressWarnings("unchecked")
    public E min()
    {
        if (size == 0)
        {
            return null;
        }

        E minValue = buffer.get(head);
        Comparable<E> comparable = (Comparable<E>) minValue;

        for (int i = 1; i < size; i++)
        {
            E value = buffer.get((head + i) % capacity);
            if (comparable.compareTo(value) > 0)
            {
                minValue = value;
                comparable = (Comparable<E>) minValue;
            }
        }
        return minValue;
    }

    /**
     * Finds the minimum value currently in the queue using the provided {@link Comparator}.
     *
     * @param comparator the comparator to determine the order of the queue.
     * @return the smallest element, or {@code null} if the queue is empty.
     */
    public E min(Comparator<E> comparator)
    {
        if (size == 0)
        {
            return null;
        }

        E minValue = buffer.get(head);
        for (int i = 1; i < size; i++)
        {
            E value = buffer.get((head + i) % capacity);
            if (comparator.compare(value, minValue) < 0)
            {
                minValue = value;
            }
        }
        return minValue;
    }

    /**
     * Finds the maximum value currently in the queue based on the natural ordering of the elements.
     *
     * @return the largest element, or {@code null} if the queue is empty.
     * @throws ClassCastException if the elements in the queue are not {@link Comparable}.
     */
    @SuppressWarnings("unchecked")
    public E max()
    {
        if (size == 0)
        {
            return null;
        }

        E maxValue = buffer.get(head);
        Comparable<E> comparable = (Comparable<E>) maxValue;

        for (int i = 1; i < size; i++)
        {
            E value = buffer.get((head + i) % capacity);
            if (comparable.compareTo(value) < 0)
            {
                maxValue = value;
                comparable = (Comparable<E>) maxValue;
            }
        }
        return maxValue;
    }

    /**
     * Finds the maximum value currently in the queue using the provided {@link Comparator}.
     *
     * @param comparator the comparator to determine the order of the queue.
     * @return the largest element, or {@code null} if the queue is empty.
     */
    public E max(Comparator<E> comparator)
    {
        if (size == 0)
        {
            return null;
        }

        E maxValue = buffer.get(head);
        for (int i = 1; i < size; i++)
        {
            E value = buffer.get((head + i) % capacity);
            if (comparator.compare(value, maxValue) > 0)
            {
                maxValue = value;
            }
        }
        return maxValue;
    }
    
    public static <E> CircularQueue<E> getSynchronized(CircularQueue<E> queue)
    {
        return new CircularQueue<E>(queue.capacity)
        {
            final Object lock = new Object();

            @Override
            public E push(E value)
            {
                synchronized(lock)
                {
                    return super.push(value);
                }
            }
            
            @Override
            public void pushAll(E[] value)
            {
                synchronized(lock)
                {
                    super.pushAll(value);
                }
            }
            
            @Override
            public E pop()
            {
                synchronized(lock)
                {
                    return super.pop();
                }
            }

            @Override
            public List<E> list()
            {
                synchronized(lock)
                {
                    return super.list();
                }
            }

            @Override
            public E get(int n)
            {
                synchronized(lock)
                {
                    return super.get(n);
                }
            }

            @Override
            public int size()
            {
                synchronized(lock)
                {
                    return super.size();
                }
            }
            
            @Override
            public boolean isEmpty()
            {
                synchronized(lock)
                {
                    return super.isEmpty();
                }
            }
            
            @Override
            public E[] array(E[] e)
            {
                synchronized(lock)
                {
                    return super.array(e);
                }
            }

            @Override
            public void foreach(Consumer<E> consumer)
            {
                synchronized(lock)
                {
                    super.foreach(consumer);
                }
            }

            @Override
            public E max()
            {
                synchronized(lock)
                {
                    return super.max();
                }
            }

            @Override
            public E max(Comparator<E> comparator)
            {
                synchronized(lock)
                {
                    return super.max(comparator);
                }
            }

            @Override
            public E min()
            {
                synchronized(lock)
                {
                    return super.min();
                }
            }

            @Override
            public E min(Comparator<E> comparator)
            {
                synchronized(lock)
                {
                    return super.min(comparator);
                }
            }
        };
    }
}
