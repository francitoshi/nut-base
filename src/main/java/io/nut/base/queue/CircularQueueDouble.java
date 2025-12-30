/*
 *  CircularQueueDouble.java
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

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

// Claude Sonnet 4.5
public class CircularQueueDouble
{

    private final double[] buffer;
    private final int capacity;
    private int head;
    private int tail;
    private int size;

    public CircularQueueDouble(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be positive, but was: " + capacity);
        }
        this.capacity = capacity;
        this.buffer = new double[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public double push(double value)
    {
        double removed = 0;
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

    public double pop()
    {
        if (size == 0)
        {
            return 0;
        }
        double value = buffer[head];
        head = (head + 1) % capacity;
        size--;
        return value;
    }

    public void foreach(DoubleConsumer consumer)
    {
        for (int i = 0; i < size; i++)
        {
            consumer.accept(buffer[(head + i) % capacity]);
        }
    }

    public double[] array()
    {
        double[] result = new double[size];
        for (int i = 0; i < size; i++)
        {
            result[i] = buffer[(head + i) % capacity];
        }
        return result;
    }

    public int size()
    {
        return size;
    }

    public double average()
    {
        if (size == 0)
        {
            return 0;
        }
        return sum() / size;
    }

    public double sum()
    {
        double total = 0;
        for (int i = 0; i < size; i++)
        {
            total += buffer[(head + i) % capacity];
        }
        return total;
    }

    public double min()
    {
        if (size == 0)
        {
            return 0;
        }
        double minValue = buffer[head];
        for (int i = 1; i < size; i++)
        {
            double value = buffer[(head + i) % capacity];
            if (value < minValue)
            {
                minValue = value;
            }
        }
        return minValue;
    }

    public double max()
    {
        if (size == 0)
        {
            return 0;
        }
        double maxValue = buffer[head];
        for (int i = 1; i < size; i++)
        {
            double value = buffer[(head + i) % capacity];
            if (value > maxValue)
            {
                maxValue = value;
            }
        }
        return maxValue;
    }

    public double get(int n)
    {
        if (n < 0 || n >= size)
        {
            return 0;
        }
        return buffer[(head + n) % capacity];
    }
    
    public static CircularQueueDouble getSynchronized(CircularQueueDouble queue)
    {
        return new CircularQueueDouble(queue.capacity)
        {
            final Object lock = new Object();
            @Override
            public double get(int n)
            {
                synchronized(lock)
                {
                    return super.get(n);
                }
            }

            @Override
            public double max()
            {
                synchronized(lock)
                {
                    return super.max();
                }
            }

            @Override
            public double min()
            {
                synchronized(lock)
                {
                    return super.min();
                }
            }

            @Override
            public double sum()
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

            @Override
            public int size()
            {
                synchronized(lock)
                {
                    return super.size();
                }
            }

            @Override
            public double[] array()
            {
                synchronized(lock)
                {
                    return super.array();
                }
            }

            @Override
            public void foreach(DoubleConsumer consumer)
            {
                synchronized(lock)
                {
                    super.foreach(consumer);
                }
            }

            @Override
            public double pop()
            {
                synchronized(lock)
                {
                    return super.pop();
                }
            }

            @Override
            public double push(double value)
            {
                synchronized(lock)
                {
                    return super.push(value);
                }
            }
            
        };
    }
    
}
