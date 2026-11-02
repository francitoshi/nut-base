/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A duplex channel where write and read operations are disconnected.
 * Writes are forwarded to a {@link ChannelWriter}, and reads are retrieved from a {@link ChannelReader}.
 *
 * @param <E> the type of elements read from the input reader and written to the output writer
 */
public final class DuplexChannel<E> extends Channel<E> implements ChannelReader<E>, ChannelWriter<E>
{
    private final ChannelReader<E> in;
    private final ChannelWriter<E> out;

    /**
     * Forwards the observable state of the delegates to this channel, so a
     * {@code select} registered on this duplex wakes up when {@code in} or
     * {@code out} report activity.
     */
    private final ChannelObservable.OperationListener<E> inListener =
            (channel, operation, value) -> notifyOperation(operation, value);
    private final ChannelObservable.OperationListener<E> outListener =
            (channel, operation, value) -> notifyOperation(operation, value);

    public DuplexChannel(ChannelReader<E> in, ChannelWriter<E> out)
    {
        this.in = Objects.requireNonNull(in, "in must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
        if(in==out)
        {
            throw new IllegalArgumentException("in and out must be different objects");
        }
        in.addOperationListener(inListener);
        out.addOperationListener(outListener);
    }

    @Override
    public void put(E value)
    {
        out.put(value);
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        return out.put(value, timeout, unit);
    }

    @Override
    public E get()
    {
        return in.get();
    }

    @Override
    public E get(long timeout, TimeUnit unit)
    {
        return in.get(timeout, unit);
    }

    @Override
    public Optional<E> tryGet()
    {
        return in.tryGet();
    }

    @Override
    public boolean tryPut(E value)
    {
        return out.tryPut(value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the input is closeable ({@link ChannelCloser}) the iteration ends once
     * it is closed and drained; otherwise it behaves as a plain {@link Channel}
     * iteration, which never ends.
     */
    @Override
    public Iterator<E> iterator()
    {
        return in instanceof ChannelCloser ? terminatingIterator() : super.iterator();
    }

    @Override
    public boolean isDuplex()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A duplex channel never marks interruptions on itself: every blocking
     * operation is delegated to the {@code in}/{@code out} channels, so the
     * interrupted state is that of whichever delegate implements
     * {@link Channel} (non-{@code Channel} delegates contribute nothing).
     */
    @Override
    public boolean isInterrupted()
    {
        boolean interrupted = false;
        if (in instanceof Channel && ((Channel<?>) in).isInterrupted())
        {
            interrupted = true;
        }
        if (out instanceof Channel && ((Channel<?>) out).isInterrupted())
        {
            interrupted = true;
        }
        return interrupted;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The listener is installed on this channel <em>and</em> propagated to the
     * delegates that implement {@link Channel}, because that is where the
     * blocking operations actually wait and get interrupted. Non-{@link
     * Channel} delegates are left untouched.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setInterruptedExceptionListener(InterruptedExceptionListener listener)
    {
        super.setInterruptedExceptionListener(listener);
        if (in instanceof Channel)
        {
            ((Channel<E>) in).setInterruptedExceptionListener(listener);
        }
        if (out instanceof Channel)
        {
            ((Channel<E>) out).setInterruptedExceptionListener(listener);
        }
    }

    /**
     * Select support: a duplex channel can only announce activity through its
     * delegates, so select must re-scan in short slices when a delegate itself
     * can only be caught reliably by polling (rendezvous or non-{@link Channel}
     * delegates).
     */
    @Override
    boolean needsPolling()
    {
        return !(in instanceof Channel)
                || !(out instanceof Channel)
                || ((Channel<?>) in).needsPolling()
                || ((Channel<?>) out).needsPolling();
    }

    @Override
    boolean isReadClosed()
    {
        return in instanceof ChannelCloser && ((ChannelCloser) in).isClosed();
    }
}
