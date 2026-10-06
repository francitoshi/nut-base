/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.BadPaddingException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecureBytesTest
{
    private static final String HELLO_WORLD = "hello world";

    private static byte[] hello()
    {
        return HELLO_WORLD.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] field(SecureBytes instance, String name) throws Exception
    {
        Field f = SecureBytes.class.getDeclaredField(name);
        f.setAccessible(true);
        return (byte[]) f.get(instance);
    }

    private static boolean allZero(byte[] array)
    {
        for(byte b : array)
        {
            if(b != 0)
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
        SecureBytes instance = new SecureBytes(null);

        assertNull(instance.getBytes());
        assertTrue(instance.isDestroyed());

        final AtomicBoolean called = new AtomicBoolean();
        final AtomicReference<byte[]> ref = new AtomicReference<>();
        instance.consume(bytes ->
        {
            called.set(true);
            ref.set(bytes);
        });
        assertTrue(called.get());
        assertNull(ref.get());

        assertDoesNotThrow(() ->
        {
            instance.close();
            instance.destroy();
        });
    }

    @Test
    public void testEmpty()
    {
        SecureBytes instance = new SecureBytes(new byte[0]);

        assertNotNull(instance.getBytes());
        assertEquals(0, instance.getBytes().length);
        assertTrue(instance.isDestroyed());

        final AtomicInteger length = new AtomicInteger(-1);
        instance.consume(bytes -> length.set(bytes.length));
        assertEquals(0, length.get());

        assertDoesNotThrow(() ->
        {
            instance.close();
            instance.destroy();
        });
    }

    // ---------------------------------------------------------------- round trip

    @Test
    public void testGetBytesRoundTrip()
    {
        try (SecureBytes instance = new SecureBytes(hello()))
        {
            assertArrayEquals(hello(), instance.getBytes());
        }
    }

    @Test
    public void testExplicitNullKripto()
    {
        try (SecureBytes instance = new SecureBytes(hello(), null))
        {
            assertArrayEquals(hello(), instance.getBytes());
        }
    }

    @Test
    public void testLargePayload()
    {
        byte[] big = new byte[1 << 20];
        for(int i = 0; i < big.length; i++)
        {
            big[i] = (byte) i;
        }
        byte[] expected = big.clone();

        try (SecureBytes instance = new SecureBytes(big))
        {
            assertArrayEquals(expected, instance.getBytes());
        }
    }

    @Test
    public void testCiphertextDoesNotContainPlaintext() throws Exception
    {
        byte[] plain = hello();
        try (SecureBytes instance = new SecureBytes(hello()))
        {
            byte[] encrypted = field(instance, "encryptedData");
            // GCM adds a 128 bit (16 byte) tag
            assertEquals(plain.length + 16, encrypted.length);
            byte[] prefix = new byte[plain.length];
            System.arraycopy(encrypted, 0, prefix, 0, plain.length);
            assertFalse(java.util.Arrays.equals(plain, prefix));
        }
    }

    @Test
    public void testTwoInstancesUseDifferentIvKeyAndCiphertext() throws Exception
    {
        try (SecureBytes a = new SecureBytes(hello());
             SecureBytes b = new SecureBytes(hello()))
        {
            assertFalse(java.util.Arrays.equals(field(a, "iv"), field(b, "iv")));
            assertFalse(java.util.Arrays.equals(field(a, "key"), field(b, "key")));
            assertFalse(java.util.Arrays.equals(field(a, "encryptedData"), field(b, "encryptedData")));
        }
    }

    // ---------------------------------------------------------------- wiping

    @Test
    public void testConstructorZeroesInput()
    {
        byte[] input = hello();
        SecureBytes instance = new SecureBytes(input);
        assertTrue(allZero(input), "the constructor must zero the caller's array");
        instance.close();
    }

    @Test
    public void testGetBytesReturnsIndependentCopies()
    {
        try (SecureBytes instance = new SecureBytes(hello()))
        {
            byte[] first = instance.getBytes();
            byte[] second = instance.getBytes();
            assertNotSame(first, second);

            java.util.Arrays.fill(first, (byte) 0);
            assertArrayEquals(hello(), instance.getBytes());
        }
    }

    @Test
    public void testConsumeDeliversPlaintextAndWipesTemporaryBuffer()
    {
        final AtomicReference<byte[]> reference = new AtomicReference<>();
        final AtomicReference<byte[]> copy = new AtomicReference<>();

        try (SecureBytes instance = new SecureBytes(hello()))
        {
            instance.consume(bytes ->
            {
                reference.set(bytes);
                copy.set(bytes.clone());
            });
        }

        assertArrayEquals(hello(), copy.get());
        assertTrue(allZero(reference.get()), "the temporary plaintext must be wiped after consume()");
    }

    @Test
    public void testConsumeWipesTemporaryBufferEvenIfConsumerThrows()
    {
        final AtomicReference<byte[]> reference = new AtomicReference<>();

        try (SecureBytes instance = new SecureBytes(hello()))
        {
            assertThrows(IllegalArgumentException.class, () -> instance.consume(bytes ->
            {
                reference.set(bytes);
                throw new IllegalArgumentException("boom");
            }));
        }

        assertNotNull(reference.get());
        assertTrue(allZero(reference.get()));
    }

    @Test
    public void testDestroyWipesMaterial() throws Exception
    {
        SecureBytes instance = new SecureBytes(hello());

        // before: all three arrays hold something (a random 12/32 byte array being all zero is negligible)
        for(String name : new String[]{"key", "iv", "encryptedData"})
        {
            assertFalse(allZero(field(instance, name)), name + " should hold data before destroy()");
        }

        instance.destroy();

        assertTrue(instance.isDestroyed());
        for(String name : new String[]{"key", "iv", "encryptedData"})
        {
            byte[] wiped = field(instance, name);
            assertNotNull(wiped, name + " should still be allocated after destroy()");
            assertTrue(allZero(wiped), name + " was not wiped");
        }
    }

    // ---------------------------------------------------------------- integrity

    @Test
    public void testTamperedCiphertextIsDetected() throws Exception
    {
        try (SecureBytes instance = new SecureBytes(hello()))
        {
            field(instance, "encryptedData")[0] ^= 1;

            RuntimeException ex = assertThrows(RuntimeException.class, instance::getBytes);
            assertTrue(ex.getCause() instanceof BadPaddingException,
                    "GCM authentication failure expected, got " + ex.getCause());
        }
    }

    // ---------------------------------------------------------------- lifecycle

    @Test
    public void testIsDestroyedLifecycle()
    {
        SecureBytes instance = new SecureBytes(hello());
        assertFalse(instance.isDestroyed());
        instance.destroy();
        assertTrue(instance.isDestroyed());
    }

    @Test
    public void testCloseDestroys()
    {
        SecureBytes instance = new SecureBytes(hello());
        instance.close();
        assertTrue(instance.isDestroyed());
    }

    @Test
    public void testTryWithResourcesDestroys()
    {
        SecureBytes outer;
        try (SecureBytes instance = new SecureBytes(hello()))
        {
            outer = instance;
            assertFalse(instance.isDestroyed());
        }
        assertTrue(outer.isDestroyed());
    }

    @Test
    public void testUseAfterDestroyThrowsIllegalState()
    {
        SecureBytes instance = new SecureBytes(hello());
        instance.destroy();

        assertThrows(IllegalStateException.class, instance::getBytes);
        assertThrows(IllegalStateException.class, () -> instance.consume(bytes -> fail("must not be called")));
    }

    @Test
    public void testDestroyIsIdempotent()
    {
        SecureBytes instance = new SecureBytes(hello());
        assertDoesNotThrow(() ->
        {
            instance.destroy();
            instance.destroy();
            instance.close();
        });
        assertTrue(instance.isDestroyed());
    }

    // ---------------------------------------------------------------- concurrency

    @Test
    public void testConcurrentDestroy() throws Exception
    {
        final SecureBytes instance = new SecureBytes(hello());
        final AtomicInteger errors = new AtomicInteger();
        final int threads = 8;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);

        for(int i = 0; i < threads; i++)
        {
            new Thread(() ->
            {
                try
                {
                    start.await();
                    instance.destroy();
                }
                catch (Throwable t)
                {
                    errors.incrementAndGet();
                }
                finally
                {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));

        assertEquals(0, errors.get());
        assertTrue(instance.isDestroyed());
    }

    /**
     * getBytes() racing with destroy() may only ever produce the correct plaintext
     * or an IllegalStateException; never garbage, a tag failure or another exception.
     */
    @Test
    public void testGetBytesRacingWithDestroy() throws Exception
    {
        for(int i = 0; i < 200; i++)
        {
            final SecureBytes instance = new SecureBytes(hello());
            final CountDownLatch start = new CountDownLatch(1);
            Thread destroyer = new Thread(() ->
            {
                try
                {
                    start.await();
                    instance.destroy();
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            });
            destroyer.start();
            start.countDown();
            try
            {
                assertArrayEquals(hello(), instance.getBytes());
            }
            catch (IllegalStateException expected)
            {
                // destroyed first: allowed
            }
            destroyer.join();
            assertTrue(instance.isDestroyed());
        }
    }
}
