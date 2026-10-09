/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.alu;

import io.nut.base.math.BigRational;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * ALU implementation for {@link BigDecimal}.
 */
public class BigDecimalALU implements ALU<BigDecimal>
{
    private final MathContext mathContext;

    public BigDecimalALU()
    {
        this(MathContext.DECIMAL128);
    }

    public BigDecimalALU(MathContext mathContext)
    {
        this.mathContext = mathContext != null ? mathContext : MathContext.DECIMAL128;
    }

    @Override
    public BigDecimal add(BigDecimal a, BigDecimal b)
    {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return a.add(b);
    }

    @Override
    public BigDecimal sub(BigDecimal a, BigDecimal b)
    {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return a.subtract(b);
    }

    @Override
    public BigDecimal mul(BigDecimal a, BigDecimal b)
    {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return a.multiply(b);
    }

    @Override
    public BigDecimal div(BigDecimal a, BigDecimal b)
    {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null || b.signum() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return a.divide(b, mathContext);
    }

    @Override
    public BigDecimal zero()
    {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal one()
    {
        return BigDecimal.ONE;
    }

    @Override
    public BigDecimal fromInt(int value)
    {
        return BigDecimal.valueOf(value);
    }

    @Override
    public BigDecimal fromLong(long value)
    {
        return BigDecimal.valueOf(value);
    }

    @Override
    public BigDecimal fromFloat(float value)
    {
        return BigDecimal.valueOf(value);
    }

    @Override
    public BigDecimal fromDouble(double value)
    {
        return BigDecimal.valueOf(value);
    }

    @Override
    public BigDecimal fromBigInteger(BigInteger value)
    {
        return new BigDecimal(value);
    }

    @Override
    public BigDecimal fromBigDecimal(BigDecimal value)
    {
        return value;
    }

    @Override
    public BigDecimal fromBigRational(BigRational value)
    {
        return new BigDecimal(value.numerator()).divide(new BigDecimal(value.denominator()), mathContext);
    }

    @Override
    public BigDecimal sqrt(BigDecimal value)
    {
        if (value == null)
        {
            value = BigDecimal.ZERO;
        }
        if (value.signum() < 0)
        {
            throw new ArithmeticException("Square root of negative value");
        }
        if (value.signum() == 0)
        {
            return BigDecimal.ZERO;
        }
        MathContext work = new MathContext(mathContext.getPrecision() + 10, mathContext.getRoundingMode());
        BigDecimal x;
        if (value.compareTo(BigDecimal.ONE) >= 0)
        {
            int magnitude = value.precision() - value.scale();
            x = BigDecimal.ONE.scaleByPowerOfTen(magnitude / 2 + 1);
        }
        else
        {
            x = BigDecimal.ONE;
        }
        BigDecimal two = BigDecimal.valueOf(2);
        for (int i = 0; i < 1000; i++)
        {
            BigDecimal next = x.add(value.divide(x, work)).divide(two, work);
            if (next.compareTo(x) == 0)
            {
                return next.round(mathContext);
            }
            x = next;
        }
        return x.round(mathContext);
    }

    @Override
    public BigDecimal abs(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value.abs();
    }

    @Override
    public boolean equals(BigDecimal a, BigDecimal b)
    {
        BigDecimal left = a == null ? BigDecimal.ZERO : a;
        BigDecimal right = b == null ? BigDecimal.ZERO : b;
        return left.compareTo(right) == 0;
    }

    @Override
    public int compare(BigDecimal a, BigDecimal b)
    {
        BigDecimal left = a == null ? BigDecimal.ZERO : a;
        BigDecimal right = b == null ? BigDecimal.ZERO : b;
        return left.compareTo(right);
    }
}
