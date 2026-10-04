/*
 * Copyright (C) 2018-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.shamir;

import io.nut.base.crypto.shamir.ShamirScheme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author franci
 */
public class ShamirSharedSecret
{
    private final ShamirScheme scheme;

  /**
   * Creates a new {@link ShamirSharedSecret} instance.
   *
   * @param n the number of parts to produce (must be {@code >1})
   * @param k the threshold of joinable parts (must be {@code <= n})
   */
    public ShamirSharedSecret(int n, int k)
    {
        this.scheme = ShamirScheme.of(n, k);
    }

    public byte[][] split(byte[] secret)
    {
        return scheme.split(secret);
    }
    
    private static final Comparator<byte[]> COMPARE_BYTE_ARRAYS = (a,b) ->
    {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++)
        {
            int c = Byte.compare(a[i], b[i]);
            if (c != 0)
            {
                return c;
            }
        }
        return Integer.compare(a.length, b.length);
    };

    public byte[] join(byte[][] parts)
    {
        //parts must be unique, so let's sort and clean repeated
        return scheme.join(sortedAndUnique(parts));
    }

    private static byte[][] sortedAndUnique(byte[][] parts)
    {
        parts = parts.clone();
        ArrayList<byte[]> sorted = new ArrayList<>();
        Arrays.sort(parts, COMPARE_BYTE_ARRAYS);
        sorted.add(parts[0]);
        for(int i=1;i<parts.length;i++)
        {
            if(COMPARE_BYTE_ARRAYS.compare(parts[i-1], parts[i])<0)
            {
                sorted.add(parts[i]);
            }
        }
        if(sorted.size()<parts.length)
        {
            parts = sorted.toArray(new byte[0][]);
        }
        return parts;
    }
}
