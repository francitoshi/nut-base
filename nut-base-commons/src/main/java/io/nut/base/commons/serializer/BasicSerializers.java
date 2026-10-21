/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.function.Function;

/**
 * Serializers for the most common java types.
 * <p>
 * The scalar types are encoded either in their natural binary form (big endian,
 * as {@link ByteBuffer} does) or as UTF-8 text. The array types use the
 * following layout:
 * <ul>
 *   <li>fixed width numeric arrays: a 4 byte count followed by the elements,
 *       one after the other, in big endian order</li>
 *   <li>text arrays: a 4 byte count followed by, for every element, its 4 byte
 *       length in bytes and its UTF-8 content</li>
 *   <li>byte arrays: the raw bytes, without any header</li>
 * </ul>
 */
public final class BasicSerializers
{

    /**
     * Ids used to register the basic serializers in a {@link SerializerRegistry}.
     */
    public static final int ID_STRING = 1;

    public static final int ID_INTEGER = 2;

    public static final int ID_LONG = 3;

    public static final int ID_FLOAT = 4;

    public static final int ID_DOUBLE = 5;

    public static final int ID_BIG_INTEGER = 6;

    public static final int ID_BIG_DECIMAL = 7;

    public static final int ID_LOCAL_DATE = 8;

    public static final int ID_LOCAL_TIME = 9;

    public static final int ID_LOCAL_DATE_TIME = 10;

    public static final int ID_ZONED_DATE_TIME = 11;

    public static final int ID_ZONE_ID = 12;

    public static final int ID_BASE64 = 13;

    public static final int ID_BOOLEAN_ARRAY = 14;

    public static final int ID_BYTE_ARRAY = 15;

    public static final int ID_CHAR_ARRAY = 16;

    public static final int ID_SHORT_ARRAY = 17;

    public static final int ID_INT_ARRAY = 18;

    public static final int ID_LONG_ARRAY = 19;

    public static final int ID_FLOAT_ARRAY = 20;

    public static final int ID_DOUBLE_ARRAY = 21;

    public static final int ID_STRING_ARRAY = 22;

    public static final int ID_BIG_INTEGER_ARRAY = 23;

    public static final int ID_BIG_DECIMAL_ARRAY = 24;

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

