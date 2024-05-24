/*
 *  SizeUnits.java
 *
 *  Copyright (C) 2009-2024 francitoshi@gmail.com
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
public class SizeUnits extends AbstractUnits
{
    public static final int BINARY  = 1024;
    public static final int DECIMAL = 1000;

    public final static long Kb = 1024L;
    public final static long Ks = 1000L;
    private final static long[] VALUES_BINARY   = new long[]{1L, Kb, Kb*Kb, Kb*Kb*Kb, Kb*Kb*Kb*Kb, Kb*Kb*Kb*Kb*Kb, Kb*Kb*Kb*Kb*Kb*Kb};
    private final static long[] VALUES_STANDARD = new long[]{1L, Ks, Ks*Ks, Ks*Ks*Ks, Ks*Ks*Ks*Ks, Ks*Ks*Ks*Ks*Ks, Ks*Ks*Ks*Ks*Ks*Ks};
    private final static String[] SHORT_NAMES = new String[]
    {
        "", "k", "M", "G", "T", "P", "E"
    };
    private final static String[] LONG_NAMES = new String[]
    {
        "", "kilo", "mega", "giga", "tera", "peta", "exa"
    };
    private final static String[] SHORT_NAMES_BINARY = new String[]
    {
        "", "Ki", "Mi", "Gi", "Ti", "Pi", "Ei"
    };
    private final static String[] LONG_NAMES_BINARY = new String[]
    {
        "", "kibi", "mebi", "gibi", "tebi", "pebi", "exbi"
    };
    static boolean binary(int flags)
    {
        return (flags&BINARY)==BINARY;
    }
    private SizeUnits(long[] values, int flags, String sufix)
    {
        super(values, binary(flags)?SHORT_NAMES_BINARY:SHORT_NAMES, binary(flags)?LONG_NAMES_BINARY:LONG_NAMES, flags, sufix);
    }
    static public SizeUnits getBinaryInstance(int flags, String sufix)
    {
        return new SizeUnits(VALUES_BINARY, flags, sufix);
    }
    static public SizeUnits getBinaryInstance(int flags)
    {
        return new SizeUnits(VALUES_BINARY, flags, "");
    }
    static public SizeUnits getBinaryInstance()
    {
        return new SizeUnits(VALUES_BINARY, LENIENT, "");
    }
    static public SizeUnits getStandardInstance(int flags, String sufix)
    {
        return new SizeUnits(VALUES_STANDARD, flags, sufix);
    }
    static public SizeUnits getStandardInstance(int flags)
    {
        return new SizeUnits(VALUES_STANDARD, flags, "");
    }
    static public SizeUnits getStandardInstance()
    {
        return new SizeUnits(VALUES_STANDARD, LENIENT, "");
    }
}
