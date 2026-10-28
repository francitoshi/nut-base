/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

import java.io.Closeable;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author franci
 */
class LineOutWriter extends AbstractAudioWriter implements AudioWriter, Closeable
{
    
    final SourceDataLine line;
    private boolean closed;

    public LineOutWriter(SourceDataLine line)
    {
        this.line = line;
    }

    @Override
    public AudioFormat getFormat()
    {
        return line.getFormat();
    }

    @Override
    public int write(byte[] bytes, int off, int len) throws IOException
    {
        return line.write(bytes, off, len);
    }

    @Override
    public void drain()
    {
        line.drain();
    }

    @Override
    public void close()
    {
        if (!closed)
        {
            line.close();
        }
    }
    
}
