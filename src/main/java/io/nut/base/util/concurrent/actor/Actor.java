/*
 *  Actor.java
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

import io.nut.base.util.concurrent.Filter;
import io.nut.base.util.concurrent.Value;


/**
 *
 * @author franci
 */
public class Actor<M,R> implements ActorBase<M,R> , Filter<M,R>
{
    private static volatile boolean forceSync = false;

    private final ActorBase<M,R> actor;

    private <T> Actor(ActorBase<M,T> head,ActorBase<T,R> tail)
    {
        actor = new ActorLink<>(head,tail);
    }
    public Actor(ActorPool pool,int threads,Filter<M,R> filter)
    {
        filter = filter==null?this:filter;
        if(forceSync||threads==0)
        {
            actor = new ActorSync<>(filter);
        }
        else if(threads==1)
        {
            actor = new ActorSingle<>(filter, pool);
        }
        else if(threads>1)
        {
            actor = new ActorMulti<>(filter, pool, threads);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    public Actor(ActorPool pool,Filter<M,R> filter)
    {
        this(pool,ActorPool.CORES,filter);
    }
    public Actor(ActorPool pool)
    {
        this(pool,ActorPool.CORES,null);
    }
    public Actor(int threads,Filter<M,R> filter)
    {
        this(null,threads,filter);
    }
    public Actor(int threads)
    {
        this(null,threads,null);
    }
    public Actor(Filter<M,R> filter)
    {
        this(null,ActorPool.CORES,filter);
    }
    public Actor()
    {
        this(null,ActorPool.CORES,null);
    }

    @Override
    public Value<R> send(final M m) throws InterruptedException
    {
        return actor.send(m);
    }
    @Override
    public Value<R> send(Value<M> m) throws InterruptedException
    {
        return actor.send(m);
    }

    public static boolean isForceSync()
    {
        return forceSync;
    }

    public static void setForceSync(boolean forceSync)
    {
        Actor.forceSync = forceSync;
    }

    public <T> Actor<M,T> link(Actor<R,T> link)
    {
        return new Actor<>(actor, link);
    }

    @Override
    public void execute(Runnable task) throws InterruptedException
    {
        actor.execute(task);
    }

    @Override
    public R filter(M a)
    {
        return null;
    }
}
