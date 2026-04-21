/*
 *  BeeTest.java
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

import io.nut.base.util.Utils;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 *
 * @author franci
 */
public class BeeTest
{
    /**
     * Test of setHive method, of class Bee.
     */
    @Test
    public void testSetHive()
    {
        Bee<String> instance = new Bee<String>()
        {
            @Override
            protected void receive(String m)
            {
                System.out.println(m);
            }
        };
        instance.send("hello");

        Hive hive = new Hive();
        instance.setHive(hive);

        instance.send("world");
        
        instance.shutdown();
    }

    /**
     * Test of getException method, of class Bee.
     */
    @Test
    public void testGetException() 
    {
        //Test Bees with no Hive work synchronously
        Bee<String> instance = new Bee()
        {
            @Override
            protected void receive(Object m) 
            {
                throw new NullPointerException();
            }
        };
        instance.dryLogger();
        instance.send("hello");        
        assertNotNull(instance.getException());
    }

    /**
     * Test of send method, of class Bee.
     */
    @Test
    public void testSend() 
    {
        final AtomicBoolean eureka = new AtomicBoolean();
        final AtomicInteger concurrent = new AtomicInteger(0);
        final AtomicInteger count = new AtomicInteger(0);
        final Hive hive = new Hive(8,16,4,1000);
        //Test Bees with no Hive work synchronously
        Bee<String> instance = new Bee(8, hive)
        {
            @Override
            protected void receive(Object m) 
            {
                int n = concurrent.incrementAndGet();
                count.incrementAndGet();
                if(n>4)
                {
                    eureka.set(true);
                }
                else if(eureka.get()==false)
                {
                    Utils.sleep(100);
                }
                concurrent.decrementAndGet();
            }
        };
        for(int i=0;i<100;i++)
        {
            instance.send("hello");
        }
        Utils.sleep(500);
        assertEquals(100, count.get());
        assertTrue(eureka.get());
    }
  
    /**
     * awaitTermination(millis) must return false when the timeout expires
     * before the Bee terminates. The bug causes it to restart the wait
     * indefinitely, so this test would hang forever without a fix.
     *
     * The @Timeout annotation kills the test after 5 s so CI never blocks.
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void awaitTermination_shouldReturnFalseWhenTimeoutExpires() throws InterruptedException
    {
        Hive hive = new Hive(2);
 
        // This Bee blocks forever on every message, so it will never finish.
        Bee<String> bee = new Bee<String>(1, hive)
        {
            @Override
            protected void receive(String message)
            {
                try 
                { 
                    Thread.sleep(10_000); 
                } 
                catch (InterruptedException ignored) 
                {
                }
            }
        }.dryLogger();
 
        bee.send("block");

        // Give the worker a moment to pick up the message.
        Thread.sleep(100);
 
        bee.shutdown();
 
        // Should return false quickly (timeout = 300 ms), NOT wait forever.
        boolean terminated = bee.awaitTermination(300);
 
        assertFalse(terminated, "awaitTermination must return false when the timeout expires");
 
        hive.shutdown();
    }

    /**
     * Tests for {@link Bee#shutdown(boolean)} with {@code onlyWhenEmpty = true}.
     *
     * <p>Key scenarios covered:
     * <ul>
     *   <li>Already idle when shutdown(true) is called — terminates immediately.</li>
     *   <li>Queue drains normally before shutdown(true) fires.</li>
     *   <li>A receive() call produces new messages via send() — shutdown must NOT
     *       trigger while those messages are still being processed.</li>
     *   <li>shutdown(false) still works as before (regression guard).</li>
     *   <li>New messages sent after shutdown(true) are still processed if they
     *       arrive before the queue empties.</li>
     * </ul>
     */
    
    private static final int TIMEOUT_SECONDS = 10;
 
    // -------------------------------------------------------------------------
    // Helper: minimal concrete Bee that records processed messages
    // -------------------------------------------------------------------------
    private static Bee<Integer> recordingBee(Hive hive, CopyOnWriteArrayList<Integer> received)
    {
        return new Bee<Integer>(2, hive)
        {
            @Override
            protected void receive(Integer m)
            {
                received.add(m);
            }
        };
    }
 
    // -------------------------------------------------------------------------
    // 1. Already idle: queue empty, no workers active → terminates at once
    // -------------------------------------------------------------------------
    @Test
    @Timeout(TIMEOUT_SECONDS)
    void shutdownWhenEmpty_alreadyIdle_terminatesImmediately() throws InterruptedException
    {
        try (Hive hive = new Hive(4))
        {
            CopyOnWriteArrayList<Integer> received = new CopyOnWriteArrayList<>();
            Bee<Integer> bee = recordingBee(hive, received);
 
            // No messages sent — queue is empty and no workers are running
            bee.shutdown(true);
 
            assertTrue(bee.awaitTermination(2000), "Bee should terminate immediately when idle");
            assertTrue(bee.isTerminated());
        }
    }
 
    // -------------------------------------------------------------------------
    // 2. Messages in flight: shutdown(true) waits for the queue to drain
    // -------------------------------------------------------------------------
    @Test
    @Timeout(TIMEOUT_SECONDS)
    void shutdownWhenEmpty_drainsPendingMessages() throws InterruptedException
    {
        try (Hive hive = new Hive(4))
        {
            CopyOnWriteArrayList<Integer> received = new CopyOnWriteArrayList<>();
            Bee<Integer> bee = recordingBee(hive, received);
 
            int total = 50;
            for (int i = 0; i < total; i++)
            {
                bee.send(i);
            }
 
            bee.shutdown(true);
 
            assertTrue(bee.awaitTermination(5000), "Bee should terminate after draining the queue");
            assertEquals(total, received.size(), "All messages must have been processed");
        }
    }
 
