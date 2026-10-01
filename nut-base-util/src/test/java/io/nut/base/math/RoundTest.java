/*
 *  RoundTest.java
 *
 *  Copyright (C) 2013-2024 francitoshi@gmail.com
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
package io.nut.base.math;

import java.math.BigDecimal;
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
public class RoundTest
{
    
    public RoundTest()
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
     * Test of getHalfUpInstance method, of class Round.
     */
    @Test
    public void testGetHalfUpInstance()
    {
        Round result = Round.getHalfUpInstance(2);
        assertNotNull(result);
    }

    /**
     * Test of getHalfDownInstance method, of class Round.
     */
    @Test
    public void testGetHalfDownInstance()
    {
        Round result = Round.getHalfDownInstance(2);
        assertNotNull(result);
    }

    /**
     * Test of getHalfEvenInstance method, of class Round.
     */
    @Test
    public void testGetHalfEvenInstance()
    {
        Round result = Round.getHalfEvenInstance(2);
        assertNotNull(result);
    }

    /**
     * Test of round method, of class Round.
     */
    @Test
    public void testRound_double()
    {
        final int decimals = 2;
        Round up = Round.getHalfUpInstance(decimals);
        Round down = Round.getHalfDownInstance(decimals);
        Round even = Round.getHalfEvenInstance(decimals);
        
        assertEquals(true, Double.isNaN(up.round(Double.NaN)));
        assertEquals(true, Double.isInfinite(up.round(Double.POSITIVE_INFINITY)));
        
        assertEquals(123.12d, up.round(123.124999999d), 0.000001d);
        assertEquals(123.13d, up.round(123.125000000d), 0.000001d);
        assertEquals(123.13d, up.round(123.125000001d), 0.000001d);
        
        assertEquals(123.12d, down.round(123.124999999d), 0.000001d);
        assertEquals(123.12d, down.round(123.125000000d), 0.000001d);
        assertEquals(123.13d, down.round(123.125000001d), 0.000001d);
                
        assertEquals(123.12d, even.round(123.124999999d), 0.000001d);
        assertEquals(123.12d, even.round(123.125000000d), 0.000001d);
        assertEquals(123.14d, even.round(123.135000000d), 0.000001d);
        assertEquals(123.13d, even.round(123.125000001d), 0.000001d);
    }
    /**
     * Test of round method, of class Round.
     */
    @Test
    public void testRound_BigDecimal()
    {
        final int decimals = 2;
        Round up = Round.getHalfUpInstance(decimals);
        Round down = Round.getHalfDownInstance(decimals);
        Round even = Round.getHalfEvenInstance(decimals);
        
        assertEquals(new BigDecimal("123.12"), up.round(new BigDecimal("123.124999999")));
        assertEquals(new BigDecimal("123.13"), up.round(new BigDecimal("123.125000000")));
        assertEquals(new BigDecimal("123.13"), up.round(new BigDecimal("123.125000001")));
        
        assertEquals(new BigDecimal("123.12"), down.round(new BigDecimal("123.124999999")));
        assertEquals(new BigDecimal("123.12"), down.round(new BigDecimal("123.125000000")));
        assertEquals(new BigDecimal("123.13"), down.round(new BigDecimal("123.125000001")));
                
        assertEquals(new BigDecimal("123.12"), even.round(new BigDecimal("123.124999999")));
        assertEquals(new BigDecimal("123.12"), even.round(new BigDecimal("123.125000000")));
        assertEquals(new BigDecimal("123.14"), even.round(new BigDecimal("123.135000000")));
        assertEquals(new BigDecimal("123.13"), even.round(new BigDecimal("123.125000001")));
    }

    /**
     * Test of round method, of class Round.
     */
    @Test
    public void testRound_float()
    {
        final int decimals = 2;
        
        Round up = Round.getHalfUpInstance(decimals);
        Round down = Round.getHalfDownInstance(decimals);
        Round even = Round.getHalfEvenInstance(decimals);
        
        assertEquals(true, Float.isNaN(up.round(Float.NaN)));
        assertEquals(true, Float.isInfinite(up.round(Float.POSITIVE_INFINITY)));
        
        assertEquals(123.12f, up.round(123.1249f), 0.000001d);
        assertEquals(123.13f, up.round(123.1250f), 0.000001d);
        assertEquals(123.13f, up.round(123.1251f), 0.000001d);
        
        assertEquals(123.12f, down.round(123.1249f), 0.000001d);
        assertEquals(123.12f, down.round(123.1250f), 0.000001d);
        assertEquals(123.13f, down.round(123.1251f), 0.000001d);
                
        assertEquals(123.12f, even.round(123.1249f), 0.000001d);
        assertEquals(123.12f, even.round(123.1250f), 0.000001d);
        assertEquals(123.14f, even.round(123.1350f), 0.000001d);
        assertEquals(123.13f, even.round(123.1251f), 0.000001d);
    }

    /**
     * Test of toString method, of class Round.
     */
    @Test
    public void testToString_double_int()
    {
        Round r1 = Round.getHalfDownInstance(6);
        assertEquals("5.50", r1.toString(5.5, 2));
        assertEquals("5.55", r1.toString(5.55, 2));
        assertEquals("5.555", r1.toString(5.555, 2));
        
        r1 = Round.getHalfUpInstance(6);
        assertEquals("5.50", r1.toString(5.5, 2));
        assertEquals("5.55", r1.toString(5.55, 2));
        assertEquals("5.555", r1.toString(5.555, 2));
    }

    /**
     * Test of toString method, of class Round.
     */
    @Test
    public void testToString_double()
    {
        Round r1 = Round.getHalfDownInstance(2);
        assertEquals("5.50", r1.toString(5.5));
        assertEquals("5.55", r1.toString(5.55));
        assertEquals("5.55", r1.toString(5.555));
        
        r1 = Round.getHalfUpInstance(2);
        assertEquals("5.50", r1.toString(5.5));
        assertEquals("5.55", r1.toString(5.55));
        assertEquals("5.56", r1.toString(5.555));
    }

    /**
     * Test of toString method, of class Round.
     */
    @Test
    public void testToString_float_int()
    {
        Round r1 = Round.getHalfDownInstance(6);
        assertEquals("5.50", r1.toString(5.5f, 2));
        assertEquals("5.55", r1.toString(5.55f, 2));
        assertEquals("5.555", r1.toString(5.555f, 2));
        
        r1 = Round.getHalfUpInstance(6);
        assertEquals("5.50", r1.toString(5.5f, 2));
        assertEquals("5.55", r1.toString(5.55f, 2));
        assertEquals("5.5555", r1.toString(5.5555f, 2));
    }

    /**
     * Test of toString method, of class Round.
     */
    @Test
    public void testToString_float()
    {
        Round r1 = Round.getHalfDownInstance(2);
        assertEquals("5.50", r1.toString(5.5f));
        assertEquals("5.55", r1.toString(5.55f));
        assertEquals("5.55", r1.toString(5.555f));
        
        r1 = Round.getHalfUpInstance(2);
        assertEquals("5.50", r1.toString(5.5f));
        assertEquals("5.55", r1.toString(5.55f));
        assertEquals("5.56", r1.toString(5.5555f));
    }

    /**
     * Test of getUpInstance method, of class Round.
     */
    @Test
    public void testGetUpInstance()
    {
        Round round = Round.getUpInstance(0);
        assertEquals(2.0, round.round(1.25), 0.0);
        assertEquals(-2.0, round.round(-1.25), 0.0);
    }

    /**
     * Test of getDownInstance method, of class Round.
     */
    @Test
    public void testGetDownInstance()
    {
        Round round = Round.getDownInstance(0);
        assertEquals(1.0, round.round(1.75), 0.0);
        assertEquals(-1.0, round.round(-1.75), 0.0);
    }

    /**
     * Test of getCeilingInstance method, of class Round.
     */
    @Test
    public void testGetCeilingInstance()
    {
        Round round = Round.getCeilingInstance(0);
        assertEquals(2.0, round.round(1.25), 0.0);
        assertEquals(-1.0, round.round(-1.75), 0.0);
    }

    /**
     * Test of round method, of class Round.
     */
    @Test
    public void testRound_doubleArr()
    {
        double[] srcNull = null;
        double[] src = {1.111,2.222,3.333};
        double[] exp = {1.11, 2.22, 3.33};
        Round round = Round.getHalfUpInstance(2);
        assertNull(round.round(srcNull));
        assertArrayEquals(exp, round.round(src), 0.0);
    }

    /**
     * Test of round method, of class Round.
     */
    @Test
    public void testRound_floatArr()
    {
        float[] srcNull = null;
        float[] src = {1.111f,2.222f,3.333f};
        float[] exp = {1.11f, 2.22f, 3.33f};
        Round round = Round.getHalfUpInstance(2);
        assertNull(round.round(srcNull));
        assertArrayEquals(exp, round.round(src), 0.0f);
    }

    /**
     * Test of round method, of class Round.
     */
    @Test
    public void testRound_double_double()
    {
        {
            Round instance = Round.getCeilingInstance(2);

            assertEquals(0.01, instance.round(0.009, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.010, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.011, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.019, 0.01), 0.000001);

            assertEquals(0.05, instance.round(0.049, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.050, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.051, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.099, 0.05), 0.000001);

            assertEquals(0.25, instance.round(0.249, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.250, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.251, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.499, 0.25), 0.000001);
        }
        {
            Round instance = Round.getFloorInstance(2);

            assertEquals(0.00, instance.round(0.009, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.010, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.011, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.019, 0.01), 0.000001);

            assertEquals(0.00, instance.round(0.049, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.050, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.051, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.099, 0.05), 0.000001);

            assertEquals(0.00, instance.round(0.249, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.250, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.251, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.499, 0.25), 0.000001);
        }
        {
            Round instance = Round.getUpInstance(2);

            assertEquals(0.01, instance.round(0.009, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.010, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.011, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.019, 0.01), 0.000001);

            assertEquals(0.05, instance.round(0.049, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.050, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.051, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.099, 0.05), 0.000001);

            assertEquals(0.25, instance.round(0.249, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.250, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.251, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.499, 0.25), 0.000001);
        }
        {
            Round instance = Round.getDownInstance(2);

            assertEquals(0.00, instance.round(0.009, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.010, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.015, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.019, 0.01), 0.000001);

            assertEquals(0.00, instance.round(-0.009, 0.01), 0.000001);
            assertEquals(-0.01, instance.round(-0.010, 0.01), 0.000001);
            assertEquals(-0.01, instance.round(-0.011, 0.01), 0.000001);
            assertEquals(-0.01, instance.round(-0.019, 0.01), 0.000001);

            assertEquals(0.00, instance.round(0.049, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.050, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.051, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.099, 0.05), 0.000001);

            assertEquals(-0.00, instance.round(-0.049, 0.05), 0.000001);
            assertEquals(-0.05, instance.round(-0.050, 0.05), 0.000001);
            assertEquals(-0.05, instance.round(-0.051, 0.05), 0.000001);
            assertEquals(-0.05, instance.round(-0.099, 0.05), 0.000001);

            assertEquals(0.00, instance.round(0.249, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.250, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.251, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.499, 0.25), 0.000001);

            assertEquals(0.00, instance.round(-0.249, 0.25), 0.000001);
            assertEquals(-0.25, instance.round(-0.250, 0.25), 0.000001);
            assertEquals(-0.25, instance.round(-0.251, 0.25), 0.000001);
            assertEquals(-0.25, instance.round(-0.499, 0.25), 0.000001);
        }
        {
            Round instance = Round.getHalfUpInstance(2);

            assertEquals(0.01, instance.round(0.009, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.010, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.015, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.019, 0.01), 0.000001);

            assertEquals(0.05, instance.round(0.049, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.050, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.074, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.075, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.076, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.099, 0.05), 0.000001);

            assertEquals(0.25, instance.round(0.249, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.250, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.374, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.375, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.376, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.499, 0.25), 0.000001);
        }
        {
            Round instance = Round.getHalfDownInstance(2);

            assertEquals(0.01, instance.round(0.009, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.010, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.015, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.019, 0.01), 0.000001);

            assertEquals(0.05, instance.round(0.049, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.050, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.074, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.075, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.076, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.099, 0.05), 0.000001);

            assertEquals(0.25, instance.round(0.249, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.250, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.374, 0.25), 0.000001);
            assertEquals(0.25, instance.round(0.375, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.376, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.499, 0.25), 0.000001);
        }
        {
            Round instance = Round.getHalfEvenInstance(2);

            assertEquals(0.01, instance.round(0.009, 0.01), 0.000001);
            assertEquals(0.01, instance.round(0.010, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.015, 0.01), 0.000001);
            assertEquals(0.02, instance.round(0.025, 0.01), 0.000001);
            assertEquals(0.04, instance.round(0.035, 0.01), 0.000001);
            assertEquals(0.04, instance.round(0.036, 0.01), 0.000001);

            assertEquals(0.05, instance.round(0.049, 0.05), 0.000001);
            assertEquals(0.05, instance.round(0.050, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.075, 0.05), 0.000001);
            assertEquals(0.10, instance.round(0.125, 0.05), 0.000001);
            assertEquals(0.20, instance.round(0.225, 0.05), 0.000001);
            assertEquals(0.25, instance.round(0.226, 0.05), 0.000001);

            assertEquals(0.00, instance.round(0.124, 0.25), 0.000001);
            assertEquals(0.00, instance.round(0.125, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.375, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.500, 0.25), 0.000001);
            assertEquals(0.50, instance.round(0.625, 0.25), 0.000001);
            assertEquals(0.75, instance.round(0.626, 0.25), 0.000001);
        }
        
    }
}
