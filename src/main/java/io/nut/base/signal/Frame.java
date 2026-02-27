/*
 * Frame.java
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

import java.util.zip.CRC32;

/**
 * Frame — builds and parses binary frames over a byte stream.
 *
 * Frame layout:
 *
 *  Offset  Size  Field
 *  ──────  ────  ──────────────────────────────────────────────────────
 *    0      1    Frame Type         0x01 data 0x02 ACK 0x03 NACK   
 *    1      2    ID                 big-endian (id_high, id_low)
 *    3      2    Payload length     big-endian (len_high, len_low)
 *    5      N    Payload            N = length declared above
 *   5+N     4    CRC32              of ID + LEN + payload, little-endian
 *
 * CRC32 input:  [type][id_high][id_low][len_high][len_low][payload…]
 */
public class Frame
{
    // ── Protocol constants ────────────────────────────────────────────────────
    private static final int ID_OFFSET   = 1;
    private static final int LEN_OFFSET  = 3;

    /** Byte offset where payload begins: 1(type) + 2(id) + 2(len) */
    private static final int PAYLOAD_OFFSET = 5;
    private static final int HEADER_SIZE    = PAYLOAD_OFFSET;
    /** 4 bytes CRC32 + 1 byte closing delimiter */
    private static final int TRAILER_SIZE   = 4;
    /** Minimum valid frame length (empty payload) */
    private static final int MIN_FRAME_SIZE = HEADER_SIZE + TRAILER_SIZE;

    // ── Reserved frame IDs ────────────────────────────────────────────────────

    /** Data frame */
    public static final byte ID_DATA  = (short) 0x0000;

    /** Positive acknowledgement — frame received and CRC verified. */
    public static final byte ID_ACK  = (short) 0x0001;

    /** Negative acknowledgement — frame received but CRC failed. */
    public static final byte ID_NACK = (short) 0x0002;

    // ── Status codes carried inside ACK / NACK payloads ──────────────────────

    public static final byte STATUS_OK          = 0x00;
    public static final byte STATUS_CRC_ERROR   = 0x01;
    public static final byte STATUS_UNKNOWN_ID  = 0x02;
    public static final byte STATUS_BUFFER_FULL = 0x03;

    // ── Private API ────────────────────────────────────────────────────────────
    
    public static short readShort(byte[] frame, int offset)
    {
        // big-endian: high byte at offset 3, low byte at offset 4
        return (short) (((frame[offset] & 0xFF) << 8) | (frame[offset+1] & 0xFF));
    }
    
    public static void writeShort(byte[] frame, int offset, short value)
    {
        byte idHigh = (byte) ((value >> 8) & 0xFF);
        byte idLow  = (byte)  (value & 0xFF);
        frame[offset+0] = idHigh;
        frame[offset+1] = idLow;
    }
    
