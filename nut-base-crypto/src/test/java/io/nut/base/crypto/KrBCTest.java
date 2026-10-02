/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import org.bouncycastle.crypto.engines.AESWrapEngine;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.engines.Salsa20Engine;
import org.bouncycastle.crypto.engines.XSalsa20Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.SkippingStreamCipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The same tests as {@link KrJdkTest}, against the Bouncy Castle provider.
 * <p>
 * Bouncy Castle implements every algorithm of the API, so nothing is skipped
 * here: this is the backend that keeps the whole API working on Java 8.
 *
 * @author franci
 * @see KrBC
 */
public class KrBCTest extends KrTest
{
    @BeforeAll
    public static void checkBouncyCastle()
    {
        assumeTrue(Kr.isBouncyCastleAvailable(),
                "Bouncy Castle is not on the classpath, so these tests cannot run");
    }

    @Override
    protected Kr kr()
    {
        return Kr.getInstance(true);
    }

    @Test
    public void everythingIsAvailable()
    {
        Kr kr = kr();
        assertEquals(Kr.BOUNCY_CASTLE_PROVIDER, kr.name());
        for (Kr.HashAlgorithm algorithm : Kr.HashAlgorithm.values())
        {
            assertTrue(kr.supports(algorithm), algorithm.name());
        }
        for (Kr.HmacAlgorithm algorithm : Kr.HmacAlgorithm.values())
        {
            assertTrue(kr.supports(algorithm), algorithm.name());
        }
        for (Kr.AeadAlgorithm algorithm : Kr.AeadAlgorithm.values())
        {
            assertTrue(kr.supports(algorithm), algorithm.name());
        }
        for (Kr.CipherAlgorithm algorithm : Kr.CipherAlgorithm.values())
        {
            assertTrue(kr.supports(algorithm), algorithm.name());
        }
        for (Kr.KeyWrapAlgorithm algorithm : Kr.KeyWrapAlgorithm.values())
        {
            assertTrue(kr.supports(algorithm), algorithm.name());
        }
        for (Kr.SignatureAlgorithm algorithm : Kr.SignatureAlgorithm.values())
        {
            assertTrue(kr.supports(algorithm), algorithm.name());
        }
        for (Kr.KeyAlgorithm algorithm : Kr.KeyAlgorithm.values())
        {
            assertTrue(kr.supports(algorithm), algorithm.name());
        }
        for (Kr.EcCurve curve : Kr.EcCurve.values())
        {
            assertTrue(kr.supports(curve), curve.name());
        }
    }

    @Test
    public void streamsAgreeWithTheLightweightBouncyCastleEngines()
    {
        Kr kr = kr();
        for (Kr.StreamAlgorithm algorithm : Kr.StreamAlgorithm.values())
        {
            if (algorithm == Kr.StreamAlgorithm.XCHACHA20)
            {
                //XChaCha20 is HChaCha20 followed by ChaCha20, so it is covered by CHACHA20 here
                continue;
            }
            byte[] key = kr.keys.generateSecretKey(algorithm).getEncoded();
            byte[] nonce = new byte[algorithm.nonceBytes()];
            kr.random.fill(nonce);
            //a few blocks, so the block counter really moves
            for (long counter : new long[] { 0, 1, 7 })
            {
                byte[] keystream = kr.streams.keystream(algorithm, key, nonce, counter, 3 * 64);
                assertArrayEquals(engineStream(algorithm, key, nonce, counter, 3 * 64), keystream,
                        algorithm.name() + " at counter " + counter);
            }
        }
    }

    @Test
    public void aesKwAgreesWithTheLightweightBouncyCastleEngine()
    {
        Kr kr = kr();
        for (Kr.KeyWrapAlgorithm algorithm : Kr.KeyWrapAlgorithm.values())
        {
            if (algorithm.usesRsaKey())
            {
                //Bouncy Castle has no RSA key wrap engine, so it is checked against the JCA encryption
                continue;
            }
            byte[] key = kr.random.bytes(algorithm.keyBytes());
            for (int blocks : new int[] { 2, 3, 4, 5 })
            {
                byte[] keyData = kr.random.bytes(blocks * 8);
                AESWrapEngine engine = new AESWrapEngine();
                engine.init(true, new KeyParameter(key));
                byte[] wrapped = engine.wrap(keyData, 0, keyData.length);
                assertEquals(keyData.length + 8, wrapped.length, algorithm.name() + " adds one block");
                assertArrayEquals(wrapped, kr.wraps.wrap(algorithm, new SecretKeySpec(key, "AES"), keyData),
                        algorithm.name() + " with " + keyData.length + " bytes of key data");
                assertArrayEquals(keyData, kr.wraps.unwrap(algorithm, new SecretKeySpec(key, "AES"), wrapped),
                        algorithm.name() + " with " + keyData.length + " bytes of key data");
            }
        }
    }

    @Test
    public void poly1305AgreesWithTheLightweightBouncyCastleMac()
    {
        Kr kr = kr();
        //the lengths around the block size are what the padding is about
        for (int length : new int[] { 0, 1, 15, 16, 17, 31, 32, 33, 64, 97 })
        {
            byte[] key = kr.random.bytes(32);
            byte[] message = kr.random.bytes(length);
            Poly1305 mac = new Poly1305();
            mac.init(new KeyParameter(key));
            mac.update(message, 0, message.length);
            byte[] tag = new byte[mac.getMacSize()];
            mac.doFinal(tag, 0);
            assertArrayEquals(tag, kr.poly1305.poly1305(key, message), length + " bytes of data");
        }
    }

    private static byte[] engineStream(Kr.StreamAlgorithm algorithm, byte[] key, byte[] nonce, long counter, int bytes)
    {
        byte[] output;
        SkippingStreamCipher engine;
        switch (algorithm)
        {
            case CHACHA20:
                engine = new ChaCha7539Engine();
                break;
            case SALSA20:
                engine = new Salsa20Engine();
                break;
            case XSALSA20:
                engine = new XSalsa20Engine();
                break;
            default:
                //XChaCha20 is HChaCha20 followed by ChaCha20, which is checked through CHACHA20
                throw new IllegalArgumentException(algorithm.name() + " is not a lightweight engine");
        }
        engine.init(true, new ParametersWithIV(new KeyParameter(key), nonce));
        //the engines always start at the first block, so the earlier blocks are dropped
        output = new byte[(int) ((counter * 64) + bytes)];
        for (int offset = 0; offset < output.length; offset += 64)
        {
            engine.processBytes(new byte[64], 0, 64, output, offset);
        }
        return Arrays.copyOfRange(output, (int) (counter * 64), output.length);
    }
}