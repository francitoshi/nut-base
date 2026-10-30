/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.Objects;
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

    public DuplexChannel(ChannelReader<E> in, ChannelWriter<E> out)
    {
        this.in = Objects.requireNonNull(in, "in must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
        if(in==out)
        {
            throw new IllegalArgumentException("in and out must be different objects");
        }
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
     * Select support: a duplex channel is read from {@code in}, so it is
     * {@code in} that has to fire the signals. If {@code in} is not a
     * {@link Channel} the select falls back to polling.
     */
    @Override
    boolean register(SelectSignal signal)
    {
        return in instanceof Channel && ((Channel<?>) in).register(signal);
    }

    @Override
    void unregister(SelectSignal signal)
    {
        if (in instanceof Channel)
        {
            ((Channel<?>) in).unregister(signal);
        }
    }

    @Override
    boolean needsPolling()
    {
        return !(in instanceof Channel) || ((Channel<?>) in).needsPolling();
    }

    @Override
    boolean isReadClosed()
    {
        return in instanceof ChannelCloser && ((ChannelCloser) in).isClosed();
    }
}
