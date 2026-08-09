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
 * A tuple of 4 elements.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @param <D> the type of the fourth element
 * @author franci
 * @since 1.8
 */
public class Tuple4<A, B, C, D> implements Tuple, Comparable<Tuple4<A, B, C, D>>
{
    private static final long serialVersionUID = 1L;

    private final A a1;
    private final B b2;
    private final C c3;
    private final D d4;

    public Tuple4(A a1, B b2, C c3, D d4)
    {
        this.a1 = a1;
        this.b2 = b2;
        this.c3 = c3;
        this.d4 = d4;
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

    public D d4()
    {
        return d4;
    }

    @Override
    public int arity()
    {
        return 4;
    }

    @Override
    public Object[] toArray()
    {
        return new Object[] { a1(), b2(), c3(), d4() };
    }

    @Override
    public List<Object> toList()
    {
        return Arrays.asList(toArray());
    }

    public <U1, U2, U3, U4> Tuple4<U1, U2, U3, U4> map(
        Function<? super A, ? extends U1> f1,
        Function<? super B, ? extends U2> f2,
        Function<? super C, ? extends U3> f3,
        Function<? super D, ? extends U4> f4)
    {
        return new Tuple4<>(f1.apply(a1()), f2.apply(b2()), f3.apply(c3()), f4.apply(d4()));
    }

    public <U> Tuple4<U, B, C, D> map1(Function<? super A, ? extends U> mapper)
    {
        return new Tuple4<>(mapper.apply(a1()), b2(), c3(), d4());
    }

    public <U> Tuple4<A, U, C, D> map2(Function<? super B, ? extends U> mapper)
    {
        return new Tuple4<>(a1(), mapper.apply(b2()), c3(), d4());
    }

    public <U> Tuple4<A, B, U, D> map3(Function<? super C, ? extends U> mapper)
    {
        return new Tuple4<>(a1(), b2(), mapper.apply(c3()), d4());
    }

    public <U> Tuple4<A, B, C, U> map4(Function<? super D, ? extends U> mapper)
    {
        return new Tuple4<>(a1(), b2(), c3(), mapper.apply(d4()));
    }

    @Override
    public int compareTo(Tuple4<A, B, C, D> that)
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
        res = Tuple.compare(this.c3(), that.c3());
        if (res != 0)
        {
            return res;
        }
        return Tuple.compare(this.d4(), that.d4());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(a1(), b2(), c3(), d4());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || !(obj instanceof Tuple4))
        {
            return false;
        }
        Tuple4<?, ?, ?, ?> that = (Tuple4<?, ?, ?, ?>) obj;
        return Objects.equals(this.a1(), that.a1()) &&
               Objects.equals(this.b2(), that.b2()) &&
               Objects.equals(this.c3(), that.c3()) &&
               Objects.equals(this.d4(), that.d4());
    }

    @Override
    public String toString()
    {
        return "(" + a1() + ", " + b2() + ", " + c3() + ", " + d4() + ")";
    }

    static final class LazyTuple4<A, B, C, D> extends Tuple4<A, B, C, D>
    {
        private static final long serialVersionUID = 1L;

        private final Lazy<A> a1;
        private final Lazy<B> b2;
        private final Lazy<C> c3;
        private final Lazy<D> d4;

        LazyTuple4(Supplier<? extends A> a1Supplier, Supplier<? extends B> b2Supplier, Supplier<? extends C> c3Supplier, Supplier<? extends D> d4Supplier)
        {
            super(null, null, null, null);
            this.a1 = new Lazy<>(a1Supplier::get);
            this.b2 = new Lazy<>(b2Supplier::get);
            this.c3 = new Lazy<>(c3Supplier::get);
            this.d4 = new Lazy<>(d4Supplier::get);
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

        @Override
        public D d4()
        {
            return d4.get();
        }

        private Object writeReplace()
        {
            return new Tuple4<>(a1(), b2(), c3(), d4());
        }
    }
}
