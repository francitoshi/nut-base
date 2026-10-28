/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.text;

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
