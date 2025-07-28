/*
 *  Chars.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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

import java.util.Arrays;

/**
 * A utility class providing static methods for manipulations on character
 * arrays ({@code char[]}).
 * <p>
 * This class is analogous to utility classes like {@link java.lang.String} or
 * {@link java.util.Arrays}, but specifically tailored for primitive character
 * arrays. It is not intended to be instantiated.
 *
 */
public final class Chars
{
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Chars()
    {
        // This class should not be instantiated.
    }

    /**
     * Checks if a character array contains a specific sequence of characters.
     *
     * @param s The character array to search in.
     * @param seq The sequence of characters to search for.
     * @return {@code true} if the character array {@code s} contains the
     * sequence {@code seq}, {@code false} otherwise. Returns {@code true} if
     * {@code seq} is an empty array.
     * @throws NullPointerException if either {@code s} or {@code seq} is null.
     */
    public static boolean contains(char[] s, char[] seq)
    {
        if(s.length==0 && seq.length==0)
        {
            return true;
        }
        for (int i = 0; i <= s.length - seq.length; i++)
        {
            int count = 0;
            for (int j = 0; (i+j)<s.length &&  j< seq.length; j++, count++)
            {
                if (s[i + j] != seq[j])
                {
                    break;
                }
            }
            if (count == seq.length)
            {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(char[] s, String seq)
    {
        return contains(s, seq.toCharArray());
    }
    
    /**
     * Converts all of the characters in a given character array to lower case.
     * <p>
     * This method creates a new character array; the original array is not
     * modified. The conversion is done using
     * {@link Character#toLowerCase(char)}.
     *
     * @param ch The character array to be converted.
     * @return A new character array representing the original array converted
     * to lower case. If the input array is {@code null} or empty, it returns
     * the input array itself.
     */
    public static char[] toLowerCase(char[] ch)
    {
        if(ch==null || ch.length==0)
        {
            return ch;
        }
        char[] low = new char[ch.length];
        for(int i=0;i<ch.length;i++)
        {
            low[i] = Character.toLowerCase(ch[i]);
        }
        return low;
    }
    
    /**
     * Converts all of the characters in a given character array to upper case.
     * <p>
     * This method creates a new character array; the original array is not
     * modified. The conversion is done using
     * {@link Character#toUpperCase(char)}.
     *
     * @param ch The character array to be converted.
     * @return A new character array representing the original array converted
     * to upper case. If the input array is {@code null} or empty, it returns
     * the input array itself.
     */
    public static char[] toUpperCase(char[] ch)
    {
        if(ch==null || ch.length==0)
        {
            return ch;
        }
        char[] up = new char[ch.length];
        for(int i=0;i<ch.length;i++)
        {
            up[i] = Character.toUpperCase(ch[i]);
        }
        return up;
    }
    
    /**
     * Returns a character array whose value is this character array, with any
     * leading and trailing whitespace removed.
     * <p>
     * This method defines whitespace as any character whose codepoint is less
     * than or equal to ' ' (U+0020). If the array has no leading or trailing
     * whitespace, a reference to the original array is returned. Otherwise, a
     * new character array is created representing the trimmed subsequence.
     *
     * @param ch The character array to be trimmed.
     * @return A new character array representing the original with leading and
     * trailing whitespace removed, or the original array if no trimming was
     * necessary. Returns the input array itself if it's {@code null} or empty.
     */
    public static char[] trim(char[] ch)
    {
        if(ch==null || ch.length==0)
        {
            return ch;
        }

        int start = 0;
        while (start < ch.length && ch[start] <= '\u0020')
        {
            start++;
        }

        int end = ch.length;
        while (end > start && ch[end - 1] <= '\u0020')
        {
            end--;
        }

        // If no trimming was needed, return the original array. Otherwise, return a copy of the relevant range.
        return (start>0 || end<ch.length) ? Arrays.copyOfRange(ch, start, end) : ch;
    }
}
