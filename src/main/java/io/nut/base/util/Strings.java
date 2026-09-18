/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    /**
     * <p>Joins the elements of the given {@code Iterable} into a single String
     * using the provided delimiter, prefix and suffix. A <code>null</code>
     * element is replaced by the given <code>nullValue</code>, or represented
     * by the literal <code>"null"</code> if <code>nullValue</code> is
     * <code>null</code>.</p>
     *
     * @param <T> the type of elements in the list
     * @param delimiter the delimiter between each element
     * @param prefix the prefix to prepend to the result
     * @param suffiex the suffix to append to the result
     * @param list the values to join together, may not be null
     * @param nullValue the value to use for {@code null} elements, may be null
     * @return the joined String
     */
    public static <T> String join(CharSequence delimiter, CharSequence prefix, CharSequence suffiex, Iterable<T> list, CharSequence nullValue)
    {
        StringJoiner sj = new StringJoiner(delimiter, prefix, suffiex);
        for(T item : list)
        {
            sj.add(item!=null ? item.toString() : nullValue);
        }
        return sj.toString();
    }
    /**
     * <p>Joins the elements of the given {@code Iterable} into a single String
     * using the provided delimiter, without prefix or suffix.</p>
     *
     * <pre>
     * Strings.join("-", Arrays.asList("a", "b", "c")) = "a-b-c"
     * </pre>
     *
     * @param <T> the type of elements in the list
     * @param delimiter the delimiter between each element
     * @param list the values to join together, may not be null
     * @return the joined String
     */
    public static <T> String join(CharSequence delimiter, Iterable<T> list)
    {
        return join(delimiter, "", "", list, null);
    }
    /**
     * <p>Joins the elements of the given array into a single String using the
     * provided delimiter, without prefix or suffix. A <code>null</code>
     * element is replaced by the given <code>nullValue</code>.</p>
     *
     * @param <T> the type of elements in the array
     * @param delimiter the delimiter between each element
     * @param list the values to join together, may not be null
     * @param nullValue the value to use for {@code null} elements, may be null
     * @return the joined String
     */
    public static <T> String join(CharSequence delimiter, T[] list, String nullValue)
    {
        return join(delimiter, "", "", Arrays.asList(list), nullValue);
    }
    /**
     * <p>Joins the elements of the given array into a single String using the
     * provided delimiter, without prefix or suffix.</p>
     *
     * <pre>
     * Strings.join("-", new String[]{"a", "b", "c"}) = "a-b-c"
     * </pre>
     *
     * @param <T> the type of elements in the array
     * @param delimiter the delimiter between each element
     * @param elements the values to join together, may not be null
     * @return the joined String
     */
    public static <T> String join(CharSequence delimiter, T[] elements)
    {
        return join(delimiter, "", "", Arrays.asList(elements), null);
    }
    
    /**
     * <p>Returns a String consisting of the given character repeated
     * <code>count</code> times.</p>
     *
     * <pre>
     * Strings.repeat('a', 3) = "aaa"
     * Strings.repeat('a', 0) = ""
     * </pre>
     *
     * @param c the character to repeat
     * @param count the number of repetitions, may be zero
     * @return the repeated String
     */
    public static String repeat(char c,int count)
    {
        char[] tmp = new char[count];
        Arrays.fill(tmp,c);
        return new String(tmp,0,count);
    }
    /**
     * <p>Returns a String consisting of the given String repeated
     * <code>count</code> times.</p>
     *
     * <pre>
     * Strings.repeat("ab", 3) = "ababab"
     * Strings.repeat("ab", 0) = ""
     * </pre>
     *
     * @param s the String to repeat, may be null
     * @param count the number of repetitions, may be zero
     * @return the repeated String, <code>null</code> if null String input
     */
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
    
    /**
     * <p>Pad or truncate the given StringBuilder to the given size filling with
     * the given character.</p>
     *
     * <p>If <code>insert</code> is <code>true</code> the padding characters are
     * inserted at the beginning, otherwise they are appended at the end.
     * If the builder is already longer than <code>size</code> it is returned
     * unchanged.</p>
     *
     * @param builder the StringBuilder to pad, may not be null
     * @param c the character to pad with
     * @param size the target size
     * @param insert if true the padding is inserted at the beginning
     * @return the padded StringBuilder
     */
    public static StringBuilder fill(StringBuilder builder,char c, int size, boolean insert)
    {
        int count = Math.max(size-builder.length(), 0);
        String cc = repeat(c,count);
        return insert?builder.insert(0, cc):builder.append(cc);
    }
    /**
     * <p>Pad or truncate the given CharSequence to the given size filling with
     * the given character.</p>
     *
     * <p>If <code>insert</code> is <code>true</code> the padding characters are
     * inserted at the beginning, otherwise they are appended at the end.
     * If the CharSequence is already longer than <code>size</code> it is returned
     * unchanged.</p>
     *
     * @param cs the CharSequence to pad, may not be null
     * @param c the character to pad with
     * @param size the target size
     * @param insert if true the padding is inserted at the beginning
     * @return the padded String
     */
    public static String fill(CharSequence cs,char c, int size, boolean insert)
    {
        return fill(new StringBuilder(cs),c,size,insert).toString();
    }
    /**
     * <p>Pad or truncate the given StringBuilder to the given size filling with
     * the given character. The padding characters are appended at the end.</p>
     *
     * @param builder the StringBuilder to pad, may not be null
     * @param c the character to pad with
     * @param size the target size
     * @return the padded StringBuilder
     */
    public static StringBuilder fill(StringBuilder builder,char c, int size)
    {
        return fill(builder,c,size,false);
    }
    /**
     * <p>Pad or truncate the given CharSequence to the given size filling with
     * the given character. The padding characters are appended at the end.</p>
     *
     * @param cs the CharSequence to pad, may not be null
     * @param c the character to pad with
     * @param size the target size
     * @return the padded String
     */
    public static String fill(CharSequence cs,char c, int size)
    {
        return fill(new StringBuilder(cs),c,size,false).toString();
    }

    /**
     * <p>Gets the leftmost <code>count</code> characters of the given String.</p>
     *
     * <p>If <code>count</code> is greater than or equal to the String length,
     * the String itself is returned. A <code>null</code> String returns
     * <code>null</code>.</p>
     *
     * @param s the String to get the leftmost characters from, may be null
     * @param count the number of characters to return
     * @return the leftmost <code>count</code> characters, <code>null</code> if null String input
     */
    public static String left(String s, int count)
    {
        if (s == null)
        {
            return null;
        }
        return count >= s.length() ? s : s.substring(0, count);
    }
    
    /**
     * <p>Gets the rightmost <code>count</code> characters of the given String.</p>
     *
     * <p>If <code>count</code> is greater than or equal to the String length,
     * the String itself is returned. A <code>null</code> String returns
     * <code>null</code>.</p>
     *
     * @param s the String to get the rightmost characters from, may be null
     * @param count the number of characters to return
     * @return the rightmost <code>count</code> characters, <code>null</code> if null String input
     */
    public static String right(String s, int count)
    {
        if (s == null)
        {
            return null;
        }
        int len = s.length();
        return count >= len ? s : s.substring(len - count);
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
    
    private static boolean isTrimChar(char c)
    {
        return c <= ' ' || c == '\u00a0';
    }
    /**
     * <p>Trims all whitespace characters, including non-breaking spaces and
     * line separators, from the given String.</p>
     *
     * <p>Besides the characters trimmed by {@link String#trim()}, this method
     * also removes Unicode non-breaking spaces and newline characters. A
     * <code>null</code> String returns <code>null</code>.</p>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String, <code>null</code> if null String input
     */
    public static String trimWhitespaces(String str)
    {
        if(str==null)
        {
            return null;
        }
        int start = 0;
        int end = str.length();
        while(start<end && isTrimChar(str.charAt(start)))
        {
            start++;
        }
        while(end>start && isTrimChar(str.charAt(end-1)))
        {
            end--;
        }
        return str.substring(start, end);
    }
            
    /**
     * <p>Returns the first non-<code>null</code> among the given values,
     * or <code>null</code> if all of them are <code>null</code>.</p>
     *
     * @param first the first candidate, may be null
     * @param second the second candidate, may be null
     * @param others further candidates, may be null
     * @return the first non-<code>null</code> value or <code>null</code> if none
     */
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
    /**
     * <p>Returns the {@code toString()} of the first non-<code>null</code>
     * among the given values, or <code>null</code> if all of them are
     * <code>null</code>.</p>
     *
     * @param <T> the type of the values
     * @param first the first candidate, may be null
     * @param second the second candidate, may be null
     * @param others further candidates, may be null
     * @return the {@code toString()} of the first non-<code>null</code> value or <code>null</code> if none
     */
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
    /**
     * <p>Returns <code>null</code> if the given String is <code>null</code> or
     * empty (""), otherwise returns the String itself.</p>
     *
     * @param value the String to check, may be null
     * @return the String itself if not empty, <code>null</code> if null or empty input
     */
    public static String nullForEmpty(String value)
    {
        return (value==null || value.length()==0) ? null : value;
    }
    /**
     * <p>Returns the given String, or an empty String ("") if it is
     * <code>null</code>.</p>
     *
     * <pre>
     * Strings.defaultString(null) = ""
     * Strings.defaultString("ab") = "ab"
     * </pre>
     *
     * @param s the String to use, may be null
     * @return the String itself or an empty String if <code>null</code> input
     */
    public static String defaultString(String s)
    {
        return (s==null) ? "" : s;
    }
    //convierte a string si es posible, evitando NullPointerException si es nulo
    /**
     * <p>Returns the {@code toString()} of the given object, or <code>null</code>
     * if it is <code>null</code>.</p>
     *
     * @param value the object to convert, may be null
     * @return the {@code toString()} or <code>null</code> if the object is null
     */
    public static String safeToString(Object value)
    {
        return (value!=null) ? value.toString() : null;
    }
    /**
     * <p>Returns the {@code toString()} of the given object, or the given
     * safe value if the object is <code>null</code>.</p>
     *
     * @param value the object to convert, may be null
     * @param safeValue the value to use if the object is null
     * @return the {@code toString()} or the safe value if the object is null
     */
    public static String safeToString(Object value, String safeValue)
    {
        return (value!=null) ? value.toString() : safeValue;
    }
    /**
     * <p>Counts how many times the given pattern occurs in the given String,
     * without overlapping matches.</p>
     *
     * @param s the String to search in, may not be null
     * @param pattern the substring to search for, may not be null
     * @return the number of occurrences
     */
    public static int ocurrences(String s, String pattern)
    {
        return ocurrences(s, pattern, false, false);
    }
    /**
     * <p>Counts how many times the given pattern occurs in the given String,
     * without overlapping matches.</p>
     *
     * @param s the String to search in, may not be null
     * @param pattern the substring to search for, may not be null
     * @param ignoreCase if true the comparison is case insensitive
     * @return the number of occurrences
     */
    public static int ocurrences(String s, String pattern, boolean ignoreCase)
    {
        return ocurrences(s, pattern, ignoreCase, false);
    }
    /**
     * <p>Counts how many times the given pattern occurs in the given String.</p>
     *
     * <p>If <code>overlap</code> is <code>true</code>, overlapping matches are
     * counted (e.g. "aa" in "aaa" gives 2); otherwise matches must not overlap
     * (e.g. "aa" in "aaa" gives 1).</p>
     *
     * @param s the String to search in, may not be null
     * @param pattern the substring to search for, may not be null
     * @param ignoreCase if true the comparison is case insensitive
     * @param overlap if true overlapping matches are counted
     * @return the number of occurrences
     */
    public static int ocurrences(String s, String pattern, boolean ignoreCase, boolean overlap)
    {
        int count=0;
        final int sn = s.length();
        final int pn = pattern.length();
        if(sn>0 && pn>0)
        {
            final int num=sn-pn;
            for(int i=0;i<=num;)
            {
                if(ignoreCase ? s.regionMatches(true, i, pattern, 0, pn) : s.startsWith(pattern, i))
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

    /**
     * <p>Removes a prefix from a String, if the String starts with it.</p>
     *
     * <p>Behaves like Python's {@code str.removeprefix(prefix)}. If the String
     * does not start with the prefix, it is returned unchanged. <code>null</code>s
     * are handled without exceptions.</p>
     *
     * <pre>
     * Strings.removePrefix(null, "ab")     = null
     * Strings.removePrefix("abc", null)    = "abc"
     * Strings.removePrefix("abc", "")      = "abc"
     * Strings.removePrefix("abcabc", "ab") = "cabc"
     * Strings.removePrefix("abc", "ab")    = "c"
     * Strings.removePrefix("abc", "abc")   = ""
     * Strings.removePrefix("abc", "xyz")   = "abc"
     * </pre>
     *
     * @param s the String to process, may be null
     * @param prefix the prefix to remove, may be null
     * @return the String without the prefix, or the original String if it does
     *  not start with the prefix
     */
    public static String removePrefix(String s, String prefix)
    {
        if(s==null || prefix==null)
        {
            return s;
        }
        if(s.startsWith(prefix))
        {
            return s.substring(prefix.length());
        }
        return s;
    }

    /**
     * <p>Removes a suffix from a String, if the String ends with it.</p>
     *
     * <p>Behaves like Python's {@code str.removesuffix(suffix)}. If the String
     * does not end with the suffix, it is returned unchanged. <code>null</code>s
     * are handled without exceptions.</p>
     *
     * <pre>
     * Strings.removeSuffix(null, "bc")     = null
     * Strings.removeSuffix("abc", null)    = "abc"
     * Strings.removeSuffix("abc", "")      = "abc"
     * Strings.removeSuffix("abcabc", "bc") = "abc"
     * Strings.removeSuffix("abc", "bc")    = "a"
     * Strings.removeSuffix("abc", "abc")   = ""
     * Strings.removeSuffix("abc", "xyz")   = "abc"
     * </pre>
     *
     * @param s the String to process, may be null
     * @param suffix the suffix to remove, may be null
     * @return the String without the suffix, or the original String if it does
     *  not end with the suffix
     */
    public static String removeSuffix(String s, String suffix)
    {
        if(s==null || suffix==null)
        {
            return s;
        }
        if(s.endsWith(suffix))
        {
            return s.substring(0, s.length()-suffix.length());
        }
        return s;
    }

    /**
     * <p>Converts a String to lower case as per {@link String#toLowerCase()}.</p>
     *
     * <p>A <code>null</code> input String returns <code>null</code>.</p>
     *
     * <pre>
     * Strings.toLowerCase(null)  = null
     * Strings.toLowerCase("")    = ""
     * Strings.toLowerCase("aBc") = "abc"
     * </pre>
     *
     * @param s the String to lower case, may be null
     * @return the lower cased String, <code>null</code> if null String input
     */
    public static String toLowerCase(String s)
    {
        return (s!=null) ? s.toLowerCase() : null;
    }
    /**
     * <p>Converts a String to lower case using the given <code>Locale</code>
     * as per {@link String#toLowerCase(Locale)}.</p>
     *
     * <p>A <code>null</code> input String returns <code>null</code>.</p>
     *
     * @param s the String to lower case, may be null
     * @param locale the Locale used to change the case
     * @return the lower cased String, <code>null</code> if null String input
     */
    public static String toLowerCase(String s, Locale locale)
    {
        return (s!=null) ? s.toLowerCase(locale) : null;
    }
    
    /**
     * <p>Converts a String to upper case as per {@link String#toUpperCase()}.</p>
     *
     * <p>A <code>null</code> input String returns <code>null</code>.</p>
     *
     * <pre>
     * Strings.toUpperCase(null)  = null
     * Strings.toUpperCase("")    = ""
     * Strings.toUpperCase("aBc") = "ABC"
     * </pre>
     *
     * @param s the String to upper case, may be null
     * @return the upper cased String, <code>null</code> if null String input
     */
    public static String toUpperCase(String s)
    {
        return (s!=null) ? s.toUpperCase() : null;
    }
    /**
     * <p>Converts a String to upper case using the given <code>Locale</code>
     * as per {@link String#toUpperCase(Locale)}.</p>
     *
     * <p>A <code>null</code> input String returns <code>null</code>.</p>
     *
     * @param s the String to upper case, may be null
     * @param locale the Locale used to change the case
     * @return the upper cased String, <code>null</code> if null String input
     */
    public static String toUpperCase(String s, Locale locale)
    {
        return (s!=null) ? s.toUpperCase(locale) : null;
    }

    /**
     * <p>Replaces the first matching character in the given String, or returns
     * <code>null</code> if the String input is <code>null</code>.</p>
     *
     * @param s the String to process, may be null
     * @param oldChar the character to search for
     * @param newChar the character to replace it with
     * @return the String with the character replaced, <code>null</code> if null String input
     */
    public static String replace(String s, char oldChar, char newChar)
    {
        return s!=null ? s.replace(oldChar, newChar) : null;
    }

    /**
     * <p>Replaces the first substring of the given String that matches the
     * given regular expression with the given replacement, or returns the
     * String unchanged if the regex or the replacement is <code>null</code>.</p>
     *
     * @param s the String to process, may be null
     * @param regex the regular expression to which the String is to be matched, may be null
     * @param replacement the String to be substituted for the first match, may be null
     * @return the String with the first match replaced, <code>null</code> if null String input
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(String s, String regex, String replacement)
    {
        return s!=null ? ( (regex!=null && replacement!=null) ? s.replaceFirst(regex, replacement) : s ) : null;
    }

    /**
     * <p>Replaces every substring of the given String that matches the given
     * regular expression with the given replacement, or returns the String
     * unchanged if the regex or the replacement is <code>null</code>.</p>
     *
     * @param s the String to process, may be null
     * @param regex the regular expression to which the String is to be matched, may be null
     * @param replacement the String to be substituted for each match, may be null
     * @return the String with the matches replaced, <code>null</code> if null String input
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(String s, String regex, String replacement)
    {
        return s!=null ? ( (regex!=null && replacement!=null) ? s.replaceAll(regex, replacement) : s ) : null;
    }

    /**
     * <p>Replaces each substring of the given String that matches the literal
     * target sequence with the literal replacement sequence, or returns the
     * String unchanged if the target or the replacement is <code>null</code>.</p>
     *
     * @param s the String to process, may be null
     * @param target the sequence that is to be replaced, may be null
     * @param replacement the replacement sequence, may be null
     * @return the String with the target replaced, <code>null</code> if null String input
     * @see String#replace(CharSequence, CharSequence)
     */
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
    
    /**
     * <p>Returns the first non-empty ("" and non-<code>null</code>) among the
     * given values, or an empty String ("") if all of them are empty or
     * <code>null</code>.</p>
     *
     * @param first the first candidate, may be null
     * @param others further candidates, may be null
     * @return the first non-empty value or an empty String if none
     */
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
    
    /**
     * <p>Returns the first non-blank (not empty, not whitespace only and not
     * <code>null</code>) among the given values, or an empty String ("") if all
     * of them are blank or <code>null</code>.</p>
     *
     * @param first the first candidate, may be null
     * @param others further candidates, may be null
     * @return the first non-blank value or an empty String if none
     */
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
    
    /**
     * <p>Builds a hex dump of the given String (using its UTF-8 bytes) and
     * returns it as a String.</p>
     *
     * @param s the String to dump, may not be null
     * @return the hex dump
     * @throws IOException if an error occurs while writing
     * @see #dumpHex(Appendable, byte[])
     */
    public static String dumpHex(String s) throws IOException
    {
        return dumpHex(new StringBuilder(), s.getBytes()).toString();
    }
    /**
     * <p>Builds a hex dump of the given bytes and returns it as a String.</p>
     *
     * @param bytes the bytes to dump, may not be null
     * @return the hex dump
     * @throws IOException if an error occurs while writing
     * @see #dumpHex(Appendable, byte[])
     */
    public static String dumpHex(byte[] bytes) throws IOException
    {
        return dumpHex(new StringBuilder(), bytes).toString();
    }
    /**
     * <p>Builds a hex dump of the given String (using its UTF-8 bytes) and
     * appends it to the given {@code Appendable}.</p>
     *
     * @param output the {@code Appendable} to write to, may not be null
     * @param s the String to dump, may not be null
     * @return the given {@code Appendable}
     * @throws IOException if an error occurs while writing
     * @see #dumpHex(Appendable, byte[])
     */
    public static Appendable dumpHex(Appendable output, String s) throws IOException
    {
        return dumpHex(output, s.getBytes());
    }
    /**
     * <p>Appends a hex dump of the given bytes to the given {@code Appendable}.</p>
     *
     * <p>Each line starts with the 8-digit hexadecimal offset of the first byte
     * of the line, followed by up to 16 bytes in hexadecimal and the printable
     * ASCII representation of those bytes, non-printable bytes being replaced
     * by a dot.</p>
     *
     * @param output the {@code Appendable} to write to, may not be null
     * @param bytes the bytes to dump, may not be null
     * @return the given {@code Appendable}
     * @throws IOException if an error occurs while writing
     */
    private static final String HEX = "0123456789ABCDEF";
    public static Appendable dumpHex(Appendable output, byte[] bytes) throws IOException
    {
        int offset = 0;
        StringBuilder line = new StringBuilder(80);
        while (offset < bytes.length)
        {
            line.setLength(0);
            appendHexOffset(line, offset);
            int i;
            for (i = 0; i < 16 && offset + i < bytes.length; i++)
            {
                appendHexByte(line, bytes[offset + i]); // Print the bytes in hexadecimal
            }
            for (; i < 16; i++)
            {
                line.append("   "); // Indentation for incomplete lines
            }
            line.append("  ");
            for (i = 0; i < 16 && offset + i < bytes.length; i++)
            {
                char c = (char) bytes[offset + i]; // Convert the byte to a character
                line.append((c >= 32 && c <= 126) ? c : '.'); // Printable characters, dots otherwise
            }
            output.append(line).append(System.lineSeparator());
            offset += 16; // Move to the next line
        }
        return output;
    }
    private static void appendHexOffset(StringBuilder sb, int offset)
    {
        sb.append(HEX.charAt((offset >>> 28) & 0xF))
          .append(HEX.charAt((offset >>> 24) & 0xF))
          .append(HEX.charAt((offset >>> 20) & 0xF))
          .append(HEX.charAt((offset >>> 16) & 0xF))
          .append(HEX.charAt((offset >>> 12) & 0xF))
          .append(HEX.charAt((offset >>> 8) & 0xF))
          .append(HEX.charAt((offset >>> 4) & 0xF))
          .append(HEX.charAt(offset & 0xF))
          .append(':');
    }
    private static void appendHexByte(StringBuilder sb, byte b)
    {
        sb.append(' ')
          .append(HEX.charAt((b >>> 4) & 0xF))
          .append(HEX.charAt(b & 0xF));
    }
    
    //nos da un resumen y añade una cadena que indica continuidad si se han descartado caracteres
    /**
     * <p>Returns a brief summary of the given String, truncating it so that its
     * length does not exceed <code>max</code> characters.</p>
     *
     * <p>If the String is longer than <code>max</code>, the result ends with
     * the given <code>more</code> String to indicate that characters were
     * discarded.</p>
     *
     * @param s the String to summarize, may not be null
     * @param max the maximum length of the result
     * @param more the String appended to indicate truncation, may be null
     * @return the summarized String
     * @see #brief(String, int, String, int)
     */
    public static String brief(String s, int max, String more)
    {
        return brief(s, max, more, Integer.MAX_VALUE);
    }
    /**
     * <p>Returns a brief summary of the given String, truncating it so that its
     * length does not exceed <code>max</code> characters and not exceed the
     * <code>allowedReturns</code> number of line breaks of the original.</p>
     *
     * <p>If the String has more than <code>allowedReturns</code> line breaks,
     * the truncation is moved back to the last line break before
     * <code>max</code>, when possible. The result ends with the given
     * <code>more</code> String if characters were discarded.</p>
     *
     * @param s the String to summarize, may not be null
     * @param max the maximum length of the result
     * @param more the String appended to indicate truncation, may be null
     * @param allowedReturns the maximum number of line breaks allowed in the result
     * @return the summarized String
     */
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
    /**
     * <p>Pads the given String on the left with the given character up to the
     * given size.</p>
     *
     * <pre>
     * Strings.paddingLeft("ab", 5, ' ') = "   ab"
     * Strings.paddingLeft("abcd", 3, ' ') = "abcd"
     * </pre>
     *
     * @param s the String to pad, may not be null
     * @param size the target size
     * @param c the character to pad with
     * @return the padded String
     */
    public static String paddingLeft(String s, int size, char c)
    {
        int n = Math.max(0, size-s.length());
        if(n>0)
        {
            s = fill(new StringBuilder(),c,n).toString()+s;
        }
        return s;
    }
    /**
     * <p>Pads the given String on the right with the given character up to the
     * given size.</p>
     *
     * <pre>
     * Strings.paddingRight("ab", 5, ' ') = "ab   "
     * Strings.paddingRight("abcd", 3, ' ') = "abcd"
     * </pre>
     *
     * @param s the String to pad, may not be null
     * @param size the target size
     * @param c the character to pad with
     * @return the padded String
     */
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
    /**
     * Returns the normalization pattern for the given Locale, looking up its
     * parent locales if no pattern is registered for it. Falls back to the
     * ASCII pattern if none is found.
     *
     * @param locale the Locale to resolve, may not be null
     * @return the normalization Pattern for the Locale
     */
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
    /**
     * <p>Normalizes the given String replacing its accented and non-ASCII
     * characters with their closest ASCII equivalent using the default
     * Locale.</p>
     *
     * <p>Characters without an ASCII equivalent are removed. A <code>null</code>
     * or empty String is returned unchanged.</p>
     *
     * @param s the String to normalize, may be null
     * @return the normalized String, <code>null</code> if null String input
     */
    public static String normalize(String s)
    {
        return normalize(s, Locale.getDefault());
    }
    /**
     * <p>Normalizes the given String replacing its accented and non-ASCII
     * characters with their closest ASCII equivalent using the given
     * Locale.</p>
     *
     * <p>Characters without an ASCII equivalent are removed. A <code>null</code>
     * or empty String is returned unchanged.</p>
     *
     * @param s the String to normalize, may be null
     * @param locale the Locale used to select the normalization rules
     * @return the normalized String, <code>null</code> if null String input
     */
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
    /**
     * <p>Replaces every U+FFF0-U+FFFF "ugly" Unicode character in the given
     * String with a space.</p>
     *
     * @param s the String to process, may not be null
     * @param group if true consecutive ugly characters are replaced as a group
     * @return the String with the ugly characters replaced
     */
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

    /**
     * <p>Compares two Strings handling <code>null</code>s without exceptions.
     * Two <code>null</code> references are considered equal.</p>
     *
     * @param s1 the first String, may be null
     * @param s2 the second String, may be null
     * @return <code>true</code> if the Strings are equal or both <code>null</code>
     */
    public static boolean equals(String s1, String s2)
    {
        return s1==null ? s2==null : s1.equals(s2);
    }
    /**
     * <p>Compares two Strings ignoring case, handling <code>null</code>s
     * without exceptions. Two <code>null</code> references are considered
     * equal.</p>
     *
     * @param s1 the first String, may be null
     * @param s2 the second String, may be null
     * @return <code>true</code> if the Strings are equal ignoring case or both <code>null</code>
     */
    public static boolean equalsIgnoreCase(String s1, String s2)
    {
        return s1==null ? s2==null : s1.equalsIgnoreCase(s2);
    }

    /**
     * <p>Splits the given String into chunks of at most <code>cols</code>
     * characters, returned as an array of rows.</p>
     *
     * <pre>
     * Strings.split("abcdef", 2) = ["ab", "cd", "ef"]
     * Strings.split("abcde", 2)  = ["ab", "cd", "e"]
     * </pre>
     *
     * @param s the String to split, may not be null
     * @param cols the maximum size of each chunk
     * @return an array of chunks
     */
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
    
    /**
     * <p>Splits the given String into chunks of at most <code>cols</code>
     * characters joined back together with the given separator.</p>
     *
     * <pre>
     * Strings.split("abcdef", 2, "-") = "ab-cd-ef"
     * Strings.split("abcde", 2, "-")  = "ab-cd-e"
     * </pre>
     *
     * @param s the String to split, may not be null
     * @param cols the maximum size of each chunk
     * @param sep the separator used to join the chunks
     * @return the String split and re-joined with the separator
     */
    public static String split(String s, int cols, String sep)
    {
        int n = s.length();
        StringJoiner sj = new StringJoiner(sep);
        for(int i=0;i<n;i+=cols)
        {
            String item = s.substring(i,Math.min(i+cols, n));
            sj.add(item);
        }
        return sj.toString();
    }

    /**
     * <p>Splits the given String into an array of lines, handling the line
     * terminators {@code \n}, {@code \r\n} and {@code \r}.</p>
     *
     * <p>Consecutive line terminators produce empty lines. A trailing line
     * terminator does not add a trailing empty line. A <code>null</code> input
     * String returns <code>null</code>.</p>
     *
     * <pre>
     * Strings.lines(null)            = null
     * Strings.lines("")              = []
     * Strings.lines("abc")           = ["abc"]
     * Strings.lines("a\nb")          = ["a", "b"]
     * Strings.lines("a\nb\nc")       = ["a", "b", "c"]
     * Strings.lines("a\nb\n")        = ["a", "b"]
     * Strings.lines("a\rb")          = ["a", "b"]
     * Strings.lines("a\r\nb")        = ["a", "b"]
     * Strings.lines("a\n\nb")        = ["a", "", "b"]
     * </pre>
     *
     * @param s the String to split into lines, may be null
     * @return an array of lines or <code>null</code> if the String input is null
     */
    public static String[] lines(String s)
    {
        if (s == null)
        {
            return null;
        }
        int len = s.length();
        List<String> list = new ArrayList<>();
        int start = 0;
        int i = 0;
        while (i < len)
        {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r')
            {
                list.add(s.substring(start, i));
                if (c == '\r' && i + 1 < len && s.charAt(i + 1) == '\n')
                {
                    i++;
                }
                start = i + 1;
            }
            i++;
        }
        if (start < len)
        {
            list.add(s.substring(start));
        }
        return list.toArray(new String[list.size()]);
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

    /**
     * <p>Compares two Strings lexicographically handling <code>null</code>s
     * without exceptions. A <code>null</code> String is considered smaller than
     * any non-<code>null</code> String, and two <code>null</code> references
     * are considered equal.</p>
     *
     * @param a the first String, may be null
     * @param b the second String, may be null
     * @return a negative integer, zero or a positive integer as the first
     *  String is less than, equal to, or greater than the second one
     */
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
    /**
     * <p>Compares two String arrays element by element lexicographically,
     * handling <code>null</code>s without exceptions.</p>
     *
     * <p>A shorter array is considered smaller if all compared elements are
     * equal.</p>
     *
     * @param a the first array, may not be null
     * @param b the second array, may not be null
     * @return a negative integer, zero or a positive integer as the first
     *  array is less than, equal to, or greater than the second one
     * @see #compareTo(String, String)
     */
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
    /**
     * <p>Compares two Strings lexicographically ignoring case, handling
     * <code>null</code>s without exceptions. A <code>null</code> String is
     * considered smaller than any non-<code>null</code> String, and two
     * <code>null</code> references are considered equal.</p>
     *
     * @param a the first String, may be null
     * @param b the second String, may be null
     * @return a negative integer, zero or a positive integer as the first
     *  String is less than, equal to, or greater than the second one ignoring case
     */
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
    /**
     * <p>Compares two String arrays element by element lexicographically
     * ignoring case, handling <code>null</code>s without exceptions.</p>
     *
     * <p>A shorter array is considered smaller if all compared elements are
     * equal.</p>
     *
     * @param a the first array, may not be null
     * @param b the second array, may not be null
     * @return a negative integer, zero or a positive integer as the first
     *  array is less than, equal to, or greater than the second one ignoring case
     * @see #compareToIgnoreCase(String, String)
     */
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

    /**
     * <p>Joins the elements of the given {@code Collection} into a
     * comma-separated String, without prefix or suffix. A <code>null</code>
     * element is replaced by the given <code>nullValue</code>.</p>
     *
     * @param <T> the type of elements in the collection
     * @param list the values to join, may not be null
     * @param nullValue the value to use for {@code null} elements, may be null
     * @return the comma-separated String
     */
    public static <T> String csv(Collection<T> list, String nullValue)
    {
        return join(",", "", "", list, nullValue);
    }
    /**
     * <p>Joins the elements of the given {@code Collection} into a
     * comma-separated String, without prefix or suffix.</p>
     *
     * @param <T> the type of elements in the collection
     * @param list the values to join, may not be null
     * @return the comma-separated String
     */
    public static <T> String csv(Collection<T> list)
    {
        return join(",", "", "", list, null);
    }
    /**
     * <p>Joins the elements of the given array into a comma-separated String,
     * without prefix or suffix. A <code>null</code> element is replaced by the
     * given <code>nullValue</code>.</p>
     *
     * @param <T> the type of elements in the array
     * @param list the values to join, may not be null
     * @param nullValue the value to use for {@code null} elements, may be null
     * @return the comma-separated String
     */
    public static <T> String csv(T[] list, String nullValue)
    {
        return join(",", "", "", Arrays.asList(list), nullValue);
    }
    /**
     * <p>Joins the elements of the given array into a comma-separated String,
     * without prefix or suffix.</p>
     *
     * @param <T> the type of elements in the array
     * @param list the values to join, may not be null
     * @return the comma-separated String
     */
    public static <T> String csv(T[] list)
    {
        return join(",", "", "", Arrays.asList(list), null);
    }

    /**
     * <p>Returns the number of Unicode code points in the given String.</p>
     *
     * @param s the String to count, may not be null
     * @return the number of code points in the String
     */
    public static int codePointCount(String s)
    {
        return s.codePointCount(0, s.length());
    }
    /**
     * <p>Returns an array of the Unicode code points of the given String.</p>
     *
     * @param s the String to convert, may be null
     * @return an array of code points, or <code>null</code> if the String input is null
     */
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
    /**
     * <p>Capitalizes each word of the given String, using the space character
     * as the only delimiter.</p>
     *
     * <p>A <code>null</code> or empty String is returned unchanged.</p>
     *
     * <pre>
     * Strings.capitalize("i am fine") = "I Am Fine"
     * </pre>
     *
     * @param s the String to capitalize, may be null
     * @return the capitalized String, <code>null</code> if null String input
     * @see #capitalize(String, char...)
     */
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
    /**
     * <p>Capitalizes each word of the given String, using the given characters
     * as delimiters. If no delimiter is provided, whitespace is used.</p>
     *
     * <p>A <code>null</code>, empty String or an empty delimiter array returns
     * the String unchanged.</p>
     *
     * <pre>
     * Strings.capitalize("i aM.fine", '.') = "I aM.Fine"
     * </pre>
     *
     * @param s the String to capitalize, may be null
     * @param delimiters the delimiter characters used to separate words,
     *  whitespace is used if none is provided
     * @return the capitalized String, <code>null</code> if null String input
     */
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

    /**
     * <p>Returns a new String containing only the first occurrence of each
     * Unicode code point in the original String, preserving order.</p>
     *
     * @param s the String to process, may not be null
     * @return a String with only unique code points
     * @see #uniqueCodepointCount(String)
     */
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
    /**
     * <p>Returns the number of unique Unicode code points in the given String.</p>
     *
     * @param s the String to analyze, may not be null
     * @return the number of unique code points
     * @see #uniqueCodepoints(String)
     */
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
    
    /**
     * <p>Merges two multi-line Strings side by side, aligning them to the
     * given number of columns.</p>
     *
     * <p>If <code>cols</code> is positive the left column is padded with
     * spaces before joining. If <code>cols</code> is zero the columns are
     * just concatenated. If <code>cols</code> is negative the shorter left
     * rows are replaced by the right rows at the given column offset.</p>
     *
     * @param left the left block of text, may not be null
     * @param right the right block of text, may not be null
     * @param cols the column alignment, or 0 for no padding
     * @return the merged text
     * @see #mergeRows(int, String...)
     */
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
    /**
     * <p>Merges the given multi-line Strings side by side using
     * {@link #mergeRows(String, String, int)}.</p>
     *
     * @param cols the column alignment passed to each merge, or 0 for no padding
     * @param rows the blocks of text to merge, may not be null
     * @return the merged text
     */
    public static String mergeRows(int cols, String... rows)
    {
        String s = rows.length>0 ? rows[0] : "";
        for(int i=1;i<rows.length;i++)
        {
            s = mergeRows(s, rows[i], cols);
        }
        return s;
    }
    /**
     * <p>Returns the size of the longest suffix of <code>start</code> that
     * matches a prefix of <code>end</code>.</p>
     *
     * <p>If either String is <code>null</code> or empty, returns 0.</p>
     *
     * @param start the String whose suffix is compared, may be null
     * @param end the String whose prefix is compared, may be null
     * @return the number of overlapping characters, possibly zero
     * @see #overlap(String...)
     */
    public static int overlapped(String start, String end)
    {
        return overlapped((CharSequence)start, end);
    }

    /**
     * <p>Returns the size of the longest suffix of <code>start</code> that
     * matches a prefix of <code>end</code>.</p>
     *
     * <p>If either sequence is <code>null</code> or empty, returns 0.</p>
     *
     * @param start the sequence whose suffix is compared, may be null
     * @param end the sequence whose prefix is compared, may be null
     * @return the number of overlapping characters, possibly zero
     * @see #overlap(String...)
     */
    public static int overlapped(CharSequence start, CharSequence end)
    {
        if(start==null||end==null||start.length()==0||end.length()==0)
        {
            return 0;
        }
        int sl = start.length();
        int el = end.length();
        int at = Math.max(0, sl-el);
        int count = 0;
        for(int i=at;i<sl && count==0;i++)
        {
            for(int j=i;j<sl;j++,count++)
            {
                if(start.charAt(j)!=end.charAt(count))
                {
                    count = 0;
                    break;
                }
            }
        }
        return count;
    }
    /**
     * <p>Concatenates the given Strings overlapping the suffix of each String
     * with the prefix of the next one when possible.</p>
     *
     * <p>For example, concatenating "abc" and "bcd" produces "abcd".</p>
     *
     * @param s the Strings to overlap and concatenate, may not be null
     * @return the concatenated String with the overlaps removed
     * @see #overlapped(String, String)
     */
    public static String overlap(String... s)
    {
        StringBuilder full = new StringBuilder();
        
        for(String item : s)
        {
            int count = overlapped(full, item);
            full.append((count==0) ? item : item.substring(count));
        }
        return full.toString();
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
    
    
    /**
     * <p>Splits the given delimiter-separated values into a {@code HashSet}
     * of Strings.</p>
     *
     * <p>The values are split using the given {@code sep} regular expression,
     * so a {@code null} element is not produced; the resulting set contains all
     * the resulting tokens.</p>
     *
     * @param values the delimiter-separated values, may not be null
     * @param sep the regular expression used to split the values
     * @return a {@code HashSet} with the split tokens
     */
    public static HashSet<String> delimiterSeparatedValuesToSetString(String values, String sep)
    {
        HashSet<String> set = new HashSet<>();
        set.addAll(Arrays.asList(values.split(sep)));
        return set;
    }
    /**
     * <p>Splits the given comma-separated values into a {@code HashSet} of
     * Strings. If <code>trim</code> is <code>true</code> the tokens are trimmed
     * of surrounding spaces.</p>
     *
     * @param values the comma-separated values, may not be null
     * @param trim if true the resulting tokens are trimmed of spaces
     * @return a {@code HashSet} with the split tokens
     */
    public static HashSet<String> commaSeparatedValuesToSetString(String values, boolean trim)
    {
        return delimiterSeparatedValuesToSetString(values, trim?" *, *":",");
    }
    /**
     * <p>Splits the given comma-separated values into a {@code HashSet} of
     * Strings.</p>
     *
     * @param values the comma-separated values, may not be null
     * @return a {@code HashSet} with the split tokens
     */
    public static HashSet<String> commaSeparatedValuesToSetString(String values)
    {
        return commaSeparatedValuesToSetString(values, false);
    }
    


    /**
     * <p>Collects the Unicode code points of the given String that are not in
     * the given exclude String, returning them as a new String.</p>
     *
     * <p>The code points in <code>exclude</code> are skipped; the remaining
     * code points of <code>s</code> are returned in their original order.</p>
     *
     * @param s the String whose code points are collected, may be null
     * @param exclude the code points to skip, may be null
     * @return a String with the collected code points, empty String if s is null
     */
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
    
    /**
     * <p>Skipping the given number of characters from the beginning of the
     * given String.</p>
     *
     * <p>If <code>n</code> is zero the String is returned unchanged; if
     * <code>n</code> is greater than or equal to the String length, an empty
     * String is returned. A <code>null</code> String returns <code>null</code>.</p>
     *
     * @param s the String to skip characters from, may be null
     * @param n the number of characters to skip
     * @return the String without the first <code>n</code> characters, <code>null</code> if null String input
     */
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

    /**
     * <p>Checks if String contains a search String irrespective of case,
     * handling <code>null</code>. This method uses
     * {@link #contains(String, String)}.</p>
     *
     * <p>A <code>null</code> String will return <code>false</code>.</p>
     *
     * <pre>
     * StringUtils.contains(null, *) = false
     * StringUtils.contains(*, null) = false
     * StringUtils.contains("", "") = true
     * StringUtils.contains("abc", "") = true
     * StringUtils.contains("abc", "a") = true
     * StringUtils.contains("abc", "z") = false
     * StringUtils.contains("abc", "A") = true
     * StringUtils.contains("abc", "Z") = false
     * </pre>
     *
     * @param str  the String to check, may be null
     * @param searchStr  the String to find, may be null
     * @return true if the String contains the search String irrespective of
     * case or false if not or <code>null</code> string input
     */
    public static boolean containsIgnoreCase(String str, String searchStr)
    {
        if (str == null || searchStr == null)
        {
            return false;
        }
        return contains(str.toUpperCase(), searchStr.toUpperCase());
    }

    /**
     * <p>Checks if String contains a search String, handling <code>null</code>.
     * This method uses {@link String#indexOf(String)}.</p>
     *
     * <p>A <code>null</code> String will return <code>false</code>.</p>
     *
     * <pre>
     * StringUtils.contains(null, *)     = false
     * StringUtils.contains(*, null)     = false
     * StringUtils.contains("", "")      = true
     * StringUtils.contains("abc", "")   = true
     * StringUtils.contains("abc", "a")  = true
     * StringUtils.contains("abc", "z")  = false
     * </pre>
     *
     * @param str  the String to check, may be null
     * @param searchStr  the String to find, may be null
     * @return true if the String contains the search String,
     *  false if not or <code>null</code> string input
     * @since 2.0
     */
    public static boolean contains(String str, String searchStr)
    {
        if (str == null || searchStr == null)
        {
            return false;
        }
        return str.contains(searchStr);
    }

    /**
     * <p>Checks if the String is an isogram (case-insensitive).</p>
     * <p>An isogram is a word or phrase without repeating letters.
     * Non-alphabetic characters (like spaces and hyphens) are ignored.</p>
     * <p>Supports full Unicode letters (e.g. Spanish 'ñ', Cyrillic, accented characters)
     * and surrogate pairs.</p>
     *
     * <pre>
     * Strings.isIsogram(null)      = false
     * Strings.isIsogram("")        = true
     * Strings.isIsogram("isogram") = true
     * Strings.isIsogram("eleven")  = false
     * Strings.isIsogram("ñandú")   = true
     * Strings.isIsogram("ñandú-ñ") = false
     * </pre>
     *
     * @param s the String to check, may be null
     * @return true if the String is an isogram, false otherwise
     */
    public static boolean isIsogram(String s)
    {
        if (s == null)
        {
            return false;
        }
        int len = s.length();
        if (len <= 1)
        {
            return true;
        }
        
        int asciiBitmask = 0;
        Set<Integer> unicodeSet = null;
        
        for (int i = 0; i < len; )
        {
            int cp = s.codePointAt(i);
            if (Character.isLetter(cp))
            {
                int lower = Character.toLowerCase(cp);
                if (lower >= 'a' && lower <= 'z')
                {
                    int bit = 1 << (lower - 'a');
                    if (unicodeSet != null)
                    {
                        if (!unicodeSet.add(lower))
                        {
                            return false;
                        }
                    }
                    else
                    {
                        if ((asciiBitmask & bit) != 0)
                        {
                            return false;
                        }
                        asciiBitmask |= bit;
                    }
                }
                else
                {
                    if (unicodeSet == null)
                    {
                        unicodeSet = new HashSet<>();
                        for (int b = 0; b < 26; b++)
                        {
                            if ((asciiBitmask & (1 << b)) != 0)
                            {
                                unicodeSet.add('a' + b);
                            }
                        }
                    }
                    if (!unicodeSet.add(lower))
                    {
                        return false;
                    }
                }
            }
            i += Character.charCount(cp);
        }
        return true;
    }
}

