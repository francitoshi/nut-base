/*
 *  ActorSingle.java
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

import io.nut.base.util.concurrent.ASyncFilter;
import io.nut.base.util.concurrent.ASyncLink;
import io.nut.base.util.concurrent.Filter;
import io.nut.base.util.concurrent.ASyncValue;
import io.nut.base.util.concurrent.Value;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class ActorSingle<M,R> implements ActorBase<M,R> , Runnable
{
    final static ActorPool shared = new ActorPool();
    
    public static final int QUEUE_SIZE = 640*1024;
    
    private final AtomicBoolean eagerLock = new AtomicBoolean(false); //controla si hay un hilo gloton
    private final BlockingQueue<Runnable> queue;
    private final ActorPool pool;
    private final Filter<M,R> filter;
    
    private volatile boolean pendingData = false;
    private volatile boolean runningTask = false;

    ActorSingle(Filter<M,R> filter,ActorPool pool,BlockingQueue<Runnable> queue)
    {
        this.queue  = (queue!=null) ? queue :new LinkedBlockingQueue<>(QUEUE_SIZE);
        this.pool   = (pool!=null)? pool:ActorSingle.shared;
        this.filter = filter;
    }
    public ActorSingle(Filter<M,R> filter,ActorPool pool)
    {
        this(filter,pool,null);
    }
    public ActorSingle(Filter<M,R> filter)
    {
        this(filter,null);
    }

    @Override
    public ASyncValue<R> send(final M m) throws InterruptedException
    {
        ASyncValue<R> task = new ASyncFilter<>(filter, m);
        queue.put(task);
        pendingData = true;
        if(!runningTask)
        {
            pool.execute(this);
        }
        return task;
    }
    @Override
    public Value<R> send(Value<M> m) throws InterruptedException
    {
        ASyncValue<R> task = new ASyncLink<>(filter, m);
        queue.put(task);
        pendingData = true;
        if(!runningTask)
        {
            pool.execute(this);
        }
        return task;
    }
    
    @Override
    public void execute(Runnable task) throws InterruptedException
    {
        pool.execute(task);
    }

    @Override
    public void run()
    {
        if(!eagerLock.compareAndSet(false, true))
            return;
        try
        {
            while(pendingData)
            {
                pendingData = false;
                runningTask = true;
                try
                {
                    Runnable task;
                    while ( (task = queue.poll()) != null)
                    {
                        task.run();
                    }
                }
                catch (Exception ex)
                {
                    pendingData = true;
                    Logger.getLogger(ActorSingle.class.getName()).log(Level.SEVERE, null, ex);
                }
                finally
                {
                    runningTask = false;
                }
            }
        }
        finally
        {
            eagerLock.set(false);
        }
    }

    public boolean isRunning()
    {
        return runningTask;
    }


}
