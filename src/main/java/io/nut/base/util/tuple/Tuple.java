/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Interface for tuples.
 *
 * @author franci
 * @since 1.8
 */
public interface Tuple extends Serializable
{
    /**
     * Returns the number of elements in this tuple.
     *
     * @return the arity of the tuple
     */
    int arity();

    /**
     * Returns an array of the elements of this tuple.
     *
     * @return an array containing the elements of this tuple
     */
    Object[] toArray();

    /**
     * Returns a list of the elements of this tuple.
     *
     * @return a list containing the elements of this tuple
     */
    List<Object> toList();

    static Tuple0 of()
    {
        return Tuple0.instance();
    }

    static <A> Tuple1<A> of(A a1)
    {
        return new Tuple1<>(a1);
    }

    static <A> Tuple1<A> of(Supplier<? extends A> a1)
    {
        return new Tuple1.LazyTuple1<>(a1);
    }

    static <A, B> Tuple2<A, B> of(A a1, B b2)
    {
        return new Tuple2<>(a1, b2);
    }

    static <A, B> Tuple2<A, B> of(Supplier<? extends A> a1, Supplier<? extends B> b2)
    {
        return new Tuple2.LazyTuple2<>(a1, b2);
    }

    static <A, B, C> Tuple3<A, B, C> of(A a1, B b2, C c3)
    {
        return new Tuple3<>(a1, b2, c3);
    }

    static <A, B, C> Tuple3<A, B, C> of(Supplier<? extends A> a1, Supplier<? extends B> b2, Supplier<? extends C> c3)
    {
        return new Tuple3.LazyTuple3<>(a1, b2, c3);
    }

    static <A, B, C, D> Tuple4<A, B, C, D> of(A a1, B b2, C c3, D d4)
    {
        return new Tuple4<>(a1, b2, c3, d4);
    }

    static <A, B, C, D> Tuple4<A, B, C, D> of(Supplier<? extends A> a1, Supplier<? extends B> b2, Supplier<? extends C> c3, Supplier<? extends D> d4)
    {
        return new Tuple4.LazyTuple4<>(a1, b2, c3, d4);
    }

    static <A, B, C, D, E> Tuple5<A, B, C, D, E> of(A a1, B b2, C c3, D d4, E e5)
    {
        return new Tuple5<>(a1, b2, c3, d4, e5);
    }

    static <A, B, C, D, E> Tuple5<A, B, C, D, E> of(Supplier<? extends A> a1, Supplier<? extends B> b2, Supplier<? extends C> c3, Supplier<? extends D> d4, Supplier<? extends E> e5)
    {
        return new Tuple5.LazyTuple5<>(a1, b2, c3, d4, e5);
    }

    static <A, B, C, D, E, F> Tuple6<A, B, C, D, E, F> of(A a1, B b2, C c3, D d4, E e5, F f6)
    {
        return new Tuple6<>(a1, b2, c3, d4, e5, f6);
    }

    static <A, B, C, D, E, F> Tuple6<A, B, C, D, E, F> of(Supplier<? extends A> a1, Supplier<? extends B> b2, Supplier<? extends C> c3, Supplier<? extends D> d4, Supplier<? extends E> e5, Supplier<? extends F> f6)
    {
        return new Tuple6.LazyTuple6<>(a1, b2, c3, d4, e5, f6);
    }

    static <A, B, C, D, E, F, G> Tuple7<A, B, C, D, E, F, G> of(A a1, B b2, C c3, D d4, E e5, F f6, G g7)
    {
        return new Tuple7<>(a1, b2, c3, d4, e5, f6, g7);
    }

    static <A, B, C, D, E, F, G> Tuple7<A, B, C, D, E, F, G> of(Supplier<? extends A> a1, Supplier<? extends B> b2, Supplier<? extends C> c3, Supplier<? extends D> d4, Supplier<? extends E> e5, Supplier<? extends F> f6, Supplier<? extends G> g7)
    {
        return new Tuple7.LazyTuple7<>(a1, b2, c3, d4, e5, f6, g7);
    }

    static <A, B, C, D, E, F, G, H> Tuple8<A, B, C, D, E, F, G, H> of(A a1, B b2, C c3, D d4, E e5, F f6, G g7, H h8)
    {
        return new Tuple8<>(a1, b2, c3, d4, e5, f6, g7, h8);
    }

    static <A, B, C, D, E, F, G, H> Tuple8<A, B, C, D, E, F, G, H> of(Supplier<? extends A> a1, Supplier<? extends B> b2, Supplier<? extends C> c3, Supplier<? extends D> d4, Supplier<? extends E> e5, Supplier<? extends F> f6, Supplier<? extends G> g7, Supplier<? extends H> h8)
    {
        return new Tuple8.LazyTuple8<>(a1, b2, c3, d4, e5, f6, g7, h8);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static int compare(Object o1, Object o2)
    {
        if (o1 == o2)
        {
            return 0;
        }
        if (o1 == null)
        {
            return -1;
        }
        if (o2 == null)
        {
            return 1;
        }
        if (o1 instanceof Comparable)
        {
            return ((Comparable) o1).compareTo(o2);
        }
        throw new ClassCastException("Type is not Comparable: " + o1.getClass().getName());
    }
}
