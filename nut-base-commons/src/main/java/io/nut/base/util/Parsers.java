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
 *
 * @author franci
 */
public class Parsers
{
    
    private static final Pattern SAFE_INT_PATTERN = Pattern.compile("[0-9]+");
    public static int safeParseInt(String s, int defaultValue)
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
    public static int safeParseInt(String s)
    {
        return safeParseInt(s, 0);
    }
    
    public static long safeParseLong(String s, long defaultValue, int radix)
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
    
    public static long safeParseLong(String s, long defaultValue)
    {
        return safeParseLong(s, defaultValue, 10);
    }
    
    public static long safeParseLong(String s)
    {
        return safeParseLong(s, 0L, 10);
    }

    public static float safeParseFloat(String s, float defaultValue)
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
    
    public static float safeParseFloat(String s)
    {
        return safeParseFloat(s, 0.0f);
    }
    
    public static double safeParseDouble(String s, double defaultValue)
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
    
    public static double safeParseDouble(String s)
    {
        return safeParseDouble(s, 0.0);
    }
    
    public static BigInteger safeParseBigInteger(String s, BigInteger defaultValue, int radix)
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
    
    public static BigInteger safeParseBigInteger(String s, BigInteger defaultValue)
    {
        return safeParseBigInteger(s, defaultValue, 10);
    }
    
    public static BigInteger safeParseBigInteger(String s)
    {
        return safeParseBigInteger(s, BigInteger.ZERO, 10);
    }

    public static BigDecimal safeParseBigDecimal(String s, BigDecimal defaultValue)
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
    public static BigDecimal safeParseBigDecimal(String s)
    {
        return safeParseBigDecimal(s, BigDecimal.ZERO);
    }

    
    public static boolean isBigInteger(String s)
    {
        return isBigInteger(s, 10);
    }
    public static boolean isBigInteger(String s, int radix)
    {
        return s!=null && !s.isEmpty() && safeParseBigInteger(s, null, radix)!=null;
    }

    public static boolean isBigDecimal(String s)
    {
        return s!=null && !s.isEmpty() && safeParseBigDecimal(s, null)!=null;
    }

}
