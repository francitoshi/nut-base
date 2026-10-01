/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

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
