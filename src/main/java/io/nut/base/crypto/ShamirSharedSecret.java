/*
 *  ShamirSharedSecret.java
 *
 *  Copyright (C) 2018-2024 francitoshi@gmail.com
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
package io.nut.base.crypto;

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
    
    private static final Comparator<byte[]> compareByteArrays = new Comparator<byte[]>()
    {
        @Override
        public int compare(byte[] a, byte[] b)
        {
            int min = Math.min(a.length,b.length);
            int cmp = 0;
            for(int i=0;i<min && cmp==0;i++)
            {
                cmp = Byte.compare(a[i], b[i]);
            }
            if(cmp==0)
            {
                cmp = Integer.compare(a.length, b.length);
            }
            return cmp;
        }
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
        Arrays.sort(parts, compareByteArrays);
        sorted.add(parts[0]);
        for(int i=1;i<parts.length;i++)
        {
            if(compareByteArrays.compare(parts[i-1], parts[i])<0)
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
