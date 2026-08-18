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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for the generic N-level {@link TieredExecutor}.
 *
 * <p>Concurrency-limit tests work by starting more tasks than a given limit allows, blocking all
 * of them on a shared latch so they stay "running" at the same time, and asserting that the
 * observed peak concurrency never crosses the limit. The blocked tasks are then released so
 * queued ones get their turn, proving they do eventually run.
 */
class TieredExecutorTest
{
    private TieredExecutor exec;

    @AfterEach
    void tearDown()
    {
        if (exec != null)
        {
            exec.shutdown();
        }
    }

    // ---------------------------------------------------------------- construction

    @Test
    @DisplayName("validates constructor arguments strictly")
    void testConstructorValidation()
    {
        assertThrows(NullPointerException.class, () -> new TieredExecutor((int[]) null));
        assertThrows(IllegalArgumentException.class, () -> new TieredExecutor());
        assertThrows(IllegalArgumentException.class, () -> new TieredExecutor(0));
        assertThrows(IllegalArgumentException.class, () -> new TieredExecutor(2, -1, 8));
        assertThrows(IllegalArgumentException.class, () -> new TieredExecutor(0, 0, 0));
        assertDoesNotThrow(() -> new TieredExecutor(0, 4, 0));
    }

    @Test
    @DisplayName("rejects levels outside the configured range")
    void testLevelBoundsValidation()
    {
        exec = new TieredExecutor(1, 1, 1);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> exec.submit(-1, () -> { }));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> exec.submit(3, () -> { }));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> exec.active(-1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> exec.active(3));
    }

    // ---------------------------------------------------------------- basic execution

    @Test
    @DisplayName("tasks run at every level and report their result and level activity")
    void levelsRunAndReturnResults() throws Exception
    {
        exec = new TieredExecutor(1, 2, 1, 3);
        assertEquals(4, exec.levelCount());
        assertArrayEquals(new int[]{1, 2, 1, 3}, exec.getLimits());

        Future<String> f0 = exec.submit(0, () -> "l0");
        Future<Integer> f1 = exec.submit(1, () -> 42);
        Future<String> f2 = exec.submit(2, () -> "l2");
        Future<Boolean> f3 = exec.submit(3, () -> true);

        assertEquals("l0", f0.get(2, TimeUnit.SECONDS));
        assertEquals(42, f1.get(2, TimeUnit.SECONDS));
        assertEquals("l2", f2.get(2, TimeUnit.SECONDS));
        assertTrue(f3.get(2, TimeUnit.SECONDS));
        assertEquals(0, exec.pending());
        for (int i = 0; i < 4; i++)
        {
            assertEquals(0, exec.active(i));
        }
    }

    // ---------------------------------------------------------------- capacity limits

    @Test
    @DisplayName("level 0 never runs more than limits[0] tasks at once")
    void levelZeroCapsAtItsOwnLimit() throws Exception
    {
        exec = new TieredExecutor(3, 5, 5, 5);
        int limit = 3;
        int taskCount = limit * 3;

        ConcurrencyProbe probe = new ConcurrencyProbe(limit);
        List<Future<Void>> futures = submitBlockingTasks(0, taskCount, probe);

        probe.awaitPeakReached();
        Thread.sleep(150);
        assertEquals(limit, probe.peak(), "level 0 must never exceed its own limit");
        assertEquals(limit, exec.active(0));
        assertEquals(taskCount - limit, exec.pending());

        probe.releaseAll();
        awaitAll(futures);
        assertEquals(0, exec.pending());
    }

    @Test
    @DisplayName("levels 0 and 1 together never exceed limits[0]+limits[1]")
    void lowerPrefixCombinedNeverExceedsItsLimit() throws Exception
    {
        exec = new TieredExecutor(2, 3, 1, 4);
        int combinedLimit = 5;
        int taskCount = combinedLimit * 2;

        ConcurrencyProbe probe = new ConcurrencyProbe(combinedLimit);
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++)
        {
            futures.add(exec.submit(i % 2, probe.task()));
        }

        probe.awaitPeakReached();
        Thread.sleep(150);
        assertEquals(combinedLimit, probe.peak(), "levels 0+1 combined usage must never exceed limits[0]+limits[1]");
        assertEquals(combinedLimit, exec.active(0) + exec.active(1));

        probe.releaseAll();
        awaitAll(futures);
    }

    @Test
    @DisplayName("all levels together never exceed the grand total")
    void allLevelsCombinedNeverExceedTheGrandTotal() throws Exception
    {
        exec = new TieredExecutor(2, 3, 1, 4);
        int total = 10;
        int taskCount = total * 2;

        ConcurrencyProbe probe = new ConcurrencyProbe(total);
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++)
        {
            futures.add(exec.submit(i % 4, probe.task()));
        }

        probe.awaitPeakReached();
        Thread.sleep(150);
        assertEquals(total, probe.peak(), "total usage must never exceed the sum of all limits");

        probe.releaseAll();
        awaitAll(futures);
    }

    @Test
    @DisplayName("the highest level can use every idle slot of the lower levels")
    void topLevelUsesEveryIdleSlot() throws Exception
    {
        exec = new TieredExecutor(2, 3, 1, 4);
        int total = 10;

        ConcurrencyProbe probe = new ConcurrencyProbe(total);
        List<Future<Void>> futures = submitBlockingTasks(3, total * 2, probe);

        probe.awaitPeakReached();
        Thread.sleep(150);
        assertEquals(total, probe.peak(), "the top level may run up to the grand total");

        probe.releaseAll();
        awaitAll(futures);
    }

    // ---------------------------------------------------------------- non-blocking submission

    @Test
    @DisplayName("submitting a task never blocks the caller, even when every slot is busy")
    void submitDoesNotBlockWhenExecutorIsFull() throws Exception
    {
        exec = new TieredExecutor(1, 1, 1, 1);
        CountDownLatch holdLatch = new CountDownLatch(1);

        for (int i = 0; i < 4; i++)
        {
            exec.submit(i, () -> awaitUninterruptibly(holdLatch));
        }
        Thread.sleep(100);

        long start = System.nanoTime();
        Future<Void> queuedTask = exec.submit(0, () -> { });
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
        exec = new TieredExecutor(1, 0, 0, 0);
        CountDownLatch holdLatch = new CountDownLatch(1);
        AtomicInteger secondTaskRan = new AtomicInteger();

        Future<Void> first = exec.submit(0, () -> awaitUninterruptibly(holdLatch));
        Thread.sleep(100);
        Future<Void> second = exec.submit(0, () -> { secondTaskRan.incrementAndGet(); });

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
    @DisplayName("a queued higher-level task is not starved by a flood of newer lower-level tasks")
    void queuedHigherLevelTaskIsNotStarvedByNewerLowerTasks() throws Exception
    {
        exec = new TieredExecutor(2, 0, 1, 0); // grand total = 3

        // fill the grand total: 2 level-0 slots + 1 level-2 slot. Task A is released alone later,
        // while task B and the initial level-2 task stay blocked, so exactly one slot frees at a time.
        CountDownLatch latchA = new CountDownLatch(1);
        CountDownLatch latchOthers = new CountDownLatch(1);
        Future<Void> taskA = exec.submit(0, () -> awaitUninterruptibly(latchA));
        Future<Void> taskB = exec.submit(0, () -> awaitUninterruptibly(latchOthers));
        Future<Void> high0 = exec.submit(2, () -> awaitUninterruptibly(latchOthers));
        Thread.sleep(100);
        assertEquals(0, exec.pending());

        // high-level task N is queued first ...
        AtomicInteger order = new AtomicInteger();
        AtomicInteger highStartOrder = new AtomicInteger(-1);
        Future<Void> queuedHigh = exec.submit(2, () ->
        {
            highStartOrder.set(order.incrementAndGet());
        });

        // ... then 5 level-0 tasks are queued behind it
        AtomicInteger firstLowStartOrder = new AtomicInteger(-1);
        List<Future<Void>> laterLowTasks = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            laterLowTasks.add(exec.submit(0, () ->
            {
                firstLowStartOrder.compareAndSet(-1, order.incrementAndGet());
            }));
        }
        assertEquals(6, exec.pending());

        latchA.countDown();
        queuedHigh.get(2, TimeUnit.SECONDS);

        latchOthers.countDown();
        for (Future<Void> f : laterLowTasks) f.get(3, TimeUnit.SECONDS);
        taskA.get(2, TimeUnit.SECONDS);
        taskB.get(2, TimeUnit.SECONDS);
        high0.get(2, TimeUnit.SECONDS);

        assertTrue(highStartOrder.get() >= 0 && firstLowStartOrder.get() >= 0,
                "both the queued high-level task and a later low-level task should have run");
        assertTrue(highStartOrder.get() < firstLowStartOrder.get(),
                "the older queued high-level task must run before any newer queued low-level task");
    }

    // ---------------------------------------------------------------- error handling

    @Test
    @DisplayName("an exception thrown by a Supplier is reported via ExecutionException")
    void exceptionInSupplierPropagates()
    {
        exec = new TieredExecutor(1, 1, 1, 1);
        RuntimeException boom = new RuntimeException("boom");

        Future<Object> future = exec.submit(1, () -> { throw boom; });

        ExecutionException thrown = assertThrows(ExecutionException.class,
                () -> future.get(2, TimeUnit.SECONDS));
        assertEquals(boom, thrown.getCause());
    }

    // ---------------------------------------------------------------- lifecycle

    @Test
    @DisplayName("shutdown() rejects further submissions")
    void shutdownRejectsNewSubmissions()
    {
        exec = new TieredExecutor(1, 1, 1, 1);

        exec.shutdown();

        assertThrows(IllegalStateException.class, () -> exec.submit(0, () -> null));
        assertThrows(IllegalStateException.class, () -> exec.submit(2, () -> { }));
    }

    // ---------------------------------------------------------------- helpers

    private List<Future<Void>> submitBlockingTasks(int level, int count, ConcurrencyProbe probe)
    {
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++)
        {
            futures.add(exec.submit(level, probe.task()));
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