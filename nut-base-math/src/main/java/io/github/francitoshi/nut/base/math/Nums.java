/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Utility class providing mathematical constants and methods for numeric operations.
 *
 * @author franci
 */
public class Nums
{
    /** BigInteger constant for 100 (hecto). */
    public static final BigInteger BIG_INT_HECTO = BigInteger.valueOf(100);

    /** BigInteger constant for 1,000 (kilo). */
    public static final BigInteger BIG_INT_KILO = BigInteger.valueOf(1000);
    /** BigInteger constant for 1,000,000 (mega). */
    public static final BigInteger BIG_INT_MEGA = BigInteger.valueOf(1000_000);	
    /** BigInteger constant for 1,000,000,000 (giga). */
    public static final BigInteger BIG_INT_GIGA = BigInteger.valueOf(1000_000_000);
    /** BigInteger constant for 10^12 (tera). */
    public static final BigInteger BIG_INT_TERA = BigInteger.valueOf(1000_000_000_000L);
    /** BigInteger constant for 10^15 (peta). */
    public static final BigInteger BIG_INT_PETA = BigInteger.valueOf(1000_000_000_000_000L);
    /** BigInteger constant for 10^18 (exa). */
    public static final BigInteger BIG_INT_EXA  = BigInteger.valueOf(1000_000_000_000_000_000L);
    /** BigInteger constant for 10^21 (zetta). */
    public static final BigInteger BIG_INT_ZETTA = BIG_INT_EXA.multiply(BIG_INT_KILO);
    /** BigInteger constant for 10^24 (yotta). */
    public static final BigInteger BIG_INT_YOTTA = BIG_INT_ZETTA.multiply(BIG_INT_KILO);	

    /** BigInteger constant for 2^10 = 1,024 (kibi). */
    public static final BigInteger BIG_INT_KIBI = BigInteger.valueOf(1024);
    /** BigInteger constant for 2^20 = 1,048,576 (mebi). */
    public static final BigInteger BIG_INT_MEBI = BigInteger.valueOf(1024*1024);
    /** BigInteger constant for 2^30 (gibi). */
    public static final BigInteger BIG_INT_GIBI = BigInteger.valueOf(1024*1024*1024);
    /** BigInteger constant for 2^40 (tebi). */
    public static final BigInteger BIG_INT_TEBI = BigInteger.valueOf(1024L*1024L*1024L*1024L);
    /** BigInteger constant for 2^50 (pebi). */
    public static final BigInteger BIG_INT_PEBI = BigInteger.valueOf(1024L*1024L*1024L*1024L*1024L);
    /** BigInteger constant for 2^60 (exbi). */
    public static final BigInteger BIG_INT_EXBI = BigInteger.valueOf(1024L*1024L*1024L*1024L*1024L*1024L);
    /** BigInteger constant for 2^70 (zebi). */
    public static final BigInteger BIG_INT_ZEBI = BIG_INT_EXBI.multiply(BIG_INT_KIBI);
    /** BigInteger constant for 2^80 (yobi). */
    public static final BigInteger BIG_INT_YOBI = BIG_INT_ZEBI.multiply(BIG_INT_KIBI);

    /** BigInteger constant for 2. */
    public static final BigInteger BIG_INT_TWO = BigInteger.valueOf(2);
    /** Alias for {@link #BIG_INT_HECTO}. */
    public static final BigInteger HUNDRED = BIG_INT_HECTO;
    /** Alias for {@link #BIG_INT_KILO}. */
    public static final BigInteger THOUSAND = BIG_INT_KILO;

    /** BigDecimal constant for 10^-1 (deci). */
    public static final BigDecimal BIG_DEC_DECI  = new BigDecimal("0.1");
    /** BigDecimal constant for 10^-2 (centi). */
    public static final BigDecimal BIG_DEC_CENTI = new BigDecimal("0.01");	
    /** BigDecimal constant for 10^-3 (milli). */
    public static final BigDecimal BIG_DEC_MILI  = new BigDecimal("0.001");
    /** BigDecimal constant for 10^-6 (micro). */
    public static final BigDecimal BIG_DEC_MICRO = new BigDecimal("0.000001");
    /** BigDecimal constant for 10^-9 (nano). */
    public static final BigDecimal BIG_DEC_NANO  = new BigDecimal("0.000000001");
    /** BigDecimal constant for 10^-12 (pico). */
    public static final BigDecimal BIG_DEC_PICO  = new BigDecimal("0.000000000001");
    /** BigDecimal constant for 10^-15 (femto). */
    public static final BigDecimal BIG_DEC_FEMTO = new BigDecimal("0.000000000000001");
    /** BigDecimal constant for 10^-18 (atto). */
    public static final BigDecimal BIG_DEC_ATTO  = new BigDecimal("0.000000000000000001");
    /** BigDecimal constant for 10^-21 (zepto). */
    public static final BigDecimal BIG_DEC_ZEPTO = new BigDecimal("0.000000000000000000001");
    /** BigDecimal constant for 10^-24 (yocto). */
    public static final BigDecimal BIG_DEC_YOCTO = new BigDecimal("0.000000000000000000000001");

    /** BigDecimal constant for 100 (hecto). */
    public static final BigDecimal BIG_DEC_HECTO = new BigDecimal(100);

