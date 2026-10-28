/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author franci
 */
public class VerboseOutputStream extends FilterOutputStream
{
    private final OutputStream verbose;
    
    public VerboseOutputStream(OutputStream out, OutputStream verbose)
    {
        super(out);
        this.verbose = verbose;
    }

    @Override
    public void write(int b) throws IOException
    {
        super.write(b);
        verbose.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        super.write(b);
        verbose.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        super.write(b, off, len);
        verbose.write(b, off, len);
    }
}
