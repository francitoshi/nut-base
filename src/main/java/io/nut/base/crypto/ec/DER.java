/*
 *  DER.java
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

import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;

/**
 *
 * @author franci
 */
public class DER
{
    public static final byte DER_COMPOUND_OBJECT = 0x30;
    public static final byte DER_INTEGER = 0x02;
    public static final byte DER_SIGHASH = 0x01;
    
    //https://b10c.me/blog/006-evolution-of-the-bitcoin-signature-length/
    //https://bitcoin.stackexchange.com/questions/77191/what-is-the-maximum-size-of-a-der-encoded-ecdsa-signature
    
    public static byte[] encode(BigInteger[] rs)
    {
        if(rs==null)
        {
            return null;
        }
        return encode(rs[0].toByteArray(), rs[1].toByteArray());
    }
    public static byte[] encode(BigInteger r, BigInteger s)
    {
        if(r==null || s==null)
        {
            throw new NullPointerException();
        }
        return encode(r.toByteArray(), s.toByteArray());
    }
    public static byte[] encode(byte[] r, byte[] s)
    {
        byte len = (byte) (r.length + s.length + 4);
        
        ByteBuffer buffer = ByteBuffer.allocate(len+3);
        
        buffer.put(DER_COMPOUND_OBJECT);
        buffer.put(len);
        
        buffer.put(DER_INTEGER);
        buffer.put((byte) r.length);
        buffer.put(r);
        
        buffer.put(DER_INTEGER);
        buffer.put((byte) s.length);
        buffer.put(s);
        
        buffer.put(DER_SIGHASH);
        
        return buffer.array();
    }
    public static BigInteger[] decode(byte[] der) throws InvalidParameterException 
    {
        if(der==null)
        {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(der);
        if(DER_COMPOUND_OBJECT!=buffer.get())
        {
            throw new InvalidParameterException("Compound Object tag not found");
        }
        
        byte len = buffer.get();
        if(DER_INTEGER!=buffer.get())
        {
            throw new InvalidParameterException("Integer tag for r field not found");
        }

        byte rlen = buffer.get();
        if(rlen>len)
        {
            throw new InvalidParameterException("r.len > len");
        }
        byte[] rbytes = new byte[rlen];
        buffer.get(rbytes);
        
        if(DER_INTEGER!=buffer.get())
        {
            throw new InvalidParameterException("Integer tag for s field not found");
        }
        byte slen = buffer.get();
        if(slen>len)
        {
            throw new InvalidParameterException("s.len > len");
        }
        byte[] sbytes = new byte[slen];
        buffer.get(sbytes);

        if(DER_SIGHASH!=buffer.get())
        {
            throw new InvalidParameterException("SIGHASH tag not found");
        }

        BigInteger r = Utils.asBigInteger(rbytes);
        BigInteger s = Utils.asBigInteger(sbytes);
        
        return new BigInteger[]{r,s};
    }
    
}
