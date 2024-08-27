/*
 * BagBase.java
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

import io.nut.base.equalizer.EqualsSame;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author franci
 * @param <E>
 */
public class BagBase<E> extends Bag<E>
{
    private volatile E[] empty;
    private final Map<E,List<E>> data;
    //this set is used to verify if an object is the same and not just equal,
    //do not try to inherit HashMap and add a Set, it takes the same amount of time
    private final Set<EqualsSame<E>> set;

    public BagBase(boolean skipSame)
    {
        this.data = new HashMap<>();
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
        E[] empty = this.empty;
        List<E> list = this.data.get(e);
        if(empty==null && list!=null)
        {
            empty = this.empty = (E[]) Array.newInstance(list.get(0).getClass(), 0);
        }
        return list!=null ? list.toArray(empty): null;
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
        E[] empty = this.empty;
        ArrayList<E[]> items = new ArrayList<>();
        for( List<E> list : this.data.values())
        {
            if(empty==null)
            {
                empty = this.empty = (E[]) Array.newInstance(list.get(0).getClass(), 0);
            }
            E[] sublist = list.toArray(empty);
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
