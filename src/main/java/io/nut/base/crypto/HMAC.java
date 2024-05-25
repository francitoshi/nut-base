/*
 *  HMAC.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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
package io.nut.base.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author franci
 */
public class HMAC
{
    public enum Hash{ HmacSHA224, HmacSHA256, HmacSHA384, HmacSHA512};

    public static byte[] hmacSHA224(byte[] key, byte[] data) 
    {
        return hmac(Hash.HmacSHA224, key, data);
    }
    
    public static byte[] hmacSHA256(byte[] key, byte[] data) 
    {
        return hmac(Hash.HmacSHA256, key, data);
    }
    
    public static byte[] hmacSHA384(byte[] key, byte[] data) 
    {
        return hmac(Hash.HmacSHA384, key, data);
    }
    
    public static byte[] hmacSHA512(byte[] key, byte[] data) 
    {
        return hmac(Hash.HmacSHA512, key, data);
    }
    
    public static byte[] hmacSHA224(SecretKey key, byte[] data) 
    {
        return hmac(Hash.HmacSHA224, key, data);
    }
    
    public static byte[] hmacSHA256(SecretKey key, byte[] data) 
    {
        return hmac(Hash.HmacSHA256, key, data);
    }
    
    public static byte[] hmacSHA384(SecretKey key, byte[] data) 
    {
        return hmac(Hash.HmacSHA384, key, data);
    }
    
    public static byte[] hmacSHA512(SecretKey key, byte[] data) 
    {
        return hmac(Hash.HmacSHA512, key, data);
    }
    
    public static byte[] hmac(Hash hash, byte[] key, byte[] data) 
    {
        return hmac(hash, new SecretKeySpec(key, hash.name()), data);
    }
    
    public static byte[] hmac(Hash hash, SecretKey key, byte[] data) 
    {
        Mac mac;
        try
        {
            mac = Mac.getInstance(hash.name());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalArgumentException("Unsupported MAC algorithm: " + hash, e);
        }
        try
        {
            mac.init(key);
        }
        catch (InvalidKeyException e)
        {
            throw new IllegalArgumentException("Invalid MAC key", e);
        }
        return mac.doFinal(data);
    }    
    
}
