/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Common empty values: primitive arrays and immutable empty collections.
 *
 * @author franci
 */
public abstract class Empty
{
    // Common primitive arrays
    public static final byte[] BYTES = new byte[0];
    public static final int[] INTS = new int[0];
    public static final long[] LONGS = new long[0];
    public static final char[] CHARS = new char[0];
    public static final boolean[] BOOLEANS = new boolean[0];
    public static final short[] SHORTS = new short[0];
    public static final float[] FLOATS = new float[0];
    public static final double[] DOUBLES = new double[0];

    // Standard objects
    public static final Object[] OBJECTS = new Object[0];
    public static final String[] STRINGS = new String[0];
    public static final Class<?>[] CLASSES = new Class<?>[0];
    public static final Throwable[] THROWABLES = new Throwable[0];

    // Common immutable collections
    public static final Collection<?> COLLECTION = Collections.emptyList();
    public static final Iterable<?> ITERABLE = Collections.emptyList();
    public static final Iterator<?> ITERATOR = Collections.emptyIterator();

    public static final List<?> LIST = Collections.emptyList();
    public static final Set<?> SET = Collections.emptySet();
    public static final Map<?, ?> MAP = Collections.emptyMap();

    public static final SortedSet<?> SORTED_SET = Collections.unmodifiableSortedSet(new TreeSet<>());
    public static final SortedMap<?, ?> SORTED_MAP = Collections.unmodifiableSortedMap(new TreeMap<>());
    public static final NavigableSet<?> NAVIGABLE_SET = Collections.unmodifiableNavigableSet(new TreeSet<>());
    public static final NavigableMap<?, ?> NAVIGABLE_MAP = Collections.unmodifiableNavigableMap(new TreeMap<>());

    public static final Queue<?> QUEUE = new EmptyDeque<>();
    public static final Deque<?> DEQUE = new EmptyDeque<>();

    private static final class EmptyDeque<E> extends AbstractCollection<E> implements Deque<E>
    {
        private static final Iterator<Object> EMPTY_ITERATOR = Collections.emptyIterator();

        @Override
        public int size()
        {
            return 0;
        }

        @Override
        public boolean isEmpty()
        {
            return true;
        }

        @Override
        public boolean contains(Object o)
        {
            return false;
        }

        @Override
        public Iterator<E> iterator()
        {
            return (Iterator<E>) EMPTY_ITERATOR;
        }

        @Override
        public Object[] toArray()
        {
            return OBJECTS;
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            if (a.length > 0)
            {
                a[0] = null;
            }
            return a;
        }

        @Override
        public boolean add(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addFirst(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addLast(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offerFirst(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offerLast(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E removeFirst()
        {
            throw new NoSuchElementException();
        }

        @Override
        public E removeLast()
        {
            throw new NoSuchElementException();
        }

        @Override
        public E pollFirst()
        {
            return null;
        }

        @Override
        public E pollLast()
        {
            return null;
        }

        @Override
        public E getFirst()
        {
            throw new NoSuchElementException();
        }

        @Override
        public E getLast()
        {
            throw new NoSuchElementException();
        }

        @Override
        public E peekFirst()
        {
            return null;
        }

        @Override
        public E peekLast()
        {
            return null;
        }

        @Override
        public boolean removeFirstOccurrence(Object o)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeLastOccurrence(Object o)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E remove()
        {
            throw new NoSuchElementException();
        }

        @Override
        public E poll()
        {
            return null;
        }

        @Override
        public E element()
        {
            throw new NoSuchElementException();
        }

        @Override
        public E peek()
        {
            return null;
        }

        @Override
        public void push(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pop()
        {
            throw new NoSuchElementException();
        }

        @Override
        public Iterator<E> descendingIterator()
        {
            return (Iterator<E>) EMPTY_ITERATOR;
        }
    }
}