    /** BigDecimal constant for 1,000 (kilo). */
    public static final BigDecimal BIG_DEC_KILO = new BigDecimal(1000);
    /** BigDecimal constant for 1,000,000 (mega). */
    public static final BigDecimal BIG_DEC_MEGA = new BigDecimal(1000_000);	
    /** BigDecimal constant for 1,000,000,000 (giga). */
    public static final BigDecimal BIG_DEC_GIGA = new BigDecimal(1000_000_000);
    /** BigDecimal constant for 10^12 (tera). */
    public static final BigDecimal BIG_DEC_TERA = new BigDecimal(1000_000_000_000L);
    /** BigDecimal constant for 10^15 (peta). */
    public static final BigDecimal BIG_DEC_PETA = new BigDecimal(1000_000_000_000_000L);
    /** BigDecimal constant for 10^18 (exa). */
    public static final BigDecimal BIG_DEC_EXA  = new BigDecimal(1000_000_000_000_000_000L);
    /** BigDecimal constant for 10^21 (zetta). */
    public static final BigDecimal BIG_DEC_ZETTA = BIG_DEC_EXA.multiply(BIG_DEC_KILO);
    /** BigDecimal constant for 10^24 (yotta). */
    public static final BigDecimal BIG_DEC_YOTTA = BIG_DEC_ZETTA.multiply(BIG_DEC_KILO);	

    /** BigDecimal constant for 2^10 = 1,024 (kibi). */
    public static final BigDecimal BIG_DEC_KIBI = new BigDecimal(1024);
    /** BigDecimal constant for 2^20 = 1,048,576 (mebi). */
    public static final BigDecimal BIG_DEC_MEBI = new BigDecimal(1024*1024);
    /** BigDecimal constant for 2^30 (gibi). */
    public static final BigDecimal BIG_DEC_GIBI = new BigDecimal(1024*1024*1024);
    /** BigDecimal constant for 2^40 (tebi). */
    public static final BigDecimal BIG_DEC_TEBI = new BigDecimal(1024L*1024L*1024L*1024L);
    /** BigDecimal constant for 2^50 (pebi). */
    public static final BigDecimal BIG_DEC_PEBI = new BigDecimal(1024L*1024L*1024L*1024L*1024L);
    /** BigDecimal constant for 2^60 (exbi). */
    public static final BigDecimal BIG_DEC_EXBI = new BigDecimal(1024L*1024L*1024L*1024L*1024L*1024L);
    /** BigDecimal constant for 2^70 (zebi). */
    public static final BigDecimal BIG_DEC_ZEBI = BIG_DEC_EXBI.multiply(BIG_DEC_KIBI);
    /** BigDecimal constant for 2^80 (yobi). */
    public static final BigDecimal BIG_DEC_YOBI = BIG_DEC_ZEBI.multiply(BIG_DEC_KIBI);

    /** BigDecimal constant for 0.5. */
    public static final BigDecimal BIG_DEC_HALF = new BigDecimal("0.5");
    /** BigDecimal constant for 2. */
    public static final BigDecimal BIG_DEC_TWO = BigDecimal.valueOf(2);
    /** BigDecimal constant for 4. */
    public static final BigDecimal BIG_DEC_FOUR = BigDecimal.valueOf(4);
    /** Alias for {@link #BIG_DEC_HECTO}. */
    public static final BigDecimal BIG_DEC_HUNDRED = BIG_DEC_HECTO;
    /** Alias for {@link #BIG_DEC_KILO}. */
    public static final BigDecimal BIG_DEC_THOUSAND = BIG_DEC_KILO;

    /** BigDecimal constant for the golden ratio (phi). */
    public static final BigDecimal BIG_DEC_GOLDEN_RATIO = new BigDecimal("1.618033988749894848204586834365638117720309179805762862135");
    
    /** MathContext with 2-digit precision using HALF_DOWN rounding. */
    public static final MathContext MC2HD = new MathContext(2, RoundingMode.HALF_DOWN);
    /** MathContext with 2-digit precision using HALF_EVEN rounding. */
    public static final MathContext MC2HE = new MathContext(2, RoundingMode.HALF_EVEN);
    /** MathContext with 2-digit precision using HALF_UP rounding. */
    public static final MathContext MC2HU = new MathContext(2, RoundingMode.HALF_UP);

    /** MathContext with 4-digit precision using HALF_DOWN rounding. */
    public static final MathContext MC4HD = new MathContext(4, RoundingMode.HALF_DOWN);
    /** MathContext with 4-digit precision using HALF_EVEN rounding. */
    public static final MathContext MC4HE = new MathContext(4, RoundingMode.HALF_EVEN);
    /** MathContext with 4-digit precision using HALF_UP rounding. */
    public static final MathContext MC4HU = new MathContext(4, RoundingMode.HALF_UP);

    /** MathContext with 8-digit precision using HALF_DOWN rounding. */
    public static final MathContext MC8HD = new MathContext(8, RoundingMode.HALF_DOWN);
    /** MathContext with 8-digit precision using HALF_EVEN rounding. */
    public static final MathContext MC8HE = new MathContext(8, RoundingMode.HALF_EVEN);
    /** MathContext with 8-digit precision using HALF_UP rounding. */
    public static final MathContext MC8HU = new MathContext(8, RoundingMode.HALF_UP);

    /** MathContext with 16-digit precision using HALF_DOWN rounding. */
    public static final MathContext MC16HD = new MathContext(16, RoundingMode.HALF_DOWN);
    /** MathContext with 16-digit precision using HALF_EVEN rounding. */
    public static final MathContext MC16HE = new MathContext(16, RoundingMode.HALF_EVEN);
    /** MathContext with 16-digit precision using HALF_UP rounding. */
    public static final MathContext MC16HU = new MathContext(16, RoundingMode.HALF_UP);
    
    /**
     * Computes Euler's number (e) to the specified precision.
     *
     * @param setPrecision the desired number of decimal digits of precision.
     * @return e computed to the given precision.
     */
    public static BigDecimal buildE(int setPrecision)
    {
        BigDecimal f = BigDecimal.ONE;
        BigDecimal e = new BigDecimal(2);
        MathContext mc = new MathContext(setPrecision);
        for (int i = 2; i < setPrecision + 3; i++)
        {
            f = f.multiply(new BigDecimal(i));
            BigDecimal e2 = e.add(BigDecimal.ONE.divide(f, mc), mc);
            if (e2.equals(e))
            {
                break;
            }
            e = e2;
        }
        return e;
    }
    
