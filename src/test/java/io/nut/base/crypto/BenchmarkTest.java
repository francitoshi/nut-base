/*
 *  BenchmarkTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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

import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class BenchmarkTest
{
    
    public BenchmarkTest()
    {
    }

    @Test
    public void testAESNI()
    {
        Benchmark instance = new Benchmark(Kripto.getInstanceBouncyCastle());
        
        Benchmark.Result[] results = instance.benchmark(1000, Kripto.SecretKeyTransformation.AES_GCM_NoPadding, Kripto.SecretKeyTransformation.ChaCha20_Poly1305);
        for(Benchmark.Result item : results)
        {
            System.out.printf("%s = %d\n", item.skt.name(), item.count);
        }
        boolean aesni = instance.isAESNI();
        System.out.printf("AESNI = %s\n", aesni);

    }
    
}
