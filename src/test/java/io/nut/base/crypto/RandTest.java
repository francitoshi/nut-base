package io.nut.base.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class RandTest
{
    final Rand instance = new Rand(new SecureRandom());

    @Test
    public void testNextInt_int()
    {
        int result = instance.nextInt(10);
        assertTrue(result < 10);
        assertTrue(result >= 0);
    }

    @Test
    public void testNextBytes()
    {
        byte[] dataNull = null;
        byte[] dataEmpty = new byte[0];

        assertNull(instance.nextBytes(dataNull));
        assertEquals(0, instance.nextBytes(dataEmpty).length);

        byte[] result = instance.nextBytes(new byte[10]);
        assertEquals(10, result.length);
    }

    @Test
    public void testNextBoolean_booleanArr()
    {
        boolean[] dataNull = null;
        boolean[] dataEmpty = new boolean[0];

        assertNull(instance.nextBoolean(dataNull));
        assertEquals(0, instance.nextBoolean(dataEmpty).length);

        boolean[] result = instance.nextBoolean(new boolean[10]);
        assertEquals(10, result.length);
    }

    @Test
    public void testNextInts_intArr()
    {
        int[] dataNull = null;
        int[] dataEmpty = new int[0];

        assertNull(instance.nextInts(dataNull));
        assertEquals(0, instance.nextInts(dataEmpty).length);

        int[] result = instance.nextInts(new int[10]);
        assertEquals(10, result.length);
    }

    @Test
    public void testNextLongs()
    {
        long[] dataNull = null;
        long[] dataEmpty = new long[0];

        assertNull(instance.nextLongs(dataNull));
        assertEquals(0, instance.nextLongs(dataEmpty).length);

        long[] result = instance.nextLongs(new long[10]);
        assertEquals(10, result.length);
    }

    @Test
    public void testNextFloats()
    {
        float[] dataNull = null;
        float[] dataEmpty = new float[0];

        assertNull(instance.nextFloats(dataNull));
        assertEquals(0, instance.nextFloats(dataEmpty).length);

        float[] result = instance.nextFloats(new float[10]);
        assertEquals(10, result.length);
    }

    @Test
    public void testNextDoubles()
    {
        double[] dataNull = null;
        double[] dataEmpty = new double[0];

        assertNull(instance.nextDoubles(dataNull));
        assertEquals(0, instance.nextDoubles(dataEmpty).length);

        double[] result = instance.nextDoubles(new double[10]);
        assertEquals(10, result.length);
    }

    @Test
    public void testNextBigIntegers_BigIntegerArr_int()
    {
        BigInteger[] dataNull = null;
        BigInteger[] dataEmpty = new BigInteger[0];

        assertNull(instance.nextBigIntegers(dataNull, 256));
        assertEquals(0, instance.nextBigIntegers(dataEmpty, 256).length);

        BigInteger[] result = instance.nextBigIntegers(new BigInteger[10], 256);
        assertEquals(10, result.length);
    }

    
}
