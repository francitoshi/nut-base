/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A utility class providing lazy operations on {@link Iterable} sources, such as
 * chunking, sliding windows, and zipping.
 *
 * <p><b>Reuse:</b> each iterable returned by this class calls {@code iterator()} on its
 * source(s) every time its own {@code iterator()} is invoked. Reiterating the result is
 * therefore only as safe as reiterating the underlying source(s); sources that only
 * support a single pass (e.g. one backed by an already-consumed {@link Iterator}) will
 * silently yield an empty or partial iterable on subsequent passes.</p>
 *
 * <p><b>Mutability:</b> the {@link Iterator}s produced by the returned iterables do not
 * support {@link Iterator#remove()} and will throw {@link UnsupportedOperationException}
 * if it is called.</p>
 *
 * <p><b>Thread safety:</b> the {@link Iterator}s produced by the returned iterables are
 * not thread-safe and must not be shared across threads without external synchronization.</p>
 */
public final class Iterables
{
    private Iterables()
    {
    }

    /**
     * Splits an {@link Iterable} into fixed-size chunks.
     *
     * <p>The returned {@link Iterable} executes lazily. The last chunk may contain
     * fewer elements than the requested size if the source has remaining elements
     * but not enough to fill a complete chunk.</p>
     *
     * @param source the source iterable to divide; must not be {@code null}
     * @param size the size of each chunk; must be > 0
     * @param <T> the element type
     * @return an iterable producing unmodifiable lists of elements; never {@code null}
     * @throws IllegalArgumentException if size is less than or equal to 0
     * @throws NullPointerException if source is null
     */
    public static <T> Iterable<List<T>> chunked(Iterable<T> source, int size)
    {
        Objects.requireNonNull(source, "source must not be null");
        if (size <= 0)
        {
            throw new IllegalArgumentException("size must be > 0: " + size);
        }
        return () -> new Iterator<List<T>>()
        {
            private final Iterator<T> iterator = source.iterator();

            @Override
            public boolean hasNext()
            {
                return iterator.hasNext();
            }

            @Override
            public List<T> next()
            {
                if (!hasNext())
                {
                    throw new NoSuchElementException();
                }
                List<T> chunk = new ArrayList<>(size);
                for (int i = 0; i < size && iterator.hasNext(); i++)
                {
                    chunk.add(iterator.next());
                }
                return Collections.unmodifiableList(chunk);
            }
        };
    }

    /**
     * Returns a sliding window of the specified size over the source {@link Iterable}.
     *
     * <p>The returned {@link Iterable} executes lazily. If the source contains fewer
     * elements than the requested size, no windows will be returned.</p>
     *
     * @param source the source iterable; must not be {@code null}
     * @param size the size of the sliding window; must be > 0
     * @param <T> the element type
     * @return an iterable producing unmodifiable lists representing sliding windows; never {@code null}
     * @throws IllegalArgumentException if size is less than or equal to 0
     * @throws NullPointerException if source is null
     */
    public static <T> Iterable<List<T>> windowed(Iterable<T> source, int size)
    {
        Objects.requireNonNull(source, "source must not be null");
        if (size <= 0)
        {
            throw new IllegalArgumentException("size must be > 0: " + size);
        }
        return () -> new Iterator<List<T>>()
        {
            private final Iterator<T> iterator = source.iterator();
            private final Deque<T> buffer = new ArrayDeque<>(size);
            private boolean initialized = false;

            private void initialize()
            {
                if (initialized)
                {
                    return;
                }
                initialized = true;
                for (int i = 0; i < size && iterator.hasNext(); i++)
                {
                    buffer.addLast(iterator.next());
                }
            }

            @Override
            public boolean hasNext()
            {
                initialize();
                return buffer.size() == size;
            }

            @Override
            public List<T> next()
            {
                if (!hasNext())
                {
                    throw new NoSuchElementException();
                }
                List<T> window = new ArrayList<>(buffer);
                if (iterator.hasNext())
                {
                    buffer.removeFirst();
                    buffer.addLast(iterator.next());
                }
                else
                {
                    buffer.clear();
                }
                return Collections.unmodifiableList(window);
            }
        };
    }

    /**
     * Combines elements of two {@link Iterable} sources pairwise using the provided mapper function.
     *
     * <p>The returned {@link Iterable} executes lazily and will terminate as soon as either
     * of the source iterables runs out of elements.</p>
     *
     * @param first the first source iterable; must not be {@code null}
     * @param second the second source iterable; must not be {@code null}
     * @param mapper the function to map elements from both sources; must not be {@code null}
     * @param <A> the first source element type
     * @param <B> the second source element type
     * @param <R> the mapped result type
     * @return an iterable of combined elements; never {@code null}
     * @throws NullPointerException if first, second, or mapper is null
     */
    public static <A, B, R> Iterable<R> zip(Iterable<A> first, Iterable<B> second, BiFunction<? super A, ? super B, ? extends R> mapper)
    {
        Objects.requireNonNull(first, "first must not be null");
        Objects.requireNonNull(second, "second must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");

        return () -> new Iterator<R>()
        {
            private final Iterator<A> it1 = first.iterator();
            private final Iterator<B> it2 = second.iterator();

            @Override
            public boolean hasNext()
            {
                return it1.hasNext() && it2.hasNext();
            }

            @Override
            public R next()
            {
                if (!hasNext())
                {
                    throw new NoSuchElementException();
                }
                return mapper.apply(it1.next(), it2.next());
            }
        };
    }
}