    private static final int SCALE = 18;
        
    /**
     * Strips trailing zeros from each element in the given BigDecimal array.
     *
     * @param src the source array, or null.
     * @return a new array with trailing zeros stripped, or null if the input is null.
     */
    public static BigDecimal[] stripTrailingZeros(BigDecimal... src)
    {
        if(src==null)
        {
            return null;
        }
        BigDecimal[] dst = new BigDecimal[src.length];
        for(int i=0;i<src.length;i++)
        {
            BigDecimal item = src[i];
            dst[i] = item!=null ? item.stripTrailingZeros(): null;
        }
        return dst;
    }
    /**
     * Strips trailing zeros from each element in the given BigDecimal list.
     *
     * @param src the source list, or null.
     * @return a new list with trailing zeros stripped, or null if the input is null.
     */
    public static List<BigDecimal> stripTrailingZeros(List<BigDecimal> src)
    {
        if(src==null) return null;
        ArrayList<BigDecimal> dst = new ArrayList<>(src.size());
        for(BigDecimal item : src)
        {
            dst.add(item!=null ? item.stripTrailingZeros() : null);
        }
        return dst;
    }
    /**
     * Converts each BigDecimal in the given array to its plain string representation.
     *
     * @param src the source array, or null.
     * @return a new string array, or null if the input is null.
     */
    public static String[] asPlainString(BigDecimal... src)
    {
        if(src==null) return null;
        String[] dst = new String[src.length];
        for(int i=0;i<src.length;i++)
        {
            BigDecimal item = src[i];
            dst[i] = item!=null ? item.toPlainString() : null;
        }
        return dst;
    }
    /**
     * Converts each BigDecimal in the given list to its plain string representation.
     *
     * @param src the source list, or null.
     * @return a new string array, or null if the input is null.
     */
    public static String[] asPlainString(List<BigDecimal> src)
    {
        if(src==null)
        {
            return null;
        }
        String[] dst = new String[src.size()];
        for(int i=0;i<dst.length;i++)
        {
            BigDecimal item = src.get(i);
            dst[i] = item!=null ? item.toPlainString() : null;
        }
        return dst;
    }

    private static final BigInteger[] TRIO_UP   = {BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(5)};
    
    /**
     * Generates a sequence of numbers in the 1-2-5 series (preferred numbers)
     * between the given start and end values using BigInteger arithmetic.
     *
     * @param start the start of the range (must be &gt; 0).
     * @param end the end of the range (must be &gt; 0).
     * @return an array of values in the 1-2-5 series within the given range.
     * @throws InvalidParameterException if start or end is &lt;= 0.
     */
    public static BigInteger[] serie125(BigInteger start, BigInteger end)
    {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        
        if(start.compareTo(BigInteger.ZERO)<=0)
        {
            throw new InvalidParameterException("start must be > 0 (was "+start+")");
        }
        if(end.compareTo(BigInteger.ZERO)<=0)
        {
            throw new InvalidParameterException("end must be > 0 (was "+end+")");
        }
        if(start.equals(end))
        {
            return new BigInteger[0];
        }
        boolean reverse = start.compareTo(end)>0;
        if(reverse)
        {
            BigInteger aux = start;
            start = end;
            end = aux;
        }
        
        ArrayList<BigInteger> items = new ArrayList<>() ;
        
        BigInteger[] trio = TRIO_UP.clone();
        
        for(int i=0; (reverse==false && trio[i].compareTo(end)<0) || (reverse==true && trio[i].compareTo(end)<=0) ;i=(++i)%trio.length)
        {
            if((reverse==false && trio[i].compareTo(start)>=0) || (reverse==true && trio[i].compareTo(start)>0) )
            {
                items.add(trio[i]);
            }
            trio[i] = trio[i].multiply(BigInteger.TEN);
        }
        if(reverse)
        {
            Collections.reverse(items);
        }        
        return items.toArray(new BigInteger[items.size()]);
    }
    private static final BigDecimal[] BIG_DECIMAL_TRIO_UP   = {BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(5)};
    private static final BigDecimal[] BIG_DECIMAL_TRIO_DOWN = {new BigDecimal("0.5"), new BigDecimal("0.2"), new BigDecimal("0.1")};
    
    /**
     * Generates a sequence of numbers in the 1-2-5 series (preferred numbers)
     * between the given start and end values using BigDecimal arithmetic.
     * Values below 1 are also included from the sub-unit 1-2-5 series (0.1, 0.2, 0.5).
     *
     * @param start the start of the range (must be &gt; 0).
     * @param end the end of the range (must be &gt; 0).
     * @return an array of values in the 1-2-5 series within the given range.
     * @throws InvalidParameterException if start or end is &lt;= 0.
     */
    public static BigDecimal[] serie125(BigDecimal start, BigDecimal end)
    {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if(start.compareTo(BigDecimal.ZERO)<=0)
        {
            throw new InvalidParameterException("start must be > 0");
        }
        if(end.compareTo(BigDecimal.ZERO)<=0)
        {
            throw new InvalidParameterException("end must be > 0");
        }
        if(start.equals(end))
        {
            return new BigDecimal[0];
        }
        boolean reverse = start.compareTo(end)>0;
        if(reverse)
        {
            BigDecimal aux = start;
            start = end;
            end = aux;
        }
        
        ArrayList<BigDecimal> items = new ArrayList<>();
        
        BigDecimal[] trio = BIG_DECIMAL_TRIO_UP.clone();
        
        for(int i=0; (reverse==false && trio[i].compareTo(end)<0) || (reverse==true && trio[i].compareTo(end)<=0) ;i=(++i)%trio.length)
        {
            if((reverse==false && trio[i].compareTo(start)>=0) || (reverse==true && trio[i].compareTo(start)>0) )
            {
                items.add(trio[i]);
            }
            trio[i] = trio[i].multiply(BigDecimal.TEN);
        }
        
        if(start.compareTo(BigDecimal.ONE)<0)
        {
            MathContext mc = new MathContext(start.precision());
            trio = BIG_DECIMAL_TRIO_DOWN.clone();

            for(int i=0; (reverse==false && trio[i].compareTo(start)>=0) || (reverse==true && trio[i].compareTo(start)>0) ; i=(++i)%trio.length)
            {
                if((reverse==false && trio[i].compareTo(end)<0) || (reverse==true && trio[i].compareTo(end)<=0))
                {
                    items.add(trio[i]);
                }
                trio[i] = trio[i].divide(BigDecimal.TEN, mc);
            }
            Collections.sort(items);
        }
        
        if(reverse)
        {
            Collections.reverse(items);
        }
        
        return items.toArray(new BigDecimal[items.size()]);
    }
    
