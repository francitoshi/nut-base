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
public class BagBaseTest
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
    /**
     * Test of add method, of class Bag.
     */
    @Test
    public void testAdd()
    {
        {
            Bag<String> instance = Bag.create();

            assertTrue(instance.add("a"));
            assertTrue(instance.add("a"));
            assertTrue(instance.add("a"));
        }
        {
            String a = "a";
            String b = "b";
            Bag<String> instance = Bag.create(true);

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
        Bag<String> instance = Bag.create();
        
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
        Bag<String> instance = Bag.create();

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
        Bag<String> instance = Bag.create();
        
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
        Bag<String> instance = Bag.create();
        
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
        Bag<String> instance = Bag.create();

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
            Bag<String> instance = Bag.create();

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
            Bag<String> instance = Bag.create(true);

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
            Bag<String> instance = Bag.create();

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
            Bag<String> instance = Bag.create(true);

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

    static class Base {
        final String name;
        Base(String name) { this.name = name; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Base base = (Base) o;
            return name != null ? name.equals(base.name) : base.name == null;
        }
        @Override
        public int hashCode() { return name != null ? name.hashCode() : 0; }
    }
    static class Sub1 extends Base {
        Sub1(String name) { super(name); }
    }
    static class Sub2 extends Base {
        Sub2(String name) { super(name); }
    }

    @Test
    public void testHeterogeneousSubclasses()
    {
        Bag<Base> bag = Bag.create();
        Sub1 s1 = new Sub1("one");
        Sub2 s2 = new Sub2("two");

        bag.add(s1);
        bag.add(s2);

        Base[] res1 = bag.get(s1);
        assertNotNull(res1);
        assertEquals(1, res1.length);
        assertTrue(res1[0] instanceof Sub1);

        Base[] res2 = bag.get(s2);
        assertNotNull(res2);
        assertEquals(1, res2.length);
        assertTrue(res2[0] instanceof Sub2);

        Base[][] res2d = bag.toArray(new Base[0][0]);
        assertNotNull(res2d);
        assertEquals(2, res2d.length);
    }
}
