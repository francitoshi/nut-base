/*
 *  CircularQueueBigDecimal.java
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A fixed-size circular queue (ring buffer) implementation for {@code BigDecimal} objects.
 * <p>
 * This structure operates with a fixed capacity. When elements are pushed into a full queue,
 * the oldest element (head) is automatically removed/overwritten to make room for the new element.
 * <p>
 * <b>Note:</b> This implementation is not thread-safe.
 */
public class CircularQueueBigDecimal
{
    private final BigDecimal[] buffer;
    private final int capacity;
    private int head;
    private int tail;
    private int size;

    /**
     * Constructs a new CircularQueueBigDecimal with the specified capacity.
     *
     * @param capacity the maximum number of elements the queue can hold.
     * @throws IllegalArgumentException if the capacity is less than or equal to 0.
     */
    public CircularQueueBigDecimal(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be positive, but was: " + capacity);
        }
        this.capacity = capacity;
        this.buffer = new BigDecimal[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public CircularQueueBigDecimal(BigDecimal[] data)
    {
        Objects.requireNonNull(data, "data cannot be null");
        if (data.length <= 0)
        {
            throw new IllegalArgumentException("data cannot be empty");
        }
        this.capacity = data.length;
        this.buffer = data.clone();
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
    public BigDecimal push(BigDecimal value)
    {
        BigDecimal removed = null;
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

    public void pushAll(BigDecimal[] value)
    {
        for(BigDecimal v : value)
        {
            push(v);
        }
    }

    /**
     * Removes and returns the element at the head of the queue.
     *
     * @return the oldest element in the queue, or {@code null} if the queue is empty.
     */
    public BigDecimal pop()
    {
        if (size == 0)
        {
            return null;
        }
        BigDecimal value = buffer[head];
        head = (head + 1) % capacity;
        size--;
        return value;
    }

    /**
     * Retrieves the element at the head of the queue without removing it.
     *
     * @return the oldest element in the queue, or null if the queue is empty.
     */
    public BigDecimal peek()
    {
        if (size == 0)
        {
            return null;
        }
        return buffer[head];
    }
    
    /**
     * Retrieves the element at a specific index relative to the head of the queue.
     * <p>
     * Index 0 corresponds to the head (oldest element).
     *
     * @param n the relative index of the element to retrieve.
     * @return the element at the specified index, or {@code null} if the index is out of bounds (n < 0 or n >= size).
     */
    public BigDecimal get(int n)
    {
        if (n < 0 || n >= size)
        {
            return null;
        }
        return buffer[(head + n) % capacity];
    }

    /**
     * Performs the given action for each element in the queue. 
     * Elements are processed in order from head (oldest) to tail (newest).
     *
     * @param consumer the action to perform on each element.
     */
    public void foreach(Consumer<BigDecimal> consumer)
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
     * @return a new BigDecimal array containing the queue elements.
     */
    public BigDecimal[] array()
    {
        BigDecimal[] result = new BigDecimal[size];
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

    public boolean isEmpty()
    {
        return size==0;
    }

    /**
     * Finds the minimum value currently in the queue.
     *
     * @return the smallest value, or {@code null} if the queue is empty.
     */
    public BigDecimal min()
    {
        if (size == 0)
        {
            return null;
        }
        BigDecimal minValue = buffer[head];
        for (int i = 1; i < size; i++)
        {
            BigDecimal value = buffer[(head + i) % capacity];
            if (value.compareTo(minValue) < 0)
            {
                minValue = value;
            }
        }
        return minValue;
    }

    /**
     * Finds the maximum value currently in the queue.
     *
     * @return the largest value, or {@code null} if the queue is empty.
     */
    public BigDecimal max()
    {
        if (size == 0)
        {
            return null;
        }
        BigDecimal maxValue = buffer[head];
        for (int i = 1; i < size; i++)
        {
            BigDecimal value = buffer[(head + i) % capacity];
            if (value.compareTo(maxValue) > 0)
            {
                maxValue = value;
            }
        }
        return maxValue;
    }

    /**
     * Calculates the arithmetic mean of the values in the queue.
     * <p>
     * The calculation is performed with a scale of <b>10</b> and uses
     * {@link RoundingMode#HALF_UP} for division.
     *
     * @return the average of the elements, or {@code BigDecimal.ZERO} if the
     * queue is empty.
     */
    public BigDecimal average()
    {
        if (size == 0)
        {
            return BigDecimal.ZERO;
        }
        return sum().divide(BigDecimal.valueOf(size), 10, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the sum of all values in the queue.
     *
     * @return the sum of all elements.
     */
    public BigDecimal sum()
    {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < size; i++)
        {
            total = total.add(buffer[(head + i) % capacity]);
        }
        return total;
    }

    
    public static CircularQueueBigDecimal getSynchronized(CircularQueueBigDecimal queue)
    {
        return new CircularQueueBigDecimal(queue.capacity)
        {
            final Object lock = new Object();

            @Override
            public BigDecimal push(BigDecimal value)
            {
                synchronized(lock)
                {
                    return super.push(value);
                }
            }

            @Override
            public void pushAll(BigDecimal[] value)
            {
                synchronized(lock)
                {
                    super.pushAll(value);
                }
            }

            @Override
            public BigDecimal pop()
            {
                synchronized(lock)
                {
                    return super.pop();
                }
            }

            @Override
            public BigDecimal get(int n)
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
            public BigDecimal[] array()
            {
                synchronized(lock)
                {
                    return super.array();
                }
            }

            @Override
            public void foreach(Consumer<BigDecimal> consumer)
            {
                synchronized(lock)
                {
                    super.foreach(consumer);
                }
            }

            @Override
            public BigDecimal max()
            {
                synchronized(lock)
                {
                    return super.max();
                }
            }

            @Override
            public BigDecimal min()
            {
                synchronized(lock)
                {
                    return super.min();
                }
            }

            @Override
            public BigDecimal sum()
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

        };
    }    
}
