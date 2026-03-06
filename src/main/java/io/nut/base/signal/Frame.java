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

import io.nut.base.util.Utils;

/**
 * Frame — builds and parses binary frames over a byte stream.
 *
 * Frame layout:
 *
 *  Offset  Size  Field
 *  ──────  ────  ──────────────────────────────────────────────────────
 *    0      1    version           1
 *    1      1    flags             bits 1=HELLO 2=BCAST 3=ACK 4=NACK
 *    2      2    src               Source ID
 *    4      2    dst               Destiny ID
 *    6      2    id                big-endian (id_high, id_low)
 *    8      2    payload-length    big-endian (len_high, len_low)
 *   10      N    payload           N = length declared above
 *  10+N     2    CRC16             of ID + LEN + payload, big-endian
 *
 * CRC32 input:  [version][flags][src][dst][id][payload-length][payload…]
 */
public class Frame
{
    // ── PROTOCOL CONSTANTS ───────────────────────────────────────────────────
    private static final int VERSION_OFFSET = 0;
    private static final int FLAGS_OFFSET   = 1;
    private static final int SRC_OFFSET     = 2;
    private static final int DST_OFFSET     = 4;
    private static final int ID_OFFSET      = 6;
    private static final int LEN_OFFSET     = 8;
    private static final int PAYLOAD_OFFSET = 10;

    private static final int HEADER_SIZE    = PAYLOAD_OFFSET;
    private static final int TRAILER_SIZE   = 2;
    private static final int MIN_FRAME_SIZE = HEADER_SIZE + TRAILER_SIZE;

    // ── FLAGS CONSTANTS ──────────────────────────────────────────────────────
    
    public static final byte DATA_FLAG  = 0x00;
    public static final byte HELLO_FLAG = 0x01;
    public static final byte BCAST_FLAG = 0x02;
    public static final byte ACK_FLAG   = 0x04;
    public static final byte NACK_FLAG  = 0x08;

    // ── Status codes carried inside ACK / NACK payloads ──────────────────────

    public static final byte STATUS_OK          = 0x00;
    public static final byte STATUS_CRC_ERROR   = 0x01;
    public static final byte STATUS_UNKNOWN_ID  = 0x02;
    public static final byte STATUS_BUFFER_FULL = 0x03;

    // ── USEFULL CONSTANTS ────────────────────────────────────────────────────

    private static final byte[] EMPTY_BYTES = new byte[0];
    
    // ── Private API ──────────────────────────────────────────────────────────
    
    public final byte version = 1;
    
    public char readChar(byte[] frame, int offset)
    {
        // big-endian: high byte at offset 3, low byte at offset 4
        return (char) (((frame[offset] & 0xFF) << 8) | (frame[offset+1] & 0xFF));
    }
    
    public void writeChar(byte[] frame, int offset, char value)
    {
        frame[offset+0] = (byte) ((value >> 8) & 0xFF);
        frame[offset+1] = (byte)  (value       & 0xFF);
    }
    
    private byte[] create(byte flags, char src, char dst, char id, byte[] payload)
    {
        if (payload == null) 
        {
            payload = EMPTY_BYTES;
        }

        int    len   = payload.length;
        byte[] frame = new byte[HEADER_SIZE + len + TRAILER_SIZE];
        int    pos   = 0;
        
        frame[pos++] = this.version;
        frame[pos++] = flags;
        
        frame[pos++] = (byte) ((src >>  8) & 0xFF);  // src1 - MSB
        frame[pos++] = (byte)  (src        & 0xFF);  // src0 - LSB

        frame[pos++] = (byte) ((dst >>  8) & 0xFF);  // dst1 - MSB
        frame[pos++] = (byte)  (dst        & 0xFF);  // dst0 - LSB

        frame[pos++] = (byte) ((id >>  8)  & 0xFF);  // id1 - MSB
        frame[pos++] = (byte)  (id         & 0xFF);  // id0 - LSB

        frame[pos++] = (byte) ((len >>  8) & 0xFF);  // len1
        frame[pos++] = (byte)  (len        & 0xFF);  // len0 — LSB

        // Payload
        System.arraycopy(payload, 0, frame, pos, len);
        pos += len;

        // CRC32 over: id_high + id_low + len_high + len_low + payload
        int crc16 = Utils.crc16(frame, 0, HEADER_SIZE + len);
        frame[pos++] = (byte) ((crc16 >>  8) & 0xFF);  // crc1
        frame[pos++] = (byte)  (crc16        & 0xFF);  // crc0 — LSB

        return frame;
    }

