/*
 *  CounterSetTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.collections;

import java.util.Collection;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class CounterSetTest
{
    
    @Test
    public void testCount()
    {
        CounterSet<String> instance = new CounterSet<>();
        instance.add("A");
        instance.add("A");
        instance.add("B",2);
        
        assertEquals(2, instance.count("A"));
        assertEquals(2, instance.count("B"));
    }

    @Test
    public void testSize()
    {
        CounterSet<String> instance = new CounterSet<>();
        instance.add("A");
        instance.add("A");
        instance.add("B",2);
        
        assertEquals(2, instance.size());
    }

    @Test
    public void testIsEmpty()
    {
        CounterSet<String> instance = new CounterSet<>();

        assertTrue(instance.isEmpty());

        instance.add("A");
        instance.add("A");
        instance.add("B",2);
        
        assertFalse(instance.isEmpty());
    }

    @Test
    public void testContains()
    {
        CounterSet<String> instance = new CounterSet<>();

        assertFalse(instance.contains("A"));

        instance.add("A");
        instance.add("A");
        instance.add("B",2);
        
        assertTrue(instance.contains("A"));
        assertTrue(instance.contains("B"));

    }

    @Test
    public void testIterator()
    {
        CounterSet<String> instance = new CounterSet<>();

        int count = 0;
        for(String item : instance)
        {
            count++;
        }

        assertEquals(0, count);

        instance.add("A");
        instance.add("A");
        instance.add("B",2);

        for(String item : instance)
        {
            count++;
        }
        
        assertEquals(2, count);
    }

    @Test
    public void testToArray_0args()
    {
        CounterSet<String> instance = new CounterSet<>();

        int count = 0;

        Object[] result = instance.toArray();

        assertEquals(0, result.length);

        instance.add("A");
        instance.add("A");
        instance.add("B",2);

        result = instance.toArray();

        assertEquals(2, result.length);
    }

    @Test
    public void testToArray_GenericType()
    {
        CounterSet<String> instance = new CounterSet<>();

        int count = 0;

        String[] result = instance.toArray(new String[0]);

        assertEquals(0, result.length);

        instance.add("A");
        instance.add("A");
        instance.add("B",2);

        result = instance.toArray(new String[0]);

        assertEquals(2, result.length);
    }

    @Test
    public void testToArray_GenericType_intArr()
    {
        CounterSet<String> instance = new CounterSet<>();

        int count = 0;

        String[] result = instance.toArray(new String[0], new int[0]);

        assertEquals(0, result.length);

        instance.add("A");
        instance.add("A");
        instance.add("B",2);

        result = instance.toArray(new String[2], new int[2]);

        assertEquals(2, result.length);
    }
    
}
