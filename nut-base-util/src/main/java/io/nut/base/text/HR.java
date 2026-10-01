/*
 *  HR.java
 *
 *  Copyright (c) 2023-2024 francitoshi@gmail.com
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

import java.util.Locale;

/**
 *
 * @author franci
 */
public class HR
{
    private final char filler;
    private final int width;
    private final int border;
    private volatile String full;
    private final Locale locale;

    public HR(char filler, int width, int border, Locale locale)
    {
        this.filler = filler;
        this.border = border;
        this.width = width;
        this.locale = locale;
    }
    public HR(char filler, int width, int border)
    {
        this(filler, width, border, null);
    }
    public HR()
    {
        this('-', 72, 5, null);
    }

    public String get(CharSequence text)
    {
        boolean spaces = text.length()!=0 && (this.width >= text.length()+this.border*2+2);
        
        StringBuilder sb = new StringBuilder();
        while(sb.length()<this.border)
        {
            sb.append(this.filler);
        }
        if(spaces)
        {
            sb.append(' ');
        }
        sb.append(text);
        if(spaces)
        {
            sb.append(' ');
        }
        while(sb.length()<this.width)
        {
            sb.append(this.filler);
        }
        return sb.toString();
    }

    public String get()
    {
        if(this.full==null)
        {
            StringBuilder sb = new StringBuilder();
            while(sb.length()<this.width)
            {
                sb.append(this.filler);
            }
            this.full = sb.toString();
        }
        return this.full;
    }
    public String format​(String format, Object... args)
    {
        return format(this.locale, format, args);
    }
    public String format​(Locale locale, String format, Object... args)
    {
        return this.get(locale!=null ? String.format(locale, format, args) : String.format(format, args));
    }
}
