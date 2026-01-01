/*
 *  Require.java
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
package io.nut.base.util;

import java.util.function.Supplier;

/**
 * Backport of java.util.Objects methods added in Java 9, 16, and 19.
 * This class provides Java 8 compatible implementations of methods that were
 * introduced in later Java versions.
 * 
 * @author (Claude) Backport for Java 8 compatibility
 * @since 1.8
 */
public final class Require
{

    /**
     * Private constructor to prevent instantiation.
     */
    private Require()
    {
    }

    // =========================================================================
    // Java 9 Methods
    // =========================================================================
    /**
     * Returns the first argument if it is non-{@code null} and otherwise
     * returns the non-{@code null} second argument.
     *
     * @param obj an object
     * @param defaultObj a non-{@code null} object to return if the first
     * argument is {@code null}
     * @param <T> the type of the reference
     * @return the first argument if it is non-{@code null} and otherwise the
     * second argument if it is non-{@code null}
     * @throws NullPointerException if both {@code obj} is null and
     * {@code defaultObj} is {@code null}
     * @since 9
     */
    public static <T> T requireNonNullElse(T obj, T defaultObj)
    {
        return obj != null ? obj : requireNonNull(defaultObj, "defaultObj");
    }

    /**
     * Returns the first argument if it is non-{@code null} and otherwise
     * returns the non-{@code null} value of {@code supplier.get()}.
     *
     * @param obj an object
     * @param supplier of a non-{@code null} object to return if the first
     * argument is {@code null}
     * @param <T> the type of the first argument and return type
     * @return the first argument if it is non-{@code null} and otherwise the
     * value from {@code supplier.get()} if it is non-{@code null}
     * @throws NullPointerException if both {@code obj} is null and either the
     * {@code supplier} is {@code null} or the {@code supplier.get()} value is
     * {@code null}
     * @since 9
     */
    public static <T> T requireNonNullElseGet(T obj, Supplier<? extends T> supplier)
    {
        return obj != null ? obj
                : requireNonNull(requireNonNull(supplier, "supplier").get(), "supplier.get()");
    }

