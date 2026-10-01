/*
 * Matrix.java
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

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Represents a multi-dimensional sparse matrix.
 *
 * @param <E> the type of elements maintained by this matrix
 */
public interface Matrix<E>
{
    /**
     * Replaces or sets the element at the specified multi-dimensional indices.
     *
     * @param e       the element to be stored
     * @param indices the coordinates in the matrix
     * @return the previous element at the specified position, or {@code null}
     *         if there was none
     * @throws IllegalArgumentException if the number of indices does not match
     *                                  the matrix dimensions
     */
    E set(E e, int... indices);

    /**
     * Retrieves the element at the specified multi-dimensional indices.
     *
     * @param indices the coordinates in the matrix
     * @return the element at the specified position, or {@code null} if the
     *         position is empty
     * @throws IllegalArgumentException if the number of indices does not match
     *                                  the matrix dimensions
     */
    E get(int... indices);

    /**
     * Removes the element at the specified multi-dimensional indices.
     *
     * @param indices the coordinates in the matrix
     * @return the element that was removed, or {@code null} if there was none
     * @throws IllegalArgumentException if the number of indices does not match
     *                                  the matrix dimensions
     */
    E remove(int... indices);

    /**
     * Returns the total number of stored elements in the matrix.
     *
     * @return the number of non-empty cells
     */
    int count();

    /**
     * Returns the number of dimensions of this matrix.
     *
     * @return the dimensionality of the matrix
     */
    int dimensions();
    
    /**
     * Performs the given action for each stored element, passing its
     * coordinates and value to the {@link BiConsumer}.
     *
     * @param action the action to perform for each element
     */
    void forEach(BiConsumer<int[], E> action);
    
    /**
     * Replaces each stored value with the result of applying the given
     * operator to that value.
     *
     * @param operator the operator to apply to each value
     */
    void replaceAll(UnaryOperator<E> operator);

    /**
     * Returns {@code true} if at least one stored element matches the given
     * predicate. Returns {@code false} on an empty matrix.
     *
     * @param predicate the condition to test
     * @return {@code true} if any element matches, {@code false} otherwise
     */
    boolean anyMatch(Predicate<E> predicate);

    /**
     * Returns {@code true} if every stored element matches the given
     * predicate. Returns {@code true} on an empty matrix (vacuous truth).
     *
     * @param predicate the condition to test
     * @return {@code true} if all elements match, {@code false} otherwise
     */
    boolean allMatch(Predicate<E> predicate);

    /**
     * Returns an {@link Optional} containing the first stored value that
     * matches the given predicate, or an empty {@code Optional} if none does.
     * Because the matrix is unordered, "first" is arbitrary but consistent
     * within a single call.
     *
     * @param predicate the condition to test
     * @return an {@code Optional} with a matching value, or empty
     */
    Optional<E> findFirst(Predicate<E> predicate);

    /**
     * Removes all elements that match the given predicate.
     *
     * @param predicate the condition that marks elements for removal
     */
    void removeIf(Predicate<E> predicate);

    /**
     * Removes all elements from the matrix. After this call {@link #count()}
     * returns {@code 0}.
     */
    void clear();
}
