/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

import java.io.Closeable;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;

/**
 * Interface for components capable of writing audio data.
 */
public interface AudioWriter extends Closeable
{

    /**
     * Writes audio data.
     *
     * @param bytes  Byte array containing the audio data
     * @param off    The starting offset in the array
     * @param len    The size of the buffer
     * @param count  The number of bytes to write
     * @return       The number of bytes actually written
     * @throws IOException If an I/O error occurs
     */
    int write(byte[] bytes, int off, int len, int count) throws IOException;

    /**
     * Writes audio data.
     *
     * @param bytes  Byte array containing the audio data
     * @param off    The starting offset in the array
     * @param len    The size of the buffer and bytes to write
     * @return       The number of bytes actually written
     * @throws IOException If an I/O error occurs
     */
    int write(byte[] bytes, int off, int len) throws IOException;

    /**
     * Closes the writer and releases any associated resources.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    void close() throws IOException;

    /**
     * Returns the audio format used by this writer.
     *
     * @return The AudioFormat object
     */
    AudioFormat getFormat();

    /**
     * Drains the internal buffers, blocking until all data has been processed.
     */
    void drain();
    
}
