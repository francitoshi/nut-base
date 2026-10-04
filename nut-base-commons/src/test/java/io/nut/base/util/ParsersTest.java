/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.math.BigDecimal;
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
public class ParsersTest
{
    
    public ParsersTest()
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
     * Test of safeParseInt method, of class Parsers.
     */
    @Test
    public void testSafeParseInt_String_int()
    {
        assertEquals(-1, Parsers.parseInt(null, -1));
        assertEquals(-1, Parsers.parseInt("", -1));
        assertEquals(-1, Parsers.parseInt("a", -1));
        assertEquals(1, Parsers.parseInt(" 1", 0));
        assertEquals(1, Parsers.parseInt("1 ", 0));
    }

    /**
     * Test of safeParseInt method, of class Parsers.
     */
    @Test
    public void testSafeParseInt_String()
    {
        assertEquals(0, Parsers.parseInt(null));
        assertEquals(0, Parsers.parseInt(""));
        assertEquals(0, Parsers.parseInt("a"));
        assertEquals(1, Parsers.parseInt(" 1"));
        assertEquals(1, Parsers.parseInt("1 "));
    }
    /**
     * Test of safeParseLong method, of class Parsers.
     */
    @Test
    public void testSafeParseLong_String_long()
    {
        assertEquals(-1, Parsers.parseLong(null, -1));
        assertEquals(-1, Parsers.parseLong("", -1));
        assertEquals(-1, Parsers.parseLong("a", -1));
        assertEquals(1, Parsers.parseLong(" 1", 0));
        assertEquals(1, Parsers.parseLong("1 ", 0));
    }
    /**
     * Test of safeParseLong method, of class Parsers.
     */
    @Test
    public void testSafeParseLong_String_long_int()
    {
        assertEquals(-1, Parsers.parseLong(null, -1, 16));
        assertEquals(-1, Parsers.parseLong("", -1, 16));
        assertEquals(10, Parsers.parseLong("a", -1, 16));
        assertEquals(1, Parsers.parseLong(" 1", 0, 16));
        assertEquals(1, Parsers.parseLong("1 ", 0, 16));
        assertEquals(255, Parsers.parseLong("ff", 0, 16));
    }

    /**
     * Test of safeParseLong method, of class Parsers.
     */
    @Test
    public void testSafeParseLong_String()
    {
        assertEquals(0, Parsers.parseLong(null));
        assertEquals(0, Parsers.parseLong(""));
        assertEquals(0, Parsers.parseLong("a"));
        assertEquals(1, Parsers.parseLong(" 1"));
        assertEquals(1, Parsers.parseLong("1 "));
    }

    /**
     * Test of safeParseDouble method, of class Parsers.
     */
    @Test
    public void testSafeParseDouble_String_double()
    {
        assertEquals(0, Parsers.safeParseDouble(null), 0.001);
        assertEquals(0, Parsers.safeParseDouble(""),   0.001);
        assertEquals(0, Parsers.safeParseDouble("a"),  0.001);
        assertEquals(1000, Parsers.safeParseDouble(" 1.0e3"), 0.001);
        assertEquals(1000, Parsers.safeParseDouble("1.0E3 "), 0.001);
        assertEquals(0.333, Parsers.safeParseDouble(".333"), 0.001);
    }

    /**
     * Test of safeParseDouble method, of class Parsers.
     */
    @Test
    public void testSafeParseDouble_String()
    {
        assertEquals(-1, Parsers.parseDouble(null, -1), 0.001);
        assertEquals(-1, Parsers.parseDouble("", -1),   0.001);
        assertEquals(-1, Parsers.parseDouble("a", -1),  0.001);
        assertEquals(1000, Parsers.parseDouble(" 1.0e3", 0),   0.001);
        assertEquals(1000, Parsers.parseDouble("1.0E3 ", 0),   0.001);
    }
   
