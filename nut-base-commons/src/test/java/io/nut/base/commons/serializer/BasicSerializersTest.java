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

    @Test
    public void testString()
    {
        assertRoundtrip(BasicSerializers.STRING, "");
        assertRoundtrip(BasicSerializers.STRING, "hola ünïcödé ñ");
    }

    @Test
    public void testBooleanArray()
    {
        assertArrayRoundtrip(BasicSerializers.BOOLEAN_ARRAY, new boolean[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.BOOLEAN_ARRAY, new boolean[] { true, false, true, true }, Assertions::assertArrayEquals);
    }

    @Test
    public void testByteArray()
    {
        assertArrayRoundtrip(BasicSerializers.BYTE_ARRAY, new byte[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.BYTE_ARRAY, new byte[] { 0, 1, -1, 127, -128 }, Assertions::assertArrayEquals);
        byte[] data = { 10, 20, 30 };
        assertSame(data, BasicSerializers.BYTE_ARRAY.serialize(data));
        assertSame(data, BasicSerializers.BYTE_ARRAY.deserialize(data));
    }

    @Test
    public void testCharArray()
    {
        assertArrayRoundtrip(BasicSerializers.CHAR_ARRAY, new char[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.CHAR_ARRAY, new char[] { 'a', 'ñ', 'Z' }, Assertions::assertArrayEquals);
    }

    @Test
    public void testShortArray()
    {
        assertArrayRoundtrip(BasicSerializers.SHORT_ARRAY, new short[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.SHORT_ARRAY, new short[] { 0, -1, Short.MAX_VALUE, Short.MIN_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testIntArray()
    {
        assertArrayRoundtrip(BasicSerializers.INT_ARRAY, new int[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.INT_ARRAY, new int[] { 0, -1, 42, Integer.MAX_VALUE, Integer.MIN_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testLongArray()
    {
        assertArrayRoundtrip(BasicSerializers.LONG_ARRAY, new long[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.LONG_ARRAY, new long[] { 0L, -1L, 42L, Long.MAX_VALUE, Long.MIN_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testFloatArray()
    {
        assertArrayRoundtrip(BasicSerializers.FLOAT_ARRAY, new float[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.FLOAT_ARRAY, new float[] { 0f, -1.5f, 3.25f, Float.MAX_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testDoubleArray()
    {
        assertArrayRoundtrip(BasicSerializers.DOUBLE_ARRAY, new double[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.DOUBLE_ARRAY, new double[] { 0d, -2.25d, 3.5d, Double.MAX_VALUE }, Assertions::assertArrayEquals);
    }

    @Test
    public void testStringArray()
    {
        assertArrayRoundtrip(BasicSerializers.STRING_ARRAY, new String[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.STRING_ARRAY, new String[] { "", "hola", "ünïcödé ñ" }, Assertions::assertArrayEquals);
    }

    @Test
    public void testBigIntegerArray()
    {
        assertArrayRoundtrip(BasicSerializers.BIG_INTEGER_ARRAY, new BigInteger[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.BIG_INTEGER_ARRAY,
                new BigInteger[] { BigInteger.ZERO, BigInteger.valueOf(-1L),
                        new BigInteger("12345678901234567890123456789012345678901234567890") }, Assertions::assertArrayEquals);
    }

    @Test
    public void testBigDecimalArray()
    {
        assertArrayRoundtrip(BasicSerializers.BIG_DECIMAL_ARRAY, new BigDecimal[0], Assertions::assertArrayEquals);
        assertArrayRoundtrip(BasicSerializers.BIG_DECIMAL_ARRAY,
                new BigDecimal[] { new BigDecimal("0.00"), new BigDecimal("-1.23456789012345678901234567890"),
                        BigDecimal.TEN.setScale(5) }, Assertions::assertArrayEquals);
    }

    @Test
    public void testNullArrays()
    {
        assertNull(BasicSerializers.BOOLEAN_ARRAY.serialize(null));
        assertNull(BasicSerializers.BOOLEAN_ARRAY.deserialize(null));
        assertNull(BasicSerializers.BYTE_ARRAY.serialize(null));
        assertNull(BasicSerializers.BYTE_ARRAY.deserialize(null));
        assertNull(BasicSerializers.CHAR_ARRAY.serialize(null));
        assertNull(BasicSerializers.CHAR_ARRAY.deserialize(null));
        assertNull(BasicSerializers.SHORT_ARRAY.serialize(null));
        assertNull(BasicSerializers.SHORT_ARRAY.deserialize(null));
        assertNull(BasicSerializers.INT_ARRAY.serialize(null));
        assertNull(BasicSerializers.INT_ARRAY.deserialize(null));
        assertNull(BasicSerializers.LONG_ARRAY.serialize(null));
        assertNull(BasicSerializers.LONG_ARRAY.deserialize(null));
        assertNull(BasicSerializers.FLOAT_ARRAY.serialize(null));
        assertNull(BasicSerializers.FLOAT_ARRAY.deserialize(null));
        assertNull(BasicSerializers.DOUBLE_ARRAY.serialize(null));
        assertNull(BasicSerializers.DOUBLE_ARRAY.deserialize(null));
        assertNull(BasicSerializers.STRING_ARRAY.serialize(null));
        assertNull(BasicSerializers.STRING_ARRAY.deserialize(null));
        assertNull(BasicSerializers.BIG_INTEGER_ARRAY.serialize(null));
        assertNull(BasicSerializers.BIG_INTEGER_ARRAY.deserialize(null));
        assertNull(BasicSerializers.BIG_DECIMAL_ARRAY.serialize(null));
        assertNull(BasicSerializers.BIG_DECIMAL_ARRAY.deserialize(null));
    }

    @Test
    public void testInvalidArrayData()
    {
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.INT_ARRAY.deserialize(new byte[] { 1, 2 }));
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.INT_ARRAY.deserialize(new byte[] { 0, 0, 0, 5, 0, 0, 0, 1 }));
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.INT_ARRAY.deserialize(new byte[] { -1, -1, -1, -1 }));
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.DOUBLE_ARRAY.deserialize(new byte[] { 0, 0, 0, 1, 0 }));
        assertThrows(IllegalArgumentException.class, () -> BasicSerializers.STRING_ARRAY.deserialize(new byte[] { 0, 0, 0, 1 }));
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
        assertEquals(BasicSerializers.ID_BIG_INTEGER, registry.getId(BigInteger.class));
        assertEquals(BasicSerializers.ID_BIG_DECIMAL, registry.getId(BigDecimal.class));
        assertEquals(BasicSerializers.ID_LOCAL_DATE, registry.getId(LocalDate.class));
        assertEquals(BasicSerializers.ID_LOCAL_TIME, registry.getId(LocalTime.class));
        assertEquals(BasicSerializers.ID_LOCAL_DATE_TIME, registry.getId(LocalDateTime.class));
        assertEquals(BasicSerializers.ID_ZONED_DATE_TIME, registry.getId(ZonedDateTime.class));
        assertEquals(BasicSerializers.ID_ZONE_ID, registry.getId(ZoneId.class));
        assertEquals(BasicSerializers.ID_BOOLEAN_ARRAY, registry.getId(boolean[].class));
        assertEquals(BasicSerializers.ID_BYTE_ARRAY, registry.getId(byte[].class));
        assertEquals(BasicSerializers.ID_CHAR_ARRAY, registry.getId(char[].class));
        assertEquals(BasicSerializers.ID_SHORT_ARRAY, registry.getId(short[].class));
        assertEquals(BasicSerializers.ID_INT_ARRAY, registry.getId(int[].class));
        assertEquals(BasicSerializers.ID_LONG_ARRAY, registry.getId(long[].class));
        assertEquals(BasicSerializers.ID_FLOAT_ARRAY, registry.getId(float[].class));
        assertEquals(BasicSerializers.ID_DOUBLE_ARRAY, registry.getId(double[].class));
        assertEquals(BasicSerializers.ID_STRING_ARRAY, registry.getId(String[].class));
        assertEquals(BasicSerializers.ID_BIG_INTEGER_ARRAY, registry.getId(BigInteger[].class));
        assertEquals(BasicSerializers.ID_BIG_DECIMAL_ARRAY, registry.getId(BigDecimal[].class));
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
        ids.add(BasicSerializers.ID_BIG_INTEGER);
        ids.add(BasicSerializers.ID_BIG_DECIMAL);
        ids.add(BasicSerializers.ID_LOCAL_DATE);
        ids.add(BasicSerializers.ID_LOCAL_TIME);
        ids.add(BasicSerializers.ID_LOCAL_DATE_TIME);
        ids.add(BasicSerializers.ID_ZONED_DATE_TIME);
        ids.add(BasicSerializers.ID_ZONE_ID);
        ids.add(BasicSerializers.ID_BOOLEAN_ARRAY);
        ids.add(BasicSerializers.ID_BYTE_ARRAY);
        ids.add(BasicSerializers.ID_CHAR_ARRAY);
        ids.add(BasicSerializers.ID_SHORT_ARRAY);
        ids.add(BasicSerializers.ID_INT_ARRAY);
        ids.add(BasicSerializers.ID_LONG_ARRAY);
        ids.add(BasicSerializers.ID_FLOAT_ARRAY);
        ids.add(BasicSerializers.ID_DOUBLE_ARRAY);
        ids.add(BasicSerializers.ID_STRING_ARRAY);
        ids.add(BasicSerializers.ID_BIG_INTEGER_ARRAY);
        ids.add(BasicSerializers.ID_BIG_DECIMAL_ARRAY);
        return ids;
    }
}
