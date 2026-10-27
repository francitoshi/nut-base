/*
 * Copyright (C) 2013-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.encoding;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
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
public class Base91Test
{
    
    public Base91Test()
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
     * Test of encode method, of class Base91.
     * @throws java.io.UnsupportedEncodingException
     */
    @Test
    public void testAll() throws UnsupportedEncodingException
    {
        BigInteger _23 = BigInteger.valueOf(5);
        String data = Base91.encodeToString(_23.toByteArray());
//        System.out.println("data="+data);
        BigInteger result = new BigInteger(Base91.decodeFromString(data));
        assertEquals(_23, result);
        
        
    }

    
}
