/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import io.nut.base.crypto.Kripto;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecureCharsTest
{
    private static final String HELLO_WORLD = "hello world";

    /**
     * Non-ASCII on purpose, written with escapes so the test does not depend on
     * the source file encoding: n with tilde, euro sign and an emoji (surrogate pair).
     * It exposes platform default charset problems (Cp1252 on Java 8 vs UTF-8 on 18+).
     */
    private static final String NON_ASCII = "contrase\u00f1a\u20ac\uD83D\uDE00";

    private static boolean allZero(char[] array)
    {
        for(char c : array)
        {
            if(c != '\0')
            {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- edge cases

    @Test
    public void testNull()
    {
        SecureChars instance = new SecureChars(null);

        assertNull(instance.getChars());
        assertTrue(instance.isDestroyed());

        final AtomicBoolean called = new AtomicBoolean();
        final AtomicReference<char[]> ref = new AtomicReference<>();
        instance.consume(chars ->
        {
            called.set(true);
            ref.set(chars);
        });
        assertTrue(called.get());
        assertNull(ref.get());

        assertNull(instance.apply(chars -> chars));

        assertDoesNotThrow(() ->
        {
            instance.close();
            instance.destroy();
        });
    }

    @Test
    public void testEmpty()
    {
        SecureChars instance = new SecureChars(new char[0]);

        char[] chars = instance.getChars();
        assertNotNull(chars);
        assertEquals(0, chars.length);
        assertTrue(instance.isDestroyed());
        instance.close();
    }

    // ---------------------------------------------------------------- round trip

    @Test
    public void testGetCharsRoundTrip()
    {
        try (SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray()))
        {
            assertArrayEquals(HELLO_WORLD.toCharArray(), instance.getChars());
        }
    }

    @Test
    public void testNonAsciiRoundTrip()
    {
        try (SecureChars instance = new SecureChars(NON_ASCII.toCharArray()))
        {
            assertArrayEquals(NON_ASCII.toCharArray(), instance.getChars());
        }
    }

    @Test
    public void testExplicitCharsets()
    {
        // ISO-8859-1 cannot encode the euro sign or the emoji, so only the n with tilde
        String latin = "contrase\u00f1a";

        try (SecureChars utf16 = new SecureChars(NON_ASCII.toCharArray(), StandardCharsets.UTF_16, null);
             SecureChars latin1 = new SecureChars(latin.toCharArray(), StandardCharsets.ISO_8859_1, null))
        {
            assertArrayEquals(NON_ASCII.toCharArray(), utf16.getChars());
            assertArrayEquals(latin.toCharArray(), latin1.getChars());
        }
    }

    @Test
    public void testKriptoFirstConstructor()
    {
        try (SecureChars instance = new SecureChars((Kripto) null, HELLO_WORLD.toCharArray()))
        {
            assertArrayEquals(HELLO_WORLD.toCharArray(), instance.getChars());
        }
    }

    @Test
    public void testNullCharsetDefaultsToUtf8()
    {
        try (SecureChars instance = new SecureChars(NON_ASCII.toCharArray(), null, null))
        {
            assertArrayEquals(NON_ASCII.toCharArray(), instance.getChars());
        }
    }

    // ---------------------------------------------------------------- wiping

    @Test
    public void testConstructorZeroesInput()
    {
        char[] input = HELLO_WORLD.toCharArray();
        SecureChars instance = new SecureChars(input);
        assertTrue(allZero(input), "the constructor must zero the caller's array");
        instance.close();
    }

    @Test
    public void testGetCharsReturnsIndependentCopies()
    {
        try (SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray()))
        {
            char[] first = instance.getChars();
            char[] second = instance.getChars();
            assertNotSame(first, second);

            java.util.Arrays.fill(first, '\0');
            assertArrayEquals(HELLO_WORLD.toCharArray(), instance.getChars());
        }
    }

    @Test
    public void testConsumeDeliversPlaintextAndWipesTemporaryBuffer()
    {
        final AtomicReference<char[]> reference = new AtomicReference<>();
        final AtomicReference<char[]> copy = new AtomicReference<>();

        try (SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray()))
        {
            instance.consume(chars ->
            {
                reference.set(chars);
                copy.set(chars.clone());
            });
        }

        assertArrayEquals(HELLO_WORLD.toCharArray(), copy.get());
        assertTrue(allZero(reference.get()), "the temporary plaintext must be wiped after consume()");
    }

    @Test
    public void testConsumeWipesTemporaryBufferEvenIfConsumerThrows()
    {
        final AtomicReference<char[]> reference = new AtomicReference<>();

        try (SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray()))
        {
            assertThrows(IllegalArgumentException.class, () -> instance.consume(chars ->
            {
                reference.set(chars);
                throw new IllegalArgumentException("boom");
            }));
        }

        assertNotNull(reference.get());
        assertTrue(allZero(reference.get()));
    }

    // ---------------------------------------------------------------- apply

    @Test
    public void testApplyReturnsResultAndWipesTemporaryBuffer()
    {
        final AtomicReference<char[]> reference = new AtomicReference<>();

        try (SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray()))
        {
            String result = instance.apply(chars ->
            {
                reference.set(chars);
                return new String(chars);
            });
            assertEquals(HELLO_WORLD, result);
        }

        assertTrue(allZero(reference.get()), "the temporary plaintext must be wiped after apply()");
    }

    @Test
    public void testApplyWipesTemporaryBufferEvenIfFunctionThrows()
    {
        final AtomicReference<char[]> reference = new AtomicReference<>();

        try (SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray()))
        {
            assertThrows(IllegalStateException.class, () -> instance.apply(chars ->
            {
                reference.set(chars);
                throw new IllegalStateException("boom");
            }));
        }

        assertNotNull(reference.get());
        assertTrue(allZero(reference.get()));
    }

    // ---------------------------------------------------------------- lifecycle

    @Test
    public void testIsDestroyedLifecycle()
    {
        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());
        assertFalse(instance.isDestroyed());
        instance.destroy();
        assertTrue(instance.isDestroyed());
    }

    @Test
    public void testCloseDestroys()
    {
        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());
        instance.close();
        assertTrue(instance.isDestroyed());
    }

    @Test
    public void testTryWithResourcesDestroys()
    {
        SecureChars outer;
        try (SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray()))
        {
            outer = instance;
            assertFalse(instance.isDestroyed());
        }
        assertTrue(outer.isDestroyed());
    }

    @Test
    public void testUseAfterDestroyThrowsIllegalState()
    {
        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());
        instance.destroy();

        assertThrows(IllegalStateException.class, instance::getChars);
        assertThrows(IllegalStateException.class, () -> instance.consume(chars -> fail("must not be called")));
        assertThrows(IllegalStateException.class, () -> instance.apply(chars -> fail("must not be called")));
    }

    @Test
    public void testDestroyIsIdempotent()
    {
        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());
        assertDoesNotThrow(() ->
        {
            instance.destroy();
            instance.destroy();
            instance.close();
        });
        assertTrue(instance.isDestroyed());
    }
}
