/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.ec;

import io.nut.base.util.As;
import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Objects;

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
        Objects.requireNonNull(r, "r must not be null");
        Objects.requireNonNull(s, "s must not be null");
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

        BigInteger r = As.bigInteger(rbytes);
        BigInteger s = As.bigInteger(sbytes);
        
        return new BigInteger[]{r,s};
    }
    
}
