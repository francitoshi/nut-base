/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A pipeline stage that accumulates individual messages into a pending batch
 * and forwards the entire batch as a single {@code List<T>} message.
 *
 * @param <T> the type of individual messages accumulated into batches
 */
public class BatchActor<T> extends LinkableActor<T,List<T>>
{
    private final int maxSize;
    private final Object batchLock = new Object();
    private List<T> batch;
    private final AtomicLong expectedSeq = new AtomicLong(1);
    private final TreeMap<Long, T> buffered = new TreeMap<>();
    private final ScheduledExecutorService scheduler;

    public BatchActor(ActorHub actorHub, int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        super(null);
        if (maxSize <= 0)
        {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.batch = new ArrayList<>(maxSize);
        ActorHooks<T> hooks = new ActorHooks<T>()
        {
            @Override
            public void receive(T m, long seq)
            {
                BatchActor.this.onReceive(m, seq);
            }

            @Override
            public void terminate()
            {
                BatchActor.this.onTerminate();
            }

            @Override
            public void exception(Exception ex)
            {
                BatchActor.this.handleException(ex);
            }
        };
        this.inner = ActorFlavors.create(actorHub, threads, queueSize, hooks);
        if (maxWaitMillis > 0)
        {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(BatchActor::newDaemonThread);
            this.scheduler.scheduleWithFixedDelay(this::flush, maxWaitMillis, maxWaitMillis, TimeUnit.MILLISECONDS);
        }
        else
        {
            this.scheduler = null;
        }
    }

    public BatchActor(ActorHub actorHub, int threads, int maxSize, long maxWaitMillis)
    {
        this(actorHub, threads, 0, maxSize, maxWaitMillis);
    }

    public BatchActor(ActorHub actorHub, int maxSize, long maxWaitMillis)
    {
        this(actorHub, 1, 0, maxSize, maxWaitMillis);
    }

    public BatchActor(int threads, int maxSize, long maxWaitMillis)
    {
        this(null, threads, 0, maxSize, maxWaitMillis);
    }

    public BatchActor(int maxSize, long maxWaitMillis)
    {
        this(null, 1, 0, maxSize, maxWaitMillis);
    }

    private static Thread newDaemonThread(Runnable r)
    {
        Thread t = new Thread(r, "BatchActor-flush-timer");
        t.setDaemon(true);
        return t;
    }

    private List<T> addInOrder(T m, long seq)
    {
        synchronized (batchLock)
        {
            if (seq == expectedSeq.get())
            {
                batch.add(m);
                expectedSeq.incrementAndGet();
                while (true)
                {
                    T next = buffered.remove(expectedSeq.get());
                    if (next == null)
                    {
                        break;
                    }
                    batch.add(next);
                    expectedSeq.incrementAndGet();
                }
                if (batch.size() >= maxSize)
                {
                    List<T> full = batch;
                    batch = new ArrayList<>(maxSize);
                    return full;
                }
            }
            else if (seq > expectedSeq.get())
            {
                buffered.put(seq, m);
            }
            return null;
        }
    }

    private void onReceive(T m, long seq)
    {
        List<T> full = addInOrder(m, seq);
        if (full != null)
        {
            forward(full);
        }
    }

    public void flush()
    {
        List<T> pending = null;
        synchronized (batchLock)
        {
            if (!batch.isEmpty())
            {
                pending = batch;
                batch = new ArrayList<>(maxSize);
            }
        }
        if (pending != null)
        {
            forward(pending);
        }
    }

    public int pending()
    {
        synchronized (batchLock)
        {
            return batch.size();
        }
    }

    private void onTerminate()
    {
        flush();
        if (scheduler != null)
        {
            scheduler.shutdownNow();
        }
    }
}
