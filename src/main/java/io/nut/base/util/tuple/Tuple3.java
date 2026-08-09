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
 * A tuple of 3 elements.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @author franci
 * @since 1.8
 */
public class Tuple3<A, B, C> implements Tuple, Comparable<Tuple3<A, B, C>>
{
    private static final long serialVersionUID = 1L;

    private final A a1;
    private final B b2;
    private final C c3;

    public Tuple3(A a1, B b2, C c3)
    {
        this.a1 = a1;
        this.b2 = b2;
        this.c3 = c3;
    }

    public A a1()
    {
        return a1;
    }

    public B b2()
    {
        return b2;
    }

    public C c3()
    {
        return c3;
    }

    @Override
    public int arity()
    {
        return 3;
    }

    @Override
    public Object[] toArray()
    {
        return new Object[] { a1(), b2(), c3() };
    }

    @Override
    public List<Object> toList()
    {
        return Arrays.asList(toArray());
    }

    public <U1, U2, U3> Tuple3<U1, U2, U3> map(
        Function<? super A, ? extends U1> f1,
        Function<? super B, ? extends U2> f2,
        Function<? super C, ? extends U3> f3)
    {
        return new Tuple3<>(f1.apply(a1()), f2.apply(b2()), f3.apply(c3()));
    }

    public <U> Tuple3<U, B, C> map1(Function<? super A, ? extends U> mapper)
    {
        return new Tuple3<>(mapper.apply(a1()), b2(), c3());
    }

    public <U> Tuple3<A, U, C> map2(Function<? super B, ? extends U> mapper)
    {
        return new Tuple3<>(a1(), mapper.apply(b2()), c3());
    }

    public <U> Tuple3<A, B, U> map3(Function<? super C, ? extends U> mapper)
    {
        return new Tuple3<>(a1(), b2(), mapper.apply(c3()));
    }

    @Override
    public int compareTo(Tuple3<A, B, C> that)
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
        res = Tuple.compare(this.b2(), that.b2());
        if (res != 0)
        {
            return res;
        }
        return Tuple.compare(this.c3(), that.c3());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(a1(), b2(), c3());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || !(obj instanceof Tuple3))
        {
            return false;
        }
        Tuple3<?, ?, ?> that = (Tuple3<?, ?, ?>) obj;
        return Objects.equals(this.a1(), that.a1()) &&
               Objects.equals(this.b2(), that.b2()) &&
               Objects.equals(this.c3(), that.c3());
    }

    @Override
    public String toString()
    {
        return "(" + a1() + ", " + b2() + ", " + c3() + ")";
    }

    static final class LazyTuple3<A, B, C> extends Tuple3<A, B, C>
    {
        private static final long serialVersionUID = 1L;

        private final Lazy<A> a1;
        private final Lazy<B> b2;
        private final Lazy<C> c3;

        LazyTuple3(Supplier<? extends A> a1Supplier, Supplier<? extends B> b2Supplier, Supplier<? extends C> c3Supplier)
        {
            super(null, null, null);
            this.a1 = new Lazy<>(a1Supplier::get);
            this.b2 = new Lazy<>(b2Supplier::get);
            this.c3 = new Lazy<>(c3Supplier::get);
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

        @Override
        public C c3()
        {
            return c3.get();
        }

        private Object writeReplace()
        {
            return new Tuple3<>(a1(), b2(), c3());
        }
    }
}
