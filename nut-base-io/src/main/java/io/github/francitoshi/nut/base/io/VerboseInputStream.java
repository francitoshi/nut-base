/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author franci
 */
public class VerboseInputStream extends FilterInputStream
{
    private final OutputStream verbose;
    
    public VerboseInputStream(InputStream in, OutputStream verbose)
    {
        super(in);
        this.verbose = verbose;
    }

    @Override
    public int read() throws IOException
    {
        int ret = super.read();
        if(ret>=0)
        {
            verbose.write(ret);
        }
        return ret;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        int ret = super.read(b);
        if(ret>0)
        {
            verbose.write(b,0,ret);
        }
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        int ret = super.read(b, off, len);
        if(ret>0)
        {
            verbose.write(b,off,ret);
        }
        return ret;
    }   
}
