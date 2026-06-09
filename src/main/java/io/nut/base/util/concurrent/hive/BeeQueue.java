/*
 *  BeeQueue.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
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
package io.nut.base.util.concurrent.hive;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * A {@link Queue} adapter that bridges the {@link Queue} interface with the
 * {@link Bee} actor model. Every element offered to this queue is immediately
 * forwarded to {@link Bee#send(Object)} for asynchronous processing and is
 * never retained in the queue itself.
 *
 * <p>Because elements are dispatched on arrival and never stored, this queue
 * is always logically empty: {@link #size()} always returns {@code 0},
 * {@link #peek()} and {@link #poll()} always return {@code null}, and
 * {@link #isEmpty()} always returns {@code true}.
 *
 * <p>This design is useful when existing APIs or frameworks expect a
 * {@link Queue} as a target but the actual processing should be delegated to
 * a {@link Bee} worker pool.
 *
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * Hive hive = new Hive(4);
 * Queue<String> queue = new BeeQueue<String>(2, hive) {
 *     @Override
 *     protected void receive(String message) {
 *         System.out.println("Processing: " + message);
 *     }
 * };
 *
 * queue.offer("task-1");        // dispatched immediately via send()
 * queue.add("task-2");          // same — delegates to offer()
 * queue.addAll(List.of("a","b","c")); // each element dispatched in order
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> thread safety guarantees are inherited
 * from {@link Bee}. Concurrent calls to {@link #offer(Object)} are safe.
 *
 * <p><strong>Unsupported read operations:</strong> {@link #remove()},
 * {@link #element()}, and any collection-read method will either throw
 * {@link NoSuchElementException} or return an empty/false result, consistent
 * with a permanently empty collection.
 *
 * @param <M> the type of messages this queue dispatches
 * @see Bee
 * @see Hive
 */
public abstract class BeeQueue<M> extends Bee<M> implements Queue<M>
{
    /**
     * Creates a {@code BeeQueue} with the specified number of worker threads
     * and the given {@link Hive} executor.
     *
     * @param threads the maximum number of concurrent worker threads;
     *                {@code 0} defaults to {@link Runtime#availableProcessors()}
     * @param hive    the {@link Hive} that executes worker tasks;
     *                {@code null} makes {@link #send} process messages synchronously
     */
    public BeeQueue(int threads, Hive hive)
    {
        super(threads, hive);
    }

    /**
     * Creates a {@code BeeQueue} with the specified number of worker threads
     * and no {@link Hive}. Messages will be processed synchronously on the
     * calling thread.
     *
     * @param threads the maximum number of concurrent worker threads;
     *                {@code 0} defaults to {@link Runtime#availableProcessors()}
     */
    public BeeQueue(int threads)
    {
        super(threads);
    }

    /**
     * Creates a {@code BeeQueue} with default settings: thread count defaults
     * to {@link Runtime#availableProcessors()} and no {@link Hive} is set,
     * so messages are processed synchronously.
     */
    public BeeQueue()
    {
        super();
    }

    public static <M> BeeQueue<M> of(Consumer<M> processor)
    {
        return new BeeQueue<M>()
        {
            @Override
            protected void receive(M m)
            {
                processor.accept(m);
            }
        };
    }

    public static <M> BeeQueue<M> of(Bee<M> bee)
    {
        return new BeeQueue<M>()
        {
            @Override
            protected void receive(M m)
            {
                bee.send(m);
            }
        };
    }    
    // -------------------------------------------------------------------------
    // Core dispatch — the only meaningful write operation
    // -------------------------------------------------------------------------

    /**
     * Dispatches the given message to {@link Bee#send(Object)} for processing
     * and returns immediately. The element is never stored in this queue.
     *
     * @param m the message to dispatch; must not be {@code null}
     * @return {@code true} if the message was accepted by {@link Bee#send},
     *         {@code false} if the Bee has been shut down or an error occurred
     */
    @Override
    public boolean offer(M m)
    {
        return send(m);
    }

    // -------------------------------------------------------------------------
    // Write operations that delegate to offer()
    // -------------------------------------------------------------------------

