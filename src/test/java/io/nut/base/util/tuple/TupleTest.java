/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for Vavr-style {@link Tuple} implementations.
 *
 * @author franci
 */
public class TupleTest
{
    @Test
    public void testTuple0()
    {
        Tuple0 t0 = Tuple.of();
        assertEquals(0, t0.arity());
        assertEquals(0, t0.toArray().length);
        assertTrue(t0.toList().isEmpty());
        assertEquals("()", t0.toString());
        assertEquals(t0, Tuple0.instance());
        assertEquals(0, t0.compareTo(Tuple.of()));
    }

    @Test
    public void testTuple1()
    {
        Tuple1<String> t1 = Tuple.of("hello");
        assertEquals(1, t1.arity());
        assertEquals("hello", t1.a1());
        assertArrayEquals(new Object[] { "hello" }, t1.toArray());
        assertEquals(Arrays.asList("hello"), t1.toList());
        assertEquals("(hello)", t1.toString());

        Tuple1<String> t1Mapped = t1.map(s -> s + " world");
        assertEquals("hello world", t1Mapped.a1());

        String applied = t1.apply(s -> s + "!");
        assertEquals("hello!", applied);

        assertEquals(0, t1.compareTo(Tuple.of("hello")));
        assertTrue(t1.compareTo(Tuple.of("abc")) > 0);
    }

    @Test
    public void testTuple2()
    {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        assertEquals(2, t2.arity());
        assertEquals("hello", t2.a1());
        assertEquals(42, t2.b2());
        assertArrayEquals(new Object[] { "hello", 42 }, t2.toArray());
        assertEquals(Arrays.asList("hello", 42), t2.toList());
        assertEquals("(hello, 42)", t2.toString());

        Tuple2<String, Integer> t2Mapped = t2.map(s -> s + "!", i -> i + 1);
        assertEquals("hello!", t2Mapped.a1());
        assertEquals(43, t2Mapped.b2());

        Tuple2<String, Integer> t2MappedBi = t2.map((s, i) -> Tuple.of(s + " world", i * 2));
        assertEquals("hello world", t2MappedBi.a1());
        assertEquals(84, t2MappedBi.b2());

        Tuple2<String, Integer> t2Mapped1 = t2.map1(s -> s + "?");
        assertEquals("hello?", t2Mapped1.a1());
        assertEquals(42, t2Mapped1.b2());

        Tuple2<String, Integer> t2Mapped2 = t2.map2(i -> i - 2);
        assertEquals("hello", t2Mapped2.a1());
        assertEquals(40, t2Mapped2.b2());

        String applied = t2.apply((s, i) -> s + " " + i);
        assertEquals("hello 42", applied);

        assertEquals(0, t2.compareTo(Tuple.of("hello", 42)));
        assertTrue(t2.compareTo(Tuple.of("hello", 41)) > 0);
    }

    @Test
    public void testTuple3()
    {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("hello", 42, true);
        assertEquals(3, t3.arity());
        assertEquals("hello", t3.a1());
        assertEquals(42, t3.b2());
        assertEquals(true, t3.c3());
        assertArrayEquals(new Object[] { "hello", 42, true }, t3.toArray());
        assertEquals("(hello, 42, true)", t3.toString());

        Tuple3<String, Integer, Boolean> t3Mapped = t3.map(s -> s + "!", i -> i + 1, b -> !b);
        assertEquals("hello!", t3Mapped.a1());
        assertEquals(43, t3Mapped.b2());
        assertEquals(false, t3Mapped.c3());

        Tuple3<String, Integer, Boolean> t3Mapped1 = t3.map1(s -> "world");
        assertEquals("world", t3Mapped1.a1());
        assertEquals(42, t3Mapped1.b2());
    }

    @Test
    public void testTuple4()
    {
        Tuple4<Integer, Integer, Integer, Integer> t4 = Tuple.of(1, 2, 3, 4);
        assertEquals(4, t4.arity());
        assertEquals(1, t4.a1());
        assertEquals(2, t4.b2());
        assertEquals(3, t4.c3());
        assertEquals(4, t4.d4());
        assertArrayEquals(new Object[] { 1, 2, 3, 4 }, t4.toArray());
        assertEquals("(1, 2, 3, 4)", t4.toString());

        Tuple4<Integer, Integer, Integer, Integer> t4Mapped = t4.map(a -> a + 10, b -> b + 10, c -> c + 10, d -> d + 10);
        assertEquals(11, t4Mapped.a1());
        assertEquals(12, t4Mapped.b2());
        assertEquals(13, t4Mapped.c3());
        assertEquals(14, t4Mapped.d4());
    }

    @Test
    public void testTuple5()
    {
        Tuple5<Integer, Integer, Integer, Integer, Integer> t5 = Tuple.of(1, 2, 3, 4, 5);
        assertEquals(5, t5.arity());
        assertEquals(1, t5.a1());
        assertEquals(2, t5.b2());
        assertEquals(3, t5.c3());
        assertEquals(4, t5.d4());
        assertEquals(5, t5.e5());
        assertArrayEquals(new Object[] { 1, 2, 3, 4, 5 }, t5.toArray());
        assertEquals("(1, 2, 3, 4, 5)", t5.toString());

        Tuple5<Integer, Integer, Integer, Integer, Integer> t5Mapped = t5.map(a -> a + 10, b -> b + 10, c -> c + 10, d -> d + 10, e -> e + 10);
        assertEquals(11, t5Mapped.a1());
        assertEquals(12, t5Mapped.b2());
        assertEquals(13, t5Mapped.c3());
        assertEquals(14, t5Mapped.d4());
        assertEquals(15, t5Mapped.e5());
    }

