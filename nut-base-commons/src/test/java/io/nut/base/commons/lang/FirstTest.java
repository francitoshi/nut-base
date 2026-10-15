/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.lang;

import io.nut.base.commons.lang.First;
import java.math.BigDecimal;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class FirstTest
{
    
    /**
     * Test of firstNonNull method, of class Utils.
     */
    @Test
    public void testFirstNonNull()
    {
        assertNull(First.firstNonNull());
        assertEquals("a", First.firstNonNull("a"));
        assertEquals("a", First.firstNonNull("a", "b"));
        
        assertEquals("a", First.firstNonNull("a", "b", "c", "d"));
        assertEquals("b", First.firstNonNull(null, "b", "c", "d"));
        assertEquals("c", First.firstNonNull(null, null, "c", "d"));
        assertEquals("d", First.firstNonNull(null, null, null, "d"));
        assertNull(First.firstNonNull(null, null, null, null));
    }

    /**
     * Test of firstNonNull method with suppliers, of class Utils.
     */
    @Test
    public void testFirstNonNullSuppliers()
    {
        assertNull(First.firstNonNull(() -> null));
        assertNull(First.firstNonNull(() -> null, () -> null));
        assertEquals("a", First.firstNonNull(() -> "a"));
        assertEquals("a", First.firstNonNull(() -> "a", () -> "b"));
        assertEquals("b", First.firstNonNull(() -> null, () -> "b"));
        assertNull(First.firstNonNull((Supplier<String>) null));
        assertEquals("a", First.firstNonNull((Supplier<String>) null, () -> "a"));
        assertEquals("a", First.firstNonNull(() -> "a", () ->
        {
            throw new AssertionError("lazy evaluation violated: later supplier was invoked");
        }));
        assertEquals("b", First.firstNonNull(() -> null, () -> "b", () ->
        {
            throw new AssertionError("lazy evaluation violated: later supplier was invoked");
        }));
    }
    
    /**
     * Test of firstNonNullOrEmpty method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrEmpty()
    {
        assertEquals("", First.firstNonNullOrEmpty(null, null));
        assertEquals("", First.firstNonNullOrEmpty(null,""));
        assertEquals("5", First.firstNonNullOrEmpty(5, "","1"));
        assertEquals("1", First.firstNonNullOrEmpty("","1"));
    }

    /**
     * Test of firstNonNullOrEmpty method with suppliers, of class Utils.
     */
    @Test
    public void testFirstNonNullOrEmptySuppliers()
    {
        assertEquals("", First.firstNonNullOrEmpty(() -> null));
        assertEquals("", First.firstNonNullOrEmpty(() -> null, () -> ""));
        assertEquals("a", First.firstNonNullOrEmpty(() -> "a"));
        assertEquals("a", First.firstNonNullOrEmpty(() -> "", () -> "a"));
        assertEquals("b", First.firstNonNullOrEmpty(() -> null, () -> "b"));
        assertEquals("5", First.firstNonNullOrEmpty(() -> 5, () -> "1"));
        assertEquals("", First.firstNonNullOrEmpty((Supplier<String>) null));
        assertEquals("a", First.firstNonNullOrEmpty((Supplier<String>) null, () -> "a"));
        assertEquals("a", First.firstNonNullOrEmpty(() -> "", () -> "a", () ->
        {
            throw new AssertionError("lazy evaluation violated: later supplier was invoked");
        }));
    }

    /**
     * Test of firstNonNullOrPoison method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrPoison_2args_1()
    {
        assertEquals(BigDecimal.ONE, First.firstNonNullOrPoison(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN));
        assertNull(First.firstNonNullOrPoison(BigDecimal.ZERO, BigDecimal.ZERO));
        assertNull(First.firstNonNullOrPoison(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    /**
     * Test of firstNonNullOrPoison method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrPoison_2args_2()
    {
        BigDecimal[] poison = {BigDecimal.ZERO, BigDecimal.ONE};
        assertEquals(BigDecimal.TEN, First.firstNonNullOrPoison(poison, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN));
        assertNull(First.firstNonNullOrPoison(poison));
        assertNull(First.firstNonNullOrPoison(poison, BigDecimal.ZERO, BigDecimal.ONE));
    }
    
    /**
     * Test of firstNonNullOrPoison method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrPoisonLenient_3args_1()
    {
        assertEquals(BigDecimal.ONE, First.firstNonNullOrPoisonLenient(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN));
        assertEquals(BigDecimal.ZERO, First.firstNonNullOrPoisonLenient(BigDecimal.ZERO, BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO, First.firstNonNullOrPoisonLenient(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    /**
     * Test of firstNonNullOrPoison method, of class Utils.
     */
    @Test
    public void testFirstNonNullOrPoisonLenient_3args_2()
    {
        BigDecimal[] poison = {BigDecimal.ZERO, BigDecimal.ONE};
        assertEquals(BigDecimal.TEN, First.firstNonNullOrPoisonLenient(poison, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN));
        assertNull(First.firstNonNullOrPoisonLenient(poison));
        assertEquals(BigDecimal.ZERO, First.firstNonNullOrPoisonLenient(poison, BigDecimal.ZERO, BigDecimal.ONE));
    }    
}
