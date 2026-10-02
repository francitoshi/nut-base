/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import java.security.Security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The factory that chooses between the two backends, the part that decides
 * what {@code Kr.getInstance()} gives back.
 *
 * @author franci
 */
public class KrFactoryTest
{
    @Test
    public void withoutArgumentsTheJdkIsUsed()
    {
        Kr kr = Kr.getInstance();
        assertNotNull(kr);
        assertSame(KrJdk.NAME, kr.name());
        assertTrue(kr instanceof KrJdk);
    }

    @Test
    public void falseIsTheJdk()
    {
        Kr kr = Kr.getInstance(false);
        assertTrue(kr instanceof KrJdk, "asking for no Bouncy Castle must not give Bouncy Castle");
        assertEquals(KrJdk.NAME, kr.name());
    }

    @Test
    public void trueIsBouncyCastleWhenItIsThere()
    {
        assumeTrue(Kr.isBouncyCastleAvailable(), "Bouncy Castle is not on the classpath");
        Kr kr = Kr.getInstance(true);
        assertTrue(kr instanceof KrBC, "asking for Bouncy Castle must give Bouncy Castle");
        assertEquals(Kr.BOUNCY_CASTLE_PROVIDER, kr.name());
        assertEquals(Kr.BOUNCY_CASTLE_PROVIDER, kr.providerName());
    }

    @Test
    public void trueFallsBackToTheJdkWhenBouncyCastleIsNotThere()
    {
        //this test runs the same code as getInstance(true) would run in an
        //application without Bouncy Castle: the check is the class loader
        assertEquals(Kr.isBouncyCastleAvailable(), isBouncyCastleAvailable(Kr.class.getClassLoader()));
        if (!Kr.isBouncyCastleAvailable())
        {
            assertTrue(Kr.getInstance(true) instanceof KrJdk, "without Bouncy Castle the only backend is the JDK one");
        }
    }

    @Test
    public void trueNeverThrowsBecauseOfAMissingBouncyCastle()
    {
        //the class that references the provider is only loaded after the check,
        //so a class loader without Bouncy Castle gets a working backend anyway
        ClassLoader withoutIt = new ClassLoader(Kr.class.getClassLoader())
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name != null && name.startsWith("org.bouncycastle"))
                {
                    throw new ClassNotFoundException(name + " is not available here");
                }
                return super.loadClass(name, resolve);
            }
        };
        assertFalse(isBouncyCastleAvailable(withoutIt), "this loader must not find Bouncy Castle");
        assertFalse(isBouncyCastleAvailable(null), "there is no class loader to look into");
        assertDoesNotThrow(() -> Kr.getInstance(true), "the fallback must not touch the missing classes");
    }

    @Test
    public void theSharedInstancesAreShared()
    {
        assertSame(Kr.getInstance(), Kr.getInstance());
        assertSame(Kr.getInstance(false), Kr.getInstance());
        assertSame(Kr.getInstance(false), Kr.getInstance(false));
        assertSame(Kr.getInstance(true), Kr.getInstance(true));
    }

    @Test
    public void bothBackendsAgreeOnTheSameAnswer()
    {
        byte[] data = "the same input for both backends".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertArrayEquals(Kr.getInstance(false).hash.sha256(data), Kr.getInstance(true).hash.sha256(data));
        assertEquals(Kr.getInstance(false).hash.hex(Kr.HashAlgorithm.SHA256, data),
                Kr.getInstance(true).hash.hex(Kr.HashAlgorithm.SHA256, data));

        byte[] key = Kr.getInstance(true).random.bytes(16);
        Kr.Aead jdk = Kr.getInstance(false).aeads.aesGcm(key);
        Kr.Aead bc = Kr.getInstance(true).aeads.aesGcm(key);
        byte[] sealedByJdk = jdk.seal(data);
        assertArrayEquals(data, bc.open(sealedByJdk), "what the JDK seals, Bouncy Castle opens");
        byte[] sealedByBc = bc.seal(data);
        assertArrayEquals(data, jdk.open(sealedByBc), "and the other way around");
    }

    @Test
    public void bouncyCastleIsRegisteredOnceAndAtTheEnd()
    {
        assumeTrue(Kr.isBouncyCastleAvailable(), "Bouncy Castle is not on the classpath");
        Kr.getInstance(true);
        Kr.getInstance(true);
        java.security.Provider provider = Security.getProvider(Kr.BOUNCY_CASTLE_PROVIDER);
        assertNotNull(provider, "the backend must register the provider it uses");
        assertSame(provider, Kr.getInstance(true).provider());
        assertEquals(Kr.getInstance(true).provider(), Security.getProviders()[Security.getProviders().length - 1],
                "the provider must be appended, not prepended, so nothing else changes");
    }

    @Test
    public void bouncyCastleVersionIsReadable()
    {
        if (Kr.isBouncyCastleAvailable())
        {
            //the version comes from the library itself, so it looks like one
            String version = Kr.bouncyCastleVersion();
            assertNotNull(version);
            assertTrue(version.matches("\\d+(\\.\\d+)+"), version);
            assertEquals(version, Kr.bouncyCastleVersion(), "asking twice must give the same answer");
        }
        else
        {
            assertNull(Kr.bouncyCastleVersion(), "there is no version to report without Bouncy Castle");
        }
    }

    private static boolean isBouncyCastleAvailable(ClassLoader classLoader)
    {
        return Kr.isBouncyCastleAvailable(classLoader);
    }
}