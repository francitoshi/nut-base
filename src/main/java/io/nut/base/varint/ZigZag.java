/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.varint;

import java.math.BigInteger;

/**
 * Utility class for ZigZag encoding and decoding of signed values.
 * <p>
 * ZigZag encoding maps signed integers to unsigned-ish integers so that small
 * negative values map to small positive values, which makes variable-length
 * encodings (e.g. varint) more compact.
 * <ul>
 *   <li>{@code 0} maps to {@code 0}</li>
 *   <li>{@code -1} maps to {@code 1}</li>
 *   <li>{@code 1} maps to {@code 2}</li>
 *   <li>{@code -2} maps to {@code 3}</li>
 *   <li>and so on</li>
 * </ul>
 *
 * @author franci
 */
public final class ZigZag
{
    private ZigZag()
    {
    }

    /**
     * Encodes the given int value using ZigZag encoding.
     *
     * @param value the int value to encode.
     * @return the ZigZag-encoded int value.
     */
    public static int encode(int value)
    {
        return (value << 1) ^ (value >> 31);
    }

    /**
     * Decodes the given ZigZag-encoded int value.
     *
     * @param value the ZigZag-encoded int value to decode.
     * @return the decoded int value.
     */
    public static int decode(int value)
    {
        return (value >>> 1) ^ -(value & 1);
    }

    /**
     * Encodes the given long value using ZigZag encoding.
     *
     * @param value the long value to encode.
     * @return the ZigZag-encoded long value.
     */
    public static long encode(long value)
    {
        return (value << 1) ^ (value >> 63);
    }

    /**
     * Decodes the given ZigZag-encoded long value.
     *
     * @param value the ZigZag-encoded long value to decode.
     * @return the decoded long value.
     */
    public static long decode(long value)
    {
        return (value >>> 1) ^ -(value & 1);
    }

    /**
     * Encodes the given BigInteger value using ZigZag encoding.
     *
     * @param value the BigInteger value to encode.
     * @return the ZigZag-encoded BigInteger value.
     * @throws NullPointerException if value is null.
     */
    public static BigInteger encode(BigInteger value)
    {
        BigInteger twice = value.shiftLeft(1);
        return value.signum() < 0 ? twice.negate().subtract(BigInteger.ONE) : twice;
    }

    /**
     * Decodes the given ZigZag-encoded BigInteger value.
     *
     * @param value the ZigZag-encoded BigInteger value to decode.
     * @return the decoded BigInteger value.
     * @throws NullPointerException if value is null.
     */
    public static BigInteger decode(BigInteger value)
    {
        BigInteger half = value.shiftRight(1);
        return value.testBit(0) ? half.add(BigInteger.ONE).negate() : half;
    }
}