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

import java.math.BigInteger;
import java.util.ArrayList;
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
    
    /**
     * Test of asIntArray method, of class ArrayUtils.
     */
    @Test
    public void testAsInt_byteArr()
    {
        {
            byte[] src = null;
            assertNull(As.ints(src));
        }
        {
            byte[] src = {};
            int[] expResult = {};
            assertArrayEquals(expResult, As.ints(src));
        }
        {
            byte[] src = {-127, -1, 0, +1, +127};
            int[] expResult = {-127, -1, 0, +1, +127};
            assertArrayEquals(expResult, As.ints(src));
        }
    }

    /**
     * Test of asIntArray method, of class ArrayUtils.
     */
    @Test
    public void testAsInt_shortArr()
    {
        assertArrayEquals(null, As.ints((short[])null));
        assertArrayEquals(intArray, As.ints(shortArray));
    }

    /**
     * Test of asIntArray method, of class ArrayUtils.
     */
    @Test
    public void testAsInt_IntegerArr_int()
    {
        assertArrayEquals(null, As.ints((Integer[])null,0));
        assertArrayEquals(intArray, As.ints(integerArray,0));
    }


    /**
     * Test of asLongArray method, of class ArrayUtils.
     */
    @Test
    public void testAsLong_byteArr()
    {
        {
            byte[] src = null;
            assertNull(As.longs(src));
        }
        {
            byte[] src = {};
            long[] expResult = {};
            assertArrayEquals(expResult, As.longs(src));
        }
        {
            byte[] src       = {Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE};
            long[] expResult = {Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE};
            assertArrayEquals(expResult, As.longs(src));
        }
    }

    /**
     * Test of asLongArray method, of class ArrayUtils.
     */
    @Test
    public void testAsLong_shortArr()
    {
        assertArrayEquals(null, As.longs((short[])null));
        assertArrayEquals(longArray, As.longs(shortArray));
    }

    /**
     * Test of asLongArray method, of class ArrayUtils.
     */
    @Test
    public void testAsLong_intArr()
    {
        assertArrayEquals(null, As.longs((int[])null));
        assertArrayEquals(longArray, As.longs(intArray));
    }
    
    /**
     * Test of asLong method, of class Utils.
     */
    @Test
    public void testAsLong_LongArr()
    {
        Long[] src = {1L,2L,3L};
        long[] expResult = {1,2,3};
        assertArrayEquals(expResult, As.longs(src));
    }

    /**
     * Test of asFloatArray method, of class ArrayUtils.
     */
    @Test
    public void testAsFloats_byteArr()
    {
        assertArrayEquals((float[])null, As.floats((byte[])null), 0f);
        assertArrayEquals(floatArray, As.floats(byteArray), 0f);
    }

    /**
     * Test of asFloatArray method, of class ArrayUtils.
     */
    @Test
    public void testAsFloats_shortArr()
    {
        assertArrayEquals((float[])null, As.floats((short[])null), 0f);
        assertArrayEquals(floatArray, As.floats(shortArray), 0f);
    }

    /**
     * Test of asFloatArray method, of class ArrayUtils.
     */
    @Test
    public void testAsFloats_intArr()
    {
        assertArrayEquals((float[])null, As.floats((int[])null), 0f);
        assertArrayEquals(floatArray, As.floats(intArray), 0f);
    }

    /**
     * Test of asDoubleArray method, of class ArrayUtils.
     */
    @Test
    public void testAsDouble_byteArr()
    {
        assertArrayEquals((double[])null, As.doubles((byte[])null), 0f);
        assertArrayEquals(doubleArray, As.doubles(byteArray), 0f);
    }

    /**
     * Test of asDoubleArray method, of class ArrayUtils.
     */
    @Test
    public void testAsDouble_shortArr()
    {
        assertArrayEquals((double[])null, As.doubles((short[])null), 0f);
        assertArrayEquals(doubleArray, As.doubles(shortArray), 0f);
    }

    /**
     * Test of asDoubleArray method, of class ArrayUtils.
     */
    @Test
    public void testAsDouble_intArr()
    {
        {
            int[] src = null;
            assertNull(As.doubles(src));
        }
        {
            int[] src = {};
            double[] expResult = {};
            assertArrayEquals(expResult, As.doubles(src), 0.0);
        }
        {
            int[] src          = {Integer.MIN_VALUE, Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE, Integer.MAX_VALUE};
            double[] expResult = {Integer.MIN_VALUE, Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE, Integer.MAX_VALUE};
            assertArrayEquals(expResult, As.doubles(src), 0.0);
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
            assertNull(As.doubles(src));
        }
        {
            long[] src = {};
            double[] expResult = {};
            assertArrayEquals(expResult, As.doubles(src), 0.0);
        }
        {
            long[] src         = {Long.MIN_VALUE, Integer.MIN_VALUE, Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE};
            double[] expResult = {Long.MIN_VALUE, Integer.MIN_VALUE, Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE};
            assertArrayEquals(expResult, As.doubles(src), 0.0);
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
            assertNull(As.doubles(src));
        }
        {
            float[] src = {};
            double[] expResult = {};
            assertArrayEquals(expResult, As.doubles(src), 0.0);
        }
        {
            float[] src = {0.1f, 0.02f, 0.003f};
            double[] expResult = {0.1d, 0.02d, 0.003d};
            assertArrayEquals(expResult, As.doubles(src), 0.000001);
        }
    }

    @Test
    public void testAsLongs()
    {
        long[] result = As.longs(VALUES_INT);
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
            assertNull(As.strings(src));
        }
        {
            Long[] src = {};
            assertEquals(0, As.strings(src).length);
        }
        {
            Integer[] src = {1,2,3};
            String[] exp = {"1","2","3"};
            assertArrayEquals(exp, As.strings(src));
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
            assertNull(As.strings(src));
        }
        {
            ArrayList<Long> src = new ArrayList<>();
            assertEquals(0, As.strings(src).length);
        }
        {
            ArrayList<Long> src = new ArrayList<>();
            src.add(1L);
            src.add(2L);
            src.add(3L);
            String[] exp = {"1","2","3"};
            assertArrayEquals(exp, As.strings(src));
        }
    }

    /**
     * Test of asLong method, of class Utils.
     */
    @Test
    public void testAsLong_StringArr_long_int()
    {
        assertNull(As.longs(null, 0));
        
        {
            String[] s123 = {};
            long[] r123 = {};
            long[] result = As.longs(s123, 0, 10);
            assertArrayEquals(r123, result);
        }
        {
            String[] s123 = {"1","2","3"};
            long[] r123 = {1,2,3};
            long[] result = As.longs(s123, 0, 10);
            assertArrayEquals(r123, result);
        }
        {
            String[] s123 = {"aa","bb","cc"};
            long[] r123 = {170, 187, 204};
            long[] result = As.longs(s123, 0, 16);
            assertArrayEquals(r123, result);
        }
        {
            String[] s123 = {"1","2",""};
            long[] r123 = {1,2,-1};
            long[] result = As.longs(s123, -1, 10);
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
        assertArrayEquals(expResult, As.ints(1, 3, 2));
    }

    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_3args_1()
    {
        byte[] values = {0, 0, 1, 10, 20};
        assertArrayEquals(VALUES, As.bigIntegers(values, 1, 4));
    }
    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_3args_2()
    {
        byte[][] values = {{0}, {0}, {1}, {10}, {20}};
        assertArrayEquals(VALUES, As.bigIntegers(values, 1, 4));
    }

    final BigInteger[] VALUES = {BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN};
    
    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_byteArr()
    {
        byte[] values = {0, 1, 10};
        assertArrayEquals(VALUES, As.bigIntegers(values));
    }

    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_3args_3()
    {
        char[] values = {0, 0, 1, 10, 20};
        assertArrayEquals(VALUES, As.bigIntegers(values, 1, 4));
    }

    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_charArr()
    {
        char[] values = {0, 1, 10};
        assertArrayEquals(VALUES, As.bigIntegers(values));
    }

    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_3args_4()
    {
        int[] values = {0, 0, 1, 10, 20};
        assertArrayEquals(VALUES, As.bigIntegers(values, 1, 4));
    }

    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_intArr()
    {
        int[] values = {0, 1, 10};
        assertArrayEquals(VALUES, As.bigIntegers(values));
    }

    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_3args_5()
    {
        long[] values = {0, 0, 1, 10, 20};
        assertArrayEquals(VALUES, As.bigIntegers(values, 1, 4));
    }

    /**
     * Test of asBigIntegers method, of class As.
     */
    @Test
    public void testAsBigIntegers_longArr()
    {
        long[] values = {0L, 1L, 10L};
        assertArrayEquals(VALUES, As.bigIntegers(values));
    }

}
