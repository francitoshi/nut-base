/*
 *  ActorMulti.java
 *
 *  Copyright (C) 2009-2023 francitoshi@gmail.com
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
package io.nut.base.util.concurrent.actor;

import io.nut.base.util.concurrent.Value;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import io.nut.base.util.concurrent.Filter;
import io.nut.base.util.concurrent.ASyncValue;

/**
 *
 * @author franci
 */
public class ActorMulti<M,R> implements ActorBase<M,R>
{
    private final AtomicInteger index = new AtomicInteger();
    private final ActorSingle<M,R> actors[];
    private final BlockingQueue<Runnable> queue;
    private final ActorPool pool;
    
    public ActorMulti(Filter<M,R> filter,ActorPool pool,int threads)
    {
        assert filter!=null:"attempt to use a null filter";
        if(threads==0)
        {
            threads = ActorPool.CORES;
        }
        this.queue  = new LinkedBlockingQueue<>(ActorSingle.QUEUE_SIZE);
        this.pool   = (pool!=null)? pool:ActorSingle.shared;
        this.actors = new ActorSingle[threads];
        for(int i=0;i<actors.length;i++)
        {
            actors[i] = new ActorSingle<>(filter, pool, queue);
        }
    }

    @Override
    public ASyncValue<R> send(final M m) throws InterruptedException
    {
        int off = index.getAndIncrement();
        for(int i=0;i<actors.length;i++)
        {
            int j = (i+off)%actors.length;
            if(!actors[j].isRunning())
            {
                return actors[j].send(m);
            }
        }
        return actors[off%actors.length].send(m);
    }
    @Override
    public Value<R> send(Value<M> m) throws InterruptedException
    {
        int off = index.getAndIncrement();
        for(int i=0;i<actors.length;i++)
        {
            int j = (i+off)%actors.length;
            if(!actors[j].isRunning())
            {
                return actors[j].send(m);
            }
        }
        return actors[off%actors.length].send(m);
    }

    @Override
    public void execute(Runnable task) throws InterruptedException
    {
        pool.execute(task);
    }



}

