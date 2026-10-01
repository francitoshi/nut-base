/*
 * Equalizer.java
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