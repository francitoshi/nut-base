/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
 * A {@link Set} that tracks the occurrence count of each element.
 * <p>
 * Each element is stored at most once in the underlying map, but an
 * {@link AtomicInteger} counter records how many times the element has
 * been added. The {@link #count(Object)} method returns this counter,
 * while {@link #size()} returns the number of <em>distinct</em> elements.
 * {@link #getSuccess()} returns the average count per element
 * ({@code count / size}).
 * <p>
 * The optional {@code weak} constructor switches the backing map to a
 * {@link WeakHashMap}, allowing entries to be garbage-collected when no
 * longer referenced elsewhere.
 *
 * @param <E> the type of elements maintained by this set
 * @author francitoshi@gmail.com
 */
public class CounterSet<E> implements Set<E>
{
    int count=0;
    final Map<E,AtomicInteger> map;

    /**
     * Constructs a new, empty CounterSet backed by a {@link HashMap}.
     */
    public CounterSet()
    {
        this(false);
    }
    /**
     * Constructs a new, empty CounterSet.
     *
     * @param weak if {@code true} the backing map is a {@link WeakHashMap},
     *             allowing entries to be garbage-collected
     */
    public CounterSet(boolean weak)
    {
        this.map = weak ? new WeakHashMap<>() : new HashMap<>();
    }

    /**
     * Constructs a new, empty CounterSet with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the backing map
     * @param weak            if {@code true} the backing map is a {@link WeakHashMap}
     */
    public CounterSet(int initialCapacity, boolean weak) 
    {
        this.map = weak ? new WeakHashMap<>(initialCapacity) : new HashMap<>(initialCapacity);
    }
    /**
     * Constructs a new, empty CounterSet with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the backing map
     */
    public CounterSet(int initialCapacity) 
    {
        this(initialCapacity, false);
    }

    /**
     * Constructs a new, empty CounterSet with the specified initial capacity
     * and load factor.
     *
     * @param initialCapacity the initial capacity of the backing map
     * @param loadFactor      the load factor of the backing map
     * @param weak            if {@code true} the backing map is a {@link WeakHashMap}
     */
    public CounterSet(int initialCapacity, float loadFactor, boolean weak) 
    {
        this.map = weak ? new WeakHashMap<>(initialCapacity, loadFactor) : new HashMap<>(initialCapacity, loadFactor);
    }
    /**
     * Constructs a new, empty CounterSet with the specified initial capacity
     * and load factor.
     *
     * @param initialCapacity the initial capacity of the backing map
     * @param loadFactor      the load factor of the backing map
     */
    public CounterSet(int initialCapacity, float loadFactor) 
    {
        this(initialCapacity, loadFactor, false);
    }
    
    /**
     * Returns the occurrence count of the given element.
     *
     * @param e the element to query
     * @return the count, or {@code 0} if the element is not present
     */
    public int count(E e)
    {
        AtomicInteger counter = map.get(e);
        return (counter!=null)? counter.get() : 0 ;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return map.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o)
    {
        return map.containsKey(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator()
    {
        return map.keySet().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray()
    {
        return map.keySet().toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(T[] a)
    {
        return map.keySet().toArray(a);
    }
    /**
     * Populates two parallel arrays: one with the distinct elements and one
     * with their respective occurrence counts.
     *
     * @param a the array to fill with elements; must be large enough
     * @param c the array to fill with counts; must be large enough
     * @return {@code a}, filled with elements
     * @throws ArrayIndexOutOfBoundsException if either array is too small
     */
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
    /**
     * Returns a sorted array of elements ordered by their occurrence count.
     *
     * @param e   the array type template
     * @param down if {@code true} sort in descending order (highest count first);
     *             if {@code false} ascending
     * @return a sorted array of elements
     */
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
    
    /**
     * A key-count pair used by {@link CounterSet#toSortedArray}.
     *
     * @param <E> the element type
     */
    public static class Item<E> implements Comparable<Item<E>>
    {
        /** The element. */
        public final E key;
        /** The occurrence count. */
        public final int count;
        /**
         * @param key   the element
         * @param count its occurrence count
         */
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

    /**
     * Adds {@code value} to the occurrence count of {@code e}.
     * If the element is not yet present it is inserted with the given count.
     *
     * @param e     the element to add
     * @param value the value to add to the occurrence count
     * @return {@code true} if the element was newly inserted;
     *         {@code false} if it already existed
     */
    public boolean add(E e, int value)
    {
        AtomicInteger counter = map.get(e);
        if(counter!=null)
        {
            counter.addAndGet(value);
            return false;
        }
        count++;
        map.put(e,new AtomicInteger(value));
        return true;
    }
    /**
     * Increments the occurrence count of {@code e} by one.
     * If the element is not yet present it is inserted with a count of 1.
     *
     * @param e the element to add
     * @return {@code true} if the element was newly inserted;
     *         {@code false} if it already existed
     */
    @Override
    public boolean add(E e)
    {
        return add(e, 1);
    }

    /**
     * Decrements the occurrence count of {@code o} by one.
     * If the count reaches zero the element is removed from the set.
     *
     * @param o the element to decrement
     * @return {@code true} if the element was present;
     *         {@code false} if it was not in the set
     */
    @Override
    public boolean remove(Object o)
    {
        AtomicInteger counter = map.get(o);
        if(counter==null)
        {
            return false;
        }
        if(counter.get()==1)
        {
            count--;
            map.remove(o);
        }
        else
        {
            counter.addAndGet(-1);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        return map.keySet().containsAll(c);
    }

    /**
     * Increments the occurrence count of each element in {@code c} by one.
     * Elements not yet present are inserted with a count of 1.
     *
     * @param c the elements to add
     * @return {@code true} (as specified by {@link Set#addAll})
     */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        for(E e : c)
        {
            this.add(e);
        }
        return true;
    }

    /**
     * Retains only the elements that are contained in {@code c},
     * removing all others.
     *
     * @param c the collection containing elements to retain
     * @return {@code true} if this set changed as a result of the call
     */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean ret=false;
        ArrayList<E> toRemove = new ArrayList<>();
        for(E k:map.keySet())
        {
            if(!c.contains(k))
            {
                toRemove.add(k);
            }
        }
        for(E k:toRemove)
        {
            count--;
            map.remove(k);
            ret = true;
        }
        return ret;
    }
    
    /**
     * Removes the elements contained in {@code c}.
     *
     * @param c the collection of elements to remove
     * @return {@code true} if this set changed as a result of the call
     */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean ret=false;
        for(Object o: c)
        {
            if(map.remove(o)!=null)
            {
                count--;
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Removes all elements and resets the total count to zero.
     */
    @Override
    public void clear()
    {
        count=0;
        map.clear();
    }

    /**
     * Returns the average occurrence count per element ({@code count / size}).
     *
     * @return the mean count, or {@code 0.0} if the set is empty
     */
    public double getSuccess()
    {
        return (double)this.count/(double)this.size();
    }

}
