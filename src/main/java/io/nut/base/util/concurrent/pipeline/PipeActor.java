/*
 *  PipeActor.java
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
package io.nut.base.util.concurrent.pipeline;

import io.nut.base.util.concurrent.Filter;
import io.nut.base.util.concurrent.ASyncValue;
import io.nut.base.util.concurrent.Value;
import io.nut.base.util.concurrent.actor.Actor;
import io.nut.base.util.concurrent.actor.ActorPool;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author franci
 */
public class PipeActor<M,R> implements Pipe<M,R>
{
    private final Actor<M,R> actor;
    private final BlockingQueue<Value<R>> queue = new LinkedBlockingQueue<>();
    private volatile boolean alive = true;
    private final ASyncValue<R> poison = new ASyncValue<R>()
    {
        @Override
        protected R call()
        {
            return null;
        }
    };
    
    PipeActor(ActorPool pool,int threads,Filter<M, R> filter)
    {
        this.actor = new Actor(pool,threads,filter);
    }
    PipeActor(int threads,Filter<M, R> filter)
    {
        this.actor = new Actor(threads,filter);
    }
    PipeActor(Filter<M, R> filter)
    {
        this.actor = new Actor(1,filter);
    }

    @Override
    public void put(M m) throws InterruptedException
    {
        if (alive)
        {
            final Value<R> value = actor.send(m);
            queue.put(value);
        }
    }
    @Override
    public void close() throws InterruptedException
    {
        if (alive)
        {
            queue.put(poison);
        }
    }

    @Override
    public R take() throws InterruptedException, ExecutionException
    {
        return get(true);
    }
    @Override
    public R poll() throws InterruptedException, ExecutionException
    {
        return get(false);
    }
    private R get(boolean blocking) throws InterruptedException, ExecutionException
    {
        R ret;
        boolean keep;
        do
        {
            final Value<R> val = blocking?queue.take():queue.poll();
            if (val == poison)
            {
                alive = false;
                queue.put(val);
                return null;
            }
            ret = val!=null?val.get():null;
            keep= val!=null;
        }
        while(alive && ret==null && (keep||blocking));
        return ret;
    }

    @Override
    public boolean isAlive()
    {
        return alive;
    }

    @Override
    public void execute(Runnable task) throws InterruptedException
    {
        actor.execute(task);
    }

    @Override
    public int size()
    {
        return queue.size();
    }
}
