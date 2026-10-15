/*
 * AudioBuilder.java
 *
 * Copyright (c) 2015-2026 francitoshi@gmail.com
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

package io.nut.base.audio.speech;

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
