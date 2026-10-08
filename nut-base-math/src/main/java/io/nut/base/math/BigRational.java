/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

/**
 * BigRational represents a rational number as an immutable fraction n/d.
 * Denominator is always positive and the value is reduced to lowest terms.
 */
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

public class BigRational extends Number implements Comparable<BigRational>
{

    public static final BigRational ZERO = new BigRational(BigInteger.ZERO, BigInteger.ONE, true);
    public static final BigRational ONE = new BigRational(BigInteger.ONE, BigInteger.ONE, true);
    public static final BigRational TWO = new BigRational(2);
    public static final BigRational TEN = new BigRational(BigInteger.TEN, BigInteger.ONE, true);

    public final BigInteger n;
    public final BigInteger d;
    final boolean simplified;

    private BigRational(BigInteger n, BigInteger d, boolean simplified)
    {
        this.n = n;
        this.d = d;
        this.simplified = simplified;
    }

    public BigRational(BigInteger n, BigInteger d)
    {
        if (d == null || n == null)
        {
            throw new NullPointerException();
        }
        if (d.signum() == 0)
        {
            throw new ArithmeticException("Division by zero");
        }
        if (n.signum() == 0)
        {
            this.n = BigInteger.ZERO;
            this.d = BigInteger.ONE;
            this.simplified = true;
            return;
        }
        // normalize sign: denominator positive
        BigInteger num = n;
        BigInteger den = d;
        if (den.signum() < 0)
        {
            num = num.negate();
            den = den.negate();
        }
        // reduce
        BigInteger g = num.gcd(den);
        if (g.equals(BigInteger.ONE))
        {
            this.n = num;
            this.d = den;
            this.simplified = false;
        }
        else
        {
            this.n = num.divide(g);
            this.d = den.divide(g);
            this.simplified = true;
        }
    }

    public BigRational()
    {
        this.n = BigInteger.ZERO;
        this.d = BigInteger.ONE;
        this.simplified = true;
    }

    public BigRational(long n)
    {
        this(BigInteger.valueOf(n), BigInteger.ONE);
    }

    public BigRational(long n, long d)
    {
        this(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }

    public BigRational(BigInteger n)
    {
        this(n, BigInteger.ONE);
    }

    /**
     * Returns a rational representing the specified value.
     *
     * @param value the long value
     * @return a BigRational instance
     */
    public static BigRational valueOf(long value)
    {
        return valueOf(BigInteger.valueOf(value));
    }

    public static BigRational valueOf(BigInteger value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.signum() == 0)
        {
            return ZERO;
        }
        if (value.equals(BigInteger.ONE))
        {
            return ONE;
        }
        if (value.equals(BigInteger.TEN))
        {
            return TEN;
        }
        return new BigRational(value, BigInteger.ONE, true);
    }

