/*
 * EmailsTest.java
 *
 * Copyright (c) 2014-2024 francitoshi@gmail.com
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

/**
 *
 * @author franci
 */
public class EmailsTest
{
    
    public EmailsTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }

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
    
}
