/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Factory methods that return immutable (unmodifiable) views of the standard
 * Java collections.
 * <p>
 * The returned views are read-only proxies over the given collection: any
 * attempt to mutate them throws {@link UnsupportedOperationException}. They are
 * backed by the original collection, so changes made directly to that
 * collection are reflected in the view. To obtain a truly independent and
 * immutable snapshot, copy the collection first (e.g. with the copy
 * constructors) and then wrap the copy.
 *
 * @author franci
 */
public abstract class Inmutable
{
    /**
     * Returns an unmodifiable view of the given collection.
     *
     * @param <E>        element type
     * @param collection the collection to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> Collection<E> collection(Collection<? extends E> collection)
    {
        return Collections.unmodifiableCollection(collection);
    }

    /**
     * Returns an unmodifiable view of the given list.
     *
     * @param <E>  element type
     * @param list the list to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> List<E> list(List<? extends E> list)
    {
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns an unmodifiable view of the given set.
     *
     * @param <E> element type
     * @param set the set to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> Set<E> set(Set<? extends E> set)
    {
        return Collections.unmodifiableSet(set);
    }

    /**
     * Returns an unmodifiable view of the given sorted set.
     *
     * @param <E> element type
     * @param set the sorted set to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> SortedSet<E> sortedSet(SortedSet<E> set)
    {
        return Collections.unmodifiableSortedSet(set);
    }

    /**
     * Returns an unmodifiable view of the given navigable set.
     *
     * @param <E> element type
     * @param set the navigable set to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> NavigableSet<E> navigableSet(NavigableSet<E> set)
    {
        return Collections.unmodifiableNavigableSet(set);
    }

    /**
     * Returns an unmodifiable view of the given map.
     *
     * @param <K> key type
     * @param <V> value type
     * @param map the map to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <K, V> Map<K, V> map(Map<? extends K, ? extends V> map)
    {
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns an unmodifiable view of the given sorted map.
     *
     * @param <K> key type
     * @param <V> value type
     * @param map the sorted map to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <K, V> SortedMap<K, V> sortedMap(SortedMap<K, ? extends V> map)
    {
        return Collections.unmodifiableSortedMap(map);
    }

    /**
     * Returns an unmodifiable view of the given navigable map.
     *
     * @param <K> key type
     * @param <V> value type
     * @param map the navigable map to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <K, V> NavigableMap<K, V> navigableMap(NavigableMap<K, ? extends V> map)
    {
        return Collections.unmodifiableNavigableMap(map);
    }

    /**
     * Returns an unmodifiable view of the given queue. Read operations
     * ({@code peek}, {@code element}, iteration) are allowed; operations that
     * remove elements throw {@link UnsupportedOperationException}.
     *
     * @param <E>   element type
     * @param queue the queue to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> Queue<E> queue(Queue<E> queue)
    {
        return new UnmodifiableQueue<>(Objects.requireNonNull(queue, "queue must not be null"));
    }

    /**
     * Returns an unmodifiable view of the given deque. Read operations
     * ({@code peekFirst}, {@code peekLast}, {@code getFirst}, {@code getLast},
     * iteration) are allowed; operations that remove elements throw
     * {@link UnsupportedOperationException}.
     *
     * @param <E>   element type
     * @param deque the deque to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> Deque<E> deque(Deque<E> deque)
    {
        return new UnmodifiableDeque<>(Objects.requireNonNull(deque, "deque must not be null"));
    }

    /**
     * Returns an unmodifiable view of the given iterable.
     *
     * @param <E>      element type
     * @param iterable the iterable to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> Iterable<E> iterable(Iterable<? extends E> iterable)
    {
        return new UnmodifiableIterable<>(Objects.requireNonNull(iterable, "iterable must not be null"));
    }

    /**
     * Returns an unmodifiable view of the given iterator. The
     * {@link Iterator#remove()} operation throws
     * {@link UnsupportedOperationException}.
     *
     * @param <E>      element type
     * @param iterator the iterator to wrap; must not be {@code null}
     * @return an unmodifiable view
     */
    public static <E> Iterator<E> iterator(Iterator<? extends E> iterator)
    {
        return new UnmodifiableIterator<>(Objects.requireNonNull(iterator, "iterator must not be null"));
    }

    private static final class UnmodifiableIterator<E> implements Iterator<E>
    {
        private final Iterator<? extends E> delegate;

        private UnmodifiableIterator(Iterator<? extends E> delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext()
        {
            return delegate.hasNext();
        }

        @Override
        public E next()
        {
            return delegate.next();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action)
        {
            delegate.forEachRemaining(action);
        }
    }

    private static final class UnmodifiableIterable<E> implements Iterable<E>
    {
        private final Iterable<? extends E> delegate;

        private UnmodifiableIterable(Iterable<? extends E> delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public Iterator<E> iterator()
        {
            return new UnmodifiableIterator<>(delegate.iterator());
        }

        @Override
        public void forEach(Consumer<? super E> action)
        {
            delegate.forEach(action);
        }
    }

    private static final class UnmodifiableQueue<E> extends AbstractCollection<E> implements Queue<E>
    {
        private final Queue<E> delegate;

        private UnmodifiableQueue(Queue<E> delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public int size()
        {
            return delegate.size();
        }

        @Override
        public boolean isEmpty()
        {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return delegate.contains(o);
        }

        @Override
        public Iterator<E> iterator()
        {
            return new UnmodifiableIterator<>(delegate.iterator());
        }

        @Override
        public Object[] toArray()
        {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return delegate.toArray(a);
        }

        @Override
        public E peek()
        {
            return delegate.peek();
        }

        @Override
        public E element()
        {
            return delegate.element();
        }

        @Override
        public boolean add(E e)
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
            throw new UnsupportedOperationException();
        }

        @Override
        public E poll()
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
        public boolean removeAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }
    }

    private static final class UnmodifiableDeque<E> extends AbstractCollection<E> implements Deque<E>
    {
        private final Deque<E> delegate;

        private UnmodifiableDeque(Deque<E> delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public int size()
        {
            return delegate.size();
        }

        @Override
        public boolean isEmpty()
        {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return delegate.contains(o);
        }

        @Override
        public Iterator<E> iterator()
        {
            return new UnmodifiableIterator<>(delegate.iterator());
        }

        @Override
        public Iterator<E> descendingIterator()
        {
            return new UnmodifiableIterator<>(delegate.descendingIterator());
        }

        @Override
        public Object[] toArray()
        {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return delegate.toArray(a);
        }

        @Override
        public E peek()
        {
            return delegate.peek();
        }

        @Override
        public E peekFirst()
        {
            return delegate.peekFirst();
        }

        @Override
        public E peekLast()
        {
            return delegate.peekLast();
        }

        @Override
        public E getFirst()
        {
            return delegate.getFirst();
        }

        @Override
        public E getLast()
        {
            return delegate.getLast();
        }

        @Override
        public E element()
        {
            return delegate.element();
        }

        @Override
        public boolean add(E e)
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
        public boolean offer(E e)
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
        public E remove()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E removeFirst()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E removeLast()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E poll()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollFirst()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollLast()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o)
        {
            throw new UnsupportedOperationException();
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
        public void push(E e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pop()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }
    }
}
