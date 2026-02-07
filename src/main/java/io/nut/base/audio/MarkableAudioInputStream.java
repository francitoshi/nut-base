/*
 * MarkableAudioInputStream.java
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

import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * AudioInputStream que soporta mark() y reset() incluso cuando el stream
 * subyacente no lo soporta, almacenando los datos marcados en un buffer.
 */
public class MarkableAudioInputStream extends AudioInputStream
{

    private final AudioInputStream source;
    private ByteArrayOutputStream markBuffer;
    private ByteArrayInputStream replayBuffer;
    private int markReadLimit;
    private boolean isReplaying;

    /**
     * Crea un MarkableAudioInputStream a partir de otro AudioInputStream.
     *
     * @param source el AudioInputStream original
     */
    public MarkableAudioInputStream(AudioInputStream source)
    {
        super(new ByteArrayInputStream(new byte[0]), source.getFormat(), source.getFrameLength());
        this.source = source;
        this.markBuffer = null;
        this.replayBuffer = null;
        this.markReadLimit = 0;
        this.isReplaying = false;
    }

    /**
     * Obtiene un AudioInputStream que soporta mark/reset. Si el stream ya lo
     * soporta, lo devuelve tal cual. Si no, lo envuelve en un
     * MarkableAudioInputStream.
     *
     * @param src el AudioInputStream original
     * @return un AudioInputStream que soporta mark/reset
     */
    public static AudioInputStream getMarkable(AudioInputStream src)
    {
        if (src.markSupported())
        {
            return src;
        }
        return new MarkableAudioInputStream(src);
    }

    @Override
    public boolean markSupported()
    {
        return true;
    }

    @Override
    public void mark(int readlimit)
    {
        markReadLimit = readlimit;
        markBuffer = new ByteArrayOutputStream(readlimit);
        replayBuffer = null;
        isReplaying = false;
    }

    @Override
    public void reset() throws IOException
    {
        if (markBuffer == null)
        {
            throw new IOException("No se ha llamado a mark() o el mark ha sido invalidado");
        }

        // Preparar el buffer de replay con los datos guardados
        replayBuffer = new ByteArrayInputStream(markBuffer.toByteArray());
        isReplaying = true;
    }

    @Override
    public int read() throws IOException
    {
        // Si estamos en modo replay, leer del buffer de replay
        if (isReplaying && replayBuffer != null)
        {
            int b = replayBuffer.read();
            if (b == -1)
            {
                // Terminó el replay, volver al stream normal
                isReplaying = false;
                replayBuffer = null;
            }
            return b;
        }

        // Leer del stream original
        int b = source.read();

        // Si hay un mark activo, guardar en el buffer
        if (markBuffer != null && b != -1)
        {
            markBuffer.write(b);

            // Verificar si excedimos el límite de mark
            if (markBuffer.size() > markReadLimit)
            {
                markBuffer = null;
            }
        }

        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (b == null)
        {
            throw new NullPointerException();
        }
        else if (off < 0 || len < 0 || len > b.length - off)
        {
            throw new IndexOutOfBoundsException();
        }
        else if (len == 0)
        {
            return 0;
        }

        // Si estamos en modo replay, leer del buffer de replay
        if (isReplaying && replayBuffer != null)
        {
            int bytesRead = replayBuffer.read(b, off, len);

            if (bytesRead < len)
            {
                // Se acabó el buffer de replay
                isReplaying = false;
                int remaining = len - Math.max(0, bytesRead);
                int additionalBytes = source.read(b, off + Math.max(0, bytesRead), remaining);

                if (bytesRead <= 0 && additionalBytes <= 0)
                {
                    return -1;
                }

                // Guardar los nuevos bytes en el mark buffer si está activo
                if (markBuffer != null && additionalBytes > 0)
                {
                    markBuffer.write(b, off + Math.max(0, bytesRead), additionalBytes);

                    if (markBuffer.size() > markReadLimit)
                    {
                        markBuffer = null;
                    }
                }

                replayBuffer = null;
                return Math.max(0, bytesRead) + Math.max(0, additionalBytes);
            }

            return bytesRead;
        }

        // Leer del stream original
        int bytesRead = source.read(b, off, len);

        // Si hay un mark activo, guardar en el buffer
        if (markBuffer != null && bytesRead > 0)
        {
            markBuffer.write(b, off, bytesRead);

            // Verificar si excedimos el límite de mark
            if (markBuffer.size() > markReadLimit)
            {
                markBuffer = null;
            }
        }

        return bytesRead;
    }

    @Override
    public long skip(long n) throws IOException
    {
        // Implementar skip leyendo y descartando bytes para mantener consistencia con mark/reset
        byte[] skipBuffer = new byte[Math.min(8192, (int) n)];
        long totalSkipped = 0;

        while (totalSkipped < n)
        {
            int toRead = (int) Math.min(skipBuffer.length, n - totalSkipped);
            int read = read(skipBuffer, 0, toRead);

            if (read == -1)
            {
                break;
            }

            totalSkipped += read;
        }

        return totalSkipped;
    }

    @Override
    public int available() throws IOException
    {
        if (isReplaying && replayBuffer != null)
        {
            return replayBuffer.available();
        }
        return source.available();
    }

    @Override
    public void close() throws IOException
    {
        source.close();
        markBuffer = null;
        replayBuffer = null;
    }
}
