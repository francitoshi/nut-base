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
 * An abstract base class that implements a Python-style Generator pattern in
 * Java.
 * <p>
 * This class allows you to produce a sequence of elements lazily using the
 * {@link #yield(Object)} method within the {@link #run()} implementation. The
 * generation happens in a separate background thread, allowing the consumer to
 * iterate over elements as they are produced.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * Generator<Integer> myGen = new Generator<>() 
 * {
 *     @Override
 *     public void run() 
 *     {
 *         for (int i = 0; i < 10; i++) 
 *         {
 *             yield(i);
 *         }
 *     }
 * };
 *
 * for (Integer val : myGen) 
 * {
 *     System.out.println(val);
 * }
 * }</pre>
 *
 * @param <E> The type of elements generated.
 */
public abstract class Generator<E> implements Iterable<E>, Iterator<E>, Runnable
{

    /**
     * Internal wrapper for elements to allow null values and sentinel markers
     * within the BlockingQueue.
     */
    static class Item<E>
    {

        final E e;

        public Item(E e)
        {
            this.e = e;
        }
    }

    /**
     * Sentinel object placed in the queue to signal that the generator has
     * finished producing elements.
     */
    static final Item POISON = new Item(null);
    
    private final String tag;
    private final BlockingQueue<Item<E>> queue;
    private volatile E nextElement = null;
    private volatile boolean terminated;

    /**
     * Creates a generator with a specific buffer capacity.
     *
     * @param capacity If 0, uses a {@link SynchronousQueue} (hand-off). If > 0,
     * uses a {@link LinkedBlockingQueue} with the specified capacity.
     */
    public Generator(int capacity)
    {
        this.tag = this.getClass().getName()+".Generator";
        this.queue = capacity==0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(capacity);
    }

    /**
     * Creates a generator with 0 capacity (synchronous hand-off). The producer
     * thread will block on {@code yield} until a consumer calls {@code next}.
     */
    public Generator()
    {
        this(0);
    }
    
    /**
     * Forces the generator to stop. Clears the internal queue and marks the
     * generator as terminated.
     */
    public void shutdown()
    {
        terminated = true;
        queue.clear();
    }
   
    /**
     * Checks if the generator has been shut down or has finished execution.
     *
     * @return true if terminated, false otherwise.
     */
    public boolean isTerminated()
    {
        return terminated;
    }
    
    /**
     * Produces an element and sends it to the consumer. This method blocks if
     * the internal queue is full.
     *
     * @param e The element to provide to the iterator.
     * @throws IllegalStateException if called after the generator has been
     * terminated.
     */
    protected final void yield(E e)
    {
        this.yield(new Item<>(e));
    }
    
    /**
     * Internal yield mechanism to handle the wrapping {@link Item}.
     */
    private void yield(Item<E> item)
    {
        try
        {
            if(terminated)
            {
                throw new IllegalStateException("Generator is terminated");
            }
            this.queue.put(item);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Generator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Starts the background generator thread and returns this instance as an
     * Iterator.
     *
     * @return An iterator over the generated elements.
     */
    @Override
    public final Iterator<E> iterator()
    {
        executeRunnable();
        return this;
    }

    /**
     * Spawns the background thread that executes the {@link #run()} method.
     * Automatically appends the {@link #POISON} pill when the run method
     * completes to signal the end of the stream.
     */
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
                }
                catch(IllegalStateException ex)
                {
                    Logger.getLogger(Generator.class.getName()).log(Level.INFO, "Generator explicitly stopped", ex);
                }
                finally
                {
                    try
                    {
                        queue.put(POISON);
                    }
                    catch (InterruptedException ex)
                    {
                        terminated = true;
                        Logger.getLogger(Generator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }, tag).start();
    }

    /**
     * Checks if there is another element available. This method blocks until
     * the background thread produces an item or finishes.
     *
     * @return true if an element is available, false if the generator has
     * finished.
     */
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

    /**
     * Retrieves the next element from the generator.
     *
     * @return The next element.
     * @throws NoSuchElementException if there are no more elements.
     */
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

    /**
     * A thread-safe version of the Generator. Synchronizes the {@code next()}
     * and {@code hasNext()} methods to allow multiple threads to consume from
     * the same generator.
     *
     * @param <E> The type of elements generated.
     */
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

    /**
     * Collects all generated elements into an Object array. Note: This will
     * block until the generator completes.
     *
     * @return An array containing all elements produced by this generator.
     */
    public Object[] toArray()
    {
        ArrayList<E> list = new ArrayList<>();
        for(E item : this)
        {
            list.add(item);
        }
        return list.toArray();
    }

    /**
     * Collects all generated elements into a typed array. Note: This will block
     * until the generator completes.
     *
     * @param array The array into which the elements are to be stored.
     * @return An array containing all elements produced by this generator.
     */
    public E[] toArray(E[] array)
    {
        ArrayList<E> list = new ArrayList<>();
        for(E item : this)
        {
            list.add(item);
        }
        return list.toArray(array);
    }
    
    /**
     * Resets the internal state of the generator.
     * <b>Note:</b> This does not restart the background thread if it is already
     * running.
     */
    public void reset()
    {
        this.terminated=false;
        this.queue.clear();
        this.nextElement = null;
    }
}
