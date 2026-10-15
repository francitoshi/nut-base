/*
 *  MeteredOutputStream.java
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

