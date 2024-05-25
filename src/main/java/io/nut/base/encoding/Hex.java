/*
 *  Hex.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.encoding;

/**
 *
 * @author franci
 */
public class Hex
{
    //the fast and simple, borrowed from
    //http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java/9855338#9855338
    private static final String HEX = "0123456789ABCDEF";
    private static final char[] hexLower = HEX.toLowerCase().toCharArray();
    private static final char[] hexUpper = HEX.toUpperCase().toCharArray();

    public static String encode(byte[] bytes, boolean upperCase)
    {
        char[] hex = upperCase ? hexUpper : hexLower;
        char[] buf = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            int v = bytes[j] & 0xFF;
            buf[j * 2] = hex[v >>> 4];
            buf[j * 2 + 1] = hex[v & 0x0F];
        }
        return new String(buf);
    }

    public static String encode(byte[] bytes)
    {
        return encode(bytes, false);
    }

    //the fast and simple, borrowed from
    //https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java/140861#140861
    public static byte[] decode(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
    
}
