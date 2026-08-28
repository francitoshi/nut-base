/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Hive#spawn(Runnable)}.
 *
 * <p>The contract under test:
 * <ol>
 *   <li>{@code spawn()} returns only after the worker thread is guaranteed to
 *       be executing {@code task.run()} as its very next instruction.</li>
 *   <li>The task runs on a pool worker thread, not on the calling thread.</li>
 *   <li>The calling thread is not blocked for the duration of the task —
 *       only for the handshake.</li>
 *   <li>A null task is rejected with {@link NullPointerException}.</li>
 *   <li>On interruption the task is not lost (fallback to {@code execute}).</li>
 * </ol>
 */
public class HiveSpawnTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = new Hive();
    }

    @AfterEach
    void tearDown()
    {
        hive.close();
    }

    // -------------------------------------------------------------------------
    // Contract: spawn() rejects null
    // -------------------------------------------------------------------------

    @Test
    void spawn_nullTask_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> hive.spawn(null));
    }

    // -------------------------------------------------------------------------
    // Contract: task is actually executed
    // -------------------------------------------------------------------------

    /**
     * The task must run eventually — basic sanity check.
     */
    @Test
    void spawn_taskIsExecuted() throws InterruptedException
    {
        CountDownLatch done = new CountDownLatch(1);

        hive.spawn(done::countDown);

        assertTrue(done.await(5, TimeUnit.SECONDS), "Task was never executed");
    }

    // -------------------------------------------------------------------------
    // Contract: task runs on a pool thread, not the calling thread
    // -------------------------------------------------------------------------

    /**
     * The thread executing the task must not be the calling thread.
     */
    @Test
    void spawn_taskRunsOnPoolThread() throws InterruptedException
    {
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        hive.spawn(() ->
        {
            workerThread.set(Thread.currentThread());
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertNotSame(callerThread, workerThread.get(), "Task must run on a pool thread, not the calling thread");
    }

    // -------------------------------------------------------------------------
    // Contract: spawn() returns before the task finishes
    // -------------------------------------------------------------------------

    /**
     * spawn() must return while the task is still running.
     * We verify this by using a long-running task and checking that spawn()
     * returns well before the task completes.
     */
    @Test
    void spawn_returnsBeforeTaskFinishes() throws InterruptedException
    {
        CountDownLatch taskStarted  = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);
        AtomicBoolean spawnReturnedBeforeTaskFinished = new AtomicBoolean(false);

        hive.spawn(() ->
        {
            taskStarted.countDown();
            try
            {
                // Hold the task open long enough for the assertion below.
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            taskFinished.countDown();
        });

        // At this point spawn() has already returned (it only waits for the
        // handshake, not for task completion).
        spawnReturnedBeforeTaskFinished.set(taskFinished.getCount() > 0);

        assertTrue(taskStarted.await(5, TimeUnit.SECONDS),
                "Task never signalled it started");
        assertTrue(spawnReturnedBeforeTaskFinished.get(),
                "spawn() must return before the task finishes");

        // Wait for the task to complete so tearDown() does not cut it short.
        assertTrue(taskFinished.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // Contract: worker is running when spawn() returns (the core guarantee)
    // -------------------------------------------------------------------------

    /**
     * The core invariant: by the time spawn() returns, the worker thread has
     * already called task.run() — proven by the fact that the task has
     * incremented a counter before spawn() returns.
     *
     * We use a task that (a) records its start instantly, then (b) parks
     * itself. spawn() must return only after (a) has happened.
     */
    @Test
    void spawn_workerIsRunningWhenSpawnReturns() throws InterruptedException
    {
        AtomicBoolean taskHasStarted = new AtomicBoolean(false);
        CountDownLatch taskCanFinish = new CountDownLatch(1);

        hive.spawn(() ->
        {
            taskHasStarted.set(true);      // (a) mark start immediately
            try
            {
                taskCanFinish.await();     // (b) park until released
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });

        // spawn() has returned — the worker must already be past (a).
        assertTrue(taskHasStarted.get(),
                "Worker must have started task.run() before spawn() returns");

        taskCanFinish.countDown();         // release the parked worker
    }

    // -------------------------------------------------------------------------
    // Contract: calling thread is not blocked for task duration
    // -------------------------------------------------------------------------

    /**
     * After spawn() returns the calling thread is free to do other work
     * concurrently with the task.
     */
    @Test
    void spawn_callerContinuesConcurrentlyWithTask() throws InterruptedException
    {
        CountDownLatch taskRunning  = new CountDownLatch(1);
        CountDownLatch callerAck    = new CountDownLatch(1);
        CountDownLatch taskCanExit  = new CountDownLatch(1);

        hive.spawn(() ->
        {
            taskRunning.countDown();       // signal: task is running
            try
            {
                callerAck.await(5, TimeUnit.SECONDS); // wait for caller ack
                taskCanExit.countDown();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });

        // Caller is free; it can interact with the running task.
        assertTrue(taskRunning.await(5, TimeUnit.SECONDS),
                "Task should be running concurrently");
        callerAck.countDown();             // prove caller is alive and responsive

        assertTrue(taskCanExit.await(5, TimeUnit.SECONDS),
                "Task should have finished after caller acknowledged");
    }

    // -------------------------------------------------------------------------
    // Contract: multiple consecutive spawns all execute
    // -------------------------------------------------------------------------

    @Test
    void spawn_multipleConsecutiveTasks_allExecuted() throws InterruptedException
    {
        int count = 10;
        CountDownLatch done = new CountDownLatch(count);

        for (int i = 0; i < count; i++)
        {
            hive.spawn(done::countDown);
        }

        assertTrue(done.await(10, TimeUnit.SECONDS),
                "Not all spawned tasks were executed");
    }

    // -------------------------------------------------------------------------
    // Contract: interruption during await falls back to execute (task not lost)
    // -------------------------------------------------------------------------

    /**
     * If the calling thread is interrupted while waiting for the handshake,
     * the task must still run via the execute() fallback — no work is lost.
     */
    @Test
    void spawn_interruptedCaller_taskNotLost() throws InterruptedException
    {
        CountDownLatch taskDone = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        Thread caller = new Thread(() ->
        {
            // Self-interrupt before spawn so await() sees the flag immediately.
            Thread.currentThread().interrupt();
            hive.spawn(taskDone::countDown);
            interrupted.set(Thread.currentThread().isInterrupted());
        });

        caller.start();
        caller.join(5_000);

        assertTrue(taskDone.await(5, TimeUnit.SECONDS),
                "Task must not be lost when caller is interrupted");
        assertTrue(interrupted.get(),
                "Interrupt flag must be restored on the calling thread");
    }
}
