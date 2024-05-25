/*
 * SortsTest.java
 *
 * Copyright (c) 2012-2023 francitoshi@gmail.com
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
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
public class SortsTest
{
    private static final int SIZE = 23;
    private static final int[] INT_UP= new int[SIZE];
    private static final int[] INT_DW= new int[SIZE];
    private static final double[] DOUBLE_UP= new double[SIZE];
    private static final double[] DOUBLE_DW= new double[SIZE];
    private static final Integer[] INTEGER_UP= new Integer[SIZE];
    private static final Integer[] INTEGER_DW= new Integer[SIZE];
    
    private static final Integer[] INTEGER_ASC = {0,1,2,3,4,5,6,7,8,9};
    private static final Integer[] INTEGER_DES = {9,8,7,6,5,4,3,2,1,0};
    private static final Integer[] INTEGER_RND = {0,2,4,6,8,9,7,5,3,1};

    static final Locale[] LOCALE_ASC = {Locale.UK,Locale.US,Locale.CHINA};
    static final Locale[] LOCALE_DES = {Locale.CHINA,Locale.US,Locale.UK};
    static final Locale[] LOCALE_RND = {Locale.US,Locale.CHINA,Locale.UK};

    public SortsTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
        for(int i=0;i<SIZE;i++)
        {
            INT_UP[i]    =i;
            INT_DW[i]    =(SIZE-i-1);
            DOUBLE_UP[i] =i;
            DOUBLE_DW[i] =(SIZE-i-1);
            INTEGER_UP[i]=i;
            INTEGER_DW[i]=(SIZE-i-1);
        }
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
     * Test of reverse method, of class Sorts.
     */
    @Test
    public void testReverse_intArr()
    {
        int[] a = INT_UP.clone();
        
        Sorts.reverse(a);
        assertArrayEquals(INT_DW, a);

        Sorts.reverse(a);
        assertArrayEquals(INT_UP, a);
    }

    /**
     * Test of reverse method, of class Sorts.
     */
    @Test
    public void testReverse_doubleArr()
    {
        double[] a = DOUBLE_UP.clone();
        
        Sorts.reverse(a);
        assertArrayEquals(DOUBLE_DW, a, 0.0);

        Sorts.reverse(a);
        assertArrayEquals(DOUBLE_UP, a, 0.0);
    }

    /**
     * Test of reverse method, of class Sorts.
     */
    @Test
    public void testReverse_GenericType()
    {
        Integer[] a = INTEGER_UP.clone();
        
        Sorts.reverse(a);
        assertArrayEquals(INTEGER_DW, a);

        Sorts.reverse(a);
        assertArrayEquals(INTEGER_UP, a);
    }
    
    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_5args_1()
    {
        int[] k = INT_DW.clone();
        int[] v = INT_UP.clone();
        
        Sorts.sort(k, v, 0, SIZE, false);
        assertArrayEquals(INT_UP, k);
        assertArrayEquals(INT_DW, v);

        Sorts.sort(k, v, 0, SIZE, true);
        assertArrayEquals(INT_DW, k);
        assertArrayEquals(INT_UP, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_intArr_intArr()
    {
        int[] k = INT_DW.clone();
        int[] v = INT_UP.clone();
        
        Sorts.sort(k, v);
        assertArrayEquals(INT_UP, k);
        assertArrayEquals(INT_DW, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_3args_1()
    {
        int[] k = INT_DW.clone();
        int[] v = INT_UP.clone();
        
        Sorts.sort(k, v, false);
        assertArrayEquals(INT_UP, k);
        assertArrayEquals(INT_DW, v);

        Sorts.sort(k, v, true);
        assertArrayEquals(INT_DW, k);
        assertArrayEquals(INT_UP, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_4args_1()
    {
        int[] k = INT_DW.clone();
        int[] v = INT_UP.clone();
        
        Sorts.sort(k, v, 0, SIZE);
        assertArrayEquals(INT_UP, k);
        assertArrayEquals(INT_DW, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_5args_2()
    {
        double[] k = DOUBLE_DW.clone();
        double[] v = DOUBLE_UP.clone();
        
        Sorts.sort(k, v, 0, SIZE, false);
        assertArrayEquals(DOUBLE_UP, k, 0.0);
        assertArrayEquals(DOUBLE_DW, v, 0.0);

        Sorts.sort(k, v, 0, SIZE, true);
        assertArrayEquals(DOUBLE_DW, k, 0.0);
        assertArrayEquals(DOUBLE_UP, v, 0.0);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_doubleArr_doubleArr()
    {
        double[] k = DOUBLE_DW.clone();
        double[] v = DOUBLE_UP.clone();
        
        Sorts.sort(k, v);
        assertArrayEquals(DOUBLE_UP, k, 0.0);
        assertArrayEquals(DOUBLE_DW, v, 0.0);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_3args_2()
    {
        double[] k = DOUBLE_DW.clone();
        double[] v = DOUBLE_UP.clone();
        
        Sorts.sort(k, v, false);
        assertArrayEquals(DOUBLE_UP, k, 0.0);
        assertArrayEquals(DOUBLE_DW, v, 0.0);

        Sorts.sort(k, v, true);
        assertArrayEquals(DOUBLE_DW, k, 0.0);
        assertArrayEquals(DOUBLE_UP, v, 0.0);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_4args_2()
    {
        double[] k = DOUBLE_DW.clone();
        double[] v = DOUBLE_UP.clone();
        
        Sorts.sort(k, v, 0, SIZE);
        assertArrayEquals(DOUBLE_UP, k, 0.0);
        assertArrayEquals(DOUBLE_DW, v, 0.0);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_5args_3()
    {
        int[] k = INT_DW.clone();
        Integer[] v = INTEGER_UP.clone();
        
        Sorts.sort(k, v, 0, SIZE, false);
        assertArrayEquals(INT_UP, k);
        assertArrayEquals(INTEGER_DW, v);

        Sorts.sort(k, v, 0, SIZE, true);
        assertArrayEquals(INT_DW, k);
        assertArrayEquals(INTEGER_UP, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_intArr_GenericType()
    {
        int[] k = INT_DW.clone();
        Integer[] v = INTEGER_UP.clone();
        
        Sorts.sort(k, v);
        assertArrayEquals(INT_UP, k);
        assertArrayEquals(INTEGER_DW, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_3args_3()
    {
        int[] k = INT_DW.clone();
        Integer[] v = INTEGER_UP.clone();
        
        Sorts.sort(k, v, false);
        assertArrayEquals(INT_UP, k);
        assertArrayEquals(INTEGER_DW, v);

        Sorts.sort(k, v, true);
        assertArrayEquals(INT_DW, k);
        assertArrayEquals(INTEGER_UP, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_4args_3()
    {
        int[] k = INT_DW.clone();
        Integer[] v = INTEGER_UP.clone();
        
        Sorts.sort(k, v, 0, SIZE);
        assertArrayEquals(INT_UP, k);
        assertArrayEquals(INTEGER_DW, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_5args_4()
    {
        Integer[] k = INTEGER_DW.clone();
        Integer[] v = INTEGER_UP.clone();
        
        Sorts.sort(k, v, 0, SIZE, false);
        assertArrayEquals(INTEGER_UP, k);
        assertArrayEquals(INTEGER_DW, v);

        Sorts.sort(k, v, 0, SIZE, true);
        assertArrayEquals(INTEGER_DW, k);
        assertArrayEquals(INTEGER_UP, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_GenericTypeArr_GenericType()
    {
        Integer[] k = INTEGER_DW.clone();
        Integer[] v = INTEGER_UP.clone();
        
        Sorts.sort(k, v);
        assertArrayEquals(INTEGER_UP, k);
        assertArrayEquals(INTEGER_DW, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_3args_4()
    {
        Integer[] k = INTEGER_DW.clone();
        Integer[] v = INTEGER_UP.clone();
        
        Sorts.sort(k, v, false);
        assertArrayEquals(INTEGER_UP, k);
        assertArrayEquals(INTEGER_DW, v);

        Sorts.sort(k, v, true);
        assertArrayEquals(INTEGER_DW, k);
        assertArrayEquals(INTEGER_UP, v);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_4args_4()
    {
        Integer[] k = INTEGER_DW.clone();
        Integer[] v = INTEGER_UP.clone();
        
        Sorts.sort(k, v, 0, SIZE);
        assertArrayEquals(INTEGER_UP, k);
        assertArrayEquals(INTEGER_DW, v);
    }

    /**
     * Test of asStringComparator method, of class Sorts.
     */
    @Test
    public void testAsStringComparator()
    {
        Comparator cmp = Sorts.asStringComparator();
        
        Integer[] tmp;
        
        tmp = INTEGER_ASC.clone();
        Arrays.sort(tmp, cmp);
        assertArrayEquals(INTEGER_ASC, tmp);

        tmp = INTEGER_DES.clone();
        Arrays.sort(tmp, cmp);
        assertArrayEquals(INTEGER_ASC, tmp);
        
        tmp = INTEGER_RND.clone();
        Arrays.sort(tmp, cmp);
        assertArrayEquals(INTEGER_ASC, tmp);
    }


    /**
     * Test of asStringComparatorReverse method, of class Sorts.
     */
    @Test
    public void testAsStringComparatorReverse()
    {
        Comparator cmp = Sorts.asStringComparatorReverse();
        
        Integer[] tmp;
        
        tmp = INTEGER_ASC.clone();
        Arrays.sort(tmp, cmp);
        assertArrayEquals(INTEGER_DES, tmp);

        tmp = INTEGER_DES.clone();
        Arrays.sort(tmp, cmp);
        assertArrayEquals(INTEGER_DES, tmp);
        
        tmp = INTEGER_RND.clone();
        Arrays.sort(tmp, cmp);
        assertArrayEquals(INTEGER_DES, tmp);
    }


    /**
     * Test of isSorted method, of class Sorts.
     */
    @Test
    public void testIsSorted()
    {
        assertFalse(Sorts.isSorted("c","b","a"));
        assertFalse(Sorts.isSorted("a","c","b"));
        assertFalse(Sorts.isSorted("a","b","a"));
        assertFalse(Sorts.isSorted("a","A"));
        
        assertTrue(Sorts.isSorted());
        assertTrue(Sorts.isSorted("a"));
        assertTrue(Sorts.isSorted("a","b"));
        assertTrue(Sorts.isSorted("a","b","c"));
    }
    /**
     * Test of getSorted method, of class Sorts.
     */
    @Test
    public void testGetSorted()
    {
        assertEquals(1,Sorts.getSorted("c","b","a"));
        assertEquals(2,Sorts.getSorted("a","c","b"));
        assertEquals(2,Sorts.getSorted("a","b","a"));
        assertEquals(1,Sorts.getSorted("a","A"));
        
        assertEquals(0,Sorts.getSorted());
        assertEquals(1,Sorts.getSorted("a"));
        assertEquals(2,Sorts.getSorted("a","b"));
        assertEquals(3,Sorts.getSorted("a","b","c"));
    }

    /**
     * Test of isSortedIgnoreCase method, of class Sorts.
     */
    @Test
    public void testIsSortedIgnoreCase_Locale_StringArr()
    {
        Locale us = Locale.US;
        assertFalse(Sorts.isSortedIgnoreCase(us,"c","b","a"));
        assertFalse(Sorts.isSortedIgnoreCase(us,"a","c","b"));
        assertFalse(Sorts.isSortedIgnoreCase(us,"a","b","a"));
        assertTrue(Sorts.isSortedIgnoreCase(us,"a","A"));
        
        assertTrue(Sorts.isSortedIgnoreCase(us));
        assertTrue(Sorts.isSortedIgnoreCase(us,"a"));
        assertTrue(Sorts.isSortedIgnoreCase(us,"a","b"));
        assertTrue(Sorts.isSortedIgnoreCase(us,"a","b","c"));
    }

    /**
     * Test of getSortedIgnoreCase method, of class Sorts.
     */
    @Test
    public void testGetSortedIgnoreCase_Locale_StringArr()
    {
        Locale us = Locale.US;        
        assertEquals(1,Sorts.getSortedIgnoreCase(us,"c","b","a"));
        assertEquals(2,Sorts.getSortedIgnoreCase(us,"a","c","b"));
        assertEquals(2,Sorts.getSortedIgnoreCase(us,"a","b","a"));
        assertEquals(2,Sorts.getSortedIgnoreCase(us,"a","A"));
        
        assertEquals(0,Sorts.getSortedIgnoreCase(us));
        assertEquals(1,Sorts.getSortedIgnoreCase(us,"a"));
        assertEquals(2,Sorts.getSortedIgnoreCase(us,"a","b"));
        assertEquals(3,Sorts.getSortedIgnoreCase(us,"a","b","c"));
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_byteArrArr()
    {
        byte[][] expected = 
        {
            {},
            {0},
            {0,1,2},
            {1},
            {1,0,2}
        };
        byte[][] items = 
        {
            {1,0,2},
            {0},
            {},
            {0,1,2},
            {1},
        };
        Sorts.sort(items);
        assertArrayEquals(expected, items);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_intArrArr()
    {
        int[][] expected = 
        {
            {},
            {0},
            {0,1,2},
            {1},
            {1,0,2}
        };
        int[][] items = 
        {
            {1,0,2},
            {0},
            {},
            {0,1,2},
            {1},
        };
        Sorts.sort(items);
        assertArrayEquals(expected, items);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_longArrArr()
    {
        long[][] expected = 
        {
            {},
            {0},
            {0,1,2},
            {1},
            {1,0,2}
        };
        long[][] items = 
        {
            {1,0,2},
            {0},
            {},
            {0,1,2},
            {1},
        };
        Sorts.sort(items);
        assertArrayEquals(expected, items);
    }

    /**
     * Test of sort method, of class Sorts.
     */
    @Test
    public void testSort_doubleArrArr()
    {
        double[][] expected = 
        {
            {},
            {0},
            {0,1,2},
            {1},
            {1,0,2}
        };
        double[][] items = 
        {
            {1,0,2},
            {0},
            {},
            {0,1,2},
            {1},
        };
        Sorts.sort(items);
        assertArrayEquals(expected, items);
    }

    
}