    /**
     * Test of safeParseFloat method, of class Parsers.
     */
    @Test
    public void testSafeParseFloat_String_float()
    {
        assertEquals(0.0f, Parsers.parseFloat(null, 0.0f), 0.0);
        assertEquals(1.0f, Parsers.parseFloat(null, 1.0f), 0.0);
        assertEquals(0.0f, Parsers.parseFloat("", 0.0f), 0.0);
        assertEquals(1.0f, Parsers.parseFloat("", 1.0f), 0.0);
        assertEquals(1.234f, Parsers.parseFloat("1.234", 0.0f), 0.000001);
        assertEquals(1.234567f, Parsers.parseFloat("1.234567", 1.0f), 0.000001);
    }

    /**
     * Test of safeParseFloat method, of class Parsers.
     */
    @Test
    public void testSafeParseFloat_String()
    {
        assertEquals(0.0f, Parsers.parseFloat(null), 0.0);
        assertEquals(0.0f, Parsers.parseFloat(""), 0.0);
        assertEquals(1.234f, Parsers.parseFloat("1.234"), 0.000001);
        assertEquals(1.234567f, Parsers.parseFloat("1.234567"), 0.000001);
    }

    /**
     * Test of safeParseLong method, of class Parsers.
     */
    @Test
    public void testSafeParseLong_3args()
    {
        assertEquals(10L, Parsers.parseLong("10", 3L, 10));
        assertEquals(10L, Parsers.parseLong("A", 3L, 16));

        assertEquals(3L, Parsers.parseLong("A", 3L, 10));
    }

    /**
     * Test of safeParseBigInteger method, of class Parsers.
     */
    @Test
    public void testSafeParseBigInteger_3args()
    {
        assertEquals(BigInteger.TEN, Parsers.parseBigInteger("10", BigInteger.ONE, 10));
        assertEquals(BigInteger.TEN, Parsers.parseBigInteger("A", BigInteger.ONE, 16));
        assertEquals(BigInteger.ONE, Parsers.parseBigInteger("nan", BigInteger.ONE, 10));
        assertEquals(BigInteger.ONE, Parsers.parseBigInteger("nan", BigInteger.ONE, 16));
        assertEquals(BigInteger.ONE, Parsers.parseBigInteger(null, BigInteger.ONE, 16));
    }

    /**
     * Test of safeParseBigInteger method, of class Parsers.
     */
    @Test
    public void testSafeParseBigInteger_String_BigInteger()
    {
        assertEquals(BigInteger.TEN, Parsers.parseBigInteger("10", BigInteger.ONE));
        assertEquals(BigInteger.ONE, Parsers.parseBigInteger("aa", BigInteger.ONE));
        assertEquals(BigInteger.ONE, Parsers.parseBigInteger(null, BigInteger.ONE));
    }

    /**
     * Test of safeParseBigInteger method, of class Parsers.
     */
    @Test
    public void testSafeParseBigInteger_String()
    {
        
        assertEquals(BigInteger.TEN, Parsers.parseBigInteger("10"));
        assertEquals(BigInteger.ZERO, Parsers.parseBigInteger("aa"));
        assertEquals(BigInteger.ZERO, Parsers.parseBigInteger(null));
    }

    /**
     * Test of safeParseBigDecimal method, of class Parsers.
     */
    @Test
    public void testSafeParseBigDecimal_String_BigDecimal()
    {
        assertEquals(BigDecimal.ZERO, Parsers.parseBigDecimal("0", BigDecimal.TEN));
        assertEquals(BigDecimal.TEN, Parsers.parseBigDecimal("x", BigDecimal.TEN));
        assertEquals(BigDecimal.TEN, Parsers.parseBigDecimal(null, BigDecimal.TEN));
    }

