/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.units;

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
public class SizeUnitsTest
{
    
    public SizeUnitsTest()
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
    
    private static final long k=1024;
    private static final long M=k*k;
    private static final long G=M*k;
    private static final long T=G*k;
    private static final long P=T*k;
    private static final long E=P*k;

    /**
     * Test of getBinaryInstance method, of class SizeUnits.
     */
    @Test
    public void testGetBinaryInstance()
    {
        SizeUnits su = SizeUnits.getBinaryInstance();
        assertEquals("1",  su.toString(1, false));
        assertEquals("1k", su.toString(k, false));
        assertEquals("1M", su.toString(M, false));
        assertEquals("1G", su.toString(G, false));
        assertEquals("1T", su.toString(T, false));
        assertEquals("1P", su.toString(P, false));
        assertEquals("1E", su.toString(E, false));
        
        assertEquals(1,    su.parse("1"));
        assertEquals(k,    su.parse("1k"));
        assertEquals(G,    su.parse("1G"));
        assertEquals(T,    su.parse("1T"));
        assertEquals(P,    su.parse("1P"));
        assertEquals(E,    su.parse("1E"));

        assertEquals(1,    su.parse("1"));
        assertEquals(k,    su.parse("1k"));
        assertEquals(G,    su.parse("1g"));
        assertEquals(T,    su.parse("1t"));
        assertEquals(P,    su.parse("1p"));
        assertEquals(E,    su.parse("1e"));
    }

    /**
     * Test of getBinaryInstance method, of class SizeUnits.
     */
    @Test
    public void testGetBinaryInstance_int_String()
    {
        SizeUnits su = SizeUnits.getBinaryInstance(SizeUnits.BINARY|SizeUnits.LENIENT|SizeUnits.SPACE,"B");
        assertEquals("1 B",  su.toString(1, false));
        assertEquals("1 KiB", su.toString(k, false));
        assertEquals("1 MiB", su.toString(M, false));
        assertEquals("1 GiB", su.toString(G, false));
        assertEquals("1 TiB", su.toString(T, false));
        assertEquals("1 PiB", su.toString(P, false));
        assertEquals("1 EiB", su.toString(E, false));
        
        assertEquals(1,    su.parse("1"));
        assertEquals(k,    su.parse("1 kiB"));
        assertEquals(G,    su.parse("1 GiB"));
        assertEquals(T,    su.parse("1 TiB"));
        assertEquals(P,    su.parse("1 PiB"));
        assertEquals(E,    su.parse("1 EiB"));

        su = SizeUnits.getBinaryInstance(SizeUnits.LENIENT|SizeUnits.SPACE,"");
        assertEquals(1,    su.parse("1"));
        assertEquals(k,    su.parse("1 k"));
        assertEquals(G,    su.parse("1 g"));
        assertEquals(T,    su.parse("1 t"));
        assertEquals(P,    su.parse("1 p"));
        assertEquals(E,    su.parse("1 e"));
        
    }

    /**
     * Test of getStandardInstance method, of class SizeUnits.
     */
    @Test
    public void testToString_double_int()
    {
        //toString(double val, int precision)
        SizeUnits su = SizeUnits.getStandardInstance(SizeUnits.LENIENT|SizeUnits.SPACE,"");
        double m = 2_500_000.0;
        assertEquals("2.5 M", su.toString(m, 3));
        double g = 2_500_000_000.0;
        assertEquals("2.5 G", su.toString(g, 3));
    }

}
