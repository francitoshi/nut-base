/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import java.io.Serializable;

/**
 * Abstract base class for encapsulating a primitive array and using it as a key in a Map,
 * because using a primitive array as key compares array memory address and not the content.
 *
 * @param <A> the type of the wrapped primitive array
 * @author franci
 */
public abstract class ArrayKey<A> implements Comparable<ArrayKey<A>>, Serializable
{
    protected final A array;

    protected ArrayKey(A array)
    {
        this.array = array;
    }

    /**
     * Compares the content of two wrapped arrays.
     *
     * @param a the first array
     * @param b the second array
     * @return a negative integer, zero, or a positive integer as {@code a} is
     *         less than, equal to, or greater than {@code b}
     */
    protected abstract int compareArrays(A a, A b);

    /**
     * Returns the value-based hash code of the wrapped array.
     *
     * @param a the array
     * @return the hash code
     */
    protected abstract int hashArray(A a);

    @Override
    public int compareTo(ArrayKey<A> other)
    {
        return compareArrays(this.array, other.array);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + hashArray(this.array);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ArrayKey<?> other = (ArrayKey<?>) obj;
        return (this.array == null || other.array == null)
            ? this.array == other.array
            : compareArrays(this.array, (A) other.array) == 0;
    }
}