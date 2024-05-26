/*
 *  Point.java
 *
 *  Copyright (C) 2023 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.crypto.ec;

import io.nut.base.math.Nums;
import java.math.BigInteger;
import java.util.Objects;

public class Point
{
    public static Point INFINITY_POINT = new Point(null, (BigInteger) null, (BigInteger) null);

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
        return Nums.isEven(this.y);
        //666 return this.y.mod(BigInteger.TWO).compareTo(BigInteger.ZERO) == 0;
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
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Point other = (Point) obj;
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
