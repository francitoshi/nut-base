/*
 *  CounterSet.java
 *
 *  Copyright (c) 2012-2025 francitoshi@gmail.com
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
package io.nut.base.collections;

import io.nut.base.util.Sorts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author franci
 * @param <E>
 */
public class CounterSet<E> implements Set<E>
{
    int count=0;
    final Map<E,AtomicInteger> map;

    public CounterSet()
    {
        this(false);
    }
    public CounterSet(boolean weak)
    {
        this.map = weak ? new WeakHashMap<>() : new HashMap<>();
    }

    public CounterSet(int initialCapacity, boolean weak) 
    {
        this.map = weak ? new WeakHashMap<>(initialCapacity) : new HashMap<>(initialCapacity);
    }
    public CounterSet(int initialCapacity) 
    {
        this(initialCapacity, false);
    }

    public CounterSet(int initialCapacity, float loadFactor, boolean weak) 
    {
        this.map = weak ? new WeakHashMap<>(initialCapacity, loadFactor) : new HashMap<>(initialCapacity, loadFactor);
    }
    public CounterSet(int initialCapacity, float loadFactor) 
    {
        this(initialCapacity, loadFactor, false);
    }
    
    public int count(E e)
    {
        AtomicInteger counter = map.get(e);
        return (counter!=null)? counter.get() : 0 ;
    }
    @Override
    public int size()
    {
        return map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator()
    {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray()
    {
        return map.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return map.keySet().toArray(a);
    }
    public E[] toArray(E[] a, int[] c)
    {
        int i=0;
        for( Entry<E, AtomicInteger> kv : map.entrySet())
        {
            a[i] = kv.getKey();
            c[i] = kv.getValue().get();
            i++;
        }
        return a;
    }
    public E[] toSortedArray(E[] e, boolean down)
    {
        ArrayList<Item<E>> list = new ArrayList<>();
        for( Entry<E, AtomicInteger> kv : map.entrySet())
        {
            list.add(new Item<>(kv.getKey(), kv.getValue().get()));
        }
        Item<E>[] items = list.toArray(new Item[0]);
        Arrays.sort(items);
        if(down) Sorts.reverse(items);
        
        ArrayList<E> result = new ArrayList<>(items.length);
        for(Item<E> item : items)
        {
            result.add(item.key);
        }
        return result.toArray(e);
    }
    
    public static class Item<E> implements Comparable<Item<E>>
    {
        public final E key;
        public final int count;
        public Item(E key, int count)
        {
            this.key = key;
            this.count = count;
        }
        @Override
        public int compareTo(Item<E> other)
        {
            if(this.count<other.count)
            {
                return -1;
            }
            if(this.count>other.count)
            {
                return +1;
            }
            return 0;
        }
    }

    public boolean add(E e, int value)
    {
        count++;
        AtomicInteger counter = map.get(e);
        if(counter!=null)
        {
            counter.addAndGet(value);
            return false;
        }
        map.put(e,new AtomicInteger(value));
        return true;
    }
    @Override
    public boolean add(E e)
    {
        return add(e, 1);
    }

    @Override
    public boolean remove(Object o)
    {
        count--;
        AtomicInteger counter = map.get(o);
        if(counter==null)
        {
            return false;
        }
        if(counter.get()==1)
        {
            map.remove(o);
        }
        else
        {
            counter.addAndGet(-1);
        }
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        for(E e : c)
        {
            this.add(e);
        }
        return true;
    }

    public boolean retainAll(Collection<?> c)
    {
        boolean ret=false;
        for(E k:map.keySet())
        {
            if(!c.contains(k))
            {
                count--;
                map.remove(k);
                ret = true;
            }
        }
        return ret;
    }
    
    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean ret=false;
        for(Object o: c)
        {
            count--;
            ret |= (map.remove(o)!=null);
        }
        return ret;
    }

    public void clear()
    {
        count=0;
        map.clear();
    }

    public double getSuccess()
    {
        return (double)this.count/(double)this.size();
    }

}
