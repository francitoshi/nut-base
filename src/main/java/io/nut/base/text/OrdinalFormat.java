/*
 *  GermanOrdinalFormat.java
 *
 *  Copyright (C) 2014-2026 francitoshi@gmail.com
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
        ResourceBundle rb = super.getResourceBundle( locale, false);
        for(Enumeration<String> e = rb.getKeys(); e.hasMoreElements();)
        {
            String id = e.nextElement();
            String value = rb.getString(id);
            long key = Double.valueOf(id).longValue();
            map.put(key, value);
        }
    }

    @Override
    ResourceBundle getResourceBundle(Locale locale, boolean strictLocale)
    {
        return ResourceBundles.getBundle(CardinalFormat.class.getName(), locale, strictLocale);
    }
    static String[][] endings=
    {
        {"zehn",    "te", null },
        {"zig",     "ste", null},
        {"ßig",     "ste", null},
        {"hundert", "ste", null},
        {"ausend",  "ste", null},
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
    ResourceBundle getResourceBundle(Locale locale, boolean strictLocale)
    {
        return ResourceBundles.getBundle(OrdinalFormat.class.getName(), locale, strictLocale);
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
