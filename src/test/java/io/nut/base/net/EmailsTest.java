/*
 * EmailsTest.java
 *
 * Copyright (c) 2014-2026 francitoshi@gmail.com
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
package io.nut.base.net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 *
 * @author franci
 */
public class EmailsTest
{
    static final String[] valid = 
    { 
        "example@yahoo.com",
        "example-100@yahoo.com", 
        "example.100@yahoo.com",
        "example111@example.com", 
        "example-100@example.net",
        "example.100@example.com.au", 
        "example@1.com",
        "example@gmail.com.com", 
        "example+100@gmail.com",
        "example-100@yahoo-test.com",
    };
    static final String[] invalid = 
    {
        "example", 
        "example@.com.my",
        "example123@gmail.a", 
        "example123@.com", 
        "example123@.com.com",
        ".example@example.com", 
        "example()*@gmail.com", 
        "example@%*.com",
        "example..2002@gmail.com", 
        "example.@gmail.com",
        "example@example@gmail.com", 
        "example@gmail.com.1a",
        "@", 
        "example@", 
        "@example.com"
    };

    /**
     * Test of isValidEmail method, of class Emails.
     */
    @Test
    public void testIsValidEmail()
    {
        for(String email : valid)
        {
            assertTrue(Emails.isValidEmail(email), email);
        }
        for(String email : invalid)
        {
            assertFalse(Emails.isValidEmail(email), email);
        }

    }
    
    // -------------------------------------------------------------------------
    // Full format: "Name <email>"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Format 'name <email>' extracts name and email correctly")
    void testFullFormat() 
    {
        String[] result = Emails.parseEmailAddress("francitoshi <francitoshi@gmail.com>");
        assertAll
        (
            () -> assertEquals("francitoshi", result[0], "Name should be 'francitoshi'"),
            () -> assertEquals("francitoshi@gmail.com", result[1], "Email should be 'francitoshi@gmail.com'")
        );
    }

    @Test
    @DisplayName("Format 'name <email>' with extra surrounding spaces")
    void testFullFormatWithExtraSpaces() 
    {
        String[] result = Emails.parseEmailAddress("  francitoshi   <francitoshi@gmail.com>  ");
        assertAll
        (
            () -> assertEquals("francitoshi", result[0]),
            () -> assertEquals("francitoshi@gmail.com", result[1])
        );
    }

    @Test
    @DisplayName("Format 'name <email>' with compound name")
    void testFullFormatWithCompoundName() 
    {
        String[] result = Emails.parseEmailAddress("Francisco Toshi <francitoshi@gmail.com>");
        assertAll
        (
            () -> assertEquals("Francisco Toshi", result[0]),
            () -> assertEquals("francitoshi@gmail.com", result[1])
        );
    }

    // -------------------------------------------------------------------------
    // Angle brackets only: "<email>"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Format '<email>' returns empty name and correct email")
    void testAngleBracketsOnly()
    {
        String[] result = Emails.parseEmailAddress("<francitoshi@gmail.com>");
        assertAll
        (
            () -> assertEquals("", result[0], "Name should be empty"),
            () -> assertEquals("francitoshi@gmail.com", result[1])
        );
    }

    @Test
    @DisplayName("Format '<email>' with inner spaces inside angle brackets")
    void testAngleBracketsWithInnerSpaces() 
    {
        String[] result = Emails.parseEmailAddress("< francitoshi@gmail.com >");
        assertAll
        (
            () -> assertEquals("", result[0]),
            () -> assertEquals("francitoshi@gmail.com", result[1])
        );
    }

    // -------------------------------------------------------------------------
    // Plain format: "email"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Plain email format returns empty name and correct email")
    void testPlainEmail()
    {
        String[] result = Emails.parseEmailAddress("francitoshi@gmail.com");
        assertAll
        (
            () -> assertEquals("", result[0], "Name should be empty"),
            () -> assertEquals("francitoshi@gmail.com", result[1])
        );
    }

    @Test
    @DisplayName("Plain email format with surrounding spaces")
    void testPlainEmailWithSpaces() 
    {
        String[] result = Emails.parseEmailAddress("  francitoshi@gmail.com  ");
        assertAll
        (
            () -> assertEquals("", result[0]),
            () -> assertEquals("francitoshi@gmail.com", result[1])
        );
    }

    // -------------------------------------------------------------------------
    // Edge cases: null, empty, invalid
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Null input returns null")
    void testNullInput()
    {
        String[] result = Emails.parseEmailAddress(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Input without '@' returns empty email")
    void testInputWithoutAtSign()
    {
        String[] result = Emails.parseEmailAddress("this-is-not-an-email");
        assertAll
        (
            () -> assertEquals("", result[0]),
            () -> assertEquals("", result[1])
        );
    }

    @Test
    @DisplayName("Angle brackets without '@' inside return empty email")
    void testAngleBracketsWithoutAtSign()
    {
        String[] result = Emails.parseEmailAddress("name <noAtSign>");
        assertAll
        (
            () -> assertEquals("name", result[0]),
            () -> assertEquals("", result[1])
        );
    }

}
