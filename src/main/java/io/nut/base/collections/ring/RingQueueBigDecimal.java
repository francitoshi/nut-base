/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.ring;

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
 * This class extends {@link RingQueue} and only adds {@code BigDecimal}-specific conveniences
 * such as {@link #sum()}, {@link #average()}, {@link #peek()} and a no-argument {@link #array()}.
 * All the generic ring-buffer behaviour is inherited from {@link RingQueue}.
 * <p>
 * <b>Note:</b> This implementation is not thread-safe.
 */
public class RingQueueBigDecimal extends RingQueue<BigDecimal>
{
    /**
     * Constructs a new RingQueueBigDecimal with the specified capacity.
     *
     * @param capacity the maximum number of elements the queue can hold.
     * @throws IllegalArgumentException if the capacity is less than or equal to 0.
     */
    public RingQueueBigDecimal(int capacity)
    {
        super(capacity);
    }

    public RingQueueBigDecimal(BigDecimal[] data)
    {
        super(Objects.requireNonNull(data, "data cannot be null").clone());
    }

    /**
     * Retrieves the element at the head of the queue without removing it.
     *
     * @return the oldest element in the queue, or null if the queue is empty.
     */
    public BigDecimal peek()
    {
        return get(0);
    }

    /**
     * Returns an array containing all of the elements in this queue in proper sequence
     * (from oldest to newest).
     *
     * @return a new BigDecimal array containing the queue elements.
     */
    public BigDecimal[] array()
    {
        return array(new BigDecimal[size()]);
    }

    /**
     * Calculates the sum of all values in the queue.
     *
     * @return the sum of all elements.
     */
    public BigDecimal sum()
    {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : list())
        {
            total = total.add(value);
        }
        return total;
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
        if (isEmpty())
        {
            return BigDecimal.ZERO;
        }
        return sum().divide(BigDecimal.valueOf(size()), 10, RoundingMode.HALF_UP);
    }

    public static RingQueueBigDecimal getSynchronized(RingQueueBigDecimal queue)
    {
        return new RingQueueBigDecimal(queue.getCapacity())
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
            public BigDecimal peek()
            {
                synchronized(lock)
                {
                    return super.peek();
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
            public BigDecimal[] array(BigDecimal[] a)
            {
                synchronized(lock)
                {
                    return super.array(a);
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
