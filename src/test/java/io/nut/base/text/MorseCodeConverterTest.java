/*
 * MorseCodeConverterTest.java
 *
 * Copyright (c) 2013-2024 francitoshi@gmail.com
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
public class MorseCodeConverterTest
{
    
    public MorseCodeConverterTest()
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
        long[] e = new long[]{0, 100, 0};
        long[] ee = new long[]{0, 100, 300, 100, 0};
        long[] sos= new long[]{ 0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 0 };
        long[] hw = new long[]{ 0, 100, 100, 100, 100, 100, 100, 100, 300, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 700, 100, 100, 300, 100, 300, 300, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 0 };
        long[] v  = new long[]{ 0, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 0 };
        long[] abc= new long[]{ 0, 100, 100, 300, 300, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 300, 100, 100, 700, 300, 100, 100, 100, 100, 100, 300, 300, 300, 100, 100, 100, 300, 100, 300, 300, 300, 100, 300, 100, 100, 100, 100, 700, 100, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 700, 300, 100, 300, 300, 300, 100, 100, 700, 300, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 100, 100, 300, 100, 100, 100, 300, 100, 100, 100, 300, 0 };
                
        
        assertArrayEquals(e, MorseCodeConverter.pattern("e"));
        assertArrayEquals(ee, MorseCodeConverter.pattern("EE"));
        assertArrayEquals(sos, MorseCodeConverter.pattern("SOS"));
        assertArrayEquals(hw, MorseCodeConverter.pattern("hello world"));
        assertArrayEquals(v, MorseCodeConverter.pattern("aeiou"));
        assertArrayEquals(abc, MorseCodeConverter.pattern("abc xyz pq mn 0123456789."));
    }
}
