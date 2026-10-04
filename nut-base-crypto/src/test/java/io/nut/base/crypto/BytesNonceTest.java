/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BytesNonceTest
{
    static final int LOOPS = 100_000;
    
    @Test
    public void testGetRandomInstance()
    {
        BytesNonce instance = BytesNonce.getRandomInstance();
        
        HashSet<byte[]> set = new HashSet();
        for(int i=0;i<LOOPS;i++)
        {
            assertTrue(set.add(instance.next()));
        }
        assertEquals(LOOPS, set.size());
    }

    @Test
    public void testGetRandomCounterInstance()
    {
        BytesNonce instance = BytesNonce.getRandomCounterInstance();
        HashSet<byte[]> set = new HashSet();
        for(int i=0;i<LOOPS;i++)
        {
            assertTrue(set.add(instance.next()));
        }
        assertEquals(LOOPS, set.size());
    }
    
}
