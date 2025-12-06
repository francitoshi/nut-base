/*
 * MemoryStallKDFTest.java
 *
 * Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto.kdf;

import io.nut.base.crypto.Kripto;
import io.nut.base.encoding.Hex;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

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
        int keyLength = 64;
        int blocks = 8; // 64 MB
        int timeCost = 3;

        long t0 = System.nanoTime();
        
        for(int i=0;i<10;i++)
        {
            MemoryStallKDF javaKDF = new MemoryStallKDF();
            byte[] key = javaKDF.deriveKey(password, salt, blocks, timeCost, 32+i*2);
            System.out.println("Derived Key (hex): " + Hex.encode(key));
        }
        long t1 = System.nanoTime();
        System.out.println(TimeUnit.NANOSECONDS.toMillis(t1-t0)+" ms");
    }
        
}
