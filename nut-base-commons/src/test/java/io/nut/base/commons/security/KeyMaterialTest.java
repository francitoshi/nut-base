/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class KeyMaterialTest
{

    @TempDir
    Path dir;

    @Test
    public void testRandomKey()
    {
        byte[] key = KeyMaterial.randomKey();
        assertEquals(KeyMaterial.KEY_BYTES, key.length);
        assertFalse(java.util.Arrays.equals(key, KeyMaterial.randomKey()));
        Wiper.wipe(key);
        assertTrue(Arrays.equals(new byte[KeyMaterial.KEY_BYTES], key));
    }

    @Test
    public void testAesKey()
    {
        SecretKey key = KeyMaterial.aesKey(new byte[32]);
        assertEquals("AES", key.getAlgorithm());
        assertThrows(IllegalArgumentException.class, () -> KeyMaterial.aesKey(new byte[17]));
        assertThrows(IllegalArgumentException.class, () -> KeyMaterial.aesKey(new byte[0]));
    }

    @Test
    public void testDeriveKeyIsDeterministic()
    {
        char[] passphrase = "correct horse battery staple".toCharArray();
        byte[] salt = KeyMaterial.randomSalt();
        byte[] key1 = KeyMaterial.deriveKey(passphrase, salt, 1000);
        byte[] key2 = KeyMaterial.deriveKey(passphrase, salt, 1000);
        assertArrayEquals(key1, key2);
        assertEquals(KeyMaterial.KEY_BYTES, key1.length);
    }

    @Test
    public void testDeriveKeyDependsOnSaltAndPassphrase()
    {
        char[] passphrase = "correct horse battery staple".toCharArray();
        byte[] salt = KeyMaterial.randomSalt();
        byte[] key = KeyMaterial.deriveKey(passphrase, salt, 1000);

        byte[] otherSalt = KeyMaterial.randomSalt();
        assertFalse(Arrays.equals(key, KeyMaterial.deriveKey(passphrase, otherSalt, 1000)),
                "a different salt must derive a different key");

        assertFalse(Arrays.equals(key, KeyMaterial.deriveKey("other".toCharArray(), salt, 1000)),
                "a different passphrase must derive a different key");

        assertFalse(Arrays.equals(key, KeyMaterial.deriveKey(passphrase, salt, 2000)),
                "a different iteration count must derive a different key");
    }

    @Test
    public void testDeriveKeyRejectsInvalidArguments()
    {
        byte[] salt = KeyMaterial.randomSalt();
        assertThrows(NullPointerException.class, () -> KeyMaterial.deriveKey(null, salt, 1000));
        assertThrows(NullPointerException.class, () -> KeyMaterial.deriveKey("x".toCharArray(), null, 1000));
        assertThrows(IllegalArgumentException.class, () -> KeyMaterial.deriveKey(new char[0], salt, 1000));
        assertThrows(IllegalArgumentException.class, () -> KeyMaterial.deriveKey("x".toCharArray(), new byte[0], 1000));
        assertThrows(IllegalArgumentException.class, () -> KeyMaterial.deriveKey("x".toCharArray(), salt, 0));
    }

    @Test
    public void testKeyFileRoundTrip() throws IOException
    {
        Path file = dir.resolve("store.bin.key");
        byte[] key = KeyMaterial.randomKey();
        KeyMaterial.writeKeyFile(file, key);
        assertArrayEquals(key, KeyMaterial.readKeyFile(file));

        byte[] other = KeyMaterial.randomKey();
        KeyMaterial.writeKeyFile(file, other);
        assertArrayEquals(other, KeyMaterial.readKeyFile(file));
    }

    @Test
    public void testKeyFileIsNotWorldReadable() throws IOException
    {
        Path file = dir.resolve("store.bin.key");
        KeyMaterial.writeKeyFile(file, KeyMaterial.randomKey());
        try
        {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
            assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    permissions);
        }
        catch (UnsupportedOperationException e)
        {
            // not a POSIX filesystem
        }
    }

    @Test
    public void testKeyFileRejectsInvalidContent() throws IOException
    {
        Path file = dir.resolve("bad.bin.key");
        Files.write(file, new byte[7]);
        assertThrows(IOException.class, () -> KeyMaterial.readKeyFile(file));
    }
}