    // ── Public API ────────────────────────────────────────────────────────────

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
    public int check(byte[] frame)
    {
        if (frame == null || frame.length < MIN_FRAME_SIZE)
        {
            return -1;
        }
        //VERIFY FLAGS
        //???
        
        int payloadLen = getPayloadLength(frame);
        if (payloadLen < 0 || HEADER_SIZE + payloadLen + TRAILER_SIZE != frame.length)
        {
            return -3;
        }
        int expected = Utils.crc16(frame, 0, HEADER_SIZE + payloadLen);
        int actual = readChar(frame, payloadLen);
        return (expected == actual) ? 0 : -4;
    }

    public byte getVersion(byte[] frame)
    {
        return frame[VERSION_OFFSET];
    }
    public byte getFlags(byte[] frame)
    {
        return frame[FLAGS_OFFSET];
    }
    public char getSrc(byte[] frame)
    {
        return readChar(frame, SRC_OFFSET);
    }
    public char getDst(byte[] frame)
    {
        return readChar(frame, DST_OFFSET);
    }

    /**
     * Extracts the ID from a frame.
     * Always call {@link #check(byte[])} first to ensure the frame is valid.
     *
     * @param frame validated frame bytes
     * @return 16-bit identifier
     */
    public char getId(byte[] frame)
    {
        if (frame == null || frame.length < MIN_FRAME_SIZE)
        {
            return 0;
        }
        return (char) (((frame[ID_OFFSET] & 0xFF) << 8) | (frame[ID_OFFSET + 1] & 0xFF));
    }
    
    private char getPayloadLength(byte[] frame) 
    {
        return readChar(frame, LEN_OFFSET);
    }

    /**
     * Extracts the payload from a frame.
     * Always call {@link #check(byte[])} first to ensure the frame is valid.
     *
     * @param frame validated frame bytes
     * @return copy of the payload, or an empty array if the payload is empty
     */
    public byte[] getPayload(byte[] frame)
    {
        if (frame == null || frame.length < MIN_FRAME_SIZE)
        {
            return EMPTY_BYTES;
        }
        int len = getPayloadLength(frame);
        if (len <= 0)
        {
            return EMPTY_BYTES;
        }
        byte[] result = new byte[len];
        System.arraycopy(frame, PAYLOAD_OFFSET, result, 0, len);
        return result;
    }

    public byte[] createData(char src, char dst, char id, byte[] payload)
    {
        return create(DATA_FLAG, src, dst, id, payload);
    }
    
    public byte[] createAck(char src, char dst, char id)
    {
        return create(ACK_FLAG, src, dst, id, EMPTY_BYTES);
    }
    public byte[] createAck(byte[] frame, char src)
    {
        char ackDst = getSrc(frame);
        char id = getId(frame);
        return createAck(src, ackDst, id);
    }

    public byte[] createNack(char src, char dst, char id, byte statusCode)
    {
        return create(NACK_FLAG, src, dst, id, new byte[]{statusCode});
    }
    public byte[] createNack(byte[] frame, char src, byte statusCode)
    {
        char ackDst = getSrc(frame);
        char id = getId(frame);
        return createNack(src, ackDst, id, statusCode);
    }

    public boolean isData(byte flags)
    {
        return (flags & (ACK_FLAG|NACK_FLAG)) == 0;
    }
    public boolean isData(byte[] frame)
    {
        return isData(getFlags(frame));
    }

    public boolean isAck(byte flags)
    {
        return (flags & ACK_FLAG) == ACK_FLAG;
    }
    public boolean isAck(byte[] frame)
    {
        return isAck(getFlags(frame));
    }

    public boolean isNack(byte flags)
    {
        return (flags & NACK_FLAG) == NACK_FLAG;
    }
    public boolean isNack(byte[] frame)
    {
        return isNack(getFlags(frame));
    }

    public char getStatus(byte[] frame)
    {
        return readChar(frame, LEN_OFFSET);
    }

}