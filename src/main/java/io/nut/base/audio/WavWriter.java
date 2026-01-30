/*
 * WavWriter.java
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

import java.io.*;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * AudioWriter implementation that writes standard-compliant WAV files. It
 * writes the WAV header first and then appends audio data transparently,
 * behaving similarly to a SourceDataLine.
 */
public class WavWriter implements AudioWriter, Closeable
{

    private final AudioFormat format;
    private final RandomAccessFile randomAccessFile;
    private volatile int dataSize;
    private volatile boolean closed;
    private volatile boolean headerWritten;

    /**
     * Constructor that writes to a File.
     *
     * @param file Destination file
     * @param format Audio format
     * @throws IOException If an error occurs creating the file
     */
    public WavWriter(File file, AudioFormat format) throws IOException
    {
        if (file == null)
        {
            throw new NullPointerException("file cannot be null");
        }
        if (format == null)
        {
            throw new NullPointerException("format cannot be null");
        }

        validateFormat(format);

        this.format = format;
        this.randomAccessFile = new RandomAccessFile(file, "rw");
        this.dataSize = 0;
        this.closed = false;
        this.headerWritten = false;

        writeWavHeader();
    }

    /**
     * Constructor with specific format parameters.
     */
    public WavWriter(File file, float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) throws IOException
    {
        this(file, new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian));
    }

    /**
     * Validates that the format is compatible with the WAV standard.
     */
    private void validateFormat(AudioFormat format) throws IOException
    {
        AudioFormat.Encoding encoding = format.getEncoding();
        if (encoding != AudioFormat.Encoding.PCM_SIGNED && encoding != AudioFormat.Encoding.PCM_UNSIGNED)
        {
            throw new IOException("Only PCM encoding is supported for WAV files");
        }

        // WAV PCM data MUST be Little Endian (except for some rare variants)
        if (format.isBigEndian() && format.getSampleSizeInBits() > 8)
        {
            throw new IOException("WAV files typically require Little Endian encoding");
        }

        int ssib = format.getSampleSizeInBits();
        if (ssib != 8 && ssib != 16 && ssib != 24 && ssib != 32)
        {
            throw new IOException("Unsupported sample size: " + ssib + " bits. Must be 8, 16, 24, or 32.");
        }
        int ch = format.getChannels();
        if (ch < 1 || ch > 65535)
        {
            throw new IOException("Invalid number of channels: " + ch);
        }
    }

    private void writeWavHeader() throws IOException
    {
        if (headerWritten)
        {
            return;
        }
        // Write header with data size 0 (will be updated on close)
        byte[] header = createWavHeader(0);
        randomAccessFile.write(header);
        headerWritten = true;
    }

    /**
     * Creates a complete WAV header.
     *
     * @param dataSize Byte size of the audio data
     * @return Byte array containing the WAV header
     */
    private byte[] createWavHeader(int dataSize) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(44);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int channels = format.getChannels();
        int sampleRate = (int) format.getSampleRate();
        int bitsPerSample = format.getSampleSizeInBits();
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        // RIFF chunk descriptor
        buffer.put("RIFF".getBytes());
        buffer.putInt((int) (36 + dataSize)); // ChunkSize
        buffer.put("WAVE".getBytes());

        // fmt sub-chunk
        buffer.put("fmt ".getBytes());
        buffer.putInt(16); // Subchunk1Size (16 for PCM)
        buffer.putShort((short) 1); // AudioFormat (1 = PCM)
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);

        // data sub-chunk
        buffer.put("data".getBytes());
        buffer.putInt((int) dataSize); // Subchunk2Size

        return buffer.array();
    }

    public int write(byte[] bytes, int off, int len) throws IOException
    {
        if (closed)
        {
            throw new IOException("WavWriter is closed");
        }
        if (bytes == null)
        {
            throw new NullPointerException("bytes cannot be null");
        }

        randomAccessFile.write(bytes, off, len);
        dataSize += len;
        return len;
    }

    private void updateWavHeader() throws IOException
    {
        long currentPos = randomAccessFile.getFilePointer();
        randomAccessFile.seek(0);
        byte[] header = createWavHeader(dataSize);
        randomAccessFile.write(header);
        randomAccessFile.seek(currentPos);
    }

    /**
     * Gets the audio format used
     *
     * @return AudioFormat
     */
    public AudioFormat getFormat()
    {
        return format;
    }

    /**
     * Gets the current size of the audio data wrote
     *
     * @return size in bytes
     */
    public int getDataSize()
    {
        return dataSize;
    }

    /**
     * verify if it is closed
     *
     * @return true if it closed
     */
    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public void close() throws IOException
    {
        if (!closed)
        {
            try
            {
                closed = true;
                updateWavHeader();
            }
            finally
            {
                randomAccessFile.close();
            }
        }

    }

    @Override
    public void drain()
    {
    }
}
