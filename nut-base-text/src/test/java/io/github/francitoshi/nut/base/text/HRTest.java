/*
 * Copyright (C) 2022-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.text;

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
public class HRTest
{
    
    public HRTest()
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

    /**
     * Test of get method, of class HR.
     */
    @Test
    public void testGet_CharSequence()
    {
        HR instance = new HR('-', 10, 3);

        assertEquals("----------", instance.get(""));
        assertEquals("---one----", instance.get("one"));
        assertEquals("---spain--", instance.get("spain"));
    }

    /**
     * Test of get method, of class HR.
     */
    @Test
    public void testGet()
    {
        HR instance = new HR('-', 10, 2);

        assertEquals("----------", instance.get());
    }
    
}
