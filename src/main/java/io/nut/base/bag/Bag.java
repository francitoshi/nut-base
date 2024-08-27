/*
 * Bag.java
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
package io.nut.base.bag;

import io.nut.base.equalizer.Equalizer;
import java.util.Comparator;


/**
 *
 * @author franci
 * @param <E>
 */
public abstract class Bag<E>
{
    public abstract boolean add(E e);
            
    public abstract E[] get(E e);
    
    public abstract int count(E e);
    
    public abstract int size();

    public abstract boolean isEmpty();

    public abstract E[] toArray(E[] dst);

    public abstract E[][] toArray(E[][] dst);

    public abstract void clear();

    
    public static <T> Bag<T> create()
    {
        return new BagBase<>(false);
    }
    public static <T> Bag<T> create(boolean skipSame)
    {
        return new BagBase<>(skipSame);
    }
    public static <T> Bag<T> create(Equalizer<T> equalizer, boolean skipSame)
    {
        return new BagEqualizer<>(equalizer, skipSame);
    }
    public static <T> Bag<T> create(Equalizer<T> equalizer)
    {
        return new BagEqualizer<>(equalizer, false);
    }
    
    public static <T> Bag<T> create(Comparator<T> comparator, boolean skipSame)
    {
        return new BagBase<>(comparator, skipSame);
    }
    public static <T> Bag<T> create(Comparator<T> comparator)
    {
        return new BagBase<>(comparator, false);
    }
    
    public static <T> Bag<T> synchronizedBag(Bag<T> bag)
    {
        return new Bag<T>() 
        {
            final Object lock = new Object();
            
            @Override
            public boolean add(T t)
            {
                synchronized (lock)
                {
                    return bag.add(t);
                }
            }

            @Override
            public T[] get(T t)
            {
                synchronized (lock)
                {
                    return bag.get(t);
                }
            }

            @Override
            public int count(T t)
            {
                synchronized (lock)
                {
                    return bag.count(t);
                }
            }

            @Override
            public int size()
            {
                synchronized (lock)
                {
                    return bag.size();
                }
            }

            @Override
            public boolean isEmpty()
            {
                synchronized (lock)
                {
                    return bag.isEmpty();
                }
            }

            @Override
            public T[] toArray(T[] dst)
            {
                synchronized (lock)
                {
                    return bag.toArray(dst);
                }
            }

            @Override
            public T[][] toArray(T[][] dst)
            {
                synchronized (lock)
                {
                    return bag.toArray(dst);
                }
            }

            @Override
            public void clear()
            {
                synchronized (lock)
                {
                    bag.clear();
                }
            }
        };
    }
}
