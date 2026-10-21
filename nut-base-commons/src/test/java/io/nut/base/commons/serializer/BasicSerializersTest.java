/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.serializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BasicSerializersTest
{

    private static <T> void assertRoundtrip(Serializer<T> serializer, T value)
    {
        byte[] data = serializer.serialize(value);
        assertNotNull(data);
        assertEquals(value, serializer.deserialize(data));
    }

    private static <T> void assertArrayRoundtrip(Serializer<T> serializer, T value, BiConsumer<T, T> assertEquals)
    {
        byte[] data = serializer.serialize(value);
        assertNotNull(data);
        assertEquals.accept(value, serializer.deserialize(data));
    }

    private static <T> void assertArray2DRoundtrip(Serializer<T> serializer, T value)
    {
        byte[] data = serializer.serialize(value);
        assertNotNull(data);
        T result = serializer.deserialize(data);
        assertNotNull(result);
        assertTrue(deepEquals(value, result), "the arrays are different");
    }

    private static boolean deepEquals(Object a, Object b)
    {
        if (a == b)
        {
            return true;
        }
        if (a == null || b == null || a.getClass() != b.getClass())
        {
            return false;
        }
        if (a instanceof Object[] && b instanceof Object[])
        {
            Object[] first = (Object[]) a;
            Object[] second = (Object[]) b;
            if (first.length != second.length)
            {
                return false;
            }
            for (int i = 0; i < first.length; i++)
            {
                if (!deepEquals(first[i], second[i]))
                {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof byte[] && b instanceof byte[])
        {
            return Arrays.equals((byte[]) a, (byte[]) b);
        }
        if (a instanceof short[] && b instanceof short[])
        {
            return Arrays.equals((short[]) a, (short[]) b);
        }
        if (a instanceof int[] && b instanceof int[])
        {
            return Arrays.equals((int[]) a, (int[]) b);
        }
        if (a instanceof long[] && b instanceof long[])
        {
            return Arrays.equals((long[]) a, (long[]) b);
        }
        if (a instanceof char[] && b instanceof char[])
        {
            return Arrays.equals((char[]) a, (char[]) b);
        }
        if (a instanceof float[] && b instanceof float[])
        {
            return Arrays.equals((float[]) a, (float[]) b);
        }
        if (a instanceof double[] && b instanceof double[])
        {
            return Arrays.equals((double[]) a, (double[]) b);
        }
        if (a instanceof boolean[] && b instanceof boolean[])
        {
            return Arrays.equals((boolean[]) a, (boolean[]) b);
        }
        return a.equals(b);
    }

    @Test
    public void testString()
    {
        assertRoundtrip(BasicSerializers.STRING, "");
        assertRoundtrip(BasicSerializers.STRING, "hola ünïcödé ñ");
    }

    @Test
    public void testBooleanArray()
    {
        assertArrayRoundtrip(BasicSerializers.BOOLEAN_ARRAY_1D, new boolean[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.BOOLEAN_ARRAY_1D, new boolean[] { true, false, true, true }, Assertions::assertArrayEquals);
    }

    @Test
    public void testByteArray()
    {
        assertArrayRoundtrip(BasicSerializers.BYTE_ARRAY_1D, new byte[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.BYTE_ARRAY_1D, new byte[] { 0, 1, -1, 127, -128 }, Assertions::assertArrayEquals);
        byte[] data = { 10, 20, 30 };
        assertSame(data, BasicSerializers.BYTE_ARRAY_1D.serialize(data));
        assertSame(data, BasicSerializers.BYTE_ARRAY_1D.deserialize(data));
    }

    @Test
    public void testCharArray()
    {
        assertArrayRoundtrip(BasicSerializers.CHAR_ARRAY_1D, new char[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.CHAR_ARRAY_1D, new char[] { 'a', 'ñ', 'Z' }, Assertions::assertArrayEquals);
    }

    @Test
    public void testShortArray()
    {
        assertArrayRoundtrip(BasicSerializers.SHORT_ARRAY_1D, new short[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.SHORT_ARRAY_1D, new short[] { 0, -1, Short.MAX_VALUE, Short.MIN_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testIntArray()
    {
        assertArrayRoundtrip(BasicSerializers.INT_ARRAY_1D, new int[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.INT_ARRAY_1D, new int[] { 0, -1, 42, Integer.MAX_VALUE, Integer.MIN_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testLongArray()
    {
        assertArrayRoundtrip(BasicSerializers.LONG_ARRAY_1D, new long[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.LONG_ARRAY_1D, new long[] { 0L, -1L, 42L, Long.MAX_VALUE, Long.MIN_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testFloatArray()
    {
        assertArrayRoundtrip(BasicSerializers.FLOAT_ARRAY_1D, new float[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.FLOAT_ARRAY_1D, new float[] { 0f, -1.5f, 3.25f, Float.MAX_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testDoubleArray()
    {
        assertArrayRoundtrip(BasicSerializers.DOUBLE_ARRAY_1D, new double[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.DOUBLE_ARRAY_1D, new double[] { 0d, -2.25d, 3.5d, Double.MAX_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testStringArray()
    {
        assertArrayRoundtrip(BasicSerializers.STRING_ARRAY_1D, new String[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.STRING_ARRAY_1D, new String[] { "", "hola", "ünïcödé ñ" }, Assertions::assertArrayEquals);
    }

    @Test
    public void testBigIntegerArray()
    {
        assertArrayRoundtrip(BasicSerializers.BIG_INTEGER_ARRAY_1D, new BigInteger[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.BIG_INTEGER_ARRAY_1D,
                new BigInteger[] { BigInteger.ZERO, BigInteger.valueOf(-1L),
                        new BigInteger("12345678901234567890123456789012345678901234567890") }, Assertions::assertArrayEquals);
    }

    @Test
    public void testBigDecimalArray()
    {
        assertArrayRoundtrip(BasicSerializers.BIG_DECIMAL_ARRAY_1D, new BigDecimal[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.BIG_DECIMAL_ARRAY_1D,
                new BigDecimal[] { new BigDecimal("0.00"), new BigDecimal("-1.23456789012345678901234567890"),
                        BigDecimal.TEN.setScale(5) }, Assertions::assertArrayEquals);
    }

    @Test
    public void testNullArrays()
    {
        assertNull(BasicSerializers.BOOLEAN_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.BOOLEAN_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.BYTE_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.BYTE_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.CHAR_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.CHAR_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.SHORT_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.SHORT_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.INT_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.INT_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.LONG_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.LONG_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.FLOAT_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.FLOAT_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.DOUBLE_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.DOUBLE_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.STRING_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.STRING_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.BIG_INTEGER_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.BIG_INTEGER_ARRAY_1D.deserialize(null));
        assertNull(BasicSerializers.BIG_DECIMAL_ARRAY_1D.serialize(null));
        assertNull(BasicSerializers.BIG_DECIMAL_ARRAY_1D.deserialize(null));
    }

    @Test
    public void testInvalidArrayData()
    {
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.INT_ARRAY_1D.deserialize(new byte[] { 1, 2 }));
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.INT_ARRAY_1D.deserialize(new byte[] { 0, 0, 0, 5, 0, 0, 0, 1 }));
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.INT_ARRAY_1D.deserialize(new byte[] { -1, -1, -1, -1 }));
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.DOUBLE_ARRAY_1D.deserialize(new byte[] { 0, 0, 0, 1, 0 }));
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.STRING_ARRAY_1D.deserialize(new byte[] { 0, 0, 0, 1 }));
    }

    @Test
    public void testBooleanArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.BOOLEAN_ARRAY_2D, new boolean[0][]);
        assertArray2DRoundtrip(BasicSerializers.BOOLEAN_ARRAY_2D, new boolean[][] { { true, false }, { true }, {} });
    }

    @Test
    public void testByteArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.BYTE_ARRAY_2D, new byte[0][]);
        assertArray2DRoundtrip(BasicSerializers.BYTE_ARRAY_2D, new byte[][] { { 1, 2 }, {}, { -1 } });
    }

    @Test
    public void testCharArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.CHAR_ARRAY_2D, new char[0][]);
        assertArray2DRoundtrip(BasicSerializers.CHAR_ARRAY_2D, new char[][] { { 'a', 'ñ' }, { 'Z' }, {} });
    }

    @Test
    public void testShortArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.SHORT_ARRAY_2D, new short[0][]);
        assertArray2DRoundtrip(BasicSerializers.SHORT_ARRAY_2D, new short[][] { { 0, -1 }, { Short.MAX_VALUE }, {} });
    }

    @Test
    public void testIntArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.INT_ARRAY_2D, new int[0][]);
        assertArray2DRoundtrip(BasicSerializers.INT_ARRAY_2D,
                new int[][] { { 0, -1, 42 }, { Integer.MAX_VALUE, Integer.MIN_VALUE }, {} });
    }

    @Test
    public void testLongArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.LONG_ARRAY_2D, new long[0][]);
        assertArray2DRoundtrip(BasicSerializers.LONG_ARRAY_2D,
                new long[][] { { 0L, -1L }, { Long.MAX_VALUE, Long.MIN_VALUE }, {} });
    }

    @Test
    public void testFloatArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.FLOAT_ARRAY_2D, new float[0][]);
        assertArray2DRoundtrip(BasicSerializers.FLOAT_ARRAY_2D, new float[][] { { 0f, -1.5f }, { 3.25f }, {} });
    }

    @Test
    public void testDoubleArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.DOUBLE_ARRAY_2D, new double[0][]);
        assertArray2DRoundtrip(BasicSerializers.DOUBLE_ARRAY_2D, new double[][] { { 0d, -2.25d }, { 3.5d }, {} });
    }

    @Test
    public void testStringArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.STRING_ARRAY_2D, new String[0][]);
        assertArray2DRoundtrip(BasicSerializers.STRING_ARRAY_2D,
                new String[][] { { "", "hola" }, { "ünïcödé ñ" }, {} });
    }

    @Test
    public void testBigIntegerArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.BIG_INTEGER_ARRAY_2D, new BigInteger[0][]);
        assertArray2DRoundtrip(BasicSerializers.BIG_INTEGER_ARRAY_2D,
                new BigInteger[][] { { BigInteger.ZERO, BigInteger.valueOf(-1L) },
                        { new BigInteger("12345678901234567890123456789012345678901234567890") }, {} });
    }

    @Test
    public void testBigDecimalArray2D()
    {
        assertArray2DRoundtrip(BasicSerializers.BIG_DECIMAL_ARRAY_2D, new BigDecimal[0][]);
        assertArray2DRoundtrip(BasicSerializers.BIG_DECIMAL_ARRAY_2D,
                new BigDecimal[][] { { new BigDecimal("0.00"), new BigDecimal("-1.23456789012345678901234567890") },
                        { BigDecimal.TEN.setScale(5) }, {} });
    }

    @Test
    public void testNullRowsAndElements()
    {
        assertArray2DRoundtrip(BasicSerializers.STRING_ARRAY_2D, new String[][] { { "a", null }, null, {} });
        assertArray2DRoundtrip(BasicSerializers.BIG_INTEGER_ARRAY_2D, new BigInteger[][] { { null }, null });
        assertArray2DRoundtrip(BasicSerializers.BIG_DECIMAL_ARRAY_2D, new BigDecimal[][] { { null }, null });
        assertArray2DRoundtrip(BasicSerializers.INT_ARRAY_2D, new int[][] { null });
        assertArray2DRoundtrip(BasicSerializers.BYTE_ARRAY_2D, new byte[][] { null });
    }

    @Test
    public void testNullArrays2D()
    {
        assertNull(BasicSerializers.BOOLEAN_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.BOOLEAN_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.BYTE_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.BYTE_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.CHAR_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.CHAR_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.SHORT_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.SHORT_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.INT_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.INT_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.LONG_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.LONG_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.FLOAT_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.FLOAT_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.DOUBLE_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.DOUBLE_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.STRING_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.STRING_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.BIG_INTEGER_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.BIG_INTEGER_ARRAY_2D.deserialize(null));
        assertNull(BasicSerializers.BIG_DECIMAL_ARRAY_2D.serialize(null));
        assertNull(BasicSerializers.BIG_DECIMAL_ARRAY_2D.deserialize(null));
    }

    @Test
    public void testInvalidArray2DData()
    {
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.INT_ARRAY_2D.deserialize(new byte[] { 1, 2 }));
        assertThrows(IllegalArgumentException.class,
                () -> BasicSerializers.INT_ARRAY_2D.deserialize(new byte[] { 0, 0, 0, 5, 0, 0, 0, 1 }));
        assertThrows(IllegalArgumentException.class,
                () -> BasicSerializers.INT_ARRAY_2D.deserialize(new byte[] { -1, -1, -1, -1 }));
        assertThrows(IllegalArgumentException.class,
                () -> BasicSerializers.INT_ARRAY_2D.deserialize(new byte[] { 0, 0, 0, 1, 0, 0, 0, 10 }));
        assertThrows(IllegalArgumentException.class,
                () -> BasicSerializers.STRING_ARRAY_2D.deserialize(new byte[] { 0, 0, 0, 1, 0, 0, 0, 2 }));
    }

    @Test
    public void testNumbers()
    {
        assertRoundtrip(BasicSerializers.INTEGER, 42);
        assertRoundtrip(BasicSerializers.INTEGER, -7);
        assertRoundtrip(BasicSerializers.LONG, Long.MAX_VALUE);
        assertRoundtrip(BasicSerializers.LONG, -1L);
        assertRoundtrip(BasicSerializers.FLOAT, 1.5f);
        assertRoundtrip(BasicSerializers.DOUBLE, -2.25d);
    }

    @Test
    public void testBoolean()
    {
        assertRoundtrip(BasicSerializers.BOOLEAN, Boolean.TRUE);
        assertRoundtrip(BasicSerializers.BOOLEAN, Boolean.FALSE);
        assertEquals(1, BasicSerializers.BOOLEAN.serialize(Boolean.TRUE).length);
    }

    @Test
    public void testByte()
    {
        assertRoundtrip(BasicSerializers.BYTE, (byte) 0);
        assertRoundtrip(BasicSerializers.BYTE, (byte) -1);
        assertRoundtrip(BasicSerializers.BYTE, Byte.MAX_VALUE);
        assertRoundtrip(BasicSerializers.BYTE, Byte.MIN_VALUE);
        assertEquals(Byte.BYTES, BasicSerializers.BYTE.serialize((byte) 1).length);
    }

    @Test
    public void testShort()
    {
        assertRoundtrip(BasicSerializers.SHORT, (short) 0);
        assertRoundtrip(BasicSerializers.SHORT, (short) -1);
        assertRoundtrip(BasicSerializers.SHORT, Short.MAX_VALUE);
        assertRoundtrip(BasicSerializers.SHORT, Short.MIN_VALUE);
        assertEquals(Short.BYTES, BasicSerializers.SHORT.serialize((short) 1).length);
    }

    @Test
    public void testChar()
    {
        assertRoundtrip(BasicSerializers.CHARACTER, 'a');
        assertRoundtrip(BasicSerializers.CHARACTER, 'ñ');
        assertRoundtrip(BasicSerializers.CHARACTER, Character.MAX_VALUE);
        assertRoundtrip(BasicSerializers.CHARACTER, Character.MIN_VALUE);
        assertEquals(Character.BYTES, BasicSerializers.CHARACTER.serialize('a').length);
    }

    @Test
    public void testFloat()
    {
        assertRoundtrip(BasicSerializers.FLOAT, 1.5f);
        assertRoundtrip(BasicSerializers.FLOAT, Float.MAX_VALUE);
        assertEquals(Float.BYTES, BasicSerializers.FLOAT.serialize(1.5f).length);
    }

    @Test
    public void testBigInteger()
    {
        assertRoundtrip(BasicSerializers.BIG_INTEGER, BigInteger.ZERO);
        assertRoundtrip(BasicSerializers.BIG_INTEGER, BigInteger.valueOf(-1234567890123456789L));
        assertRoundtrip(BasicSerializers.BIG_INTEGER,
                new BigInteger("12345678901234567890123456789012345678901234567890"));
    }

    @Test
    public void testBigDecimal()
    {
        assertRoundtrip(BasicSerializers.BIG_DECIMAL, new BigDecimal("0.00"));
        assertRoundtrip(BasicSerializers.BIG_DECIMAL, new BigDecimal("-1.23456789012345678901234567890"));
        assertRoundtrip(BasicSerializers.BIG_DECIMAL, BigDecimal.TEN.setScale(5));
    }

    @Test
    public void testLocalDate()
    {
        assertRoundtrip(BasicSerializers.LOCAL_DATE, LocalDate.of(2026, 10, 20));
        assertRoundtrip(BasicSerializers.LOCAL_DATE, LocalDate.of(1970, 1, 1));
        assertRoundtrip(BasicSerializers.LOCAL_DATE, LocalDate.of(9999, 12, 31));
    }

    @Test
    public void testLocalTime()
    {
        assertRoundtrip(BasicSerializers.LOCAL_TIME, LocalTime.of(23, 59, 59));
        assertRoundtrip(BasicSerializers.LOCAL_TIME, LocalTime.NOON);
        assertRoundtrip(BasicSerializers.LOCAL_TIME, LocalTime.of(10, 15, 30, 123_456_789));
    }

    @Test
    public void testLocalDateTime()
    {
        assertRoundtrip(BasicSerializers.LOCAL_DATE_TIME, LocalDateTime.of(2026, 10, 20, 10, 15, 30));
        assertRoundtrip(BasicSerializers.LOCAL_DATE_TIME,
                LocalDateTime.of(2026, 10, 20, 10, 15, 30, 123_456_789));
    }

    @Test
    public void testZonedDateTime()
    {
        assertRoundtrip(BasicSerializers.ZONED_DATE_TIME, ZonedDateTime.now());
        assertRoundtrip(BasicSerializers.ZONED_DATE_TIME,
                ZonedDateTime.of(2026, 10, 20, 10, 15, 30, 123_456_789, ZoneId.of("Europe/Madrid")));
        assertRoundtrip(BasicSerializers.ZONED_DATE_TIME,
                ZonedDateTime.of(2026, 1, 15, 0, 0, 0, 0, ZoneId.of("UTC")));
    }

    @Test
    public void testZoneId()
    {
        assertRoundtrip(BasicSerializers.ZONE_ID, ZoneId.of("Europe/Madrid"));
        assertRoundtrip(BasicSerializers.ZONE_ID, ZoneId.of("UTC"));
        assertRoundtrip(BasicSerializers.ZONE_ID, ZoneId.of("+02:00"));
        assertRoundtrip(BasicSerializers.ZONE_ID, ZoneId.systemDefault());
    }

    @Test
    public void testRegisterAll()
    {
        SerializerRegistry registry = new SerializerRegistry();
        BasicSerializers.registerAll(registry);
        List<Class<?>> types = new ArrayList<>();
        types.add(String.class);
        types.add(Integer.class);
        types.add(Long.class);
        types.add(Float.class);
        types.add(Double.class);
        types.add(Boolean.class);
        types.add(Byte.class);
        types.add(Short.class);
        types.add(Character.class);
        types.add(BigInteger.class);
        types.add(BigDecimal.class);
        types.add(LocalDate.class);
        types.add(LocalTime.class);
        types.add(LocalDateTime.class);
        types.add(ZonedDateTime.class);
        types.add(ZoneId.class);
        types.add(boolean[].class);
        types.add(byte[].class);
        types.add(char[].class);
        types.add(short[].class);
        types.add(int[].class);
        types.add(long[].class);
        types.add(float[].class);
        types.add(double[].class);
        types.add(String[].class);
        types.add(BigInteger[].class);
        types.add(BigDecimal[].class);
        types.add(boolean[][].class);
        types.add(byte[][].class);
        types.add(char[][].class);
        types.add(short[][].class);
        types.add(int[][].class);
        types.add(long[][].class);
        types.add(float[][].class);
        types.add(double[][].class);
        types.add(String[][].class);
        types.add(BigInteger[][].class);
        types.add(BigDecimal[][].class);
        for (Class<?> type : types)
        {
            assertTrue(registry.isRegistered(type), "missing registration for " + type);
            assertNotNull(registry.get(type));
        }
        byte[] data = registry.get(LocalDate.class).serialize(LocalDate.of(2026, 10, 20));
        assertEquals(LocalDate.of(2026, 10, 20), registry.get(LocalDate.class).deserialize(data));
    }

    @Test
    public void testRegisterAllIds()
    {
        SerializerRegistry registry = new SerializerRegistry();
        BasicSerializers.registerAll(registry);
        assertEquals(BasicSerializers.ID_STRING, registry.getId(String.class));
        assertEquals(BasicSerializers.ID_INTEGER, registry.getId(Integer.class));
        assertEquals(BasicSerializers.ID_LONG, registry.getId(Long.class));
        assertEquals(BasicSerializers.ID_FLOAT, registry.getId(Float.class));
        assertEquals(BasicSerializers.ID_DOUBLE, registry.getId(Double.class));
        assertEquals(BasicSerializers.ID_BOOLEAN, registry.getId(Boolean.class));
        assertEquals(BasicSerializers.ID_BYTE, registry.getId(Byte.class));
        assertEquals(BasicSerializers.ID_SHORT, registry.getId(Short.class));
        assertEquals(BasicSerializers.ID_CHARACTER, registry.getId(Character.class));
        assertEquals(BasicSerializers.ID_BIG_INTEGER, registry.getId(BigInteger.class));
        assertEquals(BasicSerializers.ID_BIG_DECIMAL, registry.getId(BigDecimal.class));
        assertEquals(BasicSerializers.ID_LOCAL_DATE, registry.getId(LocalDate.class));
        assertEquals(BasicSerializers.ID_LOCAL_TIME, registry.getId(LocalTime.class));
        assertEquals(BasicSerializers.ID_LOCAL_DATE_TIME, registry.getId(LocalDateTime.class));
        assertEquals(BasicSerializers.ID_ZONED_DATE_TIME, registry.getId(ZonedDateTime.class));
        assertEquals(BasicSerializers.ID_ZONE_ID, registry.getId(ZoneId.class));
        assertEquals(BasicSerializers.ID_BOOLEAN_ARRAY_1D, registry.getId(boolean[].class));
        assertEquals(BasicSerializers.ID_BYTE_ARRAY_1D, registry.getId(byte[].class));
        assertEquals(BasicSerializers.ID_CHAR_ARRAY_1D, registry.getId(char[].class));
        assertEquals(BasicSerializers.ID_SHORT_ARRAY_1D, registry.getId(short[].class));
        assertEquals(BasicSerializers.ID_INT_ARRAY_1D, registry.getId(int[].class));
        assertEquals(BasicSerializers.ID_LONG_ARRAY_1D, registry.getId(long[].class));
        assertEquals(BasicSerializers.ID_FLOAT_ARRAY_1D, registry.getId(float[].class));
        assertEquals(BasicSerializers.ID_DOUBLE_ARRAY_1D, registry.getId(double[].class));
        assertEquals(BasicSerializers.ID_STRING_ARRAY_1D, registry.getId(String[].class));
        assertEquals(BasicSerializers.ID_BIG_INTEGER_ARRAY_1D, registry.getId(BigInteger[].class));
        assertEquals(BasicSerializers.ID_BIG_DECIMAL_ARRAY_1D, registry.getId(BigDecimal[].class));
        assertEquals(BasicSerializers.ID_BOOLEAN_ARRAY_2D, registry.getId(boolean[][].class));
        assertEquals(BasicSerializers.ID_BYTE_ARRAY_2D, registry.getId(byte[][].class));
        assertEquals(BasicSerializers.ID_CHAR_ARRAY_2D, registry.getId(char[][].class));
        assertEquals(BasicSerializers.ID_SHORT_ARRAY_2D, registry.getId(short[][].class));
        assertEquals(BasicSerializers.ID_INT_ARRAY_2D, registry.getId(int[][].class));
        assertEquals(BasicSerializers.ID_LONG_ARRAY_2D, registry.getId(long[][].class));
        assertEquals(BasicSerializers.ID_FLOAT_ARRAY_2D, registry.getId(float[][].class));
        assertEquals(BasicSerializers.ID_DOUBLE_ARRAY_2D, registry.getId(double[][].class));
        assertEquals(BasicSerializers.ID_STRING_ARRAY_2D, registry.getId(String[][].class));
        assertEquals(BasicSerializers.ID_BIG_INTEGER_ARRAY_2D, registry.getId(BigInteger[][].class));
        assertEquals(BasicSerializers.ID_BIG_DECIMAL_ARRAY_2D, registry.getId(BigDecimal[][].class));
        assertEquals(types().size(), registry.size());
        assertEquals(BasicSerializers.STRING, registry.getById(BasicSerializers.ID_STRING));
        assertEquals(BasicSerializers.LOCAL_DATE, registry.getById(BasicSerializers.ID_LOCAL_DATE));
        assertTrue(registry.isRegistered(BasicSerializers.ID_ZONE_ID));
    }

    private static Set<Integer> types()
    {
        Set<Integer> ids = new HashSet<>();
        ids.add(BasicSerializers.ID_STRING);
        ids.add(BasicSerializers.ID_INTEGER);
        ids.add(BasicSerializers.ID_LONG);
        ids.add(BasicSerializers.ID_FLOAT);
        ids.add(BasicSerializers.ID_DOUBLE);
        ids.add(BasicSerializers.ID_BOOLEAN);
        ids.add(BasicSerializers.ID_BYTE);
        ids.add(BasicSerializers.ID_SHORT);
        ids.add(BasicSerializers.ID_CHARACTER);
        ids.add(BasicSerializers.ID_BIG_INTEGER);
        ids.add(BasicSerializers.ID_BIG_DECIMAL);
        ids.add(BasicSerializers.ID_LOCAL_DATE);
        ids.add(BasicSerializers.ID_LOCAL_TIME);
        ids.add(BasicSerializers.ID_LOCAL_DATE_TIME);
        ids.add(BasicSerializers.ID_ZONED_DATE_TIME);
        ids.add(BasicSerializers.ID_ZONE_ID);
        ids.add(BasicSerializers.ID_BOOLEAN_ARRAY_1D);
        ids.add(BasicSerializers.ID_BYTE_ARRAY_1D);
        ids.add(BasicSerializers.ID_CHAR_ARRAY_1D);
        ids.add(BasicSerializers.ID_SHORT_ARRAY_1D);
        ids.add(BasicSerializers.ID_INT_ARRAY_1D);
        ids.add(BasicSerializers.ID_LONG_ARRAY_1D);
        ids.add(BasicSerializers.ID_FLOAT_ARRAY_1D);
        ids.add(BasicSerializers.ID_DOUBLE_ARRAY_1D);
        ids.add(BasicSerializers.ID_STRING_ARRAY_1D);
        ids.add(BasicSerializers.ID_BIG_INTEGER_ARRAY_1D);
        ids.add(BasicSerializers.ID_BIG_DECIMAL_ARRAY_1D);
        ids.add(BasicSerializers.ID_BOOLEAN_ARRAY_2D);
        ids.add(BasicSerializers.ID_BYTE_ARRAY_2D);
        ids.add(BasicSerializers.ID_CHAR_ARRAY_2D);
        ids.add(BasicSerializers.ID_SHORT_ARRAY_2D);
        ids.add(BasicSerializers.ID_INT_ARRAY_2D);
        ids.add(BasicSerializers.ID_LONG_ARRAY_2D);
        ids.add(BasicSerializers.ID_FLOAT_ARRAY_2D);
        ids.add(BasicSerializers.ID_DOUBLE_ARRAY_2D);
        ids.add(BasicSerializers.ID_STRING_ARRAY_2D);
        ids.add(BasicSerializers.ID_BIG_INTEGER_ARRAY_2D);
        ids.add(BasicSerializers.ID_BIG_DECIMAL_ARRAY_2D);
        return ids;
    }
}
