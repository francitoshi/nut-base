/*
 *  Curve.java
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

import io.nut.base.compat.ByteBufferCompat;
import io.nut.base.math.Nums;
import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author franci
 */
public abstract class Curve
{        
    static BigInteger biHex(String hex)
    {
        return new BigInteger(hex.replace(" ", ""), 16);
    }
    
    public final int bits;
    public final int bytes;
    public final BigInteger p;
    public final BigInteger n;

    public final BigInteger a;
    public final BigInteger b;

    public final Point G;

    public final BigInteger h;

    protected final SecureRandom secureRandom;

    public Curve(int size, BigInteger p, BigInteger n, BigInteger a, BigInteger b, BigInteger x, BigInteger y, BigInteger h)
    {
        this.bytes = size;
        this.bits = size*8;
        this.p = p;
        this.n = n;
        this.a = a;
        this.b = b;
        this.G = new Point(this,x,y);
        this.h = h;
        try
        {
            this.secureRandom = SecureRandom.getInstanceStrong();
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException(ex);
        }
    }
    Curve(int size, String p, String n, String a, String b, String x, String y, String h)
    {
        this(size,
        biHex(p),                
        biHex(n),
        biHex(a),
        biHex(b),
        biHex(x),
        biHex(y),
        biHex(h));
    }

    public boolean isSquare(BigInteger x)
    {
        return x.modPow(this.p.subtract(BigInteger.ONE).mod(Nums.BIG_INT_TWO), this.p).longValue() == 1L;
    }
    
    public final Point liftX(byte[] x)
    {
        return liftX(Utils.asBigInteger(x), (byte)0);
    }
    public final Point liftX(byte[] x, byte parity)
    {
        return liftX(Utils.asBigInteger(x), parity);
    }
    public final Point liftX(BigInteger x)
    {
        return liftX(x, (byte)0);
    }
    
    public abstract Point liftX(BigInteger x, byte parity);   
    public abstract byte[] genSecKey();
    public abstract Point add(Point p1, Point p2);
    public abstract Point mul(Point P, BigInteger n);
        
    public final byte[] asBytes(BigInteger n)
    {
        byte[] b = n.toByteArray();
        
        if(b.length == this.bytes) 
        {
            return b;
        }
        if(b.length < this.bytes) 
        {
            int index = this.bytes - b.length;
            ByteBufferCompat buffer = new ByteBufferCompat(ByteBuffer.allocate(this.bytes));
            return buffer.put(index, b).array();
        }
        if(b.length == this.bytes+1 && b[0]==0) 
        {
            return Arrays.copyOfRange(b, 1, b.length);
        }
        
        int prefix = b.length - this.bytes;
        for(int i=0;i<prefix;i++)
        {
            if(b[i]!=0)
            {
                throw new BufferOverflowException();
            }
        }
        return Arrays.copyOfRange(b, prefix, b.length);
    }
    static final byte UNCOMPRESSED = 4;
    static final byte EVEN = 0x02;
    static final byte ODD = 0x03;
    
    public final byte[] rawPubKey(BigInteger x, BigInteger y)
    {
        Objects.requireNonNull(x, "x is null");
        Objects.requireNonNull(y, "y is null");
        
        byte[] xbytes = asBytes(x);
        byte[] ybytes = asBytes(y);
        ByteBuffer buffer = ByteBuffer.allocate(xbytes.length + ybytes.length);
        buffer.put(xbytes);
        buffer.put(ybytes);
        return buffer.array();
    }
    
    public final byte[] compressedPubKey(Point point)
    {
        Objects.requireNonNull(point, "point is null");
        
        return compressedPubKey(point.x, point.y); 
    }
    public final byte[] compressedPubKey(BigInteger x, BigInteger y)
    {
        Objects.requireNonNull(x, "x is null");
        Objects.requireNonNull(y, "y is null");

        byte[] xbytes = asBytes(x);
        ByteBuffer buffer = ByteBuffer.allocate(xbytes.length + 1);
        buffer.put(Nums.isEven(y) ? EVEN : ODD);
        buffer.put(xbytes);
        return buffer.array();
    }
    
    public final byte[] uncompressedPubKey(BigInteger x, BigInteger y)
    {
        Objects.requireNonNull(x, "x is null");
        Objects.requireNonNull(y, "y is null");

        byte[] xbytes = asBytes(x);
        byte[] ybytes = asBytes(y);

        ByteBuffer buffer = ByteBuffer.allocate(xbytes.length + ybytes.length + 1);
        
        buffer.put(UNCOMPRESSED);
        buffer.put(xbytes);
        buffer.put(ybytes);
        return buffer.array();
    }
    public final Point pointPubKey(byte[] pubKey) throws InvalidKeyException
    {
        Objects.requireNonNull(pubKey, "pubKey is null");

        ByteBufferCompat buffer = new ByteBufferCompat(ByteBuffer.wrap(pubKey));

        byte fmt = buffer.get();
        if(fmt==UNCOMPRESSED && pubKey.length==this.bytes*2+1)
        {
            byte[] xbytes = new byte[pubKey.length/2];
            byte[] ybytes = new byte[pubKey.length/2];
            buffer.get(xbytes);
            buffer.get(ybytes);
            return new Point(this, xbytes, ybytes);
        }
        if((fmt==EVEN || fmt==ODD) && pubKey.length==this.bytes+1)
        {
            byte[] xbytes = new byte[this.bytes];
            buffer.get(xbytes);
            return this.liftX(xbytes, fmt);
        }
        if(pubKey.length==this.bytes*2)
        {
            byte[] xbytes = new byte[pubKey.length/2];
            byte[] ybytes = new byte[pubKey.length/2];
            buffer.get(0, xbytes);
            buffer.get(xbytes.length,ybytes);
            return new Point(this, xbytes, ybytes);
        }
        if(pubKey.length==this.bytes*2)
        {
            byte[] xbytes = new byte[pubKey.length/2];
            byte[] ybytes = new byte[pubKey.length/2];
            buffer.get(0, xbytes);
            buffer.get(xbytes.length,ybytes);
            return new Point(this, xbytes, ybytes);
        }
        if(pubKey.length==this.bytes)
        {
            return this.liftX(pubKey);
        }
        throw new InvalidKeyException("unknown format");
    }
    
}
