/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Throughput benchmark for every Channel implementation in this package,
 * instantiated through the {@link Channel} factory methods.
 *
 * Scenarios: 1Px1C, 2Px8C, 8Px2C and 8Px8C. Each scenario runs every
 * implementation and prints the throughput in operations/second, followed by
 * the ratio relative to the reference implementation, UnbufferedChannel
 * (e.g. 1.20x).
 *
 * To tune the number of total messages: -Dchannel.bench.total=100000
 * To tune the buffer capacity: -Dchannel.bench.capacity=1024
 */
public class BenchmarkChannelTest
{
    private static final int TOTAL = Integer.getInteger("channel.bench.total", 2_000_000);
    private static final int WARMUP = Integer.getInteger("channel.bench.warmup", 100_000);
    private static final int BUFFER_CAPACITY = Integer.getInteger("channel.bench.capacity", 1024);

    private interface ChannelFactory
    {
        String name();

        Channel<Object> newChannel();
    }

    private static ChannelFactory factory(String name, Channel<Object> channel)
    {
        return new ChannelFactory()
        {
            @Override
            public String name()
            {
                return name;
            }

            @Override
            public Channel<Object> newChannel()
            {
                return channel;
            }
        };
    }

    @Test
    public void benchmark() throws InterruptedException
    {
        int[][] configs = {
            {1, 1},
            {2, 8},
            {8, 2},
            {8, 8}
        };

        ChannelFactory[] impls = {
            factory("Unbuffered", Channel.unbuffered()),
            factory("Buffered", Channel.buffered(BUFFER_CAPACITY)),
            factory("Unlimited", Channel.unlimited()),
            factory("CloseableUnbuffered", Channel.closeableUnbuffered()),
            factory("CloseableBuffered", Channel.closeableBuffered(BUFFER_CAPACITY)),
            factory("CloseableUnlimited", Channel.closeableUnlimited())
        };

        // JIT warm-up for every implementation
        for (ChannelFactory f : impls)
        {
            run(1, 1, f.newChannel(), WARMUP);
        }

        System.out.println("=== Channel implementations benchmark (reference: Unbuffered) ===");
        System.out.println(String.format("%-6s %-20s %16s %8s", "Config", "Channel", "ops/s", "x"));

        for (int[] cfg : configs)
        {
            int producers = cfg[0];
            int consumers = cfg[1];

            double refRate = 0;
            for (ChannelFactory f : impls)
            {
                long ms = run(producers, consumers, f.newChannel(), TOTAL);
                double rate = TOTAL / (ms / 1e9);
                if (refRate == 0)
                {
                    refRate = rate;
                }
                System.out.println(String.format("%-6s %-20s %16.0f %8s",
                        producers + "x" + consumers, f.name(), rate, String.format("%.2fx", rate / refRate)));
            }
        }
    }

    private static long run(int producers, int consumers, Channel<Object> channel, int total)
            throws InterruptedException
    {
        if (total % producers != 0 || total % consumers != 0)
        {
            throw new IllegalArgumentException(
                    "total (" + total + ") must be divisible by producers and consumers");
        }
        final int perProducer = total / producers;
        final int perConsumer = total / consumers;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(producers + consumers);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int p = 0; p < producers; p++)
        {
            final int producerIndex = p;
            Thread t = new Thread(() ->
            {
                try
                {
                    start.await();
                    for (int i = 0; i < perProducer; i++)
                    {
                        channel.put(producerIndex);
                    }
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
                catch (Throwable t2)
                {
                    error.compareAndSet(null, t2);
                }
                finally
                {
                    done.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        for (int c = 0; c < consumers; c++)
        {
            Thread t = new Thread(() ->
            {
                try
                {
                    start.await();
                    for (int i = 0; i < perConsumer; i++)
                    {
                        channel.get();
                    }
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
                catch (Throwable t2)
                {
                    error.compareAndSet(null, t2);
                }
                finally
                {
                    done.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        long t0 = System.nanoTime();
        start.countDown();
        done.await();
        long elapsed = System.nanoTime() - t0;

        Throwable e = error.get();
        if (e != null)
        {
            throw new RuntimeException(e);
        }
        return elapsed;
    }
}