/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import io.nut.base.time.JavaTime;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

/**
 *
 * @author franci
 */
public class ComparatorsTest
{

    /**
     * Test of compare method, of class Comparators.
     */
    @Test
    public void testCompare_byteArr_byteArr()
    {
        
        byte[] b0 = {};
        byte[] b1 = {1};
        byte[] b2 = {2};
        byte[] c2 = {2};
        byte[] b11 = {1,1};
        byte[] b20 = {2,0};

        //test compare with itself
        assertTrue(Comparators.compare(b0, b0)==0);
        assertTrue(Comparators.compare(b1, b1)==0);
        assertTrue(Comparators.compare(b11, b11)==0);
        assertTrue(Comparators.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Comparators.compare(b0, b1)<0);
        assertTrue(Comparators.compare(b1, b0)>0);
        
        assertTrue(Comparators.compare(b1, b2)<0);
        assertTrue(Comparators.compare(b2, b1)>0);
        assertTrue(Comparators.compare(b2, c2)==0);
        
        assertTrue(Comparators.compare(b11, b20)<0);
        assertTrue(Comparators.compare(b20, b11)>0);
        
        assertTrue(Comparators.compare(b2, b11)>0);
        assertTrue(Comparators.compare(b11, b2)<0);
        
    }
    /**
     * Test of compare method, of class Comparators.
     */
    @Test
    public void testCompare_intArr_intArr()
    {
        
        int[] b0 = {};
        int[] b1 = {1};
        int[] b2 = {2};
        int[] c2 = {2};
        int[] b11 = {1,1};
        int[] b20 = {2,0};
        
        //test compare with itself
        assertTrue(Comparators.compare(b0, b0)==0);
        assertTrue(Comparators.compare(b1, b1)==0);
        assertTrue(Comparators.compare(b11, b11)==0);
        assertTrue(Comparators.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Comparators.compare(b0, b1)<0);
        assertTrue(Comparators.compare(b1, b0)>0);
        
        assertTrue(Comparators.compare(b1, b2)<0);
        assertTrue(Comparators.compare(b2, b1)>0);
        assertTrue(Comparators.compare(b2, c2)==0);
        
        assertTrue(Comparators.compare(b11, b20)<0);
        assertTrue(Comparators.compare(b20, b11)>0);
        
        assertTrue(Comparators.compare(b2, b11)>0);
        assertTrue(Comparators.compare(b11, b2)<0);
        
    }
    /**
     * Test of compare method, of class Comparators.
     */
    @Test
    public void testCompareLong()
    {
        long[] b0 = {};
        long[] b1 = {1};
        long[] b2 = {2};
        long[] c2 = {2};
        long[] b11 = {1,1};
        long[] b20 = {2,0};
        
        //test compare with itself
        assertTrue(Comparators.compare(b0, b0)==0);
        assertTrue(Comparators.compare(b1, b1)==0);
        assertTrue(Comparators.compare(b11, b11)==0);
        assertTrue(Comparators.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Comparators.compare(b0, b1)<0);
        assertTrue(Comparators.compare(b1, b0)>0);
        
        assertTrue(Comparators.compare(b1, b2)<0);
        assertTrue(Comparators.compare(b2, b1)>0);
        assertTrue(Comparators.compare(b2, c2)==0);
        
        assertTrue(Comparators.compare(b11, b20)<0);
        assertTrue(Comparators.compare(b20, b11)>0);
        
        assertTrue(Comparators.compare(b2, b11)>0);
        assertTrue(Comparators.compare(b11, b2)<0);
    }
    /**
     * Test of compare method, of class Comparators.
     */
    @Test
    public void testCompareDouble()
    {
        
        double[] b0 = {};
        double[] b1 = {1};
        double[] b2 = {2};
        double[] c2 = {2};
        double[] b11 = {1,1};
        double[] b20 = {2,0};
        
        //test compare with itself
        assertTrue(Comparators.compare(b0, b0)==0);
        assertTrue(Comparators.compare(b1, b1)==0);
        assertTrue(Comparators.compare(b11, b11)==0);
        assertTrue(Comparators.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Comparators.compare(b0, b1)<0);
        assertTrue(Comparators.compare(b1, b0)>0);
        
        assertTrue(Comparators.compare(b1, b2)<0);
        assertTrue(Comparators.compare(b2, b1)>0);
        assertTrue(Comparators.compare(b2, c2)==0);
        
        assertTrue(Comparators.compare(b11, b20)<0);
        assertTrue(Comparators.compare(b20, b11)>0);
        
        assertTrue(Comparators.compare(b2, b11)>0);
        assertTrue(Comparators.compare(b11, b2)<0);
        
    }

    /**
     * Test of compare method, of class Comparators.
     */
    @Test
    public void testCompareChar()
    {
        char[] b0 = {};
        char[] b1 = {'a'};
        char[] b2 = {'b'};
        char[] c2 = {'b'};
        char[] b11 = {'a','a'};
        char[] b20 = {'b','a'};
        
        //test compare with itself
        assertTrue(Comparators.compare(b0, b0)==0);
        assertTrue(Comparators.compare(b1, b1)==0);
        assertTrue(Comparators.compare(b11, b11)==0);
        assertTrue(Comparators.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Comparators.compare(b0, b1)<0);
        assertTrue(Comparators.compare(b1, b0)>0);
        
        assertTrue(Comparators.compare(b1, b2)<0);
        assertTrue(Comparators.compare(b2, b1)>0);
        assertTrue(Comparators.compare(b2, c2)==0);
        
        assertTrue(Comparators.compare(b11, b20)<0);
        assertTrue(Comparators.compare(b20, b11)>0);
        
        assertTrue(Comparators.compare(b2, b11)>0);
        assertTrue(Comparators.compare(b11, b2)<0);
    }

