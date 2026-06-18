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
 * A Bee&lt;E&gt; that is, at the same time, a List&lt;E&gt;: every message
 * it receives via {@link #receive(Object)} is appended to a delegate
 * {@code List<E>} supplied at construction time, and every List method is
 * forwarded ("delegated") to that same list.
 * <p>
 * Note that, unlike {@link QueueBee}, a plain {@code List<E>} is not
 * thread-safe by itself, so callers should supply a synchronized or
 * otherwise thread-safe List (e.g. {@code Collections.synchronizedList(new
 * ArrayList<>())}) if the resulting ListBee is going to be written to and
 * read from concurrently.
 *
 * @param <E> the type of elements held by the delegate list
 */
public class ListBee<E> extends Bee<E> implements List<E>
{
    private final List<E> delegate;

    public ListBee(int threads, Hive hive, int queueSize, List<E> delegate)
    {
        super(threads, hive, queueSize);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public ListBee(int threads, Hive hive, List<E> delegate)
    {
        super(threads, hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public ListBee(Hive hive, List<E> delegate)
    {
        super(hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public ListBee(int threads, List<E> delegate)
    {
        super(threads);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public ListBee(List<E> delegate)
    {
        super();
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Receives a message and appends it to the delegate list.
     *
     * @param e the message/element to add to the delegate list
     */
    @Override
    protected void receive(E e)
    {
        delegate.add(e);
    }

    //=========================================================== Collection
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
        return delegate.iterator();
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
    public boolean add(E e)
    {
        return delegate.add(e);
    }

    @Override
    public boolean remove(Object o)
    {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        return delegate.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c)
    {
        return delegate.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return delegate.retainAll(c);
    }

    @Override
    public void clear()
    {
        delegate.clear();
    }

    //================================================================== List
    @Override
    public E get(int index)
    {
        return delegate.get(index);
    }

    @Override
    public E set(int index, E element)
    {
        return delegate.set(index, element);
    }

    @Override
    public void add(int index, E element)
    {
        delegate.add(index, element);
    }

    @Override
    public E remove(int index)
    {
        return delegate.remove(index);
    }

    @Override
    public int indexOf(Object o)
    {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator()
    {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index)
    {
        return delegate.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex)
    {
        return delegate.subList(fromIndex, toIndex);
    }
}
