/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.os;

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
public class OSTest
{
    
    public OSTest()
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
     * Test of getName method, of class OSName.
     */
    @Test
    public void testGetName()
    {
        OS os = new OS("name","version","arch");
        assertEquals("name", os.getName());
    }

    /**
     * Test of getVersion method, of class OSName.
     */
    @Test
    public void testGetVersion()
    {
        OS os = new OS("name","version","arch");
        assertEquals("version", os.getVersion());
    }

    /**
     * Test of getArch method, of class OSName.
     */
    @Test
    public void testGetArch()
    {
        OS os = new OS("name","version","arch");
        assertEquals("arch", os.getArch());
    }

    /**
     * Test of toString method, of class OSName.
     */
    @Test
    public void testToString()
    {
     
        OS name = new OS("name");
        assertEquals("name", name.toString());
        
        OS name_version = new OS("name","version");
        assertEquals("name version", name_version.toString());

        OS name_version_arch = new OS("name","version","arch");
        assertEquals("name version arch", name_version_arch.toString());
    }
}
