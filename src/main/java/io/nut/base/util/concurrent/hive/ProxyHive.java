/*
 *  ProxyHive.java
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

/**
 *
 * @author franci
 */
public class ProxyHive extends Hive implements AutoCloseable
{
    private volatile Hive hive;

    public ProxyHive()
    {
        super(null);
    }

    public void setHive(Hive hive)
    {
        this.hive = hive;
    }

    @Override
    void submit(Runnable task)
    {
        hive.submit(task);
    }

    @Override
    protected void terminated()
    {
        hive.terminated();
    }

    @Override
    public void shutdown()
    {
        hive.shutdown();
    }

    @Override
    public boolean isShutdown()
    {
        return hive.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        return hive.isTerminated();
    }

    @Override
    public boolean awaitTermination(int millis) throws InterruptedException
    {
        return hive.awaitTermination(millis);
    }

    @Override
    public void shutdownAndAwaitTermination(Bee<?>... bees)
    {
        hive.shutdownAndAwaitTermination(bees);
    }

    @Override
    public int getCorePoolSize()
    {
        return hive.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize()
    {
        return hive.getMaximumPoolSize();
    }

    @Override
    public void setCorePoolSize(int i)
    {
        hive.setCorePoolSize(i);
    }

    @Override
    public void setMaximumPoolSize(int i)
    {
        hive.setMaximumPoolSize(i);
    }

    @Override
    public void close()
    {
        hive.close();
    }
    
}
