/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecureSecretKeyTest
{
    private static byte[] material(int size)
    {
        byte[] sevens = new byte[size];
        Arrays.fill(sevens, (byte) 7);
        return sevens;
    }

    // ---------------------------------------------------------------- construction

    @Test
    public void testConstructor()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");
        assertEquals("AES", key.getAlgorithm());
        assertEquals("RAW", key.getFormat());
        assertFalse(key.isDestroyed());
        assertArrayEquals(material(32), key.getEncoded());
    }

    @Test
    public void testConstructorRejectsInvalid()
    {
        assertThrows(IllegalArgumentException.class, () -> new SecureSecretKey(null, "AES"));
        assertThrows(IllegalArgumentException.class, () -> new SecureSecretKey(new byte[0], "AES"));
        assertThrows(IllegalArgumentException.class, () -> new SecureSecretKey(material(32), null));
        assertThrows(IllegalArgumentException.class, () -> new SecureSecretKey(material(32), ""));
        assertThrows(IllegalArgumentException.class, () -> new SecureSecretKey(material(32), "  "));
    }

    @Test
    public void testAlgorithmIsTrimmed()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "  AES ");
        assertEquals("AES", key.getAlgorithm());
    }

    /** A defensive copy must be stored, otherwise the caller can mutate the key afterwards. */
    @Test
    public void testDefensiveCopyOnConstruction()
    {
        byte[] source = material(32);
        SecureSecretKey key = new SecureSecretKey(source, "AES");

        Arrays.fill(source, (byte) 0);
        assertArrayEquals(material(32), key.getEncoded());
    }

    /** The constructor must leave the caller's array untouched (documented ownership). */
    @Test
    public void testConstructorDoesNotZeroCallersArray()
    {
        byte[] source = material(32);
        new SecureSecretKey(source, "AES").destroy();
        assertArrayEquals(material(32), source);
    }

    @Test
    public void testDefensiveCopyOnGetEncoded()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");

        byte[] first = key.getEncoded();
        byte[] second = key.getEncoded();
        assertNotSame(first, second);

        Arrays.fill(first, (byte) 0);
        assertArrayEquals(material(32), key.getEncoded());
    }

    // ---------------------------------------------------------------- destruction

    @Test
    public void testDestroy()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");
        key.destroy();

        assertTrue(key.isDestroyed());
        assertThrows(IllegalStateException.class, key::getEncoded);
        assertThrows(IllegalStateException.class, key::getAlgorithm);
        assertThrows(IllegalStateException.class, key::getFormat);
    }

    /** destroy() must really zero the array it owns, not only drop the reference. */
    @Test
    public void testDestroyZeroesMaterial() throws Exception
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");
        Field field = SecureSecretKey.class.getDeclaredField("keyMaterial");
        field.setAccessible(true);
        byte[] held = (byte[]) field.get(key);
        assertArrayEquals(material(32), held);

        key.destroy();

        assertArrayEquals(new byte[32], held, "the held array must be zeroed");
        assertNull(field.get(key), "the reference must be dropped");
    }

    @Test
    public void testClose()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");
        key.close();
        assertTrue(key.isDestroyed());
    }

    @Test
    public void testTryWithResources()
    {
        SecureSecretKey outer;
        try (SecureSecretKey key = new SecureSecretKey(material(32), "AES"))
        {
            outer = key;
            assertFalse(key.isDestroyed());
        }
        assertTrue(outer.isDestroyed());
    }

    @Test
    public void testDestroyIsIdempotent()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");

        assertDoesNotThrow(() ->
        {
            key.destroy();
            key.destroy();
            key.close();
        });
        assertTrue(key.isDestroyed());
    }

    @Test
    public void testConcurrentDestroy() throws InterruptedException
    {
        final SecureSecretKey key = new SecureSecretKey(material(32), "AES");
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
                    key.destroy();
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
        assertTrue(key.isDestroyed());
    }

    // ---------------------------------------------------------------- serialization

    @Test
    public void testSerializationIsRefused() throws Exception
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertThrows(NotSerializableException.class, () ->
        {
            try (ObjectOutputStream oos = new ObjectOutputStream(bos))
            {
                oos.writeObject(key);
            }
        });
    }

    /**
     * A valid stream for this class cannot be produced (writeObject refuses), so the
     * private readObject hook is invoked directly.
     */
    @Test
    public void testDeserializationIsRefused() throws Exception
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");
        Method readObject = SecureSecretKey.class.getDeclaredMethod("readObject", ObjectInputStream.class);
        readObject.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> readObject.invoke(key, new Object[]{null}));
        assertTrue(ex.getCause() instanceof NotSerializableException);
    }

    // ---------------------------------------------------------------- equals / hashCode

    @Test
    public void testEqualsAndHashCode()
    {
        SecureSecretKey a = new SecureSecretKey(material(32), "AES");
        SecureSecretKey b = new SecureSecretKey(material(32), "AES");
        SecureSecretKey c = new SecureSecretKey(material(16), "AES");
        SecureSecretKey d = new SecureSecretKey(material(32), "HmacSHA256");

        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c, "different material");
        assertNotEquals(a, d, "different algorithm");
        assertNotEquals(a, null);
        assertNotEquals(a, "AES");
    }

    @Test
    public void testEqualsIgnoresAlgorithmCase()
    {
        SecureSecretKey upper = new SecureSecretKey(material(32), "AES");
        SecureSecretKey lower = new SecureSecretKey(material(32), "aes");

        assertEquals(upper, lower);
        assertEquals(upper.hashCode(), lower.hashCode());
    }

    /**
     * Only SecureSecretKey instances can be equal. A SecretKeySpec hashes its
     * material while this class deliberately does not, so equality across both
     * types would break the equals/hashCode contract. Compare the encoded bytes
     * instead when mixing key types.
     *
     * <p>Only the direction this class controls is asserted: the JDK's
     * {@code SecretKeySpec.equals} accepts any {@code SecretKey} with the same
     * material, so {@code spec.equals(key)} is {@code true} regardless.</p>
     */
    @Test
    public void testNotEqualToSecretKeySpec()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");
        SecretKeySpec spec = new SecretKeySpec(material(32), "AES");

        assertNotEquals(key, spec);
        assertTrue(MessageDigest.isEqual(key.getEncoded(), spec.getEncoded()));
    }

    /**
     * A key is equal to itself even when destroyed (reflexivity), but two distinct
     * keys are never equal once either is destroyed.
     */
    @Test
    public void testDestroyedKeyEquality()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");
        SecureSecretKey twin = new SecureSecretKey(material(32), "AES");
        SecureSecretKey destroyedTwin = new SecureSecretKey(material(32), "AES");
        destroyedTwin.destroy();

        key.destroy();

        assertEquals(key, key);
        assertNotEquals(key, twin);
        assertNotEquals(twin, key);
        assertNotEquals(key, destroyedTwin);
        assertNotEquals(destroyedTwin, key);
    }

    @Test
    public void testHashCodeWorksOnDestroyedKey()
    {
        SecureSecretKey key = new SecureSecretKey(material(32), "AES");
        int before = key.hashCode();
        key.destroy();
        assertDoesNotThrow(key::hashCode);
        assertEquals(before, key.hashCode());
    }

    /** hashCode must not depend on the key material. */
    @Test
    public void testHashCodeDoesNotLeakMaterial()
    {
        SecureSecretKey a = new SecureSecretKey(material(32), "AES");
        SecureSecretKey b = new SecureSecretKey(new byte[32], "AES");

        assertEquals(a.hashCode(), b.hashCode());
    }

    /**
     * equals() racing with destroy() used to be able to throw a NullPointerException.
     * The only acceptable outcomes are true or false, never an exception.
     */
    @Test
    public void testEqualsRacingWithDestroy() throws Exception
    {
        final AtomicInteger errors = new AtomicInteger();

        for(int i = 0; i < 200; i++)
        {
            final SecureSecretKey a = new SecureSecretKey(material(32), "AES");
            final SecureSecretKey b = new SecureSecretKey(material(32), "AES");
            final CountDownLatch start = new CountDownLatch(1);

            Thread destroyer = new Thread(() ->
            {
                try
                {
                    start.await();
                    a.destroy();
                }
                catch (Throwable t)
                {
                    errors.incrementAndGet();
                }
            });
            Thread reverse = new Thread(() ->
            {
                try
                {
                    start.await();
                    b.equals(a); // opposite direction: must not deadlock either
                }
                catch (Throwable t)
                {
                    errors.incrementAndGet();
                }
            });
            destroyer.start();
            reverse.start();
            start.countDown();
            try
            {
                a.equals(b);
            }
            catch (Throwable t)
            {
                errors.incrementAndGet();
            }
            destroyer.join(10_000);
            reverse.join(10_000);
            assertFalse(destroyer.isAlive() || reverse.isAlive(), "possible deadlock");
        }
        assertEquals(0, errors.get());
    }
}
