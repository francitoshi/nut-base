/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

/**
 *
 * @author franci
 * @param <T>
 */
public class EqualsProxy<T>
{
    public final Equalizer<T> equalizer;
    public final T data;

    public EqualsProxy(Equalizer<T> equalizer, T data)
    {
        this.equalizer = equalizer;
        this.data = data;
    }

    @Override
    public int hashCode()
    {
        return this.equalizer.hashCode(this.data);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final EqualsProxy<T> other = (EqualsProxy<T>) obj;
        if (this.data == other.data)
        {
            return true;
        }
        return equalizer.equals(this.data, other.data);
    }
    
}
