/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio.speech;

import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AudioBuilder
{
    protected static class NullOutputStream extends FilterOutputStream
    {
        public NullOutputStream()
        {
            super(new ByteArrayOutputStream());
        }
        @Override
        public void close() throws IOException
        {
        }

        @Override
        public void flush() throws IOException
        {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
        }

        @Override
        public void write(byte[] b) throws IOException
        {
        }
        @Override
        public void write(int b) throws IOException
        {
        }
    }
    protected final PrintStream out;
    protected final PrintStream err;
    protected final File baseDir;
    protected final String name;
    protected final String ext;
    protected final boolean overwrite;
    protected final BlockingQueue<File> wavQueue;
    protected final boolean deleteWav;

    public AudioBuilder(boolean mute, File baseDir, String name, String ext, boolean overwrite, boolean deleteWav, int queueSize)
    {
        this.out = mute ? new PrintStream(new NullOutputStream()) : System.out;
        this.err = mute ? new PrintStream(new NullOutputStream()) : System.err;
        this.baseDir = baseDir;
        this.name = name;
        this.ext = ext;
        this.overwrite = overwrite;
        this.wavQueue = (queueSize>0 ? new ArrayBlockingQueue<File>(queueSize, true) : new LinkedBlockingQueue<File>());
        this.deleteWav= deleteWav;
    }
    public abstract void start();
    public abstract void stop();
    public abstract void join() throws InterruptedException;
    public abstract void join(long millis) throws InterruptedException;
    public abstract void join(long millis, int nanos) throws InterruptedException;

    public File getBaseDir()
    {
        return baseDir;
    }
    public BlockingQueue<File> getWavQueue()
    {
        return wavQueue;
    }

    public boolean isDeleteWav()
    {
        return deleteWav;
    }
}
