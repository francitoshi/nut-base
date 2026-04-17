/*
 *  Lazy.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A thread-safe, lazily initialized container.
 *
 * <p>The value is computed exactly once, on the first call to {@link #get()},
 * using a lock-free double-checked latch built on {@link AtomicReference}.
 *
 * <h2>Usage</h2>
 *
 * <p>Basic lazy initialization:
 * <pre>{@code
 * Lazy<DatabaseConnection> conn = new Lazy<>(() -> connectToDatabase());
 *
 * // In any thread, at any time — initialized at most once:
 * conn.get().query("SELECT ...");
 * }</pre>
 *
 * <p>Lazy field inside a class with multiple instances:
 * <pre>{@code
 * public class UserService {
 *     private final Lazy<List<Permission>> permissions =
 *         new Lazy<>(() -> permissionRepo.findAll());
 *
 *     public List<Permission> getPermissions() {
 *         return permissions.get();
 *     }
 * }
 * }</pre>
 *
 * <p>Checking initialization state without triggering it:
 * <pre>{@code
 * if (conn.isInitialized()) {
 *     conn.get().close();
 * }
 * }</pre>
 *
 * <p>Wrapping an already-known value:
 * <pre>{@code
 * Lazy<String> greeting = Lazy.of("Hello");
 * }</pre>
 *
 * <h2>Error handling</h2>
 *
 * <p>If the supplier throws, the exception propagates to the caller and the
 * {@code Lazy} remains uninitialized, allowing a future call to retry:
 * <pre>{@code
 * Lazy<Connection> conn = new Lazy<>(() -> {
 *     throw new RuntimeException("DB unavailable");
 * });
 *
 * try {
 *     conn.get(); // throws
 * } catch (RuntimeException e) {
 *     // conn is still uninitialized — next get() will retry
 * }
 * }</pre>
 *
 * @param <T> the type of the lazily computed value
 */
public final class Lazy<T>
{

    /**
     * Sentinel placed in the reference before initialization begins, so we can
     * distinguish "not yet computed" from a legitimately null value.
     */
    @SuppressWarnings("rawtypes")
    private static final Object UNSET = new Object();

    /**
     * Holds either {@link #UNSET} or the real value (including {@code null}).
     * Using AtomicReference gives us volatile semantics without a lock on the
     * happy path.
     */
    @SuppressWarnings("unchecked")
    private final AtomicReference<Object> ref = new AtomicReference<>(UNSET);

    /**
     * Called at most once to produce the value.
     */
    private volatile Supplier<T> supplier;

    // ------------------------------------------------------------------ //
    // Construction
    // ------------------------------------------------------------------ //
    /**
     * Creates a {@code Lazy} whose value will be computed by {@code supplier}
     * on the first call to {@link #get()}.
     *
     * @param supplier the factory; must not be {@code null}
     * @throws NullPointerException if {@code supplier} is null
     */
    public Lazy(Supplier<T> supplier)
    {
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
    }

    /**
     * Creates an already-initialized {@code Lazy} wrapping {@code value}.
     *
     * @param value the pre-computed value (may be {@code null})
     * @return an initialized {@code Lazy}
     */
    public static <T> Lazy<T> of(T value)
    {
        Lazy<T> lazy = new Lazy<>(() -> value);
        lazy.ref.set(value);          // skip initialization on first get()
        lazy.supplier = null;         // allow GC of the supplier immediately
        return lazy;
    }

    // ------------------------------------------------------------------ //
    // Core API
    // ------------------------------------------------------------------ //
    /**
     * Returns the lazily computed value, initializing it on the first call.
     *
     * <p>
     * If the supplier throws, the exception propagates to the caller and the
     * lazy remains <em>uninitialized</em> so that a future call may retry.
     *
     * @return the computed value (may be {@code null} if the supplier returns
     * null)
     */
    @SuppressWarnings("unchecked")
    public T get()
    {
        Object current = ref.get();
        if (current != UNSET)
        {
            return (T) current;          // fast path — already initialized
        }
        return initialize();
    }

    /**
     * Returns {@code true} if the value has been computed and cached.
     */
    public boolean isInitialized()
    {
        return ref.get() != UNSET;
    }

    /**
     * Returns the cached value without triggering initialization.
     *
     * @return the cached value, or {@code empty} if not yet initialized
     */
    @SuppressWarnings("unchecked")
    public java.util.Optional<T> getIfInitialized()
    {
        Object current = ref.get();
        return current == UNSET ? java.util.Optional.empty()
                : java.util.Optional.ofNullable((T) current);
    }

    // ------------------------------------------------------------------ //
    // Internal
    // ------------------------------------------------------------------ //
    /**
     * Slow path: computes the value exactly once using a synchronized block to
     * serialize concurrent first-callers, then publishes via AtomicReference.
     */
    @SuppressWarnings("unchecked")
    private T initialize()
    {
        synchronized (this)
        {
            // Re-check inside the lock — another thread may have won the race
            Object current = ref.get();
            if (current != UNSET)
            {
                return (T) current;
            }

            T value = supplier.get();  // may throw — ref stays UNSET if it does
            ref.set(value);
            supplier = null;           // allow GC of the supplier & its captures
            return value;
        }
    }

    // ------------------------------------------------------------------ //
    // Object overrides
    // ------------------------------------------------------------------ //
    @Override
    public String toString()
    {
        Object current = ref.get();
        return current == UNSET ? "Lazy[uninitialized]"
                : "Lazy[" + current + "]";
    }
}
