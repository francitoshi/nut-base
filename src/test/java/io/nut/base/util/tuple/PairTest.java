/*
 *  PairTest.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.util.tuple;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class PairTest
{
    /**
     * Test of getKey method, of class Pair.
     */
    @Test
    public void testGet()
    {
        Pair<Integer,String> instance = new Pair<>(1, "1");
        assertEquals(1, instance.getKey());
        assertEquals(1, instance.get1st());
        assertEquals(1, instance.getLeft());

        assertEquals("1", instance.getVal());
        assertEquals("1", instance.get2nd());
        assertEquals("1", instance.getRight());
    }
    
    /**
     * Test of equals method, of class Pair.
     */
    @Test
    public void testEquals()
    {
        Pair<String, String> p11 = new Pair<>("a", null);
        Pair<String, String> p12 = new Pair<>("a", null);

        Pair<String, String> p21 = new Pair<>(null, "a");
        Pair<String, String> p22 = new Pair<>(null, "a");
        
        Pair<String, String> p31 = new Pair<>("a", "b");
        Pair<String, String> p32 = new Pair<>("a", "b");

        Pair<String, String> p44 = new Pair<>("A", "B");
        
        assertTrue(p11.equals(p12));
        assertTrue(p21.equals(p22));
        assertTrue(p31.equals(p32));
        
        assertFalse(p11.equals(p21));
        assertFalse(p12.equals(p22));
        
        assertFalse(p44.equals(p11));
        assertFalse(p44.equals(p21));
        assertFalse(p44.equals(p31));
        
    }

    /**
     * Test of getKey method, of class Pair.
     */
    @Test
    public void testGetKey_Pair()
    {
        Pair<String, String> pair0 = new Pair<>(null, "B");
        Pair<String, String> pair1 = new Pair<>("A", "B");
        
        assertNull(pair0.getKey());
        assertEquals("A", pair1.getKey());
}

    /**
     * Test of getVal method, of class Pair.
     */
    @Test
    public void testGetVal_Pair()
    {
        Pair<String, String> pair0 = new Pair<>("A", null);
        Pair<String, String> pair1 = new Pair<>("A", "B");
        
        assertNull(pair0.getVal());
        assertEquals("B", pair1.getVal());
    }
    
}
