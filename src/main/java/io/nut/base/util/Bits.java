/*
 * Bits.java
 *
 * Copyright (c) 2023 francitoshi@gmail.com
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
package io.nut.base.util;

/**
 *
 * @author franci
 */
public class Bits
{
    //borrowed from http://www.deegeu.com/4-ways-count-bits-in-a-byte/
    public static int bitCount(byte n)
    {
        byte i = n;
        //x = (x & mask[i]) + ((x >> shift) & mask[i]);     
        final byte mask1 = 0b01010101;
        final byte mask2 = 0b00110011;
        final byte mask3 = 0b00001111;

        i = (byte) ((byte) (i & mask1) + (byte) (((byte) (i >>> 1)) & mask1));
        i = (byte) ((byte) (i & mask2) + (byte) (((byte) (i >>> 2)) & mask2));
        i = (byte) ((byte) (i & mask3) + (byte) (((byte) (i >>> 4)) & mask3));
        return i;
    }

    public static int bitCount(short n)
    {
        short i = n;
        //x = (x & mask[i]) + ((x >> shift) & mask[i]);     
        final short mask1 = 0b0101010101010101;
        final short mask2 = 0b0011001100110011;
        final short mask3 = 0b0000111100001111;
        final short mask4 = 0b0000000011111111;

        i = (short) ((short) (i & mask1) + (short) (((short) (i >>> 1)) & mask1));
        i = (short) ((short) (i & mask2) + (short) (((short) (i >>> 2)) & mask2));
        i = (short) ((short) (i & mask3) + (short) (((short) (i >>> 4)) & mask3));
        i = (short) ((short) (i & mask4) + (short) (((short) (i >>> 8)) & mask4));
        return i;
    }

    public static int bitCount(char n)
    {
        char i = n;
        //x = (x & mask[i]) + ((x >> shift) & mask[i]);     
        final char mask1 = 0b0101010101010101;
        final char mask2 = 0b0011001100110011;
        final char mask3 = 0b0000111100001111;
        final char mask4 = 0b0000000011111111;

        i = (char) ((char) (i & mask1) + (char) (((char) (i >>> 1)) & mask1));
        i = (char) ((char) (i & mask2) + (char) (((char) (i >>> 2)) & mask2));
        i = (char) ((char) (i & mask3) + (char) (((char) (i >>> 4)) & mask3));
        i = (char) ((char) (i & mask4) + (char) (((char) (i >>> 8)) & mask4));
        return i;
    }
    
    public static byte bitSet(byte value, int index, boolean bitValue)
    {
        if (bitValue)
        {
            value |= 1 << index;
        }
        else
        {
            value &= ~(1 << index);
        }
        return value;
    }
    public static boolean bitGet(byte value, int index)
    {
        return ((value >> index) & 1) == 1;
    }    
    
}
