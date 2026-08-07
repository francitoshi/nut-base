/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.text;

import io.nut.base.resources.ResourceBundles;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

class GermanOrdinalFormat extends OrdinalFormat
{
    final HashMap<Long,String> map = new HashMap<>();
    public GermanOrdinalFormat(Locale locale, Gender gender, int style)
    {
        super(locale, gender, style);
        ResourceBundle rb = super.getResourceBundle( locale);
        for(Enumeration<String> e = rb.getKeys(); e.hasMoreElements();)
        {
            String id = e.nextElement();
            String value = rb.getString(id);
            long key = Double.valueOf(id).longValue();
            map.put(key, value);
        }
    }

    @Override
    ResourceBundle getResourceBundle(Locale locale)
    {
        return ResourceBundles.getBundle(CardinalFormat.class, locale);
    }
    static String[][] endings=
    {
        {"zehn",    "te", null },
        {"zig",     "ste", null},
        {"ßig",     "ste", null},
        {"hundert", "ste", null},
        {"ausend",  "ste", null},
        {"lion",    "ste", null},
        {"lionen",  "ste", null},
        {"liarde",  "ste", null},
        {"liarden", "ste", null},
        {"eins",    "erste", "eins$"},
        {null,      "te",  null},
    };
    @Override
    public String format(long num)
    {
        String value = this.map.get(num);
        if(value==null)
        {
            value = super.format(num);
            for(String[] item : endings) 
            {
                if(item[0]==null || value.endsWith(item[0]))
                {
                    value = item[2]==null ? value+item[1] : value.replaceFirst(item[2], item[1]);
                    break;
                }
            }
        }
        return value;
    }

}
/**
 *
 * @author franci
 */
public class OrdinalFormat extends CardinalFormat
{

    public OrdinalFormat(Locale locale, Gender gender, int style)
    {
        super(locale, gender, style);
    }
    
    @Override
    ResourceBundle getResourceBundle(Locale locale)
    {
        return ResourceBundles.getBundle(OrdinalFormat.class, locale);
    }
    static public OrdinalFormat getInstance(Locale locale, Gender gender) 
    {
        if(locale.getISO3Language().equals("deu"))
        {
            return new GermanOrdinalFormat(locale,gender,OrdinalFormat.SHORT);
        }
        return new OrdinalFormat(locale,gender,OrdinalFormat.SHORT);
    }
    static public OrdinalFormat getInstance(Locale locale) 
    {
        return new OrdinalFormat(locale,Gender.NEUTRAL,OrdinalFormat.SHORT);
    }
    static public OrdinalFormat getInstance(Gender gender) 
    {
        return new OrdinalFormat(Locale.getDefault(),gender,OrdinalFormat.SHORT);
    }
    static public OrdinalFormat getInstance() 
    {
        return new OrdinalFormat(Locale.getDefault(),Gender.NEUTRAL,OrdinalFormat.SHORT);
    }
}