    /** A simple comparator for BigDecimal values using natural ordering. */
    public static final Comparator<BigDecimal> SIMPLE_COMPARATOR = new Comparator<BigDecimal>()
    {
        @Override
        public int compare(BigDecimal a, BigDecimal b)
        {
            return a.compareTo(b);
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////// https://github.com/javadev/calc/blob/master/src/main/java/com/github/calc/BigDecimalUtil.java ///////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /** Default number of iterations for iterative algorithms (e.g., cube root). */
    public static long ITER = 1000;
    /** Default MathContext with 100-digit precision. */
    public static MathContext context = new MathContext( 100 );
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    /** PI divided by 180 (conversion factor from degrees to radians). */
    public static BigDecimal PI_DIV_180 = new BigDecimal("3.1415926535897932384626433832795").divide(BigDecimal.valueOf(180), 32, RoundingMode.HALF_UP);
    /** PI divided by 200 (conversion factor from grads to radians). */
    public static BigDecimal PI_DIV_200 = new BigDecimal("3.1415926535897932384626433832795").divide(BigDecimal.valueOf(200), 32, RoundingMode.HALF_UP);
    /** A very small BigDecimal value (10^-100) used as epsilon for comparisons. */
    public static BigDecimal EPS = BigDecimal.ONE.scaleByPowerOfTen(-100);

 
    /**
     * Computes the cosine of the given angle in radians using a Taylor series expansion.
     *
     * @param x the angle in radians.
     * @return the cosine of x.
     */
    public static BigDecimal cosine(BigDecimal x) 
    {

        BigDecimal currentValue = BigDecimal.ONE;
        BigDecimal lastVal      = currentValue.add(BigDecimal.ONE);
        BigDecimal xSquared     = x.multiply(x);
        BigDecimal numerator    = BigDecimal.ONE;
        BigDecimal denominator  = BigDecimal.ONE;
        int        i            = 0;

        while (lastVal.compareTo(currentValue) != 0)
        {
            lastVal = currentValue;

            int z = 2 * i + 2;

            denominator = denominator.multiply(BigDecimal.valueOf(z));
            denominator = denominator.multiply(BigDecimal.valueOf(z - 1));
            numerator   = numerator.multiply(xSquared);

            BigDecimal term = numerator.divide(denominator, SCALE + 5, ROUNDING_MODE);

            if (i % 2 == 0) 
            {
                currentValue = currentValue.subtract(term);
            } 
            else 
            {
                currentValue = currentValue.add(term);
            }
            i++;
        }

        return currentValue;
    }

    /**
     * Computes the sine of the given angle in radians using a Taylor series expansion.
     *
     * @param x the angle in radians.
     * @return the sine of x.
     */
    public static BigDecimal sine(BigDecimal x) 
    {
        BigDecimal lastVal      = x.add(BigDecimal.ONE);
        BigDecimal currentValue = x;
        BigDecimal xSquared     = x.multiply(x);
        BigDecimal numerator    = x;
        BigDecimal denominator  = BigDecimal.ONE;
        int        i            = 0;

        while (lastVal.compareTo(currentValue) != 0) 
        {
            lastVal = currentValue;

            int z = 2 * i + 3;

            denominator = denominator.multiply(BigDecimal.valueOf(z));
            denominator = denominator.multiply(BigDecimal.valueOf(z - 1));
            numerator   = numerator.multiply(xSquared);

            BigDecimal term = numerator.divide(denominator, SCALE + 5, ROUNDING_MODE);

            if (i % 2 == 0) 
            {
                currentValue = currentValue.subtract(term);
            } 
            else 
            {
                currentValue = currentValue.add(term);
            }

            i++;
        }
        return currentValue;
    }

    /**
     * Computes the tangent of the given angle in radians as sine/cosine.
     *
     * @param x the angle in radians.
     * @return the tangent of x.
     */
    public static BigDecimal tangent(BigDecimal x) {

        BigDecimal sin = sine(x);
        BigDecimal cos = cosine(x);

        return sin.divide(cos, SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Computes the base-10 logarithm of the given BigDecimal value.
     *
     * @param b the value (must be positive).
     * @return the base-10 logarithm of b.
     * @throws ArithmeticException if b is zero or negative.
     */
    public static BigDecimal log10(BigDecimal b) 
    {
        final int NUM_OF_DIGITS = SCALE + 2;
            // need to add one to get the right number of dp
            //  and then add one again to get the next number
            //  so I can round it correctly.
    
        MathContext mc = new MathContext(NUM_OF_DIGITS, RoundingMode.HALF_EVEN);
        //special conditions:
        // log(-x) -> exception
        // log(1) == 0 exactly;
        // log of a number lessthan one = -log(1/x)
        if(b.signum() <= 0) 
        {
                throw new ArithmeticException("log of a negative number! (or zero)");
        
        }
        else if (b.compareTo(BigDecimal.ONE) == 0)
        {
            return BigDecimal.ZERO;
        }
        else if (b.compareTo(BigDecimal.ONE) < 0)
        {
            return (log10((BigDecimal.ONE).divide(b, mc))).negate();
        }
    
        StringBuilder sb = new StringBuilder();
        //number of digits on the left of the decimal point
        int leftDigits = b.precision() - b.scale();
    
        //so, the first digits of the log10 are:
        sb.append(leftDigits - 1).append(".");
    
        //this is the algorithm outlined in the webpage
        int n = 0;
        while (n < NUM_OF_DIGITS)
        {
            b = (b.movePointLeft(leftDigits - 1)).pow(10, mc);
            leftDigits = b.precision() - b.scale();
            sb.append(leftDigits - 1);
            n++;
        }
    
        BigDecimal ans = new BigDecimal(sb.toString());
    
        //Round the number to the correct number of decimal places.
        ans = ans.round(new MathContext(ans.precision() - ans.scale() + SCALE, RoundingMode.HALF_EVEN));
        return ans;
    }
    
    /**
     * Computes the cube root of the given BigDecimal value using the Newton-Raphson method.
     *
     * @param b the value.
     * @return the cube root of b.
     */
    public static BigDecimal cuberoot(BigDecimal b) 
    {
        if (b.compareTo(BigDecimal.ZERO) == 0 || b.compareTo(BigDecimal.ONE) == 0)
        {
            return b;
        }

        // Specify a math context with 40 digits of precision.

        MathContext mc = new MathContext(40);

        BigDecimal x = new BigDecimal("1", mc);
        BigDecimal xPrev;

        // Search for the cube root via the Newton-Raphson loop. Output each // successive iteration's value.

        for (int i = 0; i < ITER; i++) 
        {
            xPrev = x;
            x = x.subtract(x.pow(3, mc).subtract(b, mc).divide(new BigDecimal("3", mc).multiply(x.pow(2, mc), mc), mc), mc);
            if (x.compareTo(xPrev) == 0)
            {
                break;
            }
        }
        return x;
    }

    /**
     * Computes the arc sine of the given value, using {@code Math.asin}.
     *
     * @param val the value whose arc sine is to be computed.
     * @return the arc sine of val, in radians.
     */
    public static BigDecimal asin(BigDecimal val)
    {
        return BigDecimal.valueOf(Math.asin(val.doubleValue()));
    }

    /**
     * Computes the arc cosine of the given value, using {@code Math.acos}.
     *
     * @param val the value whose arc cosine is to be computed.
     * @return the arc cosine of val, in radians.
     */
    public static BigDecimal acos(BigDecimal val)
    {
        return BigDecimal.valueOf(Math.acos(val.doubleValue()));
    }

    /**
     * Computes the arc tangent of the given value, using {@code Math.atan}.
     *
     * @param val the value whose arc tangent is to be computed.
     * @return the arc tangent of val, in radians.
     */
    public static BigDecimal atan(BigDecimal val)
    {
        return BigDecimal.valueOf(Math.atan(val.doubleValue()));
    }    
    
    /**
     * Returns the order of magnitude (base-10 exponent) of the given long value.
     * The order of magnitude is the number of times the value can be divided by 10
     * while remaining greater than 5.
     *
     * @param value the value (treated as its absolute value).
     * @return the order of magnitude.
     */
    public static int orderOfMagnitude(long value)
    {
        int order = 0;
        value = Math.abs(value);
        while (value > 5)
        {
            value /= 10;
            order++;
        }
        return order;
    }

    /**
     * Returns the order of magnitude (base-10 exponent) of the given double value.
     * Values &lt;= 0.5 return negative orders.
     *
     * @param value the value (treated as its absolute value).
     * @return the order of magnitude.
     */
    public static int orderOfMagnitude(double value)
    {
        int order = 0;
        value = Math.abs(value);

        if (value <= 0.5)
        {
            while (value <= 0.5)
            {
                value *= 10;
                order--;
            }
        }
        else
        {
            while (value > 5)
            {
                value /= 10;
                order++;
            }
        }
        return order;
    }

    static class Pow2Holder
    {
        private static final long[] POW2 =
        {
            1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L, 1024L, 2048L, 4096L,
            8192L, 16384L, 32768L, 65536L, 131072L, 262144L, 524288L, 1048576L, 2097152L,
            4194304L, 8388608L, 16777216L, 33554432L, 67108864L, 134217728L, 268435456L,
            536870912L, 1073741824L, 2147483648L, 0
        };
    }
    
    /**
     * Returns the base-2 logarithm of the given value (floor).
     *
     * @param n the value (must be &gt; 0).
     * @return floor(log2(n)), or -1 if n &lt;= 0.
     */
    public static int log2(long n)
    {
        return log2(n,false);
    }

    /**
     * Returns the base-2 logarithm of the given value, optionally rounding up.
     *
     * @param n the value (must be &gt; 0).
     * @param ceil if true, returns the ceiling instead of the floor.
     * @return floor(log2(n)) or ceil(log2(n)), or -1 if n &lt;= 0.
     */
    public static int log2(long n, boolean ceil)
    {
        if (n <= 0)
        {
            return -1; // or what it defines for the invalid case
        }
        int floor = 63 - Long.numberOfLeadingZeros(n);
        if (ceil && (n & (n - 1)) != 0)
        {
            floor++; // It is not an exact power of 2
        }
        return floor;
    }
    
    /**
     * Returns 2 raised to the given power.
     *
     * @param pow the exponent.
     * @return 2^pow.
     */
    public static long pow2(int pow)
    {
        return 1L << pow;
    }

    /**
     * Returns the greatest common divisor of all the given values.
     * <p>
     * The result is always non-negative.
     *
     * @param values the values whose gcd is to be computed.
     * @return the positive gcd of the given values, or 0 if no values are given.
     */
    public static int gcd(int ... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        int result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            result = gcd(result, values[i]);
        }
        return Math.abs(result);
    }

    /**
     * Returns the least common multiple of a and b, always non-negative.
     * <p>
     * Note that lcm(0, 0) is 0, and that the result may overflow (wrap
     * around) if it does not fit in an int.
     *
     * @param a the first value.
     * @param b the second value.
     * @return the positive lcm of a and b.
     */
    public static int lcm(int a, int b)
    {
        return Math.abs((a / gcd(a,b)) * b);
    }
    /**
     * Returns the least common multiple of all the given values.
     * <p>
     * The result is always non-negative.
     *
     * @param values the values whose lcm is to be computed.
     * @return the positive lcm of the given values, or 0 if no values are given.
     */
    public static int lcm(int ... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        int result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            result = lcm(result, values[i]);
        }
        return Math.abs(result);
    }   
    
    static final double[] log_cache=new double[256];
    
    /**
     * Returns the natural logarithm of the given integer, using a cache for small values.
     *
     * @param n the value.
     * @return ln(n).
     */
    public static double log(int n)
    {
        if(n==1)
        {
            return 0;
        }
        if(n<0 || n>=log_cache.length)
        {
            return Math.log(n);
        }
        double val = log_cache[n];
        if(val==0)
        {
            val = log_cache[n] = Math.log(n);
        }
        return val;
    }
    
    /**
     * Computes the logarithm of value with the given base.
     *
     * @param base the base of the logarithm.
     * @param value the value.
     * @return log_base(value).
     */
    public static double log(double base, double value)
    {
        //log is faster than log10
        return Math.log(value)/Math.log(base);
    }
            
    /**
     * Fills the given array with a Fibonacci sequence starting with 0, 1.
     *
     * @param seq the array to fill.
     * @return the number of elements written.
     */
    public static int fibonacci(int[] seq)
    {
        return fibonacci(seq, 0, 1, 0, seq.length);
    }
    /**
     * Fills the given array with a Fibonacci sequence starting with f0, f1.
     *
     * @param seq the array to fill.
     * @param f0 the first value in the sequence.
     * @param f1 the second value in the sequence.
     * @return the number of elements written.
     */
    public static int fibonacci(int[] seq, int f0, int f1)
    {
        return fibonacci(seq, f0, f1, 0, seq.length);
    }
    /**
     * Fills a portion of the given array with a Fibonacci sequence starting with f0, f1.
     *
     * @param seq the array to fill.
     * @param f0 the first value in the sequence.
     * @param f1 the second value in the sequence.
     * @param start the start index (inclusive).
     * @param end the end index (exclusive).
     * @return the number of elements written.
     */
    public static int fibonacci(int[] seq, int f0, int f1, int start, int end)
    {
        int count=0;
        if(start<end)
        {
            seq[start] = f0;
            count++;
            if(start+1<end)
            {
                seq[start+1]=f1;
                count++;
            }
            for(int i=start+2;i<end;i++)
            {
                int f2 = f0 + f1;
                seq[i] = f2;
                f0=f1;
                f1=f2;
                count++;
            }
        }            
        return count;
    }


    /**
     * Returns whether the given value is a perfect integer square.
     *
     * @param value the value to test.
     * @return true if value is a perfect square (n*n for some integer n).
     */
    public static boolean isIntegerPow(int value)
    {
        int sqrt = (int)Math.sqrt(value);
        return (sqrt*sqrt==value);
    }
        
    /**
     * Returns {@code d &times; 2<sup>scaleFactor</sup>} computed with optimized bit shifting.
     *
     * @param d the value to scale.
     * @param scaleFactor the power of 2 to multiply by.
     * @return d * 2^scaleFactor.
     */
    public static double scalb(double d, int scaleFactor)
    {
        if (scaleFactor >= 31 || scaleFactor <= -31)
        {
            return d * Math.pow(2, scaleFactor);
        }
        else if (scaleFactor > 0)
        {
            return d * (1 << scaleFactor);
        }
        else if (scaleFactor == 0)
        {
            return d;
        }
        else
        {
            return d * 1.0d / (1 << -scaleFactor);
        }
    }

    /**
     * Returns {@code f &times; 2<sup>scaleFactor</sup>} computed with optimized bit shifting.
     *
     * @param f the value to scale.
     * @param scaleFactor the power of 2 to multiply by.
     * @return f * 2^scaleFactor.
     */
    public static float scalb(float f, int scaleFactor)
    {
        if (scaleFactor >= 31 || scaleFactor <= -31)
        {
            return f * (float) Math.pow(2, scaleFactor);
        }
        else if (scaleFactor > 0)
        {
            return f * (1 << scaleFactor);
        }
        else if (scaleFactor == 0)
        {
            return f;
        }
        else
        {
            return f * 1.0f / (1 << -scaleFactor);
        }
    }
        
    /**
     * Returns the greatest common divisor of all the given values.
     * <p>
     * The result is always non-negative.
     *
     * @param values the values whose gcd is to be computed.
     * @return the positive gcd of the given values, or 0 if no values are given.
     */
    public static long gcd(long ... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        long result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            result = gcd(result, values[i]);
        }
        return Math.abs(result);
    }
    /**
     * Returns the greatest common divisor of a and b.
     * <p>
     * The result is always non-negative.
     *
     * @param a the first value.
     * @param b the second value.
     * @return the positive gcd of a and b.
     */
    public static BigInteger gcd(BigInteger a, BigInteger b)
    {
        return a.gcd(b);
    }

    /**
     * Returns the greatest common divisor of all the given values.
     * <p>
     * The result is always non-negative.
     *
     * @param values the values whose gcd is to be computed.
     * @return the positive gcd of the given values, or 0 if no values are given.
     */
    public static BigInteger gcd(BigInteger... values)
    {
        if (values == null || values.length == 0)
        {
            return BigInteger.ZERO;
        }
        BigInteger result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            result = gcd(result, values[i]);
        }
        return result.abs();
    }

