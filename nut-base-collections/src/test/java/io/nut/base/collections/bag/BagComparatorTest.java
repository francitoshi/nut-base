/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.bag;

import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author franci
 */
public class BagComparatorTest
{

    @BeforeAll
    public static void setUpClass() throws Exception
    {
    }

    @AfterAll
    public static void tearDownClass() throws Exception
    {
    }

    @BeforeEach
    public void setUp() throws Exception
    {
    }

    @AfterEach
    public void tearDown() throws Exception
    {
    }
    static final Comparator<String> CMP = Comparator.reverseOrder();
    /**
     * Test of add method, of class Bag.
     */
    @Test
    public void testAdd()
    {
        {
            Bag<String> instance = Bag.create(CMP);

            assertTrue(instance.add("a"));
            assertTrue(instance.add("a"));
            assertTrue(instance.add("a"));
        }
        {
            String a = "a";
            String b = "b";
            Bag<String> instance = Bag.create(CMP, true);

            assertTrue(instance.add(a));
            assertFalse(instance.add(a));
            assertTrue(instance.add(b));
        }        
    }

    /**
     * Test of size method, of class Bag.
     */
    @Test
    public void testSize()
    {
        Bag<String> instance = Bag.create(CMP);
        
        assertEquals(0, instance.size());
        instance.add("a");
        assertEquals(1, instance.size());
        instance.add("a");
        assertEquals(1, instance.size());
        instance.add("b");
        assertEquals(2, instance.size());
    }

    /**
     * Test of isEmpty method, of class Bag.
     */
    @Test
    public void testIsEmpty()
    {
        Bag<String> instance = Bag.create(CMP);

        assertTrue(instance.isEmpty());

        instance.add("a");

        assertFalse(instance.isEmpty());
    }

    /**
     * Test of toArray method, of class Bag.
     */
    @Test
    public void testToArray_1args_1()
    {
        Bag<String> instance = Bag.create(CMP);
        
        instance.add("a");
        instance.add("b");
        instance.add("b");
        instance.add("c");
        instance.add("c");
        instance.add("c");
        instance.add("d");
        instance.add("d");
        instance.add("d");
        instance.add("d");

        String[] res = instance.toArray(new String[0]);

        Arrays.sort(res);
        
        String[] exp = {"a", "b","b", "c","c","c", "d","d","d","d"};
        
        assertArrayEquals(exp, res);
    }

    /**
     * Test of toArray method, of class Bag.
     */
    @Test
    public void testToArray_1args_2()
    {
        Bag<String> instance = Bag.create(CMP);
        
        instance.add("a");
        instance.add("b");
        instance.add("b");
        instance.add("c");
        instance.add("c");
        instance.add("c");
        instance.add("d");
        instance.add("d");
        instance.add("d");
        instance.add("d");

        Comparator<String[]> cmp = (String[] x, String[] y) -> Integer.compare(x.length, y.length);

        String[][] res = instance.toArray(new String[0][0]);
        Arrays.sort(res, cmp);
        
        String[][] exp = {{"a"}, {"b","b"}, {"c","c","c"}, {"d","d","d","d"}};
        
        assertArrayEquals(exp, res);
    }

    /**
     * Test of clear method, of class Bag.
     */
    @Test
    public void testClear()
    {
        Bag<String> instance = Bag.create(CMP);

        instance.add("a");
        instance.clear();        
        assertEquals(0, instance.size());
        assertEquals(0, instance.toArray(new String[0]).length);

        instance.add("a");
        assertEquals(1, instance.size());
        assertEquals(1, instance.toArray(new String[0]).length);
    }

    /**
     * Test of count method, of class BagBase.
     */
    @Test
    public void testCount()
    {
        String a = "a";
        String b = "b";

        {
            Bag<String> instance = Bag.create(CMP);

            assertEquals(0, instance.count(a));        
            instance.add(a);
            assertEquals(1, instance.count(a));        
            instance.add(a);
            assertEquals(2, instance.count(a));        

            assertEquals(0, instance.count(b));        
            instance.add(b);
            assertEquals(1, instance.count(b));        
        }
        {
            Bag<String> instance = Bag.create(CMP, true);

            assertEquals(0, instance.count(a));        
            instance.add(a);
            assertEquals(1, instance.count(a));        
            instance.add(a);
            assertEquals(1, instance.count(a));        

            assertEquals(0, instance.count(b));        
            instance.add(b);
            assertEquals(1, instance.count(b));        
        }        
    }

    /**
     * Test of get method, of class BagBase.
     */
    @Test
    public void testGet()
    {
        {
            Bag<String> instance = Bag.create(CMP);

            assertNull(instance.get("a"));

            instance.add("a");
            instance.add("b");
            instance.add("b");
            instance.add("c");

            String[] exp1 = {"a"};
            assertArrayEquals(exp1, instance.get("a"));

            String[] exp2 = {"b", "b"};
            assertArrayEquals(exp2, instance.get("b"));
        }
        {        
            Bag<String> instance = Bag.create(CMP, true);

            assertNull(instance.get("a"));

            instance.add("a");
            instance.add("b");
            instance.add("b");
            instance.add("c");

            String[] exp1 = {"a"};
            assertArrayEquals(exp1, instance.get("a"));

            String[] exp2 = {"b"};
            assertArrayEquals(exp2, instance.get("b"));
        }        
    }
}
