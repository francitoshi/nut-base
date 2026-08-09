/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Pair}.
 *
 * @author franci
 */
public class PairTest
{
    /**
     * Verifies that the constructor stores the given key and value, and
     * that {@link Pair#getKey()} and {@link Pair#getVal()} return them
     * unchanged.
     */
    @Test
    public void testConstructorAndBasicGetters()
    {
        Pair<String, Integer> pair = new Pair<>("one", 1);

        assertEquals("one", pair.getKey());
        assertEquals(1, pair.getVal());
    }

    /**
     * Verifies that a {@code Pair} can hold {@code null} as key, as
     * value, or as both, without throwing an exception.
     */
    @Test
    public void testNullKeyAndValue()
    {
        Pair<String, String> pair = new Pair<>(null, null);

        assertNull(pair.getKey());
        assertNull(pair.getVal());
    }

    /**
     * Verifies that {@link Pair#of(Object, Object)} creates an
     * equivalent pair to using the constructor directly.
     */
    @Test
    public void testOfFactoryMethod()
    {
        Pair<String, Integer> pair = Pair.of("two", 2);

        assertEquals("two", pair.getKey());
        assertEquals(2, pair.getVal());
        assertEquals(new Pair<>("two", 2), pair);
    }

    /**
     * Verifies that all the alias getters ({@code getLeft}/{@code getRight},
     * {@code get1st}/{@code get2nd}, {@code getIn}/{@code getOut},
     * {@code getRead}/{@code getWrite}) return the same underlying values
     * as {@code getKey}/{@code getVal}.
     */
    @Test
    public void testAliasGettersReturnSameValues()
    {
        Pair<String, Integer> pair = new Pair<>("k", 42);

        // key-side aliases
        assertEquals(pair.getKey(), pair.getLeft());
        assertEquals(pair.getKey(), pair.get1st());
        assertEquals(pair.getKey(), pair.getIn());
        assertEquals(pair.getKey(), pair.getRead());

        // value-side aliases
        assertEquals(pair.getVal(), pair.getRight());
        assertEquals(pair.getVal(), pair.get2nd());
        assertEquals(pair.getVal(), pair.getOut());
        assertEquals(pair.getVal(), pair.getWrite());
    }

    /**
     * Verifies that {@link Pair#toString()} renders as {@code key=val}.
     */
    @Test
    public void testToString()
    {
        Pair<String, Integer> pair = new Pair<>("answer", 42);

        assertEquals("answer=42", pair.toString());
    }

    /**
     * Verifies that {@link Pair#toString()} handles {@code null} key and
     * value gracefully, following String concatenation semantics.
     */
    @Test
    public void testToStringWithNulls()
    {
        Pair<String, String> pair = new Pair<>(null, null);

        assertEquals("null=null", pair.toString());
    }

    /**
     * Verifies the reflexive, equal-content and unequal-content cases of
     * {@link Pair#equals(Object)}.
     */
    @Test
    public void testEqualsContract()
    {
        Pair<String, Integer> p1 = new Pair<>("a", 1);
        Pair<String, Integer> p2 = new Pair<>("a", 1);
        Pair<String, Integer> p3 = new Pair<>("a", 2);
        Pair<String, Integer> p4 = new Pair<>("b", 1);

        // reflexive
        assertEquals(p1, p1);
        // same content -> equal
        assertEquals(p1, p2);
        // different value -> not equal
        assertNotEquals(p1, p3);
        // different key -> not equal
        assertNotEquals(p1, p4);
    }

    /**
     * Verifies that {@link Pair#equals(Object)} returns {@code false}
     * when compared against {@code null} or an instance of a different
     * class.
     */
    @Test
    public void testEqualsWithNullAndDifferentClass()
    {
        Pair<String, Integer> pair = new Pair<>("a", 1);

        assertFalse(pair.equals(null));
        assertFalse(pair.equals("not a pair"));
    }

    /**
     * Verifies that equal pairs produce equal hash codes, consistent
     * with the {@code equals}/{@code hashCode} contract.
     */
    @Test
    public void testHashCodeConsistentWithEquals()
    {
        Pair<String, Integer> p1 = new Pair<>("a", 1);
        Pair<String, Integer> p2 = new Pair<>("a", 1);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    /**
     * Verifies that {@link Pair#getKey(Pair)} and {@link Pair#getVal(Pair)}
     * correctly extract values from a non-null pair.
     */
    @Test
    public void testStaticGettersWithNonNullPair()
    {
        Pair<String, Integer> pair = new Pair<>("x", 10);

        assertEquals("x", Pair.getKey(pair));
        assertEquals(10, Pair.getVal(pair));
    }

    /**
     * Verifies that {@link Pair#getKey(Pair)} and {@link Pair#getVal(Pair)}
     * return {@code null} when given a {@code null} pair, instead of
     * throwing a {@link NullPointerException}.
     */
    @Test
    public void testStaticGettersWithNullPair()
    {
        assertNull(Pair.getKey(null));
        assertNull(Pair.getVal(null));
    }

    /**
     * Verifies that {@link Pair#inverse()} swaps key and value into a
     * new pair, and that the original pair is left unmodified.
     */
    @Test
    public void testInverse()
    {
        Pair<String, Integer> pair = new Pair<>("k", 7);
        Pair<Integer, String> inverted = pair.inverse();

        assertEquals(7, inverted.getKey());
        assertEquals("k", inverted.getVal());

        // original pair must remain unchanged
        assertEquals("k", pair.getKey());
        assertEquals(7, pair.getVal());
    }

    /**
     * Verifies that applying {@link Pair#inverse()} twice returns to a
     * pair equal to the original.
     */
    @Test
    public void testDoubleInverseRestoresOriginal()
    {
        Pair<String, Integer> pair = new Pair<>("k", 7);

        Pair<String, Integer> twiceInverted = pair.inverse().inverse();

        assertEquals(pair, twiceInverted);
    }
}
