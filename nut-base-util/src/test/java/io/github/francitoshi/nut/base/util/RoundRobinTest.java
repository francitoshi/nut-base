/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
    
    /**
     * Test of getCounter method, of class RoundRobin.
     */
    @Test
    public void testSet()
    {
        Integer[] data = {0,1,2,3,4,5};
        HashSet<Integer> list = new HashSet<>(Arrays.asList(data));
        
        
        RoundRobin<Integer> instance = RoundRobin.create(list);

        //test roundrobin algorithm
        HashSet<Integer> test = new HashSet<>();
        for(int i=0;i<data.length;i++)
        {
            test.add(instance.next());
        }
        
        assertEquals(data.length, test.size());
        
        //test it works with a copy and not the origin
        list.clear();
        test.clear();
        for(int i=0;i<data.length;i++)
        {
            test.add(instance.next());
        }

        assertEquals(data.length, test.size());
    }
    
}
