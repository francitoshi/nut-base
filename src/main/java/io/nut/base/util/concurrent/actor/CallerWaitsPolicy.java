/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A {@link RejectedExecutionHandler} that enqueues tasks into the executor's
 * queue with blocking behavior instead of rejecting them. When a task cannot be
 * accepted by a {@link ThreadPoolExecutor} (e.g., when the maximum pool size is
 * reached and the queue is full), this handler causes the calling thread to
 * wait until space becomes available in the queue, then enqueues the task for
 * execution by a pool thread. This contrasts with policies like
 * {@link ThreadPoolExecutor.CallerRunsPolicy}, where the calling thread
 * executes the task directly.
 *
 * <p>
 * This handler ensures that tasks are never rejected as long as the executor is
 * running, at the cost of potentially blocking the calling thread indefinitely
 * if the queue is bounded and remains full. It is particularly useful in
 * scenarios where task rejection is unacceptable, and the caller can afford to
 * wait. Or when the caller can't wait for long tasks to end.
 *
 */
public class CallerWaitsPolicy implements RejectedExecutionHandler
{

    /**
     * Handles a task that cannot be accepted by the {@link ThreadPoolExecutor}
     * by enqueueing it with blocking behavior. If the executor is shut down,
     * the task is rejected with a {@link RejectedExecutionException}.
     * Otherwise, the calling thread blocks until space is available in the
     * queue, and the task is then enqueued for execution by a pool thread.
     *
     * <p>
     * If the calling thread is interrupted while waiting to enqueue the task,
     * the interrupt status is restored, and a
     * {@link RejectedExecutionException} is thrown wrapping the original
     * {@link InterruptedException}.
     *
     * @param r the {@link Runnable} task that was rejected by the executor
     * @param executor the {@link ThreadPoolExecutor} that rejected the task
     * @throws RejectedExecutionException if the executor is shut down or if the
     * thread is interrupted while waiting to enqueue the task
     */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
    {
        try
        {
            // If the executor is shut down, reject the task
            if (executor.isShutdown())
            {
                throw new RejectedExecutionException("Executor is shut down");
            }
            // Block until space is available in the queue
            executor.getQueue().put(r);
            // The task is enqueued and will be executed by a pool thread
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Interrupted while waiting to enqueue task", e);
        }
    }
}
