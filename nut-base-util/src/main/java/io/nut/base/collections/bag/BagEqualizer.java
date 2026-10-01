/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.bag;

import io.nut.base.equalizer.Equalizer;
import io.nut.base.equalizer.EqualsProxy;
import io.nut.base.equalizer.EqualsSame;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author franci
 * @param <E>
 */
public class BagEqualizer<E> extends Bag<E>
{
    private final Bag<EqualsProxy<E>> data;
    private final Equalizer<E> equalizer;
    private final Set<EqualsSame<E>> set;

    public BagEqualizer(Equalizer<E> equalizer, boolean skipSame)
    {
        this.data = Bag.create();
        this.equalizer = equalizer; 
        this.set = skipSame ? new HashSet() : null;
    }

    @Override
    public boolean add(E e)
    {
        if(this.set!=null && !this.set.add(new EqualsSame<>(e)))
        {
            return false;
        }
        return this.data.add(new EqualsProxy<>(equalizer,e));
    }

    @Override
    public E[] get(E e)
    {
        EqualsProxy<E>[] proxies = this.data.get(new EqualsProxy<>(equalizer,e));

        if(proxies==null)
        {
            return null;
        }

        E[] items = (E[]) Array.newInstance(e.getClass(), proxies.length);
        for(int i=0;i<items.length;i++)
        {
            items[i] = proxies[i].data;
        }
        return items;
    }

    @Override
    public int count(E e)
    {
        return this.data.count(new EqualsProxy<>(this.equalizer,e));
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
        EqualsProxy[] eq = data.toArray(new EqualsProxy[0]);
        ArrayList<E> list = new ArrayList<>();
        for(EqualsProxy<E> item : eq)
        {
            list.add(item.data);
        }
        return list.toArray(dst);
    }

    @Override
    public E[][] toArray(E[][] dst)
    {
        ArrayList<E[]> items = new ArrayList<>();
        
        EqualsProxy<E>[][] array1st = data.toArray(new EqualsProxy[0][0]);
        
        for( EqualsProxy<E>[] array2nd : array1st)
        {
            ArrayList<E> sub = new ArrayList<>();
            for( EqualsProxy<E> p : array2nd)
            {
                sub.add(p.data);
            }
            if (!sub.isEmpty())
            {
                E[] subArray = (E[]) Array.newInstance(sub.get(0).getClass(), sub.size());
                items.add(sub.toArray(subArray));
            }
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
