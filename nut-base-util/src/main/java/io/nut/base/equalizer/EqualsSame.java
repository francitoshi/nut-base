/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.equalizer;

/**
 *
 * @author franci
 */
public class EqualsSame<T>
{
    
    private final T data;

    public EqualsSame(T data)
    {
        this.data = data;
    }

    @Override
    public int hashCode()
    {
        return this.data.hashCode();
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
        final EqualsSame<?> other = (EqualsSame<?>) obj;
        return this.data == other.data;
    }
    
}
