/*
 *  CircularQueueByte.java
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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A fixed-size circular queue (ring buffer) implementation for {@code byte} primitives.
 * <p>
 * This structure operates with a fixed capacity. When elements are pushed into a full queue,
 * the oldest element (head) is automatically removed/overwritten to make room for the new element.
 * <p>
 * <b>Note:</b> This implementation is not thread-safe.
 */
public class CircularQueueBoolean
{
    private final boolean [] buffer;
    private final int capacity;
    private int head;
    private int tail;
    private int size;

    /**
     * Constructs a new CircularQueueByte with the specified capacity.
     *
     * @param capacity the maximum number of elements the queue can hold.
     * @throws IllegalArgumentException if the capacity is less than or equal to 0.
     */
    public CircularQueueBoolean(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be positive, but was: " + capacity);
        }
        this.capacity = capacity;
        this.buffer = new boolean[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public CircularQueueBoolean(boolean[] data)
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
            head = (head + 1) % capacity;
            size--;
        }
        buffer[tail] = value;
        tail = (tail + 1) % capacity;
        size++;
        return removed;
    }

    public void pushAll(boolean[] value)
    {
        for(boolean v : value)
        {
            push(v);
        }
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
        head = (head + 1) % capacity;
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
            buf[i] = buffer[(head + i) % capacity];
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
        return buffer[(head + n) % capacity];
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
            consumer.accept(buffer[(head + i) % capacity]);
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
            total += buffer[(head + i) % capacity] ? 1 : 0;
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
            minValue = buffer[(head + i) % capacity];
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
            maxValue = buffer[(head + i) % capacity];
        }
        return maxValue;
    }

    public static CircularQueueBoolean getSynchronized(CircularQueueBoolean queue)
    {
        return new CircularQueueBoolean(queue.capacity)
        {
            final Object lock = new Object();
            
            @Override
            public boolean push(boolean value)
            {
                synchronized(lock)
                {
                    return super.push(value);
                }
            }

            @Override
            public void pushAll(boolean[] value)
            {
                synchronized(lock)
                {
                    super.pushAll(value);
                }
            }

            @Override
            public boolean pop()
            {
                synchronized(lock)
                {
                    return super.pop();
                }
            }

            @Override
            public boolean get(int n)
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
            public boolean[] array()
            {
                synchronized(lock)
                {
                    return super.array();
                }
            }

            @Override
            public void foreach(Consumer<Boolean> consumer)
            {
                synchronized(lock)
                {
                    super.foreach(consumer);
                }
            }
            
            @Override
            public boolean max()
            {
                synchronized(lock)
                {
                    return super.max();
                }
            }

            @Override
            public boolean min()
            {
                synchronized(lock)
                {
                    return super.min();
                }
            }

            @Override
            public long sum()
            {
                synchronized(lock)
                {
                    return super.sum();
                }
            }

            @Override
            public double average()
            {
                synchronized(lock)
                {
                    return super.average();
                }
            }

        };
    }
}
