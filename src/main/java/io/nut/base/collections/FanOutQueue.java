/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A broadcast-only {@link BlockingQueue} that implements the fan-out pattern.
 *
 * <p>Every element added to this queue is distributed to each subscribed queue
 * that accepts it. Subscribers are the queues passed to the constructor plus any
 * queue registered later through {@link #subscribe(BlockingQueue)} or
 * {@link #subscribe(BlockingQueue, Predicate)}; the latter only forwards elements
 * that satisfy the given predicate.</p>
 *
 * <p>Subscribers are registered in a thread-safe copy-on-write list, so
 * {@code subscribe} may be called concurrently with insertions.</p>
 *
 * <p><b>Distribution limit:</b> when constructed with a {@code limit} greater
 * than zero, each element is delivered to no more than {@code limit} subscribed
 * queues. With {@code limit == 0} every accepting subscriber receives the
 * element. When the number of subscribers exceeds {@code limit}, the queues are
 * ordered by their current size (fewest elements first), then by the number of
 * messages they have already received (fewest messages first), any remaining
 * ties broken at random, and delivery is attempted on the smallest, least-served
 * queues until {@code limit} have accepted the element. If a selected queue is at
 * capacity (a queue without a predicate, or one whose predicate passed), the
 * delivery is completed with the remaining queues that have not yet received the
 * element. Consequently each element can reach {@code limit} queues, fewer, or
 * even none if every selected queue is filtered out by a predicate.</p>
 *
 * <p><b>Broadcast-only:</b> this queue never buffers elements, therefore every
 * retrieval operation ({@link #take()}, {@link #poll()}, {@link #peek()}, ...)
 * throws {@link UnsupportedOperationException}. Consumers must read from the
 * subscribed queues instead.</p>
 *
 * <p><b>Partial delivery:</b> fan-out is not atomic. If a subscriber rejects an
 * element ({@link #offer(Object)} returns {@code false} or {@link #add(Object)}
 * throws), some earlier subscribers may already have received it. In limited
 * mode delivery is always non-blocking.</p>
 *
 * @param <E> the type of elements held in this queue
 */
public class FanOutQueue<E> implements BlockingQueue<E>
{
    private static final String READ_NOT_SUPPORTED = "a fan-out queue is broadcast-only; read from a subscribed queue instead";

    private static final class Subscriber<E>
    {
        private final BlockingQueue<E> queue;
        private final Predicate<E> predicate;
        private final AtomicLong counter = new AtomicLong();

        Subscriber(BlockingQueue<E> queue, Predicate<E> predicate)
        {
            this.queue = queue;
            this.predicate = predicate;
        }

        boolean accepts(E e)
        {
            return predicate == null || predicate.test(e);
        }

        /**
         * Number of messages successfully delivered to this subscriber.
         */
        long count()
        {
            return counter.get();
        }

        void increment()
        {
            counter.incrementAndGet();
        }
    }

    private final int limit;
    private final CopyOnWriteArrayList<Subscriber<E>> subscribers = new CopyOnWriteArrayList<>();

    /**
     * Constructs a {@code FanOutQueue} with the given initial subscribers.
     *
     * @param queues the queues to subscribe initially; must not contain {@code null}
     */
    @SafeVarargs
    public FanOutQueue(BlockingQueue<E>... queues)
    {
        this(0, queues);
    }

    /**
     * Constructs a {@code FanOutQueue} with the given initial subscribers.
     *
     * @param queues the queues to subscribe initially; must not be {@code null}
     *               nor contain {@code null}
     * @throws NullPointerException if {@code queues} is {@code null}
     */
    public FanOutQueue(Collection<? extends BlockingQueue<E>> queues)
    {
        this(0, queues);
    }

    /**
     * Constructs a {@code FanOutQueue} with the given initial subscribers and
     * the maximum number of queues ({@code limit}) that receive each element.
     *
     * @param limit  the maximum number of queues per element; {@code 0} means
     *               no limit (every accepting subscriber receives the element);
     *               must not be negative
     * @param queues the queues to subscribe initially; must not contain {@code null}
     * @throws IllegalArgumentException if {@code limit} is negative
     */
    @SafeVarargs
    public FanOutQueue(int limit, BlockingQueue<E>... queues)
    {
        this.limit = validateLimit(limit);
        if (queues != null)
        {
            for (BlockingQueue<E> queue : queues)
            {
                subscribe(queue);
            }
        }
    }

    /**
     * Constructs a {@code FanOutQueue} with the given initial subscribers and
     * the maximum number of queues ({@code limit}) that receive each element.
     *
     * @param limit  the maximum number of queues per element; {@code 0} means
     *               no limit (every accepting subscriber receives the element);
     *               must not be negative
     * @param queues the queues to subscribe initially; must not be {@code null}
     *               nor contain {@code null}
     * @throws NullPointerException     if {@code queues} is {@code null}
     * @throws IllegalArgumentException if {@code limit} is negative
     */
    public FanOutQueue(int limit, Collection<? extends BlockingQueue<E>> queues)
    {
        this.limit = validateLimit(limit);
        Objects.requireNonNull(queues, "queues must not be null");
        for (BlockingQueue<E> queue : queues)
        {
            subscribe(queue);
        }
    }

    private static int validateLimit(int limit)
    {
        if (limit < 0)
        {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        return limit;
    }

    /**
     * Subscribes the given queue to receive every element added to this queue.
     *
     * @param queue the queue to subscribe; must not be {@code null}
     * @return this queue, for method chaining
     * @throws NullPointerException if {@code queue} is {@code null}
     */
    public final FanOutQueue<E> subscribe(BlockingQueue<E> queue)
    {
        subscribers.add(new Subscriber<>(Objects.requireNonNull(queue, "queue must not be null"), null));
        return this;
    }

    /**
     * Subscribes the given queue to receive only those elements for which the
     * predicate returns {@code true}. Elements that fail the predicate are
     * silently skipped for this subscriber.
     *
     * @param queue     the queue to subscribe; must not be {@code null}
     * @param predicate the filter deciding whether an element is forwarded to
     *                  {@code queue}; must not be {@code null}
     * @return this queue, for method chaining
     * @throws NullPointerException if either argument is {@code null}
     */
    public final FanOutQueue<E> subscribe(BlockingQueue<E> queue, Predicate<E> predicate)
    {
        subscribers.add(new Subscriber<>(Objects.requireNonNull(queue, "queue must not be null"),
                                         Objects.requireNonNull(predicate, "predicate must not be null")));
        return this;
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
     * Returns {@code true} if the distribution limit applies, i.e. there are
     * more subscribers than {@code limit} and {@code limit} is positive.
     */
    private boolean limited()
    {
        return limit > 0 && subscribers.size() > limit;
    }

    /**
     * Returns the subscribers ordered by current queue size ascending, then by
     * the number of messages already delivered (the counter) ascending, breaking
     * any remaining ties at random, so that delivery prioritizes the emptiest
     * and least-served queues.
     */
    private List<Subscriber<E>> orderedSubscribers()
    {
        List<Subscriber<E>> ordered = new ArrayList<>(subscribers);
        Collections.shuffle(ordered);
        ordered.sort(Comparator.comparingInt((Subscriber<E> s) -> s.queue.size()).thenComparingLong(Subscriber::count));
        return ordered;
    }

    /**
     * Delivers the element non-blocking to at most {@code limit} subscribers,
     * walking the queues ordered by size (fewest elements first) and then by
     * delivery counter (fewest messages first), ties broken at random. Queues
     * that fail a predicate or are at capacity are skipped and the delivery is
     * completed with the remaining queues until {@code limit} have accepted the
     * element or every queue has been tried.
     *
     * @return {@code true} if the element was delivered to at least one queue
     */
    private boolean deliverLimited(E e)
    {
        int delivered = 0;
        for (Subscriber<E> subscriber : orderedSubscribers())
        {
            if (delivered >= limit)
            {
                break;
            }
            if (subscriber.accepts(e) && subscriber.queue.offer(e))
            {
                subscriber.increment();
                delivered++;
            }
        }
        return delivered > 0;
    }

    /**
     * Adds the element to every subscribed queue that accepts it, throwing
     * {@link IllegalStateException} if any of them is at capacity.
     *
     * <p>When the distribution limit applies, the element is delivered
     * non-blocking to at most {@code limit} subscribed queues instead.
     *
     * @param e the element to add; must not be {@code null}
     * @return {@code true}; in limited mode {@code true} if the element was
     *         delivered to at least one queue
     * @throws NullPointerException     if {@code e} is {@code null}
     * @throws IllegalStateException    if a subscriber is at capacity
     */
    @Override
    public boolean add(E e)
    {
        Objects.requireNonNull(e, "element must not be null");
        if (limited())
        {
            return deliverLimited(e);
        }
        for (Subscriber<E> subscriber : subscribers)
        {
            if (subscriber.accepts(e))
            {
                subscriber.queue.add(e);
                subscriber.increment();
            }
        }
        return true;
    }

    /**
     * Adds the element to every subscribed queue that accepts it, waiting if
     * necessary for space to become available in each of them.
     *
     * <p>When the distribution limit applies, the element is delivered
     * non-blocking to at most {@code limit} subscribed queues instead.
     *
     * @param e the element to add; must not be {@code null}
     * @throws NullPointerException  if {@code e} is {@code null}
     * @throws InterruptedException  if interrupted while waiting
     */
    @Override
    public void put(E e) throws InterruptedException
    {
        Objects.requireNonNull(e, "element must not be null");
        if (limited())
        {
            deliverLimited(e);
            return;
        }
        for (Subscriber<E> subscriber : subscribers)
        {
            if (subscriber.accepts(e))
            {
                subscriber.queue.put(e);
                subscriber.increment();
            }
        }
    }

    /**
     * Adds the element to every subscribed queue that accepts it without
     * blocking.
     *
     * <p>When the distribution limit applies, the element is delivered to at
     * most {@code limit} subscribed queues instead; any of them that is at
     * capacity is skipped and the delivery is completed with the queues that
     * have not yet received the element.
     *
     * @param e the element to add; must not be {@code null}
     * @return {@code true} if every accepting subscriber accepted the element,
     *         {@code false} if one of them was at capacity (earlier subscribers
     *         may already have received it); in limited mode {@code true} if the
     *         element was delivered to at least one queue
     * @throws NullPointerException if {@code e} is {@code null}
     */
    @Override
    public boolean offer(E e)
    {
        Objects.requireNonNull(e, "element must not be null");
        if (limited())
        {
            return deliverLimited(e);
        }
        for (Subscriber<E> subscriber : subscribers)
        {
            if (subscriber.accepts(e))
            {
                if (!subscriber.queue.offer(e))
                {
                    return false;
                }
                subscriber.increment();
            }
        }
        return true;
    }

    /**
     * Adds the element to every subscribed queue that accepts it, waiting up to
     * the given timeout for space to become available in each of them.
     *
     * <p>When the distribution limit applies, the element is delivered
     * non-blocking to at most {@code limit} subscribed queues and the timeout
     * is ignored.
     *
     * @param e       the element to add; must not be {@code null}
     * @param timeout how long to wait before giving up, in units of {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret {@code timeout}
     * @return {@code true} if every accepting subscriber accepted the element
     *         within the timeout, {@code false} otherwise; in limited mode
     *         {@code true} if the element was delivered to at least one queue
     * @throws NullPointerException  if {@code e} or {@code unit} is {@code null}
     * @throws InterruptedException  if interrupted while waiting
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
    {
        Objects.requireNonNull(e, "element must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        if (limited())
        {
            return deliverLimited(e);
        }
        final long deadline = System.nanoTime() + unit.toNanos(timeout);
        for (Subscriber<E> subscriber : subscribers)
        {
            if (subscriber.accepts(e))
            {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0 || !subscriber.queue.offer(e, remaining, TimeUnit.NANOSECONDS))
                {
                    return false;
                }
                subscriber.increment();
            }
        }
        return true;
    }

    /**
     * Not supported. This queue is broadcast-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public E take()
    {
        throw new UnsupportedOperationException(READ_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is broadcast-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public E poll(long timeout, TimeUnit unit)
    {
        throw new UnsupportedOperationException(READ_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is broadcast-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public E poll()
    {
        throw new UnsupportedOperationException(READ_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is broadcast-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public E remove()
    {
        throw new UnsupportedOperationException(READ_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is broadcast-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public E element()
    {
        throw new UnsupportedOperationException(READ_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is broadcast-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public E peek()
    {
        throw new UnsupportedOperationException(READ_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is broadcast-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public int drainTo(Collection<? super E> c)
    {
        throw new UnsupportedOperationException(READ_NOT_SUPPORTED);
    }

    /**
     * Not supported. This queue is broadcast-only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public int drainTo(Collection<? super E> c, int maxElements)
    {
        throw new UnsupportedOperationException(READ_NOT_SUPPORTED);
    }

    /**
     * Returns the number of additional elements that can be added without
     * blocking, which is the smallest remaining capacity among the subscribed
     * queues (or {@link Integer#MAX_VALUE} if there are no subscribers).
     *
     * @return the remaining capacity
     */
    @Override
    public int remainingCapacity()
    {
        int remaining = Integer.MAX_VALUE;
        for (Subscriber<E> subscriber : subscribers)
        {
            remaining = Math.min(remaining, subscriber.queue.remainingCapacity());
        }
        return remaining;
    }

    /** Returns {@code 0}, since this queue never buffers elements. */
    @Override
    public int size()
    {
        return 0;
    }

    /** Returns {@code true}, since this queue never buffers elements. */
    @Override
    public boolean isEmpty()
    {
        return true;
    }

    /** Returns {@code false}, since this queue never buffers elements. */
    @Override
    public boolean contains(Object o)
    {
        return false;
    }

    /** Returns an empty iterator, since this queue never buffers elements. */
    @Override
    public Iterator<E> iterator()
    {
        return Collections.emptyIterator();
    }

    /** Returns an empty array, since this queue never buffers elements. */
    @Override
    public Object[] toArray()
    {
        return new Object[0];
    }

    /**
     * Returns the given array with its first element set to {@code null},
     * since this queue never buffers elements.
     *
     * @param a the array to fill
     * @param <T> the component type of the array
     * @return the given array
     * @throws NullPointerException if {@code a} is {@code null}
     */
    @Override
    public <T> T[] toArray(T[] a)
    {
        Objects.requireNonNull(a, "array must not be null");
        if (a.length > 0)
        {
            a[0] = null;
        }
        return a;
    }

    /** Returns {@code false}, since this queue never buffers elements. */
    @Override
    public boolean remove(Object o)
    {
        return false;
    }

    /**
     * Returns {@code true} if the given collection is empty, since this queue
     * never buffers elements.
     *
     * @param c the collection to check
     * @return {@code c.isEmpty()}
     * @throws NullPointerException if {@code c} is {@code null}
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        Objects.requireNonNull(c, "collection must not be null");
        return c.isEmpty();
    }

    /**
     * Adds every element of the given collection to the subscribed queues.
     *
     * @param c the collection whose elements are to be added
     * @return {@code true} if the collection was not empty
     * @throws NullPointerException if {@code c} is {@code null}
     */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        Objects.requireNonNull(c, "collection must not be null");
        for (E e : c)
        {
            add(e);
        }
        return !c.isEmpty();
    }

    /** Returns {@code false}, since this queue never buffers elements. */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        return false;
    }

    /** Returns {@code false}, since this queue never buffers elements. */
    @Override
    public boolean removeIf(Predicate<? super E> filter)
    {
        Objects.requireNonNull(filter, "filter must not be null");
        return false;
    }

    /** Returns {@code false}, since this queue never buffers elements. */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        return false;
    }

    /** Does nothing, since this queue never buffers elements. */
    @Override
    public void clear()
    {
    }

    /** Returns an empty {@link Spliterator}, since this queue never buffers elements. */
    @Override
    public Spliterator<E> spliterator()
    {
        return Spliterators.emptySpliterator();
    }

    /** Returns an empty {@link Stream}, since this queue never buffers elements. */
    @Override
    public Stream<E> stream()
    {
        return Stream.empty();
    }

    /** Returns an empty {@link Stream}, since this queue never buffers elements. */
    @Override
    public Stream<E> parallelStream()
    {
        return Stream.empty();
    }
}
