/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Measures the thread-per-channel pattern: one thread parked in {@code get()}
 * per channel, the natural way of draining many channels at once under Loom
 * (each channel's own lock/condition wakes its reader directly).
 *
 * <p>The workload: 5000 rendezvous (unbuffered) and 5000 buffered channels,
 * drained by a fixed pool of producer threads that feed the channels round
 * robin. The measured drain time starts once every consumer is parked in its
 * channel, so it excludes thread creation (reported separately as "setup").
 * The same run is executed with virtual threads and, as a reference, with
 * platform threads — on a JVM without Loom both legs are platform threads.
 *
 * <p>The library targets Java 8, so virtual threads are obtained
 * reflectively.
 *
 * <p>To tune the scenario:
 * <ul>
 *   <li>{@code -Dvt.bench.rendezvous=5000}: number of unbuffered channels</li>
 *   <li>{@code -Dvt.bench.buffered=5000}: number of buffered channels</li>
 *   <li>{@code -Dvt.bench.perChannel=2}: messages sent to each channel</li>
 *   <li>{@code -Dvt.bench.producers=8}: producer threads feeding the channels</li>
 *   <li>{@code -Dchannel.bench.capacity=1024}: capacity of the buffered channels</li>
 *   <li>{@code -Dvt.bench.warmup=200}: channels used by the JIT warm-up run</li>
 * </ul>
 */
//@Tag("benchmark")
public class BenchmarkVTChannelTest
{
    private static final int RENDEZVOUS = Integer.getInteger("vt.bench.rendezvous", 5_000);
    private static final int BUFFERED = Integer.getInteger("vt.bench.buffered", 5_000);
    private static final int MESSAGES_PER_CHANNEL = Integer.getInteger("vt.bench.perChannel", 20);
    private static final int PRODUCERS = Integer.getInteger("vt.bench.producers", 8);
    private static final int BUFFER_CAPACITY = Integer.getInteger("channel.bench.capacity", 1024);
    private static final int WARMUP_CHANNELS = Integer.getInteger("vt.bench.warmup", 200);

    /** Generous watchdog: a hung consumer must fail the test, not block the build. */
    private static final long DRAIN_TIMEOUT_MINUTES = 15;
    private static final long SETUP_TIMEOUT_SECONDS = 120;

    public static void main(String[] args) throws Exception
    {
        benchmark();
    }

    @Test
    public static void benchmark() throws Exception
    {
        System.out.println("=== Thread-per-channel drain: virtual vs platform threads ===");
        System.out.println(String.format("%d rendezvous + %d buffered channels (capacity %d), %d messages per channel, %d producers",
                RENDEZVOUS, BUFFERED, BUFFER_CAPACITY, MESSAGES_PER_CHANNEL, PRODUCERS));
        System.out.println(String.format("virtual threads available: %s", virtualThreadsAvailable()));

        warmup();

        System.out.println(String.format("%-16s %10s %10s %12s %8s", "pattern", "setup(ms)", "drain(ms)", "ops/s", "x"));

        long[] reference = null;
        for (boolean virtual : new boolean[]{true, false})
        {
            long[] result = run("run", virtual, RENDEZVOUS, BUFFERED, MESSAGES_PER_CHANNEL);
            if (reference == null)
            {
                reference = result;
            }
            double ops = (double) (RENDEZVOUS + BUFFERED) * MESSAGES_PER_CHANNEL / (result[1] / 1e9);
            System.out.println(String.format("%-16s %10d %10d %12.0f %8s",
                    virtual ? "virtual-threads" : "platform-threads",
                    result[0] / 1_000_000L, result[1] / 1_000_000L, ops,
                    String.format("%.2fx", (double) reference[1] / result[1])));
        }
    }

    /** Small-scale run of both thread kinds so the JIT compiles the hot paths before the measured runs. */
    private static void warmup() throws Exception
    {
        int rendezvous = WARMUP_CHANNELS / 2;
        int buffered = WARMUP_CHANNELS / 2;
        run("warmup", true, rendezvous, buffered, MESSAGES_PER_CHANNEL);
        run("warmup", false, rendezvous, buffered, MESSAGES_PER_CHANNEL);
    }

    /**
     * Drains the given channels with one consumer thread per channel and
     * returns {@code {setupNanos, drainNanos}}.
     *
     * @param label name prefix for the threads
     * @param virtual {@code true} for virtual threads, {@code false} for platform threads
     * @param rendezvous number of unbuffered channels
     * @param buffered number of buffered channels
     * @param perChannel messages sent to each channel
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static long[] run(String label, boolean virtual, int rendezvous, int buffered, int perChannel)
            throws Exception
    {
        int channels = rendezvous + buffered;
        Channel[] channels1 = new Channel[channels];
        for (int i = 0; i < rendezvous; i++)
        {
            channels1[i] = Channel.unbuffered();
        }
        for (int i = rendezvous; i < channels; i++)
        {
            channels1[i] = Channel.buffered(BUFFER_CAPACITY);
        }

        long setupStart = System.nanoTime();

        CountDownLatch ready = new CountDownLatch(channels);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(channels + PRODUCERS);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < channels; i++)
        {
            Channel channel = channels1[i];
            Runnable consumer = () ->
            {
                try
                {
                    ready.countDown();
                    for (int m = 0; m < perChannel; m++)
                    {
                        channel.get();
                    }
                }
                catch (Throwable t)
                {
                    error.compareAndSet(null, t);
                }
                finally
                {
                    done.countDown();
                }
            };
            if (virtual)
            {
                startVirtualThread(label + "-consumer-" + i, consumer);
            }
            else
            {
                startThread(label + "-consumer-" + i, consumer);
            }
        }
        for (int p = 0; p < PRODUCERS; p++)
        {
            final int producerIndex = p;
            startThread(label + "-producer-" + p, () ->
            {
                try
                {
                    start.await();
                    // round robin over the channels this producer owns, so many
                    // channels are being fed at the same time
                    for (int round = 0; round < perChannel; round++)
                    {
                        for (int i = producerIndex; i < channels; i += PRODUCERS)
                        {
                            channels1[i].put(i);
                        }
                    }
                }
                catch (Throwable t)
                {
                    error.compareAndSet(null, t);
                }
                finally
                {
                    done.countDown();
                }
            });
        }

        if (!ready.await(SETUP_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        {
            throw new IllegalStateException("consumers did not get ready in " + SETUP_TIMEOUT_SECONDS + "s");
        }
        long setupNanos = System.nanoTime() - setupStart;

        long t0 = System.nanoTime();
        start.countDown();
        if (!done.await(DRAIN_TIMEOUT_MINUTES, TimeUnit.MINUTES))
        {
            throw new IllegalStateException("the drain did not finish in " + DRAIN_TIMEOUT_MINUTES + " minutes");
        }
        long drainNanos = System.nanoTime() - t0;

        Throwable e = error.get();
        if (e != null)
        {
            throw new RuntimeException(e);
        }
        return new long[]{setupNanos, drainNanos};
    }

    // -----------------------------------------------------------------------
    // thread creation (reflection keeps the Java 8 target compileable)
    // -----------------------------------------------------------------------

    private static volatile Boolean virtualThreadsAvailable;

    static boolean virtualThreadsAvailable()
    {
        if (virtualThreadsAvailable == null)
        {
            try
            {
                Class<?> builderClass = Class.forName("java.lang.Thread$Builder");
                Thread.class.getMethod("ofVirtual");
                builderClass.getMethod("name", String.class);
                builderClass.getMethod("unstarted", Runnable.class);
                virtualThreadsAvailable = Boolean.TRUE;
            }
            catch (ReflectiveOperationException | LinkageError ex)
            {
                virtualThreadsAvailable = Boolean.FALSE;
            }
        }
        return virtualThreadsAvailable;
    }

    /**
     * Starts {@code body} on a virtual thread when the JVM has Loom, otherwise
     * on a daemon platform thread (so the benchmark still runs everywhere, at
     * the cost of the old per-thread footprint).
     */
    private static void startVirtualThread(String name, Runnable body) throws Exception
    {
        if (!virtualThreadsAvailable())
        {
            startThread(name, body);
            return;
        }
        Class<?> builderClass = Class.forName("java.lang.Thread$Builder");
        Object builder = Thread.class.getMethod("ofVirtual").invoke(null);
        Object named = builderClass.getMethod("name", String.class).invoke(builder, name);
        Thread thread = (Thread) builderClass.getMethod("unstarted", Runnable.class).invoke(named, body);
        thread.start();
    }

    private static void startThread(String name, Runnable body)
    {
        Thread thread = new Thread(body, name);
        thread.setDaemon(true);
        thread.start();
    }
}
