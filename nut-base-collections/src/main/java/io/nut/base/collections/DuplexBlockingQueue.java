/*
 *  DuplexBlockingQueue.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link BlockingQueue} extension of {@link DuplexQueue} that adds blocking
 * insertion and retrieval operations. Blocking reads are delegated to the
 * {@code read} queue and blocking writes to the {@code write} queue.
 *
 * @param <E> the type of elements held in this queue
 */
public class DuplexBlockingQueue<E> extends DuplexQueue<E> implements BlockingQueue<E>
{
    private final BlockingQueue<E> read;
    private final BlockingQueue<E> write;

    /**
     * Constructs a {@code DuplexBlockingQueue} backed by the given blocking read and write queues.
     *
     * @param read  the queue used for all retrieval operations; must not be {@code null}
     * @param write the queue used for all insertion operations; must not be {@code null}
     */
    public DuplexBlockingQueue(BlockingQueue<E> read, BlockingQueue<E> write)
    {
        super(read, write);
        this.read = read;
        this.write = write;
    }

    /** Inserts the element into the write queue, blocking until space is available. */
    @Override
    public void put(E e) throws InterruptedException
    {
        write.put(e);
    }

    /**
     * Inserts the element into the write queue, waiting up to the specified timeout.
     *
     * @return {@code true} if the element was added, {@code false} if the timeout elapsed
     */
    @Override
    public boolean offer(E e, long l, TimeUnit tu) throws InterruptedException
    {
        return write.offer(e, l, tu);
    }

    /** Retrieves and removes the head of the read queue, blocking until an element is available. */
    @Override
    public E take() throws InterruptedException
    {
        return read.take();
    }

    /**
     * Retrieves and removes the head of the read queue, waiting up to the specified timeout.
     *
     * @return the head element, or {@code null} if the timeout elapsed
     */
    @Override
    public E poll(long l, TimeUnit tu) throws InterruptedException
    {
        return read.poll(l, tu);
    }

    /** Drains all available elements from the read queue into the given collection. */
    @Override
    public int drainTo(Collection<? super E> clctn)
    {
        return read.drainTo(clctn);
    }

    /** Drains at most {@code i} elements from the read queue into the given collection. */
    @Override
    public int drainTo(Collection<? super E> clctn, int i)
    {
        return read.drainTo(clctn, i);
    }

    /** Returns the number of additional elements the write queue can accept without blocking. */
    @Override
    public int remainingCapacity()
    {
        return write.remainingCapacity();
    }
}
