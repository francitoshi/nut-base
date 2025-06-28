/*
 *  BytesFilter.java
 *
 *  Copyright (c) 2023-2025 francitoshi@gmail.com
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

/**
 * A functional interface that defines a contract for filtering or transforming a byte array.
 * <p>
 * Implementations of this interface can be used to modify, inspect, or replace
 * byte data, for example, within a data processing pipeline or a proxy. Common use
 * cases include encryption, decryption, compression, content manipulation, or logging.
 */
@FunctionalInterface
public interface BytesFilter 
{

    /**
     * A default, pass-through filter that performs no modification.
     * <p>
     * This implementation represents the "identity" function, returning the input
     * byte array completely unchanged. It is useful as a default or a no-op filter,
     * avoiding the need for null checks in code that conditionally applies a filter.
     */
    BytesFilter EQUAL = (byte[] bytes) -> bytes;

    /**
     * Applies a transformation to the given byte array.
     * <p>
     * An implementation of this method processes the input array and returns the result.
     * The returned array can be a new array with modified content or the original
     * input array if no changes were made.
     * <p>
     * <b>Implementation Note:</b> For predictability and to avoid side effects,
     * it is strongly recommended that implementations do not modify the input
     * {@code bytes} array in place.
     *
     * @param bytes The input byte array to be filtered. It is the responsibility of the caller
     *              to ensure this is not null.
     * @return The filtered or transformed byte array. May be a new array or the original one.
     */
    byte[] filter(byte[] bytes);
}
