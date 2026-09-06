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

import java.util.Arrays;

public class Hex
{
    private static final String HEX = "0123456789ABCDEF";
    private static final char[] hexLower = HEX.toLowerCase().toCharArray();
    private static final char[] hexUpper = HEX.toUpperCase().toCharArray();

    // lookup table: 
    //      index = ASCII code of the hex character
    //      value = its digit (0-15)
    //      -1 if invalid
    private static final int[] DECODE = new int[256];
    static
    {
        Arrays.fill(DECODE, -1);
        for (int i = 0; i < 10; i++)
        {
            DECODE['0' + i] = i;
        }
        for (int i = 0; i < 6; i++)
        {
            DECODE['a' + i] = DECODE['A' + i] = 10 + i;
        }
    }

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

    public static byte[] decode(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            char s_i = s.charAt(i);
            char s_i_1 = s.charAt(i + 1);
            int hi = s_i   < 256 ? DECODE[s_i]   : -1;
            int lo = s_i_1 < 256 ? DECODE[s_i_1] : -1;
            if (hi < 0 || lo < 0)
            {
                throw new IllegalArgumentException("Invalid hex character at index " + i);
            }
            data[i / 2] = (byte) ((hi << 4) | lo);
        }
        return data;
    }

    /**
     * Returns whether the given string is a valid hexadecimal string.
     * <p>
     * A string is valid if it is non-null, has an even length and only
     * contains hexadecimal digits (0-9, a-f, A-F). An empty string is
     * considered valid.
     *
     * @param s the string to check.
     * @return true if the string is a valid hexadecimal string, false otherwise.
     */
    public static boolean isValid(String s)
    {
        if (s == null || (s.length() & 1) != 0)
        {
            return false;
        }
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            int v = c < 256 ? DECODE[c] : -1;
            if (v < 0)
            {
                return false;
            }
        }
        return true;
    }
}
