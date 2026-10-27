/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.io;

import java.io.FilterOutputStream;
import java.io.IOException;

/**
 *
 * @author franci
 */
public class NullOutputStream extends FilterOutputStream
{
    public NullOutputStream()
    {
        super(null);
    }

    @Override
    public void write(int i) throws IOException
    {
    }

    @Override
    public void write(byte[] bytes) throws IOException
    {
    }

    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException
    {
    }

    @Override
    public void flush() throws IOException
    {
    }

    @Override
    public void close() throws IOException
    {
    }
}
