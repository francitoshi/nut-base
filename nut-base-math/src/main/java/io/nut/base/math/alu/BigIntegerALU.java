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
 * ALU implementation for {@link BigInteger}.
 */
public class BigIntegerALU implements ALU<BigInteger>
{
    @Override
    public BigInteger add(BigInteger a, BigInteger b)
    {
        if (a == null) {
            a = BigInteger.ZERO;
        }
        if (b == null) {
            b = BigInteger.ZERO;
        }
        return a.add(b);
    }

    @Override
    public BigInteger sub(BigInteger a, BigInteger b)
    {
        if (a == null) {
            a = BigInteger.ZERO;
        }
        if (b == null) {
            b = BigInteger.ZERO;
        }
        return a.subtract(b);
    }

    @Override
    public BigInteger mul(BigInteger a, BigInteger b)
    {
        if (a == null) {
            a = BigInteger.ZERO;
        }
        if (b == null) {
            b = BigInteger.ZERO;
        }
        return a.multiply(b);
    }

    @Override
    public BigInteger div(BigInteger a, BigInteger b)
    {
        if (a == null) {
            a = BigInteger.ZERO;
        }
        if (b == null || b.signum() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return a.divide(b);
    }

    @Override
    public BigInteger zero()
    {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger one()
    {
        return BigInteger.ONE;
    }

    @Override
    public BigInteger fromInt(int value)
    {
        return BigInteger.valueOf(value);
    }

    @Override
    public BigInteger fromLong(long value)
    {
        return BigInteger.valueOf(value);
    }

    @Override
    public BigInteger fromFloat(float value)
    {
        return BigDecimal.valueOf(value).toBigInteger();
    }

    @Override
    public BigInteger fromDouble(double value)
    {
        return BigDecimal.valueOf(value).toBigInteger();
    }

    @Override
    public BigInteger fromBigInteger(BigInteger value)
    {
        return value;
    }

    @Override
    public BigInteger fromBigDecimal(BigDecimal value)
    {
        return value.toBigInteger();
    }

    @Override
    public BigInteger fromBigRational(BigRational value)
    {
        return value.numerator().divide(value.denominator());
    }

    @Override
    public BigInteger sqrt(BigInteger value)
    {
        if (value == null)
        {
            value = BigInteger.ZERO;
        }
        if (value.signum() < 0)
        {
            throw new ArithmeticException("Square root of negative value");
        }
        if (value.signum() == 0)
        {
            return BigInteger.ZERO;
        }
        BigInteger two = BigInteger.valueOf(2);
        BigInteger x = BigInteger.ONE.shiftLeft((value.bitLength() + 1) / 2);
        while (true)
        {
            BigInteger next = x.add(value.divide(x)).shiftRight(1);
            if (next.compareTo(x) >= 0)
            {
                return x;
            }
            x = next;
        }
    }

    @Override
    public BigInteger abs(BigInteger value)
    {
        return value == null ? BigInteger.ZERO : value.abs();
    }

    @Override
    public boolean equals(BigInteger a, BigInteger b)
    {
        BigInteger left = a == null ? BigInteger.ZERO : a;
        BigInteger right = b == null ? BigInteger.ZERO : b;
        return left.compareTo(right) == 0;
    }

    @Override
    public int compare(BigInteger a, BigInteger b)
    {
        BigInteger left = a == null ? BigInteger.ZERO : a;
        BigInteger right = b == null ? BigInteger.ZERO : b;
        return left.compareTo(right);
    }
}