    public static final Serializer<boolean[]> BOOLEAN_ARRAY = new Serializer<boolean[]>()
    {
        @Override
        public byte[] serialize(boolean[] values)
        {
            if (values == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + values.length);
            buffer.putInt(values.length);
            for (boolean value : values)
            {
                buffer.put(value ? (byte) 1 : (byte) 0);
            }
            return buffer.array();
        }

        @Override
        public boolean[] deserialize(byte[] data)
        {
            if (data == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = readArrayLength(buffer, data.length, 1);
            boolean[] values = new boolean[length];
            for (int i = 0; i < length; i++)
            {
                values[i] = buffer.get() != 0;
            }
            return values;
        }
    };

    public static final Serializer<byte[]> BYTE_ARRAY = new Serializer<byte[]>()
    {
        @Override
        public byte[] serialize(byte[] values)
        {
            return values;
        }

        @Override
        public byte[] deserialize(byte[] data)
        {
            return data;
        }
    };

    public static final Serializer<char[]> CHAR_ARRAY = new Serializer<char[]>()
    {
        @Override
        public byte[] serialize(char[] values)
        {
            if (values == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + values.length * Character.BYTES);
            buffer.putInt(values.length);
            for (char value : values)
            {
                buffer.putChar(value);
            }
            return buffer.array();
        }

        @Override
        public char[] deserialize(byte[] data)
        {
            if (data == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = readArrayLength(buffer, data.length, Character.BYTES);
            char[] values = new char[length];
            for (int i = 0; i < length; i++)
            {
                values[i] = buffer.getChar();
            }
            return values;
        }
    };

    public static final Serializer<short[]> SHORT_ARRAY = new Serializer<short[]>()
    {
        @Override
        public byte[] serialize(short[] values)
        {
            if (values == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + values.length * Short.BYTES);
            buffer.putInt(values.length);
            for (short value : values)
            {
                buffer.putShort(value);
            }
            return buffer.array();
        }

        @Override
        public short[] deserialize(byte[] data)
        {
            if (data == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = readArrayLength(buffer, data.length, Short.BYTES);
            short[] values = new short[length];
            for (int i = 0; i < length; i++)
            {
                values[i] = buffer.getShort();
            }
            return values;
        }
    };

    public static final Serializer<int[]> INT_ARRAY = new Serializer<int[]>()
    {
        @Override
        public byte[] serialize(int[] values)
        {
            if (values == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + values.length * Integer.BYTES);
            buffer.putInt(values.length);
            for (int value : values)
            {
                buffer.putInt(value);
            }
            return buffer.array();
        }

        @Override
        public int[] deserialize(byte[] data)
        {
            if (data == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = readArrayLength(buffer, data.length, Integer.BYTES);
            int[] values = new int[length];
            for (int i = 0; i < length; i++)
            {
                values[i] = buffer.getInt();
            }
            return values;
        }
    };

    public static final Serializer<long[]> LONG_ARRAY = new Serializer<long[]>()
    {
        @Override
        public byte[] serialize(long[] values)
        {
            if (values == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + values.length * Long.BYTES);
            buffer.putInt(values.length);
            for (long value : values)
            {
                buffer.putLong(value);
            }
            return buffer.array();
        }

        @Override
        public long[] deserialize(byte[] data)
        {
            if (data == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = readArrayLength(buffer, data.length, Long.BYTES);
            long[] values = new long[length];
            for (int i = 0; i < length; i++)
            {
                values[i] = buffer.getLong();
            }
            return values;
        }
    };

    public static final Serializer<float[]> FLOAT_ARRAY = new Serializer<float[]>()
    {
        @Override
        public byte[] serialize(float[] values)
        {
            if (values == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + values.length * Float.BYTES);
            buffer.putInt(values.length);
            for (float value : values)
            {
                buffer.putFloat(value);
            }
            return buffer.array();
        }

        @Override
        public float[] deserialize(byte[] data)
        {
            if (data == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = readArrayLength(buffer, data.length, Float.BYTES);
            float[] values = new float[length];
            for (int i = 0; i < length; i++)
            {
                values[i] = buffer.getFloat();
            }
            return values;
        }
    };

    public static final Serializer<double[]> DOUBLE_ARRAY = new Serializer<double[]>()
    {
        @Override
        public byte[] serialize(double[] values)
        {
            if (values == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + values.length * Double.BYTES);
            buffer.putInt(values.length);
            for (double value : values)
            {
                buffer.putDouble(value);
            }
            return buffer.array();
        }

        @Override
        public double[] deserialize(byte[] data)
        {
            if (data == null)
            {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = readArrayLength(buffer, data.length, Double.BYTES);
            double[] values = new double[length];
            for (int i = 0; i < length; i++)
            {
                values[i] = buffer.getDouble();
            }
            return values;
        }
    };

    public static final Serializer<String[]> STRING_ARRAY = textArray(String.class,
            value -> value, text -> text);

    public static final Serializer<BigInteger[]> BIG_INTEGER_ARRAY = textArray(BigInteger.class,
            BigInteger::toString, BigInteger::new);

    public static final Serializer<BigDecimal[]> BIG_DECIMAL_ARRAY = textArray(BigDecimal.class,
            BigDecimal::toString, BigDecimal::new);

    public static void registerAll(SerializerRegistry registry)
    {
        registry.register(String.class, ID_STRING, STRING);
        registry.register(Integer.class, ID_INTEGER, INTEGER);
        registry.register(Long.class, ID_LONG, LONG);
        registry.register(Float.class, ID_FLOAT, FLOAT);
        registry.register(Double.class, ID_DOUBLE, DOUBLE);
        registry.register(BigInteger.class, ID_BIG_INTEGER, BIG_INTEGER);
        registry.register(BigDecimal.class, ID_BIG_DECIMAL, BIG_DECIMAL);
        registry.register(LocalDate.class, ID_LOCAL_DATE, LOCAL_DATE);
        registry.register(LocalTime.class, ID_LOCAL_TIME, LOCAL_TIME);
        registry.register(LocalDateTime.class, ID_LOCAL_DATE_TIME, LOCAL_DATE_TIME);
        registry.register(ZonedDateTime.class, ID_ZONED_DATE_TIME, ZONED_DATE_TIME);
        registry.register(ZoneId.class, ID_ZONE_ID, ZONE_ID);
        registry.register(boolean[].class, ID_BOOLEAN_ARRAY, BOOLEAN_ARRAY);
        registry.register(byte[].class, ID_BYTE_ARRAY, BYTE_ARRAY);
        registry.register(char[].class, ID_CHAR_ARRAY, CHAR_ARRAY);
        registry.register(short[].class, ID_SHORT_ARRAY, SHORT_ARRAY);
        registry.register(int[].class, ID_INT_ARRAY, INT_ARRAY);
        registry.register(long[].class, ID_LONG_ARRAY, LONG_ARRAY);
        registry.register(float[].class, ID_FLOAT_ARRAY, FLOAT_ARRAY);
        registry.register(double[].class, ID_DOUBLE_ARRAY, DOUBLE_ARRAY);
        registry.register(String[].class, ID_STRING_ARRAY, STRING_ARRAY);
        registry.register(BigInteger[].class, ID_BIG_INTEGER_ARRAY, BIG_INTEGER_ARRAY);
        registry.register(BigDecimal[].class, ID_BIG_DECIMAL_ARRAY, BIG_DECIMAL_ARRAY);
    }

    /**
     * Reads the number of elements of an array of fixed width elements,
     * checking that the data is long enough to hold them.
     *
     * @param buffer      The buffer positioned at the count.
     * @param dataLength  The total length of the data.
     * @param elementSize The size in bytes of every element.
     * @return The number of elements.
     * @throws IllegalArgumentException If the data is too short or the count is invalid.
     */
    private static int readArrayLength(ByteBuffer buffer, int dataLength, int elementSize)
    {
        if (dataLength < Integer.BYTES)
        {
            throw new IllegalArgumentException("Invalid data length " + dataLength);
        }
        int length = buffer.getInt();
        if (length < 0 || (long) length * elementSize > dataLength - Integer.BYTES)
        {
            throw new IllegalArgumentException("Invalid array length " + length + " for " + dataLength + " bytes of data");
        }
        return length;
    }

    /**
     * Creates a serializer for an array of elements encoded as text, each one
     * prefixed by its length in bytes.
     *
     * @param <T>           The type of the elements.
     * @param componentType The class of the elements.
     * @param toText        The function to convert an element into its text.
     * @param fromText      The function to convert a text into an element.
     * @return The serializer of the array.
     */
    private static <T> Serializer<T[]> textArray(Class<T> componentType, Function<T, String> toText,
            Function<String, T> fromText)
    {
        return new Serializer<T[]>()
        {
            @Override
            public byte[] serialize(T[] values)
            {
                if (values == null)
                {
                    return null;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream data = new DataOutputStream(out);
                try
                {
                    data.writeInt(values.length);
                    for (T value : values)
                    {
                        byte[] bytes = toText.apply(value).getBytes(StandardCharsets.UTF_8);
                        data.writeInt(bytes.length);
                        data.write(bytes);
                    }
                    data.flush();
                }
                catch (IOException e)
                {
                    throw new IllegalStateException(e);
                }
                return out.toByteArray();
            }

            @Override
            @SuppressWarnings("unchecked")
            public T[] deserialize(byte[] data)
            {
                if (data == null)
                {
                    return null;
                }
                DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
                try
                {
                    int length = input.readInt();
                    if (length < 0 || length > input.available() / Integer.BYTES)
                    {
                        throw new IllegalArgumentException("Invalid array length " + length);
                    }
                    T[] values = (T[]) Array.newInstance(componentType, length);
                    for (int i = 0; i < length; i++)
                    {
                        int size = input.readInt();
                        if (size < 0 || size > input.available())
                        {
                            throw new IllegalArgumentException("Invalid element length " + size);
                        }
                        byte[] text = new byte[size];
                        input.readFully(text);
                        values[i] = fromText.apply(new String(text, StandardCharsets.UTF_8));
                    }
                    return values;
                }
                catch (IOException e)
                {
                    throw new IllegalArgumentException("Invalid data", e);
                }
            }
        };
    }
}
