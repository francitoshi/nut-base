/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import io.nut.base.crypto.Kr.AeadAlgorithm;
import io.nut.base.crypto.Kr.CipherAlgorithm;
import io.nut.base.crypto.Kr.HashAlgorithm;
import io.nut.base.crypto.Kr.HmacAlgorithm;
import io.nut.base.crypto.Kr.KeyAlgorithm;
import io.nut.base.crypto.Kr.KeyWrapAlgorithm;
import io.nut.base.crypto.Kr.Pbkdf2Algorithm;
import io.nut.base.crypto.Kr.SignatureAlgorithm;
import io.nut.base.crypto.Kr.StreamAlgorithm;
import io.nut.base.encoding.Hex;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * How to use {@link Kr}, in the order the documentation talks about it. Every
 * test here is a small piece of code that could be copied into an application
 * as it is, so this class is both a tutorial and a regression test of the API
 * being easy to use.
 *
 * @author franci
 */
public class KrHowToTest
{
    private static Kr kr()
    {
        return Kr.getInstance(true);
    }

    @Test
    public void chooseTheBackend()
    {
        //Bouncy Castle when it is there, the JDK otherwise: this is what an
        //application should normally do, to work from Java 8 and to use the
        //algorithms the running JDK does not implement
        Kr kr = Kr.getInstance(true);
        assertNotNull(kr);
        assertNotNull(kr.name());

        //the JDK only, for example when a security policy forbids third party
        //providers
        Kr jdk = Kr.getInstance(false);
        assertEquals(KrJdk.NAME, jdk.name());

        //and what each of them can do here and now
        System.out.println(kr.name() + " " + (Kr.bouncyCastleVersion() == null ? "" : Kr.bouncyCastleVersion() + " ") + supportReport(kr));
    }