    /**
     * Test of compare method, of class Comparators.
     */
    @Test
    public void testCompareShort()
    {
        short[] b0 = {};
        short[] b1 = {1};
        short[] b2 = {2};
        short[] c2 = {2};
        short[] b11 = {1,1};
        short[] b20 = {2,0};
        
        //test compare with itself
        assertTrue(Comparators.compare(b0, b0)==0);
        assertTrue(Comparators.compare(b1, b1)==0);
        assertTrue(Comparators.compare(b11, b11)==0);
        assertTrue(Comparators.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Comparators.compare(b0, b1)<0);
        assertTrue(Comparators.compare(b1, b0)>0);
        
        assertTrue(Comparators.compare(b1, b2)<0);
        assertTrue(Comparators.compare(b2, b1)>0);
        assertTrue(Comparators.compare(b2, c2)==0);
        
        assertTrue(Comparators.compare(b11, b20)<0);
        assertTrue(Comparators.compare(b20, b11)>0);
        
        assertTrue(Comparators.compare(b2, b11)>0);
        assertTrue(Comparators.compare(b11, b2)<0);
    }

    /**
     * Test of compare method, of class Comparators.
     */
    @Test
    public void testCompareFloat()
    {
        float[] b0 = {};
        float[] b1 = {1};
        float[] b2 = {2};
        float[] c2 = {2};
        float[] b11 = {1,1};
        float[] b20 = {2,0};
        
        //test compare with itself
        assertTrue(Comparators.compare(b0, b0)==0);
        assertTrue(Comparators.compare(b1, b1)==0);
        assertTrue(Comparators.compare(b11, b11)==0);
        assertTrue(Comparators.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Comparators.compare(b0, b1)<0);
        assertTrue(Comparators.compare(b1, b0)>0);
        
        assertTrue(Comparators.compare(b1, b2)<0);
        assertTrue(Comparators.compare(b2, b1)>0);
        assertTrue(Comparators.compare(b2, c2)==0);
        
        assertTrue(Comparators.compare(b11, b20)<0);
        assertTrue(Comparators.compare(b20, b11)>0);
        
        assertTrue(Comparators.compare(b2, b11)>0);
        assertTrue(Comparators.compare(b11, b2)<0);
    }

    /**
     * Test of compare method, of class Comparators.
     */
    @Test
    public void testCompareBoolean()
    {
        boolean[] b0 = {};
        boolean[] b1 = {false};
        boolean[] b2 = {true};
        boolean[] c2 = {true};
        boolean[] b11 = {false,false};
        boolean[] b20 = {true,false};
        
        //test compare with itself
        assertTrue(Comparators.compare(b0, b0)==0);
        assertTrue(Comparators.compare(b1, b1)==0);
        assertTrue(Comparators.compare(b11, b11)==0);
        assertTrue(Comparators.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Comparators.compare(b0, b1)<0);
        assertTrue(Comparators.compare(b1, b0)>0);
        
        assertTrue(Comparators.compare(b1, b2)<0);
        assertTrue(Comparators.compare(b2, b1)>0);
        assertTrue(Comparators.compare(b2, c2)==0);
        
        assertTrue(Comparators.compare(b11, b20)<0);
        assertTrue(Comparators.compare(b20, b11)>0);
        
        assertTrue(Comparators.compare(b2, b11)>0);
        assertTrue(Comparators.compare(b11, b2)<0);
    }


    /**
     * Test of equals method, of class Comparators.
     */
    @Test
    public void testEquals_2args_1()
    {
        Object e1 = 1;
        Object e2 = 2;
        Object e22 = 2;
        
        assertFalse(Comparators.equals(e1, e2));
        assertTrue(Comparators.equals(e2, e22));
        
        Object en1 = null;
        Object en2 = null;
        assertTrue(Comparators.equals(en1, en2));

        assertFalse(Comparators.equals(e1, en1));
        assertFalse(Comparators.equals(en1, e1));
    }

    enum Dummy{ A, B, C};
    /**
     * Test of equals method, of class Comparators.
     */
    @Test
    public void testEquals_2args_2()
    {
        Dummy dummyNull = null;
        assertTrue(Comparators.equals(dummyNull, dummyNull));
        assertTrue(Comparators.equals(Dummy.A, Dummy.A));
        
        assertFalse(Comparators.equals(Dummy.A, dummyNull));
        assertFalse(Comparators.equals(dummyNull, Dummy.A));
        assertFalse(Comparators.equals(Dummy.A, Dummy.B));
    }
    
    /**
     * Test of Comparator fields, of class Comparators.
     */
    @Test
    public void testComparators() throws Exception
    {
        {
            List<String> a = Arrays.asList("a");
            List<String> b = Arrays.asList("a","b");
            List<String>[] array = new List[]{b,a};

            Arrays.sort(array, Comparators.COLLECTION_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
        {
            String[] a = {"a"};
            String[] b = {"a","b"};
            String[][] array = {b,a};

            Arrays.sort(array, Comparators.ARRAY_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
        {
            byte[] a = {1};
            byte[] b = {1,1};
            byte[][] array = {b,a};

            Arrays.sort(array, Comparators.BYTE_ARRAY_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
        {
            int[] a = {1};
            int[] b = {1,1};
            int[][] array = {b,a};

            Arrays.sort(array, Comparators.INT_ARRAY_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
        {
            long[] a = {1};
            long[] b = {1,1};
            long[][] array = {b,a};

            Arrays.sort(array, Comparators.LONG_ARRAY_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
    }
    
}
