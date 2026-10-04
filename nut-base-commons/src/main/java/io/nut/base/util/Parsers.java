/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import io.nut.base.lang.Strings;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utility class for parsing strings to various numeric types with safe defaults.
 * <p>
 * This class provides methods to parse strings to primitives and BigNumber types.
 * All parsing methods are null-safe and return default values if parsing fails
 * or if the input is null or empty.
 * </p>
 *
 * @author franci
 */
public class Parsers
{
    
    private static final Pattern SAFE_INT_PATTERN = Pattern.compile("[0-9]+");
    
    /**
     * Parses a string to an integer, returning a default value if parsing fails.
     * <p>
     * Only strings consisting of digits are considered safe for parsing.
     * </p>
     *
     * @param s the string to parse
     * @param defaultValue the value to return if parsing fails or input is null/empty
     * @return the parsed integer, or {@code defaultValue} if parsing fails
     */
    public static int parseInt(String s, int defaultValue)
    {
        try
        {
            if(s!=null && (s=s.trim()).length()>0)
            {
                if(SAFE_INT_PATTERN.matcher(s).matches())
                {
                    return Integer.parseInt(s);
                }
            }
        }
        catch(NumberFormatException ex)
        {
            Logger.getLogger(Strings.class.getName()).log(Level.CONFIG,"can't parse {0} as int",s);
        }
        return defaultValue;
    }
    
    /**
     * Parses a string to an integer, returning {@code 0} if parsing fails.
     *
     * @param s the string to parse
     * @return the parsed integer, or {@code 0} if parsing fails or input is null/empty
     */
    public static int parseInt(String s)
    {
        return Parsers.parseInt(s, 0);
    }
    
    /**
     * Parses a string to a long using the specified radix, returning a default value if parsing fails.
     * <p>
     * For radix 10, only strings consisting of digits are considered safe.
     * </p>
     *
     * @param s the string to parse
     * @param defaultValue the value to return if parsing fails or input is null/empty
     * @param radix the radix to use for parsing
     * @return the parsed long, or {@code defaultValue} if parsing fails
     */
    public static long parseLong(String s, long defaultValue, int radix)
    {
        try
        {
            if(s!=null && (s=s.trim()).length()>0)
            {
                if(radix!=10 || SAFE_INT_PATTERN.matcher(s).matches())
                {
                    return Long.parseLong(s, radix);
                }
            }
        }
        catch(NumberFormatException ex)
        {
            Logger.getLogger(Strings.class.getName()).log(Level.CONFIG,"can't parse {0} as long",s);
        }
        return defaultValue;
    }
    
    /**
     * Parses a string to a long with radix 10, returning a default value if parsing fails.
     *
     * @param s the string to parse
     * @param defaultValue the value to return if parsing fails or input is null/empty
     * @return the parsed long, or {@code defaultValue} if parsing fails
     */
    public static long parseLong(String s, long defaultValue)
    {
        return Parsers.parseLong(s, defaultValue, 10);
    }
    
    /**
     * Parses a string to a long with radix 10, returning {@code 0L} if parsing fails.
     *
     * @param s the string to parse
     * @return the parsed long, or {@code 0L} if parsing fails or input is null/empty
     */
    public static long parseLong(String s)
    {
        return Parsers.parseLong(s, 0L, 10);
    }

    /**
     * Parses a string to a float, returning a default value if parsing fails.
     *
     * @param s the string to parse
     * @param defaultValue the value to return if parsing fails or input is null/empty
     * @return the parsed float, or {@code defaultValue} if parsing fails
     */
    public static float parseFloat(String s, float defaultValue)
    {
        try
        {
            if(s!=null && (s=s.trim()).length()>0)
            {
                return Float.parseFloat(s);
            }
        }
        catch(NumberFormatException ex)
        {
            Logger.getLogger(Strings.class.getName()).log(Level.FINE,"can't parse {0} as float",s);
        }
        return defaultValue;
    }
    
    /**
     * Parses a string to a float, returning {@code 0.0f} if parsing fails.
     *
     * @param s the string to parse
     * @return the parsed float, or {@code 0.0f} if parsing fails or input is null/empty
     */
    public static float parseFloat(String s)
    {
        return Parsers.parseFloat(s, 0.0f);
    }
    
    /**
     * Parses a string to a double, returning a default value if parsing fails.
     *
     * @param s the string to parse
     * @param defaultValue the value to return if parsing fails or input is null/empty
     * @return the parsed double, or {@code defaultValue} if parsing fails
     */
    public static double parseDouble(String s, double defaultValue)
    {
        try
        {
            if(s!=null && (s=s.trim()).length()>0)
            {
                return Double.parseDouble(s);
            }
        }
        catch(NumberFormatException ex)
        {
            Logger.getLogger(Strings.class.getName()).log(Level.FINE,"can't parse {0} as double",s);
        }
        return defaultValue;
    }
    
