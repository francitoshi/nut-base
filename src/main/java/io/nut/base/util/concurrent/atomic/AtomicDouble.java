/*
 *  AtomicDouble.java
 *
 *  Copyright (c) 2018-2023 francitoshi@gmail.com
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
package io.tea.base.util.concurrent.atomic;

/**
 * A {@code double} value that may be updated atomically.  See the
 * {@link java.util.concurrent.atomic} package specification for
 * description of the properties of atomic variables. An
 * {@code AtomicDouble} is used in applications such as atomically
 * incremented sequence numbers, and cannot be used as a replacement
 * for a {@link java.lang.Double}. However, this class does extend
 * {@code Number} to allow uniform access by tools and utilities that
 * deal with numerically-based classes.
 *
 * @author franci
 */
public class AtomicDouble extends Number
{
    private final Object lock = new Object();
    
    private volatile double value;

    /**
     * Creates a new AtomicDouble with the given initial value.
     *
     * @param initialValue the initial value
     */
    public AtomicDouble(double initialValue) 
    {
        value = initialValue;
    }

    /**
     * Creates a new AtomicDouble with initial value {@code 0}.
     */
    public AtomicDouble() 
    {
    }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    public final double get() 
    {
        synchronized(lock)
        {
            return value;
        }
    }

    /**
     * Sets to the given value.
     *
     * @param newValue the new value
     */
    public final void set(double newValue) 
    {
        synchronized(lock)
        {
            this.value = newValue;
        }
    }

    /**
     * Eventually sets to the given value.
     *
     * @param newValue the new value
     */
    public final void lazySet(double newValue) 
    {
        synchronized(lock)
        {
            this.value = newValue;
        }
    }

    /**
     * Atomically sets to the given value and returns the old value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    public final double getAndSet(double newValue) 
    {
        synchronized(lock)
        {
            double ret = this.value;
            this.value = newValue;
            return ret;
        }
    }
    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(double expect, double update)
    {
        synchronized(lock)
        {
            if(this.value == expect)
            {
                this.value = update;
                return true;
            }
            return false;
        }
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful
     */
    public final boolean weakCompareAndSet(double expect, double update) 
    {
        return compareAndSet(expect, update);
    }

    /**
     * Atomically increments by one the current value.
     *
     * @return the previous value
     */
    public final double getAndIncrement() 
    {
        synchronized(lock)
        {
            double ret = this.value;
            this.value++;
            return ret;
        }
    }

    /**
     * Atomically decrements by one the current value.
     *
     * @return the previous value
     */
    public final double getAndDecrement() 
    {
        synchronized(lock)
        {
            double ret = this.value;
            this.value--;
            return ret;
        }
    }

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the previous value
     */
    public final double getAndAdd(double delta) 
    {
        synchronized(lock)
        {
            double ret = this.value;
            this.value+=delta;
            return ret;
        }
    }

    /**
     * Atomically increments by one the current value.
     *
     * @return the updated value
     */
    public final double incrementAndGet() 
    {
        synchronized(lock)
        {
            return ++this.value;
        }
    }

    /**
     * Atomically decrements by one the current value.
     *
     * @return the updated value
     */
    public final double decrementAndGet() 
    {
        synchronized(lock)
        {
            return --this.value;
        }
    }

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the updated value
     */
    public final double addAndGet(double delta) 
    {
        synchronized(lock)
        {
            return  (this.value+=delta);
        }
    }

    /**
     * Returns the String representation of the current value.
     * @return the String representation of the current value
     */
    @Override
    public String toString() 
    {
        return Double.toString(this.value);
    }

    /**
     * Returns the value of this {@code AtomicDouble} as an {@code int}
     * after a narrowing primitive conversion.
     */
    @Override
    public int intValue() 
    {
        return (int)this.value;
    }

    /**
     * Returns the value of this {@code AtomicDouble} as a {@code double}.
     */
    @Override
    public long longValue() 
    {
        return (long) this.value;
    }

    /**
     * Returns the value of this {@code AtomicDouble} as a {@code float}
     * after a widening primitive conversion.
     */
    @Override
    public float floatValue() 
    {
        return (float)this.value;
    }

    /**
     * Returns the value of this {@code AtomicDouble} as a {@code double}
     * after a widening primitive conversion.
     */
    @Override
    public double doubleValue() 
    {
        return this.value;
    }
    
    public int compareTo(AtomicDouble other)
    {
        return Double.compare(this.value, other.value);
    }
}
