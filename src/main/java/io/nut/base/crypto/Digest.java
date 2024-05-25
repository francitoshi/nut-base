/*
 *  Digest.java
 *
 *  Copyright (c) 2014-2023 francitoshi@gmail.com
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

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class Digest
{
    public static final String MD5  = "MD5";
    public static final String SHA1  = "SHA-1";
    public static final String SHA256  = "SHA-256";
    public static final String SHA512  = "SHA-512";
    public static final String RIPEMD160  = "RIPEMD160";
    public static final String BC  = "BC";

    private static class Holder
    {
        static final Digest md5 = new Digest(MD5);
        static final Digest sha1 = new Digest(SHA1);
        static final Digest sha256 = new Digest(SHA256);
        static final Digest sha512 = new Digest(SHA512);
        static final Digest ripemd160 = new Digest(RIPEMD160, BC);
    }
    
    final String algorithm;
    final String provider;

    public Digest(String algorithm)
    {
        this.algorithm = algorithm;
        this.provider = null;
    }
    public Digest(String algorithm, String provider)
    {
        this.algorithm = algorithm;
        this.provider = provider;
    }
    
    private MessageDigest get()
    {
        try
        {
            if(BC.equals(this.provider))
            {
                Crypto.registerBouncyCastle();
            } 
            return  this.provider==null ? MessageDigest.getInstance(this.algorithm) : MessageDigest.getInstance(this.algorithm, this.provider);
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public byte[] digest(byte[] bytes) 
    {
        return digest(bytes, 0, bytes.length);
    }

    public byte[] digest(byte[] bytes, int offset, int length) 
    {
        MessageDigest digest = get();
        digest.update(bytes, offset, length);
        return digest.digest();
    }
    public byte[] digest(byte[]... bytes) 
    {
        MessageDigest digest = get();
        for(byte[] item : bytes)
        {
            digest.update(item);
        }
        return digest.digest();
    }

    public byte[] digestTwice(byte[] bytes) 
    {
        return digestTwice(bytes, 0, bytes.length);
    }

    public byte[] digestTwice(byte[] bytes, int offset, int length) 
    {
        MessageDigest digest = get();
        digest.update(bytes, offset, length);
        digest.update(digest.digest());
        return digest.digest();
    }
    
    public byte[] digest(String s) 
    {
        return digest(s.getBytes());
    }
    public byte[] digest(String s, Charset charset) 
    {
        return digest(s.getBytes(charset));
    }
    
////////////////////////////////////////////////////////////////////////////////    
    
    public static byte[] md5(byte[] bytes) 
    {
        return Holder.md5.digest(bytes, 0, bytes.length);
    }

    public static byte[] md5(byte[] bytes, int offset, int length) 
    {
        return Holder.md5.digest(bytes, offset, length);
    }
    public static byte[] md5(byte[]... bytes)
    {
        return Holder.md5.digest(bytes);
    }

    public static byte[] md5Twice(byte[] bytes)
    {
        return md5Twice(bytes, 0, bytes.length);
    }

    public static byte[] md5Twice(byte[] bytes, int offset, int length) 
    {
        return Holder.md5.digestTwice(bytes, offset, length);
    }

    public static byte[] md5(String s) 
    {
        return md5(s.getBytes());
    }
    public static byte[] md5(String s, Charset charset) 
    {
        return md5(s.getBytes(charset));
    }
    
////////////////////////////////////////////////////////////////////////////////    
    
    public static byte[] sha1(byte[] bytes) 
    {
        return Holder.sha1.digest(bytes, 0, bytes.length);
    }

    public static byte[] sha1(byte[] bytes, int offset, int length) 
    {
        return Holder.sha1.digest(bytes, offset, length);
    }
    public static byte[] sha1(byte[]... bytes)
    {
        return Holder.sha1.digest(bytes);
    }

    public static byte[] sha1Twice(byte[] bytes)
    {
        return sha1Twice(bytes, 0, bytes.length);
    }

    public static byte[] sha1Twice(byte[] bytes, int offset, int length) 
    {
        return Holder.sha1.digestTwice(bytes, offset, length);
    }

    public static byte[] sha1(String s) 
    {
        return sha1(s.getBytes());
    }
    public static byte[] sha1(String s, Charset charset) 
    {
        return sha1(s.getBytes(charset));
    }

////////////////////////////////////////////////////////////////////////////////    
    
    public static byte[] sha256(byte[] bytes) 
    {
        return Holder.sha256.digest(bytes, 0, bytes.length);
    }

    public static byte[] sha256(byte[] bytes, int offset, int length) 
    {
        return Holder.sha256.digest(bytes, offset, length);
    }

    public static byte[] sha256(byte[]... bytes)
    {
        return Holder.sha256.digest(bytes);
    }
    
    public static byte[] sha256Twice(byte[] bytes)
    {
        return sha256Twice(bytes, 0, bytes.length);
    }

    public static byte[] sha256Twice(byte[] bytes, int offset, int length) 
    {
        return Holder.sha256.digestTwice(bytes, offset, length);
    }

    public static byte[] sha256(String s) 
    {
        return sha256(s.getBytes());
    }
    public static byte[] sha256(String s, Charset charset) 
    {
        return sha256(s.getBytes(charset));
    }

////////////////////////////////////////////////////////////////////////////////    
    
    public static byte[] sha512(byte[] bytes) 
    {
        return Holder.sha512.digest(bytes, 0, bytes.length);
    }

    public static byte[] sha512(byte[] bytes, int offset, int length) 
    {
        return Holder.sha512.digest(bytes, offset, length);
    }

    public static byte[] sha512(byte[]... bytes)
    {
        return Holder.sha512.digest(bytes);
    }

    public static byte[] sha512Twice(byte[] bytes)
    {
        return sha512Twice(bytes, 0, bytes.length);
    }

    public static byte[] sha512Twice(byte[] bytes, int offset, int length) 
    {
        return Holder.sha512.digestTwice(bytes, offset, length);
    }

    public static byte[] sha512(String s) 
    {
        return sha512(s.getBytes());
    }
    public static byte[] sha512(String s, Charset charset) 
    {
        return sha512(s.getBytes(charset));
    }

////////////////////////////////////////////////////////////////////////////////    
    
    public static byte[] ripemd160(byte[] bytes) 
    {
        return Holder.ripemd160.digest(bytes, 0, bytes.length);
    }

    public static byte[] ripemd160(byte[] bytes, int offset, int length) 
    {
        return Holder.ripemd160.digest(bytes, offset, length);
    }

    public static byte[] ripemd160(byte[]... bytes)
    {
        return Holder.ripemd160.digest(bytes);
    }

    public static byte[] ripemd160Twice(byte[] bytes)
    {
        return ripemd160Twice(bytes, 0, bytes.length);
    }

    public static byte[] ripemd160Twice(byte[] bytes, int offset, int length) 
    {
        return Holder.ripemd160.digestTwice(bytes, offset, length);
    }

    public static byte[] ripemd160(String s) 
    {
        return ripemd160(s.getBytes());
    }
    public static byte[] ripemd160(String s, Charset charset) 
    {
        return ripemd160(s.getBytes(charset));
    }

    
    
    /**
     * Calculates RIPEMD160(SHA256(input)).This is used in Address calculations.
     * @param input
     * @return 
     */
    public static byte[] sha256ripemd160(byte[] input) 
    {
        return ripemd160(sha256(input));
    }
    
}
