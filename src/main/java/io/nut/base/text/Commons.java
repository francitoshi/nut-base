/*
 * Copyright (C) 2013-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.text;

import io.nut.base.util.Empty;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

/**
 *
 * @author franci
 */
public abstract class Commons
{
    private static final String[] EMPTY = Empty.STRINGS;
    
    public static String[] getCommons(String[] texts)
    {
        return getCommons(texts, Integer.MAX_VALUE, 1, Integer.MAX_VALUE, false);
    }
    public static String[] getCommons(String[] texts, int num)
    {
        return getCommons(texts, num, 1, Integer.MAX_VALUE, false);
    }
    public static String[] getCommons(String[] texts, int num, int min, int max)
    {
        return getCommons(texts, num, 1, max, false);
    }
    public static String[] getCommonsIgnoreCase(String[] texts)
    {
        return getCommons(texts, Integer.MAX_VALUE, 1, Integer.MAX_VALUE, true);
    }
    public static String[] getCommonsIgnoreCase(String[] texts, int num)
    {
        return getCommons(texts, num, 1, Integer.MAX_VALUE, true);
    }
    public static String[] getCommonsIgnoreCase(String[] texts, int num, int min, int max)
    {
        return getCommons(texts, num, 1, max, true);
    }
            
    static String[] getCommons(String[] texts, int num, int min, int max, boolean ignoreCase)
    {
        if(texts.length==0)
        {
            return EMPTY;
        }
        if(texts.length==1)
        {
            return texts.clone();
        }
        String txt = null;
        
        for(String text : texts)
        {
            if (txt==null || txt.length() > text.length())
            {
                txt = text;
            }
        }
        if(txt==null)
        {
            return EMPTY;
        }
        
        int len = txt.length();
        max = Math.min(max, len);
        
        HashSet<String> list = new HashSet<>();
        for(int bi=0;bi<=len-min;bi++)
        {
            for(int ei=max;ei>bi && list.size()<num;ei--)
            {
                boolean ok=true;
                String sub = txt.substring(bi, ei);
                if(ignoreCase) sub=sub.toLowerCase();
                for(int i=0;ok && i<texts.length;i++)
                {
                    if(ignoreCase) 
                    {
                        ok=texts[i].toLowerCase().contains(sub);
                    }
                    else
                    {
                        ok=texts[i].contains(sub);
                    }
                    
                }
                if(ok) list.add(sub);
            }
        }
        return sort(list.toArray(EMPTY), ignoreCase);
    }
    
    private static String[] sort(String[] texts, final boolean ignoreCase)
    {
        Comparator<String> cmp = new Comparator<String>()
        {
            @Override
            public int compare(String o1, String o2)
            {
                if(o1.length()<o2.length())
                {
                    return +1;
                }
                if(o1.length()>o2.length())
                {
                    return -1;
                }
                return ignoreCase?o1.compareToIgnoreCase(o2):o1.compareTo(o2);
            }
        };
        Arrays.sort(texts, cmp);
        return texts;
    }
}
