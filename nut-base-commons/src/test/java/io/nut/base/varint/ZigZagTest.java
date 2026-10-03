/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.varint;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ZigZagTest
{
    /**
     * Test of encode method, of class ZigZag.
     */
    @Test
    public void testEncode_int()
    {
        assertEquals(0, ZigZag.encode(0));
        assertEquals(1, ZigZag.encode(-1));
        assertEquals(2, ZigZag.encode(1));
        assertEquals(3, ZigZag.encode(-2));
        assertEquals(4, ZigZag.encode(2));
        assertEquals(5, ZigZag.encode(-3));
        assertEquals(-2, ZigZag.encode(Integer.MAX_VALUE));
        assertEquals(-1, ZigZag.encode(Integer.MIN_VALUE));
    }

    /**
     * Test of decode method, of class ZigZag.
     */
    @Test
    public void testDecode_int()
    {
        assertEquals(0, ZigZag.decode(0));
        assertEquals(-1, ZigZag.decode(1));
        assertEquals(1, ZigZag.decode(2));
        assertEquals(-2, ZigZag.decode(3));
        assertEquals(2, ZigZag.decode(4));
        assertEquals(-3, ZigZag.decode(5));
        assertEquals(Integer.MAX_VALUE, ZigZag.decode(-2));
        assertEquals(Integer.MIN_VALUE, ZigZag.decode(-1));
    }

    /**
     * Test of encode/decode round trip, of class ZigZag.
     */
    @Test
    public void testRoundTrip_int()
    {
        int[] values = {0, 1, -1, 2, -2, 127, -128, 128, -129, 32767, -32768,
                        32768, -32769, 1000000, -1000000, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int value : values)
        {
            assertEquals(value, ZigZag.decode(ZigZag.encode(value)));
        }
    }

    /**
     * Test of encode method, of class ZigZag.
     */
    @Test
    public void testEncode_long()
    {
        assertEquals(0L, ZigZag.encode(0L));
        assertEquals(1L, ZigZag.encode(-1L));
        assertEquals(2L, ZigZag.encode(1L));
        assertEquals(3L, ZigZag.encode(-2L));
        assertEquals(4L, ZigZag.encode(2L));
        assertEquals(5L, ZigZag.encode(-3L));
        assertEquals(-2L, ZigZag.encode(Long.MAX_VALUE));
        assertEquals(-1L, ZigZag.encode(Long.MIN_VALUE));
    }

    /**
     * Test of decode method, of class ZigZag.
     */
    @Test
    public void testDecode_long()
    {
        assertEquals(0L, ZigZag.decode(0L));
        assertEquals(-1L, ZigZag.decode(1L));
        assertEquals(1L, ZigZag.decode(2L));
        assertEquals(-2L, ZigZag.decode(3L));
        assertEquals(2L, ZigZag.decode(4L));
        assertEquals(-3L, ZigZag.decode(5L));
        assertEquals(Long.MAX_VALUE, ZigZag.decode(-2L));
        assertEquals(Long.MIN_VALUE, ZigZag.decode(-1L));
    }

    /**
     * Test of encode/decode round trip, of class ZigZag.
     */
    @Test
    public void testRoundTrip_long()
    {
        long[] values = {0L, 1L, -1L, 2L, -2L, 2147483647L, -2147483648L, Long.MAX_VALUE, Long.MIN_VALUE};
        for (long value : values)
        {
            assertEquals(value, ZigZag.decode(ZigZag.encode(value)));
        }
    }

    /**
     * Test of encode method, of class ZigZag.
     */
    @Test
    public void testEncode_BigInteger()
    {
        assertEquals(BigInteger.ZERO, ZigZag.encode(BigInteger.ZERO));
        assertEquals(BigInteger.ONE, ZigZag.encode(BigInteger.ONE.negate()));
        assertEquals(BigInteger.valueOf(2), ZigZag.encode(BigInteger.ONE));
        assertEquals(BigInteger.valueOf(3), ZigZag.encode(new BigInteger("-2")));
        assertEquals(new BigInteger("123456789012345678901234567890").shiftLeft(1),
                     ZigZag.encode(new BigInteger("123456789012345678901234567890")));
        assertEquals(new BigInteger("123456789012345678901234567890").shiftLeft(1).subtract(BigInteger.ONE),
                     ZigZag.encode(new BigInteger("-123456789012345678901234567890")));
    }

    /**
     * Test of decode method, of class ZigZag.
     */
    @Test
    public void testDecode_BigInteger()
    {
        assertEquals(BigInteger.ZERO, ZigZag.decode(BigInteger.ZERO));
        assertEquals(BigInteger.ONE.negate(), ZigZag.decode(BigInteger.ONE));
        assertEquals(BigInteger.ONE, ZigZag.decode(BigInteger.valueOf(2)));
        assertEquals(new BigInteger("-2"), ZigZag.decode(BigInteger.valueOf(3)));
    }

    /**
     * Test of encode/decode round trip, of class ZigZag.
     */
    @Test
    public void testRoundTrip_BigInteger()
    {
        BigInteger[] values = {
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.ONE.negate(),
            new BigInteger("123456789012345678901234567890"),
            new BigInteger("-123456789012345678901234567890"),
            BigInteger.valueOf(Long.MAX_VALUE).shiftLeft(100),
            BigInteger.valueOf(Long.MIN_VALUE).shiftLeft(100).negate()
        };
        for (BigInteger value : values)
        {
            assertEquals(value, ZigZag.decode(ZigZag.encode(value)));
        }
    }

    /**
     * Test that decode(encode(v)) equals v consistently for int, long and BigInteger.
     */
    @Test
    public void testAllDomainsConsistent()
    {
        int[] ints = {0, 1, -1, 2, -2, 1000000, -1000000, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int value : ints)
        {
            assertEquals(value, ZigZag.decode(ZigZag.encode(value)));
            long l = value;
            assertEquals(l, ZigZag.decode(ZigZag.encode(l)));
            BigInteger b = BigInteger.valueOf(value);
            assertEquals(b, ZigZag.decode(ZigZag.encode(b)));
        }
    }
}