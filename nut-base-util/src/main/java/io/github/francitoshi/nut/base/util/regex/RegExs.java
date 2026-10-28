/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util.regex;

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
