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
 * A terminal pipeline stage that doubles as a {@link Set}: every message
 * received via the {@link Bee} machinery is added to a delegate
 * {@link Set}{@code <T>} supplied at construction time, and every {@code Set}
 * method is delegated to that same set.
 * <p>
 * This is useful for collecting the de-duplicated output of a pipeline into an
 * ordinary {@code Set} without exposing the asynchronous Bee/Hive infrastructure
 * to the collecting code:
 * <pre>{@code
 * Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>());
 * SetBee<String> sink = hive.set(seen);
 * pipe.linkTo(sink);
 * // ... send messages to pipe ...
 * Hive.shutdownAndAwaitTermination(true, false, pipe);
 * // seen now contains all unique processed messages
 * }</pre>
 * <p>
 * <strong>Thread safety:</strong> a plain {@link java.util.HashSet} is not
 * thread-safe. If the {@code SetBee} is used with a Hive (asynchronous mode),
 * multiple worker threads may call {@link #receive(Object)} concurrently. In
 * that case the caller must supply a thread-safe {@code Set}, e.g.
 * {@code Collections.newSetFromMap(new ConcurrentHashMap<>())}.
 *
 * @param <T> the type of elements held by the delegate set
 */
public class SetBee<T> extends Bee<T> implements Set<T>
{
    private final Set<T> delegate;

    /**
     * Full constructor.
     *
     * @param threads   the maximum number of concurrent worker threads
     * @param hive      the Hive thread pool, or {@code null} for synchronous mode
     * @param queueSize the internal Bee queue capacity (0 = default)
     * @param delegate  the {@code Set} that stores received elements; must not be
     *                  {@code null}
     */
    public SetBee(int threads, Hive hive, int queueSize, Set<T> delegate)
    {
        super(threads, hive, queueSize);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a SetBee with the given thread count and Hive, using the
     * default internal queue size.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param hive     the Hive thread pool, or {@code null} for synchronous mode
     * @param delegate the delegate set; must not be {@code null}
     */
    public SetBee(int threads, Hive hive, Set<T> delegate)
    {
        super(threads, hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a SetBee attached to the given Hive with the default thread
     * count and internal queue size.
     *
     * @param hive     the Hive thread pool, or {@code null} for synchronous mode
     * @param delegate the delegate set; must not be {@code null}
     */
    public SetBee(Hive hive, Set<T> delegate)
    {
        super(hive);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a standalone SetBee with the given thread count but no Hive.
     * A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param delegate the delegate set; must not be {@code null}
     */
    public SetBee(int threads, Set<T> delegate)
    {
        super(threads);
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Constructs a standalone SetBee with the default thread count and no Hive.
     * A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param delegate the delegate set; must not be {@code null}
     */
    public SetBee(Set<T> delegate)
    {
        super();
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Receives a message from the Bee pipeline and adds it to the delegate set
     * via {@link Set#add(Object)}. If the set already contains an equal element,
     * the message is silently ignored (standard {@code Set} semantics).
     *
     * @param t the message/element to add to the delegate set
     */
    @Override
    protected void receive(T t)
    {
        delegate.add(t);
    }

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
    public Iterator<T> iterator()
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
    public <A> A[] toArray(A[] a)
    {
        return delegate.toArray(a);
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(T t)
    {
        return delegate.add(t);
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
    public boolean addAll(Collection<? extends T> c)
    {
        return delegate.addAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        return delegate.retainAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        return delegate.removeAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public void clear()
    {
        delegate.clear();
    }

    /**
     * Returns the hash code of the delegate set, as specified by
     * {@link Set#hashCode()}.
     *
     * @return the hash code of the delegate set
     */
    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    /**
     * Compares the specified object with the delegate set for equality, as
     * specified by {@link Set#equals(Object)}.
     *
     * @param obj the object to compare with
     * @return {@code true} if the delegate set is equal to {@code obj}
     */
    @Override
    public boolean equals(Object obj)
    {
        return this.delegate.equals(obj);
    }
}
