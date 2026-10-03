/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.encoding;

import io.nut.base.encoding.ROT;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ROTTest
{
    static final String EMPTY = "";
    static final String SRC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz/*-+.";
    static final String ROT5 = "5678901234ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz/*-+.";
    static final String ROT13 = "0123456789NOPQRSTUVWXYZABCDEFGHIJKLMnopqrstuvwxyzabcdefghijklm/*-+.";
    static final String ROT18 = "5678901234NOPQRSTUVWXYZABCDEFGHIJKLMnopqrstuvwxyzabcdefghijklm/*-+.";
    static final String ROT47 = "_`abcdefghpqrstuvwxyz{|}~!\"#$%&'()*+23456789:;<=>?@ABCDEFGHIJK^Y\\Z]";
    
    @Test
    public void testRot5_charArr()
    {
        assertNull(ROT.rot5((char[])null));
        assertArrayEquals(EMPTY.toCharArray(), ROT.rot5(EMPTY.toCharArray()));
        assertArrayEquals(ROT5.toCharArray(), ROT.rot5(SRC.toCharArray()));
        assertArrayEquals(SRC.toCharArray(), ROT.rot5(ROT5.toCharArray()));
    }

    @Test
    public void testRot13_charArr()
    {
        assertNull(ROT.rot13((char[])null));
        assertArrayEquals(EMPTY.toCharArray(), ROT.rot13(EMPTY.toCharArray()));
        assertArrayEquals(ROT13.toCharArray(), ROT.rot13(SRC.toCharArray()));
        assertArrayEquals(SRC.toCharArray(), ROT.rot13(ROT13.toCharArray()));
    }

    @Test
    public void testRot18_charArr()
    {
        assertNull(ROT.rot18((char[])null));
        assertArrayEquals(EMPTY.toCharArray(), ROT.rot18(EMPTY.toCharArray()));
        assertArrayEquals(ROT18.toCharArray(), ROT.rot18(SRC.toCharArray()));
        assertArrayEquals(SRC.toCharArray(), ROT.rot18(ROT18.toCharArray()));
    }

    @Test
    public void testRot47_charArr()
    {
        assertNull(ROT.rot47((char[])null));
        assertArrayEquals(EMPTY.toCharArray(), ROT.rot47(EMPTY.toCharArray()));
        assertArrayEquals(ROT47.toCharArray(), ROT.rot47(SRC.toCharArray()));
        assertArrayEquals(SRC.toCharArray(), ROT.rot47(ROT47.toCharArray()));
    }

    @Test
    public void testRot5_String()
    {
        assertNull(ROT.rot5((String)null));
        assertEquals(EMPTY, ROT.rot5(EMPTY));
        assertEquals(ROT5, ROT.rot5(SRC));
        assertEquals(SRC, ROT.rot5(ROT5));
    }

    @Test
    public void testRot13_String()
    {
        assertNull(ROT.rot13((String)null));
        assertEquals(EMPTY, ROT.rot13(EMPTY));
        assertEquals(ROT13, ROT.rot13(SRC));
        assertEquals(SRC, ROT.rot13(ROT13));
    }

    @Test
    public void testRot18_String()
    {
        assertNull(ROT.rot18((String)null));
        assertEquals(EMPTY, ROT.rot18(EMPTY));
        assertEquals(ROT18, ROT.rot18(SRC));
        assertEquals(SRC, ROT.rot18(ROT18));
    }

    @Test
    public void testRot47_String()
    {
        assertNull(ROT.rot47((String)null));
        assertEquals(EMPTY, ROT.rot47(EMPTY));
        assertEquals(ROT47, ROT.rot47(SRC));
        assertEquals(SRC, ROT.rot47(ROT47));
    }
    
}
