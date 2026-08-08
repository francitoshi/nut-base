/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * ScopeGuard: runs cleanup actions when leaving a scope, similar to
 * {@code defer} in Go or RAII in C++.
 *
 * <p>The core implementation is <b>not</b> thread-safe, since the common
 * case is a guard created and used within a single method / single thread
 * (e.g. inside a try-with-resources block). If you need to share a guard
 * across threads, wrap it with {@link #threadSafe()}.
 *
 * <p>Typical usage with try-with-resources:
 * <pre>{@code
 * try (ScopeGuard guard = ScopeGuard.create()) {
 *     Connection conn = openConnection();
 *     guard.onExit(conn::close);
 *
 *     Lock lock = acquireLock();
 *     guard.onExit(lock::unlock);
 *
 *     // ... business logic ...
 *
 * } // lock is released first, then the connection (reverse order, LIFO)
 * }</pre>
 *
 * <p>It also supports the "commit / release" pattern, just like
 * {@code std::unique_ptr::release()} in C++: if everything succeeds,
 * you can cancel the cleanup with {@link #dismiss()}.
 *
 * <pre>{@code
 * try (ScopeGuard guard = ScopeGuard.create()) {
 *     File tmp = createTempFile();
 *     guard.onExit(tmp::delete); // in case something fails before we finish
 *
 *     moveToFinalLocation(tmp);
 *     guard.dismiss(); // success: don't delete the file
 * }
 * }</pre>
 *
 * <p>Thread-safe usage, when a guard must be shared across threads:
 * <pre>{@code
 * ScopeGuard guard = ScopeGuard.create().threadSafe();
 * // onExit / dismiss / close can now be called concurrently
 * }</pre>
 */
public class ScopeGuard implements AutoCloseable
{

    /** An action that may throw a checked exception, unlike {@link Runnable}. */
    @FunctionalInterface
    public interface ThrowingRunnable
    {
        void run() throws Exception;
    }

    private final Deque<ThrowingRunnable> actions = new ArrayDeque<>();
    private final boolean suppressExceptions;
    private boolean dismissed = false;
    private boolean closed = false;

    ScopeGuard(boolean suppressExceptions)
    {
        this.suppressExceptions = suppressExceptions;
    }

    /** Creates a regular ScopeGuard: if an action fails, the error is rethrown on close. */
    public static ScopeGuard create()
    {
        return new ScopeGuard(false);
    }

    /** Creates a ScopeGuard that silently swallows exceptions thrown by cleanup actions. */
    public static ScopeGuard createSuppressing()
    {
        return new ScopeGuard(true);
    }

    /**
     * Wraps this guard in a thread-safe decorator: {@code onExit}, {@code dismiss},
     * and {@code close} become safe to call concurrently from multiple threads.
     *
     * <p>Prefer the plain, unsynchronized guard whenever it stays within a single
     * thread (the common case); reach for this only when the guard genuinely needs
     * to be shared across threads.
     *
     * @return a new ScopeGuard instance that delegates to this one under a lock
     */
    public ScopeGuard threadSafe()
    {
        return new SynchronizedScopeGuard(this);
    }

    /**
     * Registers an action to run when the guard is closed.
     * Actions run in LIFO order (last registered, first executed),
     * just like stacking multiple {@code defer} calls in Go.
     *
     * <p>If the guard has already been closed or dismissed, the action
     * is discarded and never runs.
     *
     * @return this, to allow method chaining
     */
    public ScopeGuard defer(ThrowingRunnable action)
    {
        Objects.requireNonNull(action, "action");
        if (!closed && !dismissed)
        {
            actions.push(action);
        }
        return this;
    }

    /**
     * Cancels all pending actions: none of them will run on close.
     * Useful for the "everything went fine, no cleanup needed" pattern
     * (equivalent to {@code release()} on a C++ smart pointer).
     */
    public void dismiss()
    {
        dismissed = true;
        actions.clear();
    }

    private static final class ResourceAction implements ThrowingRunnable
    {
        final AutoCloseable resource;
        final ThrowingRunnable action;

        ResourceAction(AutoCloseable resource, ThrowingRunnable action)
        {
            this.resource = resource;
            this.action = action;
        }

        @Override
        public void run() throws Exception
        {
            action.run();
        }
    }

    /**
     * Registers an AutoCloseable resource to be closed on scope exit.
     * Returns the same resource that was passed in.
     *
     * @param resource the resource to register
     * @param <T> the resource type
     * @return the registered resource
     */
    public <T extends AutoCloseable> T use(T resource)
    {
        Objects.requireNonNull(resource, "resource");
        if (!closed && !dismissed)
        {
            actions.push(new ResourceAction(resource, resource::close));
        }
        return resource;
    }

    /**
     * Removes the given resource from the guard, preventing it from being closed.
     *
     * @param resource the resource to release
     */
    public void release(AutoCloseable resource)
    {
        if (resource == null || closed || dismissed)
        {
            return;
        }
        actions.removeIf(action -> action instanceof ResourceAction && ((ResourceAction) action).resource == resource);
    }

    /**
     * Removes the given resource from the guard, preventing it from being closed.
     * Synonym for {@link #release(AutoCloseable)}.
     *
     * @param resource the resource to dismiss
     */
    public void dismiss(AutoCloseable resource)
    {
        release(resource);
    }

    /**
     * Runs the pending actions in reverse order of registration.
     * If any action throws and the guard is not in "suppressing" mode,
     * the first exception is rethrown (wrapped in a RuntimeException)
     * and subsequent ones are attached via {@link Throwable#addSuppressed}.
     *
     * <p>Calling this more than once has no effect after the first call.
     */
    @Override
    public void close()
    {
        if (closed || dismissed)
        {
            closed = true;
            actions.clear();
            return;
        }
        closed = true;

        RuntimeException firstError = null;

        while (!actions.isEmpty())
        {
            ThrowingRunnable action = actions.pop();
            try
            {
                action.run();
            } 
            catch (Exception e)
            {
                if (!suppressExceptions)
                {
                    if (firstError == null)
                    {
                        firstError = new RuntimeException("Error running ScopeGuard action", e);
                    }
                    else
                    {
                        firstError.addSuppressed(e);
                    }
                }
            }
        }

        if (firstError != null)
        {
            throw firstError;
        }
    }

    /**
     * Thread-safe decorator around a plain {@link ScopeGuard}. All public methods
     * delegate to the wrapped instance under a single intrinsic lock. Since each
     * public method of ScopeGuard is a self-contained atomic operation (there are
     * no check-then-act sequences spanning two calls), synchronizing each method
     * individually is sufficient for correctness.
     */
    private static final class SynchronizedScopeGuard extends ScopeGuard
    {

        private final ScopeGuard delegate;
        private final Object lock = new Object();

        SynchronizedScopeGuard(ScopeGuard delegate)
        {
            super(false); // unused: all behavior is delegated
            this.delegate = delegate;
        }

        @Override
        public ScopeGuard threadSafe()
        {
            return this; // already thread-safe, no need to wrap again
        }

        @Override
        public ScopeGuard defer(ThrowingRunnable action)
        {
            synchronized (lock)
            {
                delegate.defer(action);
            }
            return this;
        }

        @Override
        public void dismiss()
        {
            synchronized (lock)
            {
                delegate.dismiss();
            }
        }

        @Override
        public <T extends AutoCloseable> T use(T resource)
        {
            synchronized (lock)
            {
                return delegate.use(resource);
            }
        }

        @Override
        public void release(AutoCloseable resource)
        {
            synchronized (lock)
            {
                delegate.release(resource);
            }
        }

        @Override
        public void dismiss(AutoCloseable resource)
        {
            synchronized (lock)
            {
                delegate.dismiss(resource);
            }
        }

        @Override
        public void close()
        {
            synchronized (lock)
            {
                delegate.close();
            }
        }
    }
}
