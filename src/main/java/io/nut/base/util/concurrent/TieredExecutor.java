package io.nut.base.util.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Task executor that tells apart three kinds of work:
 * <ul>
 *   <li><b>CPU-bound</b> work, submitted via {@link #cpu(Runnable)} / {@link #cpu(Supplier)}</li>
 *   <li><b>I/O-bound</b> work, submitted via {@link #io(Runnable)} / {@link #io(Supplier)}</li>
 *   <li><b>Network-bound</b> work, submitted via {@link #net(Runnable)} / {@link #net(Supplier)}</li>
 * </ul>
 *
 * <p>I/O and network tasks spend most of their time blocked, not consuming CPU, so it's safe to
 * run more of them concurrently than CPU-bound tasks without oversubscribing the CPU. The three
 * limits are nested, not independent:
 * <pre>
 *   running(cpu)                               &lt;= cpu
 *   running(cpu) + running(io)                 &lt;= cpu + io
 *   running(cpu) + running(io) + running(net)  &lt;= cpu + io + net
 * </pre>
 *
 * <p>In practice this means a CPU task is guaranteed at least {@code cpu} slots; an I/O task can
 * use a slot that CPU tasks are not currently using (as long as the combined cpu+io usage stays
 * under {@code cpu+io}); and a network task can use any slot left over after cpu and io usage, up
 * to the grand total {@code cpu+io+net}.
 *
 * <p>Submitting a task never blocks the caller. If there's no free slot for the task's tier right
 * now, the task is appended to a single internal FIFO queue shared by all three tiers. Whenever a
 * slot frees up, the whole queue is scanned front-to-back and every waiting task that now fits is
 * started, regardless of tier. This means older tasks get priority over newer ones, but a tier is
 * never starved just because another tier keeps producing work: a net task queued before a batch
 * of cpu tasks gets its chance to run as soon as there's total capacity for it, even while cpu
 * tasks behind it in the queue are still waiting on the cpu-only limit.
 *
 * <p>Thread-safe. Not reusable after {@link #shutdown()}.
 */
public class TieredExecutor
{

    private enum Tier { CPU, IO, NET }

    private final int cpuLimit;
    private final int ioLimit;
    private final int netLimit;

    private final ExecutorService pool;
    private final ReentrantLock lock = new ReentrantLock();

    private int cpuRunning = 0;
    private int ioRunning = 0;
    private int netRunning = 0;

    private final Deque<QueuedTask<?>> queue = new ArrayDeque<>();

    private volatile boolean shutdown = false;

    /**
     * @param cpu max number of concurrently running cpu() tasks
     * @param io  extra slots available to io() tasks (cpu+io tasks together never exceed cpu+io)
     * @param net extra slots available to net() tasks (all tasks together never exceed cpu+io+net)
     */
    public TieredExecutor(int cpu, int io, int net)
    {
        if (cpu < 0 || io < 0 || net < 0)
        {
            throw new IllegalArgumentException("cpu, io and net must be >= 0");
        }
        if (cpu + io + net <= 0)
        {
            throw new IllegalArgumentException("cpu + io + net must be > 0");
        }
        this.cpuLimit = cpu;
        this.ioLimit = io;
        this.netLimit = net;
        // The pool only ever runs tasks that already own a reserved slot (see tryStart), so it
        // never needs more threads than the grand total.
        this.pool = Executors.newFixedThreadPool(cpu + io + net, new TieredThreadFactory());
    }

    // ---------------------------------------------------------------- public API

    public Future<Void> cpu(Runnable task) { return submit(Tier.CPU, toSupplier(task)); }
    public <T> Future<T> cpu(Supplier<T> task) { return submit(Tier.CPU, task); }

    public Future<Void> io(Runnable task) { return submit(Tier.IO, toSupplier(task)); }
    public <T> Future<T> io(Supplier<T> task) { return submit(Tier.IO, task); }

    public Future<Void> net(Runnable task) { return submit(Tier.NET, toSupplier(task)); }
    public <T> Future<T> net(Supplier<T> task) { return submit(Tier.NET, task); }

    /** Number of cpu-tier tasks currently running. */
    public int activeCpu() { lock.lock(); try { return cpuRunning; } finally { lock.unlock(); } }

    /** Number of io-tier tasks currently running. */
    public int activeIo() { lock.lock(); try { return ioRunning; } finally { lock.unlock(); } }

    /** Number of net-tier tasks currently running. */
    public int activeNet() { lock.lock(); try { return netRunning; } finally { lock.unlock(); } }

    /** Number of tasks waiting for a free slot, across all tiers. */
    public int pending()
    {
        lock.lock();
        try
        {
            return queue.size();
        }
        finally
        {
            lock.unlock();
        }
    }

    /** Stops accepting new tasks; the underlying pool shuts down once running tasks finish. */
    public void shutdown()
    {
        shutdown = true;
        pool.shutdown();
    }

    // ---------------------------------------------------------------- internals

    private static Supplier<Void> toSupplier(Runnable task)
    {
        return () -> { task.run(); return null; };
    }

    private <T> Future<T> submit(Tier tier, Supplier<T> task)
    {
        if (shutdown)
        {
            throw new IllegalStateException("TieredExecutor is shut down");
        }
        QueuedTask<T> queued = new QueuedTask<>(tier, task);
        lock.lock();
        try
        {
            if (!tryStart(queued))
            {
                queue.addLast(queued);
            }
        }
        finally
        {
            lock.unlock();
        }
        return queued.future;
    }

    /** Must hold {@link #lock}. If capacity allows, reserves the slot and hands the task to the pool. */
    private boolean tryStart(QueuedTask<?> task)
    {
        if (!fits(task.tier))
        {
            return false;
        }
        reserve(task.tier);
        pool.execute(() -> run(task));
        return true;
    }

    /** Must hold {@link #lock}. */
    private boolean fits(Tier tier)
    {
        int cpuIoLimit = cpuLimit + ioLimit;
        int total = cpuIoLimit + netLimit;
        switch (tier)
        {
            case CPU:
                return cpuRunning < cpuLimit
                        && (cpuRunning + ioRunning) < cpuIoLimit
                        && (cpuRunning + ioRunning + netRunning) < total;
            case IO:
                return (cpuRunning + ioRunning) < cpuIoLimit
                        && (cpuRunning + ioRunning + netRunning) < total;
            case NET:
                return (cpuRunning + ioRunning + netRunning) < total;
            default:
                throw new AssertionError(tier);
        }
    }

    /** Must hold {@link #lock}. */
    private void reserve(Tier tier)
    {
        switch (tier)
        {
            case CPU: cpuRunning++; break;
            case IO:  ioRunning++;  break;
            case NET: netRunning++; break;
        }
    }

    /** Must hold {@link #lock}. */
    private void release(Tier tier)
    {
        switch (tier)
        {
            case CPU: cpuRunning--; break;
            case IO:  ioRunning--;  break;
            case NET: netRunning--; break;
        }
    }

    private <T> void run(QueuedTask<T> task)
    {
        try
        {
            T result = task.task.get();
            task.future.complete(result);
        }
        catch (Throwable t)
        {
            task.future.completeExceptionally(t);
        }
        finally
        {
            lock.lock();
            try
            {
                release(task.tier);
                dispatchQueued();
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    /**
     * Must hold {@link #lock}. Scans the queue once, front-to-back, starting every task that
     * currently fits and leaving the rest in place, in order. A single forward pass is enough:
     * starting a task can only ever consume capacity, never free it, so a task that doesn't fit
     * at some point in the scan won't fit later in the same scan either.
     */
    private void dispatchQueued()
    {
        Iterator<QueuedTask<?>> it = queue.iterator();
        while (it.hasNext())
        {
            if (tryStart(it.next()))
            {
                it.remove();
            }
        }
    }

    private static final class QueuedTask<T>
    {
        final Tier tier;
        final Supplier<T> task;
        final SettableFuture<T> future = new SettableFuture<>();

        QueuedTask(Tier tier, Supplier<T> task)
        {
            this.tier = tier;
            this.task = task;
        }
    }

    private static final class TieredThreadFactory implements ThreadFactory
    {
        private final AtomicInteger n = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r, "tiered-executor-" + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Minimal {@link Future} whose result can be set from another thread once the task finishes.
     * Cancellation of not-yet-started or running tasks is not supported (always returns false).
     */
    private static final class SettableFuture<T> implements Future<T>
    {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile T value;
        private volatile Throwable error;
        private volatile boolean done = false;

        void complete(T value)
        {
            this.value = value;
            this.done = true;
            latch.countDown();
        }

        void completeExceptionally(Throwable t)
        {
            this.error = t;
            this.done = true;
            latch.countDown();
        }

        @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return done; }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            latch.await();
            return getResultOrThrow();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            if (!latch.await(timeout, unit))
            {
                throw new TimeoutException();
            }
            return getResultOrThrow();
        }

        private T getResultOrThrow() throws ExecutionException
        {
            if (error != null)
            {
                throw new ExecutionException(error);
            }
            return value;
        }
    }
}
