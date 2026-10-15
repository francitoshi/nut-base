/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.bag;

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
