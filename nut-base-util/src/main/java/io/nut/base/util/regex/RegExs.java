package io.nut.base.util.regex;

/*
 *  RegExs.java
 *
 *  Copyright (C) 2009-2025 francitoshi@gmail.com
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
import java.util.regex.Pattern;

/**
 *
 * @author franci
 */
public class RegExs
{
    public static String wildcardToRegex(String wildcard)
    {
        char[] wc = wildcard.toCharArray();
        StringBuilder regex = new StringBuilder(wc.length*2);
        StringBuilder quoted = new StringBuilder(wc.length*2);
//        regex.append('^');
        for(int i=0;i<wc.length;i++)
        {
            switch (wc[i])
            {
                case '*':
                    if(quoted.length()>0)
                    {
                        regex.append(Pattern.quote(quoted.toString()));
                        quoted.setLength(0);
                    }
                    regex.append(".*");
                    break;
                case '?':
                    if(quoted.length()>0)
                    {
                        regex.append(Pattern.quote(quoted.toString()));
                        quoted.setLength(0);
                    }
                    regex.append(".");
                    break;
                case '[':
                    if(quoted.length()>0)
                    {
                        regex.append(Pattern.quote(quoted.toString()));
                        quoted.setLength(0);
                    }
                    StringBuilder ch = new StringBuilder();
                    for(;i<wc.length;i++)
                    {
                        if(wc[i]==']')
                            break;
                        if(wc[i]=='\\')
                            i++;
                        ch.append(wc[i]);
                    }
                    regex.append("[").append(Pattern.quote(ch.toString())).append("]");
                    break;
                case '\\':
                    i++;
                    if(i<wc.length)
                    {
                        quoted.append(wc[i]);
                    }
                    break;
                default:
                    quoted.append(wc[i]);
                    break;
            }
        }
        if(quoted.length()>0)
        {
            regex.append(Pattern.quote(quoted.toString()));
        }
//        regex.append('$');
        return regex.toString();
    }
}
