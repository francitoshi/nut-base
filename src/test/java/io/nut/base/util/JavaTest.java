/*
 *  Java.java
 *
 *  Copyright (c) 2024-2026 francitoshi@gmail.com
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
public class JavaTest
{
    
    public JavaTest()
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
        assertEquals(8, Java.JAVA_INT_VERSION);
    }
    
    @Test
    public void testBitsConstants()
    {
        assertEquals(8, Java.BYTE_BITS);
        assertEquals(16, Java.SHORT_BITS);
        assertEquals(16, Java.CHAR_BITS);
        assertEquals(32, Java.INT_BITS);
        assertEquals(64, Java.LONG_BITS);
        assertEquals(32, Java.FLOAT_BITS);
        assertEquals(64, Java.DOUBLE_BITS);

    }
    
}
