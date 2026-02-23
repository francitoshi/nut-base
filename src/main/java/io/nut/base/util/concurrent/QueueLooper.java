/*
 * QueueLooper.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages concurrent, queue-driven execution of a task using a fixed number of
 * worker threads.
 *
 * <p>
 * Workers are launched on demand when elements are present in the queue, and
 * keep running until the queue is empty. The number of concurrent workers is
 * capped at {@code threads}. If all worker slots are occupied, new
 * {@link #execute()} calls are ignored: active workers will continue draining
 * the queue.
 *
 * <p>
 * With {@code threads = 1} (default), execution is fully sequential and
 * ordered. With {@code threads > 1}, multiple workers compete for elements
 * concurrently; in this case the task must be thread-safe and order of
 * processing is not guaranteed.
 *
 * <p>
 * If the task throws an exception, it is logged and the worker continues
 * processing remaining elements without interruption.
 *
 * <p>
 * <strong>Contract:</strong> elements should be added via {@link #put(Object)},
 * which enqueues the element and automatically triggers execution. If elements
 * are added directly to the queue, {@link #execute()} must be called afterwards
 * to ensure processing is triggered.
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * BlockingQueue<String> queue = new LinkedBlockingQueue<>();
 * QueueLooper<String> looper = new QueueLooper<>(queue, () -> {
 *     String item = queue.poll();
 *     if (item != null) process(item);
 * });
 *
 * looper.put("hello");
 * looper.put("world");
 * }</pre>
 *
 * @param <E> the type of elements held in the queue.
 */
public class QueueLooper<E>
{

    private final ExecutorService executor;
    private final BlockingQueue<E> queue;
    private final Runnable task;
    private final int threads;

    // Tracks the number of currently active worker threads
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    /**
     * Constructs a new {@code QueueLooper} with the specified number of worker
     * threads.
     *
     * @param queue the queue that drives execution; workers run while it is not
     * empty.
     * @param task the task to execute repeatedly until the queue is empty. The
     * task is responsible for consuming elements from the queue (e.g. via
     * {@link BlockingQueue#poll()}). With {@code threads > 1}, the task must be
     * thread-safe.
     * @param threads the maximum number of concurrent worker threads. Must be
     * >= 1.
     * @throws IllegalArgumentException if {@code threads} is less than 1.
     */
    public QueueLooper(BlockingQueue<E> queue, Runnable task, int threads)
    {
        if (threads < 1)
        {
            throw new IllegalArgumentException("threads must be >= 1");
        }
        this.queue = queue;
        this.task = task;
        this.threads = threads;
        this.executor = Executors.newFixedThreadPool(threads);
    }

    /**
     * Constructs a new {@code QueueLooper} with a single worker thread. In this
     * mode execution is fully sequential: tasks run one at a time in submission
     * order.
     *
     * @param queue the queue that drives execution.
     * @param task the task to execute repeatedly until the queue is empty.
     */
    public QueueLooper(BlockingQueue<E> queue, Runnable task)
    {
        this(queue, task, 1);
    }

    private final Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                while (!queue.isEmpty())
                {
                    try
                    {
                        task.run();
                    }
                    catch (Exception ex)
                    {
                        // Log and continue: a failing task must not stop queue draining
                        Logger.getLogger(QueueLooper.class.getName()).log(Level.SEVERE, "error in task", ex);
                    }
                }
            }
            finally
            {
                // Release worker slot before checking the queue to avoid missing elements
                // added between the while condition and this point
                activeWorkers.decrementAndGet();
                if (!queue.isEmpty())
                {
                    execute();
                }
            }
        }
    };

    /**
     * Submits a new worker for execution if the queue is not empty and the
     * active worker count is below the configured limit.
     *
     * <p>
     * If all worker slots are already occupied, this call is a no-op: active
     * workers will continue draining any elements present in the queue.
     *
     * <p>
     * Prefer using {@link #put(Object)} over adding to the queue directly and
     * calling this method, to ensure execution is always triggered after
     * insertion.
     */
    public void execute()
    {
        if (!queue.isEmpty())
        {
            // Only decrement if we were the ones who incremented
            if (activeWorkers.incrementAndGet() <= threads)
            {
                executor.submit(runnable);
            }
            else
            {
                activeWorkers.decrementAndGet();
            }
        }
    }

    /**
     * Inserts the specified element into the queue, blocking if necessary until
     * space is available, and triggers task execution afterwards.
     *
     * @param e the element to add.
     * @throws InterruptedException if interrupted while waiting for space in
     * the queue.
     */
    public void put(E e) throws InterruptedException
    {
        queue.put(e);
        execute();
    }

    public boolean add(E e)
    {
        boolean modified = queue.add(e);
        if(modified)
        {
            execute();
        }
        return modified;
    }

    public boolean addAll(Collection<? extends E> clctn)
    {
        boolean modified = queue.addAll(clctn);
        if(modified)
        {
            execute();
        }
        return modified;
    }

}
