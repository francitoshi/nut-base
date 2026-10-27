/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

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
public class JavaTest
{
    
    public JavaTest()
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

    @Test
    public void testSomeMethod()
    {
        assertEquals(8, Java.JAVA_INT_VERSION);
    }
    
    @Test
    public void testBitsConstants()
    {
        assertEquals(8, Java.BYTE_BITS);
        assertEquals(16, Java.SHORT_BITS);
        assertEquals(16, Java.CHAR_BITS);
        assertEquals(32, Java.INT_BITS);
        assertEquals(64, Java.LONG_BITS);
        assertEquals(32, Java.FLOAT_BITS);
        assertEquals(64, Java.DOUBLE_BITS);

    }
    
}
