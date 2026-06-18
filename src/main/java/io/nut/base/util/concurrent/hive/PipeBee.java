/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import java.util.Objects;
import java.util.function.Function;

/**
 * A Pipe is a Bee&lt;T&gt; that, instead of doing some final side effect with
 * the message it receives, transforms it with a {@link Function}&lt;T,R&gt;
 * and forwards ("sends") the result to the next stage of the chain.
 * <p>
 * This is Continuation-Passing Style (CPS): {@link #receive(Object)} never
 * returns the transformed value to a caller; it computes it and explicitly
 * invokes the continuation -- the {@code next} stage's {@link Sendable#send}
 * -- to carry on the computation. The "next" stage can be another
 * {@code Pipe<R,S>} (which will keep transforming/forwarding) or a plain
 * {@code Bee<R>} (which will consume the value, e.g. printing it), since
 * both are {@code Sendable<R>}.
 */
public class PipeBee<T,R> extends Bee<T>
{
    private final Function<T,R> function;
    protected volatile Sendable<R> next;

    public PipeBee(int threads, Hive hive, int queueSize, Function<T,R> function)
    {
        super(threads, hive, queueSize);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    public PipeBee(int threads, Hive hive, Function<T,R> function)
    {
        super(threads, hive);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    public PipeBee(Hive hive, Function<T,R> function)
    {
        super(hive);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    public PipeBee(int threads, Function<T,R> function)
    {
        super(threads);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    public PipeBee(Function<T,R> function)
    {
        super();
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    /**
     * Links this pipe to the next stage of the chain (the continuation).
     * The next stage is returned as-is, so calls can be fluently chained:
     * {@code pipeA.linkTo(pipeB).linkTo(pipeC).linkTo(bee);}
     *
     * @param next the next Pipe&lt;R,S&gt; or Bee&lt;R&gt; that will receive
     *             the transformed values
     * @return the same {@code next} instance passed in, typed as given,
     *         so the next {@code linkTo} call can be chained on it
     */
    public <S extends Sendable<R>> S linkTo(S next)
    {
        this.next = Objects.requireNonNull(next, "next must not be null");
        return next;
    }

    /**
     * @return the next stage in the chain, or null if none is linked
     */
    protected Sendable<R> getNext()
    {
        return next;
    }

    @Override
    protected void receive(T m)
    {
        R r = function.apply(m);
        Sendable<R> n = this.next;
        if(n != null)
        {
            n.send(r);
        }
    }

    @Override
    public PipeBee<T,R> shutdown()
    {
        return (PipeBee<T,R>) super.shutdown();
    }

    @Override
    public PipeBee<T,R> shutdown(boolean onlyWhenEmpty)
    {
        return (PipeBee<T,R>) super.shutdown(onlyWhenEmpty);
    }

}
