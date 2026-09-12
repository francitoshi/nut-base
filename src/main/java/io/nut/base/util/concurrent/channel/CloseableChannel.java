/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Iterator;
import java.util.NoSuchElementException;

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

    /**
     * Returns an iterator over the values read from this closeable channel.
     * Each call creates an iterator specific to that occasion.
     *
     * <p>{@link Iterator#hasNext} is hooked to {@link #get()}: it blocks until
     * a value is available, keeps it ready to be returned by
     * {@link Iterator#next} and returns {@code true}. It returns {@code false}
     * only when {@code get()} yielded {@code null} while the channel is closed
     * ({@link #isClosed()}), terminating the iteration.
     *
     * <p>The value obtained in {@link #hasNext} is remembered and returned by
     * {@link Iterator#next} regardless of whether it is null or not.
     *
     * @return a new {@code Iterator} reading from this channel
     */
    @Override
    public Iterator<E> iterator()
    {
        return new Iterator<E>()
        {
            private E saved;
            private boolean hasSaved;

            @Override
            public boolean hasNext()
            {
                if (hasSaved)
                {
                    return true;
                }
                E value = get();
                if (value == null && isClosed())
                {
                    return false;
                }
                saved = value;
                hasSaved = true;
                return true;
            }

            @Override
            public E next()
            {
                if (!hasSaved)
                {
                    throw new NoSuchElementException();
                }
                hasSaved = false;
                return saved;
            }
        };
    }
}