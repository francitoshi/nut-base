/*
 *  RomanNumbers.java
 *
 *  Copyright (C) 2014-2025 francitoshi@gmail.com
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
package io.nut.base.text;

import io.nut.base.util.Strings;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 *
 * @author franci
 */
public class RomanNumbers
{
    //\bM{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})\b
    
    public static final String PATTERN  = "M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})";
    public static final String STRICT_PATTERN  = "^"+PATTERN+"$";
    public static final String LENIENT_PATTERN = "^(?=[MDCLXVI])M*(C[MD]|D?C*)(X[CL]|L?X*)(I[XV]|V?I*)$";
    
    public static final Pattern strictPattern = Pattern.compile(STRICT_PATTERN);
    public static final Pattern lenientPattern = Pattern.compile(LENIENT_PATTERN);
    private static final CardinalFormat cf = CardinalFormat.getInstance(new Locale("la"));
    
    final Pattern pattern;
    final boolean ignoreCase;
    final boolean clock4;

    
    public RomanNumbers(boolean ignoreCase, boolean clock4, boolean lenient)
    {
        this.pattern = lenient ? lenientPattern : strictPattern;
        this.ignoreCase = ignoreCase;
        this.clock4 = clock4;
    }
    public RomanNumbers(boolean ignoreCase, boolean clock4)
    {
        this(ignoreCase, clock4, false);
    }
    public RomanNumbers(boolean ignoreCase)
    {
        this(ignoreCase, false, false);
    }
    
    public boolean isValid(String s)
    {
        if(s.trim().length()==0)
        {
            return false;
        }
        s = ignoreCase  ? s.toUpperCase() : s;
        
        if(!clock4 && s.equals("IIII"))
        {
            return true;
        }
        
        return pattern.matcher(s).matches();
    }
    
    private static class Rule
    {
        String key;
        int value;
        public Rule(String key, int value)
        {
            this.key = key;
            this.value = value;
        }
    }
    
    private static final Rule[] rules = 
    {
        new Rule("MMMM",-1),
        new Rule("M",1000),
        new Rule("CM",900),
        new Rule("DD",-1),
        new Rule("D",500),
        new Rule("CD",400),
        new Rule("CCCC",-1),
        new Rule("C",100),
        new Rule("XC",90),
        new Rule("LL",-1),
        new Rule("L",50),
        new Rule("XL",40),
        new Rule("XXXX",10),
        new Rule("X",10),
        new Rule("IX",9),
        new Rule("V",5),
        new Rule("IV",4),
        new Rule("IIII",-1),
        new Rule("I",1),
        new Rule("",-1)
    };
    
    public int parse(String s)
    {
        final String master = s;
        s = ignoreCase  ? s.toUpperCase() : s;
        
        if(clock4 && s.equals("IIII"))
        {
            return 4;
        }
        
        int value = 0;
        while(s.length()>0)
        {

            for(Rule t : rules)
            {
                if(s.startsWith(t.key))
                {
                    if(t.value<0)
                    {
                        throw new NumberFormatException(master);
                    }
                    value+= t.value;
                    s = s.replaceFirst(t.key, "");
                    break;
                }
            }
        }
        return value;
    }
    public int parse(String s, int defaultValue)
    {
        int value ;
        try
        {
            value = Strings.isEmpty(s) ? defaultValue : parse(s);
        }
        catch(NumberFormatException ex)
        {
            value = defaultValue;
        }
        return value;
    }


    public String format(int i)
    {
        return cf.format(i);
    }
}
