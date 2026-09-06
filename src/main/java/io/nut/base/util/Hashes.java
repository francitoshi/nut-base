/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.zip.Adler32;
import java.util.zip.CRC32;

/**
 * Utility class providing common non-cryptographic checksum and hash functions.
 *
 * @author franci
 */
public class Hashes
{
    private Hashes()
    {
    }

    /**
     * Returns the CRC-16/CCITT-FALSE checksum of the given portion of bytes.
     * <p>
     * The polynomial used is 0x1021 with an initial value of 0xFFFF and no final XOR.
     *
     * @param bytes the input data.
     * @param off the offset of the first byte to include.
     * @param len the number of bytes to include.
     * @return the 16-bit CRC-16/CCITT-FALSE checksum.
     */
    public static int crc16(byte[] bytes, int off, int len)
    {
        int crc = 0xFFFF;
        for (int i = off; i < off + len; i++)
        {
            crc ^= (bytes[i] & 0xFF) << 8;
            for (int j = 0; j < 8; j++)
            {
                if ((crc & 0x8000) != 0)
                {
                    crc = (crc << 1) ^ 0x1021;
                }
                else
                {
                    crc <<= 1;
                }
            }
            crc &= 0xFFFF;
        }
        return crc;
    }

    /**
     * Returns the CRC-16/CCITT-FALSE checksum of the given bytes.
     * <p>
     * The polynomial used is 0x1021 with an initial value of 0xFFFF and no final XOR.
     *
     * @param bytes the input data.
     * @return the 16-bit CRC-16/CCITT-FALSE checksum.
     */
    public static int crc16(byte[] bytes)
    {
        return crc16(bytes, 0, bytes.length);
    }

    /**
     * Returns the Adler-32 checksum of the given portion of bytes.
     *
     * @param bytes the input data.
     * @param off the offset of the first byte to include.
     * @param len the number of bytes to include.
     * @return the 32-bit Adler-32 checksum.
     */
    public static long adler32(byte[] bytes, int off, int len)
    {
        Adler32 adler32 = new Adler32();
        adler32.update(bytes, off, len);
        return adler32.getValue();
    }

    /**
     * Returns the Adler-32 checksum of the given bytes.
     *
     * @param bytes the input data.
     * @return the 32-bit Adler-32 checksum.
     */
    public static long adler32(byte[] bytes)
    {
        return adler32(bytes,0,bytes.length);
    }

    /**
     * Returns the Adler-32 checksum of the given portion of bytes as a 4-byte big-endian array.
     *
     * @param bytes the input data.
     * @param off the offset of the first byte to include.
     * @param len the number of bytes to include.
     * @param rc the 4-byte array to store the result into, or null to allocate a new one.
     * @return the 4-byte big-endian Adler-32 checksum.
     */
    public static byte[] adler32(byte[] bytes, int off, int len, byte[] rc) 
    {
        if (rc == null || rc.length != 4)
        {
            rc = new byte[4];
        }
        
        long value = adler32(bytes, off, len);
        // store the 4 bytes of Adler32 int the rc array (big-endian)
        rc[0] = (byte) ((value >> 24) & 0xFF);
        rc[1] = (byte) ((value >> 16) & 0xFF);
        rc[2] = (byte) ((value >> 8) & 0xFF);
        rc[3] = (byte) (value & 0xFF);
        return rc;
    }    

    /**
     * Returns the CRC-32 checksum of the given portion of bytes.
     *
     * @param bytes the input data.
     * @param off the offset of the first byte to include.
     * @param len the number of bytes to include.
     * @return the 32-bit CRC-32 checksum.
     */
    public static long crc32(byte[] bytes, int off, int len)
    {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, off, len);
        return crc32.getValue();
    }

    /**
     * Returns the CRC-32 checksum of the given bytes.
     *
     * @param bytes the input data.
     * @return the 32-bit CRC-32 checksum.
     */
    public static long crc32(byte[] bytes)
    {
        return crc32(bytes, 0, bytes.length);
    }

    /**
     * Returns the CRC-32 checksum of the given portion of bytes as a 4-byte big-endian array.
     *
     * @param bytes the input data.
     * @param off the offset of the first byte to include.
     * @param len the number of bytes to include.
     * @param rc the 4-byte array to store the result into, or null to allocate a new one.
     * @return the 4-byte big-endian CRC-32 checksum.
     */
    public static byte[] crc32(byte[] bytes, int off, int len, byte[] rc) 
    {
        if (rc == null || rc.length != 4)
        {
            rc = new byte[4];
        }
        
        long value = crc32(bytes, off, len);
        // store the 4 bytes of CRC32 int the rc array (big-endian)
        rc[0] = (byte) ((value >> 24) & 0xFF);
        rc[1] = (byte) ((value >> 16) & 0xFF);
        rc[2] = (byte) ((value >> 8) & 0xFF);
        rc[3] = (byte) (value & 0xFF);
        return rc;
    }    
}
