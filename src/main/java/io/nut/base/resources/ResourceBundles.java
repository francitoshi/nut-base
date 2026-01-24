/*
 * ResourceBundles.java
 *
 * Copyright (c) 2014-2026 francitoshi@gmail.com
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
package io.nut.base.resources;

import io.nut.base.util.Locales;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author franci
 */
public class ResourceBundles
{
    static final Locale ROOT_LOCALE = new Locale("","","");

    public static ResourceBundle getStrictBundle(String baseName, Locale locale, Locale defaultLocale)
    {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
        return Locales.contains(bundle.getLocale(), locale) ? bundle : ResourceBundle.getBundle(baseName, defaultLocale);
    }
    
    public static ResourceBundle getBundle(String baseName, Locale locale, boolean strictLocale)
    {
        return strictLocale ? ResourceBundle.getBundle(baseName, locale) : getStrictBundle(baseName, locale, ROOT_LOCALE);
    }
    
}
