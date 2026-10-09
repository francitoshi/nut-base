/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.alu;

import io.nut.base.math.BigRational;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Arithmetic Logic Unit interface for numeric types.
 *
 * @param <N> the numeric type
 */
public interface ALU<N>
{
    /**
     * Add two numbers.
     *
     * @param a first operand
     * @param b second operand
     * @return a + b
     */
    N add(N a, N b);

    /**
     * Subtract two numbers.
     *
     * @param a first operand
     * @param b second operand
     * @return a - b
     */
    N sub(N a, N b);

    /**
     * Multiply two numbers.
     *
     * @param a first operand
     * @param b second operand
     * @return a * b
     */
    N mul(N a, N b);

    /**
     * Divide two numbers.
     *
     * @param a first operand
     * @param b second operand
     * @return a / b
     */
    N div(N a, N b);

    /**
     * Returns the additive identity (zero) of this ALU numeric type.
     *
     * @return the zero value
     */
    N zero();

    /**
     * Returns the multiplicative identity (one) of this ALU numeric type.
     *
     * @return the one value
     */
    N one();

    /**
     * Converts an {@code int} value to this ALU numeric type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    N fromInt(int value);

    /**
     * Converts a {@code long} value to this ALU numeric type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    N fromLong(long value);

    /**
     * Converts a {@code float} value to this ALU numeric type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    N fromFloat(float value);

    /**
     * Converts a {@code double} value to this ALU numeric type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    N fromDouble(double value);

    /**
     * Converts a {@link BigInteger} value to this ALU numeric type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    N fromBigInteger(BigInteger value);

    /**
     * Converts a {@link BigDecimal} value to this ALU numeric type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    N fromBigDecimal(BigDecimal value);

    /**
     * Converts a {@link BigRational} value to this ALU numeric type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    N fromBigRational(BigRational value);

    /**
     * Returns the square root of the given value.
     *
     * @param value the value (must not be negative)
     * @return the square root
     * @throws ArithmeticException if the value is negative or has no exact representation
     */
    N sqrt(N value);

    /**
     * Returns the absolute value of the given value.
     *
     * @param value the value
     * @return the absolute value
     */
    N abs(N value);

    /**
     * Returns true if both values are numerically equal.
     *
     * @param a first value
     * @param b second value
     * @return true if equal
     */
    boolean equals(N a, N b);

    /**
     * Compares the two values.
     *
     * @param a first value
     * @param b second value
     * @return a negative integer, zero, or a positive integer as {@code a} is
     *         less than, equal to, or greater than {@code b}
     */
    int compare(N a, N b);
}
