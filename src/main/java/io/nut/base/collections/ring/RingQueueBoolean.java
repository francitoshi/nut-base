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
public class RingQueueBoolean
{
    private final boolean [] buffer;
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
    public RingQueueBoolean(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be positive, but was: " + capacity);
        }
        this.capacity = capacity;
        this.mask = (capacity & (capacity - 1)) == 0 ? capacity - 1 : -1;
        this.buffer = new boolean[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public RingQueueBoolean(boolean[] data)
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
     * @return the element that was overwritten if the queue was full, otherwise false.
     */
    public boolean push(boolean value)
    {
        boolean removed = false;
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

    public void pushAll(boolean[] value)
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
     * Pushes multiple booleans from a byte array where each bit represents a
     * boolean. Bits are read from LSB to MSB of each byte.
     *
     * @param bytes the byte array containing packed boolean values.
     */
    public void push(byte[] bytes)
    {
        if (bytes == null)
        {
            return;
        }

        for (byte b : bytes)
        {
            for (int i = 0; i < 8; i++)
            {
                boolean value = (b & (1 << i)) != 0;
                push(value);
            }
        }
    }
    
    /**
     * Removes and returns the element at the head of the queue.
     *
     * @return the oldest element in the queue, or false if the queue is empty.
     */
    public boolean pop()
    {
        if (size == 0)
        {
            return false;
        }
        boolean value = buffer[head];
        head = wrap(head + 1);
        size--;
        return value;
    }
    /**
     * Pops multiple booleans and packs them into a byte array. Each byte
     * contains 8 booleans packed from LSB to MSB. The array size determines how
     * many booleans to pop (bytes.length * 8). If there aren't enough elements,
     * remaining bits are set to 0.
     *
     * @param bytes the byte array to fill with packed boolean values.
     * @return the same byte array filled with values.
     */
    public byte[] pop(byte[] bytes)
    {
        if (bytes == null)
        {
            return null;
        }

        for (int i = 0; i < bytes.length; i++)
        {
            byte b = 0;
            for (int j = 0; j < 8; j++)
            {
                boolean value = pop();
                if (value)
                {
                    b |= (1 << j);
                }
            }
            bytes[i] = b;
        }
        return bytes;
    }

    /**
     * Retrieves the element at the head of the queue without removing it.
     *
     * @return the oldest element in the queue, or false if the queue is empty.
     */
    public boolean peek()
    {
        if (size == 0)
        {
            return false;
        }
        return buffer[head];
    }

    /**
     * Retrieves multiple elements from the head of the queue without removing
     * them. Fills the provided array with elements starting from the head
     * (oldest). If the queue has fewer elements than the array size, remaining
     * positions are filled with false.
     *
     * @param buf the array to fill with queue elements.
     * @return the same array filled with values.
     */
    public boolean[] peek(boolean[] buf)
    {
        if (buf == null)
        {
            return null;
        }

        int elementsToRead = Math.min(buf.length, size);

        for (int i = 0; i < elementsToRead; i++)
        {
            buf[i] = buffer[wrap(head + i)];
        }

        // Fill remaining positions with false if queue has fewer elements
        for (int i = elementsToRead; i < buf.length; i++)
        {
            buf[i] = false;
        }

        return buf;
    }
    
    /**
     * Peeks at multiple booleans and packs them into a byte array without
     * removing them. Each byte contains 8 booleans packed from LSB to MSB. The
     * array size determines how many booleans to peek (bytes.length * 8). If
     * there aren't enough elements, remaining bits are set to 0.
     *
     * @param bytes the byte array to fill with packed boolean values.
     * @return the same byte array filled with values.
     */
    public byte[] peek(byte[] bytes)
    {
        if (bytes == null)
        {
            return null;
        }

        int bitsToRead = bytes.length * 8;
        int bitsAvailable = Math.min(bitsToRead, size);

        for (int i = 0; i < bytes.length; i++)
        {
            byte b = 0;
            for (int j = 0; j < 8; j++)
            {
                int bitPosition = i * 8 + j;
                if (bitPosition < bitsAvailable)
                {
                    boolean value = get(bitPosition);
                    if (value)
                    {
                        b |= (1 << j);
                    }
                }
            }
            bytes[i] = b;
        }
        return bytes;
    }    
    
    /**
     * Retrieves the element at a specific index relative to the head of the queue.
     * <p>
     * Index 0 corresponds to the head (oldest element).
     *
     * @param n the relative index of the element to retrieve.
     * @return the element at the specified index, or 0 if the index is out of bounds (n < 0 or n >= size).
     */
    public boolean get(int n)
    {
        if (n < 0 || n >= size)
        {
            return false;
        }
        return buffer[wrap(head + n)];
    }
    
    /**
     * Performs the given action for each element in the queue.
     * Elements are processed in order from head (oldest) to tail (newest).
     *
     * @param consumer the action to perform on each element.
     */
    public void foreach(Consumer<Boolean> consumer)
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
     * @return a new byte array containing the queue elements.
     */
    public boolean[] array()
    {
        boolean[] result = new boolean[size];
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
            total += buffer[wrap(head + i)] ? 1 : 0;
        }
        return total;
    }

    /**
     * Finds the minimum value currently in the queue.
     *
     * @return the smallest value, or false if the queue is empty.
     */
    public boolean min()
    {
        if (size == 0)
        {
            return false;
        }
        boolean minValue = buffer[head];
        for (int i = 1; i < size && minValue; i++)
        {
            minValue = buffer[wrap(head + i)];
        }
        return minValue;
    }

    /**
     * Finds the maximum value currently in the queue.
     *
     * @return the largest value, or false if the queue is empty.
     */
    public boolean max()
    {
        if (size == 0)
        {
            return false;
        }
        boolean maxValue = buffer[head];
        for (int i = 1; i < size && !maxValue; i++)
        {
            maxValue = buffer[wrap(head + i)];
        }
        return maxValue;
    }

    public static RingQueueBoolean getSynchronized(RingQueueBoolean queue)
    {
        return new RingQueueBoolean(1)
        {
            final Object lock = new Object();
            
            @Override
            public boolean push(boolean value)
            {
                synchronized(lock)
                {
                    return queue.push(value);
                }
            }

            @Override
            public void pushAll(boolean[] value)
            {
                synchronized(lock)
                {
                    queue.pushAll(value);
                }
            }

            @Override
            public boolean pop()
            {
                synchronized(lock)
                {
                    return queue.pop();
                }
            }

            @Override
            public byte[] peek(byte[] bytes)
            {
                synchronized(lock)
                {
                    return queue.peek(bytes);
                }
            }

            @Override
            public boolean[] peek(boolean[] buf)
            {
                synchronized(lock)
                {
                    return queue.peek(buf);
                }
            }

            @Override
            public boolean peek()
            {
                synchronized(lock)
                {
                    return queue.peek();
                }
            }

            @Override
            public byte[] pop(byte[] bytes)
            {
                synchronized(lock)
                {
                    return queue.pop(bytes);
                }
            }

            @Override
            public void push(byte[] bytes)
            {
                synchronized(lock)
                {
                    queue.push(bytes);
                }                
            }

            @Override
            public boolean get(int n)
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
            public boolean[] array()
            {
                synchronized(lock)
                {
                    return queue.array();
                }
            }

            @Override
            public void foreach(Consumer<Boolean> consumer)
            {
                synchronized(lock)
                {
                    queue.foreach(consumer);
                }
            }
            
            @Override
            public boolean max()
            {
                synchronized(lock)
                {
                    return queue.max();
                }
            }

            @Override
            public boolean min()
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
