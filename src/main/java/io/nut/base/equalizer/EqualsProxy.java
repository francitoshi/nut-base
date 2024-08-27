/*
 * EqualsProxy.java
 *
 * Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.equalizer;

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
