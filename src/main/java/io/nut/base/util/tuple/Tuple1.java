/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

import io.nut.base.util.concurrent.Lazy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A tuple of 1 element.
 *
 * @param <A> the type of the first element
 * @author franci
 * @since 1.8
 */
public class Tuple1<A> implements Tuple, Comparable<Tuple1<A>>
{
    private static final long serialVersionUID = 1L;

    private final A a1;

    public Tuple1(A a1)
    {
        this.a1 = a1;
    }

    public A a1()
    {
        return a1;
    }

    @Override
    public int arity()
    {
        return 1;
    }

    @Override
    public Object[] toArray()
    {
        return new Object[] { a1() };
    }

    @Override
    public List<Object> toList()
    {
        return Arrays.asList(toArray());
    }

    public <U> Tuple1<U> map(Function<? super A, ? extends U> mapper)
    {
        return new Tuple1<>(mapper.apply(a1()));
    }

    public <U> U apply(Function<? super A, ? extends U> f)
    {
        return f.apply(a1());
    }

    @Override
    public int compareTo(Tuple1<A> that)
    {
        if (that == null)
        {
            return 1;
        }
        return Tuple.compare(this.a1(), that.a1());
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(a1());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || !(obj instanceof Tuple1))
        {
            return false;
        }
        Tuple1<?> that = (Tuple1<?>) obj;
        return Objects.equals(this.a1(), that.a1());
    }

    @Override
    public String toString()
    {
        return "(" + a1() + ")";
    }

    static final class LazyTuple1<A> extends Tuple1<A>
    {
        private static final long serialVersionUID = 1L;

        private final Lazy<A> a1;

        LazyTuple1(Supplier<? extends A> a1Supplier)
        {
            super(null);
            this.a1 = new Lazy<>(a1Supplier::get);
        }

        @Override
        public A a1()
        {
            return a1.get();
        }

        private Object writeReplace()
        {
            return new Tuple1<>(a1());
        }
    }
}
