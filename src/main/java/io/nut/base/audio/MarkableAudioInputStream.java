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
 * An {@link AudioInputStream} that supports {@link #mark(int)} and {@link #reset()} even when
 * the underlying stream does not natively support these operations.
 *
 * <p>This is achieved by buffering all bytes read after a {@code mark()} call into an internal
 * {@link ByteArrayOutputStream}. When {@code reset()} is called, subsequent reads are served from
 * that buffer before resuming from the original source stream.
 *
 * <p>Example usage:
 * <pre>{@code
 * AudioInputStream original = AudioSystem.getAudioInputStream(file);
 * MarkableAudioInputStream markable = new MarkableAudioInputStream(original);
 *
 * markable.mark(4096);
 * byte[] header = markable.readNBytes(44);
 * markable.reset();                   // replay those 44 bytes on the next read
 * }</pre>
 */
public class MarkableAudioInputStream extends AudioInputStream
{
    /** The wrapped source stream from which audio data is ultimately read. */
    private final AudioInputStream source;

    /**
     * Buffer that accumulates bytes read after the most recent {@link #mark(int)} call.
     * Set to {@code null} when no mark is active or when the read-limit has been exceeded.
     */
    private ByteArrayOutputStream markBuffer;

    /**
     * Read-only view of {@link #markBuffer} used to replay bytes after {@link #reset()}.
     * Set to {@code null} when not in replay mode.
     */
    private ByteArrayInputStream replayBuffer;

    /**
     * Maximum number of bytes that may be read before the current mark becomes invalid,
     * as specified in the most recent call to {@link #mark(int)}.
     */
    private int markReadLimit;

    /**
     * {@code true} while replaying buffered bytes following a {@link #reset()} call;
     * {@code false} otherwise.
     */
    private boolean isReplaying;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Wraps an existing {@link AudioInputStream} to add mark/reset support.
     *
     * <p>The audio format and frame length of this stream are taken directly from
     * {@code source}; no audio data is read or buffered at construction time.
     *
     * @param source the {@link AudioInputStream} to wrap; must not be {@code null}
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

    // -------------------------------------------------------------------------
    // Mark / Reset
    // -------------------------------------------------------------------------

    /**
     * Reports that this stream supports the {@link #mark(int)} and {@link #reset()} operations.
     *
     * @return {@code true}, unconditionally
     */
    @Override
    public boolean markSupported()
    {
        return true;
    }

    /**
     * Marks the current position in the stream so that a subsequent call to {@link #reset()}
     * will reposition the stream to this point.
     *
     * <p>Any previously set mark is discarded. Up to {@code readlimit} bytes may be read
     * before the mark becomes invalid. If more than {@code readlimit} bytes are read before
     * {@code reset()} is called, the mark is silently invalidated and a later {@code reset()}
     * will throw an {@link IOException}.
     *
     * @param readlimit the maximum number of bytes that may be read before the mark is invalidated
     */
    @Override
    public void mark(int readlimit)
    {
        markReadLimit = readlimit;
        markBuffer = new ByteArrayOutputStream(readlimit);
        replayBuffer = null;
        isReplaying = false;
    }

    /**
     * Repositions the stream to the position recorded by the most recent {@link #mark(int)} call.
     *
     * <p>After this method returns, the next read will re-deliver the bytes that were read
     * between the {@code mark()} call and this {@code reset()} call, followed by the remaining
     * data from the original source stream.
     *
     * @throws IOException if {@link #mark(int)} has not been called, or if the mark has been
     *                     invalidated because more than {@code readlimit} bytes were read since
     *                     the mark was set
     */
    @Override
    public void reset() throws IOException
    {
        if (markBuffer == null)
        {
            throw new IOException("mark() has not been called or the mark has been invalidated");
        }

        // Prepare the replay buffer with the data saved since mark()
        replayBuffer = new ByteArrayInputStream(markBuffer.toByteArray());
        isReplaying = true;
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Reads a single byte of audio data.
     *
     * <p>If a {@link #reset()} has been performed and replay bytes remain, this method reads
     * from the internal replay buffer. When the replay buffer is exhausted, subsequent reads
     * resume from the original source stream. If a mark is active, each byte read from the
     * source is copied into the mark buffer.
     *
     * @return the next byte of data as an unsigned value in the range {@code 0–255},
     *         or {@code -1} if the end of the stream has been reached
     * @throws IOException if an I/O error occurs while reading from the source stream
     */
    @Override
    public int read() throws IOException
    {
        // If replaying, serve from the replay buffer first
        if (isReplaying && replayBuffer != null)
        {
            int b = replayBuffer.read();
            if (b == -1)
            {
                // Replay exhausted; switch back to the live source stream
                isReplaying = false;
                replayBuffer = null;
            }
            return b;
        }

        // Read from the original source stream
        int b = source.read();

        // If a mark is active, copy the byte into the mark buffer
        if (markBuffer != null && b != -1)
        {
            markBuffer.write(b);

            // Invalidate the mark if the read-limit has been exceeded
            if (markBuffer.size() > markReadLimit)
            {
                markBuffer = null;
            }
        }

        return b;
    }

    /**
     * Reads up to {@code len} bytes of audio data into the specified portion of {@code b}.
     *
     * <p>Bytes are drawn first from the replay buffer (if active), and then from the original
     * source stream. Bytes read from the source while a mark is active are transparently copied
     * into the mark buffer. If more than {@code readlimit} bytes accumulate in the mark buffer
     * the mark is invalidated.
     *
     * @param b   the buffer into which data is read
     * @param off the start offset within {@code b} at which data is written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read, or {@code -1} if the end of the stream has been
     *         reached before any byte could be read
     * @throws NullPointerException      if {@code b} is {@code null}
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} are out of bounds for {@code b}
     * @throws IOException               if an I/O error occurs while reading from the source stream
     */
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

        // --- Replay mode: serve bytes from the replay buffer ---
        if (isReplaying && replayBuffer != null)
        {
            int bytesRead = replayBuffer.read(b, off, len);

            if (bytesRead < len)
            {
                // Replay buffer exhausted; read the remainder from the source stream
                isReplaying = false;
                int remaining = len - Math.max(0, bytesRead);
                int additionalBytes = source.read(b, off + Math.max(0, bytesRead), remaining);

                if (bytesRead <= 0 && additionalBytes <= 0)
                {
                    return -1;
                }

                // Copy newly read source bytes into the mark buffer if a mark is active
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

        // --- Normal mode: read directly from the source stream ---
        int bytesRead = source.read(b, off, len);

        // Copy read bytes into the mark buffer if a mark is active
        if (markBuffer != null && bytesRead > 0)
        {
            markBuffer.write(b, off, bytesRead);

            // Invalidate the mark if the read-limit has been exceeded
            if (markBuffer.size() > markReadLimit)
            {
                markBuffer = null;
            }
        }

        return bytesRead;
    }

    // -------------------------------------------------------------------------
    // Skip / Available / Close
    // -------------------------------------------------------------------------

    /**
     * Skips over and discards up to {@code n} bytes of audio data from this stream.
     *
     * <p>Rather than delegating to the source stream's native {@code skip()}, this implementation
     * reads and discards bytes in chunks so that the mark/reset mechanism remains consistent.
     * All skipped bytes are therefore still processed by {@link #read(byte[], int, int)}, meaning
     * they are buffered when a mark is active.
     *
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped, which may be less than {@code n} if the end
     *         of the stream is reached
     * @throws IOException if an I/O error occurs while reading
     */
    @Override
    public long skip(long n) throws IOException
    {
        // Use read-and-discard so that mark/reset state stays consistent
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

    /**
     * Returns an estimate of the number of bytes that can be read (or skipped) without blocking.
     *
     * <p>When in replay mode, this reflects the number of bytes remaining in the replay buffer.
     * Otherwise, it delegates to the source stream's {@link AudioInputStream#available()} method.
     *
     * @return an estimate of the number of available bytes
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException
    {
        if (isReplaying && replayBuffer != null)
        {
            return replayBuffer.available();
        }
        return source.available();
    }

    /**
     * Closes this stream and releases all associated resources, including the underlying
     * source {@link AudioInputStream} and any internal mark/replay buffers.
     *
     * <p>Once closed, any further read or skip operations will throw an {@link IOException}.
     *
     * @throws IOException if an I/O error occurs while closing the source stream
     */
    @Override
    public void close() throws IOException
    {
        source.close();
        markBuffer = null;
        replayBuffer = null;
    }
}
