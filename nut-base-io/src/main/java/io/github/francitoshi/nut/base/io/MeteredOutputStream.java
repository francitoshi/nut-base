/*
 * Copyright (C) 2017-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

// A metered stream is a subclass of OutputStream that
//   (a) forwards all its output to a target stream
//   (b) keeps track of how many bytes have been written
public class MeteredOutputStream extends OutputStream
{
    private final OutputStream out;
    private final AtomicLong writtenCount;

    public MeteredOutputStream(OutputStream out, AtomicLong writtenCount)
    {
        this.out=out;
        this.writtenCount = writtenCount!=null? writtenCount : new AtomicLong(0L);
    }
    public MeteredOutputStream(OutputStream out, long writtenCount)
    {
        this(out, new AtomicLong(writtenCount));
    }

    public MeteredOutputStream(OutputStream out)
    {
        this(out, null);
    }

    @Override
    public void write(int b) throws IOException
    {
        this.out.write(b);
        this.writtenCount.incrementAndGet();
    }

    @Override
    public void write(byte[] buff) throws IOException
    {
        this.out.write(buff);
        this.writtenCount.addAndGet(buff.length);
    }

    @Override
    public void write(byte[] buff, int off, int len) throws IOException
    {
        this.out.write(buff,off,len);
        this.writtenCount.addAndGet(len);
    }

    public long getWrittenCount()
    {
        return this.writtenCount.get();
    }

    @Override
    public void close() throws IOException
    {
        this.out.close();
    }

    @Override
    public void flush() throws IOException
    {
        this.out.flush();
    }
    
}

