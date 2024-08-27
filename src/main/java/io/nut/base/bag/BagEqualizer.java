/*
 * BagEqualizer.java
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
    private volatile E[] empty;
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
        E[] empty = this.empty;
        ArrayList<E[]> items = new ArrayList<>();
        
        EqualsProxy<E>[][] array1st = data.toArray(new EqualsProxy[0][0]);
        
        for( EqualsProxy<E>[] array2nd : array1st)
        {
            ArrayList<E> sub = new ArrayList<>();
            
            for( EqualsProxy<E> p : array2nd)
            {
                E e = p.data;
                if(empty==null)
                {
                    empty = this.empty = (E[]) Array.newInstance(e.getClass(), 0);
                }
                sub.add(e);
            }
            items.add(sub.toArray(empty));
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
