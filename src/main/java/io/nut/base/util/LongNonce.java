/*
 * LongNonce.java
 *
 * Copyright (c) 2021-2023 francitoshi@gmail.com
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
package io.nut.base.util;

/**
 *
 * @author franci
 */
public abstract class LongNonce
{
    public static LongNonce getSequentialInstance(long start)
    {
        return new LongNonce.Sequential(start);
    }
    public static LongNonce getSequentialInstance()
    {
        return new LongNonce.Sequential(0L);
    }
    public static LongNonce getCurrentMillisInstance(long start)
    {
        return new LongNonce.CurrentMillis(start);
    }
    public static LongNonce getCurrentMillisInstance()
    {
        return new LongNonce.CurrentMillis(System.currentTimeMillis());
    }
    
    protected final Object lock = new Object();
    
    protected volatile long value;

    protected LongNonce(long value)
    {
        this.value = value;
    }

    public final long peek()
    {
        synchronized(lock)
        {
            return this.value;
        }
    }

    public abstract long get();
    
    private static class Sequential extends LongNonce
    {
        public Sequential(long value)
        {
            super(value);
        }
        @Override
        public long get()
        {
            synchronized(this.lock)
            {
                return ++this.value;
            }
        }
    }
    private static class CurrentMillis extends LongNonce
    {
        public CurrentMillis(long value)
        {
            super(value);
        }
        @Override
        public long get()
        {
            synchronized(this.lock)
            {
                return this.value=Math.max(this.value+1,System.currentTimeMillis());
            }
        }
    }
    
}
