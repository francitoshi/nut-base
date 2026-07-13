/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.net;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

public class EmailsTest
{
    static final String[] VALID = 
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
        "alice@example.com",
        "bob.smith@sub.example.org",
        "user+tag@example.co.uk",
        "a@b.co",
        "nombre_apellido123@mi-empresa.es",
        "x.y.z@dominio.com"
    };
    static final String[] INVALID = 
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
        "@", 
        "example@", 
        "@example.com",
        "",
        null,
        "correo_sin_arroba",
        "@sindominio.com",
        "usuario@",
        "usuario@@doble.com",
        "usuario espacio@example.com",
        "usuario@dominio..com",
        ".usuario@example.com"        
    };

    // Correos que solo son válidos si se permiten dominios locales (allowLocalDomains = true)
    private static final String[] LOCAL_VALID = 
    {
            "alice@localhost",
            "admin@mailserver",
            "test@intranet",
            "root@docker-host",
            "usuario@dev"
    };    
    /**
     * Test of isValidEmail method, of class Emails.
     */
    @Test
    public void testIsValidEmail()
    {
        for(String email : VALID)
        {
            assertTrue(Emails.isValidEmail(email), email);
        }
        for(String email : INVALID)
        {
            assertFalse(Emails.isValidEmail(email), email);
        }
        for(String email : LOCAL_VALID)
        {
            assertTrue(Emails.isValidEmail(email, true), email);
            assertFalse(Emails.isValidEmail(email, false), email);
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
