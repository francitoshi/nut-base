/*
 * Transceiver.java
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
package io.nut.base.signal;

/**
 * Abstraction over a point-to-point communication channel that transmits and
 * receives raw frame bytes.
 *
 * <p>Implementations are responsible for the physical or logical transport only
 * (e.g. serial port, socket, shared memory). Framing, sequencing, and
 * retransmission are handled by the layer above (see {@link DuplexLayer}).
 *
 * <p><b>Threading:</b> {@link #write(byte[])} and {@link #read()} will each be
 * called from a single dedicated thread by {@link DuplexLayer}, so
 * implementations are not required to support concurrent calls to the
 * <em>same</em> method. However, {@code write} and {@code read} may be invoked
 * concurrently with each other and must not interfere.
 */
public interface Transceiver
{
    /**
     * Transmits a frame over the channel.
     *
     * <p>The call must block until the frame has been handed off to the
     * underlying transport. It does not need to wait for acknowledgement from
     * the remote side; that responsibility belongs to {@link DuplexLayer}.
     *
     * @param frameData complete frame bytes to transmit, including any header;
     *                  must not be {@code null} or empty
     */
    void write(byte[] frameData);

    /**
     * Receives the next frame from the channel.
     *
     * <p>The call must block until a complete frame is available. The returned
     * array must contain exactly one frame, starting at index 0.
     *
     * @return the received frame bytes, never {@code null} or empty
     */
    byte[] read();
}