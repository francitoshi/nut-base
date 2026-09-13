/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import io.nut.base.encoding.Hex;
import io.nut.base.util.Utils;
import java.util.Arrays;

/**
 * This class is intended for encapsulate a byte[] and use it as keys in a Map&lt;KeyBytes,byte[]&gt; because using byte[] as key
 * compare array memory address and not the the content.
 * 
 * @author franci
 */
public class ByteKey extends ArrayKey<byte[]>
{
    public ByteKey(byte[] bytes)
    {
        super(bytes);
    }

    public ByteKey(String hex)
    {
        super(Hex.decode(hex));
    }

    @Override
    protected int compareArrays(byte[] a, byte[] b)
    {
        return Utils.compare(a, b);
    }

    @Override
    protected int hashArray(byte[] a)
    {
        return Arrays.hashCode(a);
    }

    @Override
    public String toString()
    {
        return Hex.encode(array);
    }

    public byte[] getBytes()
    {
        return array.clone();
    }
}