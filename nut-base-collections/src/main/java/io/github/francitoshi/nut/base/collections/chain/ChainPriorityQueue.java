/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections.chain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;

/**
 * A stacked view over an ordered sequence of {@link Queue}s that exposes the
 * interface of a single priority queue by inspecting the head of every queue
 * of the chain at the same time, the {@link Queue} counterpart of
 * {@link ChainMap}.
 * <p>
 * {@link #peek()} and {@link #poll()} look at the head of each queue and
 * return, resp. remove, the best of those heads according to the comparator
 * of this chain or, if none was given, to the natural ordering of the
 * elements. Every other operation considers all the queues: {@link #size()}
 * sums their sizes, {@link #contains(Object)} walks them all, and so on. The
 * chain keeps the given queue instances by reference and never copies an
 * element, therefore changes made to those queues are immediately visible
 * through this view (and vice versa).
 * <p>
 * For the view to behave as one priority queue, every underlying queue must
 * be ordered by the same criterion, so that its head is its best element (as
 * the elements of a {@link java.util.PriorityQueue} are). A queue that is not
 * ordered that way still participates, but only its head competes in
 * {@link #peek()} and {@link #poll()}.
 * <p>
 * Operations that <em>insert</em> an element are always applied to the
 * <strong>first</strong> queue: {@link #offer(Object)},
 * {@link #add(Object)} and {@link #addAll(Collection)} (and the
 * {@link Collection} default methods built on top of them), so the first
 * queue is the {@linkplain #first() write target} of the chain, exactly as
 * in {@link ChainMap} and {@link ChainSet}. {@link #remove(Object)} removes
 * the element from the first queue of the chain that contains it, and
 * {@link #removeAll(Collection)}, {@link #retainAll(Collection)} and
 * {@link #clear()} delegate to every queue, so no element is ever copied.
 * <p>
 * {@link #iterator()} walks the queues in chain order, <em>not</em> in
 * priority order, and delegates {@link Iterator#remove()} to the queue that
 * supplied the element.
 * <p>
 * Like {@link java.util.PriorityQueue}, this class does not permit
 * {@code null} elements and a non-comparable element makes the natural
 * ordering fail with a {@link ClassCastException}. Like the rest of the
 * queues of the JDK, it uses identity for {@link Object#equals(Object)} and
 * {@link Object#hashCode()}. This class is not synchronized.
 *
 * @param <E> the type of elements maintained by this chain
 */
public class ChainPriorityQueue<E> implements Queue<E>
{
    private final List<Queue<E>> queues;
    private final Comparator<? super E> comparator;

    /**
     * Constructs a new ChainPriorityQueue over the given queues, from the
     * highest (first inspected, first written to) to the lowest, using the
     * natural ordering of the elements.
     *
     * @param queues at least one queue; must not be {@code null} nor contain
     *               {@code null} elements
     * @throws NullPointerException     if {@code queues} or one of its
     *                                  elements is {@code null}
     * @throws IllegalArgumentException if no queue is given
     */
    @SafeVarargs
    public ChainPriorityQueue(Queue<E>... queues)
    {
        this(null, toCollection(queues));
    }
    /**
     * Constructs a new ChainPriorityQueue over the given queues, from the
     * highest (first inspected, first written to) to the lowest, using the
     * natural ordering of the elements.
     *
     * @param queues at least one queue; must not be {@code null} nor contain
     *               {@code null} elements
     * @throws NullPointerException     if {@code queues} or one of its
     *                                  elements is {@code null}
     * @throws IllegalArgumentException if no queue is given
     */
    public ChainPriorityQueue(Collection<? extends Queue<E>> queues)
    {
        this(null, queues);
    }
    /**
     * Constructs a new ChainPriorityQueue over the given queues, from the
     * highest (first inspected, first written to) to the lowest, using the
     * given comparator.
     *
     * @param comparator the comparator that orders the heads of the chain;
     *                   {@code null} to use the natural ordering
     * @param queues     at least one queue; must not be {@code null} nor
     *                   contain {@code null} elements
     * @throws NullPointerException     if {@code queues} or one of its
     *                                  elements is {@code null}
     * @throws IllegalArgumentException if no queue is given
     */
    @SafeVarargs
    public ChainPriorityQueue(Comparator<? super E> comparator, Queue<E>... queues)
    {
        this(comparator, toCollection(queues));
    }
    /**
     * Constructs a new ChainPriorityQueue over the given queues, from the
     * highest (first inspected, first written to) to the lowest, using the
     * given comparator.
     *
     * @param comparator the comparator that orders the heads of the chain;
     *                   {@code null} to use the natural ordering
     * @param queues     at least one queue; must not be {@code null} nor
     *                   contain {@code null} elements
     * @throws NullPointerException     if {@code queues} or one of its
     *                                  elements is {@code null}
     * @throws IllegalArgumentException if no queue is given
     */
    public ChainPriorityQueue(Comparator<? super E> comparator, Collection<? extends Queue<E>> queues)
    {
        Objects.requireNonNull(queues, "queues must not be null");
        if(queues.isEmpty())
        {
            throw new IllegalArgumentException("at least one queue is required");
        }
        ArrayList<Queue<E>> copy = new ArrayList<>(queues.size());
        for(Queue<E> q : queues)
        {
            copy.add(Objects.requireNonNull(q, "queue must not be null"));
        }
        this.queues = Collections.unmodifiableList(copy);
        this.comparator = comparator;
    }