    // -------------------------------------------------------------------------
    // 3. Core race-condition scenario: receive() calls send() on itself.
    //    shutdown(true) must NOT fire while a worker is still active and can
    //    still enqueue new messages.
    // -------------------------------------------------------------------------
    @Test
    @Timeout(TIMEOUT_SECONDS)
    void shutdownWhenEmpty_workerProducesNewMessages_allProcessed() throws InterruptedException
    {
        try (Hive hive = new Hive(4))
        {
            AtomicInteger processedCount = new AtomicInteger(0);
            int[] fanOut = {3}; // each message spawns this many children (decreasing)
 
            Bee<Integer>[] beeHolder = new Bee[1];
            beeHolder[0] = new Bee<Integer>(2, hive)
            {
                @Override
                protected void receive(Integer depth)
                {
                    processedCount.incrementAndGet();
                    if (depth > 0)
                    {
                        // While a worker is active, it sends new messages —
                        // shutdown(true) must not trigger yet.
                        for (int i = 0; i < fanOut[0]; i++)
                        {
                            beeHolder[0].send(depth - 1);
                        }
                    }
                }
            };
 
            // Send the root message and request deferred shutdown
            beeHolder[0].send(3); // will fan out: 1 + 3 + 9 + 27 = 40 messages
            beeHolder[0].shutdown(true);
 
            assertTrue(beeHolder[0].awaitTermination(8000), "Bee should terminate after all recursive messages are processed");
 
            // 1 (depth=3) + 3 (depth=2) + 9 (depth=1) + 27 (depth=0) = 40
            assertEquals(40, processedCount.get(), "Every recursively produced message must be processed");
        }
    }
 
    // -------------------------------------------------------------------------
    // 4. Regression: shutdown(false) still works as before
    // -------------------------------------------------------------------------
    @Test
    @Timeout(TIMEOUT_SECONDS)
    void shutdownFalse_rejectsNewMessagesAndDrains() throws InterruptedException
    {
        try (Hive hive = new Hive(4))
        {
            CopyOnWriteArrayList<Integer> received = new CopyOnWriteArrayList<>();
            CountDownLatch blocking = new CountDownLatch(1);
 
            Bee<Integer> bee = new Bee<Integer>(1, hive)
            {
                @Override
                protected void receive(Integer m)
                {
                    if (m == 0)
                    {
                        // Block the worker so we can test send() after shutdown(false)
                        try { blocking.await(); } catch (InterruptedException ignored) {}
                    }
                    received.add(m);
                }
            };
 
            bee.send(0);    // will block the single worker
            bee.send(1);    // queued
 
            Thread.sleep(50); // let message 0 start processing
            bee.shutdown(false);
 
            // New message after shutdown(false) must be rejected
            assertFalse(bee.send(99), "send() must return false after shutdown(false)");
 
            blocking.countDown(); // unblock the worker
            assertTrue(bee.awaitTermination(5000));
 
            assertTrue(received.contains(0));
            assertTrue(received.contains(1));
            assertFalse(received.contains(99), "Message sent after shutdown must not be processed");
        }
    }
 
    // -------------------------------------------------------------------------
    // 5. shutdown(true) still rejects send() once the actual shutdown fires
    // -------------------------------------------------------------------------
    @Test
    @Timeout(TIMEOUT_SECONDS)
    void shutdownWhenEmpty_afterTermination_rejectsSend() throws InterruptedException
    {
        try (Hive hive = new Hive(4))
        {
            CopyOnWriteArrayList<Integer> received = new CopyOnWriteArrayList<>();
            Bee<Integer> bee = recordingBee(hive, received);
 
            bee.send(1);
            bee.shutdown(true);
            assertTrue(bee.awaitTermination(5000));
 
            assertFalse(bee.send(99), "send() must return false after termination");
            assertFalse(received.contains(99));
        }
    }
 
    // -------------------------------------------------------------------------
    // 6. Messages sent BEFORE shutdown(true) but arriving late are still processed
    // -------------------------------------------------------------------------
    @Test
    @Timeout(TIMEOUT_SECONDS)
    void shutdownWhenEmpty_messagesArrivingBeforeShutdownFires_allProcessed() throws InterruptedException
    {
        try (Hive hive = new Hive(4))
        {
            CopyOnWriteArrayList<Integer> received = new CopyOnWriteArrayList<>();
            CountDownLatch holdFirstWorker = new CountDownLatch(1);
 
            Bee<Integer> bee = new Bee<Integer>(2, hive)
            {
                @Override
                protected void receive(Integer m)
                {
                    if (m == 0)
                    {
                        // Hold the first worker so the queue is not empty when shutdown(true) is called
                        try { holdFirstWorker.await(); } catch (InterruptedException ignored) {}
                    }
                    received.add(m);
                }
            };
 
            bee.send(0);              // will be held in receive()
            for (int i = 1; i <= 10; i++) bee.send(i);  // queued
 
            Thread.sleep(50);         // ensure message 0 is being processed
            bee.shutdown(true);       // must NOT fire yet — a worker is active
 
            assertFalse(bee.isShutdown(), "Bee must still be RUNNING while a worker holds a permit");
 
            holdFirstWorker.countDown(); // release the held worker
 
            assertTrue(bee.awaitTermination(5000));
            assertEquals(11, received.size(), "All 11 messages must be processed");
        }
    }
}
