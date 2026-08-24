/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;

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
    }

    @Override
    public void put(E value) throws InterruptedException
    {
        out.put(value);
    }

    @Override
    public E get() throws InterruptedException
    {
        return in.get();
    }

    @Override
    public boolean isDuplex()
    {
        return true;
    }
}
