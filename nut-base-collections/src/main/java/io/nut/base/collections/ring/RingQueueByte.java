/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.ring;

// Claude Sonnet 4.5

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A fixed-size circular queue (ring buffer) implementation for {@code byte} primitives.
 * <p>
 * This structure operates with a fixed capacity. When elements are pushed into a full queue,
 * the oldest element (head) is automatically removed/overwritten to make room for the new element.
 * <p>
 * <b>Performance:</b> when the capacity is a power of two, index arithmetic uses a
 * bitmask ({@code index & (capacity - 1)}) instead of the modulo operator, which is
 * significantly faster for push/pop and for traversals. For arbitrary capacities a
 * modulo is used instead, so no benefit is obtained.
 * <p>
 * <b>Note:</b> This implementation is not thread-safe.
 */
public class RingQueueByte
{
    private final byte[] buffer;
    private final int capacity;
    private final int mask;
    private int head;
    private int tail;
    private int size;

    /**
     * Constructs a new RingQueueByte with the specified capacity.
     *
     * @param capacity the maximum number of elements the queue can hold. When it is a
     *                power of two, operations use a bitmask instead of a modulo,
     *                which is faster.
     * @throws IllegalArgumentException if the capacity is less than or equal to 0.
     */
    public RingQueueByte(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be positive, but was: " + capacity);
        }
        this.capacity = capacity;
        this.mask = (capacity & (capacity - 1)) == 0 ? capacity - 1 : -1;
        this.buffer = new byte[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public RingQueueByte(byte[] data)
    {
        Objects.requireNonNull(data, "data cannot be null");
        if (data.length <= 0)
        {
            throw new IllegalArgumentException("data cannot be empty");
        }
        this.capacity = data.length;
        this.mask = (capacity & (capacity - 1)) == 0 ? capacity - 1 : -1;
        this.buffer = data.clone();
        this.head = 0;
        this.tail = 0;
        this.size = data.length;
    }

    /**
     * Wraps an index around the buffer, using a bitmask when the capacity is a
     * power of two and a modulo otherwise.
     */
    private int wrap(int index)
    {
        return mask >= 0 ? index & mask : index % capacity;
    }
    
    /**
     * Adds a new element to the end of the queue.
     * <p>
     * If the queue is currently at maximum capacity, the oldest element (at the head)
     * is overwritten/removed to accommodate the new value.
     *
     * @param value the element to add.
     * @return the element that was overwritten if the queue was full, otherwise 0.
     */
    public byte push(byte value)
    {
        byte removed = 0;
        if (size == capacity)
        {
            removed = buffer[head];
            head = wrap(head + 1);
            size--;
        }
        buffer[tail] = value;
        tail = wrap(tail + 1);
        size++;
        return removed;
    }

    public void pushAll(byte[] value)
    {
        int n = value.length;
        if (n <= 0)
        {
            return;
        }
        int newSize = size + n;
        int dropped = 0;
        if (newSize > capacity)
        {
            dropped = newSize - capacity;
            newSize = capacity;
        }
        int writePos = tail;
        if (writePos + n <= capacity)
        {
            System.arraycopy(value, 0, buffer, writePos, n);
        }
        else
        {
            int first = capacity - writePos;
            System.arraycopy(value, 0, buffer, writePos, first);
            System.arraycopy(value, first, buffer, 0, n - first);
        }
        head = wrap(head + dropped);
        tail = wrap(writePos + n - dropped);
        size = newSize;
    }

    /**
     * Removes and returns the element at the head of the queue.
     *
     * @return the oldest element in the queue, or 0 if the queue is empty.
     */
    public byte pop()
    {
        if (size == 0)
        {
            return 0;
        }
        byte value = buffer[head];
        head = wrap(head + 1);
        size--;
        return value;
    }

    /**
     * Retrieves the element at the head of the queue without removing it.
     *
     * @return the oldest element in the queue, or 0 if the queue is empty.
     */
    public byte peek()
    {
        if (size == 0)
        {
            return 0;
        }
        return buffer[head];
    }
    
    /**
     * Retrieves the element at a specific index relative to the head of the queue.
     * <p>
     * Index 0 corresponds to the head (oldest element).
     *
     * @param n the relative index of the element to retrieve.
     * @return the element at the specified index, or 0 if the index is out of bounds (n < 0 or n >= size).
     */
    public byte get(int n)
    {
        if (n < 0 || n >= size)
        {
            return 0;
        }
        return buffer[wrap(head + n)];
    }

    /**
     * Performs the given action for each element in the queue.
     * Elements are processed in order from head (oldest) to tail (newest).
     *
     * @param consumer the action to perform on each element.
     */
    public void foreach(Consumer<Byte> consumer)
    {
        for (int i = 0; i < size; i++)
        {
            consumer.accept(buffer[wrap(head + i)]);
        }
    }

    /**
     * Returns a copy of the current queue elements as an array.
     * The array is ordered from head (oldest) to tail (newest).
     *
     * @return a new boolean array containing the queue elements.
     */
    public byte[] array()
    {
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++)
        {
            result[i] = buffer[wrap(head + i)];
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
     * Calculates the arithmetic mean of the values in the queue.
     *
     * @return the average of the elements, or 0.0 if the queue is empty.
     */
    public double average()
    {
        if (size == 0)
        {
            return 0;
        }
        return (double) sum() / size;
    }

    /**
     * Calculates the sum of all values in the queue.
     * The result is returned as a {@code long} to prevent overflow.
     *
     * @return the sum of all elements.
     */
    public long sum()
    {
        long total = 0;
        for (int i = 0; i < size; i++)
        {
            total += buffer[wrap(head + i)];
        }
        return total;
    }

    /**
     * Finds the minimum value currently in the queue.
     *
     * @return the smallest value, or 0 if the queue is empty.
     */
    public byte min()
    {
        if (size == 0)
        {
            return 0;
        }
        byte minValue = buffer[head];
        for (int i = 1; i < size; i++)
        {
            byte value = buffer[wrap(head + i)];
            if (value < minValue)
            {
                minValue = value;
            }
        }
        return minValue;
    }

    /**
     * Finds the maximum value currently in the queue.
     *
     * @return the largest value, or 0 if the queue is empty.
     */
    public byte max()
    {
        if (size == 0)
        {
            return 0;
        }
        byte maxValue = buffer[head];
        for (int i = 1; i < size; i++)
        {
            byte value = buffer[wrap(head + i)];
            if (value > maxValue)
            {
                maxValue = value;
            }
        }
        return maxValue;
    }

    public static RingQueueByte getSynchronized(RingQueueByte queue)
    {
        return new RingQueueByte(1)
        {
            final Object lock = new Object();
            
            @Override
            public byte push(byte value)
            {
                synchronized(lock)
                {
                    return queue.push(value);
                }
            }

            @Override
            public void pushAll(byte[] value)
            {
                synchronized(lock)
                {
                    queue.pushAll(value);
                }
            }

            @Override
            public byte pop()
            {
                synchronized(lock)
                {
                    return queue.pop();
                }
            }

            @Override
            public byte peek()
            {
                synchronized(lock)
                {
                    return queue.peek();
                }
            }

            @Override
            public byte get(int n)
            {
                synchronized(lock)
                {
                    return queue.get(n);
                }
            }

            @Override
            public int size()
            {
                synchronized(lock)
                {
                    return queue.size();
                }
            }

            @Override
            public boolean isEmpty()
            {
                synchronized(lock)
                {
                    return queue.isEmpty();
                }
            }

            @Override
            public byte[] array()
            {
                synchronized(lock)
                {
                    return queue.array();
                }
            }

            @Override
            public void foreach(Consumer<Byte> consumer)
            {
                synchronized(lock)
                {
                    queue.foreach(consumer);
                }
            }

            @Override
            public byte max()
            {
                synchronized(lock)
                {
                    return queue.max();
                }
            }

            @Override
            public byte min()
            {
                synchronized(lock)
                {
                    return queue.min();
                }
            }

            @Override
            public long sum()
            {
                synchronized(lock)
                {
                    return queue.sum();
                }
            }

            @Override
            public double average()
            {
                synchronized(lock)
                {
                    return queue.average();
                }
            }

        };
    }
}
