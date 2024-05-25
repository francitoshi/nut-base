/*
 *  Encoding.java
 *
 *  Copyright (c) 2023 francitoshi@gmail.com
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
 *
 */
package io.nut.base.encoding;

import io.nut.base.encoding.Base32String;
import io.nut.base.encoding.Base43;
import io.nut.base.encoding.Base64;
import io.nut.base.encoding.Base64DecoderException;
import io.nut.base.encoding.Base91;
import io.nut.base.encoding.Hex;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Encoding
{
    private static final String UTF8 = StandardCharsets.UTF_8.name();
    
    public static final Encoding BASE16 = new Encoding(Type.Base16);
    public static final Encoding BASE32 = new Encoding(Type.Base32);
    public static final Encoding BASE43 = new Encoding(Type.Base43);
    public static final Encoding BASE58 = new Encoding(Type.Base58);
    public static final Encoding BASE64 = new Encoding(Type.Base64);
    public static final Encoding BASE91 = new Encoding(Type.Base91);
    
    public enum Type
    {         
        Base16(16), Base32(32), Base43(43), Base58(58), Base64(64), Base91(91);
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
                case Base91:
                    return Base91.encodeToString(src, UTF8);
                case Base64:
                default:
                    return Base64.encode(src);
            }
        }
        catch (UnsupportedEncodingException ex)
        {
            Logger.getLogger(Encoding.class.getName()).log(Level.SEVERE, null, ex);
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
                case Base91:
                    return Base91.decodeFromString(src, UTF8);
                case Base64:
                default:
                    return Base64.decode(src);
            }
        }
        catch (UnsupportedEncodingException | Base64DecoderException | Base58.FormatException | Base32String.DecodingException ex)
        {
            Logger.getLogger(Encoding.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
}
