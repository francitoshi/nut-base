/*
 *  Pair.java
 *
 *  Copyright (c) 2009-2024 francitoshi@gmail.com
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
package io.nut.base.util.tuple;

import java.util.Objects;

/**
 *
 * @author franci
 * @param <K> the Key
 * @param <V> the Value
 */
public class Pair<K,V>
{
    private final K key;
    private final V val;

    public Pair(K key, V val)
    {
        this.key = key;
        this.val = val;
    }

    public K getKey()
    {
        return key;
    }

    public V getVal()
    {
        return val;
    }

    @Override
    public String toString()
    {
        return key + "=" + val;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.key);
        hash = 97 * hash + Objects.hashCode(this.val);
        return hash;
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
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        if (!Objects.equals(this.key, other.key))
        {
            return false;
        }
        return Objects.equals(this.val, other.val);
    }    

    public static <K,V> K getKey(Pair<K,V> pair)
    {
        return pair!=null ? pair.getKey() : null;
    }
    public static <K,V> V getVal(Pair<K,V> pair)
    {
        return pair!=null ? pair.getVal() : null;
    }
    
    public static <K, V> Pair<K, V> of(K left, V right) 
    {
        return new Pair<>(left, right);
    }
    public K getLeft() 
    {
        return key;
    }

    public V getRight() 
    {
        return val;
    }
    
    public K get1st() 
    {
        return key;
    }
    
    public V get2nd() 
    {
        return val;
    }
}
