/*
 * Copyright (c) 2010-2026 francitoshi@gmail.com
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
package io.nut.base.util.tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Trio}.
 */
public class TrioTest
{
    /**
     * Verifies that the constructor stores the given key, value and
     * attribute, and that they are returned unchanged by
     * {@link Trio#getKey()}, {@link Trio#getVal()} and {@link Trio#getAtt()}.
     */
    @Test
    public void testConstructorAndBasicGetters()
    {
        Trio<String, Integer, Boolean> trio = new Trio<>("key", 1, true);

        assertEquals("key", trio.getKey());
        assertEquals(1, trio.getVal());
        assertEquals(true, trio.getAtt());
    }

    /**
     * Verifies that a {@code Trio} can hold {@code null} as key, value
     * and/or attribute without throwing an exception.
     */
    @Test
    public void testNullKeyValueAndAttribute()
    {
        Trio<String, String, String> trio = new Trio<>(null, null, null);

        assertNull(trio.getKey());
        assertNull(trio.getVal());
        assertNull(trio.getAtt());
    }

    /**
     * Verifies that the positional alias getters ({@code get1st},
     * {@code get2nd}, {@code get3rd}) return the same underlying values
     * as {@code getKey}, {@code getVal} and {@code getAtt}.
     */
    @Test
    public void testPositionalAliasGetters()
    {
        Trio<String, Integer, Double> trio = new Trio<>("k", 2, 3.5);

        assertEquals(trio.getKey(), trio.get1st());
        assertEquals(trio.getVal(), trio.get2nd());
        assertEquals(trio.getAtt(), trio.get3rd());
    }

    /**
     * Verifies the reflexive, equal-content and unequal-content cases of
     * {@link Trio#equals(Object)}, exercising each of the three fields
     * independently.
     */
    @Test
    public void testEqualsContract()
    {
        Trio<String, Integer, Boolean> t1 = new Trio<>("a", 1, true);
        Trio<String, Integer, Boolean> t2 = new Trio<>("a", 1, true);
        Trio<String, Integer, Boolean> diffKey = new Trio<>("b", 1, true);
        Trio<String, Integer, Boolean> diffVal = new Trio<>("a", 2, true);
        Trio<String, Integer, Boolean> diffAtt = new Trio<>("a", 1, false);

        // reflexive
        assertEquals(t1, t1);
        // same content -> equal
        assertEquals(t1, t2);
        // differing in any single field -> not equal
        assertNotEquals(t1, diffKey);
        assertNotEquals(t1, diffVal);
        assertNotEquals(t1, diffAtt);
    }

    /**
     * Verifies that {@link Trio#equals(Object)} returns {@code false}
     * when compared against {@code null} or an instance of a different
     * class.
     */
    @Test
    public void testEqualsWithNullAndDifferentClass()
    {
        Trio<String, Integer, Boolean> trio = new Trio<>("a", 1, true);

        assertFalse(trio.equals(null));
        assertFalse(trio.equals("not a trio"));
    }

    /**
     * Verifies that equal trios produce equal hash codes, consistent
     * with the {@code equals}/{@code hashCode} contract.
     */
    @Test
    public void testHashCodeConsistentWithEquals()
    {
        Trio<String, Integer, Boolean> t1 = new Trio<>("a", 1, true);
        Trio<String, Integer, Boolean> t2 = new Trio<>("a", 1, true);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    /**
     * Verifies that {@link Trio#getKey(Trio)} and {@link Trio#getVal(Trio)}
     * correctly extract values from a non-null trio.
     */
    @Test
    public void testStaticGetKeyAndGetValWithNonNullTrio()
    {
        Trio<String, Integer, Boolean> trio = new Trio<>("x", 10, false);

        assertEquals("x", Trio.getKey(trio));
        assertEquals(10, Trio.getVal(trio));
    }

    /**
     * Verifies that {@link Trio#getKey(Trio)} and {@link Trio#getVal(Trio)}
     * return {@code null} when given a {@code null} trio, instead of
     * throwing a {@link NullPointerException}.
     */
    @Test
    public void testStaticGetKeyAndGetValWithNullTrio()
    {
        assertNull(Trio.getKey(null));
        assertNull(Trio.getVal(null));
    }

    /**
     * Verifies the current (buggy) behavior of the static
     * {@link Trio#getAtt(Trio)} method: instead of returning the trio's
     * attribute, it actually returns the trio's value, because its
     * implementation delegates to {@code trio.getVal()} rather than
     * {@code trio.getAtt()}.
     * <p>
     * This test intentionally documents the existing behavior rather
     * than the seemingly intended one, so that any future fix of the
     * method is a deliberate, visible change to this test rather than a
     * silent one.
     */
    @Test
    public void testStaticGetAttActuallyReturnsValDueToExistingBug()
    {
        Trio<String, Integer, Boolean> trio = new Trio<>("x", 10, true);

        // documents current behavior: returns getVal(), not getAtt()
        assertEquals(trio.getVal(), Trio.getAtt(trio));
        assertNotEquals(trio.getAtt(), Trio.getAtt(trio));
    }

    /**
     * Verifies that {@link Trio#getAtt(Trio)} returns {@code null} when
     * given a {@code null} trio.
     */
    @Test
    public void testStaticGetAttWithNullTrio()
    {
        assertNull(Trio.getAtt(null));
    }
}