    /**
     * Safely parses a string to a double, returning {@code 0.0} if parsing fails.
     *
     * @param s the string to parse
     * @return the parsed double, or {@code 0.0} if parsing fails or input is null/empty
     */
    public static double safeParseDouble(String s)
    {
        return parseDouble(s, 0.0);
    }
    
    /**
     * Parses a string to a {@link BigInteger} using the specified radix, returning a default value if parsing fails.
     * <p>
     * For radix 10, only strings consisting of digits are considered safe and parsed optimistically.
     * </p>
     *
     * @param s the string to parse
     * @param defaultValue the value to return if parsing fails or input is null/empty
     * @param radix the radix to use for parsing
     * @return the parsed {@link BigInteger}, or {@code defaultValue} if parsing fails
     */
    public static BigInteger parseBigInteger(String s, BigInteger defaultValue, int radix)
    {
        try
        {
            if(s!=null && (s=s.trim()).length()>0)
            {
                if(radix==10 && SAFE_INT_PATTERN.matcher(s).matches())
                {
                    return new BigInteger(s);
                }
                return new BigInteger(s, radix);
            }
        }
        catch(Exception ex)
        {
            Logger.getLogger(Strings.class.getName()).log(Level.CONFIG,"can't parse {0} as BigInteger",s);
        }
        return defaultValue;
    }
    
    /**
     * Parses a string to a {@link BigInteger} with radix 10, returning a default value if parsing fails.
     *
     * @param s the string to parse
     * @param defaultValue the value to return if parsing fails or input is null/empty
     * @return the parsed {@link BigInteger}, or {@code defaultValue} if parsing fails
     */
    public static BigInteger parseBigInteger(String s, BigInteger defaultValue)
    {
        return Parsers.parseBigInteger(s, defaultValue, 10);
    }
    
    /**
     * Parses a string to a {@link BigInteger} with radix 10, returning {@link BigInteger#ZERO} if parsing fails.
     *
     * @param s the string to parse
     * @return the parsed {@link BigInteger}, or {@link BigInteger#ZERO} if parsing fails or input is null/empty
     */
    public static BigInteger parseBigInteger(String s)
    {
        return Parsers.parseBigInteger(s, BigInteger.ZERO, 10);
    }

    /**
     * Parses a string to a {@link BigDecimal}, returning a default value if parsing fails.
     *
     * @param s the string to parse
     * @param defaultValue the value to return if parsing fails or input is null/empty
     * @return the parsed {@link BigDecimal}, or {@code defaultValue} if parsing fails
     */
    public static BigDecimal parseBigDecimal(String s, BigDecimal defaultValue)
    {
        try
        {
            if(s!=null && (s=s.trim()).length()>0)
            {
                return new BigDecimal(s);
            }
        }
        catch(Exception ex)
        {
            Logger.getLogger(Strings.class.getName()).log(Level.CONFIG,"can't parse {0} as BigDecimal",s);
        }
        return defaultValue;
    }
    
    /**
     * Parses a string to a {@link BigDecimal}, returning {@link BigDecimal#ZERO} if parsing fails.
     *
     * @param s the string to parse
     * @return the parsed {@link BigDecimal}, or {@link BigDecimal#ZERO} if parsing fails or input is null/empty
     */
    public static BigDecimal parseBigDecimal(String s)
    {
        return Parsers.parseBigDecimal(s, BigDecimal.ZERO);
    }
    
    /**
     * Checks if the given string can be parsed as a {@link BigInteger} with radix 10.
     *
     * @param s the string to check
     * @return {@code true} if the string is a valid {@link BigInteger}, {@code false} otherwise
     */
    public static boolean isBigInteger(String s)
    {
        return isBigInteger(s, 10);
    }
    
    /**
     * Checks if the given string can be parsed as a {@link BigInteger} with the specified radix.
     *
     * @param s the string to check
     * @param radix the radix to use for parsing
     * @return {@code true} if the string is a valid {@link BigInteger}, {@code false} otherwise
     */
    public static boolean isBigInteger(String s, int radix)
    {
        return s!=null && !s.isEmpty() && Parsers.parseBigInteger(s, null, radix)!=null;
    }

    /**
     * Checks if the given string can be parsed as a {@link BigDecimal}.
     *
     * @param s the string to check
     * @return {@code true} if the string is a valid {@link BigDecimal}, {@code false} otherwise
     */
    public static boolean isBigDecimal(String s)
    {
        return s!=null && !s.isEmpty() && Parsers.parseBigDecimal(s, null)!=null;
    }

}
