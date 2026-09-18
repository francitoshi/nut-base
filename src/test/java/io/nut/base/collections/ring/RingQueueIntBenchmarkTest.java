/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.ring;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

/**
 * Throughput comparison between the synchronized {@link RingQueueInt} and
 * several {@code java.util.concurrent} queues using non-blocking access on all
 * of them.
 *
 * <p>5 producer threads and 5 consumer threads (10 threads in total) exchange
 * values for 5 seconds; the test prints how many values each queue managed to
 * pass and the percentage relative to {@link ArrayBlockingQueue}.</p>
 */
public class RingQueueIntBenchmarkTest
{
    private static final int PRODUCERS = 5;
    private static final int CONSUMERS = 5;
    private static final int CAPACITY = 1 << 12;
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long WARMUP_NANOS = TimeUnit.SECONDS.toNanos(1);

    private interface QueueAdapter
    {
        void push(int value);

        int pop();

        boolean isEmpty();
    }

    @Test
    public void testThroughput() throws InterruptedException
    {
        QueueAdapter ring = new QueueAdapter()
        {
            private final RingQueueInt queue = RingQueueInt.getSynchronized(new RingQueueInt(CAPACITY));

            @Override
            public void push(int value)
            {
                queue.push(value);
            }

            @Override
            public int pop()
            {
                return queue.pop();
            }

            @Override
            public boolean isEmpty()
            {
                return queue.isEmpty();
            }
        };

        QueueAdapter abq = new QueueAdapter()
        {
            private final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(CAPACITY);

            @Override
            public void push(int value)
            {
                queue.offer(value);
            }

            @Override
            public int pop()
            {
                Integer value = queue.poll();
                return value != null ? value : 0;
            }

            @Override
            public boolean isEmpty()
            {
                return queue.isEmpty();
            }
        };

        QueueAdapter clq = new QueueAdapter()
        {
            private final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();

            @Override
            public void push(int value)
            {
                queue.offer(value);
            }

            @Override
            public int pop()
            {
                Integer value = queue.poll();
                return value != null ? value : 0;
            }

            @Override
            public boolean isEmpty()
            {
                return queue.isEmpty();
            }
        };

        QueueAdapter ltq = new QueueAdapter()
        {
            private final LinkedTransferQueue<Integer> queue = new LinkedTransferQueue<>();

            @Override
            public void push(int value)
            {
                queue.offer(value);
            }

            @Override
            public int pop()
            {
                Integer value = queue.poll();
                return value != null ? value : 0;
            }

            @Override
            public boolean isEmpty()
            {
                return queue.isEmpty();
            }
        };

        run(ring, WARMUP_NANOS);
        run(abq, WARMUP_NANOS);
        run(clq, WARMUP_NANOS);
        run(ltq, WARMUP_NANOS);

        long ringPassed = run(ring, WINDOW_NANOS);
        long abqPassed = run(abq, WINDOW_NANOS);
        long clqPassed = run(clq, WINDOW_NANOS);
        long ltqPassed = run(ltq, WINDOW_NANOS);

        System.out.println();
        System.out.println("Valores pasados de 5 productores a 5 consumidores en 5 segundos (no bloqueante):");
        System.out.printf("RingQueueInt          : %,d  (%.1f %% vs ABQ)%n", ringPassed, 100.0 * ringPassed / abqPassed);
        System.out.printf("ArrayBlockingQueue    : %,d  (100.0 %% bootstrap)%n", abqPassed);
        System.out.printf("ConcurrentLinkedQueue : %,d  (%.1f %% vs ABQ)%n", clqPassed, 100.0 * clqPassed / abqPassed);
        System.out.printf("LinkedTransferQueue   : %,d  (%.1f %% vs ABQ)%n", ltqPassed, 100.0 * ltqPassed / abqPassed);
        System.out.println();
    }

    private long run(QueueAdapter q, long windowNanos) throws InterruptedException
    {
        long deadline = System.nanoTime() + windowNanos;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger producersRunning = new AtomicInteger(PRODUCERS);

        long[] consumed = new long[CONSUMERS];
        Thread[] producers = new Thread[PRODUCERS];
        Thread[] consumers = new Thread[CONSUMERS];

        for (int i = 0; i < PRODUCERS; i++)
        {
            final int id = i;
            producers[i] = new Thread(() ->
            {
                long count = 0;
                int value = id + 1;
                try
                {
                    start.await();
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                    producersRunning.decrementAndGet();
                    return;
                }
                long iter = 0;
                while (true)
                {
                    q.push(value);
                    value += PRODUCERS;
                    count++;
                    if ((++iter & 1023) == 0 && System.nanoTime() >= deadline)
                    {
                        break;
                    }
                }
                producersRunning.decrementAndGet();
            }, "producer-" + id);
        }

        for (int i = 0; i < CONSUMERS; i++)
        {
            final int id = i;
            consumers[i] = new Thread(() ->
            {
                long count = 0;
                try
                {
                    start.await();
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
                while (true)
                {
                    int value = q.pop();
                    if (value != 0)
                    {
                        count++;
                        continue;
                    }
                    if (System.nanoTime() >= deadline && producersRunning.get() == 0 && q.isEmpty())
                    {
                        break;
                    }
                    LockSupport.parkNanos(10L);
                }
                consumed[id] = count;
            }, "consumer-" + id);
        }

        for (Thread t : producers)
        {
            t.start();
        }
        for (Thread t : consumers)
        {
            t.start();
        }

        start.countDown();

        for (Thread t : producers)
        {
            t.join();
        }
        for (Thread t : consumers)
        {
            t.join();
        }

        long totalConsumed = 0;
        for (long c : consumed)
        {
            totalConsumed += c;
        }
        return totalConsumed;
    }
}