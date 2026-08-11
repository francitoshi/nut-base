/*
 * BagEqualizerTest.java
 *
 * Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.bag;

import io.nut.base.equalizer.Equalizer;
import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BagEqualizerTest
{

    /**
     * Test of add method, of class Bag.
     */
    @Test
    public void testAdd()
    {
        {
            Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE);

            assertTrue(instance.add("A"));
            assertTrue(instance.add("a"));
            assertTrue(instance.add("B"));
            assertTrue(instance.add("b"));
            
        }
        {
            String A = "A";
            String B = "B";
            String a = "a";
            String b = "b";
            Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE, true);

            assertTrue(instance.add(A));
            assertTrue(instance.add(a));
            assertTrue(instance.add(B));
            assertTrue(instance.add(b));
            
            assertFalse(instance.add(A));
            assertFalse(instance.add(B));
            assertFalse(instance.add(a));
            assertFalse(instance.add(b));
        }
    }

    /**
     * Test of size method, of class Bag.
     */
    @Test
    public void testSize()
    {
        Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE);
        
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
        Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE);

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
        Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE);
        
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
    public void testToArray_2args_1()
    {
        Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE, true);
        
        instance.add("a");
        instance.add("b");
        instance.add("B");
        instance.add("c");
        instance.add("C");
        instance.add("c");
        instance.add("d");
        instance.add("D");
        instance.add("d");
        instance.add("d");

        String[] res = instance.toArray(new String[0]);

        Arrays.sort(res);
        
        String[] exp = {"B", "C", "D", "a", "b", "c", "d"};
        
        assertArrayEquals(exp, res);
    }
    /**
     * Test of toArray method, of class Bag.
     */
    @Test
    public void testToArray_2args_2()
    {
        Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE, true);
        
        instance.add("a");
        instance.add("b");
        instance.add("B");
        instance.add("c");
        instance.add("C");
        instance.add("c");
        instance.add("d");
        instance.add("D");
        instance.add("d");
        instance.add("d");

        Comparator<String[]> cmp = (String[] x, String[] y) -> Integer.compare(x.length, y.length);

        String[][] res = instance.toArray(new String[0][0]);

        Arrays.sort(res, cmp);
        
        String[][] exp = {{"a"}, {"b","B"}, {"c","C"}, {"d","D"}};
        
        assertArrayEquals(exp, res);
        
    }

    /**
     * Test of clear method, of class Bag.
     */
    @Test
    public void testClear()
    {
        Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE);

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
        String A = "A";
        String b = "b";

        {
            Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE);

            assertEquals(0, instance.count(a));        
            instance.add(a);
            assertEquals(1, instance.count(a));        
            instance.add(A);
            assertEquals(2, instance.count(a));        
            instance.add(a);
            assertEquals(3, instance.count(a));        

            assertEquals(0, instance.count(b));        
            instance.add(b);
            assertEquals(1, instance.count(b));        
        }
        {
            Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE, true);

            assertEquals(0, instance.count(a));        
            instance.add(a);
            assertEquals(1, instance.count(a));        
            instance.add(A);
            assertEquals(2, instance.count(a));        
            instance.add(a);
            assertEquals(2, instance.count(a));        

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
        String a = "a";
        String b = "b";
        String A = "A";
        String B = "B";
        
        {
            Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE);

            assertNull(instance.get("a"));

            instance.add(a);
            instance.add(A);
            instance.add(b);
            instance.add(b);
            instance.add("B");
            instance.add("c");

            String[] exp1 = {a,A};
            assertArrayEquals(exp1, instance.get(a));

            String[] exp2 = {b, b, "B"};
            assertArrayEquals(exp2, instance.get(b));
        }
        {        
            Bag<String> instance = Bag.create(Equalizer.STRING_CASE_INSENSITIVE, true);

            assertNull(instance.get(a));

            instance.add(a);
            instance.add(A);
            instance.add(b);
            instance.add(b);
            instance.add(B);
            instance.add("c");

            String[] exp1 = {a,A};
            assertArrayEquals(exp1, instance.get(a));

            String[] exp2 = {b, B};
            assertArrayEquals(exp2, instance.get(b));
        }        
    }
}
