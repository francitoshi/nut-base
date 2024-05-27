/*
 * BalanceTest.java
 *
 * Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.util;

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