    /**
     * Dispatches the given message by delegating to {@link #offer(Object)}.
     *
     * @param m the message to dispatch
     * @return {@code true} if the message was accepted
     * @throws IllegalStateException never thrown by this implementation,
     *         but declared by the {@link Queue} contract
     */
    @Override
    public boolean add(M m)
    {
        return offer(m);
    }

    // -------------------------------------------------------------------------
    // Read operations — queue is always empty
    // -------------------------------------------------------------------------

    /**
     * Always returns {@code null} because this queue never retains elements.
     *
     * @return {@code null}
     */
    @Override
    public M poll()
    {
        return null;
    }

    /**
     * Always returns {@code null} because this queue never retains elements.
     *
     * @return {@code null}
     */
    @Override
    public M peek()
    {
        return null;
    }
    
    /**
     * Always throws {@link NoSuchElementException} because this queue is
     * always empty.
     *
     * @return never returns normally
     * @throws NoSuchElementException always
     */
    @Override
    public M remove()
    {
        throw new NoSuchElementException();
    }

    /**
     * Always throws {@link NoSuchElementException} because this queue is
     * always empty.
     *
     * @return never returns normally
     * @throws NoSuchElementException always
     */
    @Override
    public M element()
    {
        throw new NoSuchElementException();
    }

    // -------------------------------------------------------------------------
    // Collection state — permanently empty
    // -------------------------------------------------------------------------

    /**
     * Always returns {@code 0} because this queue never retains elements.
     *
     * @return {@code 0}
     */
    @Override
    public int size()
    {
        return 0;
    }

    /**
     * Always returns {@code true} because this queue never retains elements.
     *
     * @return {@code true}
     */
    @Override
    public boolean isEmpty()
    {
        return true;
    }

    /**
     * Always returns {@code false} because this queue never retains elements.
     *
     * @param o the object to look for
     * @return {@code false}
     */
    @Override
    public boolean contains(Object o)
    {
        return false;
    }

    /**
     * Returns {@code true} only if the given collection is empty, consistent
     * with this queue being permanently empty.
     *
     * @param c the collection to check
     * @return {@code true} if {@code c} is empty, {@code false} otherwise
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        return c.isEmpty();
    }

    /**
     * Always returns an empty iterator because this queue never retains elements.
     *
     * @return an empty {@link Iterator}
     */
    @Override
    public Iterator<M> iterator()
    {
        return Collections.emptyIterator();
    }

    /**
     * Always returns an empty array because this queue never retains elements.
     *
     * @return a new empty {@code Object[]}
     */
    @Override
    public Object[] toArray()
    {
        return new Object[0];
    }

    /**
     * Returns the given array unchanged (or a zero-length array of the same
     * type if the provided array has length zero) because this queue never
     * retains elements.
     *
     * @param <T> the component type of the array
     * @param a   the array to fill (returned as-is)
     * @return {@code a}
     */
    @Override
    public <T> T[] toArray(T[] a)
    {
        return a;
    }

    // -------------------------------------------------------------------------
    // Mutating collection operations — no-ops on a permanently empty queue
    // -------------------------------------------------------------------------

    /**
     * No-op. This queue never retains elements, so there is nothing to remove.
     *
     * @param o ignored
     * @return {@code false} always
     */
    @Override
    public boolean remove(Object o)
    {
        return false;
    }

   /**
     * Dispatches all elements in the given collection, in the order returned
     * by its {@link Iterator}, by calling {@link #offer(Object)} for each one.
     *
     * @param c the collection of messages to dispatch; must not be {@code null}
     * @return {@code true} always (the queue itself is unchanged, but this
     *         mirrors the {@link Collection#addAll} contract of returning
     *         {@code true} when the call produced side effects)
     */
    @Override
    public boolean addAll(Collection<? extends M> c)
    {
        c.forEach(this::offer);
        return true;
    }

    /**
     * No-op. This queue never retains elements, so there is nothing to remove.
     *
     * @param c ignored
     * @return {@code false} always
     */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        return false;
    }

    /**
     * No-op. This queue never retains elements, so there is nothing to retain.
     *
     * @param c ignored
     * @return {@code false} always
     */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        return false;
    }

    /**
     * No-op. This queue never retains elements, so there is nothing to clear.
     */
    @Override
    public void clear()
    {
    }
}
