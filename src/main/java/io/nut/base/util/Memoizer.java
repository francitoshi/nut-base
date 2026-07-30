/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import io.nut.base.cache.Cache;
import io.nut.base.cache.CacheFactory;
import io.nut.base.cache.CacheType;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class Memoizer
{
    
    private static class Key
    {
        final Object memoized;
        final Object[] params;

        public Key(Object memoized, Object[] params)
        {
            this.memoized = memoized;
            this.params = params;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(memoized, key.memoized) &&
                   java.util.Arrays.equals(params, key.params);
        }

        @Override
        public int hashCode()
        {
            int result = Objects.hashCode(memoized);
            result = 31 * result + java.util.Arrays.hashCode(params);
            return result;
        }
    }

    private final boolean ts;
    private final Cache<Key,Object> cache;

    private Memoizer(Cache<Key, Object> cache, boolean ts)
    {
        this.cache = cache;
        this.ts = ts;
    }

    public Memoizer()
    {
        this.ts = false;
        this.cache = CacheFactory.getInstance(CacheType.HASH_MAP, Integer.MAX_VALUE);
    }
        
    public Memoizer(CacheType cacheType, int capacity)
    {
        this.ts = false;
        this.cache = CacheFactory.getInstance(cacheType, capacity);
    }

    public Memoizer(CacheType cacheType, int capacity, long ttlNanos)
    {
        this.ts = false;
        this.cache = CacheFactory.getInstance(cacheType, capacity, ttlNanos);
    }

    public Memoizer(CacheType cacheType, int capacity, long ttl, TimeUnit timeUnit)
    {
        this.ts = false;
        this.cache = CacheFactory.getInstance(cacheType, capacity, ttl, timeUnit);
    }

    public Memoizer threadSafe()
    {
        return ts ? this : new Memoizer(this.cache.synchronizedCache(), true);
    }

    /**
     * Returns a non-thread-safe memoized supplier.
     */
    public <T> Supplier<T> memoize(Supplier<T> supplier)
    {
        return ts ? new ThreadSafeMemoizedSupplier<>(supplier) : new MemoizedSupplier<>(supplier);
    }

    /**
     * Returns a memoized function, thread-safe if this Memoizer is threadSafe.
     */
    public <T,R> Function<T,R> memoize(Function<T,R> function)
    {
        return ts ? new ThreadSafeMemoizedFunction<>(cache, function) : new MemoizedFunction<>(cache, function);
    }

    /**
     * Returns a memoized bi-function, thread-safe if this Memoizer is threadSafe.
     */
    public <T, U, R> BiFunction<T, U, R> memoize(BiFunction<T, U, R> function)
    {
        return ts ? new ThreadSafeMemoizedBiFunction<>(cache, function) : new MemoizedBiFunction<>(cache, function);
    }

    /**
     * Returns a memoized predicate, thread-safe if this Memoizer is threadSafe.
     */
    public <T> Predicate<T> memoize(Predicate<T> predicate)
    {
        return ts ? new ThreadSafeMemoizedPredicate<>(cache, predicate) : new MemoizedPredicate<>(cache, predicate);
    }

    /**
     * Returns a memoized unary operator, thread-safe if this Memoizer is threadSafe.
     */
    public <T> UnaryOperator<T> memoize(UnaryOperator<T> operator)
    {
        return ts ? new ThreadSafeMemoizedUnaryOperator<>(cache, operator) : new MemoizedUnaryOperator<>(cache, operator);
    }

    /**
     * Returns a memoized int-predicate, thread-safe if this Memoizer is threadSafe.
     */
    public IntPredicate memoize(IntPredicate predicate)
    {
        return ts ? new ThreadSafeMemoizedIntPredicate(cache, predicate) : new MemoizedIntPredicate(cache, predicate);
    }

    /**
     * Non-thread-safe implementation for Supplier.
     */
    private static final class MemoizedSupplier<T> implements Supplier<T>
    {
        private Supplier<T> supplier;
        private T value;
        private boolean computed;

        private MemoizedSupplier(Supplier<T> supplier)
        {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        public T get()
        {
            if (!computed)
            {
                value = supplier.get();
                supplier = null;      // Allow GC
                computed = true;
            }
            return value;
        }
    }

    /**
     * Thread-safe implementation for Supplier.
     */
    private static final class ThreadSafeMemoizedSupplier<T> implements Supplier<T>
    {
        private final Supplier<T> delegate;
        private final Object lock = new Object();

        private ThreadSafeMemoizedSupplier(Supplier<T> supplier)
        {
            this.delegate = new MemoizedSupplier<>(supplier);
        }

        @Override
        public T get()
        {
            synchronized (lock)
            {
                return delegate.get();
            }
        }
    }

    private static final class MemoizedFunction<T, R> implements Function<T, R>
    {
        private final Cache<Key, Object> cache;
        private final Function<T, R> delegate;

        private MemoizedFunction(Cache<Key, Object> cache, Function<T, R> delegate)
        {
            this.cache = Objects.requireNonNull(cache);
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public R apply(T t)
        {
            Key key = new Key(delegate, new Object[]{t});
            @SuppressWarnings("unchecked")
            R result = (R) cache.get(key, k -> delegate.apply(t));
            return result;
        }
    }

    private static final class ThreadSafeMemoizedFunction<T, R> implements Function<T, R>
    {
        private final Function<T, R> delegate;
        private final Object lock = new Object();

        private ThreadSafeMemoizedFunction(Cache<Key, Object> cache, Function<T, R> delegate)
        {
            this.delegate = new MemoizedFunction<>(cache, delegate);
        }

        @Override
        public R apply(T t)
        {
            synchronized (lock)
            {
                return delegate.apply(t);
            }
        }
    }

    private static final class MemoizedBiFunction<T, U, R> implements BiFunction<T, U, R>
    {
        private final Cache<Key, Object> cache;
        private final BiFunction<T, U, R> delegate;

        private MemoizedBiFunction(Cache<Key, Object> cache, BiFunction<T, U, R> delegate)
        {
            this.cache = Objects.requireNonNull(cache);
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public R apply(T t, U u)
        {
            Key key = new Key(delegate, new Object[]{t, u});
            @SuppressWarnings("unchecked")
            R result = (R) cache.get(key, k -> delegate.apply(t, u));
            return result;
        }
    }

    private static final class ThreadSafeMemoizedBiFunction<T, U, R> implements BiFunction<T, U, R>
    {
        private final BiFunction<T, U, R> delegate;
        private final Object lock = new Object();

        private ThreadSafeMemoizedBiFunction(Cache<Key, Object> cache, BiFunction<T, U, R> delegate)
        {
            this.delegate = new MemoizedBiFunction<>(cache, delegate);
        }

        @Override
        public R apply(T t, U u)
        {
            synchronized (lock)
            {
                return delegate.apply(t, u);
            }
        }
    }

    private static final class MemoizedPredicate<T> implements Predicate<T>
    {
        private final Cache<Key, Object> cache;
        private final Predicate<T> delegate;

        private MemoizedPredicate(Cache<Key, Object> cache, Predicate<T> delegate)
        {
            this.cache = Objects.requireNonNull(cache);
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public boolean test(T t)
        {
            Key key = new Key(delegate, new Object[]{t});
            Boolean result = (Boolean) cache.get(key, k -> delegate.test(t));
            return result != null && result;
        }
    }

    private static final class ThreadSafeMemoizedPredicate<T> implements Predicate<T>
    {
        private final Predicate<T> delegate;
        private final Object lock = new Object();

        private ThreadSafeMemoizedPredicate(Cache<Key, Object> cache, Predicate<T> delegate)
        {
            this.delegate = new MemoizedPredicate<>(cache, delegate);
        }

        @Override
        public boolean test(T t)
        {
            synchronized (lock)
            {
                return delegate.test(t);
            }
        }
    }

    private static final class MemoizedUnaryOperator<T> implements UnaryOperator<T>
    {
        private final Cache<Key, Object> cache;
        private final UnaryOperator<T> delegate;

        private MemoizedUnaryOperator(Cache<Key, Object> cache, UnaryOperator<T> delegate)
        {
            this.cache = Objects.requireNonNull(cache);
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public T apply(T t)
        {
            Key key = new Key(delegate, new Object[]{t});
            @SuppressWarnings("unchecked")
            T result = (T) cache.get(key, k -> delegate.apply(t));
            return result;
        }
    }

    private static final class ThreadSafeMemoizedUnaryOperator<T> implements UnaryOperator<T>
    {
        private final UnaryOperator<T> delegate;
        private final Object lock = new Object();

        private ThreadSafeMemoizedUnaryOperator(Cache<Key, Object> cache, UnaryOperator<T> delegate)
        {
            this.delegate = new MemoizedUnaryOperator<>(cache, delegate);
        }

        @Override
        public T apply(T t)
        {
            synchronized (lock)
            {
                return delegate.apply(t);
            }
        }
    }

    private static final class MemoizedIntPredicate implements IntPredicate
    {
        private final Cache<Key, Object> cache;
        private final IntPredicate delegate;

        private MemoizedIntPredicate(Cache<Key, Object> cache, IntPredicate delegate)
        {
            this.cache = Objects.requireNonNull(cache);
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public boolean test(int value)
        {
            Key key = new Key(delegate, new Object[]{value});
            Boolean result = (Boolean) cache.get(key, k -> delegate.test(value));
            return result != null && result;
        }
    }

    private static final class ThreadSafeMemoizedIntPredicate implements IntPredicate
    {
        private final IntPredicate delegate;
        private final Object lock = new Object();

        private ThreadSafeMemoizedIntPredicate(Cache<Key, Object> cache, IntPredicate delegate)
        {
            this.delegate = new MemoizedIntPredicate(cache, delegate);
        }

        @Override
        public boolean test(int value)
        {
            synchronized (lock)
            {
                return delegate.test(value);
            }
        }
    }
}
