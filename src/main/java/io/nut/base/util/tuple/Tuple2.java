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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A tuple of 2 elements.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @author franci
 * @since 1.8
 */
public class Tuple2<A, B> implements Tuple, Comparable<Tuple2<A, B>>
{
    private static final long serialVersionUID = 1L;

    private final A a1;
    private final B b2;

    public Tuple2(A a1, B b2)
    {
        this.a1 = a1;
        this.b2 = b2;
    }

    public A a1()
    {
        return a1;
    }

    public B b2()
    {
        return b2;
    }

    @Override
    public int arity()
    {
        return 2;
    }

    @Override
    public Object[] toArray()
    {
        return new Object[] { a1(), b2() };
    }

    @Override
    public List<Object> toList()
    {
        return Arrays.asList(toArray());
    }

    public <U1, U2> Tuple2<U1, U2> map(Function<? super A, ? extends U1> f1, Function<? super B, ? extends U2> f2)
    {
        return new Tuple2<>(f1.apply(a1()), f2.apply(b2()));
    }

    public <U1, U2> Tuple2<U1, U2> map(BiFunction<? super A, ? super B, Tuple2<U1, U2>> mapper)
    {
        return mapper.apply(a1(), b2());
    }

    public <U> Tuple2<U, B> map1(Function<? super A, ? extends U> mapper)
    {
        return new Tuple2<>(mapper.apply(a1()), b2());
    }

    public <U> Tuple2<A, U> map2(Function<? super B, ? extends U> mapper)
    {
        return new Tuple2<>(a1(), mapper.apply(b2()));
    }

    public <U> U apply(BiFunction<? super A, ? super B, ? extends U> f)
    {
        return f.apply(a1(), b2());
    }

    @Override
    public int compareTo(Tuple2<A, B> that)
    {
        if (that == null)
        {
            return 1;
        }
        int res = Tuple.compare(this.a1(), that.a1());
        if (res != 0)
        {
            return res;
        }
        return Tuple.compare(this.b2(), that.b2());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(a1(), b2());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || !(obj instanceof Tuple2))
        {
            return false;
        }
        Tuple2<?, ?> that = (Tuple2<?, ?>) obj;
        return Objects.equals(this.a1(), that.a1()) && Objects.equals(this.b2(), that.b2());
    }

    @Override
    public String toString()
    {
        return "(" + a1() + ", " + b2() + ")";
    }

    static final class LazyTuple2<A, B> extends Tuple2<A, B>
    {
        private static final long serialVersionUID = 1L;

        private final Lazy<A> a1;
        private final Lazy<B> b2;

        LazyTuple2(Supplier<? extends A> a1Supplier, Supplier<? extends B> b2Supplier)
        {
            super(null, null);
            this.a1 = new Lazy<>(a1Supplier::get);
            this.b2 = new Lazy<>(b2Supplier::get);
        }

        @Override
        public A a1()
        {
            return a1.get();
        }

        @Override
        public B b2()
        {
            return b2.get();
        }

        private Object writeReplace()
        {
            return new Tuple2<>(a1(), b2());
        }
    }
}
