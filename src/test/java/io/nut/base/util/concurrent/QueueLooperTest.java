/*
 * QueueLooperTest.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class QueueLooperTest
{

    // -----------------------------------------------------------------------
    // Constructor validation
    // -----------------------------------------------------------------------
    @Test
    void constructor_throwsWhenThreadsIsZero()
    {
        assertThrows(IllegalArgumentException.class, ()
                -> new QueueLooper<>(new LinkedBlockingQueue<>(), () ->
                {
                }, 0));
    }

    @Test
    void constructor_throwsWhenThreadsIsNegative()
    {
        assertThrows(IllegalArgumentException.class, ()
                -> new QueueLooper<>(new LinkedBlockingQueue<>(), () ->
                {
                }, -1));
    }

    @Test
    void constructor_acceptsOneThread()
    {
        assertDoesNotThrow(() -> new QueueLooper<>(new LinkedBlockingQueue<>(), () ->
        {
        }, 1));
    }

    @Test
    void constructor_defaultIsOneThread()
    {
        // Should not throw and behave as single-thread mode
        assertDoesNotThrow(() -> new QueueLooper<>(new LinkedBlockingQueue<>(), () ->
        {
        }));
    }

    // -----------------------------------------------------------------------
    // execute() — single thread mode
    // -----------------------------------------------------------------------
    @Test
    void execute_doesNothingWhenQueueIsEmpty() throws InterruptedException
    {
        AtomicInteger count = new AtomicInteger(0);
        QueueLooper<String> looper = new QueueLooper<>(new LinkedBlockingQueue<>(), count::incrementAndGet);

        looper.execute();
        Thread.sleep(200);

        assertEquals(0, count.get(), "Task must not run when queue is empty");
    }

    @Test
    void execute_runsTaskWhenQueueHasElements() throws InterruptedException
    {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            queue.poll();
            latch.countDown();
        });

        queue.put("element");
        looper.execute();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Task must run when queue has elements");
    }

    @Test
    void execute_drainsAllElements_singleThread() throws InterruptedException
    {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        int total = 20;
        CountDownLatch latch = new CountDownLatch(total);

        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            if (queue.poll() != null)
            {
                latch.countDown();
            }
        });

        for (int i = 0; i < total; i++)
        {
            queue.put("element-" + i);
        }
        looper.execute();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All elements must be processed");
        assertEquals(0, queue.size(), "Queue must be empty after processing");
    }

    @Test
    void execute_neverRunsConcurrently_singleThread() throws InterruptedException
    {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        int total = 50;
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(total);

        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            int c = concurrent.incrementAndGet();
            maxConcurrent.accumulateAndGet(c, Math::max);
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            queue.poll();
            latch.countDown();
            concurrent.decrementAndGet();
        });

        for (int i = 0; i < total; i++)
        {
            queue.put("element-" + i);
        }
        looper.execute();

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(1, maxConcurrent.get(), "Single-thread mode must never run concurrently");
    }

    // -----------------------------------------------------------------------
    // execute() — multi-thread mode
    // -----------------------------------------------------------------------
    @Test
    void execute_neverExceedsThreadLimit() throws InterruptedException
    {
        int threads = 3;
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        int total = 100;
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(total);

        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            int c = concurrent.incrementAndGet();
            maxConcurrent.accumulateAndGet(c, Math::max);
            queue.poll();
            latch.countDown();
            concurrent.decrementAndGet();
        });

        for (int i = 0; i < total; i++)
        {
            queue.put("element-" + i);
        }
        // Trigger many times to stress the worker cap
        for (int i = 0; i < 20; i++)
        {
            looper.execute();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertTrue(maxConcurrent.get() <= threads,
                "Active workers must never exceed thread limit, was: " + maxConcurrent.get());
    }

    @Test
    void execute_drainsAllElements_multiThread() throws InterruptedException
    {
        int threads = 4;
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        int total = 100;
        CountDownLatch latch = new CountDownLatch(total);

        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            if (queue.poll() != null)
            {
                latch.countDown();
            }
        }, threads);

        for (int i = 0; i < total; i++)
        {
            queue.put("element-" + i);
        }
        looper.execute();

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All elements must be processed in multi-thread mode");
        assertEquals(0, queue.size(), "Queue must be empty after processing");
    }

    // -----------------------------------------------------------------------
    // put()
    // -----------------------------------------------------------------------
    @Test
    void put_triggersExecutionAutomatically() throws InterruptedException
    {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            queue.poll();
            latch.countDown();
        });

        looper.put("element");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "put() must trigger execution automatically");
    }

    @Test
    void put_processesAllElementsAddedFromMultipleThreads() throws InterruptedException
    {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        int total = 50;
        CountDownLatch latch = new CountDownLatch(total);

        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            if (queue.poll() != null)
            {
                latch.countDown();
            }
        });

        for (int i = 0; i < total; i++)
        {
            final int n = i;
            new Thread(() ->
            {
                try
                {
                    looper.put("element-" + n);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All elements added concurrently must be processed");
        assertEquals(0, queue.size());
    }

    @Test
    void put_blocksWhenQueueIsFull() throws InterruptedException
    {
        BlockingQueue<String> limitedQueue = new LinkedBlockingQueue<>(1);
        AtomicInteger putsCompleted = new AtomicInteger(0);
        CountDownLatch putStarted = new CountDownLatch(1);

        QueueLooper<String> looper = new QueueLooper<>(limitedQueue, () ->
        {
        });

        limitedQueue.put("fills-the-queue");

        Thread t = new Thread(() ->
        {
            try
            {
                putStarted.countDown();
                looper.put("should-block");
                putsCompleted.incrementAndGet();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });
        t.start();

        assertTrue(putStarted.await(2, TimeUnit.SECONDS));
        Thread.sleep(200);
        assertEquals(0, putsCompleted.get(), "put() must block when queue is full");
        t.interrupt();
    }

    // -----------------------------------------------------------------------
    // Task exception resilience
    // -----------------------------------------------------------------------
    @Test
    void task_exceptionDoesNotStopQueueDraining() throws InterruptedException
    {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(3);

        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            String e = queue.poll();
            if (e != null)
            {
                latch.countDown();
                if ("error".equals(e))
                {
                    throw new RuntimeException("Simulated error");
                }
            }
        });

        looper.put("ok-1");
        looper.put("error");
        looper.put("ok-2");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Elements after a failing task must still be processed");
    }

    @Test
    void task_multipleExceptionsDoNotCorruptWorkerCount() throws InterruptedException
    {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        int total = 10;
        CountDownLatch latch = new CountDownLatch(total);

        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            String e = queue.poll();
            if (e != null)
            {
                latch.countDown();
                throw new RuntimeException("Always fails");
            }
        });

        for (int i = 0; i < total; i++)
        {
            looper.put("element-" + i);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All elements must be processed even when task always throws");
    }

    // -----------------------------------------------------------------------
    // Race condition: element added while worker is finishing
    // -----------------------------------------------------------------------
    @Test
    void put_elementAddedWhileWorkerIsFinishing_getsProcessed() throws InterruptedException
    {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        int total = 20;
        CountDownLatch latch = new CountDownLatch(total);

        QueueLooper<String> looper = new QueueLooper<>(queue, () ->
        {
            if (queue.poll() != null)
            {
                latch.countDown();
            }
        });

        // Add elements one by one with small delays to provoke the exit race condition
        for (int i = 0; i < total; i++)
        {
            looper.put("element-" + i);
            Thread.sleep(5);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "No element must be left unprocessed due to exit race condition");
        assertEquals(0, queue.size());
    }
}
