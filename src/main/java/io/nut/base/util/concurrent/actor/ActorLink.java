/*
 *  ActorLink.java
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
 *
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.util.concurrent.Value;

/**
 *
 * @author franci
 */
public class ActorLink<M,T,R> implements ActorBase<M,R>
{
    private final ActorBase <M,T> head;
    private final ActorBase <T,R> tail;

    public ActorLink(ActorBase<M, T> head, ActorBase<T, R> tail)
    {
        this.head = head;
        this.tail = tail;
    }

    @Override
    public Value<R> send(M m) throws InterruptedException
    {
        return tail.send(head.send(m));
    }

    @Override
    public Value<R> send(Value<M> m) throws InterruptedException
    {
        return tail.send(head.send(m));
    }

    @Override
    public void execute(Runnable task) throws InterruptedException
    {
        head.execute(task);
    }
}