    /**
     * Test of safeParseBigDecimal method, of class Parsers.
     */
    @Test
    public void testSafeParseBigDecimal_String()
    {
        assertEquals(0,BigDecimal.ZERO.compareTo(Parsers.parseBigDecimal("0")));
        assertEquals(0,BigDecimal.ONE.compareTo(Parsers.parseBigDecimal("1")));
        assertEquals(0,BigDecimal.TEN.compareTo(Parsers.parseBigDecimal("10")));

        assertEquals(0,BigDecimal.ZERO.compareTo(Parsers.parseBigDecimal("0.0")));
        assertEquals(0,BigDecimal.ONE.compareTo(Parsers.parseBigDecimal("1.0")));
        assertEquals(0,BigDecimal.TEN.compareTo(Parsers.parseBigDecimal("10.0")));
        
        assertEquals(0,BigDecimal.ZERO.compareTo(Parsers.parseBigDecimal("nan")));
        assertEquals(0,BigDecimal.ZERO.compareTo(Parsers.parseBigDecimal(null)));
    }

    /**
     * Test of isBigInteger method, of class Parsers.
     */
    @Test
    public void testIsBigInteger_String()
    {
        
        assertFalse(Parsers.isBigInteger(null));
        assertFalse(Parsers.isBigInteger(""));
        assertFalse(Parsers.isBigInteger(" "));
        assertFalse(Parsers.isBigInteger("x"));
        assertFalse(Parsers.isBigInteger("0,0,0"));
        assertFalse(Parsers.isBigInteger("0.0.0"));
        assertFalse(Parsers.isBigInteger("0.0"));
        assertFalse(Parsers.isBigInteger("0."));
        assertFalse(Parsers.isBigInteger(".0"));
        
        assertTrue(Parsers.isBigInteger("0"));
        assertTrue(Parsers.isBigInteger("1"));
        assertTrue(Parsers.isBigInteger("-1"));
    }

    /**
     * Test of isBigInteger method, of class Parsers.
     */
    @Test
    public void testIsBigInteger_String_int()
    {
        assertFalse(Parsers.isBigInteger(null,16));
        assertFalse(Parsers.isBigInteger("",16));
        assertFalse(Parsers.isBigInteger(" ",16));
        assertFalse(Parsers.isBigInteger("x",16));
        assertFalse(Parsers.isBigInteger("0,0,0",16));
        assertFalse(Parsers.isBigInteger("0.0.0",16));
        assertFalse(Parsers.isBigInteger("0.0",16));
        assertFalse(Parsers.isBigInteger("0.",16));
        assertFalse(Parsers.isBigInteger(".0",16));
        assertFalse(Parsers.isBigInteger("1H",16));
        
        assertTrue(Parsers.isBigInteger("0",16));
        assertTrue(Parsers.isBigInteger("1",16));
        assertTrue(Parsers.isBigInteger("-1",16));
        assertTrue(Parsers.isBigInteger("-1A",16));
        assertTrue(Parsers.isBigInteger("-1F",16));
    }

    /**
     * Test of isBigDecimal method, of class Parsers.
     */
    @Test
    public void testIsBigDecimal()
    {
        assertFalse(Parsers.isBigDecimal(null));
        assertFalse(Parsers.isBigDecimal(""));
        assertFalse(Parsers.isBigDecimal(" "));
        assertFalse(Parsers.isBigDecimal("x"));
        assertFalse(Parsers.isBigDecimal("0,0,0"));
        assertFalse(Parsers.isBigDecimal("0.0.0"));
        assertFalse(Parsers.isBigDecimal("0.a"));
        assertFalse(Parsers.isBigDecimal("0.1d"));
        
        assertTrue(Parsers.isBigDecimal("0"));
        assertTrue(Parsers.isBigDecimal("1"));
        assertTrue(Parsers.isBigDecimal("-1"));
        assertTrue(Parsers.isBigDecimal("0.0"));
        assertTrue(Parsers.isBigDecimal("0."));
        assertTrue(Parsers.isBigDecimal(".0"));
    }

    
}
