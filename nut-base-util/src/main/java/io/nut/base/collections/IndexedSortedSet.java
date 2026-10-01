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
import java.util.Objects;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * A {@link SortedSet} implementation that wraps a delegate sorted set and provides
 * efficient index-based access via {@link #get(int)}.
 *
 * @param <T> the type of elements maintained by this set
 * @author franci
 * @since 1.8
 */
public class IndexedSortedSet<T> implements SortedSet<T>
{
    final SortedSet<T> delegate;

    private final ArrayList<T> cache = new ArrayList<>();
    private Iterator<T> iterator;
    private boolean fullyCached = false;

    // Shared state used to synchronize cache invalidation between this set and any of its subsets/views.
    final SharedState state;
    private long localModCount;

    private static class SharedState
    {
        long modCount = 0;
    }

    /**
     * Constructs a new, empty IndexedSortedSet using a {@link TreeSet} as the default delegate.
     */
    public IndexedSortedSet()
    {
        this(new TreeSet<>());
    }

    /**
     * Constructs a new, empty IndexedSortedSet using a {@link TreeSet} with the specified comparator
     * as the default delegate.
     *
     * @param comparator the comparator that will be used to order this set
     */
    public IndexedSortedSet(Comparator<? super T> comparator)
    {
        this(new TreeSet<>(comparator));
    }

    /**
     * Constructs a new IndexedSortedSet containing the elements in the specified collection,
     * using a {@link TreeSet} as the default delegate.
     *
     * @param collection the collection whose elements will fill this set
     */
    public IndexedSortedSet(Collection<? extends T> collection)
    {
        this(new TreeSet<>(collection));
    }

    /**
     * Constructs a new IndexedSortedSet wrapping the given delegate.
     *
     * @param delegate the sorted set to wrap; must not be null
     */
    public IndexedSortedSet(SortedSet<T> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.state = new SharedState();
        this.localModCount = 0;
    }

    /**
     * Internal constructor for creating subSet, headSet, and tailSet views that share the same validation state.
     */
    private IndexedSortedSet(SortedSet<T> delegate, SharedState state)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.state = state;
        this.localModCount = state.modCount;
    }

    /**
     * Checks if the shared state has been modified, and invalidates the local cache/iterator if so.
     */
    private void checkValidity()
    {
        if (localModCount != state.modCount)
        {
            invalidateLocal();
            localModCount = state.modCount;
        }
    }

    /**
     * Invalidates the local cache and iterator.
     */
    private void invalidateLocal()
    {
        cache.clear();
        iterator = null;
        fullyCached = false;
    }

    /**
     * Triggers invalidation across all views sharing the same state.
     */
    private void invalidate()
    {
        state.modCount++;
        invalidateLocal();
    }

    /**
     * Returns the element at the specified position in this sorted set.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this set
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
     */
    public T get(int index)
    {
        if (index < 0)
        {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        checkValidity();

        if (index < cache.size())
        {
            return cache.get(index);
        }

        if (fullyCached)
        {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + cache.size());
        }

        if (iterator == null)
        {
            iterator = delegate.iterator();
        }

        // Optimize ArrayList capacity by growing it in advance
        cache.ensureCapacity(index + 1);

        while (cache.size() <= index)
        {
            if (iterator.hasNext())
            {
                cache.add(iterator.next());
            }
            else
            {
                fullyCached = true;
                iterator = null;
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + cache.size());
            }
        }

        return cache.get(index);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this set,
     * or -1 if this set does not contain the element.
     *
     * @param element element to search for
     * @return the index of the first occurrence of the specified element in this set,
     *         or -1 if this set does not contain the element
     */
    @SuppressWarnings("unchecked")
    public int indexOf(T element)
    {
        if (!contains(element))
        {
            return -1;
        }

        checkValidity();

        // 1. Search in the cache using binary search (O(log K) where K is cache size)
        int index = Collections.binarySearch(cache, element, comparator());
        if (index >= 0)
        {
            return index;
        }

        // 2. If not found in cache, advance iterator and populate cache until we find it
        if (iterator == null)
        {
            iterator = delegate.iterator();
        }

        Comparator<? super T> comp = comparator();
        while (iterator.hasNext())
        {
            T next = iterator.next();
            cache.add(next);

            boolean eq = (comp == null)
                ? ((Comparable<? super T>) next).compareTo(element) == 0
                : comp.compare(next, element) == 0;

            if (eq)
            {
                return cache.size() - 1;
            }
        }

        // Should not be reached since contains(element) was true
        return -1;
    }

    @Override
    public Comparator<? super T> comparator()
    {
        return delegate.comparator();
    }

    @Override
    public IndexedSortedSet<T> subSet(T fromElement, T toElement)
    {
        return new IndexedSortedSet<>(delegate.subSet(fromElement, toElement), state);
    }

    @Override
    public IndexedSortedSet<T> headSet(T toElement)
    {
        return new IndexedSortedSet<>(delegate.headSet(toElement), state);
    }

    @Override
    public IndexedSortedSet<T> tailSet(T fromElement)
    {
        return new IndexedSortedSet<>(delegate.tailSet(fromElement), state);
    }

    @Override
    public T first()
    {
        checkValidity();
        if (!cache.isEmpty())
        {
            return cache.get(0);
        }
        return delegate.first();
    }

    @Override
    public T last()
    {
        checkValidity();
        if (fullyCached && !cache.isEmpty())
        {
            return cache.get(cache.size() - 1);
        }
        return delegate.last();
    }

    @Override
    public int size()
    {
        checkValidity();
        return fullyCached ? cache.size() : delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        checkValidity();
        return fullyCached ? cache.isEmpty() : delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return delegate.contains(o);
    }

    @Override
    public Iterator<T> iterator()
    {
        final Iterator<T> delegateIterator = delegate.iterator();
        return new Iterator<T>()
        {
            @Override
            public boolean hasNext()
            {
                return delegateIterator.hasNext();
            }

            @Override
            public T next()
            {
                return delegateIterator.next();
            }

            @Override
            public void remove()
            {
                delegateIterator.remove();
                invalidate();
            }
        };
    }

    @Override
    public Object[] toArray()
    {
        checkValidity();
        return fullyCached ? cache.toArray() : delegate.toArray();
    }

    @Override
    public <E> E[] toArray(E[] a)
    {
        checkValidity();
        return fullyCached ? cache.toArray(a) : delegate.toArray(a);
    }

    @Override
    public boolean add(T e)
    {
        boolean modified = delegate.add(e);
        if (modified)
        {
            invalidate();
        }
        return modified;
    }

    @Override
    public boolean remove(Object o)
    {
        boolean modified = delegate.remove(o);
        if (modified)
        {
            invalidate();
        }
        return modified;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        boolean modified = delegate.addAll(c);
        if (modified)
        {
            invalidate();
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean modified = delegate.retainAll(c);
        if (modified)
        {
            invalidate();
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean modified = delegate.removeAll(c);
        if (modified)
        {
            invalidate();
        }
        return modified;
    }

    @Override
    public void clear()
    {
        if (!delegate.isEmpty())
        {
            delegate.clear();
            invalidate();
        }
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter)
    {
        boolean modified = delegate.removeIf(filter);
        if (modified)
        {
            invalidate();
        }
        return modified;
    }

    @Override
    public Spliterator<T> spliterator()
    {
        return delegate.spliterator();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }
        if (o instanceof IndexedSortedSet)
        {
            return delegate.equals(((IndexedSortedSet<?>) o).delegate);
        }
        return delegate.equals(o);
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }

    /**
     * Returns a thread-safe (synchronized) version of this IndexedSortedSet.
     * The returned set is backed by this set, so changes to either are reflected in the other.
     * All operations on the returned set are synchronized on a private, specific lock object.
     *
     * @return a thread-safe view of this set
     */
    public IndexedSortedSet<T> synchronizedView()
    {
        return new SynchronizedIndexedSortedSet<>(delegate, state, new Object());
    }

    private static class SynchronizedIndexedSortedSet<E> extends IndexedSortedSet<E>
    {
        private final Object mutex;

        SynchronizedIndexedSortedSet(SortedSet<E> delegate, SharedState state, Object mutex)
        {
            super(delegate, state);
            this.mutex = Objects.requireNonNull(mutex);
        }

        @Override
        public IndexedSortedSet<E> synchronizedView()
        {
            return this;
        }

        @Override
        public E get(int index)
        {
            synchronized (mutex)
            {
                return super.get(index);
            }
        }

        @Override
        public int indexOf(E element)
        {
            synchronized (mutex)
            {
                return super.indexOf(element);
            }
        }

        @Override
        public Comparator<? super E> comparator()
        {
            synchronized (mutex)
            {
                return super.comparator();
            }
        }

        @Override
        public IndexedSortedSet<E> subSet(E fromElement, E toElement)
        {
            synchronized (mutex)
            {
                return new SynchronizedIndexedSortedSet<>(delegate.subSet(fromElement, toElement), state, mutex);
            }
        }

        @Override
        public IndexedSortedSet<E> headSet(E toElement)
        {
            synchronized (mutex)
            {
                return new SynchronizedIndexedSortedSet<>(delegate.headSet(toElement), state, mutex);
            }
        }

        @Override
        public IndexedSortedSet<E> tailSet(E fromElement)
        {
            synchronized (mutex)
            {
                return new SynchronizedIndexedSortedSet<>(delegate.tailSet(fromElement), state, mutex);
            }
        }

        @Override
        public E first()
        {
            synchronized (mutex)
            {
                return super.first();
            }
        }

        @Override
        public E last()
        {
            synchronized (mutex)
            {
                return super.last();
            }
        }

        @Override
        public int size()
        {
            synchronized (mutex)
            {
                return super.size();
            }
        }

        @Override
        public boolean isEmpty()
        {
            synchronized (mutex)
            {
                return super.isEmpty();
            }
        }

        @Override
        public boolean contains(Object o)
        {
            synchronized (mutex)
            {
                return super.contains(o);
            }
        }

        @Override
        public Iterator<E> iterator()
        {
            synchronized (mutex)
            {
                final Iterator<E> it = super.iterator();
                return new Iterator<E>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        synchronized (mutex)
                        {
                            return it.hasNext();
                        }
                    }

                    @Override
                    public E next()
                    {
                        synchronized (mutex)
                        {
                            return it.next();
                        }
                    }

                    @Override
                    public void remove()
                    {
                        synchronized (mutex)
                        {
                            it.remove();
                        }
                    }
                };
            }
        }

        @Override
        public Object[] toArray()
        {
            synchronized (mutex)
            {
                return super.toArray();
            }
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            synchronized (mutex)
            {
                return super.toArray(a);
            }
        }

        @Override
        public boolean add(E e)
        {
            synchronized (mutex)
            {
                return super.add(e);
            }
        }

        @Override
        public boolean remove(Object o)
        {
            synchronized (mutex)
            {
                return super.remove(o);
            }
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            synchronized (mutex)
            {
                return super.containsAll(c);
            }
        }

        @Override
        public boolean addAll(Collection<? extends E> c)
        {
            synchronized (mutex)
            {
                return super.addAll(c);
            }
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            synchronized (mutex)
            {
                return super.retainAll(c);
            }
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            synchronized (mutex)
            {
                return super.removeAll(c);
            }
        }

        @Override
        public void clear()
        {
            synchronized (mutex)
            {
                super.clear();
            }
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter)
        {
            synchronized (mutex)
            {
                return super.removeIf(filter);
            }
        }

        @Override
        public Spliterator<E> spliterator()
        {
            synchronized (mutex)
            {
                return super.spliterator();
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
            {
                return true;
            }
            synchronized (mutex)
            {
                return super.equals(o);
            }
        }

        @Override
        public int hashCode()
        {
            synchronized (mutex)
            {
                return super.hashCode();
            }
        }

        @Override
        public String toString()
        {
            synchronized (mutex)
            {
                return super.toString();
            }
        }
    }
}
