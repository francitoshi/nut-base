/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread creation utilities with optional virtual-thread support on Java 21+
 * JVMs, while remaining compileable on Java 8.
 * <p>
 * The virtual-thread API ({@code Thread.ofVirtual()}) is reached reflectively,
 * once, and only when a probe proves that the running JVM actually supports
 * it; callers query {@link #isVirtualAvailable()} or simply pass
 * {@code allowVirtual} and get a plain platform thread wherever the JVM has no
 * Loom. Virtual threads are always daemon threads, so the daemon flag is not
 * applicable to them.
 */
public final class Threads
{
    private Threads()
    {
    }

    /** Counter for the generated names of virtual threads. */
    private static final AtomicLong VIRTUAL_COUNTER = new AtomicLong();

    /** Counter for the generated names of platform threads. */
    private static final AtomicLong PLATFORM_COUNTER = new AtomicLong();

    /**
     * Returns whether the running JVM supports virtual threads. The answer is
     * computed once (on first use, by the class holder) by probing the API and
     * actually starting one virtual thread, so a JVM that merely carries the
     * API without supporting it (e.g. a preview build) reports
     * {@code false}.
     *
     * @return {@code true} if virtual threads can be created on this JVM
     */
    public static boolean isVirtualAvailable()
    {
        return Holder.AVAILABLE;
    }

    /**
     * Starts a new thread running {@code task} and returns it.
     * <p>
     * When {@code allowVirtual} is {@code true} and the JVM supports virtual
     * threads ({@link #isVirtualAvailable()}), the thread is virtual. Virtual
     * threads are always daemon threads, so {@code daemon} is ignored there;
     * otherwise a platform thread is created with the requested daemon flag.
     * When virtual threads are unavailable, {@code allowVirtual} degrades to a
     * platform thread.
     *
     * @param task the runnable to execute; must not be {@code null}
     * @param daemon whether a platform thread is created as a daemon thread;
     *               ignored when the created thread is virtual
     * @param allowVirtual {@code true} to prefer a virtual thread when the JVM
     *                     has Loom
     * @return the started thread
     * @throws NullPointerException if {@code task} is {@code null}
     */
    public static Thread startThread(Runnable task, boolean daemon, boolean allowVirtual)
    {
        Objects.requireNonNull(task, "task must not be null");
        if (allowVirtual && Holder.AVAILABLE)
        {
            Thread thread = newVirtualThread(nextVirtualName(), task);
            thread.start();
            return thread;
        }
        Thread thread = new Thread(task, "threads-" + PLATFORM_COUNTER.incrementAndGet());
        thread.setDaemon(daemon);
        thread.start();
        return thread;
    }

    /**
     * Returns a {@link ThreadFactory} that creates virtual threads when
     * {@code allowVirtual} is {@code true} and the JVM supports them
     * ({@link #isVirtualAvailable()}); otherwise a plain platform factory
     * equivalent to {@code Executors.defaultThreadFactory()} (non-daemon
     * threads). Threads created by the virtual factory are always daemon.
     *
     * @param allowVirtual {@code true} to prefer a virtual-thread factory when
     *                     the JVM has Loom
     * @return a thread factory; never {@code null}
     */
    public static ThreadFactory getThreadFactory(boolean allowVirtual)
    {
        if (allowVirtual && Holder.AVAILABLE)
        {
            return task -> newVirtualThread(nextVirtualName(), task);
        }
        ThreadFactory platform = Executors.defaultThreadFactory();
        return task -> platform.newThread(Objects.requireNonNull(task, "task must not be null"));
    }

    private static String nextVirtualName()
    {
        return "virtual-" + VIRTUAL_COUNTER.incrementAndGet();
    }

    private static Thread newVirtualThread(String name, Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        try
        {
            Object builder = Holder.OF_VIRTUAL.invoke(null);
            Object named = Holder.NAME.invoke(builder, name);
            return (Thread) Holder.UNSTARTED.invoke(named, task);
        }
        catch (ReflectiveOperationException ex)
        {
            throw new IllegalStateException("virtual thread creation failed although availability was detected", ex);
        }
    }

    /**
     * Initialization-on-demand holder: the JVM guarantees lazy, thread-safe
     * initialization of this class on first access to {@link #AVAILABLE}, and
     * the reflective method lookups (and the probe) run exactly once. The
     * methods are cached so that creating threads later costs plain reflection
     * calls, no repeated lookups.
     */
    private static final class Holder
    {
        static final boolean AVAILABLE;
        static final Method OF_VIRTUAL;
        static final Method NAME;
        static final Method UNSTARTED;

        static
        {
            boolean available = false;
            Method ofVirtual = null;
            Method name = null;
            Method unstarted = null;
            try
            {
                Class<?> builderClass = Class.forName("java.lang.Thread$Builder");
                ofVirtual = Thread.class.getMethod("ofVirtual");
                name = builderClass.getMethod("name", String.class);
                unstarted = builderClass.getMethod("unstarted", Runnable.class);
                // Probe: actually create and start one, so a JVM that carries
                // the API without fully supporting it reports as unavailable.
                Thread probe = newVirtual(ofVirtual, name, unstarted, "threads-probe", () ->
                {
                });
                probe.start();
                probe.join(1000L);
                available = true;
            }
            catch (Throwable t)
            {
                available = false;
            }
            AVAILABLE = available;
            OF_VIRTUAL = ofVirtual;
            NAME = name;
            UNSTARTED = unstarted;
        }

        private static Thread newVirtual(Method ofVirtual, Method name, Method unstarted, String threadName, Runnable task) throws ReflectiveOperationException
        {
            Object builder = ofVirtual.invoke(null);
            Object named = name.invoke(builder, threadName);
            return (Thread) unstarted.invoke(named, task);
        }
    }
}
