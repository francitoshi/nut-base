/*
 *  Strings.java
 *
 *  Copyright (c) 2012-2025 francitoshi@gmail.com
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author franci
 */
public class Strings
{
    public static final String EMPTY = "";
            
    public static final String HORIZONTAL_ELLIPSIS = "…"; //U+2026
    public static final String VERTICAL_ELLIPSIS   = "⋮";  //U+22EE

    public static <T> String join(CharSequence delimiter, CharSequence prefix, CharSequence suffiex, Iterable<T> list, CharSequence nullValue)
    {
        StringJoiner sj = new StringJoiner(delimiter, prefix, suffiex);
        for(T item : list)
        {
            sj.add(item!=null ? item.toString() : nullValue);
        }
        return sj.toString();
    }
    public static <T> String join(CharSequence delimiter, Iterable<T> list)
    {
        return join(delimiter, "", "", list, null);
    }
    public static <T> String join(CharSequence delimiter, T[] list, String nullValue)
    {
        return join(delimiter, "", "", Arrays.asList(list), nullValue);
    }
    public static <T> String join(CharSequence delimiter, T[] elements)
    {
        return join(delimiter, "", "", Arrays.asList(elements), null);
    }
    
    public static String repeat(char c,int count)
    {
        char[] tmp = new char[count];
        Arrays.fill(tmp,c);
        return new String(tmp,0,count);
    }
    public static String repeat(String s,int count)
    {
        StringBuilder builder = new StringBuilder(s.length()*count);
        for(int i=0;i<count;i++)
        {
            builder.append(s);
        }
        return builder.toString();
    }
    
    /**
     * <p>Reverses a String as per {@link StringBuilder#reverse()}.</p>
     *
     * <p>A <code>null</code> String returns <code>null</code>.</p>
     *
     * <pre>
     * Strings.reverse(null)  = null
     * Strings.reverse("")    = ""
     * Strings.reverse("bat") = "tab"
     * </pre>
     *
     * @param s  the String to reverse, may be null
     * @return the reversed String, <code>null</code> if null String input
     */
    public static String reverse(String s)
    {
        return (s!=null && s.length()!=0) ? new StringBuilder(s).reverse().toString() : s;
    }
    
    public static StringBuilder fill(StringBuilder builder,char c, int size, boolean insert)
    {
        int count = Math.max(size-builder.length(), 0);
        String cc = repeat(c,count);
        return insert?builder.insert(0, cc):builder.append(cc);
    }
    public static String fill(CharSequence cs,char c, int size, boolean insert)
    {
        return fill(new StringBuilder(cs),c,size,insert).toString();
    }
    public static StringBuilder fill(StringBuilder builder,char c, int size)
    {
        return fill(builder,c,size,false);
    }
    public static String fill(CharSequence cs,char c, int size)
    {
        return fill(new StringBuilder(cs),c,size,false).toString();
    }

    public static String left(String s, int count)
    {
        if(s.length()<=count)
        {
            return s;
        }
        return s.substring(0, count);
    }
    public static String right(String s, int count)
    {
        int index = Math.max(s.length()-count,0);
        return s.substring(index);
    }    

    /**
     * <p>Checks if a CharSequence is empty ("") or null.</p>
     *
     * <pre>
     * Strings.isEmpty(null)      = true
     * Strings.isEmpty("")        = true
     * Strings.isEmpty(" ")       = false
     * Strings.isEmpty("no")     = false
     * Strings.isEmpty("  no  ") = false
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    public static boolean isEmpty(CharSequence cs) 
    {
        return (cs==null || cs.length()==0);
    }

    /**
     * <p>Checks if a CharSequence is not empty ("") and not null.</p>
     *
     * <pre>
     * Strings.isNotEmpty(null)      = false
     * Strings.isNotEmpty("")        = false
     * Strings.isNotEmpty(" ")       = true
     * Strings.isNotEmpty("yes")     = true
     * Strings.isNotEmpty("  yes  ") = true
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(CharSequence cs) 
    {
        return !isEmpty(cs);
    }
    
    /**
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * Strings.isBlank(null)      = true
     * Strings.isBlank("")        = true
     * Strings.isBlank(" ")       = true
     * Strings.isBlank("no")     = false
     * Strings.isBlank("  no  ") = false
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank(CharSequence cs)
    {
        int len;
        if (cs==null || (len=cs.length())==0)
        {
            return true;
        }
        for (int i = 0; i < len; i++)
        {
            if (Character.isWhitespace(cs.charAt(i)) == false)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     *
     * <pre>
     * Strings.isNotBlank(null)      = false
     * Strings.isNotBlank("")        = false
     * Strings.isNotBlank(" ")       = false
     * Strings.isNotBlank("yes")     = true
     * Strings.isNotBlank("  yes  ") = true
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is not empty and not null and not whitespace
     */
    public static boolean isNotBlank(CharSequence cs) 
    {
        return !isBlank(cs);
    }

    /**
     * Checks whether the given array is {@code null}, is empty or contains {@code null} 
     * elements.
     *
     * @param s the array to check, may be {@code null}
     * @param <E> the type of elements in the array
     * @return {@code true} if the array is {@code null}, is empty or any element is 
     * {@code null}; {@code false} otherwise
     */
    public static <E> boolean hasNullsOrBlank(String... s)
    {
        if (s == null || s.length==0)
        {
            return true;
        }
        for (String item : s)
        {
            if (item == null || isBlank(item))
            {
                return true;
            }
        }
        return false;
    }
    
    static final String TRIM = "(^[\\s\u00a0\n]+)|([\\s\u00a0\n]+$)";
    public static String trimWhitespaces(String str)
    {
        return str==null ? null : str.trim().replaceAll(TRIM, "");
    }
            
    public static String firstNonNull(String first, String second, String... others)
    {
        if(first!=null)
        {
            return first;
        }
        if(second!=null)
        {
            return second;
        }
        for(String item : others)
        {
            if(item!=null)
            {
                return item;
            }
        }
        return null;
    }
    public static <T> String firstNonNull(T first, T second, T... others)
    {
        if(first!=null)
        {
            return first.toString();
        }
        if(second!=null)
        {
            return second.toString();
        }
        for(T item : others)
        {
            if(item!=null)
            {
                return item.toString();
            }
        }
        return null;
    }
    public static String nullForEmpty(String value)
    {
        return (value==null || value.length()==0) ? null : value;
    }
    //convierte a string si es posible, evitando NullPointerException si es nulo
    public static String safeToString(Object value)
    {
        return (value!=null) ? value.toString() : null;
    }
    public static String safeToString(Object value, String safeValue)
    {
        return (value!=null) ? value.toString() : safeValue;
    }
    public static int ocurrences(String s, String pattern)
    {
        return ocurrences(s, pattern, false, false);
    }
    public static int ocurrences(String s, String pattern, boolean ignoreCase)
    {
        return ocurrences(s, pattern, ignoreCase, false);
    }
    public static int ocurrences(String s, String pattern, boolean ignoreCase, boolean overlap)
    {
        if(ignoreCase)
        {
            s = s.toLowerCase();
            pattern = pattern.toLowerCase();
        }
        
        int count=0;
        final int sn = s.length();
        final int pn = pattern.length();
        if(sn>0 && pn>0)
        {
            final int num=sn-pn;
            for(int i=0;i<=num;)
            {
                if(s.startsWith(pattern, i))
                {
                    count++;
                    i+= overlap?1:pn;
                    continue;
                }
                i++;
            }
        }
        return count;
    }
    
