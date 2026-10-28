/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class RotatingFileOutputStream extends FilterOutputStream
{
    private final Object lock = new Object();
    
    private final String pattern;
    private final int limit;
    private final int count;
    private volatile OutputStream output;
    private volatile long size;

    public RotatingFileOutputStream(String pattern, int limit, int count, boolean append) throws FileNotFoundException 
    {
        super(null);
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        File file = getFile(0);
        this.output = new FileOutputStream(file, append);
        this.size = file.length();
    }
    private File getFile(int i)
    {
        return new File(String.format(Locale.ROOT, pattern, i));
    }
    private void rotate() throws IOException
    {
        synchronized(lock)
        {
            if(this.size>this.limit)
            {
                this.flush();
                this.close();

                File dstFile = getFile(count-1);
                if(dstFile.exists())
                {
                    dstFile.delete();
                }

                for(int i=count-2;i>=0; i--)
                {
                    File orgFile = getFile(i);
                    if(orgFile.exists())
                    {
                        orgFile.renameTo(dstFile);
                    }
                    dstFile=orgFile;
                }
                dstFile = getFile(0);
                this.output = new FileOutputStream(dstFile, false);
                this.size = 0L;
            }            
        }
    }

    @Override
    public void close() throws IOException
    {
        synchronized(lock)
        {
            this.output.close();
        }
    }

    @Override
    public void flush() throws IOException
    {
        synchronized(lock)
        {
            this.output.flush();
        }
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException
    {
        synchronized(lock)
        {
            this.output.write(bytes, off, len);
            this.size += len;
            this.rotate();
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException
    {
        synchronized(lock)
        {
            this.output.write(bytes);
            this.size += bytes.length;
            this.rotate();
        }
    }

    @Override
    public void write(int i) throws IOException
    {
        synchronized(lock)
        {
            this.output.write(i);
            this.size++;
            this.rotate();
        }
    }
    
}