    private static <E> Collection<? extends Queue<E>> toCollection(Queue<E>[] queues)
    {
        Objects.requireNonNull(queues, "queues must not be null");
        return Arrays.asList(queues);
    }

    /**
     * Returns the queues of this chain as an unmodifiable list, from the
     * highest to the lowest.
     *
     * @return the queues of this chain; never {@code null}
     */
    public List<Queue<E>> queues()
    {
        return this.queues;
    }

    /**
     * Returns the first queue of the chain, the target of every insertion.
     *
     * @return the first queue; never {@code null}
     */
    public Queue<E> first()
    {
        return this.queues.get(0);
    }

    /**
     * Returns the comparator that orders the heads of this chain, or
     * {@code null} if the natural ordering is used.
     *
     * @return the comparator of this chain, or {@code null}
     */
    public Comparator<? super E> comparator()
    {
        return this.comparator;
    }

    /**
     * Compares two elements with the comparator of this chain or, if none was
     * given, with their natural ordering.
     *
     * @param a the first element to compare
     * @param b the second element to compare
     * @return a negative integer, zero or a positive integer as {@code a} is
     *         less than, equal to or greater than {@code b}
     * @throws ClassCastException if the natural ordering is used and the
     *                            elements are not mutually comparable
     */
    @SuppressWarnings("unchecked")
    private int compare(E a, E b)
    {
        if(this.comparator!=null)
        {
            return this.comparator.compare(a, b);
        }
        return ((Comparable<? super E>) a).compareTo(b);
    }

    /**
     * Returns the queue of the chain whose head is the best one, or
     * {@code null} if every queue of the chain is empty.
     *
     * @return the queue owning the best head, or {@code null}
     */
    private Queue<E> best()
    {
        Queue<E> bestQueue = null;
        E bestElement = null;
        for(Queue<E> q : this.queues)
        {
            E head = q.peek();
            if(head==null)
            {
                continue;
            }
            if(bestQueue==null || compare(head, bestElement)<0)
            {
                bestQueue = q;
                bestElement = head;
            }
        }
        return bestQueue;
    }