    @Test
    public void testTuple6()
    {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t6 = Tuple.of(1, 2, 3, 4, 5, 6);
        assertEquals(6, t6.arity());
        assertEquals(1, t6.a1());
        assertEquals(2, t6.b2());
        assertEquals(3, t6.c3());
        assertEquals(4, t6.d4());
        assertEquals(5, t6.e5());
        assertEquals(6, t6.f6());
        assertArrayEquals(new Object[] { 1, 2, 3, 4, 5, 6 }, t6.toArray());
        assertEquals("(1, 2, 3, 4, 5, 6)", t6.toString());
    }

    @Test
    public void testTuple7()
    {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> t7 = Tuple.of(1, 2, 3, 4, 5, 6, 7);
        assertEquals(7, t7.arity());
        assertEquals(1, t7.a1());
        assertEquals(2, t7.b2());
        assertEquals(3, t7.c3());
        assertEquals(4, t7.d4());
        assertEquals(5, t7.e5());
        assertEquals(6, t7.f6());
        assertEquals(7, t7.g7());
        assertArrayEquals(new Object[] { 1, 2, 3, 4, 5, 6, 7 }, t7.toArray());
        assertEquals("(1, 2, 3, 4, 5, 6, 7)", t7.toString());
    }

    @Test
    public void testTuple8()
    {
        Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t8 = Tuple.of(1, 2, 3, 4, 5, 6, 7, 8);
        assertEquals(8, t8.arity());
        assertEquals(1, t8.a1());
        assertEquals(2, t8.b2());
        assertEquals(3, t8.c3());
        assertEquals(4, t8.d4());
        assertEquals(5, t8.e5());
        assertEquals(6, t8.f6());
        assertEquals(7, t8.g7());
        assertEquals(8, t8.h8());
        assertArrayEquals(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8 }, t8.toArray());
        assertEquals("(1, 2, 3, 4, 5, 6, 7, 8)", t8.toString());
    }

    @Test
    public void testEqualsAndHashCode()
    {
        Tuple2<String, String> tA = Tuple.of("a", "b");
        Tuple2<String, String> tB = Tuple.of("a", "b");
        Tuple2<String, String> tC = Tuple.of("a", "c");

        assertEquals(tA, tB);
        assertNotEquals(tA, tC);
        assertEquals(tA.hashCode(), tB.hashCode());
        assertNotEquals(tA.hashCode(), tC.hashCode());
    }

    @Test
    public void testComparisonExceptions()
    {
        // Uncomparable types should trigger ClassCastException
        Tuple2<Object, Object> uncomp1 = Tuple.of(new Object(), 1);
        Tuple2<Object, Object> uncomp2 = Tuple.of(new Object(), 2);

        assertThrows(ClassCastException.class, () -> uncomp1.compareTo(uncomp2));
    }

    @Test
    public void testSerialization() throws Exception
    {
        Tuple3<String, Integer, Double> original = Tuple.of("serialize", 123, 4.56);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        Tuple3<?, ?, ?> deserialized = (Tuple3<?, ?, ?>) ois.readObject();

        assertEquals(original, deserialized);
        assertEquals(original.a1(), deserialized.a1());
        assertEquals(original.b2(), deserialized.b2());
        assertEquals(original.c3(), deserialized.c3());
    }

    @Test
    public void testLazyTuples() throws Exception
    {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);

        Tuple2<String, Integer> lazyTuple = Tuple.of(
            () -> {
                counter1.incrementAndGet();
                return "lazy";
            },
            () -> {
                counter2.incrementAndGet();
                return 99;
            }
        );

        // Verify that creators have not evaluated the suppliers yet
        assertEquals(0, counter1.get());
        assertEquals(0, counter2.get());

        // Access a1() and verify it triggers supplier 1 only
        assertEquals("lazy", lazyTuple.a1());
        assertEquals(1, counter1.get());
        assertEquals(0, counter2.get());

        // Access a1() again to ensure it uses cached value
        assertEquals("lazy", lazyTuple.a1());
        assertEquals(1, counter1.get());

        // Access b2() and verify it triggers supplier 2
        assertEquals(99, lazyTuple.b2());
        assertEquals(1, counter2.get());

        // Verify equality between Eager and Lazy tuples
        Tuple2<String, Integer> eagerTuple = Tuple.of("lazy", 99);
        assertEquals(eagerTuple, lazyTuple);
        assertEquals(eagerTuple.hashCode(), lazyTuple.hashCode());

        // Verify serialization converts LazyTuple to eager Tuple upon serialization
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(lazyTuple);
        oos.flush();

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        Tuple2<?, ?> deserialized = (Tuple2<?, ?>) ois.readObject();

        // Deserialized tuple should be equal to the original one
        assertEquals(lazyTuple, deserialized);
        assertEquals("lazy", deserialized.a1());
        assertEquals(99, deserialized.b2());
        // Since it deserialized into eager Tuple2, it should be of class Tuple2, not LazyTuple2
        assertEquals(Tuple2.class, deserialized.getClass());
    }
}
