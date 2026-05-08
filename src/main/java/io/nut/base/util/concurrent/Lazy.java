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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A thread-safe, lazily initialized container with optional time-to-live (TTL)
 * expiry.
 *
 * <p>Two modes of operation are available depending on the constructor used:
 *
 * <h2>Permanent mode (classic lazy)</h2>
 * <p>Created with {@link #Lazy(Supplier)}. The value is computed <em>exactly
 * once</em> on the first call to {@link #get()} and cached forever. The
 * supplier is eligible for garbage collection after initialization.
 *
 * <pre>{@code
 * Lazy<DatabaseConnection> conn = new Lazy<>(() -> connectToDatabase());
 * conn.get().query("SELECT ...");   // initialized on first call, reused forever
 * }</pre>
 *
 * <h2>Ephemeral mode (expiring lazy)</h2>
 * <p>Created with {@link #Lazy(long, Supplier)} or
 * {@link #Lazy(long, TimeUnit, Supplier)}. The value is computed on the first
 * call to {@link #get()} and cached until the TTL elapses; after that the next
 * call recomputes it. The supplier is <em>kept alive</em> and called again on
 * every refresh.
 *
 * <pre>{@code
 * // Re-fetch the config file at most once every 5 minutes
 * Lazy<Config> cfg = new Lazy<>(5, TimeUnit.MINUTES, () -> Config.load());
 * cfg.get();   // loads and caches
 * cfg.get();   // returns cached value (if < 5 min have passed)
 * // … 5+ minutes later …
 * cfg.get();   // reloads and caches again
 * }</pre>
 *
 * <h2>Wrapping a known value</h2>
 * <pre>{@code
 * Lazy<String> greeting = Lazy.of("Hello");
 * }</pre>
 *
 * <h2>Checking state without triggering initialization</h2>
 * <pre>{@code
 * if (conn.isInitialized()) {
 *     conn.get().close();
 * }
 * }</pre>
 *
 * <h2>Error handling</h2>
 * <p>If the supplier throws, the exception propagates to the caller and the
 * {@code Lazy} remains uninitialized (or expired, in ephemeral mode), so a
 * future call will retry.
 *
 * @param <T> the type of the lazily computed value
 */
public final class Lazy<T> implements Supplier<T>
{
    /** Sentinel meaning "not yet computed / expired". */
    @SuppressWarnings("rawtypes")
    private static final Object UNSET = new Object();

    /** {@code Long.MIN_VALUE} means "never computed". */
    private static final long NEVER = Long.MIN_VALUE;

    /** {@code Long.MAX_VALUE} means "permanent — never expires". */
    private static final long PERMANENT = Long.MAX_VALUE;

    // ------------------------------------------------------------------ //
    // State
    // ------------------------------------------------------------------ //

    /**
     * Holds either {@link #UNSET} or the real value (including {@code null}).
     * AtomicReference provides volatile semantics on the fast path.
     */
    private final AtomicReference<Object> ref = new AtomicReference<>(UNSET);

    /**
     * The delegate used to (re-)compute the value.
     * Nulled out after permanent initialization to allow GC.
     */
    private volatile Supplier<T> supplier;

    /**
     * TTL in nanoseconds. {@link #PERMANENT} for the permanent mode.
     */
    private final long ttlNanos;

    /**
     * {@code System.nanoTime()} of the last successful computation,
     * or {@link #NEVER} if never computed.
     * Only meaningful in ephemeral mode.
     */
    private final AtomicLong lastComputedAt = new AtomicLong(NEVER);

    // ------------------------------------------------------------------ //
    // Constructors
    // ------------------------------------------------------------------ //

    /**
     * Creates a <em>permanent</em> {@code Lazy}: the value is computed exactly
     * once on the first call to {@link #get()} and cached forever.
     *
     * @param supplier the factory; must not be {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public Lazy(Supplier<T> supplier)
    {
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
        this.ttlNanos = PERMANENT;
    }

    /**
     * Creates an <em>ephemeral</em> {@code Lazy} with the TTL expressed in
     * milliseconds: the value is recomputed whenever more than {@code ttlMillis}
     * milliseconds have elapsed since the last computation.
     *
     * @param ttlMillis time-to-live in milliseconds; must be &gt; 0
     * @param supplier  the factory called on every refresh; must not be
     *                  {@code null}
     * @throws IllegalArgumentException if {@code ttlMillis} &le; 0
     * @throws NullPointerException     if {@code supplier} is {@code null}
     */
    public Lazy(long ttlMillis, Supplier<T> supplier)
    {
        this(ttlMillis, TimeUnit.MILLISECONDS, supplier);
    }

    /**
     * Creates an <em>ephemeral</em> {@code Lazy} with the TTL expressed in the
     * given {@link TimeUnit}: the value is recomputed whenever more than
     * {@code ttl} units have elapsed since the last computation.
     *
     * @param ttl      time-to-live value; must be &gt; 0
     * @param timeUnit unit for {@code ttl}; must not be {@code null}
     * @param supplier the factory called on every refresh; must not be
     *                 {@code null}
     * @throws IllegalArgumentException if {@code ttl} &le; 0
     * @throws NullPointerException     if {@code timeUnit} or {@code supplier}
     *                                  is {@code null}
     */
    public Lazy(long ttl, TimeUnit timeUnit, Supplier<T> supplier)
    {
        if (ttl <= 0)
        {
            throw new IllegalArgumentException("ttl must be > 0, got: " + ttl);
        }
        Objects.requireNonNull(timeUnit, "timeUnit must not be null");
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
        this.ttlNanos = timeUnit.toNanos(ttl);
    }

    // ------------------------------------------------------------------ //
    // Factory
    // ------------------------------------------------------------------ //

    /**
     * Creates an already-initialized <em>permanent</em> {@code Lazy} wrapping
     * {@code value}.
     *
     * @param value the pre-computed value (may be {@code null})
     * @param <T>   value type
     * @return an initialized {@code Lazy}
     */
    public static <T> Lazy<T> of(T value)
    {
        Lazy<T> lazy = new Lazy<>(() -> value);
        lazy.ref.set(value);
        lazy.supplier = null;          // allow GC immediately
        return lazy;
    }

    // ------------------------------------------------------------------ //
    // Core API
    // ------------------------------------------------------------------ //

    /**
     * Returns the cached value if still valid, or computes (and caches) a new
     * one by invoking the supplier.
     *
     * <ul>
     *   <li><em>Permanent mode</em>: the supplier is called at most once.</li>
     *   <li><em>Ephemeral mode</em>: the supplier is called again whenever the
     *       TTL has elapsed since the previous computation.</li>
     * </ul>
     *
     * <p>If the supplier throws, the exception propagates and the container
     * remains in its previous state (uninitialized or expired), so the next
     * call will retry.
     *
     * @return the (possibly freshly computed) value; may be {@code null}
     */
    @SuppressWarnings("unchecked")
    public T get()
    {
        if (isEphemeral())
        {
            // Ephemeral fast path: return cached value if not expired
            if (!isExpiredEphemeral())
            {
                return (T) ref.get();
            }
            return refreshEphemeral();
        }

        // Permanent fast path
        Object current = ref.get();
        if (current != UNSET)
        {
            return (T) current;
        }
        return initializePermanent();
    }

    /**
     * Returns {@code true} if the value has been computed and is currently
     * valid (not expired).
     *
     * <ul>
     *   <li><em>Permanent mode</em>: {@code true} once initialized, forever.</li>
     *   <li><em>Ephemeral mode</em>: {@code true} only while the cached value
     *       is within its TTL window.</li>
     * </ul>
     */
    public boolean isInitialized()
    {
        if (isEphemeral())
        {
            return lastComputedAt.get() != NEVER && !isExpiredEphemeral();
        }
        return ref.get() != UNSET;
    }

    /**
     * Returns the cached value without triggering initialization or refresh.
     *
     * <ul>
     *   <li><em>Permanent mode</em>: empty if never initialized.</li>
     *   <li><em>Ephemeral mode</em>: empty if never computed <em>or</em> if the
     *       cached value has expired.</li>
     * </ul>
     *
     * @return an {@link Optional} containing the cached value, or empty
     */
    @SuppressWarnings("unchecked")
    public Optional<T> getIfInitialized()
    {
        if (isEphemeral())
        {
            if (lastComputedAt.get() == NEVER || isExpiredEphemeral())
            {
                return Optional.empty();
            }
            return Optional.ofNullable((T) ref.get());
        }
        Object current = ref.get();
        return current == UNSET ? Optional.empty() : Optional.ofNullable((T) current);
    }

    /**
     * Returns {@code true} if this instance was created with a TTL (ephemeral
     * mode), {@code false} for permanent mode.
     */
    public boolean isEphemeral()
    {
        return ttlNanos != PERMANENT;
    }

    // ------------------------------------------------------------------ //
    // Internal — permanent mode
    // ------------------------------------------------------------------ //

    /**
     * Slow path for permanent mode: initializes the value exactly once under a
     * lock, then nulls out the supplier to allow GC.
     */
    @SuppressWarnings("unchecked")
    private T initializePermanent()
    {
        synchronized (this)
        {
            Object current = ref.get();
            if (current != UNSET)
            {
                return (T) current;
            }
            T value = supplier.get();   // may throw — ref stays UNSET
            ref.set(value);
            supplier = null;            // allow GC of the supplier & captures
            return value;
        }
    }

    // ------------------------------------------------------------------ //
    // Internal — ephemeral mode
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} if the cached value has never been computed or its
     * TTL has elapsed.
     */
    private boolean isExpiredEphemeral()
    {
        long last = lastComputedAt.get();
        return last == NEVER || (System.nanoTime() - last) > ttlNanos;
    }

    /**
     * Slow path for ephemeral mode: recomputes the value under a lock and
     * updates the cache and timestamp.
     */
    @SuppressWarnings("unchecked")
    private T refreshEphemeral()
    {
        synchronized (this)
        {
            // Re-check: another thread may have already refreshed
            if (!isExpiredEphemeral())
            {
                return (T) ref.get();
            }
            T value = supplier.get();   // may throw — state stays expired
            ref.set(value);
            lastComputedAt.set(System.nanoTime());
            return value;
        }
    }

    // ------------------------------------------------------------------ //
    // Object overrides
    // ------------------------------------------------------------------ //

    /**
     * Returns a human-readable description of the current state.
     *
     * <p>Examples: {@code "Lazy[uninitialized]"}, {@code "Lazy[expired]"},
     * {@code "Lazy[hello]"}.
     */
    @Override
    public String toString()
    {
        if (isEphemeral())
        {
            long last = lastComputedAt.get();
            if (last == NEVER)        return "Lazy[uninitialized]";
            if (isExpiredEphemeral()) return "Lazy[expired]";
            return "Lazy[" + ref.get() + "]";
        }
        Object current = ref.get();
        return current == UNSET ? "Lazy[uninitialized]" : "Lazy[" + current + "]";
    }
}
