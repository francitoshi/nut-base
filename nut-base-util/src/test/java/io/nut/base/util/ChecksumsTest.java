/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ChecksumsTest
{
    /**
     * Test of crc16 method, of class Hashes.
     */
    @Test
    public void testCrc16_3args()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        assertEquals(0x29B1, Checksums.crc16(bytes123456789, 0, bytes123456789.length));

        byte[] bytes0x00 = {0x00};
        assertEquals(0xE1F0, Checksums.crc16(bytes0x00, 0, bytes0x00.length));

        byte[] bytes0 = {};
        assertEquals(0xFFFF, Checksums.crc16(bytes0, 0, bytes0.length));
    }

    /**
     * Test of crc16 method, of class Hashes.
     */
    @Test
    public void testCrc16_byteArr()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        assertEquals(0x29B1, Checksums.crc16(bytes123456789));

        byte[] bytes0x00 = {0x00};
        assertEquals(0xE1F0, Checksums.crc16(bytes0x00));

        byte[] bytes0 = {};
        assertEquals(0xFFFF, Checksums.crc16(bytes0));
    }

    /**
     * Test of adler32 method, of class Hashes.
     */
    @Test
    public void testAdler32_3args()
    {
        byte[] wiki = "Wikipedia".getBytes();
        assertEquals(0x11E60398L, Checksums.adler32(wiki, 0, wiki.length));

        byte[] empty = {};
        assertEquals(1L, Checksums.adler32(empty, 0, empty.length));
    }

    /**
     * Test of adler32 method, of class Hashes.
     */
    @Test
    public void testAdler32_byteArr()
    {
        byte[] wiki = "Wikipedia".getBytes();
        assertEquals(0x11E60398L, Checksums.adler32(wiki));

        byte[] empty = {};
        assertEquals(1L, Checksums.adler32(empty));
    }

    /**
     * Test of adler32 method, of class Hashes.
     */
    @Test
    public void testAdler32_4args()
    {
        byte[] wiki = "Wikipedia".getBytes();
        long value = Checksums.adler32(wiki, 0, wiki.length);

        byte[] rc = new byte[4];
        assertSame(rc, Checksums.adler32(wiki, 0, wiki.length, rc));
        assertEquals((byte) ((value >> 24) & 0xFF), rc[0]);
        assertEquals((byte) ((value >> 16) & 0xFF), rc[1]);
        assertEquals((byte) ((value >> 8) & 0xFF), rc[2]);
        assertEquals((byte) (value & 0xFF), rc[3]);

        byte[] rcAlloc = Checksums.adler32(wiki, 0, wiki.length, null);
        assertEquals(4, rcAlloc.length);
        assertArrayEquals(rc, rcAlloc);

        byte[] badSize = new byte[2];
        byte[] rcFixed = Checksums.adler32(wiki, 0, wiki.length, badSize);
        assertEquals(4, rcFixed.length);
        assertArrayEquals(rc, rcFixed);
    }

    /**
     * Test of crc32 method, of class Hashes.
     */
    @Test
    public void testCrc32_3args()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        assertEquals(0xCBF43926L, Checksums.crc32(bytes123456789, 0, bytes123456789.length));

        byte[] empty = {};
        assertEquals(0L, Checksums.crc32(empty, 0, empty.length));
    }

    /**
     * Test of crc32 method, of class Hashes.
     */
    @Test
    public void testCrc32_byteArr()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        assertEquals(0xCBF43926L, Checksums.crc32(bytes123456789));

        byte[] empty = {};
        assertEquals(0L, Checksums.crc32(empty));
    }

    /**
     * Test of crc32 method, of class Hashes.
     */
    @Test
    public void testCrc32_4args()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        long value = Checksums.crc32(bytes123456789, 0, bytes123456789.length);

        byte[] rc = new byte[4];
        assertSame(rc, Checksums.crc32(bytes123456789, 0, bytes123456789.length, rc));
        assertEquals((byte) ((value >> 24) & 0xFF), rc[0]);
        assertEquals((byte) ((value >> 16) & 0xFF), rc[1]);
        assertEquals((byte) ((value >> 8) & 0xFF), rc[2]);
        assertEquals((byte) (value & 0xFF), rc[3]);

        byte[] rcAlloc = Checksums.crc32(bytes123456789, 0, bytes123456789.length, null);
        assertEquals(4, rcAlloc.length);
        assertArrayEquals(rc, rcAlloc);

        byte[] badSize = new byte[2];
        byte[] rcFixed = Checksums.crc32(bytes123456789, 0, bytes123456789.length, badSize);
        assertEquals(4, rcFixed.length);
        assertArrayEquals(rc, rcFixed);
    }

    /**
     * Test of crc32c method, of class Checksums.
     */
    @Test
    public void testCrc32c_3args()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        assertEquals(0xE3069283L, Checksums.crc32c(bytes123456789, 0, bytes123456789.length));

        byte[] bytes0 = {};
        assertEquals(0L, Checksums.crc32c(bytes0, 0, bytes0.length));
    }

    /**
     * Test of crc32c method, of class Checksums.
     */
    @Test
    public void testCrc32c_byteArr()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        assertEquals(0xE3069283L, Checksums.crc32c(bytes123456789));

        byte[] bytes0 = {};
        assertEquals(0L, Checksums.crc32c(bytes0));
    }

    /**
     * Test of crc32c method, of class Checksums.
     */
    @Test
    public void testCrc32c_4args()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        long value = Checksums.crc32c(bytes123456789, 0, bytes123456789.length);

        byte[] rc = new byte[4];
        assertSame(rc, Checksums.crc32c(bytes123456789, 0, bytes123456789.length, rc));
        assertEquals((byte) ((value >> 24) & 0xFF), rc[0]);
        assertEquals((byte) ((value >> 16) & 0xFF), rc[1]);
        assertEquals((byte) ((value >> 8) & 0xFF), rc[2]);
        assertEquals((byte) (value & 0xFF), rc[3]);

        byte[] rcAlloc = Checksums.crc32c(bytes123456789, 0, bytes123456789.length, null);
        assertEquals(4, rcAlloc.length);
        assertArrayEquals(rc, rcAlloc);

        byte[] badSize = new byte[2];
        byte[] rcFixed = Checksums.crc32c(bytes123456789, 0, bytes123456789.length, badSize);
        assertEquals(4, rcFixed.length);
        assertArrayEquals(rc, rcFixed);
    }

    /**
     * Test of crc64 method, of class Checksums.
     */
    @Test
    public void testCrc64_3args()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        assertEquals(0x6C40DF5F0B497347L, Checksums.crc64(bytes123456789, 0, bytes123456789.length));

        byte[] bytes0 = {};
        assertEquals(0L, Checksums.crc64(bytes0, 0, bytes0.length));
    }

    /**
     * Test of crc64 method, of class Checksums.
     */
    @Test
    public void testCrc64_byteArr()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        assertEquals(0x6C40DF5F0B497347L, Checksums.crc64(bytes123456789));

        byte[] bytes0 = {};
        assertEquals(0L, Checksums.crc64(bytes0));
    }

    /**
     * Test of crc64 method, of class Checksums.
     */
    @Test
    public void testCrc64_4args()
    {
        byte[] bytes123456789 = "123456789".getBytes();
        long value = Checksums.crc64(bytes123456789, 0, bytes123456789.length);

        byte[] rc = new byte[8];
        assertSame(rc, Checksums.crc64(bytes123456789, 0, bytes123456789.length, rc));
        assertEquals((byte) ((value >> 56) & 0xFF), rc[0]);
        assertEquals((byte) ((value >> 48) & 0xFF), rc[1]);
        assertEquals((byte) ((value >> 40) & 0xFF), rc[2]);
        assertEquals((byte) ((value >> 32) & 0xFF), rc[3]);
        assertEquals((byte) ((value >> 24) & 0xFF), rc[4]);
        assertEquals((byte) ((value >> 16) & 0xFF), rc[5]);
        assertEquals((byte) ((value >> 8) & 0xFF), rc[6]);
        assertEquals((byte) (value & 0xFF), rc[7]);

        byte[] rcAlloc = Checksums.crc64(bytes123456789, 0, bytes123456789.length, null);
        assertEquals(8, rcAlloc.length);
        assertArrayEquals(rc, rcAlloc);

        byte[] badSize = new byte[4];
        byte[] rcFixed = Checksums.crc64(bytes123456789, 0, bytes123456789.length, badSize);
        assertEquals(8, rcFixed.length);
        assertArrayEquals(rc, rcFixed);
    }
}