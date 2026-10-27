/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

/**
 *
 * @author franci
 * @param <E>
 */
public interface Equalizer<E>
{
    abstract boolean equals(E t1, E t2);
    
    abstract int hashCode(E e);  

    final static Equalizer<String> STRING_CASE_INSENSITIVE = new Equalizer<String>()
    {
        @Override
        public boolean equals(String t1, String t2)
        {
            return t1.equalsIgnoreCase(t2);
        }

        @Override
        public int hashCode(String e)
        {
            //do not use benign race condition caching hashCode, is much worst
            return e.toUpperCase().hashCode();
        }
    };
    
    
}