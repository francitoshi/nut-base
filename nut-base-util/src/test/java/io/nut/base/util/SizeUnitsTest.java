/*
 * SizeUnitsTest.java
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
package io.nut.base.util;

import io.nut.base.util.SizeUnits;
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
