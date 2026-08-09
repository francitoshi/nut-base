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
 * A tuple of 7 elements.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @param <D> the type of the fourth element
 * @param <E> the type of the fifth element
 * @param <F> the type of the sixth element
 * @param <G> the type of the seventh element
 * @author franci
 * @since 1.8
 */
public class Tuple7<A, B, C, D, E, F, G> implements Tuple, Comparable<Tuple7<A, B, C, D, E, F, G>>
{
    private static final long serialVersionUID = 1L;

    private final A a1;
    private final B b2;
    private final C c3;
    private final D d4;
    private final E e5;
    private final F f6;
    private final G g7;

    public Tuple7(A a1, B b2, C c3, D d4, E e5, F f6, G g7)
    {
        this.a1 = a1;
        this.b2 = b2;
        this.c3 = c3;
        this.d4 = d4;
        this.e5 = e5;
        this.f6 = f6;
        this.g7 = g7;
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

    public F f6()
    {
        return f6;
    }

    public G g7()
    {
        return g7;
    }

    @Override
    public int arity()
    {
        return 7;
    }

    @Override
    public Object[] toArray()
    {
        return new Object[] { a1(), b2(), c3(), d4(), e5(), f6(), g7() };
    }

    @Override
    public List<Object> toList()
    {
        return Arrays.asList(toArray());
    }

    public <U1, U2, U3, U4, U5, U6, U7> Tuple7<U1, U2, U3, U4, U5, U6, U7> map(
        Function<? super A, ? extends U1> f1,
        Function<? super B, ? extends U2> f2,
        Function<? super C, ? extends U3> f3,
        Function<? super D, ? extends U4> f4,
        Function<? super E, ? extends U5> f5,
        Function<? super F, ? extends U6> f6,
        Function<? super G, ? extends U7> f7)
    {
        return new Tuple7<>(f1.apply(a1()), f2.apply(b2()), f3.apply(c3()), f4.apply(d4()), f5.apply(e5()), f6.apply(f6()), f7.apply(g7()));
    }

    public <U> Tuple7<U, B, C, D, E, F, G> map1(Function<? super A, ? extends U> mapper)
    {
        return new Tuple7<>(mapper.apply(a1()), b2, c3, d4, e5, f6, g7);
    }

    public <U> Tuple7<A, U, C, D, E, F, G> map2(Function<? super B, ? extends U> mapper)
    {
        return new Tuple7<>(a1(), mapper.apply(b2()), c3(), d4(), e5(), f6(), g7());
    }

    public <U> Tuple7<A, B, U, D, E, F, G> map3(Function<? super C, ? extends U> mapper)
    {
        return new Tuple7<>(a1(), b2(), mapper.apply(c3()), d4(), e5(), f6(), g7());
    }

    public <U> Tuple7<A, B, C, U, E, F, G> map4(Function<? super D, ? extends U> mapper)
    {
        return new Tuple7<>(a1(), b2(), c3(), mapper.apply(d4()), e5(), f6(), g7());
    }

    public <U> Tuple7<A, B, C, D, U, F, G> map5(Function<? super E, ? extends U> mapper)
    {
        return new Tuple7<>(a1(), b2(), c3(), d4(), mapper.apply(e5()), f6(), g7());
    }

    public <U> Tuple7<A, B, C, D, E, U, G> map6(Function<? super F, ? extends U> mapper)
    {
        return new Tuple7<>(a1(), b2(), c3(), d4(), e5(), mapper.apply(f6()), g7());
    }

    public <U> Tuple7<A, B, C, D, E, F, U> map7(Function<? super G, ? extends U> mapper)
    {
        return new Tuple7<>(a1(), b2(), c3(), d4(), e5(), f6(), mapper.apply(g7()));
    }

    @Override
    public int compareTo(Tuple7<A, B, C, D, E, F, G> that)
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
        res = Tuple.compare(this.e5(), that.e5());
        if (res != 0)
        {
            return res;
        }
        res = Tuple.compare(this.f6(), that.f6());
        if (res != 0)
        {
            return res;
        }
        return Tuple.compare(this.g7(), that.g7());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(a1(), b2(), c3(), d4(), e5(), f6(), g7());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || !(obj instanceof Tuple7))
        {
            return false;
        }
        Tuple7<?, ?, ?, ?, ?, ?, ?> that = (Tuple7<?, ?, ?, ?, ?, ?, ?>) obj;
        return Objects.equals(this.a1(), that.a1()) &&
               Objects.equals(this.b2(), that.b2()) &&
               Objects.equals(this.c3(), that.c3()) &&
               Objects.equals(this.d4(), that.d4()) &&
               Objects.equals(this.e5(), that.e5()) &&
               Objects.equals(this.f6(), that.f6()) &&
               Objects.equals(this.g7(), that.g7());
    }

    @Override
    public String toString()
    {
        return "(" + a1() + ", " + b2() + ", " + c3() + ", " + d4() + ", " + e5() + ", " + f6() + ", " + g7() + ")";
    }

    static final class LazyTuple7<A, B, C, D, E, F, G> extends Tuple7<A, B, C, D, E, F, G>
    {
        private static final long serialVersionUID = 1L;

        private final Lazy<A> a1;
        private final Lazy<B> b2;
        private final Lazy<C> c3;
        private final Lazy<D> d4;
        private final Lazy<E> e5;
        private final Lazy<F> f6;
        private final Lazy<G> g7;

        LazyTuple7(Supplier<? extends A> a1Supplier, Supplier<? extends B> b2Supplier, Supplier<? extends C> c3Supplier, Supplier<? extends D> d4Supplier, Supplier<? extends E> e5Supplier, Supplier<? extends F> f6Supplier, Supplier<? extends G> g7Supplier)
        {
            super(null, null, null, null, null, null, null);
            this.a1 = new Lazy<>(a1Supplier::get);
            this.b2 = new Lazy<>(b2Supplier::get);
            this.c3 = new Lazy<>(c3Supplier::get);
            this.d4 = new Lazy<>(d4Supplier::get);
            this.e5 = new Lazy<>(e5Supplier::get);
            this.f6 = new Lazy<>(f6Supplier::get);
            this.g7 = new Lazy<>(g7Supplier::get);
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

        @Override
        public F f6()
        {
            return f6.get();
        }

        @Override
        public G g7()
        {
            return g7.get();
        }

        private Object writeReplace()
        {
            return new Tuple7<>(a1(), b2(), c3(), d4(), e5(), f6(), g7());
        }
    }
}
