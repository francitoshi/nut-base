/*
 *  RoundRobinTest.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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

import java.util.ArrayList;
import java.util.Arrays;
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
public class RoundRobinTest
{
    
    public RoundRobinTest()
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

    /**
     * Test of next method, of class RoundRobin.
     */
    @Test
    public void testArray()
    {
        Integer[] list = {0,1,2,3,4,5};
        
        RoundRobin<Integer> instance = RoundRobin.create(list);

        //test roundrobin algorithm
        for(int i=0;i<list.length*2;i++)
        {
            assertEquals(i%list.length, instance.next());
        }

        //test it works with a copy and not the origin
        for(int i=0;i<list.length;i++)
        {
            list[i] = -1;
            assertEquals(i%list.length, instance.next());
        }
    }

    /**
     * Test of getCounter method, of class RoundRobin.
     */
    @Test
    public void testList()
    {
        Integer[] data = {0,1,2,3,4,5};
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(data));
        
        RoundRobin<Integer> instance = RoundRobin.create(list);

        //test roundrobin algorithm
        for(int i=0;i<data.length*2;i++)
        {
            assertEquals(i%data.length, instance.next());
        }
        
        //test it works with a copy and not the origin
        list.clear();
        for(int i=0;i<data.length;i++)
        {
            assertEquals(i%data.length, instance.next());
        }
    }
    
}
