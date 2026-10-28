/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/**
 * Adapts a {@link BlockingQueue} into an {@link Iterable}/{@link Iterator} that
 * terminates when a sentinel ("poison") element is consumed.
 *
 * <p>
 * The poison element must be a non-null value that signals end-of-stream.
 * Producers call {@link #add(Object)} to enqueue items and {@link #poison()} to
 * signal completion.
 *
 * <p>
 * <b>Thread safety:</b> a single consumer thread must drive the iterator;
 * multiple producer threads may call {@link #add} and {@link #poison} safely.
 *
 * <p>
 * <b>Note:</b> this class intentionally does <em>not</em> implement both
 * {@link Iterable} and {@link Iterator} on the same object. {@link #iterator()}
 * returns {@code this}, which means the instance is single-use and must not be
 * iterated more than once.
 *
 * @param <E> element type; must not be {@code null} in the queue
 */
public class IterableQueue<E> implements Iterable<E>, Iterator<E>
{

    // Internal sentinel: decoupled from the user-visible poison reference.
    private static final Object END = new Object();

    private final BlockingQueue<E> queue;
    private final E poison;

    // Prefetched element; END means the stream is exhausted.
    private Object prefetched = null;

    /**
     * @param queue backing blocking queue
     * @param poison non-null sentinel value that signals end-of-stream
     * @throws NullPointerException if either argument is null
     */
    public IterableQueue(BlockingQueue<E> queue, E poison)
    {
        this.queue = Objects.requireNonNull(queue, "queue must not be null");
        this.poison = Objects.requireNonNull(poison, "poison must not be null");
    }

    // -------------------------------------------------------------------------
    // Producer API
    // -------------------------------------------------------------------------
    /**
     * Enqueues an element. Blocks if the queue is full.
     */
    public void add(E element) throws InterruptedException
    {
        Objects.requireNonNull(element, "elements must not be null");
        queue.put(element);
    }

    /**
     * Enqueues the poison pill, signalling end-of-stream to the consumer.
     */
    public void poison() throws InterruptedException
    {
        queue.put(poison);
    }

    // -------------------------------------------------------------------------
    // Iterable / Iterator
    // -------------------------------------------------------------------------
    /**
     * Returns {@code this}. This instance is single-use.
     */
    @Override
    public Iterator<E> iterator()
    {
        return this;
    }

    /**
     * Blocks until the next element is available or the poison pill is
     * received.
     *
     * @throws IllegalStateException if the calling thread is interrupted
     */
    @Override
    public boolean hasNext()
    {
        if (prefetched == END)
        {
            return false;
        }
        if (prefetched != null)
        {
            return true;
        }
        // Prefetch the next element from the queue.
        try
        {
            E taken = queue.take();
            if (taken.equals(poison))
            {
                prefetched = END;
                return false;
            }
            prefetched = taken;
            return true;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();          // restore interrupt flag
            throw new IllegalStateException("Interrupted while waiting for next element", e);
        }
    }

    /**
     * Returns the next element.
     *
     * @throws NoSuchElementException if the stream is exhausted
     * @throws IllegalStateException if the calling thread is interrupted
     */
    @Override
    @SuppressWarnings("unchecked")
    public E next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException("No more elements in the queue");
        }
        E value = (E) prefetched;
        prefetched = null;
        return value;
    }

    /**
     * Not supported.
     */
    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("remove() is not supported");
    }

// package-private — visible only to tests in the same package
    BlockingQueue<E> getQueue()
    {
        return queue;
    }
}