    private static byte[] create(byte type, short id, byte[] payload)
    {
        if (payload == null) 
        {
            payload = new byte[0];
        }

        int    len   = payload.length;
        byte[] frame = new byte[HEADER_SIZE + len + TRAILER_SIZE];
        int    pos   = 0;

        // frame type
        frame[pos++] = type;

        // ID — big-endian
        byte idHigh = (byte) ((id >> 8) & 0xFF);
        byte idLow  = (byte)  (id & 0xFF);
        frame[pos++] = idHigh;
        frame[pos++] = idLow;

        // Payload length — big-endian
        byte lenHigh = (byte) ((len >> 8) & 0xFF);
        byte lenLow  = (byte)  (len & 0xFF);
        frame[pos++] = lenHigh;
        frame[pos++] = lenLow;

        // Payload
        System.arraycopy(payload, 0, frame, pos, len);
        pos += len;

        // CRC32 over: id_high + id_low + len_high + len_low + payload
        long crc = computeCRC32(frame, 0, HEADER_SIZE + len);
        frame[pos++] = (byte)  (crc        & 0xFF);  // crc0 — LSB
        frame[pos++] = (byte) ((crc >>  8) & 0xFF);  // crc1
        frame[pos++] = (byte) ((crc >> 16) & 0xFF);  // crc2
        frame[pos++] = (byte) ((crc >> 24) & 0xFF);  // crc3 — MSB

        return frame;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Builds a complete frame.
     *
     * @param id      16-bit identifier
     * @param payload data bytes (null is treated as empty)
     * @return fully serialized frame ready for transmission
     */
    public static byte[] createData(short id, byte[] payload)
    {
        return create(ID_DATA, id, payload);
    }

    /**
     * Verifies the structural and checksum integrity of a frame.
     *
     * @param frame received bytes
     * @return  0   valid frame
     *         -1   null or too short
     *         -2   wrong type byte
     *         -3   declared length does not match actual frame size
     *         -4   CRC mismatch
     */
    public static int check(byte[] frame)
    {
        if (frame == null || frame.length < MIN_FRAME_SIZE)
        {
            return -1;
        }
        if (frame[0] != ID_DATA && frame[0] != ID_ACK && frame[0] != ID_NACK)
        {
            return -2;
        }
        int payloadLen = getPayloadLength(frame);
        if (payloadLen < 0 || HEADER_SIZE + payloadLen + TRAILER_SIZE != frame.length)
        {
            return -3;
        }
        long expected = computeCRC32(frame, 0, HEADER_SIZE + payloadLen);
        long actual = readCRC32(frame, payloadLen);
        return (expected == actual) ? 0 : -4;
    }

    public static byte getType(byte[] frame)
    {
        return frame[0];
    }

    /**
     * Extracts the ID from a frame.
     * Always call {@link #check(byte[])} first to ensure the frame is valid.
     *
     * @param frame validated frame bytes
     * @return 16-bit identifier
     */
    public static short getId(byte[] frame)
    {
        if (frame == null || frame.length < MIN_FRAME_SIZE)
        {
            return 0;
        }
        // big-endian: high byte at offset 3, low byte at offset 4
        return (short) (((frame[ID_OFFSET] & 0xFF) << 8) | (frame[ID_OFFSET + 1] & 0xFF));
    }
    
    private static int getPayloadLength(byte[] frame) 
    {
        return readShort(frame, LEN_OFFSET);
    }

    /**
     * Extracts the payload from a frame.
     * Always call {@link #check(byte[])} first to ensure the frame is valid.
     *
     * @param frame validated frame bytes
     * @return copy of the payload, or an empty array if the payload is empty
     */
    public static byte[] getPayload(byte[] frame)
    {
        if (frame == null || frame.length < MIN_FRAME_SIZE)
        {
            return new byte[0];
        }
        int len = getPayloadLength(frame);
        if (len <= 0)
        {
            return new byte[0];
        }
        byte[] result = new byte[len];
        System.arraycopy(frame, PAYLOAD_OFFSET, result, 0, len);
        return result;
    }

    
    // ── ACK / NACK helpers ────────────────────────────────────────────────────

    /**
     * Creates an ACK frame confirming successful receipt of {@code originalId}.
     *
     * Payload layout (3 bytes):
     *   [0] id_high of the confirmed frame
     *   [1] id_low  of the confirmed frame
     *   [2] status  = STATUS_OK (0x00)
     */
    public static byte[] createAck(short id)
    {
        return create(ID_ACK, id, new byte[0]);
    }

    /**
     * Creates a NACK frame reporting a failed receipt of {@code originalId}.
     *
     * @param id  ID of the frame that failed
     * @param statusCode  reason code (use STATUS_* constants)
     *
     * Payload layout (3 bytes):
     *   [0] id_high of the failed frame
     *   [1] id_low  of the failed frame
     *   [2] status  (e.g. STATUS_CRC_ERROR)
     * @return the NACK frame
     */
    public static byte[] createNack(short id, byte statusCode)
    {
        byte[] frame = create(ID_NACK, id, new byte[0]);
        
        byte lenHigh = (byte) ((statusCode >> 8) & 0xFF);
        byte lenLow  = (byte)  (statusCode & 0xFF);
        frame[LEN_OFFSET] = lenHigh;
        frame[LEN_OFFSET] = lenLow;
        
        return frame;
    }

    /** Returns true if the frame carries an ACK. */
    public static boolean isData(byte[] frame)
    {
        return getType(frame) == ID_DATA;
    }

    /** Returns true if the frame carries an ACK. */
    public static boolean isAck(byte[] frame)
    {
        return getType(frame) == ID_ACK;
    }

    /** Returns true if the frame carries a NACK. */
    public static boolean isNack(byte[] frame)
    {
        return getType(frame) == ID_NACK;
    }

    /**
     * Extracts the status code from an ACK or NACK frame.
     *
     * @param frame a validated ACK or NACK frame
     * @return status byte, or -1 if the payload is malformed
     */
    public static short getStatus(byte[] frame)
    {
        return readShort(frame, LEN_OFFSET);
    }

    private static long computeCRC32(byte[] frame, int start, int stop)
    {
        CRC32 crc32 = new CRC32();
        crc32.update(frame, start, stop);
        return crc32.getValue();
    }

    private static long readCRC32(byte[] frame, int payloadLen) 
    {
        int i = PAYLOAD_OFFSET + payloadLen;
        return  (frame[i]   & 0xFFL)
              | ((frame[i+1] & 0xFFL) <<  8)
              | ((frame[i+2] & 0xFFL) << 16)
              | ((frame[i+3] & 0xFFL) << 24);
    }

}