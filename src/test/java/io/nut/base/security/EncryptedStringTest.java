/*
 *  EncryptedStringTest.java
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

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedStringTest
{
    private static final char[] TESTDATA = "SensitiveData123".toCharArray();

    @Test
    void testConstructor()
    {
        new EncryptedString(null);
        new EncryptedString(new char[0]);
        new EncryptedString(TESTDATA);

        final EncryptedString secureString = new EncryptedString(TESTDATA);
        assertFalse(secureString.isClosed(), "SecureString should not be closed after creation");
        assertEquals(TESTDATA.length, secureString.length(), "Length should match input data");
    }


    @Test
    void testLength()
    {
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        assertEquals(TESTDATA.length, secureString.length(), "Length should match input data");
    }

    @Test
    void testCharAt()
    {
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        for (int i = 0; i < TESTDATA.length; i++)
        {
            assertEquals(TESTDATA[i], secureString.charAt(i), "Character at index " + i + " should match");
        }
    }

    @Test
    void testCharAtInvalidIndex()
    {
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.charAt(-1), "Should throw IndexOutOfBoundsException for negative index");
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.charAt(TESTDATA.length), "Should throw IndexOutOfBoundsException for index out of bounds");
    }

    @Test
    void testSubSequence()
    {
        int start = 3;
        int end = 8;
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        CharSequence subSequence = secureString.subSequence(start, end);
        assertNotNull(subSequence, "Subsequence should not be null");
        assertTrue(subSequence instanceof EncryptedString, "Subsequence should be an AesSecureString");
        assertEquals(end - start, subSequence.length(), "Subsequence length should match");
    }

    @Test
    void testSubSequenceInvalidRange()
    {
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.subSequence(-1, 5), "Should throw IndexOutOfBoundsException for negative start");
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.subSequence(0, TESTDATA.length + 1), "Should throw IndexOutOfBoundsException for end out of bounds");
        assertThrows(IndexOutOfBoundsException.class, () -> secureString.subSequence(5, 2), "Should throw IndexOutOfBoundsException for start > end");
    }

    @Test
    void testClose()
    {
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        secureString.close();
        assertTrue(secureString.isClosed(), "SecureString should be closed");
        assertEquals(0, secureString.length(), "Length should be 0 after close");
        assertThrows(IllegalStateException.class, () -> secureString.charAt(0), "Should throw IllegalStateException after close");
    }

    @Test
    void testMultipleCloseCalls()
    {
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        secureString.close();
        secureString.close(); // Second close should be idempotent
        assertTrue(secureString.isClosed(), "SecureString should remain closed");
    }

    @Test
    void testAutoCloseable()
    {
        EncryptedString temp0;
        try (EncryptedString temp = temp0 = new EncryptedString(TESTDATA))
        {
            assertFalse(temp.isClosed(), "SecureString should not be closed within try block");
            assertEquals(TESTDATA.length, temp.length(), "Length should match input data");
        }
        assertTrue(temp0.isClosed(), "SecureString should be closed after try-with-resources");
    }

    @Test
    void testDataIntegrity()
    {
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < secureString.length(); i++)
        {
            builder.append(secureString.charAt(i));
        }
        assertEquals(new String(TESTDATA), builder.toString(), "Decrypted data should match original");
    }

    @Test
    void testEncryptionUniqueness()
    {
        final EncryptedString secureString = new EncryptedString(TESTDATA);
        // Create another SecureString with the same input
        try(EncryptedString otherSecureString = new EncryptedString(TESTDATA))
        {
            // Convert encrypted data to string for comparison (for testing purposes only)
            String encrypted1 = Arrays.toString(secureString.encryptedData);
            String encrypted2 = Arrays.toString(otherSecureString.encryptedData);
            assertNotEquals(encrypted1, encrypted2, "Encrypted data should be different due to random IV and key");
            otherSecureString.close();
        }
    }
}
