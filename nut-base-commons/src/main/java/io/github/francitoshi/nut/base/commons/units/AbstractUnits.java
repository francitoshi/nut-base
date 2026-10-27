/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.units;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 *
 * @author franci
 */
public class AbstractUnits
{
    public static final int LENIENT = 1;
    public static final int SPACE   = 2;

    private final long[] values;
    private final String[] shortNames;
    private final String[] longNames;
    protected final boolean lenient;
    protected final String space;
    protected final String sufix;

    public AbstractUnits(long[] values, String[] shortNames, String[] longNames, int flags, String sufix)
    {
        this.values = values;
        this.shortNames = shortNames;
        this.longNames  = longNames;
        this.lenient    = (flags&LENIENT)== LENIENT;
        this.space      = (flags&SPACE) == SPACE ? " ": "";
        this.sufix       = sufix;
    }

    public long parse(String nm) throws NumberFormatException
    {
        if (lenient)
        {
            nm = nm.toLowerCase();
        }
        for (int i = shortNames.length-1; i >= 0; i--)
        {
            String unit = lenient ? (shortNames[i]+sufix).toLowerCase() : space+shortNames[i]+sufix;
            if (nm.endsWith(unit) && unit.length()>0)
            {
                String val = nm.substring(0, nm.length() - unit.length()).trim();
                return Long.parseLong(val) * values[i];
            }
        }
        return Long.parseLong(nm);
    }
    public String toString(long val,boolean round)
    {
        long absVal = Math.abs(val);
        for (int i = values.length-1; i >=0; i--)
        {
            long unit = (round ? Math.round((double) val / values[i]) : val / values[i]);
            if (absVal>=values[i])
            {
                return ""+unit+space+shortNames[i]+sufix;
            }
        }
        return ""+val+space+sufix;
    }
    public String toString(double val, int precision)
    {
        BigDecimal value = BigDecimal.valueOf(val);
        double absVal = Math.abs(val);
        for (int i = values.length-1; i >=0; i--)
        {
            BigDecimal unit = value.divide(BigDecimal.valueOf(values[i]), new MathContext(precision));
            if (absVal>=values[i])
            {
                return ""+unit+space+shortNames[i]+sufix;
            }
        }
        return ""+val+space+sufix;
    }
}
