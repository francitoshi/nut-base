/*
 *  ROTTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto.human;

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
