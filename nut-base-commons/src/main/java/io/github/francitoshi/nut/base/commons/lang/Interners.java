/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory methods for {@link Interner} implementations.
 *
 * @author franci
 */
public final class Interners
{
    private Interners()
    {
    }

    /**
     * Returns a thread-safe interner that keeps strong references to the
     * interned values.
     * <p>
     * Values remain interned as long as the interner itself is reachable. This
     * is the fastest option when the set of values is bounded or when the
     * interner is short-lived.
     *
     * @param <E> the type of interned values
     * @param expectedSize initial size
     * @return a new strong interner
     */
    public static <E> Interner<E> strong(int expectedSize)
    {
        return new StrongInterner<>(expectedSize);
    }
    
    /**
     * Returns a thread-safe interner that keeps strong references to the
     * interned values.
     * <p>
     * Values remain interned as long as the interner itself is reachable. This
     * is the fastest option when the set of values is bounded or when the
     * interner is short-lived.
     *
     * @param <E> the type of interned values
     * @return a new strong interner
     */
    public static <E> Interner<E> strong()
    {
        return new StrongInterner<>();
    }

    /**
     * Returns a thread-safe interner that keeps only weak references to the
     * interned values.
     * <p>
     * A value may be garbage collected when no one else holds a reference to
     * it. This avoids retaining values indefinitely, at the cost of a slightly
     * higher overhead than {@link #strong()}.
     *
     * @param <E> the type of interned values
     * @param expectedSize initial size
     * @return a new weak interner
     */
    public static <E> Interner<E> weak(int expectedSize)
    {
        return new WeakInterner<>(expectedSize);
    }
    /**
     * Returns a thread-safe interner that keeps only weak references to the
     * interned values.
     * <p>
     * A value may be garbage collected when no one else holds a reference to
     * it. This avoids retaining values indefinitely, at the cost of a slightly
     * higher overhead than {@link #strong()}.
     *
     * @param <E> the type of interned values
     * @return a new weak interner
     */
    public static <E> Interner<E> weak()
    {
        return new WeakInterner<>();
    }

    private static final class StrongInterner<E> implements Interner<E>
    {
        private final ConcurrentHashMap<E,E> map;

        public StrongInterner(int expectedSize)
        {
            this.map = new ConcurrentHashMap<>(expectedSize);
        }
        public StrongInterner()
        {
            this.map = new ConcurrentHashMap<>();
        }

        @Override
        public E intern(E sample)
        {
            Objects.requireNonNull(sample, "sample");
            E canonical = map.putIfAbsent(sample, sample);
            return canonical != null ? canonical : sample;
        }
    }

    private static final class WeakInterner<E> implements Interner<E>
    {
        private final ConcurrentHashMap<WeakKey<E>,WeakKey<E>> map;
        private final ReferenceQueue<E> queue = new ReferenceQueue<>();

        public WeakInterner(int expectedSize)
        {
            this.map = new ConcurrentHashMap<>(expectedSize);
        }
        
        public WeakInterner()
        {
            this.map = new ConcurrentHashMap<>();
        }
        
        @Override
        public E intern(E sample)
        {
            Objects.requireNonNull(sample, "sample");
            expungeStaleEntries();

            WeakKey<E> key = new WeakKey<>(sample, queue);
            while (true)
            {
                WeakKey<E> canonical = map.putIfAbsent(key, key);
                if (canonical == null)
                {
                    return sample;
                }
                E value = canonical.get();
                if (value != null)
                {
                    return value;
                }
                map.remove(canonical, canonical);
            }
        }

        @SuppressWarnings("unchecked")
        private void expungeStaleEntries()
        {
            Reference<? extends E> ref;
            while ((ref = queue.poll()) != null)
            {
                WeakKey<E> key = (WeakKey<E>) ref;
                map.remove(key, key);
            }
        }
    }

    private static final class WeakKey<E> extends WeakReference<E>
    {
        private final int hash;

        private WeakKey(E referent, ReferenceQueue<E> queue)
        {
            super(referent, queue);
            this.hash = referent.hashCode();
        }

        @Override
        public int hashCode()
        {
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof WeakKey))
            {
                return false;
            }
            E value = get();
            Object other = ((WeakKey<?>) obj).get();
            return value != null && (value == other || value.equals(other));
        }
    }
}
