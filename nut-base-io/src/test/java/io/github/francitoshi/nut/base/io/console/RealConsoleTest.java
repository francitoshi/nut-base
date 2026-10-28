/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io.console;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;

/**
 *
 * @author franci
 */
public class RealConsoleTest
{
    
    public RealConsoleTest()
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

    RealConsole getInstance()
    {
        return new RealConsole(System.console())
        {
            @Override
            public VirtualConsole printf(String format, Object... args)
            {
                return this;
            }
        };
    }
    
    
    /**
     * Test of readLine method, of class RealConsole.
     */
    @Test
    @Timeout(1)
    public void testReadLine_String_ObjectArr()
    {
        RealConsole instance = getInstance();
        instance.addLine("1");
        instance.addLine("2");
        instance.addLine("3");
        assertEquals("1", instance.readLine("type 1"));
        assertEquals("2", instance.readLine("type 2"));
        assertEquals("3", instance.readLine("type 3"));
    }

    /**
     * Test of readLine method, of class RealConsole.
     */
    @Test
    @Timeout(1)
    public void testReadLine_0args()
    {
        RealConsole instance = getInstance();
        instance.addLine("1");
        instance.addLine("2");
        instance.addLine("3");
        assertEquals("1", instance.readLine());
        assertEquals("2", instance.readLine());
        assertEquals("3", instance.readLine());
    }

    /**
     * Test of readPassword method, of class RealConsole.
     */
    @Test
    @Timeout(1)
    public void testReadPassword_String_ObjectArr()
    {
        RealConsole instance = getInstance();
        instance.addLine("1");
        instance.addLine("2");
        instance.addLine("3");
        assertArrayEquals("1".toCharArray(), instance.readPassword("type 1"));
        assertArrayEquals("2".toCharArray(), instance.readPassword("type 2"));
        assertArrayEquals("3".toCharArray(), instance.readPassword("type 3"));
    }

    /**
     * Test of readPassword method, of class RealConsole.
     */
    @Test
    @Timeout(1)
    public void testReadPassword_0args()
    {
        RealConsole instance = getInstance();
        instance.addLine("1");
        instance.addLine("2");
        instance.addLine("3");
        assertArrayEquals("1".toCharArray(), instance.readPassword());
        assertArrayEquals("2".toCharArray(), instance.readPassword());
        assertArrayEquals("3".toCharArray(), instance.readPassword());
    }

}
