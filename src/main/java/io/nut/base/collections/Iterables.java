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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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

    /**
     * Associates each element of the source {@link Iterable} with a key generated by the
     * provided key function, returning a {@link Map} containing the elements.
     *
     * <p>If multiple elements map to the same key, the last one wins.</p>
     *
     * @param source the source iterable; must not be {@code null}
     * @param keyFunction the function to extract keys; must not be {@code null}
     * @param <T> the type of elements in the source
     * @param <K> the type of keys in the resulting map
     * @return a Map containing the associated elements; never {@code null}
     * @throws NullPointerException if source or keyFunction is null
     */
    public static <T, K> Map<K, T> associateBy(Iterable<T> source, Function<? super T, ? extends K> keyFunction)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(keyFunction, "keyFunction must not be null");

        Map<K, T> map = new HashMap<>();
        for (T element : source)
        {
            map.put(keyFunction.apply(element), element);
        }
        return map;
    }

    /**
     * Associates each element of the source {@link Iterable} with a key and value generated
     * by the provided key and value functions, returning a {@link Map} containing the values.
     *
     * <p>If multiple elements map to the same key, the last one wins.</p>
     *
     * @param source the source iterable; must not be {@code null}
     * @param keyFunction the function to extract keys; must not be {@code null}
     * @param valueFunction the function to extract values; must not be {@code null}
     * @param <T> the type of elements in the source
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @return a Map containing the associated values; never {@code null}
     * @throws NullPointerException if source, keyFunction, or valueFunction is null
     */
    public static <T, K, V> Map<K, V> associateBy(Iterable<T> source, Function<? super T, ? extends K> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(keyFunction, "keyFunction must not be null");
        Objects.requireNonNull(valueFunction, "valueFunction must not be null");

        Map<K, V> map = new HashMap<>();
        for (T element : source)
        {
            map.put(keyFunction.apply(element), valueFunction.apply(element));
        }
        return map;
    }

    /**
     * Associates each element of the source {@link Iterable} with a key generated by the
     * provided key function, returning a custom {@link Map} generated by the map supplier.
     *
     * <p>If multiple elements map to the same key, the last one wins.</p>
     *
     * @param source the source iterable; must not be {@code null}
     * @param keyFunction the function to extract keys; must not be {@code null}
     * @param mapSupplier a supplier providing the destination map; must not be {@code null}
     * @param <T> the type of elements in the source
     * @param <K> the type of keys in the resulting map
     * @return a Map containing the associated elements; never {@code null}
     * @throws NullPointerException if source, keyFunction, or mapSupplier is null
     */
    public static <T, K> Map<K, T> associateBy(Iterable<T> source, Function<? super T, ? extends K> keyFunction, Supplier<Map<K, T>> mapSupplier)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(keyFunction, "keyFunction must not be null");
        Objects.requireNonNull(mapSupplier, "mapSupplier must not be null");

        Map<K, T> map = mapSupplier.get();
        for (T element : source)
        {
            map.put(keyFunction.apply(element), element);
        }
        return map;
    }

    /**
     * Associates each element of the source {@link Iterable} with a key and value generated
     * by the provided key and value functions, returning a custom {@link Map} generated by
     * the map supplier.
     *
     * <p>If multiple elements map to the same key, the last one wins.</p>
     *
     * @param source the source iterable; must not be {@code null}
     * @param keyFunction the function to extract keys; must not be {@code null}
     * @param valueFunction the function to extract values; must not be {@code null}
     * @param mapSupplier a supplier providing the destination map; must not be {@code null}
     * @param <T> the type of elements in the source
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @return a Map containing the associated values; never {@code null}
     * @throws NullPointerException if source, keyFunction, valueFunction, or mapSupplier is null
     */
    public static <T, K, V> Map<K, V> associateBy(Iterable<T> source, Function<? super T, ? extends K> keyFunction, Function<? super T, ? extends V> valueFunction, Supplier<Map<K, V>> mapSupplier)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(keyFunction, "keyFunction must not be null");
        Objects.requireNonNull(valueFunction, "valueFunction must not be null");
        Objects.requireNonNull(mapSupplier, "mapSupplier must not be null");

        Map<K, V> map = mapSupplier.get();
        for (T element : source)
        {
            map.put(keyFunction.apply(element), valueFunction.apply(element));
        }
        return map;
    }

    /**
     * Associates each element of the source {@link Iterable} with a key generated by the
     * key mapper, resolving any duplicate key collisions using the merge function.
     *
     * @param source the source iterable; must not be {@code null}
     * @param keyMapper the function to extract keys; must not be {@code null}
     * @param mergeFunction a function to resolve collisions between two elements mapping to the same key; must not be {@code null}
     * @param <T> the type of elements in the source
     * @param <K> the type of keys in the resulting map
     * @return a Map containing the associated elements; never {@code null}
     * @throws NullPointerException if source, keyMapper, or mergeFunction is null
     */
    public static <T, K> Map<K, T> associateBy(Iterable<T> source, Function<? super T, ? extends K> keyMapper, BinaryOperator<T> mergeFunction)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Objects.requireNonNull(mergeFunction, "mergeFunction must not be null");

        Map<K, T> map = new HashMap<>();
        for (T element : source)
        {
            K key = keyMapper.apply(element);
            map.merge(key, element, mergeFunction);
        }
        return map;
    }

    /**
     * Returns {@code true} if at least one element in the source {@link Iterable}
     * satisfies the given predicate.
     *
     * @param source the source iterable; must not be {@code null}
     * @param predicate the predicate to evaluate elements against; must not be {@code null}
     * @param <T> the type of elements in the source
     * @return {@code true} if at least one element satisfies the predicate, {@code false} otherwise
     * @throws NullPointerException if source or predicate is null
     */
    public static <T> boolean any(Iterable<T> source, Predicate<? super T> predicate)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(predicate, "predicate must not be null");
        for (T element : source)
        {
            if (predicate.test(element))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if all elements in the source {@link Iterable}
     * satisfy the given predicate.
     *
     * @param source the source iterable; must not be {@code null}
     * @param predicate the predicate to evaluate elements against; must not be {@code null}
     * @param <T> the type of elements in the source
     * @return {@code true} if all elements satisfy the predicate, {@code false} otherwise
     * @throws NullPointerException if source or predicate is null
     */
    public static <T> boolean all(Iterable<T> source, Predicate<? super T> predicate)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(predicate, "predicate must not be null");
        for (T element : source)
        {
            if (!predicate.test(element))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if no elements in the source {@link Iterable}
     * satisfy the given predicate.
     *
     * @param source the source iterable; must not be {@code null}
     * @param predicate the predicate to evaluate elements against; must not be {@code null}
     * @param <T> the type of elements in the source
     * @return {@code true} if no elements satisfy the predicate, {@code false} otherwise
     * @throws NullPointerException if source or predicate is null
     */
    public static <T> boolean none(Iterable<T> source, Predicate<? super T> predicate)
    {
        return !any(source, predicate);
    }

    /**
     * Counts the total number of elements in the source {@link Iterable}.
     *
     * @param source the source iterable; must not be {@code null}
     * @param <T> the type of elements in the source
     * @return the number of elements in the source
     * @throws NullPointerException if source is null
     */
    public static <T> long count(Iterable<T> source)
    {
        Objects.requireNonNull(source, "source must not be null");
        if (source instanceof java.util.Collection)
        {
            return ((java.util.Collection<?>) source).size();
        }
        long count = 0;
        for (T ignored : source)
        {
            count++;
        }
        return count;
    }

    /**
     * Counts the number of elements in the source {@link Iterable} that satisfy
     * the given predicate.
     *
     * @param source the source iterable; must not be {@code null}
     * @param predicate the predicate to evaluate elements against; must not be {@code null}
     * @param <T> the type of elements in the source
     * @return the number of elements satisfying the predicate
     * @throws NullPointerException if source or predicate is null
     */
    public static <T> long count(Iterable<T> source, Predicate<? super T> predicate)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(predicate, "predicate must not be null");
        long count = 0;
        for (T element : source)
        {
            if (predicate.test(element))
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Concatenates multiple {@link Iterable} sources into a single lazy {@link Iterable}.
     *
     * <p>The returned iterable's iterator retrieves iterators from the input sources lazily,
     * one after another. If any source is empty, it is skipped.</p>
     *
     * @param sources the source iterables to concatenate; must not be {@code null} and must not contain {@code null}
     * @param <T> the common element type
     * @return a single concatenated lazy iterable; never {@code null}
     * @throws NullPointerException if sources is null or any individual source is null
     */
    @SafeVarargs
    public static <T> Iterable<T> concat(Iterable<? extends T>... sources)
    {
        Objects.requireNonNull(sources, "sources must not be null");
        for (Iterable<? extends T> source : sources)
        {
            Objects.requireNonNull(source, "each source must not be null");
        }
        return () -> new Iterator<T>()
        {
            private int currentIdx = 0;
            private Iterator<? extends T> currentIterator = null;

            private Iterator<? extends T> getNextIterator()
            {
                while (currentIterator == null || !currentIterator.hasNext())
                {
                    if (currentIdx >= sources.length)
                    {
                        return null;
                    }
                    Iterable<? extends T> nextIterable = sources[currentIdx++];
                    currentIterator = nextIterable.iterator();
                }
                return currentIterator;
            }

            @Override
            public boolean hasNext()
            {
                return getNextIterator() != null;
            }

            @Override
            public T next()
            {
                Iterator<? extends T> iterator = getNextIterator();
                if (iterator == null)
                {
                    throw new NoSuchElementException();
                }
                return iterator.next();
            }
        };
    }

    /**
     * Filters elements of the source {@link Iterable} lazily according to the provided predicate.
     *
     * @param source the source iterable; must not be {@code null}
     * @param predicate the predicate to evaluate elements against; must not be {@code null}
     * @param <T> the type of elements in the source
     * @return a filtered lazy {@link Iterable}; never {@code null}
     * @throws NullPointerException if source or predicate is null
     */
    public static <T> Iterable<T> filter(Iterable<T> source, Predicate<? super T> predicate)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(predicate, "predicate must not be null");

        return () -> new Iterator<T>()
        {
            private final Iterator<T> iterator = source.iterator();
            private T nextElement = null;
            private boolean hasNextCached = false;

            private boolean calculateNext()
            {
                if (hasNextCached)
                {
                    return true;
                }
                while (iterator.hasNext())
                {
                    T element = iterator.next();
                    if (predicate.test(element))
                    {
                        nextElement = element;
                        hasNextCached = true;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean hasNext()
            {
                return calculateNext();
            }

            @Override
            public T next()
            {
                if (!hasNext())
                {
                    throw new NoSuchElementException();
                }
                T result = nextElement;
                nextElement = null;
                hasNextCached = false;
                return result;
            }
        };
    }
}
