/*
 * AbstractUnits.java
 *
 * Copyright (c) 2012-2024 francitoshi@gmail.com
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
