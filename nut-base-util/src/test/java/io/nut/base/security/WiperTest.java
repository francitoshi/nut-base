/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import java.lang.reflect.Field;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Wiping a {@link SecretKeySpec} needs reflection into the JDK. It works on Java 8,
 * warns on 9-15 and is denied on 17+ unless the JVM runs with
 * {@code --add-opens java.base/javax.crypto.spec=ALL-UNNAMED}.
 *
 * <p>Each JDK-dependent behaviour is split in two tests guarded by assumptions, so
 * that exactly one of them runs on any given JVM. Run the suite in CI both with and
 * without that flag (and on Java 8, 17, 21 and 26) so both branches are really
 * exercised.</p>
 */
public class WiperTest
{
    private static byte[] ones()
    {
        byte[] ones = new byte[32];
        Arrays.fill(ones, (byte) 1);
        return ones;
    }

    private static boolean reflectionAvailable()
    {
        try
        {
            Field keyField = SecretKeySpec.class.getDeclaredField("key");
            keyField.setAccessible(true);
            return true;
        }
        catch (NoSuchFieldException | RuntimeException ex)
        {
            return false;
        }
    }

    // ---------------------------------------------------------------- test keys (no JDK flags needed)

    /** Holds an inherited instance array. */
    static class BaseKey implements SecretKey
    {
        private static final long serialVersionUID = 1L;
        protected final byte[] inherited = {1, 2, 3};

        @Override
        public String getAlgorithm()
        {
            return "TEST";
        }

        @Override
        public String getFormat()
        {
            return "RAW";
        }

        @Override
        public byte[] getEncoded()
        {
            return inherited.clone();
        }
    }

    /** Adds an own instance array, a static array and a null array. */
    static final class ChildKey extends BaseKey
    {
        private static final long serialVersionUID = 1L;
        static final byte[] SHARED = {9, 9, 9};
        private final byte[] own = {4, 5, 6};
        @SuppressWarnings("unused")
        private final byte[] absent = null;

        byte[] own()
        {
            return own;
        }
    }

    /** destroy() blows up with a runtime exception; the wipe must still fall back to reflection. */
    static final class ExplodingKey extends BaseKey
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void destroy()
        {
            throw new UnsupportedOperationException("boom");
        }
    }

    /** No array has been allocated at all. */
    static final class EmptyKey implements SecretKey
    {
        private static final long serialVersionUID = 1L;
        @SuppressWarnings("unused")
        private final byte[] material = null;

        @Override
        public String getAlgorithm()
        {
            return "TEST";
        }

        @Override
        public String getFormat()
        {
            return "RAW";
        }

        @Override
        public byte[] getEncoded()
        {
            return null;
        }
    }

    // ---------------------------------------------------------------- SecretKeySpec

    @Test
    public void testWipeSecretKeySpecWhenReflectionAllowed()
    {
        assumeTrue(reflectionAvailable());

        SecretKeySpec key = new SecretKeySpec(ones(), "AES");
        assertTrue(Wiper.wipeSecretKeySpec(key));
        assertArrayEquals(new byte[32], key.getEncoded());
    }

    @Test
    public void testWipeSecretKeySpecWhenReflectionDenied()
    {
        assumeFalse(reflectionAvailable());

        SecretKeySpec key = new SecretKeySpec(ones(), "AES");
        assertFalse(Wiper.wipeSecretKeySpec(key));
        assertArrayEquals(ones(), key.getEncoded(), "a denied wipe must leave the key intact");
    }

    @Test
    public void testWipeSecretKeyOnSpecWhenReflectionAllowed()
    {
        assumeTrue(reflectionAvailable());

        SecretKey key = new SecretKeySpec(ones(), "AES");
        assertTrue(Wiper.wipeSecretKey(key));
        assertArrayEquals(new byte[32], key.getEncoded());
    }

    @Test
    public void testWipeSecretKeyOnSpecWhenReflectionDenied()
    {
        assumeFalse(reflectionAvailable());

        SecretKey key = new SecretKeySpec(ones(), "AES");
        assertFalse(Wiper.wipeSecretKey(key));
        assertArrayEquals(ones(), key.getEncoded());
    }

    /** A best effort wipe must never propagate a failure, whatever the JVM allows. */
    @Test
    public void testWipeNeverThrows()
    {
        assertDoesNotThrow(() -> Wiper.wipeSecretKeySpec(new SecretKeySpec(ones(), "AES")));
        assertDoesNotThrow(() -> Wiper.wipeSecretKey(new SecretKeySpec(ones(), "AES")));
        assertDoesNotThrow(() -> Wiper.wipeSecretKey(new ExplodingKey()));
        assertDoesNotThrow(() -> Wiper.wipeSecretKey(new EmptyKey()));
    }

    @Test
    public void testWipeNull()
    {
        assertFalse(Wiper.wipeSecretKeySpec(null));
        assertFalse(Wiper.wipeSecretKey(null));
    }

    // ---------------------------------------------------------------- paths that need no flags

    /** SecureSecretKey supports destroy(), so it is wiped reliably on every JDK. */
    @Test
    public void testWipeSecureSecretKeyUsesDestroy()
    {
        SecureSecretKey key = new SecureSecretKey(ones(), "AES");

        assertTrue(Wiper.wipeSecretKey(key));
        assertTrue(key.isDestroyed());
        assertThrows(IllegalStateException.class, key::getEncoded);
    }

    /** Inherited instance arrays are wiped; static arrays are left alone. */
    @Test
    public void testWipeReflectsOverHierarchyAndSkipsStatics()
    {
        ChildKey key = new ChildKey();

        assertTrue(Wiper.wipeSecretKey(key));

        assertArrayEquals(new byte[3], key.own(), "own array");
        assertArrayEquals(new byte[3], key.getEncoded(), "inherited array");
        assertArrayEquals(new byte[]{9, 9, 9}, ChildKey.SHARED, "static array must not be touched");
    }

    /** A failing destroy() must not prevent the reflective fallback. */
    @Test
    public void testWipeFallsBackWhenDestroyThrows()
    {
        ExplodingKey key = new ExplodingKey();

        assertTrue(Wiper.wipeSecretKey(key));
        assertArrayEquals(new byte[3], key.getEncoded());
    }

    @Test
    public void testWipeReturnsFalseWhenNoArrayIsAllocated()
    {
        assertFalse(Wiper.wipeSecretKey(new EmptyKey()));
    }
}
