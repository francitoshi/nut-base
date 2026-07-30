/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import io.nut.base.profile.Profiler;
import io.nut.base.time.JavaTime;
import io.nut.base.util.Utils;
import io.nut.base.util.concurrent.hive.Hive;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

public class CacheBenchmarkTest
{
    @Test
    public void testBenchmark() throws InterruptedException
    {
        int N = 4000;
        int C = 400;
        int T = 1000;
        Cache<Integer, String> cache0 = new TinyLFUCache<Integer, String>(C).synchronizedCache();
        Cache<Integer, String> cache1 = new ARCCache<Integer, String>(C).synchronizedCache();
        Cache<Integer, String> cache2 = new LRULFUCache<Integer, String>(C).synchronizedCache();

        Profiler profiler = new Profiler(JavaTime.Resolution.NS, true);
        
        Profiler.Task t0 = profiler.getTask("tiny");
        Profiler.Task t1 = profiler.getTask("arc");
        Profiler.Task t2 = profiler.getTask("lru-lfu");

        System.out.printf("N=%d C=%d class=%s\n", N, C, this.getClass().getName());
        Profiler.Task[] t = new Profiler.Task[]{t0, t1, t2};
        Cache[] cache = new Cache[]{ cache0, cache1, cache2 };
        
        for(int k=0;k<cache.length;k++)
        {
            runWorkload(cache[k], 10, 0);
        }
        
        Hive hive = Hive.hive();
        CountDownLatch cdl = new CountDownLatch(cache.length);

        for(int k=0;k<cache.length;k++)
        {
            final Profiler.Task tk = t[k]; 
            final Cache cacheK = cache[k]; 
            hive.spawn(()->
            {
                tk.start();
                runWorkload(cacheK, N, T);
                tk.stop().count(N);
                cdl.countDown();
            });
        }

        cdl.await();
        profiler.print();
    }

    public static void runWorkload(Cache<Integer, String> cache, int n, long creationNanos)
    {
        Random rnd = new Random(32); // semilla fija para reproducibilidad

        int hotSetSize = Math.max(1, n / 20);      // ~5% de las claves, muy calientes
        int scanSize   = n * 2;                    // barrido mucho más grande que el caché
        int rounds     = 5;                        // repeticiones del patrón completo

        for (int round = 0; round < rounds; round++)
        {
            // --- Fase 1: acceso intensivo al conjunto caliente (favorece frecuencia) ---
            for (int i = 0; i < hotSetSize * 10; i++)
            {
                int key = rnd.nextInt(hotSetSize);
                touch(cache, key, creationNanos);
            }

            // --- Fase 2: barrido secuencial de un solo uso (favorece recencia / mata LRU pura) ---
            for (int i = 0; i < scanSize; i++)
            {
                int key = hotSetSize + i; // claves fuera del conjunto caliente, sin repetición
                touch(cache, key, creationNanos);
            }

            // --- Fase 3: acceso con distribución sesgada (mezcla realista, tipo Zipf simplificado) ---
            for (int i = 0; i < n; i++)
            {
                int key = skewedKey(rnd, n);
                touch(cache, key, creationNanos);
            }

            // --- Fase 4: revisita del conjunto caliente para comprobar si sobrevivió al barrido ---
            for (int key = 0; key < hotSetSize; key++)
            {
                touch(cache, key, creationNanos);
            }
        }
    }

    private static void touch(Cache<Integer, String> cache, int key, long creationNanos)
    {
        String value = cache.get(key);
        if (value == null)
        {
            // Simula el coste real de "crear" el dato en caso de fallo de caché
            if (creationNanos > 0)
            {
                Utils.parkNanos(creationNanos);
            }
            cache.put(key, "value-" + key);
        }
    }

    // Genera claves con sesgo hacia valores bajos, simulando popularidad desigual (~80/20)
    private static int skewedKey(Random rnd, int n)
    {
        double u = rnd.nextDouble();
        double skewed = Math.pow(u, 3); // exponente > 1 concentra hacia valores bajos
        return (int) (skewed * n);
    }

}
