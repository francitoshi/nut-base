/*
 * SparseMatrix.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.matrix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import java.util.function.UnaryOperator;

/**
 * A memory-efficient implementation of a multi-dimensional matrix using a
 * {@link HashMap}. Only non-null values are stored, making it suitable for
 * matrices where most positions are empty.
 *
 * @param <E> the type of elements maintained by this matrix
 */
public class SparseMatrix<E> implements Matrix<E>
{

    /**
     * Internal key class that wraps an {@code int[]} so it can be used safely
     * as a {@link HashMap} key. The hash is computed once at construction time.
     */
    private static final class Index
    {

        private final int[] idx;
        private final int hash;

        Index(int[] indexes)
        {
            this.idx = indexes.clone();
            this.hash = Arrays.hashCode(this.idx);
        }

        @Override
        public int hashCode()
        {
            return hash;
        }

        @Override
        public boolean equals(Object other)
        {
            return (other instanceof Index) && Arrays.equals(idx, ((Index) other).idx);
        }
    }

    private final int dimensions;
    private final Map<Index, E> data = new HashMap<>();

    /**
     * Constructs a new {@code SparseMatrix} with the specified number of
     * dimensions.
     *
     * @param dimensions the number of dimensions (must be greater than 0)
     * @throws IllegalArgumentException if {@code dimensions} is less than or
     * equal to 0
     */
    public SparseMatrix(int dimensions)
    {
        if (dimensions <= 0)
        {
            throw new IllegalArgumentException("At least one dimension is required");
        }
        this.dimensions = dimensions;
    }

    /**
     * Validates {@code indexes} and wraps them in an {@link Index} suitable for
     * map look-up.
     *
     * @param indexes the array of coordinates
     * @return a new {@code Index} for those coordinates
     * @throws IllegalArgumentException if the number of indices does not match
     * the matrix dimensions
     */
    private Index key(int[] indexes)
    {
        if (indexes.length != dimensions)
        {
            throw new IllegalArgumentException(String.format(
                    "Expected %d indices, but received %d", dimensions, indexes.length));
        }
        return new Index(indexes);
    }

    // -------------------------------------------------------------------------
    // Core operations
    // -------------------------------------------------------------------------
    @Override
    public E set(E e, int... indexes)
    {
        return data.put(key(indexes), e);
    }

    @Override
    public E get(int... indexes)
    {
        return data.get(key(indexes));
    }

    @Override
    public E remove(int... indexes)
    {
        return data.remove(key(indexes));
    }

    @Override
    public int count()
    {
        return data.size();
    }

    @Override
    public int dimensions()
    {
        return dimensions;
    }
 
    // -------------------------------------------------------------------------
    // Iteration and transformation
    // -------------------------------------------------------------------------
    @Override
    public void forEach(BiConsumer<int[], E> action)
    {
        data.forEach((index, value) -> action.accept(index.idx, value));
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator)
    {
        data.replaceAll((index, value) -> operator.apply(value));
    }    

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------
    @Override
    public boolean anyMatch(Predicate<E> predicate)
    {
        return data.values().stream().anyMatch(predicate);
}

    @Override
    public boolean allMatch(Predicate<E> predicate)
    {
        return data.values().stream().allMatch(predicate);
    }

    @Override
    public Optional<E> findFirst(Predicate<E> predicate)
    {
        return data.values().stream().filter(predicate).findFirst();
    }

    // -------------------------------------------------------------------------
    // Removal
    // -------------------------------------------------------------------------
    @Override
    public void removeIf(Predicate<E> predicate)
    {
        data.values().removeIf(predicate);
    }

    @Override
    public void clear()
    {
        data.clear();
    }
}
