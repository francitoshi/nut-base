/*
 *  MeteredInputStream.java
 *
 *  Copyright (c) 2017-2024 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

// A metered stream is a subclass of OutputStream that
//   (a) forwards all its output to a target stream
//   (b) keeps track of how many bytes have been written
public class MeteredInputStream extends InputStream
{
    private final InputStream in; 
    private final AtomicLong readCount;
    private final AtomicLong skipCount;

    public MeteredInputStream(InputStream in, AtomicLong read, AtomicLong skip)
    {
        this.in = in;
        this.readCount = read!=null? read : new AtomicLong(0L);
        this.skipCount = skip!=null? skip : new AtomicLong(0L);
    }
    public MeteredInputStream(InputStream in, long read, long skip)
    {
        this(in, new AtomicLong(read), new AtomicLong(skip));
    }

    public MeteredInputStream(InputStream in, int read)
    {
        this(in, new AtomicLong(read), null);
    }

    public MeteredInputStream(InputStream in)
    {
        this(in, null, null);
    }

    @Override
    public int read() throws IOException
    {
        int b = this.in.read();
        this.readCount.incrementAndGet();
        return b;
    }

    @Override
    public int read(byte[] bytes) throws IOException
    {
        int count = this.in.read(bytes);
        this.readCount.addAndGet(count);
        return count;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException
    {
        int count = this.in.read(bytes, off, len);
        this.readCount.addAndGet(count);
        return count;
    }

    @Override
    public long skip(long len) throws IOException
    {
        long count = this.in.skip(len);
        this.skipCount.addAndGet(count);
        return count;
    }

    @Override
    public boolean markSupported()
    {
        return this.in.markSupported();
    }

    @Override
    public synchronized void reset() throws IOException
    {
        this.in.reset();
    }

    @Override
    public synchronized void mark(int readlimit)
    {
        this.in.mark(readlimit);
    }

    @Override
    public void close() throws IOException
    {
        this.in.close();
    }

    @Override
    public int available() throws IOException
    {
        return this.in.available();
    }

    public long getReadCount()
    {
        return this.readCount.get();
    }

    public long getSkipCount()
    {
        return this.skipCount.get();
    }

}