    /**
     * Returns the least common multiple of a and b, always non-negative.
     * <p>
     * Note that lcm(0, 0) is 0, and that the result may overflow (wrap
     * around) if it does not fit in a long.
     *
     * @param a the first value.
     * @param b the second value.
     * @return the positive lcm of a and b.
     */
    public static long lcm(long a, long b)
    {
        return Math.abs((a / gcd(a,b)) * b);
    }
    /**
     * Returns the least common multiple of all the given values.
     * <p>
     * The result is always non-negative.
     *
     * @param values the values whose lcm is to be computed.
     * @return the positive lcm of the given values, or 0 if no values are given.
     */
    public static long lcm(long ... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        long result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            result = lcm(result, values[i]);
        }
        return Math.abs(result);
    }
    /**
     * Returns the least common multiple of a and b, always non-negative.
     *
     * @param a the first value.
     * @param b the second value.
     * @return the positive lcm of a and b.
     */
    public static BigInteger lcm(BigInteger a, BigInteger b)
    {
        return a.multiply(b).divide(gcd(a, b)).abs();
    }

    /**
     * Returns the least common multiple of all the given values.
     * <p>
     * The result is always non-negative.
     *
     * @param values the values whose lcm is to be computed.
     * @return the positive lcm of the given values, or 0 if no values are given.
     */
    public static BigInteger lcm(BigInteger... values)
    {
        if (values == null || values.length == 0)
        {
            return BigInteger.ZERO;
        }
        BigInteger result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            result = lcm(result, values[i]);
        }
        return result.abs();
    }

    /**
     * Fills the given array with a Fibonacci sequence starting with 0, 1 (long).
     *
     * @param seq the array to fill.
     * @return the number of elements written.
     */
    public static int fibonacci(long[] seq)
    {
        return fibonacci(seq, 0, 1, 0, seq.length);
    }
    /**
     * Fills the given array with a Fibonacci sequence starting with f0, f1 (long).
     *
     * @param seq the array to fill.
     * @param f0 the first value in the sequence.
     * @param f1 the second value in the sequence.
     * @return the number of elements written.
     */
    public static int fibonacci(long[] seq, long f0, long f1)
    {
        return fibonacci(seq, f0, f1, 0, seq.length);
    }
    /**
     * Fills a portion of the given array with a Fibonacci sequence starting with f0, f1 (long).
     *
     * @param seq the array to fill.
     * @param f0 the first value in the sequence.
     * @param f1 the second value in the sequence.
     * @param start the start index (inclusive).
     * @param end the end index (exclusive).
     * @return the number of elements written.
     */
    public static int fibonacci(long[] seq, long f0, long f1, int start, int end)
    {
        int count=0;
        if(start<end)
        {
            seq[start] = f0;
            count++;
            if(start+1<end)
            {
                seq[start+1]=f1;
                count++;
            }
            for(int i=start+2;i<end;i++)
            {
                long f2 = f0 + f1;
                seq[i] = f2;
                f0=f1;
                f1=f2;
                count++;
            }
        }            
        return count;
    }

    /**
     * Fills the given array with a Fibonacci sequence starting with 0, 1 (BigInteger).
     *
     * @param seq the array to fill.
     * @return the number of elements written.
     */
    public static int fibonacci(BigInteger[] seq)
    {
        return fibonacci(seq, BigInteger.ZERO, BigInteger.ONE, 0, seq.length);
    }

    /**
     * Fills the given array with a Fibonacci sequence starting with f0, f1 (BigInteger).
     *
     * @param seq the array to fill.
     * @param f0 the first value in the sequence.
     * @param f1 the second value in the sequence.
     * @return the number of elements written.
     */
    public static int fibonacci(BigInteger[] seq, BigInteger f0, BigInteger f1)
    {
        return fibonacci(seq, f0, f1, 0, seq.length);
    }

    /**
     * Fills a portion of the given array with a Fibonacci sequence starting with f0, f1 (BigInteger).
     *
     * @param seq the array to fill.
     * @param f0 the first value in the sequence.
     * @param f1 the second value in the sequence.
     * @param start the start index (inclusive).
     * @param end the end index (exclusive).
     * @return the number of elements written.
     */
    public static int fibonacci(BigInteger[] seq, BigInteger f0, BigInteger f1, int start, int end)
    {
        int count = 0;
        if (start < end)
        {
            seq[start] = f0;
            count++;
            if (start + 1 < end)
            {
                seq[start + 1] = f1;
                count++;
            }
            for (int i = start + 2; i < end; i++)
            {
                BigInteger f2 = f0.add(f1);
                seq[i] = f2;
                f0 = f1;
                f1 = f2;
                count++;
            }
        }
        return count;
    }
    
    /**
     * This method is equivalent to Nums.safePrime(random, bitLength, 100)
     * 
     * @param random source of random bits used to select candidates to be tested for primality.
     * @param bitLength bitLength of the returned BigInteger.

     * @return a safe prime the form of p=2q+1 that is faster than q=(p-1)/2
     */
    public static BigInteger safePrime(Random random, int bitLength)
    {
        return safePrime(random, bitLength, 100);
    }

    /**
     *
     * @param random source of random bits used to select candidates to be tested for primality.
     * @param bitLength bitLength of the returned BigInteger.
     * @param certainty a measure of the uncertainty that the caller is willing to tolerate: if the call returns true the probability that this BigInteger is prime exceeds (1 - 1/2certainty). The execution time of this method is proportional to the value of this parameter.

     * @return a safe prime the form of p=2q+1 that is faster than q=(p-1)/2
     */
    public static BigInteger safePrime(Random random, int bitLength, int certainty)
    {
        //get p=2q+1 is faster than q=(p-1)/2
        
        for (int i = 0; true; i++)
        {
            BigInteger q = BigInteger.probablePrime(bitLength - 1, random);
            BigInteger p = q.multiply(BIG_INT_TWO).add(BigInteger.ONE);
            if (p.isProbablePrime(certainty))
            {
                return p;
            }
        }
    }
    
    /**
     * Constructs a randomly generated BigInteger, uniformly distributed over the range start (inclusive) to bound (exclusive).
     * @param random
     * @param start
     * @param bound
     * @return a BigInteger, uniformly distributed over the range start (inclusive) to bound (exclusive).
     */
    public static BigInteger random(Random random, BigInteger start, BigInteger bound)
    {
        int bitLength = bound.bitLength();
        for (int i = 0; true; i++)
        {
            BigInteger n = new BigInteger(bitLength, random);
            if(n.compareTo(start)>=0 && n.compareTo(bound)<0)
            {
                return n;
            }
        }
    }    
    
    /**
     * Returns the greatest common divisor of a and b, always non-negative.
     * <p>
     * Note that gcd(0, 0) is 0, and that the result can only remain negative
     * when the true gcd is Integer.MIN_VALUE in absolute value, which does not
     * fit in an int (e.g. gcd(Integer.MIN_VALUE, 0)).
     *
     * @param a the first value.
     * @param b the second value.
     * @return the positive gcd of |a| and |b|.
     */
    public static int gcd(int a, int b)
    {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0)
        {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
    
    /**
     * Returns the greatest common divisor of a and b, always non-negative.
     * <p>
     * Note that gcd(0, 0) is 0, and that the result can only remain negative
     * when the true gcd is Long.MIN_VALUE in absolute value, which does not
     * fit in a long (e.g. gcd(Long.MIN_VALUE, 0)).
     *
     * @param a the first value.
     * @param b the second value.
     * @return the positive gcd of |a| and |b|.
     */
    public static long gcd(long a, long b)
    {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0)
        {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    /**
     * Returns the smallest integer value that is greater than or equal to the result of a / b.
     *
     * @param a the dividend.
     * @param b the divisor.
     * @return the ceiling of the division of a by b.
     * @throws ArithmeticException if b is zero.
     */
    public static int ceilDiv(int a, int b)
    {
        if (b == 0)
        {
            throw new ArithmeticException("/ by zero");
        }

        int q = a / b;
        int r = a % b;

        if (r != 0 && ((a ^ b) >= 0))
        {
            q++;
        }

        return q;
    }

    /**
     * Returns the smallest integer value that is greater than or equal to the result of a / b.
     *
     * @param a the dividend.
     * @param b the divisor.
     * @return the ceiling of the division of a by b.
     * @throws ArithmeticException if b is zero.
     */
    public static long ceilDiv(long a, long b)
    {
        if (b == 0)
        {
            throw new ArithmeticException("/ by zero");
        }

        long q = a / b;
        long r = a % b;

        if (r != 0 && ((a ^ b) >= 0))
        {
            q++;
        }

        return q;
    }    

    /**
     * Treats the given byte array as a big-endian counter and increments it by one.
     * Each element wraps around at the given module value.
     *
     * @param b the counter array to increment (modified in place).
     * @param module the modulus at which each element wraps to 0.
     * @return true if the counter overflowed (all digits wrapped).
     */
    public static boolean inc(byte[] b, byte module)
    {
        for (int i = b.length - 1; i >= 0; i--)
        {
            if (++b[i] != module)
            {
                return false;
            }
            b[i] = 0;
        }
        return true;
    }

    /**
     * Treats the given short array as a big-endian counter and increments it by one.
     * Each element wraps around at the given module value.
     *
     * @param b the counter array to increment (modified in place).
     * @param module the modulus at which each element wraps to 0.
     * @return true if the counter overflowed (all digits wrapped).
     */
    public static boolean inc(short[] b, short module)
    {
        for (int i = b.length - 1; i >= 0; i--)
        {
            if (++b[i] != module)
            {
                return false;
            }
            b[i] = 0;
        }
        return true;
    }

    /**
     * Treats the given int array as a big-endian counter and increments it by one.
     * Each element wraps around at the given module value.
     *
     * @param b the counter array to increment (modified in place).
     * @param module the modulus at which each element wraps to 0.
     * @return true if the counter overflowed (all digits wrapped).
     */
    public static boolean inc(int[] b, int module)
    {
        for (int i = b.length - 1; i >= 0; i--)
        {
            if (++b[i] != module)
            {
                return false;
            }
            b[i] = 0;
        }
        return true;
    }

    /**
     * Treats the given long array as a big-endian counter and increments it by one.
     * Each element wraps around at the given module value.
     *
     * @param b the counter array to increment (modified in place).
     * @param module the modulus at which each element wraps to 0.
     * @return true if the counter overflowed (all digits wrapped).
     */
    public static boolean inc(long[] b, long module)
    {
        for (int i = b.length - 1; i >= 0; i--)
        {
            if (++b[i] != module)
            {
                return false;
            }
            b[i] = 0;
        }
        return true;
    }
}
