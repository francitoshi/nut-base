/*
 *  Generator.java
 *
 *  Copyright (C) 2024-2025 francitoshi@gmail.com
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
package io.nut.base.util.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 * @param <E>
 */
public abstract class Generator<E> implements Iterable<E>, Iterator<E>, Runnable
{
    static class Item<E>
    {
        public Item(E e)
        {
            this.e = e;
        }
        final E e;
    }

    static final Item POISON = new Item(null);

    private final BlockingQueue<Item<E>> queue;
    private volatile E nextElement = null;
    private volatile boolean terminated;

    public Generator(int capacity)
    {
        this.queue = capacity==0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(capacity);
    }

    public Generator()
    {
        this(0);
    }
    
    public void shutdown()
    {
        terminated = true;
    }
   
    public boolean isTerminated()
    {
        return terminated;
    }
    
    protected final void yield(E e)
    {
        this.yield(new Item<>(e));
    }
    
    private void yield(Item<E> item)
    {
        try
        {
            if(terminated)
            {
                this.queue.offer(POISON);
                throw new IllegalStateException();
            }
            this.queue.put(item);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Generator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public final Iterator<E> iterator()
    {
        executeRunnable();
        return this;
    }

    private void executeRunnable()
    {
        new Thread(new Runnable() 
        {
            @Override
            public void run()
            {
                try
                {
                    Generator.this.run();
                    Generator.this.yield(POISON);
                }
                catch(IllegalStateException ex)
                {
                }
            }
        },"Generator").start();
    }

    @Override
    public boolean hasNext()
    {
        try
        {
            if(this.terminated)
            {
                return false;
            }
            if(this.nextElement != null)
            {
                return true;
            }
            
            Item<E> item = this.queue.take();

            if(POISON.equals(item))
            {
                terminated = true;
                return false;
            }
            this.nextElement = item.e;
            return true;            
        }
        catch (InterruptedException ex) 
        {
            Logger.getLogger(Generator.class.getName()).log(Level.SEVERE, null, ex);
            this.terminated=true;
            return false;
        }
    }

    @Override
    public E next()
    {
        if(this.nextElement == null && !hasNext())
        {
            throw new NoSuchElementException();
        }
        E currentElement = this.nextElement;
        this.nextElement = null;
        return currentElement;
    }

    public static abstract class Safe<E> extends Generator<E>
    {
        private final Object lock = new Object();
        
        @Override
        public E next()
        {
            synchronized(lock)
            {
                return super.next();
            }
        }

        @Override
        public boolean hasNext()
        {
            synchronized(lock)
            {
                return super.hasNext();                
            }
        }
        
    }

    public Object[] toArray()
    {
        ArrayList<E> list = new ArrayList();
        for(E item : this)
        {
            list.add(item);
        }
        return list.toArray();
    }

    public E[] toArray(E[] array)
    {
        ArrayList<E> list = new ArrayList();
        for(E item : this)
        {
            list.add(item);
        }
        return list.toArray(array);
    }
    
    
}
