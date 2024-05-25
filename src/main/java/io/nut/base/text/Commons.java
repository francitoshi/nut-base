/*
 * Commons.java
 *
 * Copyright (c) 2013-2023 francitoshi@gmail.com
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
package io.tea.base.text;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

/**
 *
 * @author franci
 */
public abstract class Commons
{
    private static final String[] EMPTY = new String[0];
    
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
