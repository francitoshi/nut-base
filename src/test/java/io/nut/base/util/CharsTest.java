/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
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
    
}
