/*
 *  BigRational.java
 *
 *  Copyright (c) 2012-2024 francitoshi@gmail.com
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
package io.nut.base.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class BigRational extends Number implements Comparable<BigRational> 
{
    public static final BigRational ZERO = new BigRational(BigInteger.ZERO,BigInteger.ONE);
    public static final BigRational ONE = new BigRational(BigInteger.ONE,BigInteger.ONE);
    public static final BigRational TEN = new BigRational(BigInteger.TEN,BigInteger.ONE);
    
    final BigInteger n;
    final BigInteger d;

    public BigRational(BigInteger n, BigInteger d, boolean simplified)
    {
        this.n = n;
        this.d = d;
    }
    
    public BigRational(BigInteger n, BigInteger d)
    {
        this.n = n;
        this.d = d;
    }

    public BigRational add(long value)
    {
        BigInteger nn = n.add(BigInteger.valueOf(value).multiply(d));
        return new BigRational(nn, d);
    }

    public BigRational add(BigInteger value)
    {
        BigInteger nn = n.add(value.multiply(d));
        return new BigRational(nn, d);
    }

    public BigRational add(BigRational value)
    {
        if(value.d.equals(BigInteger.ONE))
        {
            return add(value.n);
        }
        if(value.d.equals(d))
        {
            return new BigRational(n.add(value.n), d);
        }
        //podría verificarse si un denominador es multiplo del otro
        //también podría usarse el mínimo comun múltiplo
        //además podrí establecerse una variable para controlar el comportamiento
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
        return add(value.negate());
    }

    public BigRational sub(BigRational value)
    {
        return add(value.negate());
    }

    public BigRational mul(long value)
    {
        return new BigRational(this.n.multiply(BigInteger.valueOf(value)), d);
    }

    public BigRational mul(BigInteger value)
    {
        return new BigRational(this.n.multiply(value), d);
    }

    public BigRational mul(BigRational value)
    {
        return new BigRational(this.n.multiply(value.n),this.d.multiply(value.d));
    }

    public BigRational div(long value)
    {
        return new BigRational(this.n, this.d.multiply(BigInteger.valueOf(value)));
    }

    public BigRational div(BigInteger value)
    {
        return new BigRational(this.n, this.d.multiply(value));
    }

    public BigRational div(BigRational value)
    {
        return new BigRational(this.n.multiply(value.d), this.d.multiply(value.n));
    }
    public BigRational negate()
    {
        return new BigRational(this.n.negate(),this.d);
    }
    @Override
    public int intValue()
    {
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
        BigDecimal dn = new BigDecimal(this.n);
        BigDecimal dd = new BigDecimal(this.d);
        return dn.divide(dd).floatValue();
    }

    @Override
    public double doubleValue()
    {
        BigDecimal dn = new BigDecimal(this.n);
        BigDecimal dd = new BigDecimal(this.d);
        return dn.divide(dd).doubleValue();
    }

    public BigDecimal BigDecimalValue(MathContext mc)
    {
        BigDecimal dn = new BigDecimal(this.n);
        BigDecimal dd = new BigDecimal(this.d);
        return dn.divide(dd, mc);
    }

    @Override
    public int compareTo(BigRational other)
    {
        BigInteger n1 = this.n.multiply(other.d);
        BigInteger n2 = other.n.multiply(this.d);
        return n1.compareTo(n2);
    }
    public static BigRational valueOf(long value)
    {
        return valueOf(BigInteger.valueOf(value));
    }
    public static BigRational valueOf(BigInteger value)
    {
        return new BigRational(value, BigInteger.ONE);
    }

    public BigRational simplify()
    {
        BigInteger gcd = Nums.gcd(n, d);
        if(gcd.equals(BigInteger.ONE))
        {
            return this;
        }
        return new BigRational(n.divide(gcd), d.divide(gcd));
    }

    public String toString(boolean simple)
    {
        if(simple && this.d.equals(BigInteger.ONE))
        {
            return this.n.toString();
        }
        return this.n.toString()+"/"+this.d.toString();
    }

    @Override
    public int hashCode()
    {
        return ( this.n.hashCode() ^ this.d.hashCode() );
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final BigRational other = (BigRational) obj;
        if (this.n != other.n && (this.n == null || !this.n.equals(other.n)))
            return false;
        if (this.d != other.d && (this.d == null || !this.d.equals(other.d)))
            return false;
        return true;
    }
    
    
    public String toString()
    {
        return toString(false);
    }
    public static BigRational build(double value, int precision)
    {
        return build(BigDecimal.valueOf(value), precision, 0.0);
    }
    public static BigRational build(BigDecimal value, int precision)
    {
        return build(value, precision, 0.0);
    }
    public static BigRational build(double value, int precision, double delta)
    {
        return build(BigDecimal.valueOf(value), precision, delta);
    }
    public static BigRational build(BigDecimal value, int precision, double delta)
    {
        final MathContext mc = new MathContext(precision);
        
        if(value.equals(BigDecimal.ZERO))
        {
            return BigRational.ZERO;
        }
        if(value.equals(BigDecimal.ONE))
        {
            return BigRational.ONE;
        }
                
        boolean negative = value.compareTo(BigDecimal.ZERO)<0;
        value = value.abs();
        
        BigInteger n = BigInteger.ONE;
        BigInteger d = BigInteger.ONE;
        
        BigDecimal cur = new BigDecimal(n).divide(new BigDecimal(d), mc);
        BigDecimal pre = value;
        
        while( !cur.equals(value) && !cur.equals(pre) && (delta==0.0 || cur.subtract(value).abs().doubleValue()>delta))
        {
            pre = cur;
            
            if(cur.compareTo(value)<0)
            {
                n = n.add(BigInteger.ONE);
            }
            else
            {
                d = d.add(BigInteger.ONE);
            }
            cur = new BigDecimal(n).divide(new BigDecimal(d), mc);
        }
        return negative ?  new BigRational(n.negate(), d) : new BigRational(n, d);
    }
    
}
