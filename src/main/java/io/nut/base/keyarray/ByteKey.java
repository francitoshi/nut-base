/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import io.nut.base.encoding.Hex;
import io.nut.base.util.Utils;
import java.io.Serializable;
import java.util.Arrays;

/**
 * This class is intended for encapsulate a byte[] and use it as keys in a Map&lt;KeyBytes,byte[]&gt; because using byte[] as key
 * compare array memory address and not the the content.
 * 
 * @author franci
 */
public class ByteKey implements Comparable<ByteKey>, Serializable
{
    
    protected final byte[] bytes;

    public ByteKey(byte[] bytes)
    {
        this.bytes = bytes;
    }

    public ByteKey(String hex)
    {
        this.bytes = Hex.decode(hex);
    }

    @Override
    public int compareTo(ByteKey other)
    {
        return Utils.compare(this.bytes, other.bytes);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.bytes);
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
        final ByteKey other = (ByteKey) obj;
        return Arrays.equals(this.bytes, other.bytes);
    }

    @Override
    public String toString()
    {
        return Hex.encode(bytes);
    }

    public byte[] getBytes()
    {
        return bytes.clone();
    }
    
}
