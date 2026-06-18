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
import java.util.Objects;
import java.util.Set;

/**
 * A Bee&lt;T&gt; that is, at the same time, a Set&lt;T&gt;: every message
 * it receives via {@link #receive(Object)} is added to a delegate
 * {@code Set<T>} supplied at construction time, and every Set method is
 * forwarded ("delegated") to that same set.
 * <p>
 * Note that, unlike {@link QueueBee}, a plain {@code Set<T>} is not
 * thread-safe by itself, so callers should supply a synchronized or
 * otherwise thread-safe Set (e.g. {@code
 * Collections.newSetFromMap(new ConcurrentHashMap<>())}) if the resulting
 * SetBee is going to be written to and read from concurrently.
 *
 * @param <T> the type of elements held by the delegate set
 */
public class SetBee<T> extends Bee<T> implements Set<T>
{
    private final Set<T> delegate;

    public SetBee(int threads, Hive hive, int queueSize, Set<T> delegate)
    {
        super(threads, hive, queueSize);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public SetBee(int threads, Hive hive, Set<T> delegate)
    {
        super(threads, hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public SetBee(Hive hive, Set<T> delegate)
    {
        super(hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public SetBee(int threads, Set<T> delegate)
    {
        super(threads);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public SetBee(Set<T> delegate)
    {
        super();
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Receives a message and adds it to the delegate set.
     *
     * @param t the message/element to add to the delegate set
     */
    @Override
    protected void receive(T t)
    {
        delegate.add(t);
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
    public Iterator<T> iterator()
    {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    @Override
    public <A> A[] toArray(A[] a)
    {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(T t)
    {
        return delegate.add(t);
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
    public boolean addAll(Collection<? extends T> c)
    {
        return delegate.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return delegate.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return delegate.removeAll(c);
    }

    @Override
    public void clear()
    {
        delegate.clear();
    }

    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.delegate.equals(obj);
    }
    
}
