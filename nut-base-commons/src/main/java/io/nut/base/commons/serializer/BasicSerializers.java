/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.nut.base.commons.serializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

    public static void registerAll(SerializerRegistry registry)
    {
        registry.register(String.class, STRING);
        registry.register(Integer.class, INTEGER);
        registry.register(Long.class, LONG);
        registry.register(Double.class, DOUBLE);
    }
}
