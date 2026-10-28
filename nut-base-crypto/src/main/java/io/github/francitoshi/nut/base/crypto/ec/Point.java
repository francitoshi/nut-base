/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto.ec;

import io.github.francitoshi.nut.base.commons.lang.BigMaths;
import java.math.BigInteger;
import java.util.Objects;

public class Point
{
    public static final Point INFINITY_POINT = new Point(null, (BigInteger) null, (BigInteger) null);

    public final Curve curve;
    public final BigInteger x;
    public final BigInteger y;

    public Point(Curve curve, BigInteger x, BigInteger y)
    {
        this.curve = curve;
        this.x = x;
        this.y = y;
    }

    public Point(Curve curve, byte[] x, byte[] y)
    {
        this(curve, new BigInteger(1, x), new BigInteger(1, y));
    }

    public boolean isInfinite()
    {
        return this.x == null || this.y == null;
    }

    public boolean hasEvenY()
    {
        return BigMaths.isEven(this.y);
    }


    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.x);
        hash = 59 * hash + Objects.hashCode(this.y);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(obj == null)
        {
            return false;
        }
        if(getClass() != obj.getClass())
        {
            return false;
        }
        final Point other = (Point) obj;
        if(this.isInfinite() && other.isInfinite())
        {
            return true;
        }
        if(this.isInfinite() || other.isInfinite())
        {
            return false;
        }
        return this.x.compareTo(other.x)==0 && this.y.compareTo(other.y)==0;
    }

    public Point add(Point other)
    {
        return curve.add(this, other);
    }

    public Point mul(BigInteger n)
    {
        return curve.mul(this, n);
    }

    @Override
    public String toString()
    {
        return toString(16);
    }
    public String toString(int radix)
    {
        return x.toString(radix) + " - " + y.toString(radix);
    }
    
}
