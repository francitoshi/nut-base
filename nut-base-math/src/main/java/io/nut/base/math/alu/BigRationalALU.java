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
 * ALU implementation for {@link BigRational}.
 */
public class BigRationalALU implements ALU<BigRational>
{
    @Override
    public BigRational add(BigRational a, BigRational b)
    {
        if (a == null) {
            a = BigRational.ZERO;
        }
        if (b == null) {
            b = BigRational.ZERO;
        }
        return a.add(b);
    }

    @Override
    public BigRational sub(BigRational a, BigRational b)
    {
        if (a == null) {
            a = BigRational.ZERO;
        }
        if (b == null) {
            b = BigRational.ZERO;
        }
        return a.sub(b);
    }

    @Override
    public BigRational mul(BigRational a, BigRational b)
    {
        if (a == null) {
            a = BigRational.ZERO;
        }
        if (b == null) {
            b = BigRational.ZERO;
        }
        return a.mul(b);
    }

    @Override
    public BigRational div(BigRational a, BigRational b)
    {
        if (a == null) {
            a = BigRational.ZERO;
        }
        if (b == null || b.signum() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return a.div(b);
    }

    @Override
    public BigRational zero()
    {
        return BigRational.ZERO;
    }

    @Override
    public BigRational one()
    {
        return BigRational.ONE;
    }

    @Override
    public BigRational fromInt(int value)
    {
        return BigRational.valueOf(value);
    }

    @Override
    public BigRational fromLong(long value)
    {
        return BigRational.valueOf(value);
    }

    @Override
    public BigRational fromFloat(float value)
    {
        return BigRational.valueOf(BigDecimal.valueOf(value));
    }

    @Override
    public BigRational fromDouble(double value)
    {
        return BigRational.valueOf(BigDecimal.valueOf(value));
    }

    @Override
    public BigRational fromBigInteger(BigInteger value)
    {
        return BigRational.valueOf(value);
    }

    @Override
    public BigRational fromBigDecimal(BigDecimal value)
    {
        return BigRational.valueOf(value);
    }

    @Override
    public BigRational fromBigRational(BigRational value)
    {
        return value;
    }

    @Override
    public BigRational sqrt(BigRational value)
    {
        if (value == null)
        {
            value = BigRational.ZERO;
        }
        if (value.signum() < 0)
        {
            throw new ArithmeticException("Square root of negative value");
        }
        if (value.signum() == 0)
        {
            return BigRational.ZERO;
        }
        BigRational s = value.simplify();
        BigInteger n = s.numerator();
        BigInteger d = s.denominator();
        BigInteger sn = integerSqrt(n);
        BigInteger sd = integerSqrt(d);
        if (!sn.multiply(sn).equals(n) || !sd.multiply(sd).equals(d))
        {
            throw new ArithmeticException("Square root is not rational: " + value);
        }
        return BigRational.valueOf(sn).div(sd);
    }

    private static BigInteger integerSqrt(BigInteger value)
    {
        if (value.signum() == 0)
        {
            return BigInteger.ZERO;
        }
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
    public BigRational abs(BigRational value)
    {
        return value == null ? BigRational.ZERO : value.abs();
    }

    @Override
    public boolean equals(BigRational a, BigRational b)
    {
        BigRational left = a == null ? BigRational.ZERO : a;
        BigRational right = b == null ? BigRational.ZERO : b;
        return left.equals(right);
    }

    @Override
    public int compare(BigRational a, BigRational b)
    {
        BigRational left = a == null ? BigRational.ZERO : a;
        BigRational right = b == null ? BigRational.ZERO : b;
        return left.compareTo(right);
    }
}
