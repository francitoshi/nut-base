/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ArgsTest
{
    
    public ArgsTest()
    {
    }

    /**
     * Test of get method, of class Args.
     */
    @Test
    public void testGet()
    {
        Args instance = new Args();
        assertEquals(0, instance.get().size());
        instance.add("1", "2");
        assertEquals(2, instance.get().size());
        instance.add("3", "4");
        assertEquals(4, instance.get().size());
    }


    /**
     * Test of add method, of class Args.
     */
    @Test
    public void testAdd_boolean_StringArr()
    {
        Args instance = new Args();
        assertEquals(0, instance.get().size());
        instance.add(false, "1", "2");
        assertEquals(0, instance.get().size());
        instance.add(true, "3", "4");
        assertEquals(2, instance.get().size());
    }

    /**
     * Test of get method with an index, of class Args.
     */
    @Test
    public void testGet_int()
    {
        Args instance = new Args("a", "b", "c");

        assertEquals("a", instance.get(0));
        assertEquals("b", instance.get(1));
        assertEquals("c", instance.get(2));
        assertNull(instance.get(3));
        assertNull(instance.get(100));
    }

    /**
     * Test of get method with an index and an eager default value, of class Args.
     */
    @Test
    public void testGet_int_String()
    {
        Args instance = new Args("a", "b");

        assertEquals("a", instance.get(0, "default"));
        assertEquals("b", instance.get(1, "default"));
        assertEquals("default", instance.get(2, "default"));
        assertEquals("default", instance.get(100, "default"));
        assertNull(instance.get(2, (String)null));
    }

    /**
     * Test of get method with an index and a default supplier, of class Args.
     */
    @Test
    public void testGet_int_Supplier()
    {
        Args instance = new Args("a", "b");

        AtomicInteger calls = new AtomicInteger();
        assertEquals("a", instance.get(0, () -> {
            calls.incrementAndGet();
            return "default";
        }));
        assertEquals("b", instance.get(1, () -> {
            calls.incrementAndGet();
            return "default";
        }));
        assertEquals(0, calls.get());

        assertEquals("default", instance.get(2, () -> {
            calls.incrementAndGet();
            return "default";
        }));
        assertEquals(1, calls.get());

        AtomicBoolean evaluated = new AtomicBoolean(false);
        assertEquals("lazy", instance.get(100, () -> {
            evaluated.set(true);
            return "lazy";
        }));
        assertTrue(evaluated.get());
    }

    /**
     * Test of get method with a null default supplier, of class Args.
     */
    @Test
    public void testGet_int_NullSupplier()
    {
        Args instance = new Args("a", "b");

        assertEquals("a", instance.get(0, (Supplier<String>)null));
        assertEquals("b", instance.get(1, (Supplier<String>)null));
        assertNull(instance.get(2, (Supplier<String>)null));
        assertNull(instance.get(100, (Supplier<String>)null));
    }

}
