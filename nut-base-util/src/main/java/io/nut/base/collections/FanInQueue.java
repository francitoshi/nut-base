/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A consume-only {@link BlockingQueue} that implements the fan-in pattern.
 *
 * <p>Elements are not inserted into this queue; instead it reads them from the
 * subscribed queues (the sources). Sources are the queues passed to the
 * constructor plus any queue registered later through
 * {@link #subscribe(BlockingQueue)} or {@link #subscribe(BlockingQueue, Predicate)}.
 * A subscriber predicate filters the elements read from that source: elements
 * that fail the predicate are consumed from the source and discarded, so they
 * never block the head of the source.</p>
 *
 * <p><b>Reading strategy:</b> {@link #take()} and {@link #poll(long, java.util.concurrent.TimeUnit)}
 * sweep all subscribed queues round after round. On every round each source is
 * polled waiting an incremental time: 1 ms on the first round, 2 ms on the
 * second, 3 ms on the third and so on, up to the maximum latency (1000 ms by
 * default, configurable through the constructors that take a
 * {@code maxLatencyMillis} argument) or up to the remaining timeout of the
 * operation that started the reading. {@link #poll()} performs a
 * single non-blocking round and returns the first element found. To avoid
 * starving a source, every round starts at a different queue: the round number
 * (modulo the number of sources) for {@code take} and the timed {@code poll},
 * and {@code counter % (number of sources)} for {@link #poll()}.</p>
 *
 * <p>Sources are registered in a thread-safe copy-on-write list, so
 * {@code subscribe} may be called concurrently with reads.</p>
 *
 * <p><b>Consume-only:</b> this queue never accepts insertions, therefore every
 * insertion operation ({@link #add(Object)}, {@link #put(Object)},
 * {@link #offer(Object)}, ...) throws {@link UnsupportedOperationException}.
 * Producers must write to the subscribed queues instead.</p>
 *
 * @param <E> the type of elements held in this queue
 */
public class FanInQueue<E> implements BlockingQueue<E>
{
    private static final String WRITE_NOT_SUPPORTED = "a fan-in queue is consume-only; write to a subscribed queue instead";

    private static final long NO_TIMEOUT = -1L;

    private static final class Subscriber<E>
    {
        private final BlockingQueue<E> queue;
        private final Predicate<E> predicate;

        Subscriber(BlockingQueue<E> queue, Predicate<E> predicate)
        {
            this.queue = queue;
            this.predicate = predicate;
        }

        boolean accepts(E e)
        {
            return predicate == null || predicate.test(e);
        }
    }

    private static final long DEFAULT_MAX_LATENCY_MILLIS = 1000L;

    private final long maxLatencyMillis;
    private final CopyOnWriteArrayList<Subscriber<E>> subscribers = new CopyOnWriteArrayList<>();
    private final AtomicInteger roundRobin = new AtomicInteger();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition hasSubscribers = lock.newCondition();

    /**
     * Constructs a {@code FanInQueue} with the given initial sources.
     *
     * @param queues the source queues to subscribe initially; must not contain {@code null}
     */
    @SafeVarargs
    public FanInQueue(BlockingQueue<E>... queues)
    {
        this(DEFAULT_MAX_LATENCY_MILLIS, queues);
    }

    /**
     * Constructs a {@code FanInQueue} with the given initial sources.
     *
     * @param queues the source queues to subscribe initially; must not be
     *               {@code null} nor contain {@code null}
     * @throws NullPointerException if {@code queues} is {@code null}
     */
    public FanInQueue(Collection<? extends BlockingQueue<E>> queues)
    {
        this(DEFAULT_MAX_LATENCY_MILLIS, queues);
    }

    /**
     * Constructs a {@code FanInQueue} with the given initial sources and the
     * maximum latency, in milliseconds, waited before beginning the next round.
     *
     * <p>The per-round wait grows from 1 ms on the first round by 1 ms each
     * round until it reaches {@code maxLatencyMillis}, which is then kept for
     * the following rounds.
     *
     * @param maxLatencyMillis the maximum wait in milliseconds before starting
     *                         the next round; must be at least 1
     * @param queues           the source queues to subscribe initially; must not
     *                         contain {@code null}
     * @throws IllegalArgumentException if {@code maxLatencyMillis} is less than 1
     */
    @SafeVarargs
    public FanInQueue(long maxLatencyMillis, BlockingQueue<E>... queues)
    {
        this.maxLatencyMillis = validateLatency(maxLatencyMillis);
        if (queues != null)
        {
            for (BlockingQueue<E> queue : queues)
            {
                subscribe(queue);
            }
        }
    }

    /**
     * Constructs a {@code FanInQueue} with the given initial sources and the
     * maximum latency, in milliseconds, waited before beginning the next round.
     *
     * @param maxLatencyMillis the maximum wait in milliseconds before starting
     *                         the next round; must be at least 1
     * @param queues           the source queues to subscribe initially; must not
     *                         be {@code null} nor contain {@code null}
     * @throws NullPointerException     if {@code queues} is {@code null}
     * @throws IllegalArgumentException if {@code maxLatencyMillis} is less than 1
     */
    public FanInQueue(long maxLatencyMillis, Collection<? extends BlockingQueue<E>> queues)
    {
        this.maxLatencyMillis = validateLatency(maxLatencyMillis);
        Objects.requireNonNull(queues, "queues must not be null");
        for (BlockingQueue<E> queue : queues)
        {
            subscribe(queue);
        }
    }

    private static long validateLatency(long maxLatencyMillis)
    {
        if (maxLatencyMillis < 1)
        {
            throw new IllegalArgumentException("maxLatencyMillis must be at least 1: " + maxLatencyMillis);
        }
        return maxLatencyMillis;
    }

    /**
     * Subscribes the given queue as a source of elements for this queue.
     *
     * @param queue the queue to subscribe; must not be {@code null}
     * @return this queue, for method chaining
     * @throws NullPointerException if {@code queue} is {@code null}
     */
    public final FanInQueue<E> subscribe(BlockingQueue<E> queue)
    {
        subscribers.add(new Subscriber<>(Objects.requireNonNull(queue, "queue must not be null"), null));
        signalSubscriber();
        return this;
    }

    /**
     * Subscribes the given queue as a source of elements for this queue, reading
     * only those elements for which the predicate returns {@code true}. Elements
     * that fail the predicate are consumed from the source and discarded.
     *
     * @param queue     the queue to subscribe; must not be {@code null}
     * @param predicate the filter deciding whether an element read from
     *                  {@code queue} is handed out; must not be {@code null}
     * @return this queue, for method chaining
     * @throws NullPointerException if either argument is {@code null}
     */
    public final FanInQueue<E> subscribe(BlockingQueue<E> queue, Predicate<E> predicate)
    {
        subscribers.add(new Subscriber<>(Objects.requireNonNull(queue, "queue must not be null"),
                                         Objects.requireNonNull(predicate, "predicate must not be null")));
        signalSubscriber();
        return this;
    }

    /** Wakes up any reader blocked waiting for the first subscriber. */
    private void signalSubscriber()
    {
        lock.lock();
        try
        {
            hasSubscribers.signalAll();
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Removes the subscription for the given queue, if present.
     *
     * <p>If the same queue was subscribed more than once (e.g. once with a
     * predicate and once without), only its first subscription is removed.</p>
     *
     * @param queue the queue to unsubscribe; may be {@code null}
     * @return {@code true} if a subscription for the queue was found and removed,
     *         {@code false} otherwise
     */
    public final boolean unsubscribe(BlockingQueue<E> queue)
    {
        if (queue == null)
        {
            return false;
        }
        for (int i = 0; i < subscribers.size(); i++)
        {
            if (subscribers.get(i).queue == queue)
            {
                subscribers.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves and removes the head of this queue, reading from the subscribed
     * queues with the incremental round-robin strategy (up to 1000 ms per round).
     * Blocks until an element is available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public E take() throws InterruptedException
    {
        return takeFromSources(NO_TIMEOUT);
    }

    /**
     * Retrieves and removes the head of this queue, reading from the subscribed
     * queues with the incremental round-robin strategy, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret {@code timeout}
     * @return the head of this queue, or {@code null} if the specified waiting
     *         time elapses
     * @throws NullPointerException  if {@code unit} is {@code null}
     * @throws InterruptedException  if interrupted while waiting
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException
    {
        Objects.requireNonNull(unit, "unit must not be null");
        return takeFromSources(unit.toNanos(timeout));
    }

    /**
     * Retrieves and removes the head of this queue, performing a single
     * non-blocking round over the subscribed queues. Each round starts at a
     * different queue (counter modulo the number of sources) to avoid starving
     * a source.
     *
     * @return the head of this queue, or {@code null} if no element is available
     */
    @Override
    public E poll()
    {
        int n = subscribers.size();
        if (n == 0)
        {
            return null;
        }
        final int start = nextStart(n);
        for (int i = 0; i < n; i++)
        {
            Subscriber<E> subscriber = subscribers.get((start + i) % n);
            E e = subscriber.queue.poll();
            if (e != null && subscriber.accepts(e))
            {
                return e;
            }
        }
        return null;
    }

    /**
     * Retrieves and removes the head of this queue, or throws if this queue
     * (i.e. all sources) is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public E remove()
    {
        E e = poll();
        if (e == null)
        {
            throw new NoSuchElementException();
        }
        return e;
    }

    /**
     * Retrieves, but does not remove, the head of this queue by peeking at the
     * subscribed queues, or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    @Override
    public E peek()
    {
        int n = subscribers.size();
        if (n == 0)
        {
            return null;
        }
        final int start = nextStart(n);
        for (int i = 0; i < n; i++)
        {
            Subscriber<E> subscriber = subscribers.get((start + i) % n);
            E e = subscriber.queue.peek();
            if (e != null && subscriber.accepts(e))
            {
                return e;
            }
        }
        return null;
    }

    /**
     * Retrieves, but does not remove, the head of this queue, or throws if this
     * queue (i.e. all sources) is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public E element()
    {
        E e = peek();
        if (e == null)
        {
            throw new NoSuchElementException();
        }
        return e;
    }

    /**
     * Reads an element from the subscribed queues using the incremental
     * round-robin strategy. Blocks waiting for the first subscriber to be
     * registered if there is none.
     *
     * @param budgetNanos the maximum time to wait in nanoseconds, or a negative
     *                    value to wait indefinitely
     */
    private E takeFromSources(long budgetNanos) throws InterruptedException
    {
        final boolean timed = budgetNanos >= 0;
        final long deadline = timed ? System.nanoTime() + budgetNanos : Long.MAX_VALUE;

        for (int round = 0;; round++)
        {
            List<Subscriber<E>> snapshot = new ArrayList<>(subscribers);
            int n = snapshot.size();
            if (n == 0)
            {
                if (!waitForSubscriber(deadline, timed))
                {
                    return null;
                }
                continue;
            }

            long roundWait = TimeUnit.MILLISECONDS.toNanos(Math.min(round + 1L, maxLatencyMillis));
            for (int i = 0; i < n; i++)
            {
                long wait = roundWait;
                if (timed)
                {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0)
                    {
                        return null;
                    }
                    wait = Math.min(roundWait, remaining);
                }
                Subscriber<E> subscriber = snapshot.get((round + i) % n);
                E e = subscriber.queue.poll(wait, TimeUnit.NANOSECONDS);
                if (e != null && subscriber.accepts(e))
                {
                    return e;
                }
            }
        }
    }

    /**
     * Blocks until at least one subscriber is registered or, for a timed read,
     * until the deadline elapses.
     *
     * @param deadline the absolute deadline in nanoseconds ({@link Long#MAX_VALUE}
     *                 for an unbounded wait)
     * @param timed    whether the wait is bounded by the deadline
     * @return {@code true} if a subscriber is available, {@code false} if the
     *         deadline elapsed while waiting for one
     */
    private boolean waitForSubscriber(long deadline, boolean timed) throws InterruptedException
    {
        lock.lockInterruptibly();
        try
        {
            while (subscribers.isEmpty())
            {
                if (timed)
                {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0)
                    {
                        return false;
                    }
                    hasSubscribers.awaitNanos(remaining);
                }
                else
                {
                    hasSubscribers.await();
                }
            }
            return true;
        }
        finally
        {
            lock.unlock();
        }
    }

    private int nextStart(int n)
    {
        return Math.floorMod(roundRobin.getAndIncrement(), n);
    }

    /**
     * Removes all elements from this queue that are available in the subscribed
     * queues and adds them to the given collection.
     *
     * @param c the collection to transfer elements into
     * @return the number of elements transferred
     * @throws NullPointerException if the specified collection is null
     */
    @Override
    public int drainTo(Collection<? super E> c)
    {
        Objects.requireNonNull(c, "collection must not be null");
        int count = 0;
        E e;
        while ((e = poll()) != null)
        {
            c.add(e);
            count++;
        }
        return count;
    }

    /**
     * Removes at most the given number of elements that are available in the
     * subscribed queues and adds them to the given collection.
     *
     * @param c           the collection to transfer elements into
     * @param maxElements the maximum number of elements to transfer
     * @return the number of elements transferred
     * @throws NullPointerException if the specified collection is null
     */
    @Override
    public int drainTo(Collection<? super E> c, int maxElements)
    {
        Objects.requireNonNull(c, "collection must not be null");
        if (maxElements <= 0)
        {
            return 0;
        }
        int count = 0;
        E e;
        while (count < maxElements && (e = poll()) != null)
        {
            c.add(e);
            count++;
        }
        return count;
    }

    /**
     * Not supported. This queue is consume-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean add(E e)
    {
        throw new UnsupportedOperationException(WRITE_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is consume-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void put(E e)
    {
        throw new UnsupportedOperationException(WRITE_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is consume-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean offer(E e)
    {
        throw new UnsupportedOperationException(WRITE_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is consume-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit)
    {
        throw new UnsupportedOperationException(WRITE_NOT_SUPPORTED);
    }

    /**
     * Returns the total number of elements across all subscribed queues,
     * saturating at {@link Integer#MAX_VALUE}.
     *
     * @return the total number of available elements
     */
    @Override
    public int size()
    {
        long total = 0;
        for (Subscriber<E> subscriber : subscribers)
        {
            total += subscriber.queue.size();
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /** Returns {@code true} if every subscribed queue is empty. */
    @Override
    public boolean isEmpty()
    {
        for (Subscriber<E> subscriber : subscribers)
        {
            if (!subscriber.queue.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    /** Returns {@code true} if any subscribed queue contains the specified element. */
    @Override
    public boolean contains(Object o)
    {
        for (Subscriber<E> subscriber : subscribers)
        {
            if (subscriber.queue.contains(o))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a single instance of the specified element from a subscribed queue,
     * if it is present.
     *
     * @param o object to be removed from this queue, if present
     * @return {@code true} if an element was removed as a result of this call
     */
    @Override
    public boolean remove(Object o)
    {
        for (Subscriber<E> subscriber : subscribers)
        {
            if (subscriber.queue.remove(o))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if every element of the given collection is contained
     * in some subscribed queue.
     *
     * @param c the collection to check
     * @return {@code true} if this queue contains all elements of {@code c}
     * @throws NullPointerException if {@code c} is {@code null}
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        Objects.requireNonNull(c, "collection must not be null");
        for (Object o : c)
        {
            if (!contains(o))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Not supported. This queue is consume-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        throw new UnsupportedOperationException(WRITE_NOT_SUPPORTED);
    }

    /**
     * Removes from the subscribed queues all elements contained in the given
     * collection.
     *
     * @param c the collection containing elements to be removed from this queue
     * @return {@code true} if this queue changed as a result of the call
     */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        Objects.requireNonNull(c, "collection must not be null");
        boolean changed = false;
        for (Subscriber<E> subscriber : subscribers)
        {
            changed |= subscriber.queue.removeAll(c);
        }
        return changed;
    }

    /**
     * Removes from the subscribed queues all elements that satisfy the given
     * predicate.
     *
     * @param filter a predicate which returns {@code true} for elements to be removed
     * @return {@code true} if this queue changed as a result of the call
     * @throws NullPointerException if {@code filter} is {@code null}
     */
    @Override
    public boolean removeIf(Predicate<? super E> filter)
    {
        Objects.requireNonNull(filter, "filter must not be null");
        boolean changed = false;
        for (Subscriber<E> subscriber : subscribers)
        {
            changed |= subscriber.queue.removeIf(filter);
        }
        return changed;
    }

    /**
     * Retains only the elements in the subscribed queues that are contained in
     * the given collection.
     *
     * @param c the collection containing elements to be retained
     * @return {@code true} if this queue changed as a result of the call
     */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        Objects.requireNonNull(c, "collection must not be null");
        boolean changed = false;
        for (Subscriber<E> subscriber : subscribers)
        {
            changed |= subscriber.queue.retainAll(c);
        }
        return changed;
    }

    /** Removes all elements from every subscribed queue. */
    @Override
    public void clear()
    {
        for (Subscriber<E> subscriber : subscribers)
        {
            subscriber.queue.clear();
        }
    }

    /**
     * Returns an iterator over all elements currently held by the subscribed
     * queues, in source order. The iterator is a weakly-consistent snapshot of
     * the sources and never consumes elements.
     *
     * @return an iterator over the available elements
     */
    @Override
    public Iterator<E> iterator()
    {
        return snapshot().iterator();
    }

    /** Returns an array containing all elements currently in the subscribed queues. */
    @Override
    public Object[] toArray()
    {
        return snapshot().toArray();
    }

    /**
     * Returns an array containing all elements currently in the subscribed
     * queues, using the provided array if it is big enough.
     *
     * @param a the array into which the elements are to be stored
     * @param <T> the component type of the array
     * @return an array containing the available elements
     * @throws NullPointerException if {@code a} is {@code null}
     */
    @Override
    public <T> T[] toArray(T[] a)
    {
        Objects.requireNonNull(a, "array must not be null");
        return snapshot().toArray(a);
    }

    /** Returns a {@link Spliterator} over the available elements (a snapshot). */
    @Override
    public Spliterator<E> spliterator()
    {
        return snapshot().spliterator();
    }

    /** Returns a sequential {@link Stream} over the available elements (a snapshot). */
    @Override
    public Stream<E> stream()
    {
        return snapshot().stream();
    }

    /** Returns a parallel {@link Stream} over the available elements (a snapshot). */
    @Override
    public Stream<E> parallelStream()
    {
        return snapshot().parallelStream();
    }

    /**
     * Returns the total remaining capacity of the subscribed queues, saturating
     * at {@link Integer#MAX_VALUE}.
     *
     * @return the total remaining capacity
     */
    @Override
    public int remainingCapacity()
    {
        long total = 0;
        for (Subscriber<E> subscriber : subscribers)
        {
            int capacity = subscriber.queue.remainingCapacity();
            if (capacity >= Integer.MAX_VALUE || total >= Integer.MAX_VALUE)
            {
                return Integer.MAX_VALUE;
            }
            total += capacity;
        }
        return (int) total;
    }

    private List<E> snapshot()
    {
        List<E> all = new ArrayList<>();
        for (Subscriber<E> subscriber : subscribers)
        {
            for (E e : subscriber.queue)
            {
                all.add(e);
            }
        }
        return all;
    }
}