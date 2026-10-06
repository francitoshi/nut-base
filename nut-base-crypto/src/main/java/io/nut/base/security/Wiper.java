/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

/**
 * Best-effort utilities for wiping cryptographic key material from memory.
 *
 * <p><b>Reliability by Java version.</b> Wiping a {@link SecretKeySpec} needs
 * reflection on a private JDK field. That works on Java 8, prints an
 * "illegal reflective access" warning on 9-15, is denied by default on 16 and
 * is always denied on 17+ unless the JVM runs with
 * {@code --add-opens java.base/javax.crypto.spec=ALL-UNNAMED}. When denied, the
 * methods return {@code false} and the key stays on the heap. Code that must
 * wipe its own key material should keep the bytes in an array it owns (see
 * {@link SecureSecretKey}) and zero it directly instead of relying on this
 * class.</p>
 *
 * <p>Java's garbage collector may also have moved or copied the data before it
 * is wiped; for stronger guarantees use an HSM or a secure enclave.</p>
 */
public final class Wiper
{
    private Wiper()
    {
    }

    /**
     * Attempts to wipe the key material held by a {@link SecretKeySpec} via
     * reflection on its private {@code key} field.
     *
     * @param key the key to wipe; may be {@code null}
     * @return {@code true} only if a non-null array was found and zeroed;
     *         {@code false} if {@code key} is {@code null}, access is denied
     *         (JDK 16+ without {@code --add-opens}), the field does not exist,
     *         or the field holds no array.
     */
    public static boolean wipeSecretKeySpec(SecretKeySpec key)
    {
        if(key == null)
        {
            return false;
        }
        try
        {
            Field keyField = SecretKeySpec.class.getDeclaredField("key");
            keyField.setAccessible(true);
            byte[] keyBytes = (byte[]) keyField.get(key);
            if(keyBytes == null)
            {
                return false;
            }
            Arrays.fill(keyBytes, (byte) 0);
            return true;
        }
        catch (NoSuchFieldException | IllegalAccessException | RuntimeException ex)
        {
            // InaccessibleObjectException (JDK 9+) and SecurityException are RuntimeExceptions
            return false;
        }
    }

    /**
     * Attempts to wipe a generic {@link SecretKey}, trying in order:
     * <ol>
     * <li>{@link SecretKey#destroy()} (reliable for implementations that
     * support it, such as {@link SecureSecretKey}; the default implementation
     * throws on most JDK keys);</li>
     * <li>{@link #wipeSecretKeySpec(SecretKeySpec)} for {@code SecretKeySpec};</li>
     * <li>reflection over the non-static {@code byte[]} fields of the key's
     * class and its superclasses.</li>
     * </ol>
     *
     * <p>Best effort only: key material may live in other field types, in
     * native memory, or be unreachable due to module encapsulation.</p>
     *
     * @param key the key to wipe; may be {@code null}
     * @return {@code true} if the key was destroyed or at least one non-null
     *         {@code byte[]} field was zeroed; {@code false} otherwise.
     */
    public static boolean wipeSecretKey(SecretKey key)
    {
        if(key == null)
        {
            return false;
        }

        try
        {
            key.destroy();
            if(key.isDestroyed())
            {
                return true;
            }
        }
        catch (DestroyFailedException | RuntimeException ex)
        {
            // not supported by this key: fall through to reflection
        }

        if(key instanceof SecretKeySpec && wipeSecretKeySpec((SecretKeySpec) key))
        {
            return true;
        }

        boolean wiped = false;
        for(Class<?> c = key.getClass(); c != null && c != Object.class; c = c.getSuperclass())
        {
            for(Field field : c.getDeclaredFields())
            {
                if(field.getType() != byte[].class || Modifier.isStatic(field.getModifiers()))
                {
                    continue;
                }
                try
                {
                    field.setAccessible(true);
                    byte[] array = (byte[]) field.get(key);
                    if(array != null)
                    {
                        Arrays.fill(array, (byte) 0);
                        wiped = true;
                    }
                }
                catch (IllegalAccessException | RuntimeException ex)
                {
                    // inaccessible (e.g. InaccessibleObjectException on JDK 9+): skip
                }
            }
        }
        return wiped;
    }
}
