/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.io;

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
