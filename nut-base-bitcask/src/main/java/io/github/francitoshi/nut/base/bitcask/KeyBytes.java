/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.util.Arrays;

/**
 * A {@code byte[]} wrapper that provides content-based {@code equals} and
 * {@code hashCode}, for use as map key (section 2 of bitcask-spec.md: bytes
 * are opaque and equal by content).
 * <p>
 * The wrapped array is copied defensively on construction. The stored copy
 * must not be modified after the wrapper is attached to a map.
 */
final class KeyBytes
{
    final byte[] array;
    private final int hash;

    /**
     * Constructs a wrapper with a defensive copy of the given bytes.
     *
     * @param bytes the bytes to wrap
     */
    KeyBytes(byte[] bytes)
    {
        this.array = bytes.clone();
        this.hash = Arrays.hashCode(this.array);
    }

    /**
     * Returns the wrapped bytes. The array is owned by this wrapper and must
     * not be modified by callers.
     *
     * @return the wrapped bytes
     */
    byte[] array()
    {
        return array;
    }

    @Override
    public int hashCode()
    {
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj || obj instanceof KeyBytes && Arrays.equals(array, ((KeyBytes) obj).array);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(array);
    }
}
