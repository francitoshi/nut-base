/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class BalanceTest
{
    
    public BalanceTest()
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
     * Test of add method, of class Balance.
     */
    @Test
    public void testAdd()
    {
        Balance instance = new Balance(16, RoundingMode.HALF_UP, 2);

        BigDecimal a1 = instance.add("a", BigDecimal.ONE);
        assertTrue(BigDecimal.ONE.compareTo(a1)==0);

        BigDecimal b1 = instance.add("b", BigDecimal.ONE);
        assertTrue(BigDecimal.ONE.compareTo(b1)==0);

        BigDecimal a2 = instance.add("a", BigDecimal.ONE);
        assertTrue(BigDecimal.valueOf(2).compareTo(a2)==0);
        
        BigDecimal b2 = instance.add("b", BigDecimal.ONE);
        assertTrue(BigDecimal.valueOf(2).compareTo(b2)==0);
    }

    /**
     * Test of subtract method, of class Balance.
     */
    @Test
    public void testSubtract()
    {
        Balance instance = new Balance(16, RoundingMode.HALF_UP, 2);

        BigDecimal a1 = instance.subtract("a", BigDecimal.ONE);
        assertTrue(BigDecimal.ONE.negate().compareTo(a1)==0);

        BigDecimal b1 = instance.subtract("b", BigDecimal.ONE);
        assertTrue(BigDecimal.ONE.negate().compareTo(b1)==0);

        BigDecimal a2 = instance.subtract("a", BigDecimal.ONE);
        assertTrue(BigDecimal.valueOf(-2).compareTo(a2)==0);
        
        BigDecimal b2 = instance.subtract("b", BigDecimal.ONE);
        assertTrue(BigDecimal.valueOf(-2).compareTo(b2)==0);

    }

    /**
     * Test of get method, of class Balance.
     */
    @Test
    public void testGet_1args_1()
    {
        Balance instance = new Balance(16, RoundingMode.HALF_UP, 2);

        instance.add("a", BigDecimal.ONE);
        BigDecimal a = instance.get("a");
        assertTrue(BigDecimal.ONE.compareTo(a)==0);
    }
    
}
