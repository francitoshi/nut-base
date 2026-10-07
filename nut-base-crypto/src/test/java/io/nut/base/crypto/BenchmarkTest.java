/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import io.nut.base.jca.Kr;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
@Tag("benchmark")
public class BenchmarkTest
{
    
    public BenchmarkTest()
    {
    }

    @Test
    public void testAESNI()
    {
        Benchmark instance = new Benchmark(Kripto.getInstanceBouncyCastle());
        
        Benchmark.Result[] results = instance.benchmark(1000, Kr.SecretKeyTransformation.AES_GCM_NoPadding, Kr.SecretKeyTransformation.ChaCha20_Poly1305);
        for(Benchmark.Result item : results)
        {
            System.out.printf("%s = %d\n", item.skt.name(), item.count);
        }
        boolean aesni = instance.isAESNI();
        System.out.printf("AESNI = %s\n", aesni);

    }
    
}
