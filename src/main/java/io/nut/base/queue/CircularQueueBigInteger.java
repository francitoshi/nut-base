/*
 *  CircularQueueBigInteger.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Consumer;

// Claude Sonnet 4.5

/**
 * A fixed-size circular queue (ring buffer) implementation for {@code BigInteger} objects.
 * <p>
 * This structure operates with a fixed capacity. When elements are pushed into a full queue,
 * the oldest element (head) is automatically removed/overwritten to make room for the new element.
 * <p>
 * <b>Note:</b> This implementation is not thread-safe.
 */
public class CircularQueueBigInteger
{
    private final BigInteger[] buffer;
    private final int capacity;
    private int head;
    private int tail;
    private int size;

    /**
     * Constructs a new CircularQueueBigInteger with the specified capacity.
     *
     * @param capacity the maximum number of elements the queue can hold.
     * @throws IllegalArgumentException if the capacity is less than or equal to 0.
     */
    public CircularQueueBigInteger(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be positive, but was: " + capacity);
        }
        this.capacity = capacity;
        this.buffer = new BigInteger[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    /**
     * Adds a value to the end of the queue.
     * <p>
     * If the queue is currently at maximum capacity, the oldest element (at the head)
     * is overwritten/removed to accommodate the new value.
     *
     * @param value the BigInteger value to add.
     * @return the value that was overwritten if the queue was full, otherwise {@code null}.
     */
    public BigInteger push(BigInteger value)
    {
        BigInteger removed = null;
        if (size == capacity)
        {
            removed = buffer[head];
            head = (head + 1) % capacity;
            size--;
        }
        buffer[tail] = value;
        tail = (tail + 1) % capacity;
        size++;
        return removed;
    }

    /**
     * Removes and returns the element at the head of the queue.
     *
     * @return the oldest element in the queue, or {@code null} if the queue is empty.
     */
    public BigInteger pop()
    {
        if (size == 0)
        {
            return null;
        }
        BigInteger value = buffer[head];
        head = (head + 1) % capacity;
        size--;
        return value;
    }

    /**
     * Performs the given action for each element in the queue.
     * Elements are processed in order from head (oldest) to tail (newest).
     *
     * @param consumer the action to perform on each element.
     */
    public void foreach(Consumer<BigInteger> consumer)
    {
        for (int i = 0; i < size; i++)
        {
            consumer.accept(buffer[(head + i) % capacity]);
        }
    }

    /**
     * Returns a copy of the current queue elements as an array.
     * The array is ordered from head (oldest) to tail (newest).
     *
     * @return a new BigInteger array containing the queue elements.
     */
    public BigInteger[] array()
    {
        BigInteger[] result = new BigInteger[size];
        for (int i = 0; i < size; i++)
        {
            result[i] = buffer[(head + i) % capacity];
        }
        return result;
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

    /**
     * Calculates the arithmetic mean of the values in the queue.
     * <p>
     * <b>Note:</b> Since {@code BigInteger} supports only integer arithmetic,
     * this method performs integer division. Any fractional part of the average
     * is discarded (truncated).
     *
     * @return the average of the elements, or {@code BigInteger.ZERO} if the queue is empty.
     */
    public BigDecimal average()
    {
        if (size == 0)
        {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(sum()).divide(BigDecimal.valueOf(size));
    }

    /**
     * Calculates the sum of all values in the queue.
     *
     * @return the sum of all elements.
     */
    public BigInteger sum()
    {
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < size; i++)
        {
            total = total.add(buffer[(head + i) % capacity]);
        }
        return total;
    }

    /**
     * Finds the minimum value currently in the queue.
     *
     * @return the smallest value, or {@code null} if the queue is empty.
     */
    public BigInteger min()
    {
        if (size == 0)
        {
            return null;
        }
        BigInteger minValue = buffer[head];
        for (int i = 1; i < size; i++)
        {
            BigInteger value = buffer[(head + i) % capacity];
            if (value.compareTo(minValue) < 0)
            {
                minValue = value;
            }
        }
        return minValue;
    }

    public BigInteger max()
    {
        if (size == 0)
        {
            return null;
        }
        BigInteger maxValue = buffer[head];
        for (int i = 1; i < size; i++)
        {
            BigInteger value = buffer[(head + i) % capacity];
            if (value.compareTo(maxValue) > 0)
            {
                maxValue = value;
            }
        }
        return maxValue;
    }

    public BigInteger get(int n)
    {
        if (n < 0 || n >= size)
        {
            return null;
        }
        return buffer[(head + n) % capacity];
    }
    
    public static CircularQueueBigInteger getSynchronized(CircularQueueBigInteger queue)
    {
        return new CircularQueueBigInteger(queue.capacity)
        {
            final Object lock = new Object();
            @Override
            public BigInteger get(int n)
            {
                synchronized(lock)
                {
                    return super.get(n);
                }
            }

            @Override
            public BigInteger max()
            {
                synchronized(lock)
                {
                    return super.max();
                }
            }

            @Override
            public BigInteger min()
            {
                synchronized(lock)
                {
                    return super.min();
                }
            }

            @Override
            public BigInteger sum()
            {
                synchronized(lock)
                {
                    return super.sum();
                }
            }

            @Override
            public BigDecimal average()
            {
                synchronized(lock)
                {
                    return super.average();
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
            public BigInteger[] array()
            {
                synchronized(lock)
                {
                    return super.array();
                }
            }

            @Override
            public void foreach(Consumer<BigInteger> consumer)
            {
                synchronized(lock)
                {
                    super.foreach(consumer);
                }
            }

            @Override
            public BigInteger pop()
            {
                synchronized(lock)
                {
                    return super.pop();
                }
            }

            @Override
            public BigInteger push(BigInteger value)
            {
                synchronized(lock)
                {
                    return super.push(value);
                }
            }
            
        };
    }
}
