/*
 *  QueueBee.java
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
package io.nut.base.util.concurrent.hive;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author franci
 */
public abstract class QueueBee<M,R> extends Bee<M>
{
    private final BlockingQueue<R> tail;

    public QueueBee(int threads, Hive hive, BlockingQueue<R> tail)
    {
        super(threads, hive);
        this.tail = tail;
    }
    
    public QueueBee(Hive hive, int threads, int capacity)
    {
        this(threads, hive, new LinkedBlockingQueue<>(capacity));
    }
    
    public QueueBee(int threads, Hive hive)
    {
        this(threads, hive, new LinkedBlockingQueue<>());
    }

    public BlockingQueue<R> getTail()
    {
        return tail;
    }
    
    public R take() throws InterruptedException
    {
        return tail.take();
    }

    public R poll()
    {
        return tail.poll();
    }

    public R peek()
    {
        return tail.peek();
    }
            
    
}
