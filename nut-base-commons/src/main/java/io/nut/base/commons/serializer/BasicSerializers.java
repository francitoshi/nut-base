/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.serializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

public final class BasicSerializers
{

    private BasicSerializers()
    {
    }

    public static final Serializer<String> STRING = new Serializer<String>()
    {
        @Override
        public byte[] serialize(String s)
        {
            return s.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String deserialize(byte[] d)
        {
            return new String(d, StandardCharsets.UTF_8);
        }
    };

    public static final Serializer<Integer> INTEGER = new Serializer<Integer>()
    {
        @Override
        public byte[] serialize(Integer i)
        {
            return ByteBuffer.allocate(4).putInt(i).array();
        }

        @Override
        public Integer deserialize(byte[] d)
        {
            return ByteBuffer.wrap(d).getInt();
        }
    };

    public static final Serializer<Long> LONG = new Serializer<Long>()
    {
        @Override
        public byte[] serialize(Long l)
        {
            return ByteBuffer.allocate(8).putLong(l).array();
        }

        @Override
        public Long deserialize(byte[] d)
        {
            return ByteBuffer.wrap(d).getLong();
        }
    };

    public static final Serializer<Float> FLOAT = new Serializer<Float>()
    {
        @Override
        public byte[] serialize(Float v)
        {
            return ByteBuffer.allocate(8).putFloat(v).array();
        }

        @Override
        public Float deserialize(byte[] d)
        {
            return ByteBuffer.wrap(d).getFloat();
        }
    };
    
    public static final Serializer<Double> DOUBLE = new Serializer<Double>()
    {
        @Override
        public byte[] serialize(Double v)
        {
            return ByteBuffer.allocate(8).putDouble(v).array();
        }

        @Override
        public Double deserialize(byte[] d)
        {
            return ByteBuffer.wrap(d).getDouble();
        }
    };
    
    public static final Serializer<String> BASE64 = new Serializer<String>()
    {
        @Override
        public byte[] serialize(String s)
        {
            return s!=null ? Base64.getDecoder().decode(s) : null;
        }

        @Override
        public String deserialize(byte[] bytes)
        {
            return bytes!=null ? Base64.getEncoder().encodeToString(bytes) : null;
        }
    };

    public static final Serializer<BigInteger> BIG_INTEGER = new Serializer<BigInteger>()
    {
        @Override
        public byte[] serialize(BigInteger i)
        {
            return i.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public BigInteger deserialize(byte[] d)
        {
            return new BigInteger(new String(d, StandardCharsets.UTF_8));
        }
    };

    public static final Serializer<BigDecimal> BIG_DECIMAL = new Serializer<BigDecimal>()
    {
        @Override
        public byte[] serialize(BigDecimal v)
        {
            return v.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public BigDecimal deserialize(byte[] d)
        {
            return new BigDecimal(new String(d, StandardCharsets.UTF_8));
        }
    };

    public static final Serializer<LocalDate> LOCAL_DATE = new Serializer<LocalDate>()
    {
        @Override
        public byte[] serialize(LocalDate v)
        {
            return v.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public LocalDate deserialize(byte[] d)
        {
            return LocalDate.parse(new String(d, StandardCharsets.UTF_8));
        }
    };

    public static final Serializer<LocalTime> LOCAL_TIME = new Serializer<LocalTime>()
    {
        @Override
        public byte[] serialize(LocalTime v)
        {
            return v.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public LocalTime deserialize(byte[] d)
        {
            return LocalTime.parse(new String(d, StandardCharsets.UTF_8));
        }
    };

    public static final Serializer<LocalDateTime> LOCAL_DATE_TIME = new Serializer<LocalDateTime>()
    {
        @Override
        public byte[] serialize(LocalDateTime v)
        {
            return v.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public LocalDateTime deserialize(byte[] d)
        {
            return LocalDateTime.parse(new String(d, StandardCharsets.UTF_8));
        }
    };

    public static final Serializer<ZonedDateTime> ZONED_DATE_TIME = new Serializer<ZonedDateTime>()
    {
        @Override
        public byte[] serialize(ZonedDateTime v)
        {
            return v.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public ZonedDateTime deserialize(byte[] d)
        {
            return ZonedDateTime.parse(new String(d, StandardCharsets.UTF_8));
        }
    };

    public static final Serializer<ZoneId> ZONE_ID = new Serializer<ZoneId>()
    {
        @Override
        public byte[] serialize(ZoneId v)
        {
            return v.getId().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public ZoneId deserialize(byte[] d)
        {
            return ZoneId.of(new String(d, StandardCharsets.UTF_8));
        }
    };

    public static void registerAll(SerializerRegistry registry)
    {
        registry.register(String.class, STRING);
        registry.register(Integer.class, INTEGER);
        registry.register(Long.class, LONG);
        registry.register(Float.class, FLOAT);
        registry.register(Double.class, DOUBLE);
        registry.register(BigInteger.class, BIG_INTEGER);
        registry.register(BigDecimal.class, BIG_DECIMAL);
        registry.register(LocalDate.class, LOCAL_DATE);
        registry.register(LocalTime.class, LOCAL_TIME);
        registry.register(LocalDateTime.class, LOCAL_DATE_TIME);
        registry.register(ZonedDateTime.class, ZONED_DATE_TIME);
        registry.register(ZoneId.class, ZONE_ID);
    }
}
