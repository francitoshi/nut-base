/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * A terminal pipeline stage that doubles as a {@link List}: every message
 * received via the {@link Bee} machinery is appended to a delegate
 * {@link List}{@code <E>} supplied at construction time, and every {@code List}
 * method is delegated to that same list.
 * <p>
 * This is useful for collecting the output of a pipeline into an ordinary
 * {@code List} without exposing the asynchronous Bee/Hive infrastructure to the
 * collecting code:
 * <pre>{@code
 * List<String> results = Collections.synchronizedList(new ArrayList<>());
 * ListBee<String> sink = hive.list(results);
 * pipe.linkTo(sink);
 * // ... send messages to pipe ...
 * Hive.shutdownAndAwaitTermination(true, false, pipe);
 * // results now contains all processed messages
 * }</pre>
 * <p>
 * <strong>Thread safety:</strong> a plain {@link java.util.ArrayList} or
 * {@link java.util.LinkedList} is not thread-safe. If the {@code ListBee} is
 * used with a Hive (asynchronous mode), multiple worker threads may call
 * {@link #receive(Object)} concurrently. In that case the caller must supply a
 * synchronized or thread-safe {@code List}, e.g.
 * {@code Collections.synchronizedList(new ArrayList<>())}.
 *
 * @param <E> the type of elements held by the delegate list
 */
public class ListBee<E> extends Bee<E> implements List<E>
{
    private final List<E> delegate;

    /**
     * Full constructor.
     *
     * @param threads   the maximum number of concurrent worker threads
     * @param hive      the Hive thread pool, or {@code null} for synchronous mode
     * @param queueSize the internal Bee queue capacity (0 = default)
     * @param delegate  the {@code List} that stores received elements; must not
     *                  be {@code null}
     */
    public ListBee(int threads, Hive hive, int queueSize, List<E> delegate)
    {
        super(threads, hive, queueSize);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a ListBee with the given thread count and Hive, using the
     * default internal queue size.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param hive     the Hive thread pool, or {@code null} for synchronous mode
     * @param delegate the delegate list; must not be {@code null}
     */
    public ListBee(int threads, Hive hive, List<E> delegate)
    {
        super(threads, hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a ListBee attached to the given Hive with the default thread
     * count and internal queue size.
     *
     * @param hive     the Hive thread pool, or {@code null} for synchronous mode
     * @param delegate the delegate list; must not be {@code null}
     */
    public ListBee(Hive hive, List<E> delegate)
    {
        super(hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a standalone ListBee with the given thread count but no Hive.
     * A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param delegate the delegate list; must not be {@code null}
     */
    public ListBee(int threads, List<E> delegate)
    {
        super(threads);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a standalone ListBee with the default thread count and no
     * Hive. A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param delegate the delegate list; must not be {@code null}
     */
    public ListBee(List<E> delegate)
    {
        super();
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Receives a message from the Bee pipeline and appends it to the delegate
     * list via {@link List#add(Object)}.
     *
     * @param e the message/element to append to the delegate list
     */
    @Override
    protected void receive(E e)
    {
        delegate.add(e);
    }

    // =========================================================== Collection

    /** {@inheritDoc} */
    @Override
    public int size()
    {
        return delegate.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(Object o)
    {
        return delegate.contains(o);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<E> iterator()
    {
        return delegate.iterator();
    }

    /** {@inheritDoc} */
    @Override
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T[] toArray(T[] a)
    {
        return delegate.toArray(a);
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(E e)
    {
        return delegate.add(e);
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Object o)
    {
        return delegate.remove(o);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        return delegate.containsAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        return delegate.addAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(int index, Collection<? extends E> c)
    {
        return delegate.addAll(index, c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        return delegate.removeAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        return delegate.retainAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public void clear()
    {
        delegate.clear();
    }

    // ================================================================== List

    /** {@inheritDoc} */
    @Override
    public E get(int index)
    {
        return delegate.get(index);
    }

    /** {@inheritDoc} */
    @Override
    public E set(int index, E element)
    {
        return delegate.set(index, element);
    }

    /** {@inheritDoc} */
    @Override
    public void add(int index, E element)
    {
        delegate.add(index, element);
    }

    /** {@inheritDoc} */
    @Override
    public E remove(int index)
    {
        return delegate.remove(index);
    }

    /** {@inheritDoc} */
    @Override
    public int indexOf(Object o)
    {
        return delegate.indexOf(o);
    }

    /** {@inheritDoc} */
    @Override
    public int lastIndexOf(Object o)
    {
        return delegate.lastIndexOf(o);
    }

    /** {@inheritDoc} */
    @Override
    public ListIterator<E> listIterator()
    {
        return delegate.listIterator();
    }

    /** {@inheritDoc} */
    @Override
    public ListIterator<E> listIterator(int index)
    {
        return delegate.listIterator(index);
    }

    /** {@inheritDoc} */
    @Override
    public List<E> subList(int fromIndex, int toIndex)
    {
        return delegate.subList(fromIndex, toIndex);
    }
}
