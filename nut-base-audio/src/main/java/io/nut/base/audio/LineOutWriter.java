/*
 * LineOutWriter.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.audio;

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
