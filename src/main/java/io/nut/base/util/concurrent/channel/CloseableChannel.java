/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

public abstract class CloseableChannel<E> extends Channel<E> implements ChannelReader<E>, ChannelWriter<E>, ChannelCloser
{
    /**
     * {@inheritDoc}
     */
    public abstract boolean close();
    /**
     * {@inheritDoc}
     */
    public abstract boolean isClosed();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void join() throws InterruptedException;
}