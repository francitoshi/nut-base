/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;

/**
 * The {@link Kr} backend that uses the providers of the running JDK, from
 * Java 8 to the last one.
 * <p>
 * Nothing is pinned: the algorithms are looked up in the default provider
 * order, which on a standard Java runtime means its own providers. Bouncy
 * Castle is never registered nor used here, but if the application itself
 * registers it, this backend will happily use it for the algorithms the JDK
 * does not implement.
 * <p>
 * The JDK adds algorithms as the versions go by, so what this backend can do
 * depends on the runtime: SHA-3 and ChaCha20-Poly1305 need Java 9 or later,
 * Ed25519 and X25519 need Java 15 or later. Ask
 * {@link Kr#supports(io.nut.base.crypto.Kr.HashAlgorithm)} before relying on
 * them, or use {@link Kr#getInstance(boolean)} to get Bouncy Castle when it is
 * available.
 *
 * @author franci
 * @see Kr
 * @see KrBC
 */
public class KrJdk extends Kr
{
    /**
     * Name of this backend.
     */
    public static final String NAME = "jdk";

    private static final class SecureRandomHolder
    {
        static final SecureRandom SECURE_RANDOM = new SecureRandom();
    }

    private static final class InstanceHolder
    {
        static final KrJdk INSTANCE = new KrJdk();
    }

    static KrJdk instance()
    {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Creates a new instance. {@link #getInstance()} returns a shared one, so
     * creating instances is only necessary to pin a class loader or a
     * security manager.
     */
    public KrJdk()
    {
        super();
    }

    @Override
    public String name()
    {
        return NAME;
    }

    @Override
    protected MessageDigest messageDigest(String jcaName)
    {
        return jdk(jcaName, () -> MessageDigest.getInstance(jcaName));
    }

    @Override
    protected Mac mac(String jcaName)
    {
        return jdk(jcaName, () -> Mac.getInstance(jcaName));
    }

    @Override
    protected Cipher cipher(String transformation)
    {
        return jdk(transformation, () -> Cipher.getInstance(transformation));
    }

    @Override
    protected KeyPairGenerator keyPairGenerator(String jcaName)
    {
        return jdk(jcaName, () -> KeyPairGenerator.getInstance(jcaName));
    }

    @Override
    protected KeyFactory keyFactory(String jcaName)
    {
        return jdk(jcaName, () -> KeyFactory.getInstance(jcaName));
    }

    @Override
    protected Signature signature(String jcaName)
    {
        return jdk(jcaName, () -> Signature.getInstance(jcaName));
    }

    @Override
    protected KeyAgreement keyAgreement(String jcaName)
    {
        return jdk(jcaName, () -> KeyAgreement.getInstance(jcaName));
    }

    @Override
    protected SecureRandom secureRandom()
    {
        return SecureRandomHolder.SECURE_RANDOM;
    }

    @Override
    protected AlgorithmParameterSpec ecParameters(EcCurve curve)
    {
        require(curve, "curve");
        return new ECGenParameterSpec(curve.jcaName());
    }

    @Override
    protected byte[] deriveArgon2(Argon2Algorithm algorithm, byte[] password, byte[] salt,
            int memoryKb, int iterations, int parallelism, int outputBytes)
    {
        throw unsupported("argon2");
    }

    @Override
    protected byte[] deriveScrypt(byte[] password, byte[] salt, int cost, int blockSize,
            int parallelism, int outputBytes)
    {
        throw unsupported("scrypt");
    }

    @Override
    protected String deriveBcrypt(char[] password, int cost)
    {
        throw unsupported("bcrypt");
    }

    @Override
    protected boolean verifyBcryptHash(String encoded, char[] password)
    {
        throw unsupported("bcrypt");
    }

    @Override
    protected String unavailableAdvice()
    {
        return "add Bouncy Castle to the classpath and use Kr.getInstance(true), or run on a Java version that implements it";
    }

    private <T> T jdk(String jcaName, JcaOperation<T> operation)
    {
        try
        {
            return operation.get();
        }
        catch (GeneralSecurityException ex)
        {
            throw unsupported(jcaName, ex);
        }
    }

    @FunctionalInterface
    private interface JcaOperation<T>
    {
        T get() throws GeneralSecurityException;
    }
}
