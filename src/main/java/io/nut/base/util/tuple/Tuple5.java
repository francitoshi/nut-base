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
 * A tuple of 5 elements.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @param <D> the type of the fourth element
 * @param <E> the type of the fifth element
 * @author franci
 * @since 1.8
 */
public class Tuple5<A, B, C, D, E> implements Tuple, Comparable<Tuple5<A, B, C, D, E>>
{
    private static final long serialVersionUID = 1L;

    private final A a1;
    private final B b2;
    private final C c3;
    private final D d4;
    private final E e5;

    public Tuple5(A a1, B b2, C c3, D d4, E e5)
    {
        this.a1 = a1;
        this.b2 = b2;
        this.c3 = c3;
        this.d4 = d4;
        this.e5 = e5;
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

    public E e5()
    {
        return e5;
    }

    @Override
    public int arity()
    {
        return 5;
    }

    @Override
    public Object[] toArray()
    {
        return new Object[] { a1(), b2(), c3(), d4(), e5() };
    }

    @Override
    public List<Object> toList()
    {
        return Arrays.asList(toArray());
    }

    public <U1, U2, U3, U4, U5> Tuple5<U1, U2, U3, U4, U5> map(
        Function<? super A, ? extends U1> f1,
        Function<? super B, ? extends U2> f2,
        Function<? super C, ? extends U3> f3,
        Function<? super D, ? extends U4> f4,
        Function<? super E, ? extends U5> f5)
    {
        return new Tuple5<>(f1.apply(a1()), f2.apply(b2()), f3.apply(c3()), f4.apply(d4()), f5.apply(e5()));
    }

    public <U> Tuple5<U, B, C, D, E> map1(Function<? super A, ? extends U> mapper)
    {
        return new Tuple5<>(mapper.apply(a1()), b2(), c3(), d4(), e5());
    }

    public <U> Tuple5<A, U, C, D, E> map2(Function<? super B, ? extends U> mapper)
    {
        return new Tuple5<>(a1(), mapper.apply(b2()), c3(), d4(), e5());
    }

    public <U> Tuple5<A, B, U, D, E> map3(Function<? super C, ? extends U> mapper)
    {
        return new Tuple5<>(a1(), b2(), mapper.apply(c3()), d4(), e5());
    }

    public <U> Tuple5<A, B, C, U, E> map4(Function<? super D, ? extends U> mapper)
    {
        return new Tuple5<>(a1(), b2(), c3(), mapper.apply(d4()), e5());
    }

    public <U> Tuple5<A, B, C, D, U> map5(Function<? super E, ? extends U> mapper)
    {
        return new Tuple5<>(a1(), b2(), c3(), d4(), mapper.apply(e5()));
    }

    @Override
    public int compareTo(Tuple5<A, B, C, D, E> that)
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
        res = Tuple.compare(this.d4(), that.d4());
        if (res != 0)
        {
            return res;
        }
        return Tuple.compare(this.e5(), that.e5());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(a1(), b2(), c3(), d4(), e5());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || !(obj instanceof Tuple5))
        {
            return false;
        }
        Tuple5<?, ?, ?, ?, ?> that = (Tuple5<?, ?, ?, ?, ?>) obj;
        return Objects.equals(this.a1(), that.a1()) &&
               Objects.equals(this.b2(), that.b2()) &&
               Objects.equals(this.c3(), that.c3()) &&
               Objects.equals(this.d4(), that.d4()) &&
               Objects.equals(this.e5(), that.e5());
    }

    @Override
    public String toString()
    {
        return "(" + a1() + ", " + b2() + ", " + c3() + ", " + d4() + ", " + e5() + ")";
    }

    static final class LazyTuple5<A, B, C, D, E> extends Tuple5<A, B, C, D, E>
    {
        private static final long serialVersionUID = 1L;

        private final Lazy<A> a1;
        private final Lazy<B> b2;
        private final Lazy<C> c3;
        private final Lazy<D> d4;
        private final Lazy<E> e5;

        LazyTuple5(Supplier<? extends A> a1Supplier, Supplier<? extends B> b2Supplier, Supplier<? extends C> c3Supplier, Supplier<? extends D> d4Supplier, Supplier<? extends E> e5Supplier)
        {
            super(null, null, null, null, null);
            this.a1 = new Lazy<>(a1Supplier::get);
            this.b2 = new Lazy<>(b2Supplier::get);
            this.c3 = new Lazy<>(c3Supplier::get);
            this.d4 = new Lazy<>(d4Supplier::get);
            this.e5 = new Lazy<>(e5Supplier::get);
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

        @Override
        public E e5()
        {
            return e5.get();
        }

        private Object writeReplace()
        {
            return new Tuple5<>(a1(), b2(), c3(), d4(), e5());
        }
    }
}
