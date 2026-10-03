/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.kdf;

import io.nut.base.crypto.Kripto;
import io.nut.base.encoding.Hex;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class MemoryStallKDFTest
{
    /**
     * Test of main method, of class JavaKDF.
     */
    @Test
    public void testMain()
    {
        Kripto.registerBouncyCastle();
        byte[] password = "password".getBytes();
        byte[] salt = "somesalt".getBytes();
        int blocks = 2; // minimal power-of-two
        int timeCost = 1;

        long t0 = System.nanoTime();
        for(int i=0;i<3;i++)
        {
            MemoryStallKDF javaKDF = new MemoryStallKDF();
            int keyLength = 32+i*2;
            byte[] key = javaKDF.deriveKey(password, salt, blocks, timeCost, keyLength);
            assertEquals(keyLength, key.length, "i="+i);
            System.out.println("Derived Key (hex): " + Hex.encode(key));
        }
        long t1 = System.nanoTime();
        System.out.println(TimeUnit.NANOSECONDS.toMillis(t1-t0)+" ms");
    }
        
}
