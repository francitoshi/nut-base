/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author franci
 * @param <E>
 */
public abstract class RoundRobin<E>
{
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public static <E> RoundRobin<E> create(E[] e)
    {
        if(e==null || e.length==0)
        {
            return new RoundRobinNull<>();
        }
        return new RoundRobinArray<>(e.clone());
    }
    public static <E> RoundRobin<E> create(List<E> list)
    {
        if(list==null || list.isEmpty())
        {
            return new RoundRobinNull<>();
        }
        return new RoundRobinList<>(new ArrayList<>(list));
    }
    public static <E> RoundRobin<E> create(Set<E> set)
    {
        if(set==null || set.isEmpty())
        {
            return new RoundRobinNull<>();
        }
        return new RoundRobinList<>(new ArrayList<>(set));
    }
    
    protected abstract E get(int index);
    
    public E next()
    {
        return get(this.counter.getAndIncrement());
    }

    public int getCounter()
    {
        return counter.get();
    }

    private static class RoundRobinNull<E> extends RoundRobin<E>
    {
        @Override
        protected E get(int index)
        {
            return null;
        }
    }
    private static class RoundRobinArray<E> extends RoundRobin<E>
    {
        private final E[] e;
        
        public RoundRobinArray(E[] e)
        {
            this.e = e;
        }
        @Override
        protected E get(int index)
        {
            return this.e[index % this.e.length];
        }
    }
    private static class RoundRobinList<E> extends RoundRobin<E>
    {
        private final List<E> list;
        private final int size;
        
        public RoundRobinList(List<E> list)
        {
            this.list = list;
            this.size = this.list.size();
        }
        @Override
        protected E get(int index)
        {
            return list.get(index % size);
        }
    }

    
}
