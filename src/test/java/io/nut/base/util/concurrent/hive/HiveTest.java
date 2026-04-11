/*
 *  HiveTest.java
 *
 *  Copyright (C) 2024-2026 francitoshi@gmail.com
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
package io.nut.base.util.concurrent.hive;

import io.nut.base.profile.Profiler;
import io.nut.base.util.Utils;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class HiveTest
{
    final Hive hive = new Hive();
    final Bee<Byte> beeByte = new Bee<Byte>(1, hive) 
    {
        @Override
        public void receive(Byte m)
        {
            beeShort.send(m.shortValue());
        }
    };
    final Bee<Short> beeShort = new Bee<Short>(1, hive) 
    {
        @Override
        public void receive(Short m)
        {
            beeInteger.send(m.intValue());
        }
    };
    final Bee<Integer> beeInteger = new Bee<Integer>(1, hive) 
    {
        @Override
        public void receive(Integer m)
        {
            beeLong.send(m.longValue());
        }
    };
    final Bee<Long> beeLong = new Bee<Long>(1, hive) 
    {
        @Override
        public void receive(Long m)
        {
            beeString.send(m.toString());
            beeString.send(",");
        }
    };
    final Bee<String> beeString = new Bee<String>(1, hive) 
    {
        @Override
        public void receive(String m)
        {
            s += m;
        }
    };
    
    volatile String s = "";
    
    /**
     * Test of shutdown method, of class Hive.
     */
    @Test
    public void testSomeMethod1() throws InterruptedException
    {   
        hive.add(beeByte, beeShort, beeInteger, beeLong, beeString);
        
        hive.add(beeByte).add(beeShort).add(beeInteger).add(beeLong).add(beeString);
        
        for(int i=0;i<10;i++)
        {
            beeByte.send((byte)i);
        }
        
        hive.shutdownAndAwaitTermination(beeByte, beeShort, beeInteger, beeLong, beeString);
        
        assertEquals("0,1,2,3,4,5,6,7,8,9,", s);
        assertTrue(hive.isShutdown(), "Shutdown");
        assertTrue(hive.isTerminated(), "Terminated");
    }
        
    /**
     * Test of shutdown method, of class Hive.
     */
    @Test
    public void testWaitPolicy() throws InterruptedException
    {   
        final Runnable fastTask = new Runnable()
        {
            @Override
            public void run()
            {
                Utils.sleep(5);
            }
        };
        final Runnable slowTask = new Runnable()
        {
            @Override
            public void run()
            {
                Utils.sleep(5_000);
            }
        };
        int th = 5;
        int q = 5;
        int loops = 4000;
        int slow = 10;
        Profiler profiler = new Profiler();
        Profiler.Task profilerRun = profiler.getTask("run");
        Profiler.Task profilerWait = profiler.getTask("wait");
        {
            Hive hive1 = new Hive(th, th, q, 60_000, false);
            profilerRun.start();
            for(int i=0;i<loops;i++)
            {
                hive1.execute(i==slow ? slowTask : fastTask);
                profilerRun.count();
            }
            hive1.shutdownAndAwaitTermination();
            profilerRun.stop().count();
        }
        
        {
            Hive hive2 = new Hive(th, th, q, 60_000, true);
            profilerWait.start();
            for(int i=0;i<loops;i++)
            {
                hive2.execute(i==slow ? slowTask : fastTask);
                profilerWait.count();
            }
            hive2.shutdownAndAwaitTermination();
            profilerWait.stop().count();
        }
        profiler.print();
        
        assertTrue(profilerRun.nanos()>profilerWait.nanos());
    }

    /**
     * Test of lazy method, of class Hive.
     */
    @Test
    public void testLazy_Runnable() throws InterruptedException, ExecutionException
    {
        final AtomicInteger value = new AtomicInteger();
        Hive instance = new Hive();
        Future result = instance.lazy(()->value.set(7));
        result.get();
        assertEquals(7, value.get());
    }

    /**
     * Test of lazy method, of class Hive.
     */
    @Test
    public void testLazy_Supplier() throws InterruptedException, ExecutionException
    {
        Hive instance = new Hive();
        Future<Integer> result = instance.lazy(()->1+2);
        assertEquals(3, result.get());
    }

    private Hive taskManager;
    private ThreadPoolExecutor executor;

    @BeforeEach
    void setUp()
    {
        // Inicializamos un pool para las pruebas
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        taskManager = new Hive(executor);
    }

    @AfterEach
    void tearDown()
    {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("Async Runnable debe ejecutarse asíncronamente")
    void testAsyncRunnable() throws Exception
    {
        AtomicBoolean executed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Future<Void> future = taskManager.async(() ->
        {
            executed.set(true);
            latch.countDown();
        });

        // Esperamos a que termine (máximo 1 segundo para no bloquear el test si falla)
        latch.await(1, TimeUnit.SECONDS);

        assertTrue(executed.get(), "El runnable debería haberse ejecutado");
        assertTrue(future.isDone());
    }

    @Test
    @DisplayName("Async Supplier debe retornar el valor correctamente")
    void testAsyncSupplier() throws Exception
    {
        Future<String> future = taskManager.async(() -> "Hola Mundo");
        assertEquals("Hola Mundo", future.get(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Lazy Runnable NO debe ejecutarse hasta llamar a get()")
    void testLazyRunnable() throws Exception
    {
        AtomicBoolean executed = new AtomicBoolean(false);

        Future<Void> future = taskManager.lazy(() -> executed.set(true));

        // Verificamos que tras un pequeño tiempo NO se ha ejecutado
        Thread.sleep(100);
        assertFalse(executed.get(), "No debería haberse ejecutado todavía (es lazy)");

        // Al llamar a get(), se debe ejecutar
        future.get();
        assertTrue(executed.get(), "Debería haberse ejecutado tras llamar a get()");
    }

    @Test
    @DisplayName("Lazy Supplier NO debe ejecutarse hasta llamar a get() y debe devolver valor")
    void testLazySupplier() throws Exception
    {
        AtomicInteger counter = new AtomicInteger(0);

        Future<Integer> future = taskManager.lazy(() -> counter.incrementAndGet());

        // Verificamos que el contador sigue en 0
        Thread.sleep(100);
        assertEquals(0, counter.get(), "El supplier no debería haber incrementado el contador aún");

        // Al llamar a get(), se ejecuta
        Integer result = future.get();

        assertEquals(1, result);
        assertEquals(1, counter.get(), "El contador debería ser 1 tras el primer get()");

        // Verificamos que si llamamos a get() otra vez, no se vuelve a ejecutar (comportamiento de FutureTask)
        future.get();
        assertEquals(1, counter.get(), "No debería ejecutarse dos veces");
    }

    @Test
    @DisplayName("Lazy Supplier debe funcionar con timeout")
    void testLazySupplierWithTimeout() throws Exception
    {
        Future<String> future = taskManager.lazy(() -> "Resultado");

        String result = future.get(500, TimeUnit.MILLISECONDS);

        assertEquals("Resultado", result);
    }

}