    /**
     * <p>Check if a String starts with a specified prefix.</p>
     *
     * <p><code>null</code>s are handled without exceptions. Two <code>null</code>
     * references are considered to be equal. The comparison is case sensitive.</p>
     *
     * <pre>
     * Strings.startsWith(null, null)      = true
     * Strings.startsWith(null, "abcdef")  = false
     * Strings.startsWith("abc", null)     = false
     * Strings.startsWith("abc", "abcdef") = true
     * Strings.startsWith("abc", "ABCDEF") = false
     * </pre>
     *
     * @see java.lang.String#startsWith(String)
     * @param s  the String to check, may be null
     * @param prefix the prefix to find, may be null
     * @return <code>true</code> if the String starts with the prefix, case sensitive, or
     *  both <code>null</code>
     */
    public static boolean startsWith(String s, String prefix)
    {
        if(s==null || prefix==null) 
        {
            return (s==null && prefix==null);
        }
        if (prefix.length() > s.length()) 
        {
            return false;
        }
        return s.startsWith(prefix);
    }
    
    /**
     * <p>Check if a String starts with any specified prefixes.</p>
     *
     * <p><code>null</code>s are handled without exceptions. Two <code>null</code>
     * references are considered to be equal. The comparison is case sensitive.</p>
     *
     * <pre>
     * Strings.startsWithAny(null, null, "abcdef")      = true
     * Strings.startsWithAny(null, "abcdef","xxxyyy")  = false
     * Strings.startsWithAny("abc", null, "xxxyyy")     = false
     * Strings.startsWithAny("abc", "abcdef", "ABCDEF") = false
     * </pre>
     *
     * @see java.lang.String#startsWith(String)
     * @param s  the String to check, may be null
     * @param prefix the prefix to find, may be null
     * @return <code>true</code> if the String starts with the prefix, case sensitive, or
     *  both <code>null</code>
     */
    public static boolean startsWithAny(String s, String... prefix)
    {
        for(String item : prefix)
        {
            if(startsWith(s, item))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Check if a String ends with a specified suffix.</p>
     *
     * <p><code>null</code>s are handled without exceptions. Two <code>null</code>
     * references are considered to be equal. The comparison is case sensitive.</p>
     *
     * <pre>
     * StringUtils.endsWith(null, null)      = true
     * StringUtils.endsWith(null, "abcdef")  = false
     * StringUtils.endsWith("def", null)     = false
     * StringUtils.endsWith("def", "abcdef") = true
     * StringUtils.endsWith("def", "ABCDEF") = false
     * </pre>
     *
     * @see java.lang.String#endsWith(String)
     * @param s  the String to check, may be null
     * @param suffix the suffix to find, may be null
     * @return <code>true</code> if the String ends with the suffix, case sensitive, or
     *  both <code>null</code>
     */
    public static boolean endsWith(String s, String suffix)
    {
        if(s==null || suffix==null) 
        {
            return (s==null && suffix==null);
        }
        if (suffix.length() > s.length()) 
        {
            return false;
        }
        return s.endsWith(suffix);
    }
    
    /**
     * <p>Check if a String ends with any specified suffix.</p>
     *
     * <p><code>null</code>s are handled without exceptions. Two <code>null</code>
     * references are considered to be equal. The comparison is case sensitive.</p>
     *
     * <pre>
     * Strings.endsWithAny(null, null, "abcdef")      = true
     * Strings.endsWithAny(null, "abcdef","xxxyyy")  = false
     * Strings.endsWithAny("def", null, "xxxyyy")     = false
     * Strings.endsWithAny("def", "abcdef", "ABCDEF") = false
     * </pre>
     *
     * @see java.lang.String#endsWith(String)
     * @param s  the String to check, may be null
     * @param suffix the suffix to find, may be null
     * @return <code>true</code> if the String ends with the suffix, case sensitive, or
     *  both <code>null</code>
     */
    public static boolean endsWithAny(String s, String... suffix)
    {
        for(String item : suffix)
        {
            if(endsWith(s, item))
            {
                return true;
            }
        }
        return false;
    }

    public static String toLowerCase(String s)
    {
        return (s!=null) ? s.toLowerCase() : null;
    }
    public static String toLowerCase(String s, Locale locale)
    {
        return (s!=null) ? s.toLowerCase(locale) : null;
    }
    public static String toUpperCase(String s)
    {
        return (s!=null) ? s.toUpperCase() : null;
    }
    public static String toUpperCase(String s, Locale locale)
    {
        return (s!=null) ? s.toUpperCase(locale) : null;
    }

    public static String replace(String s, char oldChar, char newChar)
    {
        return s!=null ? s.replace(oldChar, newChar) : null;
    }

    public static String replaceFirst(String s, String regex, String replacement)
    {
        return s!=null ? ( (regex!=null && replacement!=null) ? s.replaceFirst(regex, replacement) : s ) : null;
    }

    public static String replaceAll(String s, String regex, String replacement)
    {
        return s!=null ? ( (regex!=null && replacement!=null) ? s.replaceAll(regex, replacement) : s ) : null;
    }

    public static String replace(String s, CharSequence target, CharSequence replacement)
    {
        return s!=null ? ( (target!=null && replacement!=null) ? s.replace(target, replacement) : s ) : null;
    }
    
    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String, handling <code>null</code> by returning
     * <code>null</code>.</p>
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #strip(String)}.</p>
     *
     * <p>To trim your choice of characters, use the
     * {@link #strip(String, String)} methods.</p>
     *
     * <pre>
     * Strings.trim(null)          = null
     * Strings.trim("")            = ""
     * Strings.trim("     ")       = ""
     * Strings.trim("abc")         = "abc"
     * Strings.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param s  the String to be trimmed, may be null
     * @return the trimmed string, <code>null</code> if null String input
     */
    public static String trim(String s)
    {
        return (s!=null && s.length()!=0) ? s.trim() : s;
    }
    
    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this CharSequence, handling <code>null</code> by returning
     * <code>null</code>.</p>
     *
     * <p>The CharSequence is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #strip(String)}.</p>
     *
     * <p>To trim your choice of characters, use the
     * {@link #strip(String, String)} methods.</p>
     *
     * <pre>
     * Strings.trim(null)          = null
     * Strings.trim("")            = ""
     * Strings.trim("     ")       = ""
     * Strings.trim("abc")         = "abc"
     * Strings.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param cs  the CharSequence to be trimmed, may be null
     * @return the trimmed string, <code>null</code> if null String input
     */
    public static String trim(CharSequence cs)
    {
        return cs!=null ? cs.toString().trim() : null;
    }

    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning <code>null</code> if the String is
     * empty ("") after the trim or if it is <code>null</code>.
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #stripToNull(String)}.</p>
     *
     * <pre>
     * Strings.trimToNull(null)          = null
     * Strings.trimToNull("")            = null
     * Strings.trimToNull("     ")       = null
     * Strings.trimToNull("abc")         = "abc"
     * Strings.trimToNull("    abc    ") = "abc"
     * </pre>
     *
     * @param str  the String to be trimmed, may be null
     * @return the trimmed String,
     *  <code>null</code> if only chars &lt;= 32, empty or null String input
     */
    public static String trimToNull(String str) 
    {
        String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning an empty String ("") if the String
     * is empty ("") after the trim or if it is <code>null</code>.
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #stripToEmpty(String)}.</p>
     *
     * <pre>
     * Strings.trimToEmpty(null)          = ""
     * Strings.trimToEmpty("")            = ""
     * Strings.trimToEmpty("     ")       = ""
     * Strings.trimToEmpty("abc")         = "abc"
     * Strings.trimToEmpty("    abc    ") = "abc"
     * </pre>
     *
     * @param s  the String to be trimmed, may be null
     * @return the trimmed String, or an empty String if <code>null</code> input
     */
    public static String trimToEmpty(String s)
    {
        return s==null ? EMPTY : s.trim();
    }
    
    public static String firstNonEmpty(String first, String... others)
    {
        if(first!=null && first.length()>0)
        {
            return first;
        }
        for(String item : others)
        {
            if(item!=null && item.length()>0)
            {
                return item;
            }
        }
        return "";
    }
    
    public static String firstNonBlank(String first, String... others)
    {
        if(first!=null && !isBlank(first))
        {
            return first;
        }
        for(String item : others)
        {
            if(item!=null && !isBlank(item))
            {
                return item;
            }
        }
        return "";
    }
    
    /**
     * Creates a copy of the items passed as parameter removing the null and empty values, and keeping the order.
     * @param items
     * @return an array with the non-null items
     */
    public static String[] nonNullNonEmpty(String... items)
    {
        if(items!=null && items.length>0)
        {
            items = items.clone();
            int count = 0;
            for(int i = 0; i < items.length; i++)
            {
                if(items[i] != null && !items[i].isEmpty())
                {
                    if(count != i)
                    {
                        items[count++] = items[i];
                    }
                    else
                    {
                        count++;
                    }
                }
            }
            items = count<items.length ? Arrays.copyOf(items, count) : items;
        }
        return items;
    }
    
    public static String dumpHex(String s) throws IOException
    {
        return dumpHex(new StringBuilder(), s.getBytes()).toString();
    }
    public static String dumpHex(byte[] bytes) throws IOException
    {
        return dumpHex(new StringBuilder(), bytes).toString();
    }
    public static Appendable dumpHex(Appendable output, String s) throws IOException
    {
        return dumpHex(output, s.getBytes());
    }
    public static Appendable dumpHex(Appendable output, byte[] bytes) throws IOException
    {
        int offset = 0;
        while (offset < bytes.length)
        {
            output.append(String.format("%08X:", offset)); // Print the current offset in hexadecimal
            int i;
            for (i = 0; i < 16 && offset + i < bytes.length; i++)
            {
                output.append(String.format(" %02X", bytes[offset + i])); // Print the bytes in hexadecimal
            }
            for (; i < 16; i++)
            {
                output.append("   "); // Indentation for incomplete lines
            }
            output.append("  ");
            for (i = 0; i < 16 && offset + i < bytes.length; i++)
            {
                char c = (char) bytes[offset + i]; // Convert the byte to a character
                if (c >= 32 && c <= 126)
                {
                    output.append(c); // Print printable characters
                }
                else
                {
                    output.append("."); // Print a dot for non-printable characters
                }
            }
            output.append(System.lineSeparator());
            offset += 16; // Move to the next line
        }
        return output;
    }
    
    //nos da un resumen y añade una cadena que indica continuidad si se han descartado caracteres
    public static String brief(String s, int max, String more)
    {
        return brief(s, max, more, Integer.MAX_VALUE);
    }
    public static String brief(String s, int max, String more, int allowedReturns)
    {
        if(s.length()<=max && s.length()<=allowedReturns)
        {
            return s;
        }
        int n = Math.max(0, max-more.length());
        if(n>allowedReturns && s.length()>allowedReturns)
        {
            int r = 0;
            for(int i=0, index=-1; i<=allowedReturns && r>=0 && r<n ; i++)
            {
                index = s.indexOf('\n', index+1);
                r = index;
            }
            if(r>=0 && r<n)
            {
                n = r;
            }
        }
        return left(s,n)+more;
    }
    public static String paddingLeft(String s, int size, char c)
    {
        int n = Math.max(0, size-s.length());
        if(n>0)
        {
            s = fill(new StringBuilder(),c,n).toString()+s;
        }
        return s;
    }
    public static String paddingRight(String s, int size, char c)
    {
        int n = Math.max(0, size-s.length());
        if(n>0)
        {
            s = s+fill(new StringBuilder(),c,n).toString();
        }
        return s;
    }
    
    /**
     * <p>
     * Replaces all occurrences of Strings within another String.
     * </p>
     *
     * <p>
     * A {@code null} reference passed to this method is a no-op, or if
     * any "search string" or "string to replace" is null, that replace will be
     * ignored.
     * </p>
     *
     * <pre>
     *  Strings.replaceEach(null, *, *, *) = null
     *  Strings.replaceEach("", *, *, *) = ""
     *  Strings.replaceEach("aba", null, null, *) = "aba"
     *  Strings.replaceEach("aba", new String[0], null, *) = "aba"
     *  Strings.replaceEach("aba", null, new String[0], *) = "aba"
     *  Strings.replaceEach("aba", new String[]{"a"}, null, *) = "aba"
     *  Strings.replaceEach("aba", new String[]{"a"}, new String[]{""}, *) = "b"
     *  Strings.replaceEach("aba", new String[]{null}, new String[]{"a"}, *) = "aba"
     *  Strings.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"w", "t"}, *) = "wcte"
     *  (example of how it repeats)
     *  Strings.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "t"}, false) = "dcte"
     *  Strings.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "t"}, true) = "tcte"
     *  Strings.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "ab"}, *) = IllegalStateException
     * </pre>
     *
     * @param s
     *            text to search and replace in, no-op if null
     * @param src
     *            the Strings to search for, no-op if null
     * @param dst
     *            the Strings to replace them with, no-op if null
     * @param repeat if true, then replace repeatedly
     *       until there are no more possible replacements or timeToLive &lt; 0
     * @param levels
     *            if less than 0 then there is a circular reference and endless
     *            loop
     * @return the text with any replacements processed, {@code null} if
     *         null String input
     * @throws IllegalStateException
     *             if the search is repeating and there is an endless loop due
     *             to outputs of one being inputs to another
     * @throws IllegalArgumentException
     *             if the lengths of the arrays are not the same (null is ok,
     *             and/or size 0)
     */
    public static String replaceEach(final String s, final String[] src, final String[] dst, final boolean repeat, final int levels) 
    {
        // mchyzer Performance note: This creates very few new objects (one major goal)
        // let me know if there are performance requests, we can create a harness to measure

        if (s == null || s.length()==0 || src == null || src.length == 0 || dst == null || dst.length == 0) 
        {
            return s;
        }

        // if recursing, this shouldn't be less than 0
        if (levels < 0) 
        {
            throw new IllegalStateException("Aborting to protect against StackOverflowError - output of one loop is the input of another");
        }

        // make sure lengths are ok, these need to be equal
        if (src.length != dst.length) 
        {
            throw new IllegalArgumentException("Search and Replace array lengths don't match: "
                + src.length
                + " vs "
                + dst.length);
        }

        // keep track of which still have matches
        final boolean[] noMoreMatchesForReplIndex = new boolean[(src.length)];

        // index on index that the match was found
        int textIndex = -1;
        int replaceIndex = -1;
        int tempIndex = -1;

        // index of replace array that will replace the search string found
        // NOTE: logic duplicated below START
        for (int i = 0; i < src.length; i++) 
        {
            if (noMoreMatchesForReplIndex[i] || src[i] == null || src[i].length()==0 || dst[i] == null) 
            {
                continue;
            }
            tempIndex = s.indexOf(src[i]);

            // see if we need to keep searching for this
            if (tempIndex == -1) 
            {
                noMoreMatchesForReplIndex[i] = true;
            } 
            else 
            {
                if (textIndex == -1 || tempIndex < textIndex) 
                {
                    textIndex = tempIndex;
                    replaceIndex = i;
                }
            }
        }
        // NOTE: logic mostly below END

        // no search strings found, we are done
        if (textIndex == -1) 
        {
            return s;
        }

        int start = 0;

        // get a good guess on the size of the result buffer so it doesn't have to double if it goes over a bit
        int increase = 0;

        // count the replacement text elements that are larger than their corresponding text being replaced
        for (int i = 0; i < src.length; i++) 
        {
            if (src[i] == null || dst[i] == null) 
            {
                continue;
            }
            final int greater = dst[i].length() - src[i].length();
            if (greater > 0) 
            {
                increase += 3 * greater; // assume 3 matches
            }
        }
        // have upper-bound at 20% increase, then let Java take over
        increase = Math.min(increase, s.length() / 5);

        final StringBuilder buf = new StringBuilder(s.length() + increase);

        while (textIndex != -1) 
        {

            for (int i = start; i < textIndex; i++) 
            {
                buf.append(s.charAt(i));
            }
            buf.append(dst[replaceIndex]);

            start = textIndex + src[replaceIndex].length();

            textIndex = -1;
            replaceIndex = -1;
            tempIndex = -1;
            // find the next earliest match
            // NOTE: logic mostly duplicated above START
            for (int i = 0; i < src.length; i++) 
            {
                if (noMoreMatchesForReplIndex[i] || src[i] == null || src[i].length()==0 || dst[i] == null) 
                {
                    continue;
                }
                tempIndex = s.indexOf(src[i], start);

                // see if we need to keep searching for this
                if (tempIndex == -1) 
                {
                    noMoreMatchesForReplIndex[i] = true;
                } 
                else 
                {
                    if (textIndex == -1 || tempIndex < textIndex) 
                    {
                        textIndex = tempIndex;
                        replaceIndex = i;
                    }
                }
            }
            // NOTE: logic duplicated above END

        }
        final int textLength = s.length();
        for (int i = start; i < textLength; i++) 
        {
            buf.append(s.charAt(i));
        }
        final String result = buf.toString();
        if (!repeat) 
        {
            return result;
        }

        return replaceEach(result, src, dst, repeat, levels - 1);
    }

    private static class Normalizer
    {
        private static final String NORMALIZE_ASCII = "A-Za-z0-9.\\s";
        private static final String NORMALIZE_ES    = "ÁÉÍÓÚáéíóúÜüÑñ";
        private static final String NORMALIZE_FR    = "ŒÆÂÊÎÔÛœæâêîôûÇçÀÈàèùÉéËÏÜŸëïüÿ";
        private static final String NORMALIZE_DE    = "ÄÖÜäöüẞß";
        private static final String NORMALIZE_RU    = "\\p{InCyrillic}";
        private static final String NORMALIZE_CJK   = "\\p{InCJK_UNIFIED_IDEOGRAPHS}";
        private static final String NORMALIZE_EL    = "\\p{InGreek}";
        private static final String NORMALIZE_AR    = "\\p{InArabic}";
        private static final String NORMALIZE_IW    = "\\p{InHebrew}";
        private static final String NORMALIZE_TH    = "\\p{InThai}";

        private static final String NORMALIZE_DICTIONARY[] =
        {
            "A=Á,À,Â,Ä,Ấ,Ã,Å", "C=Ć,Ĉ,Č,Ç", "E=É,È,Ê,Ë", "G=Ĝ,Ġ", "H=Ĥ,Ḧ", "I=Í,Ì,Î,Ï,Ĭ,Ĩ", "J=Ĵ", "K=Ḱ", "L=Ĺ", "M=Ḿ", "N=Ń,Ǹ,Ň,Ṅ,Ņ,Ṇ,Ṋ,Ṉ,Ñ", "O=Ó,Ò,Ô,Ö,Ő,Ṏ,Õ", "P=Ṕ", "R=Ŕ", "S=Ś,Ŝ,Š", "T=Ť,Ţ",   "U=Ú,Ù,Û,Ü,Ű,Ů,Ǘ,Ǜ", "W=Ẃ,Ẁ,Ŵ,Ẅ", "X=Ẍ", "Y=Ý,Ỳ,Ŷ,Ÿ", "Z=Ź,Ẑ",
            "a=á,à,â,ä,ấ,ã,å", "c=ć,ĉ,č,ç", "e=é,è,ê,ë", "g=ĝ,ġ", "h=ĥ,ḧ", "i=í,ì,î,ï,ĭ,ĩ", "j=ĵ", "k=ḱ", "l=ĺ", "m=ḿ", "n=ń,ǹ,ň,ṅ,ņ,ṇ,ṋ,ṉ,ñ", "o=ó,ò,ô,ö,ő,ṏ,õ", "p=ṕ", "r=ŕ", "s=ś,ŝ,š", "t=ť,ţ,ẗ", "u=ú,ù,û,ü,ű,ů,ǘ,ǜ", "w=ẃ,ẁ,ŵ,ẅ", "x=ẍ", "y=ý,ỳ,ŷ,ÿ", "z=ź,ẑ",
            "J=J́",
            "j=j́",
            //russian cyrillic - Passport (2013), ICAO
            "A=А", "B=Б", "V=В", "G=Г", "D=Д", "E=Е,Ё,Э", "ZH=Ж", "Z=З", "I=И,Й", "K=К", "L=Л", "M=М", "N=Н", "O=О", "P=П", "R=Р", "S=С", "T=Т", "U=У", "F=Ф", "kh=Х", "TS=Ц", "CH=Ч", "SH=Ш", "SHCH=Щ", "IE=Ъ", "Y=Ы", "=Ь", "IU=Ю", "IA=Я",
            "a=а", "b=б", "v=в", "g=г", "d=д", "e=е,ё,э", "zh=ж", "z=з", "i=и,й", "k=к", "l=л", "m=м", "n=н", "o=о", "p=п", "r=р", "s=с", "t=т", "u=у", "f=ф", "kh=х", "ts=ц", "ch=ч", "sh=ш", "shch=щ", "ie=ъ", "y=ы", "=ь", "iu=ю", "ia=я",
            //Latin Extended-A Block
            "A=Ā,Ă","E=Ę,Ě","L=Ł","R=Ř",
            "a=ā,ă","e=ę,ě","l=ł","r=ř",
        };

        private final Pattern normalizeAscii = Pattern.compile("[^"+NORMALIZE_ASCII+"]");
        private final Pattern normalizeEn = normalizeAscii;
        private final Pattern normalizeEs = Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_ES+"]");
        private final Pattern normalizeFr = Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_FR+"]");
        private final Pattern normalizeDe = Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_DE+"]");
        private final Pattern normalizeRu = Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_RU+"]");
        
        private final Pattern normalizeCjk= Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_CJK+"]");
        private final Pattern normalizeZh = normalizeCjk;
        private final Pattern normalizeJp = normalizeCjk;
        private final Pattern normalizeKo = normalizeCjk;
        private final Pattern normalizeEl = Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_EL+"]");
        private final Pattern normalizeAr = Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_AR+"]");
        private final Pattern normalizeIw = Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_IW+"]");
        private final Pattern normalizeTh = Pattern.compile("[^"+NORMALIZE_ASCII+NORMALIZE_TH+"]");
        
        private final HashMap<Locale,Pattern> normalizePatterns = new HashMap<>();
        private final HashMap<String,String> normalizeMap = new HashMap<>();
        //ɲ  ƞ ᶇ ɳ ȵ  --> n
        //r̀r̂r̃r̈rʼřt̀t̂ẗţỳỹẙyʼy̎ýp̂p̈s̀s̃s̈s̊sʼs̸Şşd̂d̃d̈ďdʼḑf̈f̸g̀g̃g̈gʼģq‌​́ĝǧḧĥj̈jʼḱk̂k̈k̸ǩl̂l̃l̈Łłẅẍc̃c̈c̊cʼ̸v̂v̈vʼ̸b́b̧ǹn̂n̈n̊nʼñm̀m̂m̃m̈‌​m̊m̌ǵß
        
        public Normalizer()
        {
            for (String item : NORMALIZE_DICTIONARY)
            {
                String[] terms = item.split("=");
                String fix = terms[0];
                String[] tokens = terms[1].split(",");
                for(int i=0;i<tokens.length;i++)
                {
                    String prev = normalizeMap.put(tokens[i], fix);
                    assert prev==null : item;
                }
            }
            normalizePatterns.put(Locale.ENGLISH,   normalizeEn);
            normalizePatterns.put(new Locale("es"), normalizeEs);
            normalizePatterns.put(Locale.FRENCH,    normalizeFr);
            normalizePatterns.put(Locale.GERMAN,    normalizeDe);
            normalizePatterns.put(new Locale("ru"), normalizeRu);           
            normalizePatterns.put(new Locale("zh"), normalizeZh);
            normalizePatterns.put(new Locale("jp"), normalizeJp);
            normalizePatterns.put(new Locale("ko"), normalizeKo);
            normalizePatterns.put(new Locale("el"), normalizeEl);
            normalizePatterns.put(new Locale("ar"), normalizeAr);
            normalizePatterns.put(new Locale("iw"), normalizeIw);
            normalizePatterns.put(new Locale("th"), normalizeTh);            
        }
        static final Normalizer INSTANCE = new Normalizer();
    }
    private static Pattern getNormalizePattern(Locale locale)
    {
        Pattern pattern = Normalizer.INSTANCE.normalizePatterns.get(locale);
        if(pattern==null)
        {
            Locale[] locales = Locales.getParents(locale);
            for(int i=0; i<locales.length && pattern==null ;i++)
            {
                pattern = Normalizer.INSTANCE.normalizePatterns.get(locales[i]);
            }
        }
        return (pattern!=null) ? pattern : Normalizer.INSTANCE.normalizeAscii;
    }
    public static String normalize(String s)
    {
        return normalize(s, Locale.getDefault());
    }
    public static String normalize(String s, Locale locale)
    {
        if(s==null || s.length()==0) 
        {
            return s;
        }
        
        Matcher matcher = getNormalizePattern(locale).matcher(s);
        StringBuffer sb = new StringBuffer();
        boolean found = false;
        while(matcher.find()) 
        {
            found=true;
            String token = matcher.group();
            String fix = Normalizer.INSTANCE.normalizeMap.get(token);
            matcher.appendReplacement(sb, fix!=null ? fix : "");
        }
        return found ? matcher.appendTail(sb).toString() : s;
    }
    public static String normalizeUgly(String s, boolean group)
    {
        String regex = group ? "[\ufff0-\uffff]+" : "[\ufff0-\uffff]";
        return s.replaceAll( regex, " ");
    }
    
    /**
     * Returns a new String composed of copies of the 
     * {@code CharSequence items} joined together with no delimiter.
     *
     * <blockquote>For example,
     * <pre>{@code
     *     String message = Strings.join("Java", "Is", "Cool");
     *     // message returned is: "JavaIsCool"
     * }</pre></blockquote>
     *
     * Note that if an element is null, then {@code "null"} is added.
     *
     * @param  items the items to join together.
     *
     * @return a new {@code String} that is composed of the {@code items}
     *
     * @throws NullPointerException If {@code items} is {@code null}
     *
     */    
    public static String join(CharSequence... items)
    {
        StringBuilder sb = new StringBuilder();
        for(CharSequence cs : items)
        {
            sb.append(cs!=null ? cs : "null");
        }
        return sb.toString();
    }

    public static boolean equals(String s1, String s2)
    {
        return s1==null ? s2==null : s1.equals(s2);
    }
    public static boolean equalsIgnoreCase(String s1, String s2)
    {
        return s1==null ? s2==null : s1.equalsIgnoreCase(s2);
    }

    public static String[] split(String s, int cols)
    {
        int n = s.length();
        int r = (n/cols) + (n%cols!=0?1:0);
        String rows[] = new String[r];
        for(int i=0,j=0;i<n;i+=cols,j++)
        {
            rows[j] = s.substring(i,Math.min(i+cols, n));
        }
        return rows;
    }
    public static String split(String s, int cols, String sep)
    {
        int n = s.length();
        StringBuilder rows = new StringBuilder();
        for(int i=0;i<n;i+=cols)
        {
            rows.append(s.substring(i,Math.min(i+cols, n))).append(sep);
        }
        return rows.toString();
    }
    
    /**
     * Returns true if, and only if, s is a palindrome. 
     * @param s the string to be tested as a palindrome
     * @return true if s is a palindrome, otherwise false
     */
    public static boolean isPalindrome(String s)
    {
        int cp = s.codePointCount(0, s.length());
        for(int i=0,e=cp;i<cp;i++,e--)
        {
            if(s.codePointAt(i) != s.codePointBefore(e))
            {
                return false;
            }
        }
        return true;
    }

    public static int compareTo(String a, String b)
    {
        if(a==null && b!=null)
        {
            return -1;
        }
        if(a!=null && b==null)
        {
            return +1;
        }
        if(a!=null && b!=null)
        {
            return a.compareTo(b);
        }
        return 0;
    }
    public static int compareTo(String[] a, String[] b)
    {
        int cmp = 0;
        int count = Math.min(a.length, b.length);
        for(int i=0;i<count && cmp==0;i++)
        {
            cmp = compareTo(a[i], b[i]);
        }
        if(cmp==0 && a.length!=b.length)
        {
            cmp = Integer.compare(a.length, b.length);
        }
        return cmp;
    }
    public static int compareToIgnoreCase(String a, String b)
    {
        if(a==null && b!=null)
        {
            return -1;
        }
        if(a!=null && b==null)
        {
            return +1;
        }
        if(a!=null && b!=null)
        {
            return a.compareToIgnoreCase(b);
        }
        return 0;
    }
    public static int compareToIgnoreCase(String[] a, String[] b)
    {
        int cmp = 0;
        int count = Math.min(a.length, b.length);
        for(int i=0;i<count && cmp==0;i++)
        {
            cmp = compareToIgnoreCase(a[i], b[i]);
        }
        if(cmp==0 && a.length!=b.length)
        {
            cmp = Integer.compare(a.length, b.length);
        }
        return cmp;
    }

    public static <T> String csv(Collection<T> list, String nullValue)
    {
        return join(",", "", "", list, nullValue);
    }
    public static <T> String csv(Collection<T> list)
    {
        return join(",", "", "", list, null);
    }
    public static <T> String csv(T[] list, String nullValue)
    {
        return join(",", "", "", Arrays.asList(list), nullValue);
    }
    public static <T> String csv(T[] list)
    {
        return join(",", "", "", Arrays.asList(list), null);
    }

    public static int codePointCount(String s)
    {
        return s.codePointCount(0, s.length());
    }
    public static int[] codePoints(String s)
    {
        if(s==null)
        {
            return null;
        }
        int count = s.codePointCount(0, s.length());
        int[] codepoints = new int[count];
        for(int i=0;i<count;i++)
        {
            codepoints[i] = s.codePointAt(i);
        }
        return codepoints;
    }

