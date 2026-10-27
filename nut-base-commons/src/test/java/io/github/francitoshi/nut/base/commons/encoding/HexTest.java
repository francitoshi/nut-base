/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.encoding;

import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class HexTest
{
    
    static final byte[] DATA1 = {0x00,0x01,0x12,0x23,0x34,0x45,0x56,0x67,0x78,0x0a,0x0b,0x1c,0x2d,0x3e,0x4f};
    static final byte[] DATA2 = {0,1,1,2,3,5,8,0xD,0x15,0x22,0x37,0x59,0x1a,0x2b,0x3c,0x4d,0x5e,0x6f};
    /**
     * Test of encode method, of class Hex.
     */
    @Test
    public void testEncode_byteArr_boolean()
    {
        assertEquals("0001122334455667780a0b1c2d3e4f", Hex.encode(DATA1, false));
        assertEquals("0001122334455667780A0B1C2D3E4F", Hex.encode(DATA1, true));
    
        assertEquals("000101020305080d152237591a2b3c4d5e6f", Hex.encode(DATA2, false));
        assertEquals("000101020305080D152237591A2B3C4D5E6F", Hex.encode(DATA2, true));
    }

    /**
     * Test of encode method, of class Hex.
     */
    @Test
    public void testEncode_byteArr()
    {
        assertEquals("0001122334455667780a0b1c2d3e4f", Hex.encode(DATA1));
        assertEquals("000101020305080d152237591a2b3c4d5e6f", Hex.encode(DATA2));    
    }

    /**
     * Test of decode method, of class Hex.
     */
    @Test
    public void testDecode() throws NoSuchAlgorithmException
    {
        assertArrayEquals(DATA1, Hex.decode("0001122334455667780a0b1c2d3e4f"));
        assertArrayEquals(DATA1, Hex.decode("0001122334455667780A0B1C2D3E4F"));
        assertArrayEquals(DATA2, Hex.decode("000101020305080d152237591a2b3c4d5e6f"));
        assertArrayEquals(DATA2, Hex.decode("000101020305080D152237591A2B3C4D5E6F"));
    }

    /**
     * Test of isValid method, of class Hex.
     */
    @Test
    public void testIsValid()
    {
        assertTrue(Hex.isValid(""));
        assertTrue(Hex.isValid("00"));
        assertTrue(Hex.isValid("0001122334455667780a0b1c2d3e4f"));
        assertTrue(Hex.isValid("0001122334455667780A0B1C2D3E4F"));
        assertTrue(Hex.isValid("0123456789ABCDEF"));
        assertTrue(Hex.isValid("0123456789abcdef"));

        assertFalse(Hex.isValid(null));
        assertFalse(Hex.isValid("0"));
        assertFalse(Hex.isValid("abc"));
        assertFalse(Hex.isValid("0g"));
        assertFalse(Hex.isValid("0G"));
        assertFalse(Hex.isValid("00 11"));
        assertFalse(Hex.isValid("0x00"));
        assertFalse(Hex.isValid("éé"));
        assertFalse(Hex.isValid("ññ"));
    }
}
