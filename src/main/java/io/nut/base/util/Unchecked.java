/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * Utility class for executing code that declares checked exceptions without
 * requiring explicit {@code try/catch} blocks or {@code throws} clauses.
 *
 * <p>Checked exceptions are rethrown as-is using the {@link #sneakyThrow(Throwable)}
 * trick (type-erasure cast), so the exception remains in its original form in the
 * stack trace without being wrapped in {@link RuntimeException}.</p>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * // In a lambda context (e.g. Stream.map):
 * stream.map(Unchecked.function(reader -> Files.readString(path)));
 *
 * // Standalone statement:
 * Unchecked.run(() -> socket.connect(endpoint));
 *
 * // Supplier:
 * String content = Unchecked.get(() -> Files.readString(path));
 * }</pre>
 *
 * @author franci
 */
public final class Unchecked
{

    private Unchecked()
    {
    }

    /**
     * A functional interface equivalent to {@link Runnable} but allowing
     * checked exceptions to be thrown.
     */
    @FunctionalInterface
    public interface CheckedRunnable
    {
        void run() throws Throwable;
    }

    /**
     * A functional interface equivalent to {@link java.util.function.Supplier}
     * but allowing checked exceptions to be thrown.
     *
     * @param <T> the return type
     */
    @FunctionalInterface
    public interface CheckedSupplier<T>
    {
        T get() throws Throwable;
    }

    /**
     * A functional interface equivalent to {@link Function} but allowing
     * checked exceptions to be thrown.
     *
     * @param <T> the input type
     * @param <R> the result type
     */
    @FunctionalInterface
    public interface CheckedFunction<T, R>
    {
        R apply(T t) throws Throwable;
    }

    /**
     * Executes a {@link CheckedRunnable} that may throw a checked exception.
     *
     * @param runnable the action to execute; must not be {@code null}
     * @throws NullPointerException if {@code runnable} is null
     */
    public static void run(CheckedRunnable runnable)
    {
        Objects.requireNonNull(runnable);
        try
        {
            runnable.run();
        }
        catch (Throwable t)
        {
            throw sneakyThrow(t);
        }
    }

    /**
     * Obtains a value from a {@link CheckedSupplier} that may throw a checked
     * exception.
     *
     * @param <T>    the return type
     * @param supplier the supplier to call; must not be {@code null}
     * @return the value produced by the supplier
     * @throws NullPointerException if {@code supplier} is null
     */
    public static <T> T get(CheckedSupplier<T> supplier)
    {
        Objects.requireNonNull(supplier);
        try
        {
            return supplier.get();
        }
        catch (Throwable t)
        {
            throw sneakyThrow(t);
        }
    }

    /**
     * Converts a {@link CheckedFunction} into a standard {@link Function},
     * suitable for use in streams and other functional APIs.
     *
     * @param <T>    the input type
     * @param <R>    the result type
     * @param function the checked function to wrap; must not be {@code null}
     * @return a {@link Function} that applies the same logic but rethrows
     *         checked exceptions without wrapping
     * @throws NullPointerException if {@code function} is null
     */
    public static <T, R> Function<T, R> function(CheckedFunction<T, R> function)
    {
        Objects.requireNonNull(function);
        return t ->
        {
            try
            {
                return function.apply(t);
            }
            catch (Throwable ex)
            {
                throw sneakyThrow(ex);
            }
        };
    }

    /**
     * Rethrows any {@link Throwable} without wrapping it in
     * {@link RuntimeException}, taking advantage of type erasure.
     *
     * <p>This allows checked exceptions to be propagated without requiring a
     * {@code throws} declaration on the calling method.</p>
     *
     * @param <E>    the throwable type
     * @param t      the throwable to rethrow
     * @return this method never returns; the return type is declared so that
     *         the compiler accepts {@code throw sneakyThrow(t)} in any context
     * @throws Throwable always throws {@code t}
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> RuntimeException sneakyThrow(Throwable t) throws E
    {
        throw (E) t;
    }
}