    /**
     * Checks if the {@code index} is within the bounds of the range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>
     * The {@code index} is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     * <li>{@code index < 0}</li>
     * <li>{@code index >= length}</li>
     * <li>{@code length < 0}, which is implied from the former
     * inequalities</li>
     * </ul>
     *
     * @param index the index
     * @param length the upper-bound (exclusive) of the range
     * @return {@code index} if it is within bounds of the range
     * @throws IndexOutOfBoundsException if the {@code index} is out of bounds
     * @since 9
     */
    public static int checkIndex(int index, int length)
    {
        if (index < 0 || index >= length)
        {
            throw new IndexOutOfBoundsException(outOfBoundsCheckIndexMsg(index, length));
        }
        return index;
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive) is within the bounds of range from {@code 0}
     * (inclusive) to {@code length} (exclusive).
     *
     * <p>
     * The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     * <li>{@code fromIndex < 0}</li>
     * <li>{@code fromIndex > toIndex}</li>
     * <li>{@code toIndex > length}</li>
     * <li>{@code length < 0}, which is implied from the former
     * inequalities</li>
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-range
     * @param toIndex the upper-bound (exclusive) of the sub-range
     * @param length the upper-bound (exclusive) the range
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 9
     */
    public static int checkFromToIndex(int fromIndex, int toIndex, int length)
    {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length)
        {
            throw new IndexOutOfBoundsException(outOfBoundsCheckFromToIndexMsg(fromIndex, toIndex, length));
        }
        return fromIndex;
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code fromIndex + size} (exclusive) is within the bounds of range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>
     * The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     * <li>{@code fromIndex < 0}</li>
     * <li>{@code size < 0}</li>
     * <li>{@code fromIndex + size > length}, taking into account integer
     * overflow</li>
     * <li>{@code length < 0}, which is implied from the former
     * inequalities</li>
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-interval
     * @param size the size of the sub-range
     * @param length the upper-bound (exclusive) of the range
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 9
     */
    public static int checkFromIndexSize(int fromIndex, int size, int length)
    {
        // Check for integer overflow in fromIndex + size
        int end = fromIndex + size;
        if (fromIndex < 0 || size < 0 || end < 0 || end > length)
        {
            throw new IndexOutOfBoundsException(outOfBoundsCheckFromIndexSizeMsg(fromIndex, size, length));
        }
        return fromIndex;
    }

    // =========================================================================
    // Java 16 Methods (long overloads)
    // =========================================================================
    /**
     * Checks if the {@code index} is within the bounds of the range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>
     * The {@code index} is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     * <li>{@code index < 0}</li>
     * <li>{@code index >= length}</li>
     * <li>{@code length < 0}, which is implied from the former
     * inequalities</li>
     * </ul>
     *
     * @param index the index
     * @param length the upper-bound (exclusive) of the range
     * @return {@code index} if it is within bounds of the range
     * @throws IndexOutOfBoundsException if the {@code index} is out of bounds
     * @since 16
     */
    public static long checkIndex(long index, long length)
    {
        if (index < 0 || index >= length)
        {
            throw new IndexOutOfBoundsException(outOfBoundsCheckIndexMsg(index, length));
        }
        return index;
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive) is within the bounds of range from {@code 0}
     * (inclusive) to {@code length} (exclusive).
     *
     * <p>
     * The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     * <li>{@code fromIndex < 0}</li>
     * <li>{@code fromIndex > toIndex}</li>
     * <li>{@code toIndex > length}</li>
     * <li>{@code length < 0}, which is implied from the former
     * inequalities</li>
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-range
     * @param toIndex the upper-bound (exclusive) of the sub-range
     * @param length the upper-bound (exclusive) the range
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 16
     */
    public static long checkFromToIndex(long fromIndex, long toIndex, long length)
    {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length)
        {
            throw new IndexOutOfBoundsException(outOfBoundsCheckFromToIndexMsg(fromIndex, toIndex, length));
        }
        return fromIndex;
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code fromIndex + size} (exclusive) is within the bounds of range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>
     * The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     * <li>{@code fromIndex < 0}</li>
     * <li>{@code size < 0}</li>
     * <li>{@code fromIndex + size > length}, taking into account integer
     * overflow</li>
     * <li>{@code length < 0}, which is implied from the former
     * inequalities</li>
     * </ul>
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-interval
     * @param size the size of the sub-range
     * @param length the upper-bound (exclusive) of the range
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     * @since 16
     */
    public static long checkFromIndexSize(long fromIndex, long size, long length)
    {
        // Check for long overflow in fromIndex + size
        long end = fromIndex + size;
        if (fromIndex < 0 || size < 0 || end < 0 || end > length)
        {
            throw new IndexOutOfBoundsException(outOfBoundsCheckFromIndexSizeMsg(fromIndex, size, length));
        }
        return fromIndex;
    }

    // =========================================================================
    // Java 19 Methods
    // =========================================================================
    /**
     * Returns a string equivalent to the string returned by
     * {@code Object.toString} if that method and {@code hashCode} are not
     * overridden.
     *
     * <p>
     * This method returns a string for an object without calling any
     * overridable methods of the object.
     *
     * @param o an object
     * @return a string equivalent to the string returned by
     * {@code Object.toString} if that method and {@code hashCode} are not
     * overridden
     * @throws NullPointerException if the argument is null
     * @since 19
     */
    public static String toIdentityString(Object o)
    {
        requireNonNull(o, "o");
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================
    /**
     * Helper method from java.util.Objects for null checking. Included here to
     * avoid dependency on external Objects class.
     */
    private static <T> T requireNonNull(T obj, String paramName)
    {
        if (obj == null)
        {
            throw new NullPointerException(paramName + " must not be null");
        }
        return obj;
    }

    /**
     * Generates error message for checkIndex failures.
     */
    private static String outOfBoundsCheckIndexMsg(int index, int length)
    {
        return "Index " + index + " out of bounds for length " + length;
    }

    /**
     * Generates error message for checkIndex failures (long version).
     */
    private static String outOfBoundsCheckIndexMsg(long index, long length)
    {
        return "Index " + index + " out of bounds for length " + length;
    }

    /**
     * Generates error message for checkFromToIndex failures.
     */
    private static String outOfBoundsCheckFromToIndexMsg(int fromIndex, int toIndex, int length)
    {
        return "Range [" + fromIndex + ", " + toIndex + ") out of bounds for length " + length;
    }

    /**
     * Generates error message for checkFromToIndex failures (long version).
     */
    private static String outOfBoundsCheckFromToIndexMsg(long fromIndex, long toIndex, long length)
    {
        return "Range [" + fromIndex + ", " + toIndex + ") out of bounds for length " + length;
    }

    /**
     * Generates error message for checkFromIndexSize failures.
     */
    private static String outOfBoundsCheckFromIndexSizeMsg(int fromIndex, int size, int length)
    {
        return "Range [" + fromIndex + ", " + fromIndex + " + " + size + ") out of bounds for length " + length;
    }

    /**
     * Generates error message for checkFromIndexSize failures (long version).
     */
    private static String outOfBoundsCheckFromIndexSizeMsg(long fromIndex, long size, long length)
    {
        return "Range [" + fromIndex + ", " + fromIndex + " + " + size + ") out of bounds for length " + length;
    }
}
