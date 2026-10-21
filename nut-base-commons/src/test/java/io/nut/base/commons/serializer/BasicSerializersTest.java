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

    @Test
    public void testString()
    {
        assertRoundtrip(BasicSerializers.STRING, "");
        assertRoundtrip(BasicSerializers.STRING, "hola ünïcödé ñ");
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
        return ids;
    }
}