//     * <pre>
//     * Strings.capitalize(null)        = null
//     * Strings.capitalize("")          = ""
//     * Strings.capitalize("i am FINE") = "I Am FINE"
//     * </pre>
    public static String capitalize(String s) 
    {
        return capitalize(s, null);
    }
    
    private static final char[] defaultDelimiter = {' '};
//     * <pre>
//     * Strings.capitalize(null, *)            = null
//     * Strings.capitalize("", *)              = ""
//     * Strings.capitalize(*, new char[0])     = *
//     * Strings.capitalize("i am fine", null)  = "I Am Fine"
//     * Strings.capitalize("i aM.fine", {'.'}) = "I aM.Fine"
//     * </pre>    
    public static String capitalize(String s, char... delimiters) 
    {
        int delimLen = (delimiters==null ? (delimiters=defaultDelimiter).length : delimiters.length);
        if (s == null || s.length() == 0 || delimLen == 0) 
        {
            return s;
        }
        int size = s.codePointCount(0, s.length());
        StringBuilder buffer = new StringBuilder(size);
        boolean capitalizeNext = true;
        for (int i = 0; i < size; i++) 
        {
            int cp = s.codePointAt(i);
            if(isDelimiter(cp, delimiters)) 
            {
                buffer.appendCodePoint(cp);
                capitalizeNext = true;
            } 
            else if (capitalizeNext) 
            {
                buffer.appendCodePoint(Character.toTitleCase(cp));
                capitalizeNext = false;
            } 
            else 
            {
                buffer.appendCodePoint(cp);
            }
        }
        return buffer.toString();
    }    
    
     /**
     * Is the character a delimiter.
     *
     * @param ch  the character to check
     * @param delimiters  the delimiters
     * @return true if it is a delimiter
     */
    private static boolean isDelimiter(char ch, char[] delimiters) 
    {
        if (delimiters == null) 
        {
            return Character.isWhitespace(ch);
        }
        for (int i = 0, isize = delimiters.length; i < isize; i++) 
        {
            if (ch == delimiters[i]) {
                return true;
            }
        }
        return false;
    }    
     /**
     * Is the character a delimiter.
     *
     * @param codePoint  the character to check
     * @param delimiters  the delimiters
     * @return true if it is a delimiter
     */
    private static boolean isDelimiter(int codePoint, char[] delimiters) 
    {
        if (delimiters == null) 
        {
            return Character.isWhitespace(codePoint);
        }
        for (int i = 0, isize = delimiters.length; i < isize; i++) 
        {
            if (codePoint == delimiters[i]) 
            {
                return true;
            }
        }
        return false;
    }

    public static String uniqueCodepoints(String s)
    {
        StringBuilder sb = new StringBuilder();
        HashSet<Integer> uniques = new HashSet<>();
        int cp = s.codePointCount(0, s.length());
        for(int i=0;i<cp;i++)
        {
            int cpa = s.codePointAt(i);
            if(uniques.add(cpa))
            {
                sb.appendCodePoint(cpa);
            }
        }
        return sb.toString();
    }
    public static int uniqueCodepointCount(String s)
    {
        HashSet<Integer> uniques = new HashSet<>();
        int cp = s.codePointCount(0, s.length());
        for(int i=0;i<cp;i++)
        {
            int cpa = s.codePointAt(i);
            uniques.add(cpa);
        }
        return uniques.size();
    }
    
    //0 just cat, >0 add n to the bigger column, <0 replace at column -n
    public static String mergeRows(String left, String right, int cols)
    {
        String[] leftRows = left.split("\n");
        String[] rightRows = right.split("\n");
        int minCount = Math.min(leftRows.length, rightRows.length);
        
        int leftWidth = 0;
        
        if(cols>=0)
        {
            for(String row : leftRows)
            {
                leftWidth = Math.max(leftWidth, row.length());
            }
            leftWidth += cols;
        }
        
        String sep = cols>=0 ? repeat(' ', leftWidth+cols) : "";
        
        StringBuilder rows = new StringBuilder();

        for(int i=0;i<minCount;i++)
        {
            if(!leftRows[i].isEmpty() || !rightRows[i].isEmpty())
            {
                rows.append(leftRows[i]);
                int tail = leftWidth - leftRows[i].length();
                if(tail>0) 
                {
                    rows.append(sep, 0, tail);
                }
                rows.append(rightRows[i]);
            }
            rows.append('\n');
        }
        for(int i=minCount;i<leftRows.length;i++)
        {
            if(!leftRows[i].isEmpty())
            {
                rows.append(leftRows[i]);
                int tail = leftWidth - leftRows[i].length();
                if(tail>0) 
                {
                    rows.append(sep, 0, tail);
                }
            }
            rows.append('\n');
        }
        for(int i=minCount;i<rightRows.length;i++)
        {
            if(!rightRows[i].isEmpty())
            {
                rows.append(sep, 0, leftWidth);
                rows.append(rightRows[i]);
            }
            rows.append('\n');
        }
        return rows.toString();
    }
    public static String mergeRows(int cols, String... rows)
    {
        String s = rows.length>0 ? rows[0] : "";
        for(int i=1;i<rows.length;i++)
        {
            s = mergeRows(s, rows[i], cols);
        }
        return s;
    }
    public static int overlapped(String start, String end)
    {
        if(start==null||end==null||start.isEmpty()||end.isEmpty())
        {
            return 0;
        }
        char[] s = start.toCharArray();
        char[] e = end.toCharArray();
        int at = Math.max(0, s.length-e.length);
        int count = 0;
        for(int i=at;i<s.length && count==0;i++)
        {
            for(int j=i;j<s.length;j++,count++)
            {
                if(s[j]!=e[count])
                {
                    count = 0;
                    break;
                }
            }
        }
        return count;
    }
    public static String overlap(String... s)
    {
        String full = "";
        
        for(String item : s)
        {
            int count = overlapped(full, item);
            full += (count==0) ? item : item.substring(count);
        }
        return full;
    }
    
    private static final String UNQUOTE = "Q([^Q]+|q[^q]+q)Q";
    private static final char DOUBLE_QUOTATION = '"';
    private static final char SINGLE_QUOTATION = '\'';
    
    private static final Pattern unquoteDouble = Pattern.compile(UNQUOTE.replace('Q',DOUBLE_QUOTATION).replace('q',SINGLE_QUOTATION));
    private static final Pattern unquoteSingle = Pattern.compile(UNQUOTE.replace('Q',SINGLE_QUOTATION).replace('q',DOUBLE_QUOTATION));
    
    /**
     * Unquotes a string recursively from double and single quotations. This
     * method does not trim the passed value nor the unquoted.
     * @param s the string to be unquoted
     * @return the unquoted string if it has valid quotation or the same string if not
     */
    public static String unquote(String s)
    {
        String quoted = null;
        while(s!=quoted)
        {
            quoted = s;
            s = unquoteDouble(s);
            s = unquoteSingle(s);
        }
        return s;
    }

    /**
     * Unquotes a string from a double quotation, just one. This method does not 
     * unquote recursively and does not trim before unquote.
     * @param s the string to be unquoted
     * @return the unquoted string if it has valid quotations or the same string if not
     */
    public static String unquoteDouble(String s)
    {
        if(s!=null && unquoteDouble.matcher(s).matches())
        {
            return s.substring(1, s.length()-1);
        }
        return s;
    }

    /**
     * Unquotes a string from a single quotation, just one. This method does not 
     * unquote recursively and does not trim before unquote.
     * @param s the string to be unquoted
     * @return the unquoted string if it has valid quotations or the same string if not
     */
    public static String unquoteSingle(String s)
    {
        if(s!=null && unquoteSingle.matcher(s).matches())
        {
            return s.substring(1, s.length()-1);
        }
        return s;
    }
    
    /**
     * Quotes a string using a single quotation, and will scape existing single quotes.
     * @param s the string to be quoted
     * @return the quoted string or null is the value is null
     */
    public static String quoteSingle(String s)
    {
        if(s!=null)
        {
            s = "'"+s.replace("'","\\'")+"'";
        }
        return s;
    }
    /**
     * Quotes a string using a double quotation, and will scape existing double quotes.
     * @param s the string to be quoted
     * @return the quoted string or null is the value is null
     */
    public static String quoteDouble(String s)
    {
        if(s!=null)
        {
            s = "\""+s.replaceAll("\"","\\\"")+"\"";
        }
        return s;
    }
    
    
    public static HashSet<String> delimiterSeparatedValuesToSetString(String values, String sep)
    {
        HashSet<String> set = new HashSet<>();
        set.addAll(Arrays.asList(values.split(sep)));
        return set;
    }
    public static HashSet<String> commaSeparatedValuesToSetString(String values, boolean trim)
    {
        return delimiterSeparatedValuesToSetString(values, trim?" *, *":",");
    }
    public static HashSet<String> commaSeparatedValuesToSetString(String values)
    {
        return commaSeparatedValuesToSetString(values, false);
    }
    


    public static String collectCodePoints(String s, String exclude)
    {
        HashSet<Integer> set = new HashSet<>();
        int excludeCount;
        if(exclude!=null && (excludeCount=exclude.codePointCount(0, exclude.length()))>0)
        {
            for(int i=0;i<excludeCount;i++)
            {
                set.add(exclude.codePointAt(i));
            }
        }
        
        int count;
        if(s!=null && (count=s.codePointCount(0, s.length()))>0)
        {
            StringBuilder sb = new StringBuilder(1024);
            for(int i=0;i<count;i++)
            {
                int cp = s.codePointAt(i);
                if(set.add(cp))
                {
                    sb.appendCodePoint(cp);
                }
            }
            return sb.toString();
        }
        return "";
    }
    
    /**
     * <p>Compares all Strings in an array and returns the initial sequence of 
     * characters that is common to all of them.</p>
     *
     * <p>For example,
     * <code>getCommonPrefix(new String[] {"i am a machine", "i am a robot"}) -> "i am a "</code></p>
     *
     * <pre>
     * Strings.getCommonPrefix(null) = ""
     * Strings.getCommonPrefix(new String[] {}) = ""
     * Strings.getCommonPrefix(new String[] {"abc"}) = "abc"
     * Strings.getCommonPrefix(new String[] {null, null}) = ""
     * Strings.getCommonPrefix(new String[] {"", ""}) = ""
     * Strings.getCommonPrefix(new String[] {"", null}) = ""
     * Strings.getCommonPrefix(new String[] {"abc", null, null}) = ""
     * Strings.getCommonPrefix(new String[] {null, null, "abc"}) = ""
     * Strings.getCommonPrefix(new String[] {"", "abc"}) = ""
     * Strings.getCommonPrefix(new String[] {"abc", ""}) = ""
     * Strings.getCommonPrefix(new String[] {"abc", "abc"}) = "abc"
     * Strings.getCommonPrefix(new String[] {"abc", "a"}) = "a"
     * Strings.getCommonPrefix(new String[] {"ab", "abxyz"}) = "ab"
     * Strings.getCommonPrefix(new String[] {"abcde", "abxyz"}) = "ab"
     * Strings.getCommonPrefix(new String[] {"abcde", "xyz"}) = ""
     * Strings.getCommonPrefix(new String[] {"xyz", "abcde"}) = ""
     * Strings.getCommonPrefix(new String[] {"i am a machine", "i am a robot"}) = "i am a "
     * </pre>
     *
     * @param strs  array of String objects, entries may be null
     * @return the initial sequence of characters that are common to all Strings
     * in the array; empty String if the array is null, the elements are all null 
     * or if there is no common prefix. 
     */
    public static String commonPrefix(String[] strs) 
    {
        if(strs == null || strs.length == 0) 
        {
            return EMPTY;
        }
        if(strs.length==1)
        {
            return strs[0]!=null ? strs[0] : EMPTY;
        }
        
        int min = Integer.MAX_VALUE;
        for (String s : strs)
        {
            if(s==null)
            {
                return EMPTY;
            }
            min = Math.min(min, s.length());
        }
        
        for(int i=0;i<min;i++)    
        {
            char c = strs[0].charAt(i);
            for(int j=1;j<strs.length;j++)
            {
                char cc = strs[j].charAt(i);
                if(c!=cc)
                {
                    return strs[j].substring(0, i);
                }
            }
        }
        return strs[0].substring(0, min);
    }
    
    public static String skip(String s, int n)
    {
        if(s==null)
        {
            return null;
        }
        if(n==0)
        {
            return s;
        }
        if(n>=s.length())
        {
            return "";
        }
        return s.substring(n, s.length());
    }
    
    /**
     * <p>Centers a String in a larger String of size <code>size</code>
     * using the space character (' ').<p>
     *
     * <p>If the size is less than the String length, the String is returned.
     * A <code>null</code> String returns <code>null</code>.
     * A negative size is treated as zero.</p>
     *
     * <p>Equivalent to <code>center(str, size, " ")</code>.</p>
     *
     * <pre>
     * Strings.center(null, *)   = null
     * Strings.center("", 4)     = "    "
     * Strings.center("ab", -1)  = "ab"
     * Strings.center("ab", 4)   = " ab "
     * Strings.center("abcd", 2) = "abcd"
     * Strings.center("a", 4)    = " a  "
     * </pre>
     *
     * @param s  the String to center, may be null
     * @param size  the int size of new String, negative treated as zero
     * @return centered String, <code>null</code> if null String input
     */
    public static String center(String s, int size)
    {
        return center(s, size, ' ');
    }

    /**
     * <p>Centers a String in a larger String of size <code>size</code>.
     * Uses a supplied character as the value to pad the String with.</p>
     *
     * <p>If the size is less than the String length, the String is returned.
     * A <code>null</code> String returns <code>null</code>.
     * A negative size is treated as zero.</p>
     *
     * <pre>
     * Strings.center(null, *, *)     = null
     * Strings.center("", 4, ' ')     = "    "
     * Strings.center("ab", -1, ' ')  = "ab"
     * Strings.center("ab", 4, ' ')   = " ab "
     * Strings.center("abcd", 2, ' ') = "abcd"
     * Strings.center("a", 4, ' ')    = " a  "
     * Strings.center("a", 4, 'y')    = "yayy"
     * </pre>
     *
     * @param s  the String to center, may be null
     * @param size  the int size of new String, negative treated as zero
     * @param pad  the character to pad the new String with
     * @return centered String, <code>null</code> if null String input
     */
    public static String center(String s, int size, char pad)
    {
        if (s == null || size <= 0)
        {
            return s;
        }
        int strLen = s.length();
        int pads = size - strLen;
        if (pads <= 0)
        {
            return s;
        }
        s = paddingLeft(s, strLen + pads / 2, pad);
        s = paddingRight(s, size, pad);
        return s;
    }

    /**
     * Gets a CharSequence's length or <code>0</code> if the CharSequence is <code>null</code>.
     * 
     * @param s a CharSequence or <code>null</code>
     * @return CharSequence length or <code>0</code> if the CharSequence is <code>null</code>.
     */
    public static int length(CharSequence s)
    {
        return s==null ? 0 : s.length();
    }

}