    public static BigRational valueOf(BigDecimal value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.signum() == 0)
        {
            return ZERO;
        }
        // try to convert via scale if exact
        int scale = value.scale();
        if (scale >= 0)
        {
            BigInteger unscaled = value.unscaledValue();
            BigInteger den = BigInteger.TEN.pow(scale);
            return new BigRational(unscaled, den);
        }
        else
        {
            // scale < 0 means * 10^|scale|
            int absScale = -scale;
            BigInteger den = BigInteger.ONE;
            BigInteger num = value.unscaledValue().multiply(BigInteger.TEN.pow(absScale));
            return new BigRational(num, den);
        }
    }

    public BigRational add(long value)
    {
        return add(BigInteger.valueOf(value));
    }

    public BigRational add(BigInteger value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.signum() == 0)
        {
            return this;
        }
        BigInteger nn = this.n.add(value.multiply(this.d));
        return new BigRational(nn, this.d);
    }

    public BigRational add(BigRational value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.equals(ZERO))
        {
            return this;
        }
        if (this.equals(ZERO))
        {
            return value;
        }
        if (value.d.equals(this.d))
        {
            BigInteger nn = this.n.add(value.n);
            return new BigRational(nn, this.d);
        }
        BigInteger nn = this.n.multiply(value.d).add(value.n.multiply(this.d));
        BigInteger dd = this.d.multiply(value.d);
        return new BigRational(nn, dd);
    }

    public BigRational sub(long value)
    {
        return add(-value);
    }

    public BigRational sub(BigInteger value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        return add(value.negate());
    }

    public BigRational sub(BigRational value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        return add(value.negate());
    }

    public BigRational mul(long value)
    {
        return mul(BigInteger.valueOf(value));
    }

    public BigRational mul(BigInteger value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.signum() == 0 || this.signum() == 0)
        {
            return ZERO;
        }
        if (value.equals(BigInteger.ONE))
        {
            return this;
        }
        if (this.equals(ONE))
        {
            return valueOf(value);
        }
        return new BigRational(this.n.multiply(value), this.d);
    }

    public BigRational mul(BigRational value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.signum() == 0 || this.signum() == 0)
        {
            return ZERO;
        }
        return new BigRational(this.n.multiply(value.n), this.d.multiply(value.d));
    }

    public BigRational div(long value)
    {
        return div(BigInteger.valueOf(value));
    }

    public BigRational div(BigInteger value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.signum() == 0)
        {
            throw new ArithmeticException("Division by zero");
        }
        if (value.equals(BigInteger.ONE))
        {
            return this;
        }
        return new BigRational(this.n, this.d.multiply(value));
    }

    public BigRational div(BigRational value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.signum() == 0)
        {
            throw new ArithmeticException("Division by zero");
        }
        if (value.equals(ONE))
        {
            return this;
        }
        return new BigRational(this.n.multiply(value.d), this.d.multiply(value.n));
    }

    public BigRational negate()
    {
        if (this.signum() == 0)
        {
            return ZERO;
        }
        if (this.equals(ONE))
        {
            return valueOf(-1);
        }
        return new BigRational(this.n.negate(), this.d, this.simplified);
    }

    public BigRational abs()
    {
        if (signum() >= 0)
        {
            return this;
        }
        return negate();
    }

    public int signum()
    {
        return n.signum();
    }

    public boolean isZero()
    {
        return signum() == 0;
    }

    public boolean isPositive()
    {
        return signum() > 0;
    }

    public boolean isNegative()
    {
        return signum() < 0;
    }

    public BigRational simplify()
    {
        if (this.simplified)
        {
            return this;
        }
        if (this.n.signum() == 0)
        {
            return ZERO;
        }
        BigInteger g = this.n.gcd(this.d);
        if (g.equals(BigInteger.ONE))
        {
            return new BigRational(this.n, this.d, true);
        }
        return new BigRational(this.n.divide(g), this.d.divide(g), true);
    }

    public BigInteger numerator()
    {
        return this.n;
    }

    public BigInteger denominator()
    {
        return this.d;
    }

    public static BigRational build(BigDecimal value, int precision)
    {
        return build(value, precision, 0.0);
    }

    public static BigRational build(BigDecimal value, int precision, double epsilon)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        if (value.signum() == 0)
        {
            return ZERO;
        }
        if (epsilon < 0.0)
        {
            epsilon = 0.0;
        }
        // try to find best approximation using fraction
        // use continued fraction approach similar to common implementations
        BigDecimal numVal = value;
        int maxDen = 1;
        for (int i = 0; i < precision; i++)
        {
            maxDen *= 10;
        }
        // if precision is small, also allow up to reasonable
        if (maxDen < 1000000)
        {
            maxDen = 1000000;
        }
        // simple method: convergents
        return fromDoubleApprox(numVal.doubleValue(), maxDen, epsilon);
    }

    private static BigRational fromDoubleApprox(double value, int maxDen, double epsilon)
    {
        if (Math.abs(value) < epsilon)
        {
            return ZERO;
        }
        boolean negative = value < 0;
        double x = Math.abs(value);
        long a0 = (long) Math.floor(x);
        if (a0 > 1000000000)
        { // avoid huge
            a0 = 1000000000;
        }
        double r = x - a0;
        if (r < epsilon)
        {
            BigRational res = valueOf(BigInteger.valueOf(a0));
            return negative ? res.negate() : res;
        }
        long p0 = 1, q0 = 0;
        long p1 = a0, q1 = 1;
        long p2 = 0, q2 = 1;
        int maxIterations = 200;
        for (int i = 0; i < maxIterations; i++)
        {
            if (Math.abs(r) < epsilon)
            {
                break;
            }
            double ai = 1.0 / r;
            long m = (long) Math.floor(ai + epsilon);
            if (m < 1)
            {
                m = 1;
            }
            p2 = m * p1 + p0;
            q2 = m * q1 + q0;
            if (q2 > maxDen)
            {
                // backtrack
                break;
            }
            p0 = p1;
            q0 = q1;
            p1 = p2;
            q1 = q2;
            r = ai - m;
            // check epsilon
            double approx = (double) p1 / (double) q1;
            if (Math.abs(x - approx) <= epsilon)
            {
                break;
            }
        }
        BigRational res = new BigRational(BigInteger.valueOf(p1), BigInteger.valueOf(q1));
        if (negative)
        {
            res = res.negate();
        }
        return res.simplify();
    }

    public static BigRational build(double value, int precision)
    {
        return build(BigDecimal.valueOf(value), precision);
    }

    public static BigRational build(double value, int precision, double epsilon)
    {
        return build(BigDecimal.valueOf(value), precision, epsilon);
    }

    public BigDecimal BigDecimalValue(MathContext mc)
    {
        if (mc == null)
        {
            throw new NullPointerException();
        }
        return new BigDecimal(this.n).divide(new BigDecimal(this.d), mc);
    }

    public BigDecimal toBigDecimal(MathContext mc)
    {
        return BigDecimalValue(mc);
    }

    public BigDecimal toBigDecimal(int scale, RoundingMode roundingMode)
    {
        return new BigDecimal(this.n).divide(new BigDecimal(this.d), scale, roundingMode);
    }

    @Override
    public int intValue()
    {
        // truncate toward zero
        return this.n.divide(this.d).intValue();
    }

    @Override
    public long longValue()
    {
        return this.n.divide(this.d).longValue();
    }

    @Override
    public float floatValue()
    {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue()
    {
        // use double approximation
        return new BigDecimal(this.n).divide(new BigDecimal(this.d), MathContext.DECIMAL128).doubleValue();
    }

    @Override
    public int compareTo(BigRational o)
    {
        if (o == null)
        {
            throw new NullPointerException();
        }
        if (this.equals(o))
        {
            return 0;
        }
        // cross multiply with sign normalization already done
        BigInteger left = this.n.multiply(o.d);
        BigInteger right = o.n.multiply(this.d);
        return left.compareTo(right);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof BigRational))
        {
            return false;
        }
        BigRational other = (BigRational) obj;
        // compare normalized reduced forms
        BigRational a = this.simplified ? this : this.simplify();
        BigRational b = other.simplified ? other : other.simplify();
        return a.n.equals(b.n) && a.d.equals(b.d);
    }

    @Override
    public int hashCode()
    {
        BigRational s = this.simplified ? this : this.simplify();
        return Objects.hash(s.n, s.d);
    }

    @Override
    public String toString()
    {
        return toString(false);
    }

    public String toString(boolean omitOne)
    {
        if (this.signum() == 0)
        {
            if (omitOne)
            {
                return "0";
            }
            return "0/1";
        }
        BigRational s = this.simplified ? this : this.simplify();
        if (omitOne && s.d.equals(BigInteger.ONE))
        {
            return s.n.toString();
        }
        return s.n + "/" + s.d;
    }
}
