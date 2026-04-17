/*
 *  Splitter.java
 *
 *  Copyright (c) 2012-2026 francitoshi@gmail.com
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
package io.nut.base.util;

import io.nut.base.bag.Bag;
import io.nut.base.equalizer.Equalizer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class that provides methods for splitting arrays into groups of equal
 * (or equivalent) elements.
 *
 * <p>Elements are considered equal according to one of three strategies:
 * <ul>
 *   <li>Natural ordering via {@link Comparable} (default).</li>
 *   <li>A caller-supplied {@link Comparator}.</li>
 *   <li>A caller-supplied {@link Equalizer}.</li>
 * </ul>
 *
 * <p>All methods are static; this class is not meant to be instantiated.
 *
 * <pre>{@code
 * String[] words = {"banana", "apple", "apple", "cherry", "banana"};
 * String[][] groups = Splitter.splitEquals(words);
 * // groups -> [["apple","apple"], ["banana","banana"], ["cherry"]]
 * }</pre>
 */
public class Splitter
{
    /**
     * Splits a source array into groups of equal elements using a
     * {@link Comparator} to determine equality.
     *
     * <p>Elements for which {@code cmp.compare(a, b) == 0} are placed in the
     * same group. When {@code cmp} is {@code null} the elements' natural
     * ordering (via {@link Comparable}) is used instead.
     *
     * @param <T> the type of elements in the array
     * @param src the source array to split; must not be {@code null}
     * @param cmp the comparator used to decide equality, or {@code null} to
     *            use natural ordering
     * @return a two-dimensional array where each inner array contains a group
     *         of mutually-equal elements; never {@code null}
     */
    public static <T> T[][] splitEquals(T[] src, Comparator<T> cmp)
    {
        Bag<T> bag = (cmp == null) ? Bag.create() : Bag.create(cmp);

        for (T item : src)
        {
            bag.add(item);
        }
        T[][] empty = (T[][]) Array.newInstance(src.getClass(), 0);
        return bag.toArray(empty);
    }

    /**
     * Splits a source array into groups of equal elements using an
     * {@link Equalizer} to determine equality.
     *
     * <p>Elements for which {@code cmp.equal(a, b)} returns {@code true} are
     * placed in the same group. When {@code cmp} is {@code null} the elements'
     * natural ordering is used instead.
     *
     * @param <T> the type of elements in the array
     * @param src the source array to split; must not be {@code null}
     * @param cmp the equalizer used to decide equality, or {@code null} to
     *            fall back to natural ordering
     * @return a two-dimensional array where each inner array contains a group
     *         of mutually-equal elements; never {@code null}
     */
    public static <T> T[][] splitEquals(T[] src, Equalizer<T> cmp)
    {
        Bag<T> bag = cmp != null ? Bag.create(cmp) : Bag.create();

        for (T item : src)
        {
            bag.add(item);
        }

        T[][] dst = (T[][]) Array.newInstance(src.getClass(), bag.size());
        return bag.toArray(dst);
    }

    /**
     * Splits a source array into groups of equal elements using the elements'
     * natural ordering.
     *
     * <p>This is a convenience overload equivalent to calling
     * {@link #splitEquals(Object[], Comparator) splitEquals(src, (Comparator) null)}.
     *
     * @param <T> the type of elements in the array; must implement
     *            {@link Comparable}
     * @param src the source array to split; must not be {@code null}
     * @return a two-dimensional array where each inner array contains a group
     *         of mutually-equal elements; never {@code null}
     */
    public static <T> T[][] splitEquals(T[] src)
    {
        return splitEquals(src, (Comparator<T>) null);
    }

    /**
     * Applies {@link #splitEquals(Object[], Comparator)} to every inner array
     * of a two-dimensional source array, collecting all resulting groups into a
     * single flat two-dimensional result array.
     *
     * <p>This is useful for performing successive refinement passes: each
     * previously grouped set of elements is split again according to a
     * potentially finer-grained comparator.
     *
     * @param <T> the type of elements in the arrays
     * @param src the two-dimensional source array whose inner arrays are to be
     *            re-split; must not be {@code null}
     * @param cmp the comparator used to decide equality, or {@code null} to
     *            use natural ordering
     * @return a two-dimensional array containing all groups produced by
     *         splitting every inner array of {@code src}; never {@code null}
     */
    public static <T> T[][] splitAgainEquals(T[][] src, Comparator<T> cmp)
    {
        // TODO: verify behaviour when a non-null comparator is supplied
        List<T[]> list = new ArrayList<>(src.length);

        for (int i = 0; i < src.length; i++)
        {
            T[][] split = splitEquals(src[i], cmp);
            Collections.addAll(list, split);
        }
        return list.toArray(Arrays.copyOf(src, 0));
    }

    /**
     * Applies {@link #splitEquals(Object[])} to every inner array of a
     * two-dimensional source array, collecting all resulting groups into a
     * single flat two-dimensional result array.
     *
     * <p>This is a convenience overload equivalent to calling
     * {@link #splitAgainEquals(Object[][], Comparator)
     * splitAgainEquals(src, null)}.
     *
     * @param <T> the type of elements in the arrays; must implement
     *            {@link Comparable}
     * @param src the two-dimensional source array whose inner arrays are to be
     *            re-split; must not be {@code null}
     * @return a two-dimensional array containing all groups produced by
     *         splitting every inner array of {@code src}; never {@code null}
     */
    public static <T> T[][] splitAgainEquals(T[][] src)
    {
        return splitAgainEquals(src, null);
    }    
}
