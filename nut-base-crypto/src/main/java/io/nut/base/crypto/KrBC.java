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
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;

/**
 * The {@link Kr} backend that uses the Bouncy Castle provider.
 * <p>
 * Bouncy Castle implements everything the JDK leaves out, including SHA-3,
 * BLAKE2, ChaCha20-Poly1305, Ed25519, X25519 and the secp256k1 curve, so this
 * backend is the one that keeps the whole API available on Java 8.
 * <p>
 * The provider is registered with {@link Security#addProvider(Provider)} the
 * first time an instance is created, which is a global, one time change. It
 * does not replace any other provider, it is appended at the end of the list,
 * so the behaviour of the rest of the application does not change.
 * <p>
 * This class must not be loaded when Bouncy Castle is absent: it references
 * its provider class directly. {@link Kr#getInstance(boolean)} checks
 * {@link #isBouncyCastleAvailable()} before getting here.
 *
 * @author franci
 * @see Kr
 * @see KrJdk
 */
public class KrBC extends Kr
{
    /**
     * The Argon2 version of RFC 9106, the one every implementation writes in
     * the encoded form of a hash.
     */
    private static final int ARGON2_VERSION = Argon2Parameters.ARGON2_VERSION_13;

    /**
     * The bcrypt variant to write, the $2b$ one, which is the fixed one every
     * current implementation writes.
     */
    private static final String BCRYPT_VERSION = "2b";

    private final Provider provider;

    private static final class SecureRandomHolder
    {
        static final SecureRandom SECURE_RANDOM = create();

        private static SecureRandom create()
        {
            try
            {
                return SecureRandom.getInstance("DEFAULT", BOUNCY_CASTLE_PROVIDER);
            }
            catch (GeneralSecurityException ex)
            {
                return new SecureRandom();
            }
        }
    }

    private static final class InstanceHolder
    {
        static final KrBC INSTANCE = new KrBC();
    }

    static KrBC instance()
    {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Creates a new instance and registers the Bouncy Castle provider if it is
     * not registered yet. {@link Kr#getInstance(boolean)} returns a shared one,
     * so creating instances is only necessary to pin a class loader or a
     * security manager.
     *
     * @throws UnsupportedAlgorithmException when Bouncy Castle is not on the
     * classpath or cannot be registered
     */
    public KrBC()
    {
        Provider registered = Security.getProvider(BOUNCY_CASTLE_PROVIDER);
        if (registered == null)
        {
            try
            {
                Security.addProvider(new BouncyCastleProvider());
                registered = Security.getProvider(BOUNCY_CASTLE_PROVIDER);
            }
            catch (RuntimeException | LinkageError ex)
            {
                throw new UnsupportedAlgorithmException("Bouncy Castle is on the classpath but it cannot be registered: " + ex, ex);
            }
        }
        this.provider = registered;
    }

    @Override
    public String name()
    {
        return BOUNCY_CASTLE_PROVIDER;
    }

    @Override
    public Provider provider()
    {
        return provider;
    }

    @Override
    protected MessageDigest messageDigest(String jcaName)
    {
        return bc(jcaName, () -> MessageDigest.getInstance(jcaName, BOUNCY_CASTLE_PROVIDER));
    }

    @Override
    protected Mac mac(String jcaName)
    {
        return bc(jcaName, () -> Mac.getInstance(jcaName, BOUNCY_CASTLE_PROVIDER));
    }

    @Override
    protected Cipher cipher(String transformation)
    {
        return bc(transformation, () -> Cipher.getInstance(transformation, BOUNCY_CASTLE_PROVIDER));
    }

    @Override
    protected KeyPairGenerator keyPairGenerator(String jcaName)
    {
        return bc(jcaName, () -> KeyPairGenerator.getInstance(jcaName, BOUNCY_CASTLE_PROVIDER));
    }

    @Override
    protected KeyFactory keyFactory(String jcaName)
    {
        return bc(jcaName, () -> KeyFactory.getInstance(jcaName, BOUNCY_CASTLE_PROVIDER));
    }

    @Override
    protected Signature signature(String jcaName)
    {
        return bc(jcaName, () -> Signature.getInstance(jcaName, BOUNCY_CASTLE_PROVIDER));
    }

    @Override
    protected KeyAgreement keyAgreement(String jcaName)
    {
        return bc(jcaName, () -> KeyAgreement.getInstance(jcaName, BOUNCY_CASTLE_PROVIDER));
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
    protected byte[] deriveArgon2(Argon2Algorithm algorithm, byte[] password, byte[] salt, int memoryKb, int iterations, int parallelism, int outputBytes)
    {
        require(algorithm, "algorithm");
        require(password, "password");
        require(salt, "salt");
        //Argon2 is not exposed through the JCA, so the algorithm from Bouncy
        //Castle is used directly
        Argon2Parameters parameters = new Argon2Parameters.Builder(argon2Type(algorithm))
                .withVersion(ARGON2_VERSION)
                .withSalt(salt)
                .withMemoryAsKB(memoryKb)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .build();
        Argon2BytesGenerator argon2 = new Argon2BytesGenerator();
        argon2.init(parameters);
        byte[] hash = new byte[outputBytes];
        argon2.generateBytes(password, hash);
        return hash;
    }

    @Override
    protected byte[] deriveScrypt(byte[] password, byte[] salt, int cost, int blockSize, int parallelism, int outputBytes)
    {
        require(password, "password");
        require(salt, "salt");
        return SCrypt.generate(password, salt, cost, blockSize, parallelism, outputBytes);
    }

    @Override
    protected String deriveBcrypt(char[] password, int cost)
    {
        require(password, "password");
        byte[] salt = new byte[16];
        secureRandom().nextBytes(salt);
        try
        {
            return OpenBSDBCrypt.generate(BCRYPT_VERSION, password, salt, cost);
        }
        catch (IllegalArgumentException ex)
        {
            throw new IllegalArgumentException("bcrypt cannot hash this password with cost " + cost
                    + ": it must be between " + Password.MINIMUM_BCRYPT_COST
                    + " and " + Password.MAXIMUM_BCRYPT_COST, ex);
        }
    }

    @Override
    protected boolean verifyBcryptHash(String encoded, char[] password)
    {
        require(encoded, "encoded");
        require(password, "password");
        try
        {
            return OpenBSDBCrypt.checkPassword(encoded, password);
        }
        catch (IllegalArgumentException | DataLengthException ex)
        {
            //not even a hash I can compare against, so it is not a password
            throw new IllegalArgumentException("this is not a bcrypt hash I understand: " + encoded, ex);
        }
    }

    private int argon2Type(Argon2Algorithm algorithm)
    {
        switch (algorithm)
        {
            case D:
                return Argon2Parameters.ARGON2_d;
            case I:
                return Argon2Parameters.ARGON2_i;
            case ID:
                return Argon2Parameters.ARGON2_id;
            default:
                throw new IllegalArgumentException("unknown Argon2 variant " + algorithm);
        }
    }

    @Override
    protected String unavailableAdvice()
    {
        return "check the version of Bouncy Castle, newer algorithms are added with every release";
    }

    private <T> T bc(String jcaName, JcaOperation<T> operation)
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
