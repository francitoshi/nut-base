/*
 *  MockConsoleTest.java
 *
 *  Copyright (C) 2015-2025 francitoshi@gmail.com
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
 *
 */
package io.nut.base.io.console;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;

/**
 *
 * @author franci
 */
public class MockConsoleTest
{
    MockConsole getInstance()
    {
        return new MockConsole(System.in, System.out)
        {
            @Override
            public VirtualConsole printf(String format, Object... args)
            {
                return this;
            }
        };
    }
        
    /**
     * Test of readLine method, of class MockConsole.
     */
    @Test
    @Timeout(1)
    public void testReadLine_String_ObjectArr()
    {
        MockConsole instance = new MockConsole(System.in, System.out);
        instance.addLine("1");
        instance.addLine("2");
        instance.addLine("3");
        assertEquals("1", instance.readLine("type 1"));
        assertEquals("2", instance.readLine("type 2"));
        assertEquals("3", instance.readLine("type 3"));
    }

    /**
     * Test of readLine method, of class MockConsole.
     */
    @Test
    @Timeout(1)
    public void testReadLine_0args()
    {
        MockConsole instance = getInstance();
        instance.addLine("1");
        instance.addLine("2");
        instance.addLine("3");
        assertEquals("1", instance.readLine());
        assertEquals("2", instance.readLine());
        assertEquals("3", instance.readLine());
    }

    /**
     * Test of readPassword method, of class MockConsole.
     */
    @Test
    @Timeout(1)
    public void testReadPassword_String_ObjectArr()
    {
        MockConsole instance = getInstance();
        instance.addLine("1");
        instance.addLine("2");
        instance.addLine("3");
        assertArrayEquals("1".toCharArray(), instance.readPassword("type 1"));
        assertArrayEquals("2".toCharArray(), instance.readPassword("type 2"));
        assertArrayEquals("3".toCharArray(), instance.readPassword("type 3"));
    }

    /**
     * Test of readPassword method, of class MockConsole.
     */
    @Test
    @Timeout(1)
    public void testReadPassword_0args()
    {
        MockConsole instance = getInstance();
        instance.addLine("1");
        instance.addLine("2");
        instance.addLine("3");
        assertArrayEquals("1".toCharArray(), instance.readPassword());
        assertArrayEquals("2".toCharArray(), instance.readPassword());
        assertArrayEquals("3".toCharArray(), instance.readPassword());
    }

}
