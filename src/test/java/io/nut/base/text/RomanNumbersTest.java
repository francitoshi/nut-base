/*
 *  RomanNumbersTest.java
 *
 *  Copyright (C) 2014-2024 francitoshi@gmail.com
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
package io.nut.base.text;

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
public class RomanNumbersTest
{
    
    public RomanNumbersTest()
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
     * Test of isValid method, of class RomanNumbers.
     */
    @Test
    public void testIsValid()
    {
        RomanNumbers instance;
        
        instance = new RomanNumbers(true, true);
        assertFalse(instance.isValid(""));
        assertFalse(instance.isValid("VL"));
        assertFalse(instance.isValid("DCCCC"));
        assertFalse(instance.isValid("CCCCXXXXIIII"));
        
        assertTrue(instance.isValid("XLV"));
        assertTrue(instance.isValid("CCCXXXIII"));
        assertTrue(instance.isValid("CDXLIV"));
                
        for(int i=1;i<4000;i++)
        {
            String s = instance.format(i);
            assertTrue(instance.isValid(s), s);
            assertFalse(instance.isValid(s+"y"), s);
            assertFalse(instance.isValid("y"+s), s);
        }
        assertTrue(instance.isValid("iii"));
        
        instance = new RomanNumbers(false, true);
        assertFalse(instance.isValid("iii"));
    }

    /**
     * Test of parse method, of class RomanNumbers.
     */
    @Test
    public void testParse_String_boolean()
    {
        RomanNumbers instance = new RomanNumbers(true, true);

        for(int i=1;i<4000;i++)
        {
            String s = instance.format(i);
            assertEquals(i, instance.parse(s), ""+i);
            assertEquals(i, instance.parse(s.toLowerCase()), ""+i);
            try
            {
                instance.parse("("+s+")");
                fail("("+s+") should throw an exception");
            }
            catch(NumberFormatException ex)
            {
            }
        }
    }

    /**
     * Test of format method, of class RomanNumbers.
     */
    @Test
    public void testFormat()
    {
        RomanNumbers instance = new RomanNumbers(true, true);
        assertEquals("I", instance.format(1));
        assertEquals("II", instance.format(2));
        assertEquals("III", instance.format(3));
        assertEquals("V", instance.format(5));
        assertEquals("VI", instance.format(6));
        assertEquals("VII", instance.format(7));
        assertEquals("IX", instance.format(9));
        assertEquals("XI", instance.format(11));
        assertEquals("XIII", instance.format(13));
        assertEquals("XV", instance.format(15));
        assertEquals("XVII", instance.format(17));
        assertEquals("XIX", instance.format(19));
        assertEquals("XXIII", instance.format(23));
        assertEquals("XXVII", instance.format(27));
        assertEquals("XXIX", instance.format(29));
        assertEquals("XXXI", instance.format(31));
        assertEquals("XL", instance.format(40));
        assertEquals("XLV", instance.format(45));
        assertEquals("CD", instance.format(400));
        assertEquals("MDCCCLXX", instance.format(1970));
    }

    /**
     * Test of parse method, of class RomanNumbers.
     */
    @Test
    public void testParse_String()
    {
        RomanNumbers instance = new RomanNumbers(true, true);
        assertEquals(4, instance.parse("IIII"));
        assertEquals(23, instance.parse("XXIII"));
    }

    /**
     * Test of parse method, of class RomanNumbers.
     */
    @Test
    public void testParse_String_int()
    {
        RomanNumbers instance = new RomanNumbers(true, true);
        assertEquals(Integer.MIN_VALUE, instance.parse(null,Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, instance.parse("",Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, instance.parse(" ",Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, instance.parse("not a number",Integer.MIN_VALUE));
        assertEquals(23, instance.parse("XXIII"));
    }
    
}
