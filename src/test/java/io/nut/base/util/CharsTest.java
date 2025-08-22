/*
 *  CharsTest.java
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
package io.nut.base.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class CharsTest
{
    
    public CharsTest()
    {
    }

    @Test
    public void testToLowerCase()
    {
        assertNull(Chars.toLowerCase(null));
        assertEquals(0, Chars.toLowerCase("".toCharArray()).length);
        assertArrayEquals("abcdefghi123".toCharArray(), Chars.toLowerCase("ABCDEFGHI123".toCharArray()));
        assertArrayEquals("abcdefghi123".toCharArray(), Chars.toLowerCase("AbCdEfGhI123".toCharArray()));
    }

    @Test
    public void testToUpperCase()
    {
        assertNull(Chars.toUpperCase(null));
        assertEquals(0, Chars.toUpperCase("".toCharArray()).length);
        assertArrayEquals("ABCDEFGHI123".toCharArray(), Chars.toUpperCase("abcdefghi123".toCharArray()));
        assertArrayEquals("ABCDEFGHI123".toCharArray(), Chars.toUpperCase("AbCdEfGhI123".toCharArray()));
    }

    @Test
    public void testTrim()
    {
        assertNull(Chars.trim(null));
        assertEquals(0, Chars.trim("".toCharArray()).length);
        assertArrayEquals("ABCDEFGHI123".toCharArray(), Chars.trim("ABCDEFGHI123".toCharArray()));
        assertArrayEquals("   ABCDEFGHI123".trim().toCharArray(), Chars.trim("   ABCDEFGHI123".toCharArray()));
        assertArrayEquals("   ABCDEFGHI123   ".trim().toCharArray(), Chars.trim("   ABCDEFGHI123   ".toCharArray()));
        assertArrayEquals("ABCDEFGHI123   ".trim().toCharArray(), Chars.trim("ABCDEFGHI123   ".toCharArray()));
        assertArrayEquals("   ABC   DEFGHI   123   ".trim().toCharArray(), Chars.trim("   ABC   DEFGHI   123   ".toCharArray()));
    }

    @Test
    public void testContains()
    {
        
        assertTrue( "".contains("") );
        assertTrue( Chars.contains("".toCharArray(), "".toCharArray()) );
        
        assertTrue( "abc".contains("abc"));
        assertTrue( Chars.contains("abc".toCharArray(), "abc".toCharArray()) );

        assertFalse( Chars.contains("ab".toCharArray(), "abc".toCharArray()) );

        assertTrue( "123abc".contains("abc"));
        assertTrue( Chars.contains("123abc".toCharArray(), "abc".toCharArray()) );

        assertFalse( "123abc".contains("abcd"));
        assertFalse( Chars.contains("123abc".toCharArray(), "abcd".toCharArray()) );

        assertFalse( Chars.contains("abc".toCharArray(), "A".toCharArray()) );
        assertFalse( Chars.contains("abc".toCharArray(), "ABCD".toCharArray()) );
    }

    @Test
    public void testStartsWith()
    {
        assertTrue(Chars.startsWith("hello world!!!".toCharArray(), ""));
        assertFalse(Chars.startsWith("hello world!!!".toCharArray(), null));
        assertFalse(Chars.startsWith(null, null));
        assertTrue(Chars.startsWith("hello world!!!".toCharArray(), "hello"));
        assertFalse(Chars.startsWith("hello world!!!".toCharArray(), "hola"));
    }
    
}
