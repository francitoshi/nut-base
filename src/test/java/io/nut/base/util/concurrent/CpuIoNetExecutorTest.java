package io.nut.base.util.concurrent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link CpuIoNetExecutor}.
 *
 * <p>Concurrency-limit tests work by starting more tasks than a given limit allows, blocking all
 * of them on a shared latch so they stay "running" at the same time, and asserting that the
 * observed peak concurrency never crosses the limit. The blocked tasks are then released so
 * queued ones get their turn, proving they do eventually run.
 */
class CpuIoNetExecutorTest
{
    private CpuIoNetExecutor exec;

    @AfterEach
    void tearDown()
    {
        if (exec != null)
        {
            exec.shutdown();
        }
    }

    // ---------------------------------------------------------------- basic execution

    @Test
    @DisplayName("cpu(Supplier) runs the task and returns its result")
    void cpuSupplierReturnsResult() throws Exception
    {
        exec = new CpuIoNetExecutor(1, 1, 1);

        Future<Integer> future = exec.cpu(() -> 2 + 2);

        assertEquals(4, future.get(2, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("io(Runnable) runs the task's side effect")
    void ioRunnableRunsSideEffect() throws Exception
    {
        exec = new CpuIoNetExecutor(1, 1, 1);
        AtomicInteger sideEffect = new AtomicInteger();

        Future<Void> future = exec.io(() -> { sideEffect.incrementAndGet(); });

        future.get(2, TimeUnit.SECONDS);
        assertEquals(1, sideEffect.get());
    }

    @Test
    @DisplayName("net(Supplier) runs the task and returns its result")
    void netSupplierReturnsResult() throws Exception
    {
        exec = new CpuIoNetExecutor(1, 1, 1);

        Future<String> future = exec.net(() -> "pong");

        assertEquals("pong", future.get(2, TimeUnit.SECONDS));
    }

    // ---------------------------------------------------------------- capacity limits

    @Test
    @DisplayName("cpu tier never runs more than 'cpu' tasks at once")
    void cpuTierNeverExceedsItsLimit() throws Exception
    {
        int cpu = 3;
        exec = new CpuIoNetExecutor(cpu, 5, 5);
        int taskCount = cpu * 3;

        ConcurrencyProbe probe = new ConcurrencyProbe(cpu);
        List<Future<Void>> futures = submitBlockingTasks(exec::cpu, taskCount, probe);

        probe.awaitPeakReached();
        Thread.sleep(150); // give any incorrect extra starts a chance to show up
        assertEquals(cpu, probe.peak(), "cpu tier must never exceed its limit");
        assertEquals(cpu, exec.activeCpu());
        assertEquals(taskCount - cpu, exec.pending());

        probe.releaseAll();
        awaitAll(futures);
        assertEquals(0, exec.pending());
    }

    @Test
    @DisplayName("cpu() + io() together never exceed cpu+io")
    void cpuAndIoCombinedNeverExceedTheirLimit() throws Exception
    {
        int cpu = 2, io = 3;
        exec = new CpuIoNetExecutor(cpu, io, 10);
        int combinedLimit = cpu + io;
        int taskCount = combinedLimit * 2;

        ConcurrencyProbe probe = new ConcurrencyProbe(combinedLimit);
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++)
        {
            // alternate cpu/io so both tiers contend for the same shared budget
            futures.add(i % 2 == 0 ? exec.cpu(probe.task()) : exec.io(probe.task()));
        }

        probe.awaitPeakReached();
        Thread.sleep(150);
        assertEquals(combinedLimit, probe.peak(), "cpu+io combined usage must never exceed cpu+io");
        assertEquals(combinedLimit, exec.activeCpu() + exec.activeIo());

        probe.releaseAll();
        awaitAll(futures);
    }

    @Test
    @DisplayName("cpu() + io() + net() together never exceed cpu+io+net")
    void allTiersCombinedNeverExceedTheGrandTotal() throws Exception
    {
        int cpu = 2, io = 2, net = 2;
        exec = new CpuIoNetExecutor(cpu, io, net);
        int total = cpu + io + net;
        int taskCount = total * 2;

        ConcurrencyProbe probe = new ConcurrencyProbe(total);
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++)
        {
            switch (i % 3)
            {
                case 0: futures.add(exec.cpu(probe.task())); break;
                case 1: futures.add(exec.io(probe.task())); break;
                default: futures.add(exec.net(probe.task()));
            }
        }

        probe.awaitPeakReached();
        Thread.sleep(150);
        assertEquals(total, probe.peak(), "total usage must never exceed cpu+io+net");
        assertEquals(total, exec.activeCpu() + exec.activeIo() + exec.activeNet());

        probe.releaseAll();
        awaitAll(futures);
    }

    // ---------------------------------------------------------------- non-blocking submission

    @Test
    @DisplayName("submitting a task never blocks the caller, even when every slot is busy")
    void submitDoesNotBlockWhenExecutorIsFull() throws Exception
    {
        exec = new CpuIoNetExecutor(1, 1, 1);
        CountDownLatch holdLatch = new CountDownLatch(1);

        // saturate all 3 slots with tasks that block until we say so
        exec.cpu(() -> awaitUninterruptibly(holdLatch));
        exec.io(() -> awaitUninterruptibly(holdLatch));
        exec.net(() -> awaitUninterruptibly(holdLatch));
        Thread.sleep(100); // let them actually start running

        long start = System.nanoTime();
        Future<Void> queuedTask = exec.cpu(() -> { });
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs < 500, "submit() should return immediately, took " + elapsedMs + "ms");
        assertFalse(queuedTask.isDone(), "the queued task should not have run yet");

        holdLatch.countDown();
        queuedTask.get(2, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("a queued task starts automatically once a slot frees up")
    void queuedTaskStartsAutomaticallyAfterSlotFrees() throws Exception
    {
        exec = new CpuIoNetExecutor(1, 0, 0);
        CountDownLatch holdLatch = new CountDownLatch(1);
        AtomicInteger secondTaskRan = new AtomicInteger();

        Future<Void> first = exec.cpu(() -> awaitUninterruptibly(holdLatch));
        Thread.sleep(100);
        Future<Void> second = exec.cpu(() -> { secondTaskRan.incrementAndGet(); });

        assertEquals(1, exec.pending());
        assertFalse(second.isDone());

        holdLatch.countDown();
        first.get(2, TimeUnit.SECONDS);
        second.get(2, TimeUnit.SECONDS);

        assertEquals(1, secondTaskRan.get());
        assertEquals(0, exec.pending());
    }

    // ---------------------------------------------------------------- global FIFO fairness

    @Test
    @DisplayName("a queued net task is not starved by a flood of newer cpu tasks")
    void queuedNetTaskIsNotStarvedByNewerCpuTasks() throws Exception
{
        int cpu = 2, io = 0, net = 1;
        exec = new CpuIoNetExecutor(cpu, io, net); // total capacity = 3

        // fill the grand total: 2 cpu slots + 1 net slot. Task A is released alone later, while
        // task B and the initial net task stay blocked, so exactly one slot frees at a time.
        CountDownLatch latchA = new CountDownLatch(1);
        CountDownLatch latchOthers = new CountDownLatch(1);
        Future<Void> taskA = exec.cpu(() -> awaitUninterruptibly(latchA));
        Future<Void> taskB = exec.cpu(() -> awaitUninterruptibly(latchOthers));
        Future<Void> net0 = exec.net(() -> awaitUninterruptibly(latchOthers));
        Thread.sleep(100);
        assertEquals(0, exec.pending());

        // net task N is queued first ...
        AtomicInteger order = new AtomicInteger();
        AtomicInteger netStartOrder = new AtomicInteger(-1);
        Future<Void> queuedNet = exec.net(() -> netStartOrder.set(order.incrementAndGet()));

        // ... then 5 cpu tasks are queued behind it
        AtomicInteger firstCpuStartOrder = new AtomicInteger(-1);
        List<Future<Void>> laterCpuTasks = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            laterCpuTasks.add(exec.cpu(() ->
            {
                firstCpuStartOrder.compareAndSet(-1, order.incrementAndGet());
            }));
        }
        assertEquals(6, exec.pending());

        // free exactly one cpu slot (task A). The freed capacity could go to N (queued first,
        // net tier) or to a cpu task queued after it; global FIFO order must favor N.
        latchA.countDown();
        queuedNet.get(2, TimeUnit.SECONDS);

        // release everything else and let the rest of the cpu backlog drain
        latchOthers.countDown();
        for (Future<Void> f : laterCpuTasks) f.get(3, TimeUnit.SECONDS);
        taskA.get(2, TimeUnit.SECONDS);
        taskB.get(2, TimeUnit.SECONDS);
        net0.get(2, TimeUnit.SECONDS);

        assertTrue(netStartOrder.get() >= 0 && firstCpuStartOrder.get() >= 0, "both the queued net task and a later cpu task should have run");
        assertTrue(netStartOrder.get() < firstCpuStartOrder.get(), "the older queued net task must run before any newer queued cpu task, even though cpu tasks normally get first pick of freed capacity");
    }

    // ---------------------------------------------------------------- error handling

    @Test
    @DisplayName("an exception thrown by a Supplier is reported via ExecutionException")
    void exceptionInSupplierPropagates()
    {
        exec = new CpuIoNetExecutor(1, 1, 1);
        RuntimeException boom = new RuntimeException("boom");

        Future<Object> future = exec.cpu(() -> { throw boom; });

        ExecutionException thrown = assertThrows(ExecutionException.class,
                () -> future.get(2, TimeUnit.SECONDS));
        assertEquals(boom, thrown.getCause());
    }

    @Test
    @DisplayName("an exception thrown by a Runnable is reported via ExecutionException")
    void exceptionInRunnablePropagates()
    {
        exec = new CpuIoNetExecutor(1, 1, 1);
        RuntimeException boom = new RuntimeException("boom");

        Future<Void> future = exec.io(() -> { throw boom; });

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));
        assertEquals(boom, thrown.getCause());
    }

    // ---------------------------------------------------------------- lifecycle

    @Test
    @DisplayName("shutdown() rejects further submissions")
    void shutdownRejectsNewSubmissions()
    {
        exec = new CpuIoNetExecutor(1, 1, 1);

        exec.shutdown();

        assertThrows(IllegalStateException.class, () -> exec.cpu(() -> null));
        assertThrows(IllegalStateException.class, () -> exec.io(() -> { }));
        assertThrows(IllegalStateException.class, () -> exec.net(() -> null));
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Submits {@code count} tasks built from {@code probe.task()} through the given submitter
     * (e.g. {@code exec::cpu}).
     */
    private List<Future<Void>> submitBlockingTasks(Function<Runnable, Future<Void>> submitter, int count, ConcurrencyProbe probe)
    {
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++)
        {
            futures.add(submitter.apply(probe.task()));
        }
        return futures;
    }

    private static void awaitAll(List<Future<Void>> futures) throws Exception
    {
        for (Future<Void> f : futures)
        {
            f.get(3, TimeUnit.SECONDS);
        }
    }

    private static void awaitUninterruptibly(CountDownLatch latch)
    {
        try
        {
            if (!latch.await(5, TimeUnit.SECONDS))
            {
                fail("latch was not released in time");
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            fail("interrupted while waiting on latch");
        }
    }

    /**
     * Tracks how many blocking tasks are running at once and their peak, then lets the test
     * release them all together once the peak has been observed.
     */
    private static final class ConcurrencyProbe
    {
        private final AtomicInteger running = new AtomicInteger();
        private final AtomicInteger peak = new AtomicInteger();
        private final CountDownLatch peakReached;
        private final CountDownLatch releaseLatch = new CountDownLatch(1);

        ConcurrencyProbe(int expectedPeak)
        {
            this.peakReached = new CountDownLatch(expectedPeak);
        }

        Runnable task()
        {
            return () ->
            {
                int now = running.incrementAndGet();
                peak.updateAndGet(m -> Math.max(m, now));
                peakReached.countDown();
                awaitUninterruptibly(releaseLatch);
                running.decrementAndGet();
            };
        }

        void awaitPeakReached() throws InterruptedException
        {
            if (!peakReached.await(2, TimeUnit.SECONDS))
            {
                fail("expected concurrency peak was never reached");
            }
        }

        int peak()
        {
            return peak.get();
        }

        void releaseAll()
        {
            releaseLatch.countDown();
        }
    }
}
