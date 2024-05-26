/*
 *  Secp256k1.java
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

/**
 *
 * @author franci
 */
public class Secp256k1 extends Curve
{    
    public static final Secp256k1 INSTANCE = new Secp256k1();
    
    public Secp256k1()
    {
        super(32,    
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141",
            "0000000000000000000000000000000000000000000000000000000000000000",
            "0000000000000000000000000000000000000000000000000000000000000007",
            "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798",
            "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8",
            "01");
    }
    
    //https://learnmeabitcoin.com/technical/public-key#compressed
    //https://en.bitcoin.it/wiki/Secp256k1
    
    /**
     * Generate a random private key that can be used with Secp256k1.
     * 
     * https://cryptobook.nakov.com/digital-signatures/ecdsa-sign-verify-messages
     * 
     * @return
     */
    @Override
    public final byte[] genSecKey()
    {
        byte[] sk = new byte[32];
        for(BigInteger bi = new BigInteger(sk);bi.compareTo(BigInteger.ONE)<=0 || bi.compareTo(this.n)>=0;)
        {
            secureRandom.nextBytes(sk);
            bi = new BigInteger(sk);
        }
        return sk;
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    private static final BigInteger _2L_ = BigInteger.valueOf(2);
    private static final BigInteger _3L_ = BigInteger.valueOf(3);
    private static final BigInteger _4L_ = BigInteger.valueOf(4);
    private static final BigInteger _7L_ = BigInteger.valueOf(7);

    private static final Point INFINITY_POINT = new Point(null, (BigInteger)null, (BigInteger)null);
    
    public final Point add(Point p1, Point p2)
    {
        if ((p1 != null && p2 != null && p1.isInfinite() && p2.isInfinite()))
        {
            return INFINITY_POINT;
        }
        if (p1 == null || p1.isInfinite())
        {
            return p2;
        }
        if (p2 == null || p2.isInfinite())
        {
            return p1;
        }
        if (p1.x.equals(p2.x) && !p1.y.equals(p2.y))
        {
            return INFINITY_POINT;
        }
        BigInteger lam;
        if (p1.equals(p2))
        {
            BigInteger base = p2.y.multiply(_2L_);
            lam = (_3L_.multiply(p1.x).multiply(p1.x).multiply(base.modPow(this.p.subtract(_2L_), this.p))).mod(this.p);
        } 
        else
        {
            BigInteger base = p2.x.subtract(p1.x);
            lam = ((p2.y.subtract(p1.y)).multiply(base.modPow(this.p.subtract(_2L_), this.p))).mod(this.p);
        }

        BigInteger x3 = (lam.multiply(lam).subtract(p1.x).subtract(p2.x)).mod(this.p);
        return new Point(this, x3, lam.multiply(p1.x.subtract(x3)).subtract(p1.y).mod(this.p));
    }

    @Override
    public final Point mul(Point P, BigInteger n)
    {
        Point R = null;

        for (int i = 0; i < this.bits; i++)
        {
            if (n.shiftRight(i).and(BigInteger.ONE).compareTo(BigInteger.ZERO) > 0)
            {
                R = add(R, P);
            }
            P = add(P, P);
        }

        return R;
    }

    @Override
    public final Point liftX(BigInteger x, byte parity)
    {
        if (x.compareTo(this.p) >= 0)
        {
            return null;
        }
        BigInteger y_sq = x.modPow(_3L_, this.p).add(_7L_).mod(this.p);
        BigInteger y = y_sq.modPow(this.p.add(BigInteger.ONE).divide(_4L_), this.p);

        if (y.modPow(_2L_, this.p).compareTo(y_sq) != 0)
        {
            return null;
        } 
        
        if( (parity==3 && Nums.isEven(y)) || (parity!=3 && Nums.isOdd(y)) )
        {
            y = this.p.subtract(y).mod(this.p);
        }
        return new Point(this, x, y);
    }
  
}
