/*
 *  PipeLine.java
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class PipeLine<M,R> implements Pipe<M,R>, Filter<M,R>, Runnable
{
    final private boolean needFlush;
    final private Pipe<M,?> first;
    final private Pipe<?,R> last;
    final private Pipe[] links;
    private final AtomicBoolean firstLock = new AtomicBoolean(false); //controla si hay un hilo gloton
    private volatile boolean pendingData = false;
    private volatile boolean runningTask = false;

    public PipeLine()
    {
        Pipe<M,R> pipe = new PipeActor<>(this);
        this.first = pipe;
        this.last  = pipe;
        this.links = new Pipe[1];
        this.links[0] = first;
        this.needFlush= false;
    }
    public PipeLine(int threads)
    {
        Pipe<M,R> pipe = new PipeActor<>(threads, this);
        this.first = pipe;
        this.last  = pipe;
        this.links = new Pipe[1];
        this.links[0] = first;
        this.needFlush= false;
    }
    public PipeLine(Filter<M,R> filter)
    {
        Pipe<M,R> pipe = new PipeActor<>(filter);
        this.first = pipe;
        this.last  = pipe;
        this.links = new Pipe[1];
        this.links[0] = first;
        this.needFlush= false;
    }
    protected <C> PipeLine(PipeLine<M,C> ini, PipeLine<C,R> end)
    {
        assert ini!=end:"ini!=end";

        this.first = ini.first;
        this.last  = end.last;
        this.links = new Pipe<?,?>[ini.links.length+end.links.length];
        for(int i=0;i<ini.links.length;i++)
        {
            this.links[i] = ini.links[i];
        }
        for(int i=0;i<end.links.length;i++)
        {
            this.links[ini.links.length+i] = end.links[i];
        }
        needFlush = true;
    }
    protected <C> PipeLine(PipeLine<M,C> ini, Pipe<C,R> end)
    {
        assert ini!=end:"ini!=end";

        this.first = ini.first;
        this.last  = end;
        this.links = new Pipe<?,?>[ini.links.length+1];
        for(int i=0;i<ini.links.length;i++)
        {
            this.links[i] = ini.links[i];
        }
        this.links[ini.links.length] = end;
        needFlush = true;
    }
    protected <C> PipeLine(Pipe<M,C> ini, PipeLine<C,R> end)
    {
        assert ini!=end:"ini!=end";

        this.first = ini;
        this.last  = end.last;
        this.links = new Pipe<?,?>[end.links.length+1];
        this.links[0] = ini;
        for(int i=0;i<end.links.length;i++)
        {
            this.links[i+1] = end.links[i];
        }
        needFlush = true;
    }
    protected <C> PipeLine(Pipe<M,C> ini, Pipe<C,R> end)
    {
        assert ini!=end:"ini!=end";

        this.first = ini;
        this.last  = end;
        this.links = new Pipe<?,?>[2];
        this.links[0] = ini;
        this.links[1] = end;
        needFlush = true;
    }

    @Override
    public void put(M a) throws InterruptedException
    {
        first.put(a);
        if(needFlush)
        {
            pendingData = true;
            if(!runningTask)
            {
                execute(this);
            }
        }
    }

    @Override
    public R take() throws InterruptedException, ExecutionException
    {
        return last.take();
    }
    @Override
    public R poll() throws InterruptedException, ExecutionException
    {
        return last.poll();
    }

    @Override
    public void close() throws InterruptedException
    {
        first.close();
        if(needFlush)
        {
            pendingData = true;
            if(!runningTask)
            {
                execute(this);
            }
        }
    }

    @Override
    public boolean isAlive()
    {
        return last.isAlive();
    }

    @Override
    public void execute(Runnable task) throws InterruptedException
    {
        first.execute(task);
    }
    public <T> PipeLine<M,T> link(PipeLine<R,T> next)
    {
        return new PipeLine<>(this, next);
    }
    public <T> PipeLine<M,T> link(Pipe<R,T> next)
    {
        return new PipeLine<>(this, next);
    }
    @Override
    public R filter(M a)
    {
        return null;
    }

    @Override
    public void run()
    {
        if(!firstLock.compareAndSet(false, true))
            return;
        try
        {
            boolean loop=true;
            while(loop || pendingData)
            {
                pendingData = false;
                runningTask = true;
                try
                {
                    loop = flush();
                }
                catch (InterruptedException | ExecutionException ex)
                {
                    pendingData = true;
                    Logger.getLogger(PipeLine.class.getName()).log(Level.SEVERE, null, ex);
                }
                finally
                {
                    runningTask = false;
                }
            }
        }
        finally
        {
            firstLock.set(false);
        }
    }
    private boolean flush() throws InterruptedException, ExecutionException
    {
        boolean keep = false;
        for(int i=0;i<links.length-1;i++)
        {
            boolean eager = (i == (links.length-2) );
            Object val=this;
            while(val!=null && (eager || links[i].size()>=links[i+1].size()) )
            {
                val = links[i].poll();
                if (val != null)
                {
                    keep = true;
                    links[i + 1].put(val);
                }
                else if (!links[i].isAlive())
                {
                    links[i + 1].close();
                }
            }
        }
        return keep;
    }

    public int size()
    {
        return last.size();
    }
}
