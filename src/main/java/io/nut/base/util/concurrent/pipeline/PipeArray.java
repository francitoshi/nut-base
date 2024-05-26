/*
 *  PipeArray.java
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

import java.util.concurrent.ExecutionException;

/**
 *
 * @author franci
 */
public class PipeArray<M,R> implements Pipe<M[],R> , Filter<M,R>
{
    final private Pipe<M,R> pipe;

    public PipeArray()
    {
        this.pipe = new PipeActor<>(this);
    }

    @Override
    public R filter(M a)
    {
        return null;
    }

    @Override
    public void put(M[] a) throws InterruptedException
    {
        for(M item:a)
        {
            pipe.put(item);
        }
    }

    @Override
    public R take() throws InterruptedException, ExecutionException
    {
        return pipe.take();
    }
    @Override
    public R poll() throws InterruptedException, ExecutionException
    {
        return pipe.poll();
    }

    @Override
    public void close() throws InterruptedException
    {
        pipe.close();
    }

    @Override
    public boolean isAlive()
    {
        return pipe.isAlive();
    }

    @Override
    public void execute(Runnable task) throws InterruptedException
    {
        pipe.execute(task);
    }

    public <T> PipeLine<M[],T> link(PipeLine<R,T> pipe)
    {
        return new PipeLine<>(this, pipe);
    }
    public <T> PipeLine<M[],T> link(Pipe<R,T> pipe)
    {
        return new PipeLine<>(this, pipe);
    }

    @Override
    public int size()
    {
        return pipe.size();
    }
}
