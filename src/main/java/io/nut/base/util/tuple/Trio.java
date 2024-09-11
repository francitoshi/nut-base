/*
 *  Trio.java
 *
 *  Copyright (c) 2018-2024 francitoshi@gmail.com
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
 * @param <A> the Attribute
 */
public class Trio<K,V,A>
{
    private final K key;
    private final V val;
    private final A att;

    public Trio(K key, V val, A att)
    {
        this.key = key;
        this.val = val;
        this.att = att;
    }

    public K getKey()
    {
        return key;
    }

    public V getVal()
    {
        return val;
    }

    public A getAtt()
    {
        return att;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.key);
        hash = 17 * hash + Objects.hashCode(this.val);
        hash = 17 * hash + Objects.hashCode(this.att);
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
        final Trio<?, ?, ?> other = (Trio<?, ?, ?>) obj;
        if (!Objects.equals(this.key, other.key))
        {
            return false;
        }
        if (!Objects.equals(this.val, other.val))
        {
            return false;
        }
        return Objects.equals(this.att, other.att);
    }   

    public static <K,V,A> K getKey(Trio<K,V,A> trio)
    {
        return trio!=null ? trio.getKey() : null;
    }
    public static <K,V,A> V getVal(Trio<K,V,A> trio)
    {
        return trio!=null ? trio.getVal() : null;
    }
    public static <K,V,A> V getAtt(Trio<K,V,A> trio)
    {
        return trio!=null ? trio.getVal() : null;
    }
    
    public K get1st() 
    {
        return key;
    }
    
    public V get2nd() 
    {
        return val;
    }
    
    public A get3rd() 
    {
        return att;
    }
}
