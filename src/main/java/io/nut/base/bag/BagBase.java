/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.bag;

import io.nut.base.equalizer.EqualsSame;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author franci
 * @param <E>
 */
public class BagBase<E> extends Bag<E>
{
    private final Map<E,List<E>> data;
    //this set is used to verify if an object is the same and not just equal,
    //do not try to inherit HashMap and add a Set, it takes the same amount of time
    private final Set<EqualsSame<E>> set;

    public BagBase(boolean skipSame)
    {
        this.data = new HashMap<>();
        this.set = skipSame ? new HashSet() : null;
    }
    public BagBase(Comparator<E> comparator, boolean skipSame)
    {
        this.data = new TreeMap<>(comparator);
        this.set = skipSame ? new HashSet() : null;
    }
    
    @Override
    public boolean add(E e)
    {
        if(this.set!=null && !this.set.add(new EqualsSame<>(e)))
        {
            return false;
        }
        List<E> list = this.data.get(e);
        if(list==null)
        {
            this.data.put(e, list = new ArrayList<>());
        }
        return list.add(e);
    }

    @Override
    public E[] get(E e)
    {
        List<E> list = this.data.get(e);
        if (list == null)
        {
            return null;
        }
        E[] arr = (E[]) Array.newInstance(list.get(0).getClass(), list.size());
        return list.toArray(arr);
    }

    @Override
    public int count(E e)
    {
        List<E> list = this.data.get(e);
        return list!=null ? list.size() : 0;
    }

    @Override
    public int size()
    {
        return data.size();
    }

    @Override
    public boolean isEmpty()
    {
        return data.isEmpty();
    }

    @Override
    public E[] toArray(E[] dst)
    {
        ArrayList<E> items = new ArrayList<>();
        
        for( List<E> list : this.data.values())
        {
            for(E e : list)
            {
                items.add(e);
            }
        }
        return items.toArray(dst);
    }

    @Override
    public E[][] toArray(E[][] dst)
    {
        ArrayList<E[]> items = new ArrayList<>();
        for( List<E> list : this.data.values())
        {
            E[] sublist = list.toArray((E[]) Array.newInstance(list.get(0).getClass(), list.size()));
            items.add(sublist);
        }
        return items.toArray(dst);
    }

    @Override
    public void clear()
    {
        this.data.clear();
        if(set!=null)
        {
            this.set.clear();
        }
    }
}