    @Test
    public void hashSomething()
    {
        Kr kr = kr();
        //digests, in bytes or in hexadecimal
        byte[] digest = kr.hash.sha256("a file that was downloaded");
        byte[] sameDigest = kr.hash.digest(HashAlgorithm.SHA256, "a file that was downloaded");
        assertArrayEquals(digest, sameDigest);
        assertEquals(64, kr.hash.hex(HashAlgorithm.SHA256, "a file that was downloaded").length());

        //a large file does not need to be in memory: hash it in chunks
        try
        {
            java.io.InputStream stream = new java.io.ByteArrayInputStream("a file that was downloaded".getBytes(StandardCharsets.UTF_8));
            java.security.MessageDigest messageDigest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64];
            for (int read = stream.read(buffer); read > 0; read = stream.read(buffer))
            {
                messageDigest.update(buffer, 0, read);
            }
            assertEquals(kr.hash.hex(HashAlgorithm.SHA256, "a file that was downloaded"),
                    io.nut.base.encoding.Hex.encode(messageDigest.digest()));
        }
        catch (java.io.IOException | java.security.NoSuchAlgorithmException ex)
        {
            fail(ex.getMessage());
        }
    }

    @Test
    public void authenticateARequest()
    {
        Kr kr = kr();
        //a key that both sides share, for example taken from the configuration
        byte[] sharedKey = kr.random.bytes(32);
        String method = "POST";
        String path = "/api/v1/transfer";
        byte[] body = "{\"amount\":10}".getBytes(StandardCharsets.UTF_8);
        String timestamp = "1767225600";

        //the signature covers the method, the path, the timestamp and the body,
        //so none of them can be changed without breaking it
        byte[] signature = kr.hmac.sha256(sharedKey,
                method.getBytes(StandardCharsets.UTF_8),
                path.getBytes(StandardCharsets.UTF_8),
                timestamp.getBytes(StandardCharsets.UTF_8),
                body);

        //the other side recomputes it and compares in constant time
        assertTrue(kr.hmac.verify(signature, HmacAlgorithm.SHA256, sharedKey,
                method.getBytes(StandardCharsets.UTF_8),
                path.getBytes(StandardCharsets.UTF_8),
                timestamp.getBytes(StandardCharsets.UTF_8),
                body));
    }

    @Test
    public void encryptSomething()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(AeadAlgorithm.AES_256_GCM), "AES-GCM is always available");

        //generate a key once, store it with the data that it protects
        SecretKey key = kr.keys.generateSecretKey(AeadAlgorithm.AES_256_GCM);
        assertEquals(32, key.getEncoded().length);

        Kr.Aead aead = kr.aeads.of(AeadAlgorithm.AES_256_GCM, key);
        byte[] secret = "the credit card number".getBytes(StandardCharsets.UTF_8);
        byte[] context = "user:1234".getBytes(StandardCharsets.UTF_8);

        //seal returns the nonce, the ciphertext and the tag together, so there
        //is nothing to keep out of sync
        byte[] sealed = aead.seal(secret, context);
        assertEquals(aead.nonceBytes() + secret.length + aead.tagBytes(), sealed.length);

        //the nonce is the first thing in the envelope and must not be reused
        byte[] nonce = new byte[aead.nonceBytes()];
        System.arraycopy(sealed, 0, nonce, 0, nonce.length);

        //open verifies the tag: a modified ciphertext, a modified context or
        //another key all fail the same way, with an exception
        assertArrayEquals(secret, aead.open(sealed, context));
        sealed[aead.nonceBytes()] ^= 0x01;
        assertThrows(Kr.BadTagException.class, () -> aead.open(sealed, context));
    }

    @Test
    public void encryptSomethingWithAStreamCipher()
    {
        Kr kr = kr();

        //a stream cipher needs a key, a nonce and nothing else: the data is xored
        //with the keystream, and the keystream is a function of key and nonce only
        SecretKey key = kr.keys.generateSecretKey(StreamAlgorithm.CHACHA20);
        byte[] nonce = kr.random.bytes(StreamAlgorithm.CHACHA20.nonceBytes());
        byte[] data = "the credit card number".getBytes(StandardCharsets.UTF_8);

        //a nonce must never be used twice with the same key, so it is stored
        //next to the data: it does not need to be secret
        byte[] encrypted = kr.streams.xor(StreamAlgorithm.CHACHA20, key, nonce, data);
        assertArrayEquals(data, kr.streams.xor(StreamAlgorithm.CHACHA20, key, nonce, encrypted));

        //the keystream is what encrypts, so it is also possible to ask for it
        //directly, and to continue where a previous chunk ended with a counter
        byte[] keystream = kr.streams.keystream(StreamAlgorithm.CHACHA20, key, nonce, data.length);
        byte[] encryptedAgain = data.clone();
        for (int i = 0; i < encryptedAgain.length; i++)
        {
            encryptedAgain[i] ^= keystream[i];
        }
        assertArrayEquals(encrypted, encryptedAgain);

        //the key of another cipher is refused, so streams cannot be mixed up
        assertThrows(IllegalArgumentException.class, () -> kr.streams.xor(StreamAlgorithm.SALSA20,
                kr.keys.generateSecretKey(StreamAlgorithm.CHACHA20),
                kr.random.bytes(StreamAlgorithm.SALSA20.nonceBytes()), data));
    }

    @Test
    public void encryptWithAPassword()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(AeadAlgorithm.AES_256_GCM), "AES-GCM is always available");
        char[] password = "the password of the user".toCharArray();
        byte[] salt = kr.random.bytes(16);

        //PBKDF2 with the default number of iterations and a random salt: the
        //salt and the algorithm must be stored with the ciphertext
        SecretKey key = kr.kdf.pbkdf2Key(AeadAlgorithm.AES_256_GCM, password, salt,
                Pbkdf2Algorithm.HMAC_SHA256.minimumIterations());
        Kr.Aead aead = kr.aeads.aesGcm(key);
        byte[] sealed = aead.seal("a note for the user".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("a note for the user".getBytes(StandardCharsets.UTF_8), aead.open(sealed));

        //the same password and salt always give the same key, the same password
        //with another salt gives another one
        assertArrayEquals(key.getEncoded(), kr.kdf.pbkdf2Key(AeadAlgorithm.AES_256_GCM, password, salt,
                Pbkdf2Algorithm.HMAC_SHA256.minimumIterations()).getEncoded());
        assertFalse(java.util.Arrays.equals(key.getEncoded(),
                kr.kdf.pbkdf2Key(AeadAlgorithm.AES_256_GCM, password, kr.random.bytes(16),
                        Pbkdf2Algorithm.HMAC_SHA256.minimumIterations()).getEncoded()));
    }

    @Test
    public void deriveAKeyBetweenTwoParties()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(AeadAlgorithm.AES_256_GCM) && kr.supports(KeyAlgorithm.X25519),
                "AES-GCM and X25519 are needed");

        //each party has its own key pair, generated once and stored
        KeyPair alice = kr.keys.generateX25519();
        KeyPair bob = kr.keys.generateX25519();

        //the same salt and info on both sides, for example the identifiers of
        //the conversation
        byte[] salt = kr.random.bytes(16);
        byte[] info = "chat between alice and bob".getBytes(StandardCharsets.UTF_8);

        byte[] fromAlice = kr.kdf.ecdh(alice.getPrivate(), bob.getPublic());
        byte[] fromBob = kr.kdf.ecdh(bob.getPrivate(), alice.getPublic());
        assertArrayEquals(fromAlice, fromBob, "the shared secret is the same on both sides");

        //with HKDF on top of it, so that both sides derive the very same key
        SecretKey key = kr.kdf.ecdhHkdfKey(AeadAlgorithm.AES_256_GCM, alice.getPrivate(), bob.getPublic(), salt, info);
        Kr.Aead aead = kr.aeads.aesGcm(key);
        assertArrayEquals("hello bob".getBytes(StandardCharsets.UTF_8), aead.open(aead.seal("hello bob".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void signADocument()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(SignatureAlgorithm.SHA256_WITH_ECDSA), "ECDSA needs a Java version that has it");

        //the key pair of the signer, generated once and stored, the private key
        //never leaves the machine that signs
        KeyPair keyPair = kr.keys.generate(KeyAlgorithm.EC);
        byte[] document = "a document to sign".getBytes(StandardCharsets.UTF_8);

        byte[] signature = kr.sign.sign(SignatureAlgorithm.SHA256_WITH_ECDSA, keyPair.getPrivate(), document);

        //anybody with the public key can check it, for instance to validate a
        //signature made against a public key file
        assertTrue(kr.sign.verify(SignatureAlgorithm.SHA256_WITH_ECDSA, keyPair.getPublic(), signature, document));
        assertFalse(kr.sign.verify(SignatureAlgorithm.SHA256_WITH_ECDSA, keyPair.getPublic(), signature, "another document".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void encryptSomethingWithABlockCipher()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(CipherAlgorithm.AES_256_CBC), "AES-CBC is always available");

        //a block cipher needs a key and an initialization vector, which is not
        //secret but must never be used twice with the same key
        SecretKey key = kr.keys.generateSecretKey(CipherAlgorithm.AES_256_CBC);
        byte[] data = "the credit card number".getBytes(StandardCharsets.UTF_8);

        //encrypt generates a random initialization vector and puts it in front
        //of the ciphertext, so there is nothing to keep out of sync
        Kr.BlockCipher cbc = kr.ciphers.aesCbc(key);
        byte[] encrypted = cbc.encrypt(data);
        assertArrayEquals(data, cbc.decrypt(encrypted));

        //the initialization vector is part of the envelope and can also be
        //given by the caller, which is what a format with its own header needs
        byte[] iv = kr.random.bytes(cbc.ivBytes());
        assertArrayEquals(cbc.decrypt(encrypted), cbc.decrypt(cbc.encryptWithIv(iv, data)));

        //counter mode needs no padding and the ciphertext is the same length as
        //the plaintext, which is what a disk or a stream of blocks wants
        assumeTrue(kr.supports(CipherAlgorithm.AES_256_CTR), "AES-CTR is always available");
        Kr.BlockCipher ctr = kr.ciphers.aesCtr(key);
        assertEquals(data.length + ctr.ivBytes(), ctr.encrypt(data).length);
        assertArrayEquals(data, ctr.decrypt(ctr.encrypt(data)));

        //any of the block ciphers of the API is also reachable by algorithm
        assertEquals(CipherAlgorithm.AES_256_CBC, kr.ciphers.of(CipherAlgorithm.AES_256_CBC, key).algorithm());
    }

    @Test
    public void wrapAKey()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(KeyWrapAlgorithm.AES_256_KW), "AES key wrap is always available");

        //key wrapping encrypts a key with another key, so that it can travel
        //wherever a key of the first kind is allowed
        SecretKey kek = kr.keys.generateSecretKey(CipherAlgorithm.AES_256_CBC);
        byte[] keyToWrap = kr.random.bytes(32);
        byte[] wrapped = kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW, kek, keyToWrap);
        assertArrayEquals(keyToWrap, kr.wraps.unwrap(KeyWrapAlgorithm.AES_256_KW, kek, wrapped));

        //the wrapped key carries its own integrity check, so a tampered one or
        //another key to wrap it with fail the same way, with an exception
        wrapped[wrapped.length - 1] ^= 0x01;
        assertThrows(Kr.CryptoException.class, () -> kr.wraps.unwrap(KeyWrapAlgorithm.AES_256_KW, kek, wrapped));

        //an RSA key pair wraps a key for anybody who has the private key, which
        //is what a certificate or a public key file is good for
        assumeTrue(kr.supports(KeyAlgorithm.RSA), "RSA is always available");
        KeyPair keyPair = kr.keys.generate(KeyAlgorithm.RSA);
        byte[] wrappedForTheOwner = kr.wraps.wrap(KeyWrapAlgorithm.RSA_KW, keyPair.getPublic(), keyToWrap);
        assertArrayEquals(keyToWrap, kr.wraps.unwrap(KeyWrapAlgorithm.RSA_KW, keyPair.getPrivate(), wrappedForTheOwner));
    }

    @Test
    public void authenticateWithPoly1305()
    {
        Kr kr = kr();

        //Poly1305 authenticates with a one time key of 32 bytes that comes
        //from a key agreement or from a key derivation, never from a password
        //and never twice for two messages
        byte[] key = kr.random.bytes(32);
        byte[] message = "the body of the request".getBytes(StandardCharsets.UTF_8);
        byte[] tag = kr.poly1305.poly1305(key, message);

        //the other side recomputes the tag and compares it in constant time
        assertTrue(kr.poly1305.verify(tag, key, message));

        //the tag is 16 bytes and it is sent with the message, not kept secret
        assertEquals(16, tag.length);
        assertEquals(Hex.encode(tag), kr.poly1305.hex(key, message));

        //a modified message, another key or a modified tag all fail the same way
        assertFalse(kr.poly1305.verify(tag, key, "the body of the reques".getBytes(StandardCharsets.UTF_8)));
        assertFalse(kr.poly1305.verify(tag, kr.random.bytes(32), message));
    }

    @Test
    public void moveKeysAround()
    {
        Kr kr = kr();
        KeyPair keyPair = kr.keys.generate(KeyAlgorithm.EC);

        //a key pair travels as its two encodings: X.509 for the public key and
        //PKCS#8 for the private one
        byte[] publicKey = kr.keys.encode(keyPair.getPublic());
        byte[] privateKey = kr.keys.encode(keyPair.getPrivate());
        KeyPair received = kr.keys.decodePair(KeyAlgorithm.EC, privateKey, publicKey);

        byte[] signature = kr.sign.sign(SignatureAlgorithm.SHA256_WITH_ECDSA, received.getPrivate(), "data".getBytes(StandardCharsets.UTF_8));
        assertTrue(kr.sign.verify(SignatureAlgorithm.SHA256_WITH_ECDSA, received.getPublic(), signature, "data".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void askWhatIsAvailable()
    {
        Kr kr = kr();
        //the JDK backend grows with the Java version, so ask before using an
        //algorithm and give the user a message when it is not there
        if (!kr.supports(AeadAlgorithm.CHACHA20_POLY1305))
        {
            try
            {
                kr.aeads.chacha20Poly1305(kr.random.bytes(32));
                fail("it should not be available");
            }
            catch (Kr.UnsupportedAlgorithmException ex)
            {
                assertTrue(ex.getMessage().contains("ChaCha20"), ex.getMessage());
            }
        }
        //and the recommendation is part of the message
        try
        {
            kr.hash.digest(HashAlgorithm.BLAKE2B_512, "data".getBytes(StandardCharsets.UTF_8));
        }
        catch (Kr.UnsupportedAlgorithmException ex)
        {
            assertTrue(ex.getMessage().contains("Bouncy Castle"), ex.getMessage());
        }
    }

    private static String supportReport(Kr kr)
    {
        StringBuilder report = new StringBuilder();
        for (Kr.HashAlgorithm algorithm : Kr.HashAlgorithm.values())
        {
            report.append(kr.supports(algorithm) ? '+' : '-').append(algorithm.name()).append(' ');
        }
        report.append('|');
        for (Kr.AeadAlgorithm algorithm : Kr.AeadAlgorithm.values())
        {
            report.append(kr.supports(algorithm) ? '+' : '-').append(algorithm.name()).append(' ');
        }
        return report.toString().trim();
    }
}