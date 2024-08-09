/*
 *  RoundRobin.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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
package io.nut.base.util;

import java.util.ArrayList;
import java.util.List;
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
        return new RoundRobinArray<>(e);
    }
    public static <E> RoundRobin<E> create(List<E> list)
    {
        if(list==null || list.isEmpty())
        {
            return new RoundRobinNull<>();
        }
        return new RoundRobinList<>(list);
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
            this.e = e.clone();
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
            this.list = new ArrayList<>(list);
            this.size = this.list.size();
        }
        @Override
        protected E get(int index)
        {
            return list.get(index % size);
        }
    }

    
}
