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
import java.util.function.Predicate;

/**
 * A FilterBee is a Bee&lt;T&gt; that, like {@link PipeBee}, follows
 * Continuation-Passing Style: it never transforms the message, but
 * tests it with a {@link Predicate}&lt;T&gt; and only forwards
 * ("sends") it to the next stage when the predicate is satisfied.
 * Messages that don't match the predicate are simply dropped.
 * <p>
 * The "next" stage can be another {@code PipeBee<T,S>}, a plain
 * {@code Bee<T>}, or any other {@code Sendable<T>}, since FilterBee
 * doesn't change the message type at all.
 *
 * @param <T> the type of messages this FilterBee receives, tests and
 *            may forward unchanged
 */
public class FilterBee<T> extends Bee<T>
{
    private final Predicate<T> predicate;
    protected volatile Sendable<T> next;

    public FilterBee(int threads, Hive hive, int queueSize, Predicate<T> predicate)
    {
        super(threads, hive, queueSize);
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
    }

    public FilterBee(int threads, Hive hive, Predicate<T> predicate)
    {
        super(threads, hive);
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
    }

    public FilterBee(Hive hive, Predicate<T> predicate)
    {
        super(hive);
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
    }

    public FilterBee(int threads, Predicate<T> predicate)
    {
        super(threads);
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
    }

    public FilterBee(Predicate<T> predicate)
    {
        super();
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
    }

    /**
     * Links this filter to the next stage of the chain (the continuation),
     * invoked only for messages that pass the predicate. The next stage is
     * returned as-is, so calls can be fluently chained:
     * {@code filterA.linkTo(pipeB).linkTo(bee);}
     *
     * @param next the next Sendable&lt;T&gt; that will receive the
     *             messages that pass the predicate
     * @return the same {@code next} instance passed in, typed as given,
     *         so the next {@code linkTo} call can be chained on it
     */
    public <S extends Sendable<T>> S linkTo(S next)
    {
        this.next = Objects.requireNonNull(next, "next must not be null");
        return next;
    }

    /**
     * @return the next stage in the chain, or null if none is linked
     */
    protected Sendable<T> getNext()
    {
        return next;
    }

    @Override
    protected void receive(T m)
    {
        if (predicate.test(m))
        {
            Sendable<T> n = this.next;
            if (n != null)
            {
                n.send(m);
            }
        }
    }

    @Override
    public FilterBee<T> shutdown()
    {
        return (FilterBee<T>) super.shutdown();
    }

    @Override
    public FilterBee<T> shutdown(boolean onlyWhenEmpty)
    {
        return (FilterBee<T>) super.shutdown(onlyWhenEmpty);
    }

}
