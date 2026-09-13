/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.encoding;

import io.nut.base.util.Exceptions;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Encoding
{
    private static final Logger LOG = Logger.getLogger(Encoding.class.getName());

    private static final String UTF8 = StandardCharsets.UTF_8.name();
    
    public static final Encoding BASE16 = new Encoding(Type.Base16);
    public static final Encoding BASE32 = new Encoding(Type.Base32);
    public static final Encoding BASE43 = new Encoding(Type.Base43);
    public static final Encoding BASE58 = new Encoding(Type.Base58);
    public static final Encoding BASE58CHECK = new Encoding(Type.Base58Check);
    public static final Encoding BASE64 = new Encoding(Type.Base64);
    public static final Encoding BASE91 = new Encoding(Type.Base91);
    
    public enum Type
    {         
        Base16(16), Base32(32), Base43(43), Base58(58), Base58Check(58), Base64(64), Base91(91);
        Type(int base)
        {
            this.base = base;
        }
        public final int base;
    }
    
    public final Type type;

    Encoding(Type type)
    {
        this.type = type;
    }
    
    public String encode(byte[] src)
    {
        return Encoding.encode(src, type);
    }
    
    public byte[] decode(String src)
    {
        return Encoding.decode(src, type);
    }
    
    public static String encode(byte[] src, Type type)
    {
        if (src == null)
        {
            return null;
        }
        try
        {
            switch (type)
            {
                case Base16:
                    return Hex.encode(src, false); //lowercase
                case Base32:
                    return Base32String.encode(src);
                case Base43:
                    return Base43.encode(src);
                case Base58:
                    return Base58.encode(src);
                case Base58Check:
                    return Base58Check.bytesToBase58(src);
                case Base91:
                    return Base91.encodeToString(src, UTF8);
                case Base64:
                default:
                    return Base64.encode(src);
            }
        }
        catch (UnsupportedEncodingException ex)
        {
            Exceptions.severe(LOG, ex);
            return null;
        }
    }

    public static byte[] decode(String src, Type type)
    {
        if (src == null)
        {
            return null;
        }
        try
        {
            switch (type)
            {
                case Base16:
                    return Hex.decode(src);
                case Base32:
                    return Base32String.decode(src);
                case Base43:
                    return Base43.decode(src);
                case Base58:
                    return Base58.decode(src);
                case Base58Check:
                    return Base58Check.base58ToBytes(src);
                case Base91:
                    return Base91.decodeFromString(src, UTF8);
                case Base64:
                default:
                    return Base64.decode(src);
            }
        }
        catch (IllegalArgumentException | UnsupportedEncodingException | Base64DecoderException | Base58.FormatException | Base32String.DecodingException ex)
        {
            Exceptions.severe(LOG, ex);
            return null;
        }
    }
    
}
