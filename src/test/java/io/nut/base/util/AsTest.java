/*
 *  AsTest.java
 *
 *  Copyright (c) 2024-2025 francitoshi@gmail.com
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

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class AsTest
{
    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_2args_1()
    {
        String[] keys = "1,2,3,4,5".split(",");
        Integer[] values = {1,2,3,4,5};

        Map<String,Integer> map = As.map(keys, values);
        for(int i=0;i<keys.length;i++)
        {
            assertEquals(values[i], map.getOrDefault(keys[i],null));
        }
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_2args_2()
    {
        Map<String,Integer> map = As.map("1", 1);
        assertEquals(1, map.size());
        assertEquals(1, map.getOrDefault("1",null));
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_4args()
    {
        Map<String,Integer> map = As.map("1", 1, "2", 2);
        assertEquals(2, map.size());
        assertEquals(1, map.getOrDefault("1",null));
        assertEquals(2, map.getOrDefault("2",null));
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_6args()
    {
        Map<String,Integer> map = As.map("1", 1, "2", 2, "3", 3);
        assertEquals(3, map.size());
        assertEquals(1, map.getOrDefault("1",null));
        assertEquals(2, map.getOrDefault("2",null));
        assertEquals(3, map.getOrDefault("3",null));
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_8args()
    {
        Map<String,Integer> map = As.map("1", 1, "2", 2, "3", 3, "4", 4);
        assertEquals(4, map.size());
        assertEquals(1, map.getOrDefault("1",null));
        assertEquals(2, map.getOrDefault("2",null));
        assertEquals(3, map.getOrDefault("3",null));
        assertEquals(4, map.getOrDefault("4",null));
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_10args()
    {
        Map<String,Integer> map = As.map("1", 1, "2", 2, "3", 3, "4", 4, "5", 5);
        assertEquals(5, map.size());
        assertEquals(1, map.getOrDefault("1",null));
        assertEquals(2, map.getOrDefault("2",null));
        assertEquals(3, map.getOrDefault("3",null));
        assertEquals(4, map.getOrDefault("4",null));
        assertEquals(5, map.getOrDefault("5",null));
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_12args()
    {
        Map<String,Integer> map = As.map("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6);
        assertEquals(6, map.size());
        assertEquals(1, map.getOrDefault("1",null));
        assertEquals(2, map.getOrDefault("2",null));
        assertEquals(3, map.getOrDefault("3",null));
        assertEquals(4, map.getOrDefault("4",null));
        assertEquals(5, map.getOrDefault("5",null));
        assertEquals(6, map.getOrDefault("6",null));
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_14args()
    {
        Map<String,Integer> map = As.map("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7);
        assertEquals(7, map.size());
        assertEquals(1, map.getOrDefault("1",null));
        assertEquals(2, map.getOrDefault("2",null));
        assertEquals(3, map.getOrDefault("3",null));
        assertEquals(4, map.getOrDefault("4",null));
        assertEquals(5, map.getOrDefault("5",null));
        assertEquals(6, map.getOrDefault("6",null));
        assertEquals(7, map.getOrDefault("7",null));
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_16args()
    {
        Map<String,Integer> map = As.map("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7, "8", 8);
        assertEquals(8, map.size());
        assertEquals(1, map.getOrDefault("1",null));
        assertEquals(2, map.getOrDefault("2",null));
        assertEquals(3, map.getOrDefault("3",null));
        assertEquals(4, map.getOrDefault("4",null));
        assertEquals(5, map.getOrDefault("5",null));
        assertEquals(6, map.getOrDefault("6",null));
        assertEquals(7, map.getOrDefault("7",null));
        assertEquals(8, map.getOrDefault("8",null));
    }

    /**
     * Test of map method, of class As.
     */
    @Test
    public void testMap_18args()
    {
        Map<String,Integer> map = As.map("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7, "8", 8, "9", 9);
        assertEquals(9, map.size());
        assertEquals(1, map.getOrDefault("1",null));
        assertEquals(2, map.getOrDefault("2",null));
        assertEquals(3, map.getOrDefault("3",null));
        assertEquals(4, map.getOrDefault("4",null));
        assertEquals(5, map.getOrDefault("5",null));
        assertEquals(6, map.getOrDefault("6",null));
        assertEquals(7, map.getOrDefault("7",null));
        assertEquals(8, map.getOrDefault("8",null));
        assertEquals(9, map.getOrDefault("9",null));
    }

    /**
     * Test of list method, of class As.
     */
    @Test
    public void testList()
    {
        List<Integer> r1 = As.list(0, 1, 2, 3, 4);
        List<String> r2 = As.list("0", "1", "2", "3", "4");
        
        for(int i=0;i<5;i++)
        {
            assertEquals(i, r1.get(i));
            assertEquals(""+i, r2.get(i));
        }
    }

    /**
     * Test of queue method, of class As.
     */
    @Test
    public void testQueue()
    {
        Queue<Integer> r1 = As.queue(0, 1, 2, 3, 4);
        Queue<String> r2 = As.queue("0", "1", "2", "3", "4");
        
        for(int i=0;i<5;i++)
        {
            assertEquals(i, r1.remove());
            assertEquals(""+i, r2.remove());
        }
    }

    /**
     * Test of deque method, of class As.
     */
    @Test
    public void testDeque()
    {
        Deque<Integer> r1 = As.deque(0, 1, 2, 3, 4);
        Deque<String> r2 = As.deque("0", "1", "2", "3", "4");
        
        for(int i=0;i<5;i++)
        {
            assertEquals(i, r1.removeFirst());
            assertEquals(""+i, r2.removeFirst());
        }
    }

    /**
     * Test of blockingQueue method, of class As.
     */
    @Test
    public void testBlockingQueue()
    {
        BlockingQueue<Integer> r1 = As.blockingQueue(0, 1, 2, 3, 4);
        BlockingQueue<String> r2 = As.blockingQueue("0", "1", "2", "3", "4");
        
        for(int i=0;i<5;i++)
        {
            assertEquals(i, r1.remove());
            assertEquals(""+i, r2.remove());
        }
    }

    /**
     * Test of blockingDeque method, of class As.
     */
    @Test
    public void testBlockingDeque()
    {
        BlockingDeque<Integer> r1 = As.blockingDeque(0, 1, 2, 3, 4);
        BlockingDeque<String> r2 = As.blockingDeque("0", "1", "2", "3", "4");
        
        for(int i=0;i<5;i++)
        {
            assertEquals(i, r1.removeFirst());
            assertEquals(""+i, r2.removeFirst());
        }
    }

    /**
     * Test of set method, of class As.
     */
    @Test
    public void testSet()
    {
        Set<Integer> result = As.set(0,1,2,3,4);
        for(int i=0;i<5;i++)
        {
            assertTrue(result.contains(i));
        }
    }
    
}
