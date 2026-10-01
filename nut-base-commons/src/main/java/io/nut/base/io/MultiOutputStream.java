/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io;

import io.nut.base.lang.Require;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * An {@link OutputStream} that forwards every write to a fixed set of delegate
 * streams, so the same bytes end up in all of them.
 *
 * <p>The delegate array is copied on construction, so later changes to the caller
 * array do not affect this stream. A {@code null} delegate is rejected with
 * {@link NullPointerException} at construction time rather than failing later on
 * the first write.
 *
 * <p>{@link #flush()} and {@link #close()} are best effort: every delegate is
 * always invoked even if a previous one fails, the first failure being rethrown
 * with the remaining ones attached as {@linkplain Throwable#addSuppressed
 * suppressed exceptions}. {@link #close()} is idempotent and any operation
 * afterwards fails with {@link IOException}.
 *
 * <p>Writes are not atomic across delegates: if one delegate fails, the ones
 * already written to keep the bytes and the remaining ones do not, so the
 * delegates may stay out of sync. This stream is not thread safe.
 *
 * @author franci
 */
public class MultiOutputStream extends OutputStream
{
    private final OutputStream[] items;
    private boolean closed;

    public MultiOutputStream(OutputStream... items)
    {
        Objects.requireNonNull(items, "items must not be null");
        this.items = Arrays.copyOf(items, items.length);
        for (int i = 0; i < this.items.length; i++)
        {
            Objects.requireNonNull(this.items[i], "items[" + i + "] must not be null");
        }
    }

    @FunctionalInterface
    private interface IoAction
    {
        void accept(OutputStream item) throws IOException;
    }

    /**
     * Applies the action to every delegate, always attempting all of them.
     *
     * @param action the action to perform on each delegate
     * @throws IOException the first failure, with any later one attached as a
     *                     suppressed exception
     */
    private void forEach(IoAction action) throws IOException
    {
        IOException first = null;
        for (OutputStream item : items)
        {
            try
            {
                action.accept(item);
            }
            catch (IOException e)
            {
                if (first == null)
                {
                    first = e;
                }
                else
                {
                    first.addSuppressed(e);
                }
            }
        }
        if (first != null)
        {
            throw first;
        }
    }

    private void checkOpen() throws IOException
    {
        if (closed)
        {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public void write(int b) throws IOException
    {
        checkOpen();
        forEach(item -> item.write(b));
    }

    @Override
    public void close() throws IOException
    {
        if (closed)
        {
            return;
        }
        closed = true;
        forEach(OutputStream::close);
    }

    @Override
    public void flush() throws IOException
    {
        checkOpen();
        forEach(OutputStream::flush);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        Objects.requireNonNull(b, "b must not be null");
        Require.checkFromIndexSize(off, len, b.length);
        checkOpen();
        forEach(item -> item.write(b, off, len));
    }
}
