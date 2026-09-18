/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.ring;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Single-threaded throughput comparison between {@link RingQueueInt} and
 * several {@code java.util.concurrent} queues using a batch access pattern: a
 * random number between 1 and 1024 elements is pushed, then popped until the
 * queue is empty, repeated for 5 seconds.
 *
 * <p>The test prints how many values each queue managed to consume and the
 * percentage relative to {@link ArrayBlockingQueue}.</p>
 */
public class RingQueueIntBatchBenchmarkTest
{
    private static final int CAPACITY = 1 << 12;
    private static final int MAX_BATCH = 1024;
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long WARMUP_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final long SEED = 42L;

    private interface QueueAdapter
    {
        void push(int value);

        int pop();
    }

    @Test
    public void testSingleThreadBatch()
    {
        QueueAdapter ring = new QueueAdapter()
        {
            private final RingQueueInt queue = new RingQueueInt(CAPACITY);

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
        };

        run(ring, WARMUP_NANOS);
        run(abq, WARMUP_NANOS);
        run(clq, WARMUP_NANOS);
        run(ltq, WARMUP_NANOS);

        long ringConsumed = run(ring, WINDOW_NANOS);
        long abqConsumed = run(abq, WINDOW_NANOS);
        long clqConsumed = run(clq, WINDOW_NANOS);
        long ltqConsumed = run(ltq, WINDOW_NANOS);

        System.out.println();
        System.out.println("Valores consumidos con un único hilo en 5 segundos:");
        System.out.println("Lotes aleatorios de 1 a 1024 elementos push, hasta vaciar pop:");
        System.out.printf("RingQueueInt          : %,d  (%.1f %% vs ABQ)%n", ringConsumed, 100.0 * ringConsumed / abqConsumed);
        System.out.printf("ArrayBlockingQueue    : %,d  (100.0 %% bootstrap)%n", abqConsumed);
        System.out.printf("ConcurrentLinkedQueue : %,d  (%.1f %% vs ABQ)%n", clqConsumed, 100.0 * clqConsumed / abqConsumed);
        System.out.printf("LinkedTransferQueue   : %,d  (%.1f %% vs ABQ)%n", ltqConsumed, 100.0 * ltqConsumed / abqConsumed);
        System.out.println();
    }

    private long run(QueueAdapter q, long windowNanos)
    {
        long deadline = System.nanoTime() + windowNanos;
        Random rnd = new Random(SEED);
        long count = 0;
        while (System.nanoTime() < deadline)
        {
            int n = rnd.nextInt(MAX_BATCH) + 1;
            for (int i = 0; i < n; i++)
            {
                q.push(i + 1);
            }
            int value;
            while ((value = q.pop()) != 0)
            {
                count++;
            }
        }
        return count;
    }
}