    /**
     * Builds a fresh copy of the content of this chain, queue by queue.
     *
     * @return a fresh snapshot; never {@code null}
     */
    private ArrayList<E> snapshot()
    {
        ArrayList<E> snap = new ArrayList<>(size());
        for(Queue<E> q : this.queues)
        {
            snap.addAll(q);
        }
        return snap;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the best head of the chain, that is, the head of the queue of
     * the chain that owns the best one, without removing it; {@code null} if
     * every queue of the chain is empty.
     */
    @Override
    public E peek()
    {
        Queue<E> best = best();
        return best==null ? null : best.peek();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes and returns the best head of the chain from the queue that owns
     * it; {@code null} if every queue of the chain is empty.
     */
    @Override
    public E poll()
    {
        Queue<E> best = best();
        return best==null ? null : best.poll();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the best head of the chain without removing it.
     *
     * @throws NoSuchElementException if every queue of the chain is empty
     */
    @Override
    public E element()
    {
        E head = peek();
        if(head==null)
        {
            throw new NoSuchElementException("No head element in the chain");
        }
        return head;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes and returns the best head of the chain from the queue that owns
     * it.
     *
     * @throws NoSuchElementException if every queue of the chain is empty
     */
    @Override
    public E remove()
    {
        E head = poll();
        if(head==null)
        {
            throw new NoSuchElementException("No head element in the chain");
        }
        return head;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The element is inserted into the first queue of the chain.
     *
     * @throws NullPointerException if the element is {@code null}
     */
    @Override
    public boolean offer(E element)
    {
        Objects.requireNonNull(element, "element must not be null");
        return first().offer(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The element is inserted into the first queue of the chain.
     *
     * @throws NullPointerException     if the element is {@code null}
     * @throws IllegalArgumentException if the first queue of the chain is
     *                                  full
     */
    @Override
    public boolean add(E element)
    {
        Objects.requireNonNull(element, "element must not be null");
        if(first().add(element))
        {
            return true;
        }
        throw new IllegalArgumentException("Queue full");
    }

    /**
     * {@inheritDoc}
     * <p>
     * The size is the number of elements of all the queues of the chain,
     * recomputed on every call so that changes made to those queues are
     * reflected at once.
     */
    @Override
    public int size()
    {
        int size = 0;
        for(Queue<E> q : this.queues)
        {
            size += q.size();
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        for(Queue<E> q : this.queues)
        {
            if(!q.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object element)
    {
        for(Queue<E> q : this.queues)
        {
            if(q.contains(element))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        for(Object o : c)
        {
            if(!contains(o))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes the element from the first queue of the chain that contains it.
     */
    @Override
    public boolean remove(Object element)
    {
        for(Queue<E> q : this.queues)
        {
            if(q.remove(element))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * All the elements are inserted into the first queue of the chain.
     *
     * @throws NullPointerException if {@code c} or one of its elements is
     *                              {@code null}
     */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        Objects.requireNonNull(c, "collection must not be null");
        for(E element : c)
        {
            Objects.requireNonNull(element, "element must not be null");
        }
        return first().addAll(c);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The removal is delegated to every queue of the chain, so no element is
     * copied.
     */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean modified = false;
        for(Queue<E> q : this.queues)
        {
            modified |= q.removeAll(c);
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The removal is delegated to every queue of the chain, so no element is
     * copied.
     */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean modified = false;
        for(Queue<E> q : this.queues)
        {
            modified |= q.retainAll(c);
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Every queue of the chain is cleared, so that the view becomes empty.
     */
    @Override
    public void clear()
    {
        for(Queue<E> q : this.queues)
        {
            q.clear();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a snapshot of the elements of the chain, queue by queue.
     */
    @Override
    public Object[] toArray()
    {
        return snapshot().toArray();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a snapshot of the elements of the chain, queue by queue.
     */
    @Override
    public <T> T[] toArray(T[] a)
    {
        return snapshot().toArray(a);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned iterator walks the queues of the chain in order, not in
     * priority order, and delegates {@link Iterator#remove()} to the queue
     * that supplied the element.
     */
    @Override
    public Iterator<E> iterator()
    {
        return new ChainIterator();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string of the form {@code [e, e, e]} holding the elements of
     * the chain, queue by queue. The string is built without materialising a
     * copy of the elements.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean firstElement = true;
        for(Queue<E> q : this.queues)
        {
            for(E e : q)
            {
                if(!firstElement)
                {
                    sb.append(", ");
                }
                sb.append(e);
                firstElement = false;
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * A live cursor over the queues of the chain that delegates removal to
     * the queue that supplied the last element.
     */
    private final class ChainIterator implements Iterator<E>
    {
        private int nextQueue = 0;
        private Iterator<E> current;
        private Iterator<E> last;

        /**
         * {@inheritDoc}
         * <p>
         * Empty queues are skipped on the fly.
         */
        @Override
        public boolean hasNext()
        {
            while(this.current==null || !this.current.hasNext())
            {
                if(this.nextQueue>=ChainPriorityQueue.this.queues.size())
                {
                    return false;
                }
                this.current = ChainPriorityQueue.this.queues.get(this.nextQueue++).iterator();
            }
            return true;
        }
        /**
         * {@inheritDoc}
         *
         * @throws NoSuchElementException if all the queues of the chain are
         *                               exhausted
         */
        @Override
        public E next()
        {
            if(!hasNext())
            {
                throw new NoSuchElementException();
            }
            E element = this.current.next();
            this.last = this.current;
            return element;
        }
        /**
         * {@inheritDoc}
         * <p>
         * Removes the last returned element from the queue that supplied it.
         *
         * @throws IllegalStateException    if {@link #next()} has not been
         *                                  called since the last removal
         * @throws UnsupportedOperationException if the queue iterator does
         *                                  not support removal
         */
        @Override
        public void remove()
        {
            if(this.last==null)
            {
                throw new IllegalStateException();
            }
            this.last.remove();
            this.last = null;
        }
    }
}
