/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Task executor that tells apart three kinds of work:
 * <ul>
 * <li><b>CPU-bound</b> work, submitted via
 * {@link #cpu(Runnable)} / {@link #cpu(Supplier)}</li>
 * <li><b>I/O-bound</b> work, submitted via
 * {@link #io(Runnable)} / {@link #io(Supplier)}</li>
 * <li><b>Network-bound</b> work, submitted via
 * {@link #net(Runnable)} / {@link #net(Supplier)}</li>
 * </ul>
 *
 * <p>
 * I/O and network tasks spend most of their time blocked, not consuming CPU, so
 * it's safe to run more of them concurrently than CPU-bound tasks without
 * oversubscribing the CPU. The three limits are nested, not independent:
 * <pre>
 *   running(cpu)                               &lt;= cpu
 *   running(cpu) + running(io)                 &lt;= cpu + io
 *   running(cpu) + running(io) + running(net)  &lt;= cpu + io + net
 * </pre>
 *
 * <p>
 * In practice this means a CPU task is guaranteed at least {@code cpu} slots;
 * an I/O task can use a slot that CPU tasks are not currently using (as long as
 * the combined cpu+io usage stays under {@code cpu+io}); and a network task can
 * use any slot left over after cpu and io usage, up to the grand total
 * {@code cpu+io+net}.
 *
 * <p>
 * Submitting a task never blocks the caller. If there's no free slot for the
 * task's tier right now, the task is appended to a single internal FIFO queue
 * shared by all three tiers. Whenever a slot frees up, the whole queue is
 * scanned front-to-back and every waiting task that now fits is started,
 * regardless of tier. This means older tasks get priority over newer ones, but
 * a tier is never starved just because another tier keeps producing work: a net
 * task queued before a batch of cpu tasks gets its chance to run as soon as
 * there's total capacity for it, even while cpu tasks behind it in the queue
 * are still waiting on the cpu-only limit.
 *
 * <p>
 * This is a thin facade over a three-level {@link TieredExecutor}: cpu is level
 * 0, io is level 1 and net is level 2.
 *
 * <p>
 * Thread-safe. Not reusable after {@link #shutdown()}.
 */
public class CpuIoNetExecutor
{
    static final int CPU = 0;
    static final int IO = 1;
    static final int NET = 2;

    private final TieredExecutor executor;

    /**
     * @param cpu max number of concurrently running cpu() tasks
     * @param io extra slots available to io() tasks (cpu+io tasks together
     * never exceed cpu+io)
     * @param net extra slots available to net() tasks (all tasks together never
     * exceed cpu+io+net)
     */
    public CpuIoNetExecutor(int cpu, int io, int net)
    {
        this.executor = new TieredExecutor(cpu, io, net);
    }

    // ---------------------------------------------------------------- public API
    public Future<Void> cpu(Runnable task)
    {
        return executor.submit(CPU, task);
    }

    public <T> Future<T> cpu(Supplier<T> task)
    {
        return executor.submit(CPU, task);
    }

    public Future<Void> io(Runnable task)
    {
        return executor.submit(IO, task);
    }

    public <T> Future<T> io(Supplier<T> task)
    {
        return executor.submit(IO, task);
    }

    public Future<Void> net(Runnable task)
    {
        return executor.submit(NET, task);
    }

    public <T> Future<T> net(Supplier<T> task)
    {
        return executor.submit(NET, task);
    }

    /**
     * Number of cpu-tier tasks currently running.
     */
    public int activeCpu()
    {
        return executor.active(CPU);
    }

    /**
     * Number of io-tier tasks currently running.
     */
    public int activeIo()
    {
        return executor.active(IO);
    }

    /**
     * Number of net-tier tasks currently running.
     */
    public int activeNet()
    {
        return executor.active(NET);
    }

    /**
     * Number of tasks waiting for a free slot, across all tiers.
     */
    public int pending()
    {
        return executor.pending();
    }

    /**
     * Stops accepting new tasks; the underlying pool shuts down once running
     * tasks finish.
     */
    public void shutdown()
    {
        executor.shutdown();
    }
}
