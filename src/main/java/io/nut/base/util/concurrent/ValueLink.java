/*
 *  ValueLink.java
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
package io.nut.base.util.concurrent;

import java.util.concurrent.ExecutionException;

/**
 *
 * @author franci
 */
public class ValueLink<M,R> implements Value<R>
{
    private final Value<M> m;
    private final Filter<M,R> filter;

    public ValueLink(Filter<M, R> filter,Value<M> m)
    {
        this.m = m;
        this.filter = filter;
    }

    @Override
    public R get() throws InterruptedException, ExecutionException
    {
        return filter.filter(m.get());
    }
    
}
