/*
 * UtilsTest.java
 *
 * Copyright (c) 2023-2025 francitoshi@gmail.com
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

import io.nut.base.encoding.Encoding;
import io.nut.base.math.Nums;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class UtilsTest
{
    @Test
    public void testReverseBytes()
    {
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, Sorts.reverseOf(new byte[]{5, 4, 3, 2, 1}));
        assertArrayEquals(new byte[]{0}, Sorts.reverseOf(new byte[]{0}));
        assertArrayEquals(new byte[]{}, Sorts.reverseOf(new byte[]{}));
    }

    /**
     * Test of firstNonNull method, of class Utils.
     */
    @Test
    public void testFirstNonNull()
    {
        assertNull(Utils.firstNonNull());
        assertEquals("a", Utils.firstNonNull("a"));
        assertEquals("a", Utils.firstNonNull("a", "b"));
        
        assertEquals("a", Utils.firstNonNull("a", "b", "c", "d"));
        assertEquals("b", Utils.firstNonNull(null, "b", "c", "d"));
        assertEquals("c", Utils.firstNonNull(null, null, "c", "d"));
        assertEquals("d", Utils.firstNonNull(null, null, null, "d"));
        assertNull(Utils.firstNonNull(null, null, null, null));
    }
    
    /**
     * Test of firstNonNullOrEmpty method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrEmpty()
    {
        assertEquals("", Utils.firstNonNullOrEmpty(null, null));
        assertEquals("", Utils.firstNonNullOrEmpty(null,""));
        assertEquals("5", Utils.firstNonNullOrEmpty(5, "","1"));
        assertEquals("1", Utils.firstNonNullOrEmpty("","1"));
    }

    /**
     * Test of sleep method, of class Utils.
     */
    @Test
    public void testSleep_long_int()
    {
        long t0 = System.nanoTime();
        Utils.sleep(123,1234);
        long t1 = System.nanoTime();
        long diff = (t1-t0)/Utils.NANOS_PER_MILLIS;
        
        assertTrue(diff>=123);
    }
    /**
     * Test of sleep method, of class Utils.
     */
    @Test
    public void testSleep_long()
    {
        long t0 = System.nanoTime();
        Utils.sleep(123);
        long t1 = System.nanoTime();
        long diff = (t1-t0)/Utils.NANOS_PER_MILLIS;
        
        assertTrue(diff>=123);
    }

    /**
     * Test of asIntArray method, of class ArrayUtils.
     */
    @Test
    public void testAsInt_byteArr()
    {
        {
            byte[] src = null;
            assertNull(Utils.asInts(src));
        }
        {
            byte[] src = {};
            int[] expResult = {};
            assertArrayEquals(expResult, Utils.asInts(src));
        }
        {
            byte[] src = {-127, -1, 0, +1, +127};
            int[] expResult = {-127, -1, 0, +1, +127};
            assertArrayEquals(expResult, Utils.asInts(src));
        }
    }

    /**
     * Test of asIntArray method, of class ArrayUtils.
     */
    @Test
    public void testAsInt_shortArr()
    {
        assertArrayEquals(null, Utils.asInts((short[])null));
        assertArrayEquals(intArray, Utils.asInts(shortArray));
    }
    
    /**
     * Test of asIntArray method, of class ArrayUtils.
     */
    @Test
    public void testAsInt_IntegerArr_int()
    {
        assertArrayEquals(null, Utils.asInts((Integer[])null,0));
        assertArrayEquals(intArray, Utils.asInts(integerArray,0));
    }


    /**
     * Test of asLongArray method, of class ArrayUtils.
     */
    @Test
    public void testAsLong_byteArr()
    {
        {
            byte[] src = null;
            assertNull(Utils.asLongs(src));
        }
        {
            byte[] src = {};
            long[] expResult = {};
            assertArrayEquals(expResult, Utils.asLongs(src));
        }
        {
            byte[] src       = {Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE};
            long[] expResult = {Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE};
            assertArrayEquals(expResult, Utils.asLongs(src));
        }
    }

    /**
     * Test of asLongArray method, of class ArrayUtils.
     */
    @Test
    public void testAsLong_shortArr()
    {
        assertArrayEquals(null, Utils.asLongs((short[])null));
        assertArrayEquals(longArray, Utils.asLongs(shortArray));
    }

    /**
     * Test of asLongArray method, of class ArrayUtils.
     */
    @Test
    public void testAsLong_intArr()
    {
        assertArrayEquals(null, Utils.asLongs((int[])null));
        assertArrayEquals(longArray, Utils.asLongs(intArray));
    }
    
    /**
     * Test of asLong method, of class Utils.
     */
    @Test
    public void testAsLong_LongArr()
    {
        Long[] src = {1L,2L,3L};
        long[] expResult = {1,2,3};
        assertArrayEquals(expResult, Utils.asLongs(src));
    }

    /**
     * Test of asFloatArray method, of class ArrayUtils.
     */
    @Test
    public void testAsFloats_byteArr()
    {
        assertArrayEquals((float[])null, Utils.asFloats((byte[])null), 0f);
        assertArrayEquals(floatArray, Utils.asFloats(byteArray), 0f);
    }

    /**
     * Test of asFloatArray method, of class ArrayUtils.
     */
    @Test
    public void testAsFloats_shortArr()
    {
        assertArrayEquals((float[])null, Utils.asFloats((short[])null), 0f);
        assertArrayEquals(floatArray, Utils.asFloats(shortArray), 0f);
    }

    /**
     * Test of asFloatArray method, of class ArrayUtils.
     */
    @Test
    public void testAsFloats_intArr()
    {
        assertArrayEquals((float[])null, Utils.asFloats((int[])null), 0f);
        assertArrayEquals(floatArray, Utils.asFloats(intArray), 0f);
    }

    /**
     * Test of asDoubleArray method, of class ArrayUtils.
     */
    @Test
    public void testAsDouble_byteArr()
    {
        assertArrayEquals((double[])null, Utils.asDoubles((byte[])null), 0f);
        assertArrayEquals(doubleArray, Utils.asDoubles(byteArray), 0f);
    }

    /**
     * Test of asDoubleArray method, of class ArrayUtils.
     */
    @Test
    public void testAsDouble_shortArr()
    {
        assertArrayEquals((double[])null, Utils.asDoubles((short[])null), 0f);
        assertArrayEquals(doubleArray, Utils.asDoubles(shortArray), 0f);
    }

    /**
     * Test of asDoubleArray method, of class ArrayUtils.
     */
    @Test
    public void testAsDouble_intArr()
    {
        {
            int[] src = null;
            assertNull(Utils.asDoubles(src));
        }
        {
            int[] src = {};
            double[] expResult = {};
            assertArrayEquals(expResult, Utils.asDoubles(src), 0.0);
        }
        {
            int[] src          = {Integer.MIN_VALUE, Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE, Integer.MAX_VALUE};
            double[] expResult = {Integer.MIN_VALUE, Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE, Integer.MAX_VALUE};
            assertArrayEquals(expResult, Utils.asDoubles(src), 0.0);
        }
        
    }

    /**
     * Test of asDoubleArray method, of class ArrayUtils.
     */
    @Test
    public void testAsDouble_longArr()
    {
        {
            long[] src = null;
            assertNull(Utils.asDoubles(src));
        }
        {
            long[] src = {};
            double[] expResult = {};
            assertArrayEquals(expResult, Utils.asDoubles(src), 0.0);
        }
        {
            long[] src         = {Long.MIN_VALUE, Integer.MIN_VALUE, Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE};
            double[] expResult = {Long.MIN_VALUE, Integer.MIN_VALUE, Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE};
            assertArrayEquals(expResult, Utils.asDoubles(src), 0.0);
        }
        
    }

    /**
     * Test of asDoubleArray method, of class ArrayUtils.
     */
    @Test
    public void testAsDouble_floatArr()
    {
        {
            float[] src = null;
            assertNull(Utils.asDoubles(src));
        }
        {
            float[] src = {};
            double[] expResult = {};
            assertArrayEquals(expResult, Utils.asDoubles(src), 0.0);
        }
        {
            float[] src = {0.1f, 0.02f, 0.003f};
            double[] expResult = {0.1d, 0.02d, 0.003d};
            assertArrayEquals(expResult, Utils.asDoubles(src), 0.000001);
        }
    }
    
    @Test
    public void testAsLongs()
    {
        long[] result = Utils.asLongs(VALUES_INT);
        assertArrayEquals(VALUES_LONG, result);
    }
    
    static final int[] VALUES_INT = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    static final long[] VALUES_LONG = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    static final float[] VALUES_FLOAT = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    static final double[] VALUES_DOUBLE = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    
    static final byte[] byteArray = {0,1,2,3,4};
    static final short[] shortArray = {0,1,2,3,4};
    static final int[] intArray = {0,1,2,3,4};
    static final Integer[] integerArray = {null,1,2,3,4};
    static final long[] longArray = {0,1,2,3,4};
    static final float[] floatArray = {0,1,2,3,4};
    static final double[] doubleArray = {0,1,2,3,4};
    

    /**
     * Test of asString method, of class Utils.
     */
    @Test
    public void testAsString_GenericType()
    {
        {
            Long[] src = null;
            assertNull(Utils.asStrings(src));
        }
        {
            Long[] src = {};
            assertEquals(0, Utils.asStrings(src).length);
        }
        {
            Integer[] src = {1,2,3};
            String[] exp = {"1","2","3"};
            assertArrayEquals(exp, Utils.asStrings(src));
        }
    }

    /**
     * Test of asString method, of class Utils.
     */
    @Test
    public void testAsString_List()
    {
        {
            ArrayList<Long> src = null;
            assertNull(Utils.asStrings(src));
        }
        {
            ArrayList<Long> src = new ArrayList<>();
            assertEquals(0, Utils.asStrings(src).length);
        }
        {
            ArrayList<Long> src = new ArrayList<>();
            src.add(1L);
            src.add(2L);
            src.add(3L);
            String[] exp = {"1","2","3"};
            assertArrayEquals(exp, Utils.asStrings(src));
        }
    }

    /**
     * Test of inverse method, of class Utils.
     */
    @Test
    public void testInverse()
    {
        HashMap<String,Integer> map = new HashMap<>();
        
        for(int i=0;i<10;i++)
        {
            map.put( (""+i), i);
        }
        
        Map<Integer,String> inverse = Utils.inverse(map);
        
        for(int i=0;i<10;i++)
        {
            String s = inverse.get(i);
            assertEquals( (""+i), s);
        }
        
    }

    /**
     * Test of asLong method, of class Utils.
     */
    @Test
    public void testAsLong_StringArr_long_int()
    {
        assertNull(Utils.asLongs(null, 0));
        
        {
            String[] s123 = {};
            long[] r123 = {};
            long[] result = Utils.asLongs(s123, 0, 10);
            assertArrayEquals(r123, result);
        }
        {
            String[] s123 = {"1","2","3"};
            long[] r123 = {1,2,3};
            long[] result = Utils.asLongs(s123, 0, 10);
            assertArrayEquals(r123, result);
        }
        {
            String[] s123 = {"aa","bb","cc"};
            long[] r123 = {170, 187, 204};
            long[] result = Utils.asLongs(s123, 0, 16);
            assertArrayEquals(r123, result);
        }
        {
            String[] s123 = {"1","2",""};
            long[] r123 = {1,2,-1};
            long[] result = Utils.asLongs(s123, -1, 10);
            assertArrayEquals(r123, result);
        }
    }

    /**
     * Test of asInt method, of class Utils.
     */
    @Test
    public void testAsInts_intArr()
    {
        int[] expResult = {1, 3, 2};
        assertArrayEquals(expResult, Utils.asInts(1, 3, 2));
    }

    /**
     * Test of minOf method, of class Utils.
     */
    @Test
    public void testMinOf_intArr()
    {
        assertEquals(0, Utils.minOf());
        assertEquals(1, Utils.minOf(1));
        assertEquals(1, Utils.minOf(2,1));
        assertEquals(1, Utils.minOf(3,2,1));
        assertEquals(1, Utils.minOf(3,2,1,4));
    }

    /**
     * Test of maxOf method, of class Utils.
     */
    @Test
    public void testMaxOf_intArr()
    {
        assertEquals(0, Utils.maxOf());
        assertEquals(1, Utils.maxOf(1));
        assertEquals(2, Utils.maxOf(2,1));
        assertEquals(3, Utils.maxOf(3,2,1));
        assertEquals(4, Utils.maxOf(3,2,1,4));
    }
    /**
     * Test of minOf method, of class Utils.
     */
    @Test
    public void testMinOf_longArr()
    {
        assertEquals(1L, Utils.minOf(1L));
        assertEquals(1L, Utils.minOf(2L,1L));
        assertEquals(1L, Utils.minOf(3L,2L,1L));
        assertEquals(1L, Utils.minOf(3L,2L,1L,4L));
    }

    /**
     * Test of maxOf method, of class Utils.
     */
    @Test
    public void testMaxOf_longArr()
    {
        assertEquals(1L, Utils.maxOf(1L));
        assertEquals(2L, Utils.maxOf(2L,1L));
        assertEquals(3L, Utils.maxOf(3L,2L,1L));
        assertEquals(4L, Utils.maxOf(3L,2L,1L,4L));
    }

    /**
     * Test of minOf method, of class Utils.
     */
    @Test
    public void testMinOf_doubleArr()
    {
        assertEquals(0.0, Utils.minOf(), 0.0);
        assertEquals(1.0, Utils.minOf(1.0), 0.0);
        assertEquals(1.0, Utils.minOf(2.0, 1.0), 0.0);
        assertEquals(1.0, Utils.minOf(3.0, 2.0, 1.0), 0.0);
        assertEquals(1.0, Utils.minOf(3.0, 2.0, 1.0, 4.0), 0.0);
    }

    /**
     * Test of maxOf method, of class Utils.
     */
    @Test
    public void testMaxOf_doubleArr()
    {
        assertEquals(0.0, Utils.maxOf() ,0.0);
        assertEquals(1.0, Utils.maxOf(1.0) ,0.0);
        assertEquals(2.0, Utils.maxOf(2.0, 1.0) ,0.0);
        assertEquals(3.0, Utils.maxOf(3.0, 2.0, 1.0) ,0.0);
        assertEquals(4.0, Utils.maxOf(3.0, 2.0, 1.0, 4.0) ,0.0);
    }

    /**
     * Test of nonNull method, of class Utils.
     */
    @Test
    public void testNonNull()
    {
        Object[] objectNull = null;
        Object[] objectEmpty = {};
        Integer[] integer1 = {1};
        Double[] double1 = {1.0};
        String[] abc = {"a","b","c"};
        
        assertNull(Utils.nonNull(objectNull));
        assertArrayEquals(objectEmpty, Utils.nonNull());
        assertArrayEquals(integer1, Utils.nonNull(1));
        assertArrayEquals(double1, Utils.nonNull(1.0));
        
        assertArrayEquals(abc, Utils.nonNull("a","b","c"));
        assertArrayEquals(abc, Utils.nonNull("a","b","c",null));
        assertArrayEquals(abc, Utils.nonNull(null, "a","b","c",null));
        assertArrayEquals(abc, Utils.nonNull(null, "a",null, "b", null, "c",null));
    }

    /**
     * Test of compare method, of class Utils.
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
        assertTrue(Utils.compare(b0, b0)==0);
        assertTrue(Utils.compare(b1, b1)==0);
        assertTrue(Utils.compare(b11, b11)==0);
        assertTrue(Utils.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Utils.compare(b0, b1)<0);
        assertTrue(Utils.compare(b1, b0)>0);
        
        assertTrue(Utils.compare(b1, b2)<0);
        assertTrue(Utils.compare(b2, b1)>0);
        assertTrue(Utils.compare(b2, c2)==0);
        
        assertTrue(Utils.compare(b11, b20)<0);
        assertTrue(Utils.compare(b20, b11)>0);
        
        assertTrue(Utils.compare(b2, b11)>0);
        assertTrue(Utils.compare(b11, b2)<0);
        
    }
    /**
     * Test of compare method, of class Utils.
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
        assertTrue(Utils.compare(b0, b0)==0);
        assertTrue(Utils.compare(b1, b1)==0);
        assertTrue(Utils.compare(b11, b11)==0);
        assertTrue(Utils.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Utils.compare(b0, b1)<0);
        assertTrue(Utils.compare(b1, b0)>0);
        
        assertTrue(Utils.compare(b1, b2)<0);
        assertTrue(Utils.compare(b2, b1)>0);
        assertTrue(Utils.compare(b2, c2)==0);
        
        assertTrue(Utils.compare(b11, b20)<0);
        assertTrue(Utils.compare(b20, b11)>0);
        
        assertTrue(Utils.compare(b2, b11)>0);
        assertTrue(Utils.compare(b11, b2)<0);
        
    }
    /**
     * Test of compare method, of class Utils.
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
        assertTrue(Utils.compare(b0, b0)==0);
        assertTrue(Utils.compare(b1, b1)==0);
        assertTrue(Utils.compare(b11, b11)==0);
        assertTrue(Utils.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Utils.compare(b0, b1)<0);
        assertTrue(Utils.compare(b1, b0)>0);
        
        assertTrue(Utils.compare(b1, b2)<0);
        assertTrue(Utils.compare(b2, b1)>0);
        assertTrue(Utils.compare(b2, c2)==0);
        
        assertTrue(Utils.compare(b11, b20)<0);
        assertTrue(Utils.compare(b20, b11)>0);
        
        assertTrue(Utils.compare(b2, b11)>0);
        assertTrue(Utils.compare(b11, b2)<0);
    }
    /**
     * Test of compare method, of class Utils.
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
        assertTrue(Utils.compare(b0, b0)==0);
        assertTrue(Utils.compare(b1, b1)==0);
        assertTrue(Utils.compare(b11, b11)==0);
        assertTrue(Utils.compare(b20, b20)==0);

        //test arrays of different size
        assertTrue(Utils.compare(b0, b1)<0);
        assertTrue(Utils.compare(b1, b0)>0);
        
        assertTrue(Utils.compare(b1, b2)<0);
        assertTrue(Utils.compare(b2, b1)>0);
        assertTrue(Utils.compare(b2, c2)==0);
        
        assertTrue(Utils.compare(b11, b20)<0);
        assertTrue(Utils.compare(b20, b11)>0);
        
        assertTrue(Utils.compare(b2, b11)>0);
        assertTrue(Utils.compare(b11, b2)<0);
        
    }

    /**
     * Test of transpose method, of class Utils.
     */
    @Test
    public void testTranspose_byteArrArr_byte()
    {
        assertNull(Utils.transpose((byte[][])null, (byte)0));
        
        byte[][] a = {};
        assertArrayEquals(a, Utils.transpose(a, (byte)-1));
        
        byte[][] b1 = {{0},{1}};
        byte[][] b2 = {{0,1}};
        assertArrayEquals(b2, Utils.transpose(b1, (byte)-2));
        
        byte[][] c1 = {{0,1,2},{10,11}};
        byte[][] c2 = {{0,10},{1,11},{2,-3}};
        assertArrayEquals(c2, Utils.transpose(c1, (byte)-3));
        
        byte[][] d1 = {{0,1},{10,11,12},{20,21,22,23}};
        byte[][] d2 = {{0,10,20},{1,11,21},{-4,12,22},{-4,-4,23}};
        assertArrayEquals(d2, Utils.transpose(d1, (byte)-4));
    }

    /**
     * Test of transpose method, of class Utils.
     */
    @Test
    public void testTranspose_intArrArr_int()
    {
        assertNull(Utils.transpose((int[][])null, 0));
        
        int[][] a = {};
        assertArrayEquals(a, Utils.transpose(a, -1));
        
        int[][] b1 = {{0},{1}};
        int[][] b2 = {{0,1}};
        assertArrayEquals(b2, Utils.transpose(b1, -2));
        
        int[][] c1 = {{0,1,2},{10,11}};
        int[][] c2 = {{0,10},{1,11},{2,-3}};
        assertArrayEquals(c2, Utils.transpose(c1, -3));
        
        int[][] d1 = {{0,1},{10,11,12},{20,21,22,23}};
        int[][] d2 = {{0,10,20},{1,11,21},{-4,12,22},{-4,-4,23}};
        assertArrayEquals(d2, Utils.transpose(d1, -4));
        
    }

    /**
     * Test of transpose method, of class Utils.
     */
    @Test
    public void testTranspose_longArrArr_long()
    {
        assertNull(Utils.transpose((long[][])null, 0L));
        
        long[][] a = {};
        assertArrayEquals(a, Utils.transpose(a, -1L));
        
        long[][] b1 = {{0},{1}};
        long[][] b2 = {{0,1}};
        assertArrayEquals(b2, Utils.transpose(b1, -2L));
        
        long[][] c1 = {{0,1,2},{10,11}};
        long[][] c2 = {{0,10},{1,11},{2,-3}};
        assertArrayEquals(c2, Utils.transpose(c1, -3L));
        
        long[][] d1 = {{0,1},{10,11,12},{20,21,22,23}};
        long[][] d2 = {{0,10,20},{1,11,21},{-4,12,22},{-4,-4,23}};
        assertArrayEquals(d2, Utils.transpose(d1, -4L));
    }

    /**
     * Test of transpose method, of class Utils.
     */
    @Test
    public void testTranspose_floatArrArr_float()
    {
        assertNull(Utils.transpose((float[][])null, 0.0f));
        
        float[][] a = {};
        assertArrayEquals(a, Utils.transpose(a, -1.0f));
        
        float[][] b1 = {{0},{1}};
        float[][] b2 = {{0,1}};
        assertArrayEquals(b2, Utils.transpose(b1, -2.0f));
        
        float[][] c1 = {{0,1,2},{10,11}};
        float[][] c2 = {{0,10},{1,11},{2,-3}};
        assertArrayEquals(c2, Utils.transpose(c1, -3.0f));
        
        float[][] d1 = {{0,1},{10,11,12},{20,21,22,23}};
        float[][] d2 = {{0,10,20},{1,11,21},{-4,12,22},{-4,-4,23}};
        assertArrayEquals(d2, Utils.transpose(d1, -4.0f));
    }

    /**
     * Test of transpose method, of class Utils.
     */
    @Test
    public void testTranspose_doubleArrArr_double()
    {
        assertNull(Utils.transpose((double[][])null, 0.0f));
        
        double[][] a = {};
        assertArrayEquals(a, Utils.transpose(a, -1.0));
        
        double[][] b1 = {{0},{1}};
        double[][] b2 = {{0,1}};
        assertArrayEquals(b2, Utils.transpose(b1, -2.0));
        
        double[][] c1 = {{0,1,2},{10,11}};
        double[][] c2 = {{0,10},{1,11},{2,-3}};
        assertArrayEquals(c2, Utils.transpose(c1, -3.0));
        
        double[][] d1 = {{0,1},{10,11,12},{20,21,22,23}};
        double[][] d2 = {{0,10,20},{1,11,21},{-4,12,22},{-4,-4,23}};
        assertArrayEquals(d2, Utils.transpose(d1, -4.0));
    }

    /**
     * Test of transpose method, of class Utils.
     */
    @Test
    public void testTranspose_GenericType_GenericType()
    {
        assertNull(Utils.transpose((Integer[][])null, 0.0));
        
        Integer[][] a = {};
        assertArrayEquals(a, Utils.transpose(a, -1));
        
        Integer[][] b1 = {{0},{1}};
        Integer[][] b2 = {{0,1}};
        assertArrayEquals(b2, Utils.transpose(b1, -2));
        
        Integer[][] c1 = {{0,1,2},{10,11}};
        Integer[][] c2 = {{0,10},{1,11},{2,-3}};
        assertArrayEquals(c2, Utils.transpose(c1, -3));
        
        Integer[][] d1 = {{0,1},{10,11,12},{20,21,22,23}};
        Integer[][] d2 = {{0,10,20},{1,11,21},{-4,12,22},{-4,-4,23}};
        assertArrayEquals(d2, Utils.transpose(d1, -4));
        
        
        Integer[][] e1 = {null, null};
        Integer[][] e2 = {};
        assertArrayEquals(e2, Utils.transpose(e1, -1));
        
    }

    /**
     * Test of add method, of class Utils.
     */
    @Test
    public void testAdd_intArr()
    {
        assertEquals(0, Utils.add(0));
        assertEquals(0, Utils.add(0,0));
        assertEquals(1, Utils.add(1,0));
        assertEquals(3, Utils.add(1,2));
        assertEquals(6, Utils.add(1,2,3));
    }

    /**
     * Test of add method, of class Utils.
     */
    @Test
    public void testAdd_longArr()
    {
        assertEquals(0L, Utils.add(0L));
        assertEquals(0L, Utils.add(0L,0L));
        assertEquals(1L, Utils.add(1L,0L));
        assertEquals(3L, Utils.add(1L,2L));
        assertEquals(6L, Utils.add(1L,2L,3L));
    }

    /**
     * Test of add method, of class Utils.
     */
    @Test
    public void testAdd_doubleArr()
    {
        assertEquals(0.0, Utils.add(0.0),0.0);
        assertEquals(0.0, Utils.add(0.0,0.0),0.0);
        assertEquals(1.0, Utils.add(1.0,0.0),0.0);
        assertEquals(3.0, Utils.add(1.0,2.0),0.0);
        assertEquals(6.0, Utils.add(1.0,2.0,3.0),0.0);
    }

    /**
     * Test of add method, of class Utils.
     */
    @Test
    public void testAdd_BigIntegerArr()
    {
        assertEquals(BigInteger.ZERO, Utils.add(BigInteger.ZERO));
        assertEquals(BigInteger.ZERO, Utils.add(BigInteger.ZERO, BigInteger.ZERO));
        assertEquals(BigInteger.ONE, Utils.add(BigInteger.ONE, BigInteger.ZERO));
        assertEquals(BigInteger.valueOf(3), Utils.add(BigInteger.ONE, BigInteger.valueOf(2)));
        assertEquals(BigInteger.valueOf(6), Utils.add(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(6), Utils.add(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3), null));
    }

    /**
     * Test of add method, of class Utils.
     */
    @Test
    public void testAdd_BigDecimalArr()
    {
        assertEquals(BigDecimal.ZERO, Utils.add(BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO, Utils.add(BigDecimal.ZERO, BigDecimal.ZERO));
        assertEquals(BigDecimal.ONE, Utils.add(BigDecimal.ONE, BigDecimal.ZERO));
        assertEquals(BigDecimal.valueOf(3), Utils.add(BigDecimal.ONE, BigDecimal.valueOf(2)));
        assertEquals(BigDecimal.valueOf(6), Utils.add(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), null));
    }
    /**
     * Test of deepCopy method, of class Utils.
     */
    @Test
    public void testDeepCopy_intArrArr()
    {
        int[][] byteNull = null;
        int[][] byteSome = {{},{1},{1,2}};
        
        assertNull(Utils.safeDeepCopy(byteNull));
        assertArrayEquals(byteSome, Utils.safeDeepCopy(byteSome));
        assertNotSame(byteSome, Utils.safeDeepCopy(byteSome));
        
        int[][] byteOther = Utils.safeDeepCopy(byteSome);
        
        for(int i=0;i<byteSome.length;i++)
        {
            assertNotSame(byteSome[i], byteOther[i]);
        }
    }

    /**
     * Test of deepCopy method, of class Utils.
     */
    @Test
    public void testDeepCopy_byteArrArr()
    {
        byte[][] byteNull = null;
        byte[][] byteSome = {{},{1},{1,2}};
        
        assertNull(Utils.safeDeepCopy(byteNull));
        assertArrayEquals(byteSome, Utils.safeDeepCopy(byteSome));
        assertNotSame(byteSome, Utils.safeDeepCopy(byteSome));
        
        byte[][] byteOther = Utils.safeDeepCopy(byteSome);
        
        for(int i=0;i<byteSome.length;i++)
        {
            assertNotSame(byteSome[i], byteOther[i]);
        }
    }


    /**
     * Test of copy method, of class Utils.
     */
    @Test
    public void testCopy_byteArr()
    {
        byte[] byteNull = null;
        byte[] byteSome = {0,1,2};
        
        assertNull(Utils.safeCopy(byteNull));
        assertNotSame(byteSome, Utils.safeCopy(byteSome));
        assertArrayEquals(byteSome, Utils.safeCopy(byteSome));
    }

    /**
     * Test of copy method, of class Utils.
     */
    @Test
    public void testCopy_intArr()
    {
        int[] byteNull = null;
        int[] byteSome = {0,1,2};
        
        assertNull(Utils.safeCopy(byteNull));
        assertArrayEquals(byteSome, Utils.safeCopy(byteSome));
        assertNotSame(byteSome, Utils.safeCopy(byteSome));
    }


    /**
     * Test of sequence method, of class Utils.
     */
    @Test
    public void testSequence_3args_1()
    {
        {
            byte[] exp = {1,2,3};
            byte[] res = Utils.sequence(new byte[exp.length], (byte)1, (byte)1);
            assertArrayEquals(exp, res);
        }
        {
            byte[] exp = {2,4,6};
            byte[] res = Utils.sequence(new byte[exp.length], (byte)2, (byte)2);
            assertArrayEquals(exp, res);
        }
        {
            byte[] exp = {3,1,-1,-3};
            byte[] res = Utils.sequence(new byte[exp.length], (byte)3, (byte)-2);
            assertArrayEquals(exp, res);
        }
    }
    /**
     * Test of sequence method, of class Utils.
     */
    @Test
    public void testSequence_3args_2()
    {
        {
            short[] exp = {1,2,3};
            short[] res = Utils.sequence(new short[exp.length], (short)1, (short)1);
            assertArrayEquals(exp, res);
        }
        {
            short[] exp = {2,4,6};
            short[] res = Utils.sequence(new short[exp.length], (short)2, (short)2);
            assertArrayEquals(exp, res);
        }
        {
            short[] exp = {3,1,-1,-3};
            short[] res = Utils.sequence(new short[exp.length], (short)3, (short)-2);
            assertArrayEquals(exp, res);
        }
    }
    /**
     * Test of sequence method, of class Utils.
     */
    @Test
    public void testSequence_3args_3()
    {
        {
            char[] exp = {1,2,3};
            char[] res = Utils.sequence(new char[exp.length], (char)1, (char)1);
            assertArrayEquals(exp, res);
        }
        {
            char[] exp = {2,4,6};
            char[] res = Utils.sequence(new char[exp.length], (char)2, (char)2);
            assertArrayEquals(exp, res);
        }
        {
            char[] exp = {9,7,5,3};
            char[] res = Utils.sequence(new char[exp.length], (char)9, -2);
            assertArrayEquals(exp, res);
        }
    }
    /**
     * Test of sequence method, of class Utils.
     */
    @Test
    public void testSequence_3args_4()
    {
        {
            int[] exp = {1,2,3};
            int[] res = Utils.sequence(new int[exp.length], 1, 1);
            assertArrayEquals(exp, res);
        }
        {
            int[] exp = {2,4,6};
            int[] res = Utils.sequence(new int[exp.length], 2, 2);
            assertArrayEquals(exp, res);
        }
        {
            int[] exp = {3,1,-1,-3};
            int[] res = Utils.sequence(new int[exp.length], 3, -2);
            assertArrayEquals(exp, res);
        }
    }
    /**
     * Test of sequence method, of class Utils.
     */
    @Test
    public void testSequence_3args_5()
    {
        {
            long[] exp = {1,2,3};
            long[] res = Utils.sequence(new long[exp.length], 1, 1);
            assertArrayEquals(exp, res);
        }
        {
            long[] exp = {2,4,6};
            long[] res = Utils.sequence(new long[exp.length], 2, 2);
            assertArrayEquals(exp, res);
        }
        {
            long[] exp = {3,1,-1,-3};
            long[] res = Utils.sequence(new long[exp.length], 3, -2);
            assertArrayEquals(exp, res);
        }
    }

    /**
     * Test of sequence method, of class Utils.
     */
    @Test
    public void testSequence_3args_6()
    {
        {
            double[] exp = {1,2,3};
            double[] res = Utils.sequence(new double[exp.length], 1, 1);
            assertArrayEquals(exp, res, 0.0);
        }
        {
            double[] exp = {2,4,6};
            double[] res = Utils.sequence(new double[exp.length], 2, 2);
            assertArrayEquals(exp, res, 0.0);
        }
        {
            double[] exp = {3,1,-1,-3};
            double[] res = Utils.sequence(new double[exp.length], 3, -2);
            assertArrayEquals(exp, res, 0.0);
        }
    }


    /**
     * Test of bound method, of class Utils.
     */
    @Test
    public void testBound_3args_1()
    {
        byte z = 0;
        byte c = 100;
        byte n = -1;
        byte m = 50;
        byte o = 127;
        assertEquals(z, Utils.bound(z, c, n));
        assertEquals(z, Utils.bound(z, c, z));
        assertEquals(m, Utils.bound(z, c, m));
        assertEquals(c, Utils.bound(z, c, c));
        assertEquals(c, Utils.bound(z, c, o));
        assertEquals(c, Utils.bound(c, z, o));
    }

    /**
     * Test of bound method, of class Utils.
     */
    @Test
    public void testBound_3args_2()
    {
        short z = 0;
        short c = 100;
        short n = -1;
        short m = 50;
        short o = 127;
        assertEquals(z, Utils.bound(z, c, n));
        assertEquals(z, Utils.bound(z, c, z));
        assertEquals(m, Utils.bound(z, c, m));
        assertEquals(c, Utils.bound(z, c, c));
        assertEquals(c, Utils.bound(z, c, o));
        assertEquals(c, Utils.bound(c, z, o));
    }

    /**
     * Test of bound method, of class Utils.
     */
    @Test
    public void testBound_3args_3()
    {
        int z = 0;
        int c = 100;
        int n = -1;
        int m = 50;
        int o = 127;
        assertEquals(z, Utils.bound(z, c, n));
        assertEquals(z, Utils.bound(z, c, z));
        assertEquals(m, Utils.bound(z, c, m));
        assertEquals(c, Utils.bound(z, c, c));
        assertEquals(c, Utils.bound(z, c, o));
        assertEquals(c, Utils.bound(c, z, o));
    }

    /**
     * Test of bound method, of class Utils.
     */
    @Test
    public void testBound_3args_4()
    {
        long z = 0;
        long c = 100;
        long n = -1;
        long m = 50;
        long o = 127;
        assertEquals(z, Utils.bound(z, c, n));
        assertEquals(z, Utils.bound(z, c, z));
        assertEquals(m, Utils.bound(z, c, m));
        assertEquals(c, Utils.bound(z, c, c));
        assertEquals(c, Utils.bound(z, c, o));
        assertEquals(c, Utils.bound(c, z, o));
    }

    /**
     * Test of bound method, of class Utils.
     */
    @Test
    public void testBound_3args_5()
    {
        float z = 0;
        float c = 100;
        float n = -1;
        float m = 50;
        float o = 127;
        assertEquals(z, Utils.bound(z, c, n), 0.0f);
        assertEquals(z, Utils.bound(z, c, z), 0.0f);
        assertEquals(m, Utils.bound(z, c, m), 0.0f);
        assertEquals(c, Utils.bound(z, c, c), 0.0f);
        assertEquals(c, Utils.bound(z, c, o), 0.0f);
        assertEquals(c, Utils.bound(c, z, o));
    }

    /**
     * Test of bound method, of class Utils.
     */
    @Test
    public void testBound_3args_6()
    {
        double z = 0;
        double c = 100;
        double n = -1;
        double m = 50;
        double o = 127;
        assertEquals(z, Utils.bound(z, c, n), 0.0);
        assertEquals(z, Utils.bound(z, c, z), 0.0);
        assertEquals(m, Utils.bound(z, c, m), 0.0);
        assertEquals(c, Utils.bound(z, c, c), 0.0);
        assertEquals(c, Utils.bound(z, c, o), 0.0);
        assertEquals(c, Utils.bound(c, z, o));
    }

        /**
     * Test of bound method, of class Utils.
     */
    @Test
    public void testBound_3args_7()
    {
        BigInteger z = BigInteger.ZERO;
        BigInteger c = BigInteger.valueOf(100);
        BigInteger n = BigInteger.valueOf(-1);
        BigInteger m = BigInteger.valueOf(50);
        BigInteger o = BigInteger.valueOf(127);
        assertEquals(z, Utils.bound(z, c, n));
        assertEquals(z, Utils.bound(z, c, z));
        assertEquals(m, Utils.bound(z, c, m));
        assertEquals(c, Utils.bound(z, c, c));
        assertEquals(c, Utils.bound(z, c, o));
        assertEquals(c, Utils.bound(c, z, o));
    }

    /**
     * Test of bound method, of class Utils.
     */
    @Test
    public void testBound_3args_8()
    {
        BigDecimal z = BigDecimal.ZERO;
        BigDecimal c = BigDecimal.valueOf(100);
        BigDecimal n = BigDecimal.valueOf(-1);
        BigDecimal m = BigDecimal.valueOf(50);
        BigDecimal o = BigDecimal.valueOf(127);
        assertEquals(z, Utils.bound(z, c, n));
        assertEquals(z, Utils.bound(z, c, z));
        assertEquals(m, Utils.bound(z, c, m));
        assertEquals(c, Utils.bound(z, c, c));
        assertEquals(c, Utils.bound(z, c, o));
        assertEquals(c, Utils.bound(c, z, o));
    }
    

    /**
     * Test of unique method, of class Utils.
     */
    @Test
    public void testUnique_GenericType()
    {
        String[] u = {"1","3","2"};
        String[] r = {"1","3","2","1","2","3"};
        assertArrayEquals(u, Utils.unique(u));
        assertArrayEquals(u, Utils.unique(r));
    }

    /**
     * Test of unique method, of class Utils.
     */
    @Test
    public void testUnique_byteArr()
    {
        byte[] u = {1,3,2};
        byte[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Utils.unique(u));
        assertArrayEquals(u, Utils.unique(r));
    }

    /**
     * Test of unique method, of class Utils.
     */
    @Test
    public void testUnique_shortArr()
    {
        short[] u = {1,3,2};
        short[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Utils.unique(u));
        assertArrayEquals(u, Utils.unique(r));
    }

    /**
     * Test of unique method, of class Utils.
     */
    @Test
    public void testUnique_charArr()
    {
        char[] u = {1,3,2};
        char[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Utils.unique(u));
        assertArrayEquals(u, Utils.unique(r));
    }

    /**
     * Test of unique method, of class Utils.
     */
    @Test
    public void testUnique_intArr()
    {
        int[] u = {1,3,2};
        int[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Utils.unique(u));
        assertArrayEquals(u, Utils.unique(r));
    }

    /**
     * Test of unique method, of class Utils.
     */
    @Test
    public void testUnique_longArr()
    {
        long[] u = {1,3,2};
        long[] r = {1,3,2,1,2,3};
        assertArrayEquals(u, Utils.unique(u));
        assertArrayEquals(u, Utils.unique(r));
    }

    /**
     * Test of unique method, of class Utils.
     */
    @Test
    public void testUnique_doubleArr()
    {
        double[] u = {1, 3, 2};
        double[] r = {1, 3, 2, 1, 2, 3};
        assertArrayEquals(u, Utils.unique(u), 0.0);
        assertArrayEquals(u, Utils.unique(r), 0.0);
    }


    @Test
    public void testMin()
    {
        {
            long t0 = System.currentTimeMillis();
            long t1 = t0+60_000;
            long t2 = t1+60_000;
            long t3 = t2+60_000;
            Date a = new Date(t0);
            Date a2 = new Date(t0);
            Date b = new Date(t1);
            Date c = new Date(t2);
            Date d = new Date(t3);

            assertNull(Utils.min());

            assertEquals(a, Utils.min(a));

            assertEquals(a, Utils.min(a, a2));
            assertEquals(a, Utils.min(a, b));
            assertEquals(a, Utils.min(b, a));

            assertEquals(a, Utils.min(a, b, c));
            assertEquals(a, Utils.min(b, c, a));
            assertEquals(a, Utils.min(c, b, a));

            assertEquals(a, Utils.min(a, b, c, d));
            assertEquals(a, Utils.min(b, c, a, d));
            assertEquals(a, Utils.min(d, c, b, a));
        }        
        {
            Long a = 0L;
            Long a2 = 0L;
            Long b = 1L;
            Long c = 2L;
            Long d = 3L;

            assertNull(Utils.min());

            assertEquals(a, Utils.min(a));

            assertEquals(a, Utils.min(a, a2));
            assertEquals(a, Utils.min(a, b));
            assertEquals(a, Utils.min(b, a));

            assertEquals(a, Utils.min(a, b, c));
            assertEquals(a, Utils.min(b, c, a));
            assertEquals(a, Utils.min(c, b, a));

            assertEquals(a, Utils.min(a, b, c, d));
            assertEquals(a, Utils.min(b, c, a, d));
            assertEquals(a, Utils.min(d, c, b, a));
        }        
    }

    @Test
    public void testMax()
    {
        {
            long t0 = System.currentTimeMillis();
            long t1 = t0+60_000;
            long t2 = t1+60_000;
            long t3 = t2+60_000;
            Date a = new Date(t0);
            Date a2 = new Date(t0);
            Date b = new Date(t1);
            Date c = new Date(t2);
            Date d = new Date(t3);

            assertNull(Utils.max());

            assertEquals(a, Utils.max(a));

            assertEquals(a, Utils.max(a, a2));
            assertEquals(b, Utils.max(a, b));
            assertEquals(b, Utils.max(b, a));

            assertEquals(c, Utils.max(a, b, c));
            assertEquals(c, Utils.max(b, c, a));
            assertEquals(c, Utils.max(c, b, a));

            assertEquals(d, Utils.max(a, b, c, d));
            assertEquals(d, Utils.max(b, c, a, d));
            assertEquals(d, Utils.max(d, c, b, a));
        }
        {
            Long a = 0L;
            Long a2 = 0L;
            Long b = 1L;
            Long c = 2L;
            Long d = 3L;

            assertNull(Utils.max());

            assertEquals(a, Utils.max(a));

            assertEquals(a, Utils.max(a, a2));
            assertEquals(b, Utils.max(a, b));
            assertEquals(b, Utils.max(b, a));

            assertEquals(c, Utils.max(a, b, c));
            assertEquals(c, Utils.max(b, c, a));
            assertEquals(c, Utils.max(c, b, a));

            assertEquals(d, Utils.max(a, b, c, d));
            assertEquals(d, Utils.max(b, c, a, d));
            assertEquals(d, Utils.max(d, c, b, a));
        }
    }

    /**
     * Test of nextGaussian method, of class Utils.
     */
    @Test
    public void testNextGaussian_6args_1()
    {
        Random random = new Random(0);

        {
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for(int i=0;i<100;i++)
            {
                double value = Utils.nextGaussian(random, 5.0, 2.0, 0.0, 9.0, false);
                sum+= value;
                min = Math.min(value, min);
                max = Math.max(value, max);
            }

            double mean = sum/100.0;

            assertEquals(5, mean, 0.3);
            assertEquals(0, min, 0.3);
            assertEquals(9, max, 0.3);
        }
        {
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for(int i=0;i<100;i++)
            {
                double value = Utils.nextGaussian(random, 5.0, 2.0, 0.0, 9.0, true);
                sum+= value;
                min = Math.min(value, min);
                max = Math.max(value, max);
            }

            double mean = sum/100.0;

            assertEquals(5, mean, 2);
            assertEquals(5, min, 0.3);
            assertEquals(9, max, 0.3);
        }
        
    }

    /**
     * Test of nextGaussian method, of class Utils.
     */
    @Test
    public void testNextGaussian_6args_2()
    {
        Random random = new Random(0);

        {
            int sum = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for(int i=0;i<100;i++)
            {
                int value = Utils.nextGaussian(random, 5, 2, 0, 9, false);
                sum+= value;
                min = Math.min(value, min);
                max = Math.max(value, max);
            }

            double mean = sum/100.0;

            assertEquals(5, mean, 0.3);
            assertEquals(0, min);
            assertEquals(9, max);
        }
        {
            int sum = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for(int i=0;i<100;i++)
            {
                int value = Utils.nextGaussian(random, 5, 2, 0, 9, true);
                sum+= value;
                min = Math.min(value, min);
                max = Math.max(value, max);
            }

            double mean = sum/100.0;

            assertEquals(5, mean, 2);
            assertEquals(5, min);
            assertEquals(9, max);
        }
    }
    /**
     * Test of nextGaussian method, of class Utils.
     */
    @Test
    public void testNextGaussian_6args_3()
    {
        Random random = new Random(0);

        {
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;

            for(int i=0;i<100;i++)
            {
                long value = Utils.nextGaussian(random, 5L, 2L, 0L, 9L, false);
                sum+= value;
                min = Math.min(value, min);
                max = Math.max(value, max);
            }

            double mean = sum/100.0;

            assertEquals(5, mean, 2);
            assertEquals(0, min);
            assertEquals(9, max);
        }
        {
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;

            for(int i=0;i<100;i++)
            {
                long value = Utils.nextGaussian(random, 5L, 2L, 0L, 9L, true);
                sum+= value;
                min = Math.min(value, min);
                max = Math.max(value, max);
            }

            double mean = sum/100.0;

            assertEquals(5, mean, 2);
            assertEquals(5, min);
            assertEquals(9, max);
        }
    }

    /**
     * Test of increment method, of class Utils.
     */
    @Test
    public void testIncrementByte()
    {
        byte[] b   = {0,0,0,0,0};
        byte[] b01 = {0,0,0,0,1};
        byte[] b02 = {0,0,0,0,2};
        byte[] b10 = {0,0,0,1,0};
        byte[] b11 = {0,0,0,1,1};
        byte[] b12 = {0,0,0,1,2};
        byte[] b20 = {0,0,0,2,0};
        byte[] b21 = {0,0,0,2,1};
        byte[] bc  = {0,0,0,0,0};
        
        byte module = 3;
        
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b01, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b02, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b10, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b11, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b12, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b20, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b21, b);
        assertFalse(Nums.inc(b, module));
        
        b = new byte[3];
        
        for(int i=0;i<0xfff;i++)
        {
            assertFalse(Nums.inc(b, (byte)16));
        }
        assertTrue(Nums.inc(b, (byte)16));
        byte[] b0 = new byte[3];
        assertArrayEquals(b0, b);
    }
    /**
     * Test of increment method, of class Utils.
     */
    @Test
    public void testIncrementShort()
    {
        short[] b   = {0,0,0,0,0};
        short[] b01 = {0,0,0,0,1};
        short[] b02 = {0,0,0,0,2};
        short[] b10 = {0,0,0,1,0};
        short[] b11 = {0,0,0,1,1};
        short[] b12 = {0,0,0,1,2};
        short[] b20 = {0,0,0,2,0};
        short[] b21 = {0,0,0,2,1};
        short[] bc  = {0,0,0,0,0};
        
        short module = 3;
        
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b01, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b02, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b10, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b11, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b12, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b20, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b21, b);
        assertFalse(Nums.inc(b, module));
        
        b = new short[3];
        
        for(int i=0;i<0xfff;i++)
        {
            assertFalse(Nums.inc(b, (short)16));
        }
        assertTrue(Nums.inc(b, (short)16));
        short[] b0 = new short[3];
        assertArrayEquals(b0, b);
    }
    /**
     * Test of increment method, of class Utils.
     */
    @Test
    public void testIncrementInt()
    {
        int[] b   = {0,0,0,0,0};
        int[] b01 = {0,0,0,0,1};
        int[] b02 = {0,0,0,0,2};
        int[] b10 = {0,0,0,1,0};
        int[] b11 = {0,0,0,1,1};
        int[] b12 = {0,0,0,1,2};
        int[] b20 = {0,0,0,2,0};
        int[] b21 = {0,0,0,2,1};
        int[] bc  = {0,0,0,0,0};
        
        byte module = 3;
        
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b01, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b02, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b10, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b11, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b12, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b20, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b21, b);
        assertFalse(Nums.inc(b, module));
        
        b = new int[3];
        
        for(int i=0;i<0xfff;i++)
        {
            assertFalse(Nums.inc(b, 16));
        }
        assertTrue(Nums.inc(b, 16));
        int[] b0 = new int[3];
        assertArrayEquals(b0, b);
    }
    /**
     * Test of increment method, of class Utils.
     */
    @Test
    public void testIncrementLong()
    {
        long[] b   = {0,0,0,0,0};
        long[] b01 = {0,0,0,0,1};
        long[] b02 = {0,0,0,0,2};
        long[] b10 = {0,0,0,1,0};
        long[] b11 = {0,0,0,1,1};
        long[] b12 = {0,0,0,1,2};
        long[] b20 = {0,0,0,2,0};
        long[] b21 = {0,0,0,2,1};
        long[] bc  = {0,0,0,0,0};
        
        long module = 3;
        
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b01, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b02, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b10, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b11, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b12, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b20, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b21, b);
        assertFalse(Nums.inc(b, module));
        
        b = new long[3];
        
        for(int i=0;i<0xfff;i++)
        {
            assertFalse(Nums.inc(b, 16));
        }
        assertTrue(Nums.inc(b, 16));
        long[] b0 = new long[3];
        assertArrayEquals(b0, b);
    }
    

    /**
     * Test of toJava method, of class Utils.
     */
    @Test
    public void testToJava()
    {
        byte[] data0 = { 0, 1, 2, 3, -1, -2, -3};
        String expResult0 = "byte[] data = { 0, 1, 2, 3, -1, -2, -3}";
        String result0 = Utils.toJava(data0);
        assertEquals(expResult0, result0);
        
        byte[] data1 = { };
        String expResult1 = "byte[] data = {}";
        String result1 = Utils.toJava(data1);
        assertEquals(expResult1, result1);
        
    }

    /**
     * Test of xor method, of class SimpleAbstractCipher.
     */
    @Test
    public void testXor()
    {
        byte[] x0 = {3,  1, 1};
        byte[] y0 = {12, 1, 2, 3};
        byte[] xy0= {15, 0, 3, 3};
                
        byte[] result0 = Utils.xor(x0, y0);
        assertArrayEquals(xy0, result0);

        byte[] result = Utils.xor(y0, x0);
        assertArrayEquals(xy0, result);
    }

    /**
     * Test of rollXor method, of class Utils.
     */
    @Test
    public void testRollXor()
    {
        long xor = 1234567890L;
        byte[] data = {0, 1, 2, 3, 4};
        assertArrayEquals(data, Utils.rollXor(Utils.rollXor(data, xor), xor));
    }



    /**
     * Test of equals method, of class Utils.
     */
    @Test
    public void testEquals_2args_1()
    {
        Object e1 = 1;
        Object e2 = 2;
        Object e22 = 2;
        
        assertFalse(Utils.equals(e1, e2));
        assertTrue(Utils.equals(e2, e22));
        
        Object en1 = null;
        Object en2 = null;
        assertTrue(Utils.equals(en1, en2));

        assertFalse(Utils.equals(e1, en1));
        assertFalse(Utils.equals(en1, e1));
    }

    enum Dummy{ A, B, C};
    /**
     * Test of equals method, of class Utils.
     */
    @Test
    public void testEquals_2args_2()
    {
        Dummy dummyNull = null;
        assertTrue(Utils.equals(dummyNull, dummyNull));
        assertTrue(Utils.equals(Dummy.A, Dummy.A));
        
        assertFalse(Utils.equals(Dummy.A, dummyNull));
        assertFalse(Utils.equals(dummyNull, Dummy.A));
        assertFalse(Utils.equals(Dummy.A, Dummy.B));
    }


    /**
     * Test of iso method, of class Utils.
     */
    @Test
    public void testFormat_double_int()
    {
        assertEquals("0", Utils.format(0, 0));
        assertEquals("1.0", Utils.format(1, 1));
        assertEquals("2.00", Utils.format(2, 2));

        assertEquals("4", Utils.format(3.5, 0));
        assertEquals("1234.50", Utils.format(1234.5, 2));

        assertEquals("12345.68", Utils.format(12345.678, 2));
    }

    /**
     * Test of iso method, of class Utils.
     */
    @Test
    public void testFormat_3args()
    {
        assertEquals("0.01", Utils.format(0.01, 2, '_'));
        assertEquals("0.21", Utils.format(0.21, 2, '_'));
        assertEquals("3.21", Utils.format(3.21, 2, '_'));
        assertEquals("43.21", Utils.format(43.21, 2, '_'));
        assertEquals("543.21", Utils.format(543.21, 2, '_'));
        assertEquals("6543.21", Utils.format(6543.21, 2, '_'));
        assertEquals("76_543.21", Utils.format(76_543.21, 2, '_'));
        assertEquals("876_543.21", Utils.format(876_543.21, 2, '_'));
        assertEquals("9_876_543.21", Utils.format(9_876_543.21, 2, '_'));

        assertEquals("-0.01", Utils.format(-0.01, 2, '_'));
        assertEquals("-0.21", Utils.format(-0.21, 2, '_'));
        assertEquals("-3.21", Utils.format(-3.21, 2, '_'));
        assertEquals("-43.21", Utils.format(-43.21, 2, '_'));
        assertEquals("-543.21", Utils.format(-543.21, 2, '_'));
        assertEquals("-6543.21", Utils.format(-6543.21, 2, '_'));
        assertEquals("-76_543.21", Utils.format(-76_543.21, 2, '_'));
        assertEquals("-876_543.21", Utils.format(-876_543.21, 2, '_'));
        assertEquals("-9_876_543.21", Utils.format(-9_876_543.21, 2, '_'));

    }


    /**
     * Test of equivalent method, of class Utils.
     */
    @Test
    public void testEquivalent_4args()
    {
        Integer[] keys = {1,2,3,4,5, null};
        String[] values = {"1","2","3","4","5","null"};

        assertNull(Utils.equivalent(0, keys, values, null));
        assertEquals("0", Utils.equivalent(0, keys, values, "0"));
        assertEquals("1", Utils.equivalent(1, keys, values, "0"));
        assertEquals("2", Utils.equivalent(2, keys, values, "0"));
        assertEquals("null", Utils.equivalent(null, keys, values, "0"));
        
        String[] values2 = {"16","32","43","58","58c","64","91"};

        assertEquals("16", Utils.equivalent(Encoding.Type.Base16, Encoding.Type.values(), values2, "0"));
        assertEquals("32", Utils.equivalent(Encoding.Type.Base32, Encoding.Type.values(), values2, "0"));
        assertEquals("43", Utils.equivalent(Encoding.Type.Base43, Encoding.Type.values(), values2, "0"));
        assertEquals("58", Utils.equivalent(Encoding.Type.Base58, Encoding.Type.values(), values2, "0"));
        assertEquals("58c", Utils.equivalent(Encoding.Type.Base58Check, Encoding.Type.values(), values2, "0"));
        assertEquals("64", Utils.equivalent(Encoding.Type.Base64, Encoding.Type.values(), values2, "0"));
        assertEquals("91", Utils.equivalent(Encoding.Type.Base91, Encoding.Type.values(), values2, "0"));
    }

    /**
     * Test of equivalent method, of class Utils.
     */
    @Test
    public void testEquivalent_3args()
    {
        Integer[] keys = {1,2,3,4,5, null};
        String[] values = {"1","2","3","4","5","null"};

        assertNull(Utils.equivalent(0, keys, values));
        assertEquals("1", Utils.equivalent(1, keys, values));
        assertEquals("2", Utils.equivalent(2, keys, values));
        assertEquals("null", Utils.equivalent(null, keys, values));
        
        String[] values2 = {"16","32","43","58","58c","64","91"};

        assertEquals("16", Utils.equivalent(Encoding.Type.Base16, Encoding.Type.values(), values2));
        assertEquals("43", Utils.equivalent(Encoding.Type.Base43, Encoding.Type.values(), values2));
        assertEquals("58", Utils.equivalent(Encoding.Type.Base58, Encoding.Type.values(), values2));
        assertEquals("58c", Utils.equivalent(Encoding.Type.Base58Check, Encoding.Type.values(), values2));
        assertEquals("64", Utils.equivalent(Encoding.Type.Base64, Encoding.Type.values(), values2));
        assertEquals("91", Utils.equivalent(Encoding.Type.Base91, Encoding.Type.values(), values2));
    }
    
    /**
     * Test of isPositive method, of class Utils.
     */
    @Test
    public void testIsPositive_BigInteger()
    {
        assertTrue(Utils.isPositive(BigInteger.ONE));
        assertTrue(Utils.isPositive(BigInteger.TEN));
        assertFalse(Utils.isPositive(BigInteger.ZERO));
        assertFalse(Utils.isPositive(BigInteger.valueOf(-1)));
    }

    /**
     * Test of isPositive method, of class Utils.
     */
    @Test
    public void testIsPositive_BigDecimal()
    {
        assertTrue(Utils.isPositive(BigDecimal.ONE));
        assertTrue(Utils.isPositive(BigDecimal.TEN));
        assertFalse(Utils.isPositive(BigDecimal.ZERO));
        assertFalse(Utils.isPositive(BigDecimal.valueOf(-1)));
    }

    /**
     * Test of isPositiveOrZero method, of class Utils.
     */
    @Test
    public void testIsPositiveOrZero_BigInteger()
    {
        assertTrue(Utils.isPositiveOrZero(BigInteger.ONE));
        assertTrue(Utils.isPositiveOrZero(BigInteger.TEN));
        assertTrue(Utils.isPositiveOrZero(BigInteger.ZERO));
        assertFalse(Utils.isPositiveOrZero(BigInteger.valueOf(-1)));
    }

    /**
     * Test of isPositiveOrZero method, of class Utils.
     */
    @Test
    public void testIsPositiveOrZero_BigDecimal()
    {
        assertTrue(Utils.isPositiveOrZero(BigDecimal.ONE));
        assertTrue(Utils.isPositiveOrZero(BigDecimal.TEN));
        assertTrue(Utils.isPositiveOrZero(BigDecimal.ZERO));
        assertFalse(Utils.isPositiveOrZero(BigDecimal.valueOf(-1)));
    }

    /**
     * Test of isNegative method, of class Utils.
     */
    @Test
    public void testIsNegative_BigInteger()
    {
        assertTrue(Utils.isNegative(BigInteger.valueOf(-1)));
        assertTrue(Utils.isNegative(BigInteger.valueOf(-10)));
        assertFalse(Utils.isNegative(BigInteger.ZERO));
        assertFalse(Utils.isNegative(BigInteger.ONE));
    }

    /**
     * Test of isNegative method, of class Utils.
     */
    @Test
    public void testIsNegative_BigDecimal()
    {
        assertTrue(Utils.isNegative(BigDecimal.valueOf(-1)));
        assertTrue(Utils.isNegative(BigDecimal.valueOf(-10)));
        assertFalse(Utils.isNegative(BigDecimal.ZERO));
        assertFalse(Utils.isNegative(BigDecimal.ONE));
    }

    /**
     * Test of isNegativeOrZero method, of class Utils.
     */
    @Test
    public void testIsNegativeOrZero_BigInteger()
    {
        assertTrue(Utils.isNegativeOrZero(BigInteger.valueOf(-1)));
        assertTrue(Utils.isNegativeOrZero(BigInteger.valueOf(-10)));
        assertTrue(Utils.isNegativeOrZero(BigInteger.ZERO));
        assertFalse(Utils.isNegativeOrZero(BigInteger.ONE));
    }

    /**
     * Test of isNegativeOrZero method, of class Utils.
     */
    @Test
    public void testIsNegativeOrZero_BigDecimal()
    {
        assertTrue(Utils.isNegativeOrZero(BigDecimal.valueOf(-1)));
        assertTrue(Utils.isNegativeOrZero(BigDecimal.valueOf(-10)));
        assertTrue(Utils.isNegativeOrZero(BigDecimal.ZERO));
        assertFalse(Utils.isNegativeOrZero(BigDecimal.ONE));
    }

    /**
     * Test of isZero method, of class Utils.
     */
    @Test
    public void testIsZero_BigDecimal()
    {
        assertFalse(Utils.isZero(BigDecimal.valueOf(-1)));
        assertTrue(Utils.isZero(BigDecimal.ZERO));
        assertFalse(Utils.isZero(BigDecimal.ONE));
    }

    /**
     * Test of isZero method, of class Utils.
     */
    @Test
    public void testIsZero_BigDecimal_BigDecimal()
    {
        BigDecimal delta = new BigDecimal("0.002");
        assertFalse(Utils.isZero(BigDecimal.valueOf(-1), delta));
        assertTrue(Utils.isZero(BigDecimal.ZERO, delta));
        assertFalse(Utils.isZero(BigDecimal.ONE, delta));

        assertFalse(Utils.isZero(new BigDecimal(0.003), delta));
        assertTrue(Utils.isZero(new BigDecimal(0.001), delta));
        assertTrue(Utils.isZero(new BigDecimal(-0.001), delta));
        assertFalse(Utils.isZero(new BigDecimal(-0.003), delta));
    }

    /**
     * Test of isNullOrZero method, of class Utils.
     */
    @Test
    public void testIsNullOrZero_BigDecimal()
    {
        assertFalse(Utils.isNullOrZero(BigDecimal.valueOf(-1)));
        assertFalse(Utils.isNullOrZero(BigDecimal.valueOf(-1)));
        assertTrue(Utils.isNullOrZero(BigDecimal.ZERO));
        assertFalse(Utils.isNullOrZero(BigDecimal.ONE));
    }

    /**
     * Test of isNullOrZero method, of class Utils.
     */
    @Test
    public void testIsNullOrZero_BigDecimal_BigDecimal()
    {
        BigDecimal delta = new BigDecimal("0.002");
        assertFalse(Utils.isNullOrZero(BigDecimal.valueOf(-1), delta));
        assertTrue(Utils.isNullOrZero(BigDecimal.ZERO, delta));
        assertFalse(Utils.isNullOrZero(BigDecimal.ONE, delta));

        assertFalse(Utils.isNullOrZero(new BigDecimal(0.003), delta));
        assertTrue(Utils.isNullOrZero(new BigDecimal(0.001), delta));
        assertTrue(Utils.isNullOrZero(new BigDecimal(-0.001), delta));
        assertFalse(Utils.isNullOrZero(new BigDecimal(-0.003), delta));
    }


    @Test
    public void testBitSet()
    {
        assertEquals(0b00000001, Bits.bitSet((byte) 0b00000000, 0, true));
        assertEquals(0b00000010, Bits.bitSet((byte) 0b00000000, 1, true));
        assertEquals(0b00000100, Bits.bitSet((byte) 0b00000000, 2, true));
        assertEquals(0b00001000, Bits.bitSet((byte) 0b00000000, 3, true));
        assertEquals(0b00010000, Bits.bitSet((byte) 0b00000000, 4, true));
        assertEquals(0b00100000, Bits.bitSet((byte) 0b00000000, 5, true));
        assertEquals(0b01000000, Bits.bitSet((byte) 0b00000000, 6, true));
        assertEquals((byte) 0b10000000, Bits.bitSet((byte) 0b00000000, 7, true));

        assertEquals((byte) 0b11111110, Bits.bitSet((byte) 0b11111111, 0, false));
        assertEquals((byte) 0b11111101, Bits.bitSet((byte) 0b11111111, 1, false));
        assertEquals((byte) 0b11111011, Bits.bitSet((byte) 0b11111111, 2, false));
        assertEquals((byte) 0b11110111, Bits.bitSet((byte) 0b11111111, 3, false));
        assertEquals((byte) 0b11101111, Bits.bitSet((byte) 0b11111111, 4, false));
        assertEquals((byte) 0b11011111, Bits.bitSet((byte) 0b11111111, 5, false));
        assertEquals((byte) 0b10111111, Bits.bitSet((byte) 0b11111111, 6, false));
        assertEquals((byte) 0b01111111, Bits.bitSet((byte) 0b11111111, 7, false));
    }

    @Test
    public void testNextGaussian()
    {
        Random random = new Random(0);
        for (int i = 0; i < 1000; i++)
        {
            BigDecimal n = Utils.nextGaussian(random, BigDecimal.valueOf(500), BigDecimal.valueOf(500 / 3), BigDecimal.ONE, BigDecimal.valueOf(500), true);
        }
    }

    /**
     * Test of between method, of class JavaTime.
     */
    @Test
    public void testBetween()
    {
        {
            BigDecimal a = BigDecimal.ZERO;
            BigDecimal b = BigDecimal.ONE;
            BigDecimal c = BigDecimal.TEN;

            assertTrue(Utils.between(a, a, a));
            assertTrue(Utils.between(a, c, b));
            assertTrue(Utils.between(a, b, b));
            assertTrue(Utils.between(a, b, a));
            assertTrue(Utils.between(c, a, b));

            assertFalse(Utils.between(a, b, c));
            assertFalse(Utils.between(b, c, a));
        }
        {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime a = now.minusDays(10);
            LocalDateTime b = now;
            LocalDateTime c = now.plusDays(10);

            assertTrue(Utils.between(a, a, a));
            assertTrue(Utils.between(a, c, b));
            assertTrue(Utils.between(a, b, b));
            assertTrue(Utils.between(a, b, a));
            assertTrue(Utils.between(c, a, b));

            assertFalse(Utils.between(a, b, c));
            assertFalse(Utils.between(b, c, a));
        }
    }

    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_3args_1()
    {
        byte[] values = {0, 0, 1, 10, 20};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values, 1, 4));
    }
    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_3args_2()
    {
        byte[][] values = {{0}, {0}, {1}, {10}, {20}};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values, 1, 4));
    }

    final BigInteger[] VALUES = {BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN};
    
    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_byteArr()
    {
        byte[] values = {0, 1, 10};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values));
    }

    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_3args_3()
    {
        char[] values = {0, 0, 1, 10, 20};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values, 1, 4));
    }

    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_charArr()
    {
        char[] values = {0, 1, 10};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values));
    }

    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_3args_4()
    {
        int[] values = {0, 0, 1, 10, 20};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values, 1, 4));
    }

    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_intArr()
    {
        int[] values = {0, 1, 10};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values));
    }

    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_3args_5()
    {
        long[] values = {0, 0, 1, 10, 20};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values, 1, 4));
    }

    /**
     * Test of asBigIntegers method, of class Utils.
     */
    @Test
    public void testAsBigIntegers_longArr()
    {
        long[] values = {0L, 1L, 10L};
        assertArrayEquals(VALUES, Utils.asBigIntegers(values));
    }

    /**
     * Test of asBytes method, of class Utils.
     */
    @Test
    public void testAsBytes_BigInteger_int()
    {
        assertNull(Utils.asBytes((BigInteger)null, 0));
        assertEquals(5, Utils.asBytes(BigInteger.ZERO, 5).length);
        assertEquals(5, Utils.asBytes(BigInteger.ONE, 5).length);
        assertEquals(5, Utils.asBytes(BigInteger.TEN, 5).length);
        assertEquals(8, Utils.asBytes(BigInteger.valueOf(Long.MAX_VALUE), 5).length);
    } 

    /**
     * Test of firstNonNullOrPoison method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrPoison_2args_1()
    {
        assertEquals(BigDecimal.ONE, Utils.firstNonNullOrPoison(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN));
        assertNull(Utils.firstNonNullOrPoison(BigDecimal.ZERO, BigDecimal.ZERO));
        assertNull(Utils.firstNonNullOrPoison(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    /**
     * Test of firstNonNullOrPoison method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrPoison_2args_2()
    {
        BigDecimal[] poison = {BigDecimal.ZERO, BigDecimal.ONE};
        assertEquals(BigDecimal.TEN, Utils.firstNonNullOrPoison(poison, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN));
        assertNull(Utils.firstNonNullOrPoison(poison));
        assertNull(Utils.firstNonNullOrPoison(poison, BigDecimal.ZERO, BigDecimal.ONE));
    }
    
    /**
     * Test of firstNonNullOrPoison method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrPoisonLenient_3args_1()
    {
        assertEquals(BigDecimal.ONE, Utils.firstNonNullOrPoisonLenient(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN));
        assertEquals(BigDecimal.ZERO, Utils.firstNonNullOrPoisonLenient(BigDecimal.ZERO, BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO, Utils.firstNonNullOrPoisonLenient(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    /**
     * Test of firstNonNullOrPoison method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrPoisonLenient_3args_2()
    {
        BigDecimal[] poison = {BigDecimal.ZERO, BigDecimal.ONE};
        assertEquals(BigDecimal.TEN, Utils.firstNonNullOrPoisonLenient(poison, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN));
        assertNull(Utils.firstNonNullOrPoisonLenient(poison));
        assertEquals(BigDecimal.ZERO, Utils.firstNonNullOrPoisonLenient(poison, BigDecimal.ZERO, BigDecimal.ONE));
    }

    /**
     * Test of Comparator fields, of class Utils.
     */
    @Test
    public void testComparators() throws Exception
    {
        {
            List<String> a = Arrays.asList("a");
            List<String> b = Arrays.asList("a","b");
            List<String>[] array = new List[]{b,a};

            Arrays.sort(array, Utils.COLLECTION_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
        {
            String[] a = {"a"};
            String[] b = {"a","b"};
            String[][] array = {b,a};

            Arrays.sort(array, Utils.ARRAY_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
        {
            byte[] a = {1};
            byte[] b = {1,1};
            byte[][] array = {b,a};

            Arrays.sort(array, Utils.BYTE_ARRAY_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
        {
            int[] a = {1};
            int[] b = {1,1};
            int[][] array = {b,a};

            Arrays.sort(array, Utils.INT_ARRAY_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
        {
            long[] a = {1};
            long[] b = {1,1};
            long[][] array = {b,a};

            Arrays.sort(array, Utils.LONG_ARRAY_SIZE_COMPARATOR);

            assertTrue(a==array[0]);
            assertTrue(b==array[1]);
        }
    }

    @Test
    public void testUnion()
    {
        HashSet<String>[] sets = new HashSet[10];
        for(int i=0;i<10;i++)
        {
            sets[i] = new HashSet<>();
            for(int j=0;j<=i;j++)
            {
                sets[i].add(Integer.toString(j));
            }
        }
        Arrays.sort(sets, Utils.COLLECTION_SIZE_COMPARATOR);
        
        Set<String> result = Utils.union(sets);
        assertEquals(sets.length, result.size());
    }

    /**
     * Test of intersection method, of class Utils.
     */
    @Test
    public void testIntersection()
    {
        HashSet<String>[] sets = new HashSet[10];
        for(int i=0;i<10;i++)
        {
            sets[i] = new HashSet<>();
            for(int j=0;j<=i;j++)
            {
                sets[i].add(Integer.toString(j));
            }
        }
        Arrays.sort(sets, Utils.COLLECTION_SIZE_COMPARATOR);

        Set<String> result = Utils.intersection(sets);
        assertEquals(1, result.size());
    }

    /**
     * Test of append method, of class Utils.
     */
    @Test
    public void testAppend_List_GenericType()
    {
        List<String> exp = Arrays.asList("1","2","3","4","5");
        List<String> result = Utils.append(new ArrayList<>(), "1","2","3","4","5");
        assertEquals(exp, result);
    }

    /**
     * Test of append method, of class Utils.
     */
    @Test
    public void testAppend_Set_GenericType()
    {
        Set<String> exp = new HashSet<>(Arrays.asList("1","2","3","4","5"));
        Set<String> result = Utils.append(new HashSet<>(), "1","2","3","4","5");
        assertEquals(exp, result);
    }

    /**
     * Test of append method, of class Utils.
     */
    @Test
    public void testAppend_Queue_GenericType()
    {
        Queue<String> exp = new ConcurrentLinkedQueue<>(Arrays.asList("1","2","3","4","5"));
        Queue<String> result = Utils.append(new ConcurrentLinkedQueue<>(), "1","2","3","4","5");
        assertEquals(exp.size(), result.size());
    }

    @Test
    public void testCountColums_GenericType()
    {
        String[][] cells1 = {};
        String[][] cells2 = {null};
        String[][] cells3 = {{}};
        String[][] cells4 = {{"1"}};
        String[][] cells5 = {{"1","2"}};
        String[][] cells6 = {{},{"1","2"},null};
        String[][] cells7 = {null, {"1","2"}};
        String[][] cells8 = {null,null,null};
        
        assertEquals(0, Utils.countColums(cells1));
        assertEquals(0, Utils.countColums(cells2));
        assertEquals(0, Utils.countColums(cells3));
        assertEquals(1, Utils.countColums(cells4));
        assertEquals(2, Utils.countColums(cells5));
        assertEquals(2, Utils.countColums(cells6));
        assertEquals(2, Utils.countColums(cells7));
        assertEquals(0, Utils.countColums(cells8));
    }

    @Test
    public void testCountColums_byteArrArr()
    {
        byte[][] cells1 = {};
        byte[][] cells2 = {null};
        byte[][] cells3 = {{}};
        byte[][] cells4 = {{1}};
        byte[][] cells5 = {{1,2}};
        byte[][] cells6 = {{},{1,2},null};
        byte[][] cells7 = {null, {1,2}};
        byte[][] cells8 = {null,null,null};
        
        assertEquals(0, Utils.countColums(cells1));
        assertEquals(0, Utils.countColums(cells2));
        assertEquals(0, Utils.countColums(cells3));
        assertEquals(1, Utils.countColums(cells4));
        assertEquals(2, Utils.countColums(cells5));
        assertEquals(2, Utils.countColums(cells6));
        assertEquals(2, Utils.countColums(cells7));
        assertEquals(0, Utils.countColums(cells8));
    }

    @Test
    public void testCountColums_shortArrArr()
    {
        short[][] cells1 = {};
        short[][] cells2 = {null};
        short[][] cells3 = {{}};
        short[][] cells4 = {{1}};
        short[][] cells5 = {{1,2}};
        short[][] cells6 = {{},{1,2},null};
        short[][] cells7 = {null, {1,2}};
        short[][] cells8 = {null,null,null};
        
        assertEquals(0, Utils.countColums(cells1));
        assertEquals(0, Utils.countColums(cells2));
        assertEquals(0, Utils.countColums(cells3));
        assertEquals(1, Utils.countColums(cells4));
        assertEquals(2, Utils.countColums(cells5));
        assertEquals(2, Utils.countColums(cells6));
        assertEquals(2, Utils.countColums(cells7));
        assertEquals(0, Utils.countColums(cells8));
    }

    @Test
    public void testCountColums_charArrArr()
    {
        char[][] cells1 = {};
        char[][] cells2 = {null};
        char[][] cells3 = {{}};
        char[][] cells4 = {{1}};
        char[][] cells5 = {{1,2}};
        char[][] cells6 = {{},{1,2},null};
        char[][] cells7 = {null, {1,2}};
        char[][] cells8 = {null,null,null};
        
        assertEquals(0, Utils.countColums(cells1));
        assertEquals(0, Utils.countColums(cells2));
        assertEquals(0, Utils.countColums(cells3));
        assertEquals(1, Utils.countColums(cells4));
        assertEquals(2, Utils.countColums(cells5));
        assertEquals(2, Utils.countColums(cells6));
        assertEquals(2, Utils.countColums(cells7));
        assertEquals(0, Utils.countColums(cells8));
    }

    @Test
    public void testCountColums_intArrArr()
    {
        int[][] cells1 = {};
        int[][] cells2 = {null};
        int[][] cells3 = {{}};
        int[][] cells4 = {{1}};
        int[][] cells5 = {{1,2}};
        int[][] cells6 = {{},{1,2},null};
        int[][] cells7 = {null, {1,2}};
        int[][] cells8 = {null,null,null};
        
        assertEquals(0, Utils.countColums(cells1));
        assertEquals(0, Utils.countColums(cells2));
        assertEquals(0, Utils.countColums(cells3));
        assertEquals(1, Utils.countColums(cells4));
        assertEquals(2, Utils.countColums(cells5));
        assertEquals(2, Utils.countColums(cells6));
        assertEquals(2, Utils.countColums(cells7));
        assertEquals(0, Utils.countColums(cells8));
    }

    @Test
    public void testCountColums_longArrArr()
    {
        long[][] cells1 = {};
        long[][] cells2 = {null};
        long[][] cells3 = {{}};
        long[][] cells4 = {{1}};
        long[][] cells5 = {{1,2}};
        long[][] cells6 = {{},{1,2},null};
        long[][] cells7 = {null, {1,2}};
        long[][] cells8 = {null,null,null};
        
        assertEquals(0, Utils.countColums(cells1));
        assertEquals(0, Utils.countColums(cells2));
        assertEquals(0, Utils.countColums(cells3));
        assertEquals(1, Utils.countColums(cells4));
        assertEquals(2, Utils.countColums(cells5));
        assertEquals(2, Utils.countColums(cells6));
        assertEquals(2, Utils.countColums(cells7));
        assertEquals(0, Utils.countColums(cells8));
    }

    @Test
    public void testCountColums_floatArrArr()
    {
        float[][] cells1 = {};
        float[][] cells2 = {null};
        float[][] cells3 = {{}};
        float[][] cells4 = {{1}};
        float[][] cells5 = {{1,2}};
        float[][] cells6 = {{},{1,2},null};
        float[][] cells7 = {null, {1,2}};
        float[][] cells8 = {null,null,null};
        
        assertEquals(0, Utils.countColums(cells1));
        assertEquals(0, Utils.countColums(cells2));
        assertEquals(0, Utils.countColums(cells3));
        assertEquals(1, Utils.countColums(cells4));
        assertEquals(2, Utils.countColums(cells5));
        assertEquals(2, Utils.countColums(cells6));
        assertEquals(2, Utils.countColums(cells7));
        assertEquals(0, Utils.countColums(cells8));
    }

    @Test
    public void testCountColums_doubleArrArr()
    {
        double[][] cells1 = {};
        double[][] cells2 = {null};
        double[][] cells3 = {{}};
        double[][] cells4 = {{1}};
        double[][] cells5 = {{1,2}};
        double[][] cells6 = {{},{1,2},null};
        double[][] cells7 = {null, {1,2}};
        double[][] cells8 = {null,null,null};
        
        assertEquals(0, Utils.countColums(cells1));
        assertEquals(0, Utils.countColums(cells2));
        assertEquals(0, Utils.countColums(cells3));
        assertEquals(1, Utils.countColums(cells4));
        assertEquals(2, Utils.countColums(cells5));
        assertEquals(2, Utils.countColums(cells6));
        assertEquals(2, Utils.countColums(cells7));
        assertEquals(0, Utils.countColums(cells8));
    }

    @Test
    public void testExecute()
    {
        Runnable task = () ->
        {
            //do nothing
        };
        Thread result = Utils.execute(task, "name", true);
        assertEquals("name", result.getName());
        assertTrue(result.isDaemon());
    }

    @Test
    public void testHasNulls()
    {
        assertFalse(Utils.hasNulls());
        assertTrue(Utils.hasNulls((Object) null));
        assertFalse(Utils.hasNulls(new Object[0]));
        assertTrue(Utils.hasNulls(new Object[]{null}));
        assertTrue(Utils.hasNulls(new Object[]{null, ""}));
        assertFalse(Utils.hasNulls(new Object[]{"", ""}));
        assertTrue(Utils.hasNulls(new Object[]{"", null}));
    }

    @Test
    public void testHasNullsOrEmpty()
    {
        assertTrue(Utils.hasNullsOrEmpty());
        assertTrue(Utils.hasNullsOrEmpty((Object) null));
        assertTrue(Utils.hasNullsOrEmpty(new Object[0]));
        assertTrue(Utils.hasNullsOrEmpty(new Object[]{null}));
        assertTrue(Utils.hasNullsOrEmpty(new Object[]{null, ""}));
        assertFalse(Utils.hasNullsOrEmpty(new Object[]{"", ""}));
        assertTrue(Utils.hasNullsOrEmpty(new Object[]{"", null}));
    }

    @Test
    public void testIsEmpty_ObjectArr()
    {
        Object[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        Object[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        Object[] array2 = {1};
        assertFalse(Utils.isEmpty(array2));
    }

    @Test
    public void testIsEmpty_longArr()
    {
        long[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        long[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        long[] array2 = {1};
        assertFalse(Utils.isEmpty(array2));
    }

    @Test
    public void testIsEmpty_intArr()
    {
        int[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        int[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        int[] array2 = {1};
        assertFalse(Utils.isEmpty(array2));
    }

    @Test
    public void testIsEmpty_shortArr()
    {
        short[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        short[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        short[] array2 = {1};
        assertFalse(Utils.isEmpty(array2));
    }

    @Test
    public void testIsEmpty_charArr()
    {
        char[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        char[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        char[] array2 = {1};
        assertFalse(Utils.isEmpty(array2));
    }

    @Test
    public void testIsEmpty_byteArr()
    {
        byte[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        byte[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        byte[] array2 = {1};
        assertFalse(Utils.isEmpty(array2));
    }

    @Test
    public void testIsEmpty_doubleArr()
    {
        double[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        double[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        double[] array2 = {1};
        assertFalse(Utils.isEmpty(array2));
    }

    @Test
    public void testIsEmpty_floatArr()
    {
        float[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        float[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        float[] array2 = {1};
        assertFalse(Utils.isEmpty(array2));
    }

    @Test
    public void testIsEmpty_booleanArr()
    {
        boolean[] array0 = null;
        assertTrue(Utils.isEmpty(array0));
        
        boolean[] array1 = {};
        assertTrue(Utils.isEmpty(array1));
        
        boolean[] array2 = {true};
        assertFalse(Utils.isEmpty(array2));
    }

}
