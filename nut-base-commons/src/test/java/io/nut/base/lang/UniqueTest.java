/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class UniqueTest
{
    
    @Test
    public void testUnique_GenericType()
    {
        String[] u = {"1","3","2"};
        String[] r = {"1","3","2","1","2","3"};
        assertArrayEquals(u, Unique.objects(u));
        assertArrayEquals(u, Unique.objects(r));
    }

    @Test
    public void testUnique_bytes()
    {
        byte[] u = {1,3,2};
        byte[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Unique.bytes(u));
        assertArrayEquals(u, Unique.bytes(r));
    }

    @Test
    public void testUnique_shorts()
    {
        short[] u = {1,3,2};
        short[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Unique.shorts(u));
        assertArrayEquals(u, Unique.shorts(r));
    }

    @Test
    public void testUnique_chars()
    {
        char[] u = {1,3,2};
        char[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Unique.chars(u));
        assertArrayEquals(u, Unique.chars(r));
    }

    @Test
    public void testUnique_ints()
    {
        int[] u = {1,3,2};
        int[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Unique.ints(u));
        assertArrayEquals(u, Unique.ints(r));
    }

    @Test
    public void testUnique_longs()
    {
        long[] u = {1,3,2};
        long[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Unique.longs(u));
        assertArrayEquals(u, Unique.longs(r));
    }

    @Test
    public void testUnique_doubles()
    {
        double[] u = {1, 3, 2};
        double[] r = {1, 3, 2, 1, 2, 3};
        assertArrayEquals(u, Unique.doubles(u), 0.0);
        assertArrayEquals(u, Unique.doubles(r), 0.0);
    }

    @Test
    public void testUnique_floats()
    {
        float[] u = {1, 3, 2};
        float[] r = {1, 3, 2, 1, 2, 3};
        assertArrayEquals(u, Unique.floats(u), 0.0f);
        assertArrayEquals(u, Unique.floats(r), 0.0f);
    }

    
}
