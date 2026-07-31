/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A thread-safe utility class that ensures duplicate concurrent executions of the
 * same operation (identified by a key) are coalesced. Only one execution runs at
 * a time, and all concurrent callers for that key wait and receive the same result.
 *
 * <p>This prevents "cache stampedes" or "dogpiling" to backend systems.</p>
 *
 * @param <K> the type of the key
 * @param <V> the type of the result value
 * @author franci
 * @since 1.8
 */
public final class SingleFlight<K, V>
{
    private final ConcurrentHashMap<K, Flight<V>> flights = new ConcurrentHashMap<>();

    /**
     * Constructs a new SingleFlight manager.
     */
    public SingleFlight()
    {
    }

    /**
     * Executes the task for the given key. If a task for the same key is already in progress,
     * the calling thread waits until it finishes and returns its result (or throws its exception).
     *
     * @param key the identifier for the operation; must not be {@code null}
     * @param task the operation to execute; must not be {@code null}
     * @return the result of the operation
     * @throws Exception if the operation throws an exception
     * @throws NullPointerException if key or task is null
     */
    public V call(K key, Callable<V> task) throws Exception
    {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(task, "task must not be null");

        Flight<V> newFlight = new Flight<>();
        Flight<V> flight = flights.putIfAbsent(key, newFlight);
        boolean isLeader = (flight == null);

        if (isLeader)
        {
            flight = newFlight;
            V value = null;
            Throwable error = null;
            try
            {
                value = task.call();
                return value;
            }
            catch (Throwable t)
            {
                error = t;
                if (t instanceof Exception)
                {
                    throw (Exception) t;
                }
                if (t instanceof Error)
                {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }
            finally
            {
                flights.remove(key, flight);
                flight.complete(value, error);
            }
        }
        else
        {
            return flight.await();
        }
    }

    /**
     * Executes the supplier for the given key. If a supplier/task for the same key is
     * already in progress, the calling thread waits until it finishes and returns its result.
     *
     * @param key the identifier for the operation; must not be {@code null}
     * @param supplier the supplier to execute; must not be {@code null}
     * @return the result of the operation
     * @throws NullPointerException if key or supplier is null
     * @throws RuntimeException if the supplier throws a runtime exception
     */
    public V call(K key, Supplier<V> supplier)
    {
        Objects.requireNonNull(supplier, "supplier must not be null");
        try
        {
            Callable<V> task = supplier::get;
            return call(key, task);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes the function for the given key. If a function/task for the same key is
     * already in progress, the calling thread waits until it finishes and returns its result.
     *
     * @param key the identifier for the operation; must not be {@code null}
     * @param function the function to execute; must not be {@code null}
     * @return the result of the operation
     * @throws NullPointerException if key or function is null
     * @throws RuntimeException if the function throws a runtime exception
     */
    public V call(K key, Function<? super K, ? extends V> function)
    {
        Objects.requireNonNull(function, "function must not be null");
        try
        {
            Callable<V> task = () -> function.apply(key);
            return call(key, task);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns whether an operation is currently in progress for the specified key.
     *
     * @param key the key to check; must not be {@code null}
     * @return {@code true} if an operation for the key is currently running, otherwise {@code false}
     * @throws NullPointerException if key is null
     */
    public boolean isRunning(K key)
    {
        Objects.requireNonNull(key, "key must not be null");
        return flights.containsKey(key);
    }

    /**
     * Returns the number of currently active operations.
     *
     * @return the number of in-flight operations
     */
    public int size()
    {
        return flights.size();
    }

    private static final class Flight<V>
    {
        private final Object lock = new Object();
        private V result;
        private Throwable exception;
        private boolean done;

        V await() throws Exception
        {
            synchronized (lock)
            {
                while (!done)
                {
                    try
                    {
                        lock.wait();
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
                if (exception != null)
                {
                    if (exception instanceof Exception)
                    {
                        throw (Exception) exception;
                    }
                    if (exception instanceof Error)
                    {
                        throw (Error) exception;
                    }
                    throw new RuntimeException(exception);
                }
                return result;
            }
        }

        void complete(V value, Throwable error)
        {
            synchronized (lock)
            {
                this.result = value;
                this.exception = error;
                this.done = true;
                lock.notifyAll();
            }
        }
    }
}
