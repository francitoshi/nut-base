/*
 *  ObfuscatedStringTest.java
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
package io.nut.base.security;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class ObfuscatedStringTest
{
    private static final char[] TESTDATA = "SensitiveData123".toCharArray();

    @Test
    void testConstructor()
    {
        new ObfuscatedString((char[])null);
        new ObfuscatedString((CharSequence)null);
        new ObfuscatedString(new char[0]);
        new ObfuscatedString(TESTDATA);

        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        assertFalse(secureString.isClosed(), "SecureString should not be closed after creation");
        assertEquals(TESTDATA.length, secureString.length(), "Length should match input data");
    }


    @Test
    void testLength()
    {
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        assertEquals(TESTDATA.length, secureString.length(), "Length should match input data");
    }

    @Test
    void testCharAt()
    {
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        for (int i = 0; i < TESTDATA.length; i++)
        {
            assertEquals(TESTDATA[i], secureString.charAt(i), "Character at index " + i + " should match");
        }
    }

    @Test
    void testToCharArray()
    {
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        assertArrayEquals(TESTDATA, secureString.toCharArray());
    }

    @Test
    void testCharAtInvalidIndex()
    {
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.charAt(-1), "Should throw IndexOutOfBoundsException for negative index");
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.charAt(TESTDATA.length), "Should throw IndexOutOfBoundsException for index out of bounds");
    }

    @Test
    void testSubSequence()
    {
        int start = 3;
        int end = 8;
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        CharSequence subSequence = secureString.subSequence(start, end);
        assertNotNull(subSequence, "Subsequence should not be null");
        assertTrue(subSequence instanceof ObfuscatedString, "Subsequence should be an AesSecureString");
        assertEquals(end - start, subSequence.length(), "Subsequence length should match");
    }

    @Test
    void testSubSequenceInvalidRange()
    {
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.subSequence(-1, 5), "Should throw IndexOutOfBoundsException for negative start");
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.subSequence(0, TESTDATA.length + 1), "Should throw IndexOutOfBoundsException for end out of bounds");
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.subSequence(5, 2), "Should throw IndexOutOfBoundsException for start > end");
    }

    @Test
    void testClose() throws Exception
    {
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        secureString.close();
        assertTrue(secureString.isClosed(), "SecureString should be closed");
        assertEquals(0, secureString.length(), "Length should be 0 after close");
        assertThrows(IllegalStateException.class, () -> secureString.charAt(0), "Should throw IllegalStateException after close");
    }

    @Test
    void testMultipleCloseCalls() throws Exception
    {
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        secureString.close();
        secureString.close(); // Second close should be idempotent
        assertTrue(secureString.isClosed(), "SecureString should remain closed");
    }

    @Test
    void testAutoCloseable() throws Exception
    {
        ObfuscatedString temp0;
        try (ObfuscatedString temp = temp0 = new ObfuscatedString(TESTDATA))
        {
            assertFalse(temp.isClosed(), "SecureString should not be closed within try block");
            assertEquals(TESTDATA.length, temp.length(), "Length should match input data");
        }
        assertTrue(temp0.isClosed(), "SecureString should be closed after try-with-resources");
    }

    @Test
    void testDataIntegrity()
    {
        final ObfuscatedString secureString = new ObfuscatedString(TESTDATA);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < secureString.length(); i++)
        {
            builder.append(secureString.charAt(i));
        }
        assertEquals(new String(TESTDATA), builder.toString(), "Decrypted data should match original");
    }

}
