/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SaturatedTest
{
    /**
     * Test of saturatedAdd method, of class Saturated.
     */
    @Test
    public void testSaturatedAdd_int()
    {
        assertEquals(0, Saturated.saturatedAdd(0, 0));
        assertEquals(3, Saturated.saturatedAdd(1, 2));
        assertEquals(-3, Saturated.saturatedAdd(-1, -2));
        assertEquals(2, Saturated.saturatedAdd(-1, 3));
        assertEquals(-2, Saturated.saturatedAdd(1, -3));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedAdd(Integer.MAX_VALUE, 1));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedAdd(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Saturated.saturatedAdd(Integer.MIN_VALUE, -1));
        assertEquals(Integer.MIN_VALUE, Saturated.saturatedAdd(Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    /**
     * Test of saturatedAdd method, of class Saturated.
     */
    @Test
    public void testSaturatedAdd_long()
    {
        assertEquals(0L, Saturated.saturatedAdd(0L, 0L));
        assertEquals(3L, Saturated.saturatedAdd(1L, 2L));
        assertEquals(-3L, Saturated.saturatedAdd(-1L, -2L));
        assertEquals(2L, Saturated.saturatedAdd(-1L, 3L));
        assertEquals(-2L, Saturated.saturatedAdd(1L, -3L));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedAdd(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, Saturated.saturatedAdd(Long.MIN_VALUE, -1L));
        assertEquals(Long.MIN_VALUE, Saturated.saturatedAdd(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    /**
     * Test of saturatedSubtract method, of class Saturated.
     */
    @Test
    public void testSaturatedSubtract_int()
    {
        assertEquals(0, Saturated.saturatedSubtract(0, 0));
        assertEquals(3, Saturated.saturatedSubtract(5, 2));
        assertEquals(-3, Saturated.saturatedSubtract(-5, -2));
        assertEquals(-4, Saturated.saturatedSubtract(-1, 3));
        assertEquals(4, Saturated.saturatedSubtract(1, -3));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedSubtract(Integer.MAX_VALUE, -1));
        assertEquals(0, Saturated.saturatedSubtract(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, Saturated.saturatedSubtract(Integer.MIN_VALUE, 1));
    }

    /**
     * Test of saturatedSubtract method, of class Saturated.
     */
    @Test
    public void testSaturatedSubtract_long()
    {
        assertEquals(0L, Saturated.saturatedSubtract(0L, 0L));
        assertEquals(3L, Saturated.saturatedSubtract(5L, 2L));
        assertEquals(-3L, Saturated.saturatedSubtract(-5L, -2L));
        assertEquals(-4L, Saturated.saturatedSubtract(-1L, 3L));
        assertEquals(4L, Saturated.saturatedSubtract(1L, -3L));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedSubtract(Long.MAX_VALUE, -1L));
        assertEquals(0L, Saturated.saturatedSubtract(Long.MIN_VALUE, Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, Saturated.saturatedSubtract(Long.MIN_VALUE, 1L));
    }

    /**
     * Test of saturatedMultiply method, of class Saturated.
     */
    @Test
    public void testSaturatedMultiply_int()
    {
        assertEquals(0, Saturated.saturatedMultiply(0, 100));
        assertEquals(0, Saturated.saturatedMultiply(100, 0));
        assertEquals(6, Saturated.saturatedMultiply(2, 3));
        assertEquals(-6, Saturated.saturatedMultiply(-2, 3));
        assertEquals(-6, Saturated.saturatedMultiply(2, -3));
        assertEquals(6, Saturated.saturatedMultiply(-2, -3));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedMultiply(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MIN_VALUE, Saturated.saturatedMultiply(Integer.MIN_VALUE, 2));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedMultiply(Integer.MIN_VALUE, -1));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedMultiply(-1, Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, Saturated.saturatedMultiply(1, Integer.MIN_VALUE));
    }

    /**
     * Test of saturatedMultiply method, of class Saturated.
     */
    @Test
    public void testSaturatedMultiply_long()
    {
        assertEquals(0L, Saturated.saturatedMultiply(0L, 100L));
        assertEquals(0L, Saturated.saturatedMultiply(100L, 0L));
        assertEquals(6L, Saturated.saturatedMultiply(2L, 3L));
        assertEquals(-6L, Saturated.saturatedMultiply(-2L, 3L));
        assertEquals(-6L, Saturated.saturatedMultiply(2L, -3L));
        assertEquals(6L, Saturated.saturatedMultiply(-2L, -3L));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedMultiply(Long.MAX_VALUE, 2L));
        assertEquals(Long.MIN_VALUE, Saturated.saturatedMultiply(Long.MIN_VALUE, 2L));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedMultiply(Long.MIN_VALUE, -1L));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedMultiply(-1L, Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, Saturated.saturatedMultiply(1L, Long.MIN_VALUE));
    }

    /**
     * Test of saturatedNegate method, of class Saturated.
     */
    @Test
    public void testSaturatedNegate_int()
    {
        assertEquals(0, Saturated.saturatedNegate(0));
        assertEquals(-5, Saturated.saturatedNegate(5));
        assertEquals(5, Saturated.saturatedNegate(-5));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedNegate(Integer.MIN_VALUE));
        assertEquals(-Integer.MAX_VALUE, Saturated.saturatedNegate(Integer.MAX_VALUE));
    }

    /**
     * Test of saturatedNegate method, of class Saturated.
     */
    @Test
    public void testSaturatedNegate_long()
    {
        assertEquals(0L, Saturated.saturatedNegate(0L));
        assertEquals(-5L, Saturated.saturatedNegate(5L));
        assertEquals(5L, Saturated.saturatedNegate(-5L));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedNegate(Long.MIN_VALUE));
        assertEquals(-Long.MAX_VALUE, Saturated.saturatedNegate(Long.MAX_VALUE));
    }

    /**
     * Test of saturatedAbs method, of class Saturated.
     */
    @Test
    public void testSaturatedAbs_int()
    {
        assertEquals(0, Saturated.saturatedAbs(0));
        assertEquals(5, Saturated.saturatedAbs(5));
        assertEquals(5, Saturated.saturatedAbs(-5));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedAbs(Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, Saturated.saturatedAbs(Integer.MAX_VALUE));
    }

    /**
     * Test of saturatedAbs method, of class Saturated.
     */
    @Test
    public void testSaturatedAbs_long()
    {
        assertEquals(0L, Saturated.saturatedAbs(0L));
        assertEquals(5L, Saturated.saturatedAbs(5L));
        assertEquals(5L, Saturated.saturatedAbs(-5L));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedAbs(Long.MIN_VALUE));
        assertEquals(Long.MAX_VALUE, Saturated.saturatedAbs(Long.MAX_VALUE));
    }

    /**
     * A huge timeout added to the current nanoTime must stay in the future
     * instead of wrapping around into the past and expiring immediately.
     */
    @Test
    public void testDeadlineDoesNotWrapAround()
    {
        long now = System.nanoTime();
        long deadline = Saturated.saturatedAdd(now, Long.MAX_VALUE);
        assertTrue(deadline > 0, "deadline must not wrap into the past: " + deadline);
    }
}
