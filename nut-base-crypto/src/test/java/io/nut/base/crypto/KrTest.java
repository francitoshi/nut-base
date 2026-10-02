/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import io.nut.base.crypto.Kr.AeadAlgorithm;
import io.nut.base.crypto.Kr.CryptoException;
import io.nut.base.crypto.Kr.EcCurve;
import io.nut.base.crypto.Kr.HashAlgorithm;
import io.nut.base.crypto.Kr.HkdfAlgorithm;
import io.nut.base.crypto.Kr.HmacAlgorithm;
import io.nut.base.crypto.Kr.KeyAlgorithm;
import io.nut.base.crypto.Kr.KeyWrapAlgorithm;
import io.nut.base.crypto.Kr.Pbkdf2Algorithm;
import io.nut.base.crypto.Kr.SignatureAlgorithm;
import io.nut.base.crypto.Kr.StreamAlgorithm;
import io.nut.base.crypto.Kr.UnsupportedAlgorithmException;
import io.nut.base.encoding.Hex;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The behaviour every {@link Kr} backend must have, no matter if it is backed
 * by the JDK or by Bouncy Castle.
 * <p>
 * The expected values are published test vectors (NIST, RFC 4231, RFC 5869,
 * RFC 8018, RFC 8439, RFC 8032 and RFC 7748), not values produced by this
 * library, so both backends are checked against something external.
 * <p>
 * A test whose algorithm is not available in the running Java version is
 * skipped, never silently ignored: a Java 8 runtime has no SHA-3, no
 * BLAKE2, no ChaCha20-Poly1305, no Ed25519 and no X25519.
 *
 * @author franci
 */
public abstract class KrTest
{
    private static final byte[] HELLO = "hello".getBytes(StandardCharsets.UTF_8);

    /**
     * The backend under test.
     *
     * @return the backend, never null
     */
    protected abstract Kr kr();

    ////////////////////////////////////////////////////////////////////////////
    ///// hash ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void digestKnownAnswers()
    {
        Kr kr = kr();
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                kr.hash.hex(HashAlgorithm.SHA256, "abc"));
        assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
                + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
                kr.hash.hex(HashAlgorithm.SHA512, "abc"));
        assertEquals("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
                kr.hash.hex(HashAlgorithm.SHA256, "The quick brown fox jumps over the lazy dog"));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                kr.hash.hex(HashAlgorithm.SHA256, ""));
        assumeSupports(kr, HashAlgorithm.SHA3_256);
        assertEquals("3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532",
                kr.hash.hex(HashAlgorithm.SHA3_256, "abc"));
        assumeSupports(kr, HashAlgorithm.SHA3_512);
        assertEquals("b751850b1a57168a5693cd924b6b096e08f621827444f70d884f5d0240d2712e"
                + "10e116e9192af3c91a7ec57647e3934057340b4cf408d5a56592f8274eec53f0",
                kr.hash.hex(HashAlgorithm.SHA3_512, "abc"));
        assumeSupports(kr, HashAlgorithm.BLAKE2B_512);
        assertEquals("ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d1"
                + "7d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923",
                kr.hash.hex(HashAlgorithm.BLAKE2B_512, "abc"));
    }

    @Test
    public void digestIsTheSameInChunksAndInOneGo()
    {
        Kr kr = kr();
        for (HashAlgorithm algorithm : HashAlgorithm.values())
        {
            if (!kr.supports(algorithm))
            {
                continue;
            }
            byte[] all = "the concatenation of these three chunks".getBytes(StandardCharsets.UTF_8);
            byte[] digest = kr.hash.digest(algorithm, all);
            assertEquals(algorithm.digestBytes(), digest.length, algorithm.name());
            assertArrayEquals(digest, kr.hash.digest(algorithm,
                    "the concatenation ".getBytes(StandardCharsets.UTF_8),
                    "of these three ".getBytes(StandardCharsets.UTF_8),
                    "chunks".getBytes(StandardCharsets.UTF_8)), algorithm.name());
            assertArrayEquals(digest, kr.hash.digest(algorithm, all, 0, all.length), algorithm.name());
            assertEquals(Hex.encode(digest), kr.hash.hex(algorithm, all), algorithm.name());
        }
    }

    @Test
    public void digestDoesNotTouchTheData()
    {
        Kr kr = kr();
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        byte[] copy = data.clone();
        kr.hash.digest(HashAlgorithm.SHA256, data);
        kr.hash.digest(HashAlgorithm.SHA256, data, 0, data.length);
        assertArrayEquals(copy, data);
        assertArrayEquals(kr.hash.digest(HashAlgorithm.SHA256, "bc"),
                kr.hash.digest(HashAlgorithm.SHA256, data, 1, data.length - 1), "the offset and length select the data");
    }

    @Test
    public void digestRejectsNulls()
    {
        Kr kr = kr();
        assertThrows(NullPointerException.class, () -> kr.hash.digest(HashAlgorithm.SHA256, (byte[]) null));
        assertThrows(NullPointerException.class, () -> kr.hash.digest(null, HELLO));
        assertThrows(NullPointerException.class, () -> kr.hash.digest(HashAlgorithm.SHA256, HELLO, null));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// hmac ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void hmacRfc4231()
    {
        Kr kr = kr();
        assumeSupports(kr, HmacAlgorithm.SHA256);
        //test case 1
        assertEquals("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
                kr.hmac.hex(HmacAlgorithm.SHA256, hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"), ascii("Hi There")));
        //test case 2
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                kr.hmac.hex(HmacAlgorithm.SHA256, ascii("Jefe"), ascii("what do ya want for nothing?")));
        //test case 6, a key longer than the block size
        assertEquals("60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
                kr.hmac.hex(HmacAlgorithm.SHA256,
                        filled(0xaa, 131),
                        ascii("Test Using Larger Than Block-Size Key - Hash Key First")));

        assumeSupports(kr, HmacAlgorithm.SHA384);
        assertEquals("afd03944d84895626b0825f4ab46907f15f9dadbe4101ec682aa034c7cebc59cfaea9ea9076ede7f4af152e8b2fa9cb6",
                kr.hmac.hex(HmacAlgorithm.SHA384, hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"), ascii("Hi There")));

        assumeSupports(kr, HmacAlgorithm.SHA512);
        assertEquals("87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cdedaa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854",
                kr.hmac.hex(HmacAlgorithm.SHA512, hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"), ascii("Hi There")));
    }

    @Test
    public void hmacVerify()
    {
        Kr kr = kr();
        assumeSupports(kr, HmacAlgorithm.SHA256);
        byte[] key = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
        byte[] tag = kr.hmac.hmac(HmacAlgorithm.SHA256, key, ascii("Hi "), ascii("There"));
        assertTrue(kr.hmac.verify(tag, HmacAlgorithm.SHA256, key, ascii("Hi "), ascii("There")));
        assertTrue(kr.hmac.verify(kr.hmac.sha256(key, ascii("Hi "), ascii("There")), HmacAlgorithm.SHA256, key, ascii("Hi There")),
                "the chunks are concatenated before hashing");
        assertFalse(kr.hmac.verify(tag, HmacAlgorithm.SHA256, key, ascii("Hi there")), "one letter less");
        assertFalse(kr.hmac.verify(tag, HmacAlgorithm.SHA256, key, ascii("Hi "), ascii("there")), "a single letter changes everything");
        assertFalse(kr.hmac.verify(tag, HmacAlgorithm.SHA256, kr.random.bytes(20), ascii("Hi "), ascii("There")), "different key");
        assertFalse(kr.hmac.verify(tag, HmacAlgorithm.SHA384, key, ascii("Hi "), ascii("There")), "different algorithm");

        byte[] tampered = tag.clone();
        tampered[0] ^= 0x01;
        assertFalse(kr.hmac.verify(tampered, HmacAlgorithm.SHA256, key, ascii("Hi "), ascii("There")), "a tampered tag");
        assertFalse(kr.hmac.verify(new byte[32], HmacAlgorithm.SHA256, key, ascii("Hi "), ascii("There")), "a zeroed tag");
    }

    @Test
    public void hmacKeyIsNotModified()
    {
        Kr kr = kr();
        assumeSupports(kr, HmacAlgorithm.SHA256);
        byte[] key = kr.random.bytes(32);
        byte[] copy = key.clone();
        kr.hmac.hmac(HmacAlgorithm.SHA256, key, ascii("data"));
        assertArrayEquals(copy, key);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// aead ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void aesGcmNistKnownAnswer()
    {
        Kr kr = kr();
        assumeSupports(kr, AeadAlgorithm.AES_256_GCM);
        //NIST GCM test case 16
        Kr.Aead aead = kr.aeads.aesGcm(hex("feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308"));
        byte[] nonce = hex("cafebabefacedbaddecaf888");
        byte[] aad = hex("feedfacedeadbeeffeedfacedeadbeefabaddad2");
        byte[] plaintext = hex("d9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72"
                + "1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39");
        byte[] ciphertextAndTag = hex("522dc1f099567d07f47f37a32a84427d643a8cdcbfe5c0c97598a2bd2555d1aa"
                + "8cb08e48590dbb3da7b08b1056828838c5f61e6393ba7a0abcc9f66276fc6ece0f4e1768cddf8853bb2d551b");
        assertArrayEquals(plaintext, aead.openWithNonce(nonce, ciphertextAndTag, aad), "NIST test case 16, an empty aad is not this one");

        //and the same, sealed by the library: byte for byte the same ciphertext
        assertArrayEquals(ciphertextAndTag, aead.sealWithNonce(nonce, plaintext, aad));
    }

    @Test
    public void chacha20Poly1305Rfc8439KnownAnswer()
    {
        Kr kr = kr();
        assumeSupports(kr, AeadAlgorithm.CHACHA20_POLY1305);
        Kr.Aead aead = kr.aeads.chacha20Poly1305(hex("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f"));
        byte[] nonce = hex("070000004041424344454647");
        byte[] aad = hex("50515253c0c1c2c3c4c5c6c7");
        byte[] plaintext = ascii("Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it.");
        byte[] expected = hex("d31a8d34648e60db7b86afbc53ef7ec2a4aded51296e08fea9e2b5a736ee62d6"
                + "3dbea45e8ca9671282fafb69da92728b1a71de0a9e060b2905d6a5b67ecd3b36"
                + "92ddbd7f2d778b8c9803aee328091b58fab324e4fad675945585808b4831d7b"
                + "c3ff4def08e4b7a9de576d26586cec64b61161ae10b594f09e26a7e902ecbd0600691");
        assertArrayEquals(expected, aead.sealWithNonce(nonce, plaintext, aad));
        assertArrayEquals(plaintext, aead.openWithNonce(nonce, expected, aad));
    }

    @Test
    public void xchacha20Poly1305DraftKnownAnswer()
    {
        Kr kr = kr();
        assumeSupports(kr, AeadAlgorithm.XCHACHA20_POLY1305);
        Kr.Aead aead = kr.aeads.xchacha20Poly1305(hex("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f"));
        byte[] nonce = hex("404142434445464748494a4b4c4d4e4f5051525354555657");
        byte[] aad = hex("50515253c0c1c2c3c4c5c6c7");
        byte[] plaintext = ascii("Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it.");
        byte[] expected = hex("bd6d179d3e83d43b9576579493c0e939572a1700252bfaccbed2902c21396cbb"
                + "731c7f1b0b4aa6440bf3a82f4eda7e39ae64c6708c54c216cb96b72e1213b452"
                + "2f8c9ba40db5d945b11b69b982c1bb9e3f3fac2bc369488f76b2383565d3fff921f9664c9"
                + "7637da9768812f615c68b13b52ec0875924c1c7987947deafd8780acf49");
        assertArrayEquals(expected, aead.sealWithNonce(nonce, plaintext, aad), "the sealed data is the nonce, the ciphertext and the tag");
        assertArrayEquals(plaintext, aead.openWithNonce(nonce, expected, aad));
        assertEquals(24, aead.nonceBytes(), "XChaCha20 takes a nonce of 24 bytes");
        assertThrows(Kr.BadTagException.class, () -> aead.openWithNonce(new byte[24], expected, aad),
                "another nonce must not open it");
        assertThrows(IllegalArgumentException.class, () -> aead.sealWithNonce(new byte[12], plaintext, aad),
                "12 bytes is the nonce of ChaCha20-Poly1305, not of XChaCha20-Poly1305");
    }

    @Test
    public void aeadRoundTrip()
    {
        Kr kr = kr();
        for (AeadAlgorithm algorithm : AeadAlgorithm.values())
        {
            if (!kr.supports(algorithm))
            {
                continue;
            }
            Kr.Aead aead = kr.aeads.of(algorithm, kr.keys.generateSecretKey(algorithm));
            assertEquals(algorithm, aead.algorithm());
            byte[] plaintext = ("the message that will be encrypted with " + algorithm).getBytes(StandardCharsets.UTF_8);
            byte[] aad = ascii("this is not encrypted but it is authenticated");
            byte[] sealed = aead.seal(plaintext, aad);
            assertEquals(algorithm.nonceBytes() + plaintext.length + algorithm.tagBytes(), sealed.length, algorithm.name());
            assertArrayEquals(plaintext, aead.open(sealed, aad), algorithm.name());
        }
    }

    @Test
    public void aeadUsesARandomNonceEveryTime()
    {
        Kr kr = kr();
        assumeSupports(kr, AeadAlgorithm.AES_256_GCM);
        Kr.Aead aead = kr.aeads.of(AeadAlgorithm.AES_256_GCM, kr.keys.generateSecretKey(AeadAlgorithm.AES_256_GCM));
        byte[] plaintext = ascii("the same message");
        byte[] first = aead.seal(plaintext);
        byte[] second = aead.seal(plaintext);
        assertFalse(Arrays.equals(first, second), "sealing twice must not give the same bytes");
        assertFalse(Arrays.equals(Arrays.copyOf(first, 12), Arrays.copyOf(second, 12)), "the nonce must not be reused");
        assertArrayEquals(plaintext, aead.open(first));
        assertArrayEquals(plaintext, aead.open(second));
    }

    @Test
    public void aeadDetectsTampering()
    {
        Kr kr = kr();
        assumeSupports(kr, AeadAlgorithm.AES_256_GCM);
        Kr.Aead aead = kr.aeads.of(AeadAlgorithm.AES_256_GCM, kr.keys.generateSecretKey(AeadAlgorithm.AES_256_GCM));
        byte[] plaintext = ascii("a message long enough to be tampered with");
        byte[] aad = ascii("a header");
        byte[] sealed = aead.seal(plaintext, aad);

        for (int position : new int[]{0, AeadAlgorithm.NONCE_BYTES, sealed.length - 1})
        {
            byte[] tampered = sealed.clone();
            tampered[position] ^= 0x01;
            assertThrows(Kr.BadTagException.class, () -> aead.open(tampered, aad), "tampering the byte " + position + " must be detected");
        }
        assertThrows(Kr.BadTagException.class, () -> aead.open(sealed, ascii("another header")), "the aad is authenticated");
        assertThrows(Kr.BadTagException.class, () -> aead.open(sealed), "the aad was used to seal");
        assertThrows(Kr.BadTagException.class, () -> kr.aeads.of(AeadAlgorithm.AES_256_GCM,
                        kr.keys.generateSecretKey(AeadAlgorithm.AES_256_GCM)).open(sealed), "another key must not open it");
    }

    @Test
    public void aeadRejectsMalformedEnvelopes()
    {
        Kr kr = kr();
        assumeSupports(kr, AeadAlgorithm.AES_256_GCM);
        Kr.Aead aead = kr.aeads.aesGcm(kr.random.bytes(16));
        assertThrows(CryptoException.class, () -> aead.open(new byte[AeadAlgorithm.NONCE_BYTES + AeadAlgorithm.TAG_BYTES - 1]));
        assertThrows(IllegalArgumentException.class, () -> aead.sealWithNonce(new byte[11], ascii("data")));
        assertThrows(IllegalArgumentException.class, () -> aead.openWithNonce(new byte[13], new byte[16]));
        assertThrows(NullPointerException.class, () -> aead.seal(null));
        assertThrows(NullPointerException.class, () -> aead.open(null));
    }

    @Test
    public void aeadRejectsKeysOfTheWrongLength()
    {
        Kr kr = kr();
        assertThrows(IllegalArgumentException.class, () -> kr.aeads.aesGcm(new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> kr.aeads.chacha20Poly1305(new byte[31]));
        assertThrows(IllegalArgumentException.class, () -> kr.keys.secretKey(AeadAlgorithm.AES_256_GCM, new byte[17]));
        assertThrows(NullPointerException.class, () -> kr.keys.secretKey(AeadAlgorithm.AES_128_GCM, null));
    }

    @Test
    public void aeadOfChecksTheKeyAlgorithm()
    {
        Kr kr = kr();
        SecretKey aes = kr.keys.secretKey(AeadAlgorithm.AES_256_GCM, kr.random.bytes(32));
        assertThrows(IllegalArgumentException.class, () -> kr.aeads.chacha20Poly1305(aes), "an AES key is not a ChaCha20 key");
        assertThrows(NullPointerException.class, () -> kr.aeads.of(AeadAlgorithm.AES_256_GCM, null));
        assertThrows(NullPointerException.class, () -> kr.aeads.of(null, aes));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// streams /////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void chacha20Rfc8439KnownAnswer()
    {
        Kr kr = kr();
        byte[] key = hex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        byte[] nonce = hex("000000000000004a00000000");
        //RFC 8439 section 2.4.2, the Sunscreen example, whose block counter starts at 1
        byte[] plaintext = ascii("Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it.");
        byte[] expected = hex("6e2e359a2568f98041ba0728dd0d6981e97e7aec1d4360c20a27afccfd9fae0bf91b65c5"
                + "524733ab8f593dabcd62b3571639d624e65152ab8f530c359f0861d807ca0dbf500d6a61"
                + "56a38e088a22b65e52bc514d16ccf806818ce91ab77937365af90bbf74a35be6b40b8eed"
                + "f2785e42874d");
        assertArrayEquals(expected, kr.streams.xor(StreamAlgorithm.CHACHA20, key, nonce, 1, plaintext),
                "the ciphertext of RFC 8439 section 2.4.2");

        //and the same, asking for the keystream that makes it
        byte[] keystream = kr.streams.keystream(StreamAlgorithm.CHACHA20, key, nonce, 1, plaintext.length);
        byte[] encrypted = plaintext.clone();
        for (int i = 0; i < encrypted.length; i++)
        {
            encrypted[i] ^= keystream[i];
        }
        assertArrayEquals(expected, encrypted, "the keystream xor the plaintext is the ciphertext");

        //the block counter is the one of RFC 8439, so the keystream of the second block
        //continues where the first one ended
        assertArrayEquals(Arrays.copyOfRange(keystream, 64, keystream.length),
                kr.streams.keystream(StreamAlgorithm.CHACHA20, key, nonce, 2, keystream.length - 64),
                "the block counter moves 64 bytes ahead");
    }

    @Test
    public void xchacha20DraftKnownAnswer()
    {
        Kr kr = kr();
        byte[] key = hex("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f");
        byte[] nonce = hex("404142434445464748494a4b4c4d4e4f5051525354555658");
        //draft-irtf-cfrg-xchacha-03 section A.3.2.1, the dhole paragraph with the block counter at 0
        byte[] plaintext = hex("5468652064686f6c65202870726f6e6f756e6365642022646f6c65222920697320616c73"
                + "6f206b6e6f776e2061732074686520417369617469632077696c6420646f672c20726564"
                + "20646f672c20616e642077686973746c696e6720646f672e2049742069732061626f7574"
                + "207468652073697a65206f662061204765726d616e20736865706865726420627574206c"
                + "6f6f6b73206d6f7265206c696b652061206c6f6e672d6c656767656420666f782e205468"
                + "697320686967686c7920656c757369766520616e6420736b696c6c6564206a756d706572"
                + "20697320636c6173736966696564207769746820776f6c7665732c20636f796f7465732c"
                + "206a61636b616c732c20616e6420666f78657320696e20746865207461786f6e6f6d6963"
                + "2066616d696c792043616e696461652e");
                assertArrayEquals(hex("1131ce9a2a20ae0d67c8935c7789fa1025c9e5bb720fb96f11354fb97af0bd9aadec0863"
                + "ba60cac8582c48f86cdfc48edd46a48642c5de62ccf11c7b21bf337d29624b4b1b140ace"
                + "53740e405b2168540fd7d630c1f536fecd722fc3cddba7f4cca98cf9e47e5e64d115450f"
                + "9b125b54449ff76141ca620a1f9cfcab2a1a8a255e766a5266b878846120ea64ad99aa47"
                + "9471e63befcbd37cd1c22a221fe462215cf32c74895bf505863ccddd48f62916dc6521f1"
                + "ec50a5ae08903aa259d9bf607cd8026fba548604f1b6072d91bc91243a5b845f7fd171b0"
                + "2edc5a0a84cf28dd241146bc376e3f48df5e7fee1d11048c190a3d3deb0feb64b42d9c6f"
                + "deee290fa0e6ae2c26c0249ea8c181f7e2ffd100cbe5fd3c4f8271d62b15330cb8fdcf00"
                + "b3df507ca8c924f7017b7e712d15a2eb"), kr.streams.keystream(StreamAlgorithm.XCHACHA20, key, nonce, plaintext.length),
                "the keystream of the XChaCha20 section A.3.2.1");
                assertArrayEquals(hex("4559abba4e48c16102e8bb2c05e6947f50a786de162f9b0b7e592a9b53d0d4e98d8d6410"
                + "d540a1a6375b26d80dace4fab52384c731acbf16a5923c0c48d3575d4d0d2c673b666faa"
                + "731061277701093a6bf7a158a8864292a41c48e3a9b4c0daece0f8d98d0d7e05b37a307b"
                + "bb66333164ec9e1b24ea0d6c3ffddcec4f68e7443056193a03c810e11344ca06d8ed8a2b"
                + "fb1e8d48cfa6bc0eb4e2464b748142407c9f431aee769960e15ba8b96890466ef2457599"
                + "852385c661f752ce20f9da0c09ab6b19df74e76a95967446f8d0fd415e7bee2a12a114c2"
                + "0eb5292ae7a349ae577820d5520a1f3fb62a17ce6a7e68fa7c79111d8860920bc048ef43"
                + "fe84486ccb87c25f0ae045f0cce1e7989a9aa220a28bdd4827e751a24a6d5c62d790a663"
                + "93b93111c1a55dd7421a10184974c7c5"), kr.streams.xor(StreamAlgorithm.XCHACHA20, key, nonce, plaintext),
                "and the ciphertext that comes out of it");
    }

    @Test
    public void salsa20EstreamKnownAnswer()
    {
        Kr kr = kr();
        byte[] key = hex("0053A6F94C9FF24598EB3E91E4378ADD3083D6297CCF2275C81B6EC11467BA0D");
        byte[] nonce = hex("0D74DB42A91077DE");
        //the eSTREAM test vectors of set 6, which is the one with a 256 bits key
        assertArrayEquals(hex("F5FAD53F79F9DF58C4AEA0D0ED9A9601F278112CA7180D565B420A48019670EAF24CE493A86263F677B46ACE1924773D2BB25571E1AA8593758FC382B1280B71"), kr.streams.keystream(StreamAlgorithm.SALSA20, key, nonce, 64),
                "the keystream of the first block");
        assertArrayEquals(hex("81582C65D7562B80AEC2F1A673A9D01C9F892A23D4919F6AB47B9154E08E699B4117D7C666477B60F8391481682F5D95D96623DBC489D88DAA6956B9F0646B6E"), kr.streams.keystream(StreamAlgorithm.SALSA20, key, nonce, 1024, 64),
                "and the one of the block 1024, which is where the block counter is");
    }

    @Test
    public void xsalsa20NaClKnownAnswer()
    {
        Kr kr = kr();
        //a vector of the NaCl test suite, 238 bytes of plaintext
        assertArrayEquals(hex("b2af688e7d8fc4b508c05cc39dd583d6714322c64d7f3e63147aede2d9534934b04ff6f337b031815cd094bdbc6d7a92077dce709412286822ef0737ee47f6b7ffa22f9d53f11dd2b0a3bb9fc01d9a88f9d53c26e9365c2c3c063bc4840bfc812e4b80463e69d179530b25c158f543191cff993106511aa036043bbc75866ab7e34afc57e2cce4934a5faae6eabe4f221770183dd060467827c27a354159a081275a291f69d946d6fe28ed0b9ce08206cf484925a51b9498dbde178ddd3ae91a8581b91682d860f840782f6eea49dbb9bd721501d2c67122dea3b7283848c5f13e0c0de876bd227a856e4de593a3"),
                kr.streams.xor(StreamAlgorithm.XSALSA20, hex("a6a7251c1e72916d11c2cb214d3c252539121d8e234e652d651fa4c8cff88030"), hex("9e645a74e9e0a60d8243acd9177ab51a1beb8d5a2f5d700c"), hex("093c5e5585579625337bd3ab619d615760d8c5b224a85b1d0efe0eb8a7ee163abb0376529fcc09bab506c618e13ce777d82c3ae9d1a6f972d4160287cbfe60bf2130fc0a6ff6049d0a5c8a82f429231f008082e845d7e189d37f9ed2b464e6b919e6523a8c1210bd52a02a4c3fe406d3085f5068d1909eeeca6369abc981a42e87fe665583f0ab85ae71f6f84f528e6b397af86f6917d9754b7320dbdc2fea81496f2732f532ac78c4e9c6cfb18f8e9bdf74622eb126141416776971a84f94d156beaf67aecbf2ad412e76e66e8fad7633f5b6d7f3d64b5c6c69ce29003c6024465ae3b89be78e915d88b4b5621d")),
                "NaCl vector 0 of 17");
        //a vector of the NaCl test suite, 3 bytes of plaintext
        assertArrayEquals(hex("8fd7df"),
                kr.streams.xor(StreamAlgorithm.XSALSA20, hex("6799d76e5ffb5b4920bc2768bafd3f8c16554e65efcf9a16f4683a7a06927c11"), hex("61ab951921e54ff06d9b77f313a4e49df7a057d5fd627989"), hex("472766")),
                "NaCl vector 6 of 17");
        //a vector of the NaCl test suite, 25 bytes of plaintext
        assertArrayEquals(hex("16c4006c28365190411eb1593814cf15e74c22238f210afc3d"),
                kr.streams.xor(StreamAlgorithm.XSALSA20, hex("3d02bff3375d403027356b94f514203737ee9a85d2052db3e4e5a217c259d18a"), hex("74216c95031895f48c1dba651555ebfa3ca326a755237025"), hex("0d4b0f54fd09ae39baa5fa4baccf2e6682e61b257e01f42b8f")),
                "NaCl vector 12 of 17");
        //a vector of the NaCl test suite, 68 bytes of plaintext
        assertArrayEquals(hex("02fe84ce81e178e7aabdd3ba925a766c3c24756eefae33942af75e8b464556b5997e616f3f2dfc7fce91848afd79912d9fb55201b5813a5a074d2c0d4292c1fd441807c5"),
                kr.streams.xor(StreamAlgorithm.XSALSA20, hex("ad1a5c47688874e6663a0f3fa16fa7efb7ecadc175c468e5432914bdb480ffc6"), hex("e489eed440f1aae1fac8fb7a9825635454f8f8f1f52e2fcc"), hex("aa6c1e53580f03a9abb73bfdadedfecada4c6b0ebe020ef10db745e54ba861caf65f0e40dfc520203bb54d29e0a8f78f16b3f1aa525d6bfa33c54726e59988cfbec78056")),
                "NaCl vector 13 of 17");
    }

    @Test
    public void streamsAreSymmetric()
    {
        Kr kr = kr();
        for (StreamAlgorithm algorithm : StreamAlgorithm.values())
        {
            byte[] key = kr.random.bytes(32);
            byte[] nonce = kr.random.bytes(algorithm.nonceBytes());
            for (int length : new int[]{0, 1, 63, 64, 65, 127, 128, 129, 1000})
            {
                byte[] plaintext = kr.random.bytes(length);
                byte[] encrypted = kr.streams.xor(algorithm, key, nonce, plaintext);
                assertEquals(length, encrypted.length, algorithm.name() + " keeps the length");
                assertArrayEquals(plaintext, kr.streams.xor(algorithm, key, nonce, encrypted),
                        algorithm.name() + " is its own inverse");
            }
        }
    }

    @Test
    public void theKeystreamIsTheCipherOfZeroes()
    {
        Kr kr = kr();
        for (StreamAlgorithm algorithm : StreamAlgorithm.values())
        {
            byte[] key = kr.random.bytes(32);
            byte[] nonce = kr.random.bytes(algorithm.nonceBytes());
            byte[] zeroes = new byte[200];
            assertArrayEquals(kr.streams.keystream(algorithm, key, nonce, zeroes.length),
                    kr.streams.xor(algorithm, key, nonce, zeroes), algorithm.name());
            assertEquals(200, kr.streams.keystream(algorithm, key, nonce, 200).length, algorithm.name());
            assertEquals(0, kr.streams.keystream(algorithm, key, nonce, 0).length, algorithm.name());
        }
    }

    @Test
    public void streamsOnlyNeedA32BytesKey()
    {
        Kr kr = kr();
        for (StreamAlgorithm algorithm : StreamAlgorithm.values())
        {
            assertEquals(256, algorithm.keyBits(), algorithm.name());
            assertEquals(32, algorithm.keyBytes(), algorithm.name());
            byte[] key = kr.random.bytes(32);
            byte[] nonce = kr.random.bytes(algorithm.nonceBytes());
            assertArrayEquals(kr.streams.xor(algorithm, key, nonce, ascii("data")),
                    kr.streams.xor(algorithm, kr.keys.secretKey(algorithm, key), nonce, ascii("data")),
                    algorithm.name() + " takes the same key as a byte array or as a SecretKey");
            assertFalse(Arrays.equals(kr.streams.keystream(algorithm, key, nonce, 64),
                            kr.streams.keystream(algorithm, kr.random.bytes(32), nonce, 64)),
                    algorithm.name() + " must not repeat the keystream of two keys");
        }
    }

    @Test
    public void streamsRejectMalformedArguments()
    {
        Kr kr = kr();
        for (StreamAlgorithm algorithm : StreamAlgorithm.values())
        {
            byte[] key = kr.random.bytes(32);
            byte[] nonce = kr.random.bytes(algorithm.nonceBytes());
            assertThrows(IllegalArgumentException.class, () -> kr.streams.xor(algorithm, new byte[31], nonce, ascii("data")),
                    algorithm.name() + " needs a 32 bytes key");
            assertThrows(IllegalArgumentException.class, () -> kr.streams.xor(algorithm, key, new byte[algorithm.nonceBytes() - 1], ascii("data")),
                    algorithm.name() + " needs a nonce of " + algorithm.nonceBytes() + " bytes");
            assertThrows(IllegalArgumentException.class, () -> kr.streams.xor(algorithm, key, new byte[algorithm.nonceBytes() + 1], ascii("data")),
                    algorithm.name() + " does not take a longer nonce");
            assertThrows(IllegalArgumentException.class, () -> kr.streams.xor(algorithm, key, nonce, -1, ascii("data")),
                    algorithm.name() + " does not take a negative block counter");
            assertThrows(IllegalArgumentException.class, () -> kr.streams.keystream(algorithm, key, nonce, -1),
                    algorithm.name() + " does not ask for a negative number of bytes");
            assertThrows(NullPointerException.class, () -> kr.streams.xor(algorithm, (byte[]) null, nonce, ascii("data")));
            assertThrows(NullPointerException.class, () -> kr.streams.xor(algorithm, key, null, ascii("data")));
            assertThrows(NullPointerException.class, () -> kr.streams.xor(algorithm, key, nonce, (byte[]) null));
            assertThrows(NullPointerException.class, () -> kr.streams.xor(algorithm, (SecretKey) null, nonce, ascii("data")));
            assertThrows(NullPointerException.class, () -> kr.streams.keystream(algorithm, (SecretKey) null, nonce, 8),
                    algorithm.name() + " needs a key");
        }
        //the block counter of ChaCha20 has 32 bits, so the keystream cannot go past it
        assertThrows(CryptoException.class, () -> kr.streams.keystream(StreamAlgorithm.CHACHA20, kr.random.bytes(32),
                kr.random.bytes(12), 1L << 32, 64), "ChaCha20 would reuse its first block");
        assertThrows(CryptoException.class, () -> kr.streams.keystream(StreamAlgorithm.XCHACHA20, kr.random.bytes(32),
                kr.random.bytes(24), (1L << 32) - 1, 128), "and neither XChaCha20");
        assertEquals(64, kr.streams.keystream(StreamAlgorithm.CHACHA20, kr.random.bytes(32), kr.random.bytes(12),
                (1L << 32) - 1, 64).length, "the last block of the keystream is a valid one");
        //Salsa20 counts in 64 bits, so its counter goes much further
        assertEquals(64, kr.streams.keystream(StreamAlgorithm.SALSA20, kr.random.bytes(32), kr.random.bytes(8),
                1L << 40, 64).length);
        assertEquals(64, kr.streams.keystream(StreamAlgorithm.XSALSA20, kr.random.bytes(32), kr.random.bytes(24),
                1L << 40, 64).length);
    }

    @Test
    public void streamKeysAreChecked()
    {
        Kr kr = kr();
        for (StreamAlgorithm algorithm : StreamAlgorithm.values())
        {
            SecretKey key = kr.keys.generateSecretKey(algorithm);
            assertEquals(algorithm.keyBytes(), key.getEncoded().length, algorithm.name());
            assertEquals(algorithm.secretKeyAlgorithm().jcaName(), key.getAlgorithm(), algorithm.name());
            assertThrows(IllegalArgumentException.class, () -> kr.keys.secretKey(algorithm, new byte[31]), algorithm.name());
            assertThrows(NullPointerException.class, () -> kr.keys.secretKey(algorithm, (byte[]) null), algorithm.name());
            assertThrows(NullPointerException.class, () -> kr.keys.generateSecretKey((StreamAlgorithm) null));
        }
        //a key of another algorithm is not a valid key, even when its bytes are the right ones
        byte[] chachaKey = kr.keys.generateSecretKey(StreamAlgorithm.CHACHA20).getEncoded();
        assertThrows(IllegalArgumentException.class, () -> kr.streams.xor(StreamAlgorithm.SALSA20,
                        new SecretKeySpec(chachaKey, StreamAlgorithm.CHACHA20.secretKeyAlgorithm().jcaName()),
                        kr.random.bytes(8), ascii("data")),
                "a ChaCha20 key is not a Salsa20 key");
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// block ciphers ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void aesCtrNistSp80038aKnownAnswer()
    {
        Kr kr = kr();
        //the four blocks of plaintext and initialization counter of NIST SP 800-38A section F.5
        byte[] plaintext = hex("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e51"
                + "30c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710");
        byte[] iv = hex("f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff");
        //F.5.1, CTR-AES128.Encrypt
        assertArrayEquals(hex("874d6191b620e3261bef6864990db6ce9806f66b7970fdff8617187bb9fffdff"
                        + "5ae4df3edbd5d35e5b4f09020db03eab1e031dda2fbe03d1792170a0f3009cee"),
                ctr(Kr.CipherAlgorithm.AES_128_CTR, hex("2b7e151628aed2a6abf7158809cf4f3c"), iv, plaintext),
                "CTR-AES128.Encrypt");
        //F.5.2, CTR-AES192.Encrypt
        assertArrayEquals(hex("1abc932417521ca24f2b0459fe7e6e0b090339ec0aa6faefd5ccc2c6f4ce8e94"
                        + "1e36b26bd1ebc670d1bd1d665620abf74f78a7f6d29809585a97daec58c6b050"),
                ctr(Kr.CipherAlgorithm.AES_192_CTR, hex("8e73b0f7da0e6452c810f32b809079e562f8ead2522c6b7b"), iv, plaintext),
                "CTR-AES192.Encrypt");
        //F.5.3, CTR-AES256.Encrypt
        assertArrayEquals(hex("601ec313775789a5b7a7f504bbf3d228f443e3ca4d62b59aca84e990cacaf5c5"
                        + "2b0930daa23de94ce87017ba2d84988ddfc9c58db67aada613c2dd08457941a6"),
                ctr(Kr.CipherAlgorithm.AES_256_CTR, hex("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4"), iv, plaintext),
                "CTR-AES256.Encrypt");
    }

    @Test
    public void aesCbcNistSp80038aKnownAnswer()
    {
        Kr kr = kr();
        //the four blocks of plaintext and the initialization vector of NIST SP 800-38A section F.2
        byte[] plaintext = hex("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e51"
                + "30c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710");
        byte[] iv = hex("000102030405060708090a0b0c0d0e0f");
        //F.2.1, CBC-AES128.Encrypt
        assertArrayEquals(hex("7649abac8119b246cee98e9b12e9197d5086cb9b507219ee95db113a917678b2"
                        + "73bed6b8e3c1743b7116e69e222295163ff1caa1681fac09120eca307586e1a7"),
                nistBlocks(cbc(Kr.CipherAlgorithm.AES_128_CBC, hex("2b7e151628aed2a6abf7158809cf4f3c"), iv, plaintext), iv.length),
                "CBC-AES128.Encrypt");
        //F.2.2, CBC-AES192.Encrypt
        assertArrayEquals(hex("4f021db243bc633d7178183a9fa071e8b4d9ada9ad7dedf4e5e738763f69145a"
                        + "571b242012fb7ae07fa9baac3df102e008b0e27988598881d920a9e64f5615cd"),
                nistBlocks(cbc(Kr.CipherAlgorithm.AES_192_CBC, hex("8e73b0f7da0e6452c810f32b809079e562f8ead2522c6b7b"), iv, plaintext), iv.length),
                "CBC-AES192.Encrypt");
        //F.2.3, CBC-AES256.Encrypt
        assertArrayEquals(hex("f58c4c04d6e5f1ba779eabfb5f7bfbd69cfc4e967edb808d679f777bc6702c7d"
                        + "39f23369a9d9bacfa530e26304231461b2eb05e2c39be9fcda6c19078c6a9d1b"),
                nistBlocks(cbc(Kr.CipherAlgorithm.AES_256_CBC, hex("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4"), iv, plaintext), iv.length),
                "CBC-AES256.Encrypt");
    }

    @Test
    public void blockCiphersAreReversible()
    {
        Kr kr = kr();
        byte[] data = ascii("the credit card number");
        for (Kr.CipherAlgorithm algorithm : Kr.CipherAlgorithm.values())
        {
            assumeTrue(kr.supports(algorithm), algorithm.name() + " is always available");
            Kr.BlockCipher cipher = kr.ciphers.of(algorithm, kr.keys.generateSecretKey(algorithm));
            byte[] encrypted = cipher.encrypt(data);
            assertArrayEquals(data, cipher.decrypt(encrypted), algorithm.name() + " goes back to the plaintext");
            //the initialization vector is random, so the same data never gives the same ciphertext twice
            assertFalse(Arrays.equals(encrypted, cipher.encrypt(data)), algorithm.name() + " uses a fresh iv every time");
            //the data is padded to whole blocks by CBC, and left alone by CTR
            assertEquals(16 + (algorithm.isPadded() ? 32 : data.length), encrypted.length, algorithm.name() + " length");
            assertArrayEquals(new byte[0], cipher.decrypt(cipher.encrypt(new byte[0])), algorithm.name() + " with no data");
        }
    }

    @Test
    public void blockCiphersFollowTheKeyOfTheirAlgorithm()
    {
        Kr kr = kr();
        for (Kr.CipherAlgorithm algorithm : Kr.CipherAlgorithm.values())
        {
            SecretKey key = kr.keys.generateSecretKey(algorithm);
            assertEquals(algorithm.keyBytes(), key.getEncoded().length, algorithm.name() + " key length");
            assertEquals("AES", key.getAlgorithm(), algorithm.name() + " key algorithm");
            assertThrows(IllegalArgumentException.class,
                    () -> kr.ciphers.of(algorithm, new byte[algorithm.keyBytes() + 1]), algorithm.name() + " needs the right key size");
            assertThrows(IllegalArgumentException.class, () -> kr.ciphers.of(algorithm,
                            new SecretKeySpec(kr.keys.generateSecretKey(Kr.CipherAlgorithm.AES_256_CTR).getEncoded(), "ChaCha20")),
                    "a ChaCha20 key is not an AES key");
        }
        assertThrows(IllegalArgumentException.class, () -> kr.ciphers.aesCtr(new byte[17]), "17 is not an AES key size");
        assertThrows(IllegalArgumentException.class, () -> kr.ciphers.aesCbc(new byte[17]), "17 is not an AES key size");
    }

    @Test
    public void blockCiphersNeedAnInitializationVectorOfTheBlockSize()
    {
        Kr kr = kr();
        for (Kr.CipherAlgorithm algorithm : Kr.CipherAlgorithm.values())
        {
            Kr.BlockCipher cipher = kr.ciphers.of(algorithm, kr.keys.generateSecretKey(algorithm));
            assertEquals(16, cipher.ivBytes(), algorithm.name() + " iv length");
            assertThrows(IllegalArgumentException.class, () -> cipher.encryptWithIv(new byte[15], ascii("data")),
                    algorithm.name() + " needs a whole initialization vector");
            assertThrows(NullPointerException.class, () -> cipher.encryptWithIv(null, ascii("data")),
                    algorithm.name() + " needs an initialization vector");
            assertThrows(NullPointerException.class, () -> cipher.encrypt(null),
                    algorithm.name() + " needs plaintext");
            assertThrows(CryptoException.class, () -> cipher.decrypt(new byte[8]),
                    algorithm.name() + " needs a whole envelope");
            assertThrows(NullPointerException.class, () -> cipher.encryptWithIv(new byte[16], null),
                    algorithm.name() + " needs plaintext");
            assertThrows(NullPointerException.class, () -> cipher.decrypt(null),
                    algorithm.name() + " needs ciphertext");
            assertThrows(NullPointerException.class, () -> kr.ciphers.of(algorithm, (SecretKey) null),
                    algorithm.name() + " needs a key");
        }
    }

    private byte[] ctr(Kr.CipherAlgorithm algorithm, byte[] keyBytes, byte[] iv, byte[] plaintext)
    {
        return Arrays.copyOfRange(kr().ciphers.of(algorithm, keyBytes).encryptWithIv(iv, plaintext), iv.length,
                iv.length + plaintext.length);
    }

    private byte[] cbc(Kr.CipherAlgorithm algorithm, byte[] keyBytes, byte[] iv, byte[] plaintext)
    {
        return kr().ciphers.of(algorithm, keyBytes).encryptWithIv(iv, plaintext);
    }

    /**
     * The NIST vectors leave out the padding, so only the blocks of the
     * plaintext are compared.
     */
    private static byte[] nistBlocks(byte[] encrypted, int ivLength)
    {
        return Arrays.copyOfRange(encrypted, ivLength, ivLength + 64);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// key wrapping ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void aesKwRfc3394KnownAnswer()
    {
        Kr kr = kr();
        //section 4.1, 128 bits of key data with a 128 bits key encryption key
        assertArrayEquals(hex("1fa68b0a8112b447aef34bd8fb5a7b829d3e862371d2cfe5"),
                kr.wraps.wrap(KeyWrapAlgorithm.AES_128_KW, kek(16), hex("00112233445566778899aabbccddeeff")),
                "4.1 wrap 128 bits of key data with a 128-bit KEK");
        //section 4.2, the same with a 192 bits key encryption key
        assertArrayEquals(hex("96778b25ae6ca435f92b5b97c050aed2468ab8a17ad84e5d"),
                kr.wraps.wrap(KeyWrapAlgorithm.AES_192_KW, kek(24), hex("00112233445566778899aabbccddeeff")),
                "4.2 wrap 128 bits of key data with a 192-bit KEK");
        //section 4.3, the same with a 256 bits key encryption key
        assertArrayEquals(hex("64e8c3f9ce0f5ba263e9777905818a2a93c8191e7d6e8ae7"),
                kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW, kek(32), hex("00112233445566778899aabbccddeeff")),
                "4.3 wrap 128 bits of key data with a 256-bit KEK");
        //section 4.4, 192 bits of key data with a 192 bits key encryption key
        assertArrayEquals(hex("031d33264e15d33268f24ec260743edce1c6c7ddee725a936ba814915c6762d2"),
                kr.wraps.wrap(KeyWrapAlgorithm.AES_192_KW, kek(24),
                        hex("00112233445566778899aabbccddeeff0001020304050607")),
                "4.4 wrap 192 bits of key data with a 192-bit KEK");
        //section 4.5, the same with a 256 bits key encryption key
        assertArrayEquals(hex("a8f9bc1612c68b3ff6e6f4fbe30e71e4769c8b80a32cb8958cd5d17d6b254da1"),
                kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW, kek(32),
                        hex("00112233445566778899aabbccddeeff0001020304050607")),
                "4.5 wrap 192 bits of key data with a 256-bit KEK");
        //section 4.6, 256 bits of key data with a 256 bits key encryption key
        assertArrayEquals(hex("28c9f404c4b810f4cbccb35cfb87f8263f5786e2d80ed326cbc7f0e71a99f43bfb988b9b7a02dd21"),
                kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW, kek(32),
                        hex("00112233445566778899aabbccddeeff000102030405060708090a0b0c0d0e0f")),
                "4.6 wrap 256 bits of key data with a 256-bit KEK");
    }

    @Test
    public void aesKwUnwrapsWhatItWrapped()
    {
        Kr kr = kr();
        for (KeyWrapAlgorithm algorithm : new KeyWrapAlgorithm[] { KeyWrapAlgorithm.AES_128_KW,
            KeyWrapAlgorithm.AES_192_KW, KeyWrapAlgorithm.AES_256_KW })
        {
            assumeTrue(kr.supports(algorithm), algorithm.name() + " is always available");
            SecretKey kek = kek(algorithm.keyBytes());
            byte[] keyData = hex("00112233445566778899aabbccddeeff000102030405060708090a0b0c0d0e0f");
            byte[] wrapped = kr.wraps.wrap(algorithm, kek, keyData);
            assertEquals(keyData.length + 8, wrapped.length, algorithm.name() + " adds one block");
            assertArrayEquals(keyData, kr.wraps.unwrap(algorithm, kek, wrapped), algorithm.name() + " round trip");
            //the wrapped key data carries its own check, so another key does not unwrap it
            assertThrows(CryptoException.class, () -> kr.wraps.unwrap(algorithm,
                            new SecretKeySpec(kr.random.bytes(algorithm.keyBytes()), "AES"), wrapped),
                    algorithm.name() + " is not unwrapped by another key");
            byte[] tampered = wrapped.clone();
            tampered[tampered.length - 1] ^= 0x01;
            assertThrows(CryptoException.class, () -> kr.wraps.unwrap(algorithm, kek, tampered),
                    algorithm.name() + " detects a tampered wrapped key");
        }
    }

    @Test
    public void rsaKwAgreesWithTheRsaEncryption()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(KeyAlgorithm.RSA), "RSA is always available");
        //RSA key wrapping is the RSAES-PKCS1-v1_5 encryption of the initial
        //value, the length of the key data and the key data, so what the JCA
        //encrypts and decrypts is what this wraps and unwraps
        for (int bits : new int[] { 2048, 3072 })
        {
            KeyPair keyPair = kr.keys.generate(KeyAlgorithm.RSA, bits);
            for (int length : new int[] { 16, 24, 32 })
            {
                byte[] keyData = kr.random.bytes(length);
                byte[] payload = kr.utils.concat(
                        new byte[] { (byte) 0xA6, (byte) 0xA6, (byte) 0xA6, (byte) 0xA6,
                            (byte) 0xA6, (byte) 0xA6, (byte) 0xA6, (byte) 0xA6,
                            0, 0, 0, (byte) length },
                        keyData);
                //the JCA encrypts what this unwraps
                assertArrayEquals(keyData, kr.wraps.unwrap(KeyWrapAlgorithm.RSA_KW, keyPair.getPrivate(),
                                rsaCipher(Cipher.ENCRYPT_MODE, keyPair.getPublic(), null, payload)),
                        bits + " bits RSA key, " + length + " bytes of key data");
                //and this wraps what the JCA decrypts
                byte[] wrapped = kr.wraps.wrap(KeyWrapAlgorithm.RSA_KW, keyPair.getPublic(), keyData);
                assertEquals(bits / 8, wrapped.length, bits + " bits RSA key wrap length");
                assertArrayEquals(payload,
                        rsaCipher(Cipher.DECRYPT_MODE, null, keyPair.getPrivate(), wrapped),
                        bits + " bits RSA key wrap, " + length + " bytes of key data");
            }
        }
    }

    /**
     * The RSA encryption of the JCA with the PKCS#1 v1.5 padding, which is
     * what an RSA key wrap is built on.
     */
    private byte[] rsaCipher(int mode, PublicKey publicKey, PrivateKey privateKey, byte[] data)
    {
        try
        {
            Provider provider = kr().provider();
            Cipher cipher = provider == null ? Cipher.getInstance("RSA/ECB/PKCS1Padding")
                    : Cipher.getInstance("RSA/ECB/PKCS1Padding", provider);
            if (publicKey != null)
            {
                cipher.init(mode, publicKey);
            }
            else
            {
                cipher.init(mode, privateKey);
            }
            return cipher.doFinal(data);
        }
        catch (GeneralSecurityException ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    public void rsaKwWrapsAndUnwrapsKeys()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(KeyAlgorithm.RSA), "RSA is always available");
        for (int bits : new int[] { 2048, 3072 })
        {
            KeyPair keyPair = kr.keys.generate(KeyAlgorithm.RSA, bits);
            for (int keyBytes : new int[] { 16, 24, 32 })
            {
                byte[] keyData = kr.random.bytes(keyBytes);
                byte[] wrapped = kr.wraps.wrap(KeyWrapAlgorithm.RSA_KW, keyPair.getPublic(), keyData);
                assertEquals(bits / 8, wrapped.length, bits + " bits RSA key wrap length");
                assertArrayEquals(keyData, kr.wraps.unwrap(KeyWrapAlgorithm.RSA_KW, keyPair.getPrivate(), wrapped),
                        bits + " bits RSA key, " + keyBytes + " bytes of key data");
            }
            //the integrity check of the wrapped key data is what detects another private key
            KeyPair other = kr.keys.generate(KeyAlgorithm.RSA, bits);
            assertThrows(CryptoException.class, () -> kr.wraps.unwrap(KeyWrapAlgorithm.RSA_KW, other.getPrivate(),
                            kr.wraps.wrap(KeyWrapAlgorithm.RSA_KW, keyPair.getPublic(), kr.random.bytes(32))),
                    bits + " bits RSA key wrap needs its own private key");
        }
    }

    @Test
    public void keyWrapsAreChecked()
    {
        Kr kr = kr();
        assumeTrue(kr.supports(KeyAlgorithm.RSA), "RSA is always available");
        assertThrows(NullPointerException.class, () -> kr.wraps.wrap((KeyWrapAlgorithm) null, kek(32), new byte[32]),
                "an algorithm is needed");
        assertThrows(NullPointerException.class, () -> kr.wraps.unwrap(KeyWrapAlgorithm.AES_256_KW, (SecretKey) null, new byte[24]),
                "a key encryption key is needed");
        assertThrows(NullPointerException.class, () -> kr.wraps.wrap(KeyWrapAlgorithm.RSA_KW, (PublicKey) null, new byte[32]),
                "a public key is needed");
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW, kek(16), new byte[32]),
                "a 256 bits key wrap needs a 256 bits key encryption key");
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW, kek(32), new byte[17]),
                "the key data must be a whole number of 8 bytes blocks");
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW, kek(32), new byte[8]),
                "the key data must be at least 16 bytes long");
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.unwrap(KeyWrapAlgorithm.AES_256_KW, kek(32), new byte[20]),
                "the wrapped key data must be a whole number of 8 bytes blocks");
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW, kek(32), new byte[0]),
                "there must be key data to wrap");
        assertThrows(IllegalArgumentException.class,
                () -> kr.wraps.wrap(KeyWrapAlgorithm.RSA_KW, kek(32), new byte[32]),
                "RSA key wrap does not take a key encryption key");
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.wrap(KeyWrapAlgorithm.AES_256_KW,
                        kr.keys.generate(KeyAlgorithm.RSA).getPublic(), new byte[32]),
                "AES key wrap does not take an RSA public key");
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.unwrap(KeyWrapAlgorithm.AES_256_KW,
                        kr.keys.generate(KeyAlgorithm.RSA).getPrivate(), new byte[24]),
                "AES key unwrap does not take an RSA private key");
        //the key data of a small RSA key does not fit
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.wrap(KeyWrapAlgorithm.RSA_KW,
                        kr.keys.generate(KeyAlgorithm.RSA, 1024).getPublic(), new byte[128]),
                "128 bytes of key data do not fit in a 1024 bits RSA key wrap");
        //and neither does one that does not hold the key of another algorithm
        assertThrows(IllegalArgumentException.class, () -> kr.wraps.wrap(KeyWrapAlgorithm.RSA_KW,
                        kr.keys.generateEc(Kr.EcCurve.SECP256R1).getPublic(), new byte[32]),
                "an EC key is not an RSA key");
    }

    /**
     * The key encryption key of the RFC 3394 vectors is the sequence
     * 00, 01, 02 and so on.
     */
    ////////////////////////////////////////////////////////////////////////////
    ///// poly1305 ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void poly1305Rfc8439Example()
    {
        Kr kr = kr();
        //section 2.5.2, where the key comes from a key generation that is not
        //done here
        assertEquals("a8061dc1305136c6c22b8baf0c0127a9", kr.poly1305.hex(
                hex("85d6be7857556d337f4452fe42d506a80103808afb0db2fd4abff6af4149f51b"),
                hex("43727970746f6772617068696320466f72756d2052657365617263682047726f7570")), "2.5.2");
        //an empty message is nothing but s
        assertEquals("0103808afb0db2fd4abff6af4149f51b", kr.poly1305.hex(
                hex("85d6be7857556d337f4452fe42d506a80103808afb0db2fd4abff6af4149f51b")), "an empty message");
    }

    @Test
    public void poly1305Rfc8439TestVectors()
    {
        Kr kr = kr();
        //test vector #1
        assertEquals("00000000000000000000000000000000", kr.poly1305.hex(
                hex("0000000000000000000000000000000000000000000000000000000000000000"),
                hex("00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")), "#1");
        //test vector #2
        assertEquals("36e5f6b5c5e06070f0efca96227a863e", kr.poly1305.hex(
                hex("0000000000000000000000000000000036e5f6b5c5e06070f0efca96227a863e"),
                hex("416e79207375626d697373696f6e20746f20746865204945544620696e74656e6465642062792074686520436f6e7472696275746f7220666f72207075626c69636174696f6e20617320616c6c206f722070617274206f6620616e204945544620496e7465726e65742d4472616674206f722052464320616e6420616e792073746174656d656e74206d6164652077697468696e2074686520636f6e74657874206f6620616e204945544620616374697669747920697320636f6e7369646572656420616e20224945544620436f6e747269627574696f6e222e20537563682073746174656d656e747320696e636c756465206f72616c2073746174656d656e747320696e20494554462073657373696f6e732c2061732077656c6c206173207772697474656e20616e6420656c656374726f6e696320636f6d6d756e69636174696f6e73206d61646520617420616e792074696d65206f7220706c6163652c207768696368206172652061646472657373656420746f")), "#2");
        //test vector #3: r is zero, so the tag is s
        assertEquals("f3477e7cd95417af89a6b8794c310cf0", kr.poly1305.hex(
                hex("36e5f6b5c5e06070f0efca96227a863e00000000000000000000000000000000"),
                hex("416e79207375626d697373696f6e20746f20746865204945544620696e74656e6465642062792074686520436f6e7472696275746f7220666f72207075626c69636174696f6e20617320616c6c206f722070617274206f6620616e204945544620496e7465726e65742d4472616674206f722052464320616e6420616e792073746174656d656e74206d6164652077697468696e2074686520636f6e74657874206f6620616e204945544620616374697669747920697320636f6e7369646572656420616e20224945544620436f6e747269627574696f6e222e20537563682073746174656d656e747320696e636c756465206f72616c2073746174656d656e747320696e20494554462073657373696f6e732c2061732077656c6c206173207772697474656e20616e6420656c656374726f6e696320636f6d6d756e69636174696f6e73206d61646520617420616e792074696d65206f7220706c6163652c207768696368206172652061646472657373656420746f")), "#3");
        //test vector #4
        assertEquals("4541669a7eaaee61e708dc7cbcc5eb62", kr.poly1305.hex(
                hex("1c9240a5eb55d38af333888604f6b5f0473917c1402b80099dca5cbc207075c0"),
                hex("2754776173206272696c6c69672c20616e642074686520736c6974687920746f7665730a446964206779726520616e642067696d626c6520696e2074686520776162653a0a416c6c206d696d737920776572652074686520626f726f676f7665732c0a416e6420746865206d6f6d65207261746873206f757467726162652e")), "#4");
        //test vector #5: a block that needs a partial reduction
        assertEquals("03000000000000000000000000000000", kr.poly1305.hex(
                hex("0200000000000000000000000000000000000000000000000000000000000000"),
                hex("ffffffffffffffffffffffffffffffff")), "#5");
        //test vector #6: the sum of s wraps around
        assertEquals("03000000000000000000000000000000", kr.poly1305.hex(
                hex("02000000000000000000000000000000ffffffffffffffffffffffffffffffff"),
                hex("02000000000000000000000000000000")), "#6");
        //test vector #7: a block of all ones with a carry
        assertEquals("05000000000000000000000000000000", kr.poly1305.hex(
                hex("0100000000000000000000000000000000000000000000000000000000000000"),
                hex("fffffffffffffffffffffffffffffffff0ffffffffffffffffffffffffffffff11000000000000000000000000000000")), "#7");
        //test vector #8: the accumulator ends up exactly at the prime
        assertEquals("00000000000000000000000000000000", kr.poly1305.hex(
                hex("0100000000000000000000000000000000000000000000000000000000000000"),
                hex("fffffffffffffffffffffffffffffffffbfefefefefefefefefefefefefefefe01010101010101010101010101010101")), "#8");
        //test vector #9: the accumulator ends up one below the prime
        assertEquals("faffffffffffffffffffffffffffffff", kr.poly1305.hex(
                hex("0200000000000000000000000000000000000000000000000000000000000000"),
                hex("fdffffffffffffffffffffffffffffff")), "#9");
        //test vector #10: the reduction takes 131 bits
        assertEquals("14000000000000005500000000000000", kr.poly1305.hex(
                hex("0100000000000000040000000000000000000000000000000000000000000000"),
                hex("e33594d7505e43b900000000000000003394d7505e4379cd01000000000000000000000000000000000000000000000001000000000000000000000000000000")), "#10");
        //test vector #11: the reduced result takes 131 bits
        assertEquals("13000000000000000000000000000000", kr.poly1305.hex(
                hex("0100000000000000040000000000000000000000000000000000000000000000"),
                hex("e33594d7505e43b900000000000000003394d7505e4379cd010000000000000000000000000000000000000000000000")), "#11");
    }

    @Test
    public void poly1305AuthenticatesTheChunksAsOneMessage()
    {
        Kr kr = kr();
        byte[] key = kr.random.bytes(32);
        byte[] message = kr.random.bytes(97);
        byte[] tag = kr.poly1305.poly1305(key, message);
        assertEquals(16, tag.length, "the length of the tag");
        //the chunks are the message, no matter how they are cut
        for (int split : new int[] { 0, 1, 15, 16, 17, 32, 48, 96, 97 })
        {
            assertArrayEquals(tag, kr.poly1305.poly1305(key, Arrays.copyOfRange(message, 0, split),
                    Arrays.copyOfRange(message, split, message.length)), "split at " + split);
        }
        assertArrayEquals(tag, kr.poly1305.poly1305(key, message, new byte[0]), "with an empty chunk");
        //the key is a one time key of 32 bytes, and it is not a password
        assertThrows(IllegalArgumentException.class, () -> kr.poly1305.poly1305(kr.random.bytes(16), message),
                "a 16 bytes key");
        assertThrows(IllegalArgumentException.class, () -> kr.poly1305.poly1305(kr.random.bytes(33), message),
                "a 33 bytes key");
        assertThrows(NullPointerException.class, () -> kr.poly1305.poly1305((byte[]) null, message), "a key");
        assertThrows(NullPointerException.class, () -> kr.poly1305.poly1305(key, (byte[]) null), "data");
        assertThrows(NullPointerException.class, () -> kr.poly1305.poly1305(key, message, null), "data");
    }

    @Test
    public void poly1305VerifiesTags()
    {
        Kr kr = kr();
        byte[] key = kr.random.bytes(32);
        byte[] message = kr.random.bytes(64);
        byte[] tag = kr.poly1305.poly1305(key, message);
        assertEquals(Hex.encode(tag), kr.poly1305.hex(key, message), "the tag in hexadecimal");
        assertTrue(kr.poly1305.verify(tag, key, message), "the tag of the message");
        assertTrue(kr.poly1305.verify(tag, new SecretKeySpec(key, "Poly1305"), message), "the same key as a SecretKey");
        byte[] tampered = tag.clone();
        tampered[15] ^= 0x01;
        assertFalse(kr.poly1305.verify(tampered, key, message), "a tampered tag");
        assertFalse(kr.poly1305.verify(Arrays.copyOf(tag, 15), key, message), "a short tag");
        assertFalse(kr.poly1305.verify(tag, kr.random.bytes(32), message), "another key");
        assertFalse(kr.poly1305.verify(tag, key, kr.random.bytes(64)), "another message");
        assertThrows(NullPointerException.class, () -> kr.poly1305.verify(null, key, message), "a tag");
    }

    private static SecretKey kek(int bytes)
    {
        byte[] keyBytes = new byte[bytes];
        for (int i = 0; i < bytes; i++)
        {
            keyBytes[i] = (byte) i;
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// kdf /////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void hkdfRfc5869()
    {
        Kr kr = kr();
        byte[] ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
        byte[] salt = hex("000102030405060708090a0b0c");
        byte[] info = hex("f0f1f2f3f4f5f6f7f8f9");

        //test case 1, SHA-256
        assertEquals("3cb25f25faacd57a90434f64d0362f2a"
                + "2d2d0a90cf1a5a4c5db02d56ecc4c5bf"
                + "34007208d5b887185865",
                Hex.encode(kr.kdf.hkdf(HkdfAlgorithm.SHA256, ikm, salt, info, 42)));
        //the same, asking for 64 bytes instead of 42
        assertEquals("3cb25f25faacd57a90434f64d0362f2a"
                + "2d2d0a90cf1a5a4c5db02d56ecc4c5bf"
                + "34007208d5b887185865"
                + "b4b0a85a993b89b9b65683d60f0106d28fff039d0b6f",
                Hex.encode(kr.kdf.hkdf(HkdfAlgorithm.SHA256, ikm, salt, info, 64)));
        //test case 3, without salt and without info
        assertEquals("8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d"
                + "9d201395faa4b61a96c8",
                Hex.encode(kr.kdf.hkdf(HkdfAlgorithm.SHA256, ikm, null, null, 42)));
        //test case 4 uses SHA-1, which is not part of the API on purpose
        assertEquals("9b5097a86038b805309076a44b3a9f38063e25b516dcbf369f394cfab43685f74"
                + "8b6457763e4f0204fc5",
                Hex.encode(kr.kdf.hkdf384(ikm, salt, info, 42)));
        assertEquals("832390086cda71fb47625bb5ceb168e4c8e26a1a16ed34d9fc7fe92c1481579"
                + "338da362cb8d9f925d7cb",
                Hex.encode(kr.kdf.hkdf512(ikm, salt, info, 42)));
    }

    @Test
    public void hkdfRejectsBadSizes()
    {
        Kr kr = kr();
        byte[] ikm = kr.random.bytes(32);
        assertThrows(IllegalArgumentException.class, () -> kr.kdf.hkdf(HkdfAlgorithm.SHA256, ikm, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> kr.kdf.hkdf(HkdfAlgorithm.SHA256, ikm, null, null, 255 * 32 + 1));
        assertThrows(NullPointerException.class, () -> kr.kdf.hkdf(HkdfAlgorithm.SHA256, null, null, null, 32));
        assertThrows(NullPointerException.class, () -> kr.kdf.hkdf(null, ikm, null, null, 32));
    }

    @Test
    public void hkdfKeyHasTheSizeOfTheAead()
    {
        Kr kr = kr();
        assumeSupports(kr, AeadAlgorithm.AES_256_GCM);
        SecretKey key = kr.kdf.hkdfKey(AeadAlgorithm.AES_256_GCM, kr.random.bytes(32), null, ascii("info"));
        assertEquals("AES", key.getAlgorithm());
        assertEquals(32, key.getEncoded().length);
        Kr.Aead aead = kr.aeads.of(AeadAlgorithm.AES_256_GCM, key);
        byte[] plaintext = ascii("sealed with a derived key");
        assertArrayEquals(plaintext, aead.open(aead.seal(plaintext)));
    }

    @Test
    public void pbkdf2KnownAnswer()
    {
        Kr kr = kr();
        //well known values, they are in every password hashing tutorial
        char[] password = "password".toCharArray();
        byte[] salt = "salt".getBytes(StandardCharsets.UTF_8);
        assertEquals("c5e478d59288c841aa530db6845c4c8d962893a0",
                Hex.encode(kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 4096, 20)));
        assertEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
                Hex.encode(kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 4096, 32)));
        assertEquals("d197b1b33db0143e018b12f3d1d1479e6cdebdcc",
                Hex.encode(kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA512, password, salt, 4096, 20)));
        assertEquals("d197b1b33db0143e018b12f3d1d1479e6cdebdcc97c5c0f87f6902e072f457b5",
                Hex.encode(kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA512, password, salt, 4096, 32)));
    }

    @Test
    public void pbkdf2IsDeterministicAndDependsOnEverything()
    {
        Kr kr = kr();
        char[] password = "a password long enough".toCharArray();
        byte[] salt = new byte[16];
        //1000 iterations is what a test can afford, the strict method does not
        //allow it and the legacy one does
        byte[] expected = kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 1000, 32);
        assertEquals(32, expected.length);
        assertArrayEquals(expected, kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 1000, 32), "it must be deterministic");
        assertFalse(Arrays.equals(expected, kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 1001, 32)), "iterations");
        assertFalse(Arrays.equals(expected, kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, kr.random.bytes(16), 1000, 32)), "salt");
        assertFalse(Arrays.equals(expected, kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA512, password, salt, 1000, 32)), "algorithm");
        assertFalse(Arrays.equals(expected, kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, "another password".toCharArray(), salt, 1000, 32)), "password");
        assertFalse(Arrays.equals(expected, kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 1000, 31)), "output length");
        assertArrayEquals(Arrays.copyOf(expected, 16), kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 1000, 16), "the prefix must not depend on the requested length");

        //a char[] password and its UTF-8 bytes must give the same result
        assertArrayEquals(kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 1000, 32),
                kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, "a password long enough".getBytes(StandardCharsets.UTF_8), salt, 1000, 32));
    }

    @Test
    public void pbkdf2EnforcesTheSafeDefaults()
    {
        Kr kr = kr();
        char[] password = "a password long enough".toCharArray();
        byte[] salt = new byte[16];
        assertThrows(IllegalArgumentException.class, () -> kr.kdf.pbkdf2(Pbkdf2Algorithm.HMAC_SHA256, password,
                new byte[Pbkdf2Algorithm.MINIMUM_SALT_BYTES - 1], 1000, 32), "a short salt is refused");
        assertThrows(IllegalArgumentException.class, () -> kr.kdf.pbkdf2(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 1000, 0));
        assertThrows(IllegalArgumentException.class, () -> kr.kdf.pbkdf2(Pbkdf2Algorithm.HMAC_SHA256, password, salt,
                Pbkdf2Algorithm.HMAC_SHA256.minimumIterations() - 1, 32), "too few iterations for SHA-256");
        assertThrows(IllegalArgumentException.class, () -> kr.kdf.pbkdf2(Pbkdf2Algorithm.HMAC_SHA512, password, salt,
                Pbkdf2Algorithm.HMAC_SHA512.minimumIterations() - 1, 32), "too few iterations for SHA-512");
        assertThrows(NullPointerException.class, () -> kr.kdf.pbkdf2(Pbkdf2Algorithm.HMAC_SHA256, (char[]) null, salt, 1000, 32));

        //the legacy method exists exactly to read what was derived in the past
        assertEquals(32, kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, 1000, 32).length);
        assertEquals(32, kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, new byte[4], 1000, 32).length);
        int iterations = Pbkdf2Algorithm.HMAC_SHA256.minimumIterations();
        assertArrayEquals(kr.kdf.pbkdf2ForLegacyInterop(Pbkdf2Algorithm.HMAC_SHA256, password, salt, iterations, 32),
                kr.kdf.pbkdf2(Pbkdf2Algorithm.HMAC_SHA256, password, salt, iterations, 32),
                "the legacy method must agree with the strict one when the parameters are good");
    }

    @Test
    public void pbkdf2KeyCanEncrypt()
    {
        Kr kr = kr();
        assumeSupports(kr, AeadAlgorithm.CHACHA20_POLY1305);
        SecretKey key = kr.kdf.pbkdf2Key(AeadAlgorithm.CHACHA20_POLY1305,
                "a password long enough".toCharArray(), new byte[16], Pbkdf2Algorithm.HMAC_SHA256.minimumIterations());
        Kr.Aead aead = kr.aeads.chacha20Poly1305(key);
        byte[] plaintext = ascii("the password derived the key");
        assertArrayEquals(plaintext, aead.open(aead.seal(plaintext)));
    }

    @Test
    public void ecdhAgreesOnBothSides()
    {
        Kr kr = kr();
        assumeSupports(kr, EcCurve.SECP256R1);
        KeyPair alice = kr.keys.generateEc(EcCurve.SECP256R1);
        KeyPair bob = kr.keys.generateEc(EcCurve.SECP256R1);

        byte[] fromAlice = kr.kdf.ecdh(alice.getPrivate(), bob.getPublic());
        byte[] fromBob = kr.kdf.ecdh(bob.getPrivate(), alice.getPublic());
        assertEquals(32, fromAlice.length, "P-256 gives 32 bytes");
        assertArrayEquals(fromAlice, fromBob, "both sides must agree");
        assertFalse(Arrays.equals(fromAlice, kr.kdf.ecdh(alice.getPrivate(), alice.getPublic())), "its own public key is not the shared secret");

        //the raw shared secret is already a symmetric key
        Kr.Aead aead = kr.aeads.aesGcm(fromAlice);
        byte[] plaintext = ascii("a message that only alice and bob can read");
        assertArrayEquals(plaintext, aead.open(aead.seal(plaintext)));
    }

    @Test
    public void ecdhHkdfAgreesOnBothSides()
    {
        Kr kr = kr();
        assumeSupports(kr, EcCurve.SECP256R1);
        assumeSupports(kr, AeadAlgorithm.AES_256_GCM);
        KeyPair alice = kr.keys.generateEc(EcCurve.SECP256R1);
        KeyPair bob = kr.keys.generateEc(EcCurve.SECP256R1);
        byte[] salt = kr.random.bytes(16);
        byte[] info = ascii("a conversation between alice and bob");
        assertArrayEquals(
                kr.kdf.ecdhHkdf(HkdfAlgorithm.SHA256, alice.getPrivate(), bob.getPublic(), salt, info, 32),
                kr.kdf.ecdhHkdf(HkdfAlgorithm.SHA256, bob.getPrivate(), alice.getPublic(), salt, info, 32));

        SecretKey key = kr.kdf.ecdhHkdfKey(AeadAlgorithm.AES_256_GCM, alice.getPrivate(), bob.getPublic(), salt, info);
        Kr.Aead aead = kr.aeads.of(AeadAlgorithm.AES_256_GCM, key);
        byte[] plaintext = ascii("hello bob");
        assertArrayEquals(plaintext, aead.open(aead.seal(plaintext)));
    }

    @Test
    public void ecdhRejectsKeysOfTheWrongType()
    {
        Kr kr = kr();
        assumeSupports(kr, EcCurve.SECP256R1);
        KeyPair rsa = kr.keys.generate(KeyAlgorithm.RSA);
        assertThrows(IllegalArgumentException.class, () -> kr.kdf.ecdh(rsa.getPrivate(), rsa.getPublic()), "RSA keys cannot agree on a key");
        assumeSupports(kr, KeyAlgorithm.ED25519);
        KeyPair ed25519 = kr.keys.generateEd25519();
        assertThrows(IllegalArgumentException.class, () -> kr.kdf.ecdh(ed25519.getPrivate(), ed25519.getPublic()),
                "Ed25519 keys sign, they do not agree on a key");
        assertThrows(NullPointerException.class, () -> kr.kdf.ecdh(null, rsa.getPublic()));
    }

    @Test
    public void x25519Rfc7748KnownAnswer()
    {
        Kr kr = kr();
        assumeSupports(kr, KeyAlgorithm.X25519);
        //RFC 7748, section 6.1
        KeyPair alice = kr.keys.decodeRawPair(KeyAlgorithm.X25519,
                hex("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"),
                hex("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a"));
        KeyPair bob = kr.keys.decodeRawPair(KeyAlgorithm.X25519,
                hex("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb"),
                hex("de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f"));
        byte[] shared = hex("4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742");
        assertArrayEquals(shared, kr.kdf.ecdh(alice.getPrivate(), bob.getPublic()));
        assertArrayEquals(shared, kr.kdf.ecdh(bob.getPrivate(), alice.getPublic()));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// password ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void argon2iKnownAnswer()
    {
        Kr kr = kr();
        assumeSupports(kr, Kr.Argon2Algorithm.I);
        //the vector of the reference implementation readme, the only published
        //Argon2 vector that this library can reproduce without the optional
        //secret and the associated data
        assertArrayEquals(hex("45d7ac72e76f242b20b77b9bf9bf9d5915894e669a24e6c6"),
                kr.password.argon2(chars("password"), ascii("somesalt"), Kr.Argon2Algorithm.I, 65536, 2, 4, 24));
    }

    @Test
    public void argon2EncodedIsTheModularCryptFormat()
    {
        Kr kr = kr();
        assumeSupports(kr, Kr.Argon2Algorithm.I);
        String encoded = "$argon2i$v=19$m=65536,t=2,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG";
        assertEquals(encoded, kr.password.argon2Encoded(chars("password"), ascii("somesalt"),
                Kr.Argon2Algorithm.I, 65536, 2, 4, 24));
        assertTrue(kr.password.verify(encoded, "password"), encoded);
        assertFalse(kr.password.verify(encoded, "Password"), "a different password");
        assertFalse(kr.password.verify(encoded, "passwor"), "a prefix of the password");
        assertFalse(kr.password.verify(encoded, "password "), "the password plus a space");
        assertFalse(kr.password.verify("$argon2i$v=19$m=65536,t=2,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObA",
                "password"), "a tampered hash");
    }

    @Test
    public void argon2VariantsRoundTrip()
    {
        Kr kr = kr();
        byte[] salt = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        for (Kr.Argon2Algorithm algorithm : Kr.Argon2Algorithm.values())
        {
            assumeSupports(kr, algorithm);
            byte[] hash = kr.password.argon2(chars("password"), salt, algorithm, 8192, 1, 1, 24);
            assertEquals(24, hash.length, algorithm.name());
            String encoded = kr.password.argon2Encoded(chars("password"), salt, algorithm, 8192, 1, 1, 24);
            assertTrue(encoded.startsWith("$" + algorithm.jcaName() + "$v=19$m=8192,t=1,p=1$"), encoded);
            assertTrue(kr.password.verify(encoded, "password"), algorithm.name());
            assertFalse(kr.password.verify(encoded, "wrong"), algorithm.name());
        }
    }

    @Test
    public void scryptKnownAnswer()
    {
        Kr kr = kr();
        assumeSupports(kr, Kr.PasswordAlgorithm.SCRYPT);
        //RFC 7914, section 12, where N is 1024 and the salt is 4 bytes, shorter
        //than what this library asks for a new hash, but a hash that already
        //exists has to verify anyway
        assertTrue(kr.password.verify(
                "$scrypt$ln=10,r=8,p=16$TmFDbA$/bq+HJ00cgB4VucZDQHp/nxq18vII3gw53N2Y0s3MWIurzDZLiKjiG/xCSedmDDaxyevuUqD7m2DYMvfoswGQA",
                "password"));
        assertFalse(kr.password.verify(
                "$scrypt$ln=10,r=8,p=16$TmFDbA$/bq+HJ00cgB4VucZDQHp/nxq18vII3gw53N2Y0s3MWIurzDZLiKjiG/xCSedmDDaxyevuUqD7m2DYMvfoswGQA",
                "Password"));
        byte[] salt = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        assertEquals(64, kr.password.scrypt(chars("password"), salt, 1024, 8, 16, 64).length);
        String encoded = kr.password.scryptEncoded(chars("password"), salt, 1024, 8, 16, 64);
        assertTrue(encoded.startsWith("$scrypt$ln=10,r=8,p=16$"), encoded);
        assertTrue(kr.password.verify(encoded, "password"), encoded);
        assertFalse(kr.password.verify(encoded, "password!"), "a different password");
    }

    @Test
    public void bcryptKnownAnswers()
    {
        Kr kr = kr();
        assumeSupports(kr, Kr.PasswordAlgorithm.BCRYPT);
        //the jbcrypt test vectors, they say 2a and this library writes 2b, but
        //both are the same function for a password of at most 72 bytes
        String[][] vectors = {
            {"a", "$2a$06$m0CrhHm10qJ3lXRY.5zDGO3rS2KdeeWLuGmsfGlMfOxih58VYVfxe"},
            {"abc", "$2a$06$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0i"},
            {"abc", "$2a$08$Ro0CUfOqk6cXEKf3dyaM7OhSCvnwM9s4wIX9JeLapehKK5YdLxKcm"},
            {"abc", "$2a$12$EXRkfkdmXn2gzds2SSitu.MW9.gAVqa9eLS1//RYtYCmB1eLHg.9q"},
            {"abcdefghijklmnopqrstuvwxyz", "$2a$06$.rCVZVOThsIa97pEDOxvGuRRgzG64bvtJ0938xuqzv18d3ZpQhstC"},
            {"~!@#$%^&*()      ~!@#$%^&*()PNBFRD", "$2a$10$LgfYWkbzEvQ4JakH7rOvHe0y8pHKF9OaFgwUZ2q7W2FFZmZzJYlfS"},
            {"\u03c0\u03c0\u03c0\u03c0\u03c0\u03c0\u03c0\u03c0", "$2a$10$.TtQJ4Jr6isd4Hp.mVfZeuh6Gws4rOQ/vdBczhDx.19NFK0Y84Dle"}
        };
        for (String[] vector : vectors)
        {
            assertTrue(kr.password.verify(vector[1], vector[0]), vector[1]);
            assertFalse(kr.password.verify(vector[1], vector[0] + "!"), vector[1]);
        }
    }

    @Test
    public void bcryptWritesTheVersionItSays()
    {
        Kr kr = kr();
        assumeSupports(kr, Kr.PasswordAlgorithm.BCRYPT);
        String encoded = kr.password.bcrypt(chars("secret"), 4);
        assertTrue(encoded.startsWith("$2b$04$"), encoded);
        assertEquals(60, encoded.length(), encoded);
        assertTrue(kr.password.verify(encoded, "secret"));
        assertNotEquals(encoded, kr.password.bcrypt(chars("secret"), 4), "the salt must be random");
    }

    @Test
    public void passwordHashesTakeThePasswordAsUtf8()
    {
        Kr kr = kr();
        String password = "\u03c0\u03c0\u03c0\u03c0";
        byte[] salt = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        assumeSupports(kr, Kr.Argon2Algorithm.ID);
        assertTrue(kr.password.verify(
                kr.password.argon2Encoded(password.toCharArray(), salt, Kr.Argon2Algorithm.ID, 8192, 1, 1, 32), password));
        assumeSupports(kr, Kr.PasswordAlgorithm.SCRYPT);
        assertTrue(kr.password.verify(
                kr.password.scryptEncoded(password.toCharArray(), salt, 1024, 8, 1, 32), password));
        assumeSupports(kr, Kr.PasswordAlgorithm.BCRYPT);
        assertTrue(kr.password.verify(kr.password.bcrypt(password.toCharArray(), 4), password));
    }

    @Test
    public void passwordHashesRejectBadParameters()
    {
        Kr kr = kr();
        byte[] salt = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        assumeSupports(kr, Kr.Argon2Algorithm.ID);
        assertThrows(IllegalArgumentException.class,
                () -> kr.password.argon2(chars("secret"), new byte[Kr.Password.MINIMUM_SALT_BYTES - 1],
                        Kr.Argon2Algorithm.ID, 8192, 1, 1, 32));
        assertThrows(IllegalArgumentException.class,
                () -> kr.password.argon2(chars("secret"), salt, Kr.Argon2Algorithm.ID, 8192, 0, 1, 32));
        assertThrows(IllegalArgumentException.class,
                () -> kr.password.argon2(chars("secret"), salt, Kr.Argon2Algorithm.ID, 8192, 1, 0, 32));
        assertThrows(IllegalArgumentException.class,
                () -> kr.password.argon2(chars("secret"), salt, Kr.Argon2Algorithm.ID, 8192, 1, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> kr.password.argon2(chars(""), salt, Kr.Argon2Algorithm.ID, 8192, 1, 1, 32));
        assertThrows(NullPointerException.class,
                () -> kr.password.argon2(null, salt, Kr.Argon2Algorithm.ID, 8192, 1, 1, 32));
        assumeSupports(kr, Kr.PasswordAlgorithm.SCRYPT);
        assertThrows(IllegalArgumentException.class,
                () -> kr.password.scrypt(chars("secret"), new byte[Kr.Password.MINIMUM_SALT_BYTES - 1], 1024, 8, 1, 32));
        assertThrows(IllegalArgumentException.class, () -> kr.password.scrypt(chars("secret"), salt, 3, 8, 1, 32),
                "the scrypt cost must be a power of two");
        assertThrows(IllegalArgumentException.class, () -> kr.password.scrypt(chars("secret"), salt, 1024, 8, 0, 32));
        assumeSupports(kr, Kr.PasswordAlgorithm.BCRYPT);
        assertThrows(IllegalArgumentException.class,
                () -> kr.password.bcrypt(chars("secret"), Kr.Password.MINIMUM_BCRYPT_COST - 1));
        assertThrows(IllegalArgumentException.class,
                () -> kr.password.bcrypt(chars("secret"), Kr.Password.MAXIMUM_BCRYPT_COST + 1));
    }

    @Test
    public void malformedModularCryptHashesAreRejected()
    {
        Kr kr = kr();
        assertThrows(NullPointerException.class, () -> kr.password.verify((String) null, "password"));
        assumeSupports(kr, Kr.Argon2Algorithm.ID);
        for (String broken : new String[]{
            "",
            "$",
            "not a password hash",
            "$argon2",
            "$argon2i$v=19$m=65536,t=2,p=4$c29tZXNhbHQ",
            "$argon2i$v=19$m=65536,t=2$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG",
            "$argon2i$v=17$m=65536,t=2,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG",
            "$argon2i$v=19$m=65536,t=2,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG$extra",
            "$argon2i$v=19$m=65536,t=2,4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG",
            "$argon2i$v=19$m=notanumber,t=2,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG",
            "$argon2x$v=19$m=65536,t=2,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG",
            "$argon2i$v=19$m=65536,t=2,p=4$c29tZXNhbHQ$not base64!",
            "$argon2i$v=19$m=65536,t=2,p=4$not base64!$RdescudvJCsgt3ub+b+dWRWJTmaaJObG"
        })
        {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> kr.password.verify(broken, "password"), broken);
            assertTrue(exception.getMessage().contains(broken), exception.getMessage());
        }
        assumeSupports(kr, Kr.PasswordAlgorithm.SCRYPT);
        for (String broken : new String[]{
            "$scrypt$ln=16,r=8$c29tZXNhbHQ$dGFzaA",
            "$scrypt$ln=16,r=8,p=1$not base64!$dGFzaA",
            "$scrypt$ln=16,r=8,p=1$c29tZXNhbHQ$not base64!",
            "$scrypt$n=16,r=8,p=1$c29tZXNhbHQ$dGFzaA"
        })
        {
            assertThrows(IllegalArgumentException.class, () -> kr.password.verify(broken, "password"), broken);
        }
    }

    @Test
    public void malformedBcryptHashesAreRejected()
    {
        Kr kr = kr();
        assumeSupports(kr, Kr.PasswordAlgorithm.BCRYPT);
        for (String broken : new String[]{
            "$2b$",
            "$2b$06$",
            "$2b$06$aaaa",
            "$2b$06$aaaaaaaaaaaaaa",
            "$2b$99$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0i",
            "$2z$06$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0i",
            "$2b$06$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0!"
        })
        {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> kr.password.verify(broken, "abc"), broken);
            assertTrue(exception.getMessage().contains(broken), exception.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// sign ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void signAndVerifyRoundTrip()
    {
        Kr kr = kr();
        for (SignatureAlgorithm algorithm : SignatureAlgorithm.values())
        {
            if (!kr.supports(algorithm))
            {
                continue;
            }
            KeyPair keyPair = kr.keys.generate(algorithm.keyAlgorithm());
            byte[] data = ascii("a message that will be signed");
            byte[] signature = kr.sign.sign(algorithm, keyPair.getPrivate(), data);
            assertTrue(kr.sign.verify(algorithm, keyPair.getPublic(), signature, data), algorithm.name());
            assertFalse(kr.sign.verify(algorithm, keyPair.getPublic(), signature, ascii("a message that was modified")), algorithm.name());

            byte[] tampered = signature.clone();
            tampered[tampered.length - 1] ^= 0x01;
            assertFalse(kr.sign.verify(algorithm, keyPair.getPublic(), tampered, data), algorithm.name());

            //another key of the same kind must not verify it
            assertFalse(kr.sign.verify(algorithm, kr.keys.generate(algorithm.keyAlgorithm()).getPublic(), signature, data), algorithm.name());
        }
    }

    @Test
    public void signEd25519Rfc8032KnownAnswer()
    {
        Kr kr = kr();
        assumeSupports(kr, SignatureAlgorithm.ED25519);
        //RFC 8032, section 7.1, test 1: the empty message
        KeyPair keyPair = kr.keys.decodeRawPair(KeyAlgorithm.ED25519,
                hex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"),
                hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"));
        byte[] signature = hex("e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e0652249015"
                + "55fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");
        assertArrayEquals(signature, kr.sign.signEd25519(keyPair.getPrivate(), new byte[0]), "Ed25519 signs the message as it is");
        assertTrue(kr.sign.verifyEd25519(keyPair.getPublic(), signature, new byte[0]));

        //test 2, a one byte message
        KeyPair second = kr.keys.decodeRawPair(KeyAlgorithm.ED25519,
                hex("4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb"),
                hex("3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c"));
        assertArrayEquals(hex("92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da0"
                        + "85ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00"),
                kr.sign.signEd25519(second.getPrivate(), ascii("r")));
    }

    @Test
    public void signEd25519IsWholeAndDeterministic()
    {
        Kr kr = kr();
        assumeSupports(kr, SignatureAlgorithm.ED25519);
        KeyPair keyPair = kr.keys.generateEd25519();
        byte[] data = ascii("a message");
        byte[] signature = kr.sign.signEd25519(keyPair.getPrivate(), data);
        assertEquals(64, signature.length, "Ed25519 signatures are always 64 bytes");
        assertArrayEquals(signature, kr.sign.signEd25519(keyPair.getPrivate(), data), "Ed25519 is deterministic");
        assertTrue(kr.sign.verifyEd25519(keyPair.getPublic(), signature, data));
        assertFalse(kr.sign.verifyEd25519(keyPair.getPublic(), signature, ascii("another message")));
        assertFalse(kr.sign.verifyEd25519(keyPair.getPublic(), signature, ascii("a message ")), "not even a trailing space is ignored");
        assertArrayEquals(signature, kr.sign.signEd25519(keyPair.getPrivate(), ascii("a "), ascii("message")), "the chunks are concatenated");
    }

    @Test
    public void signRejectsKeysOfAnotherAlgorithm()
    {
        Kr kr = kr();
        assumeSupports(kr, SignatureAlgorithm.SHA256_WITH_RSA);
        assumeSupports(kr, EcCurve.SECP256R1);
        KeyPair ec = kr.keys.generateEc(EcCurve.SECP256R1);
        KeyPair rsa = kr.keys.generate(KeyAlgorithm.RSA);
        assertThrows(CryptoException.class, () -> kr.sign.sign(SignatureAlgorithm.SHA256_WITH_RSA, ec.getPrivate(), ascii("data")));
        assertThrows(CryptoException.class, () -> kr.sign.verify(SignatureAlgorithm.SHA256_WITH_RSA, ec.getPublic(), new byte[256], ascii("data")));
        assertThrows(CryptoException.class, () -> kr.sign.sign(SignatureAlgorithm.SHA256_WITH_ECDSA, rsa.getPrivate(), ascii("data")));
        assertThrows(NullPointerException.class, () -> kr.sign.sign(SignatureAlgorithm.SHA256_WITH_RSA, null, ascii("data")));
        assertThrows(NullPointerException.class, () -> kr.sign.sign(SignatureAlgorithm.SHA256_WITH_RSA, rsa.getPrivate(), (byte[]) null));
        assertThrows(NullPointerException.class, () -> kr.sign.sign(SignatureAlgorithm.SHA256_WITH_RSA, rsa.getPrivate(), ascii("data"), (byte[]) null));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// keys ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void keyPairRoundTrip()
    {
        Kr kr = kr();
        for (KeyAlgorithm algorithm : KeyAlgorithm.values())
        {
            if (!kr.supports(algorithm))
            {
                continue;
            }
            KeyPair keyPair = kr.keys.generate(algorithm);
            assertNotNull(keyPair.getPublic());
            assertNotNull(keyPair.getPrivate());
            if (algorithm == KeyAlgorithm.RSA || algorithm == KeyAlgorithm.EC)
            {
                assertEquals(algorithm.defaultKeyBits(), keyBits(keyPair), algorithm.name());
            }
            else
            {
                //the X.509 encoding of these public keys is always 12 + 32 bytes, and
                //the raw one is what travels
                assertEquals(44, kr.keys.encode(keyPair.getPublic()).length, algorithm.name());
                byte[] raw = kr.keys.encodeRaw(keyPair.getPublic());
                assertEquals(32, raw.length, algorithm.name());
                assertArrayEquals(keyPair.getPublic().getEncoded(),
                        kr.keys.encode(kr.keys.decodeRawPublic(algorithm, raw)), algorithm.name());
            }

            byte[] encodedPublic = kr.keys.encode(keyPair.getPublic());
            byte[] encodedPrivate = kr.keys.encode(keyPair.getPrivate());
            assertNotNull(encodedPublic);
            assertNotNull(encodedPrivate);
            assertArrayEquals(encodedPublic, kr.keys.encode(kr.keys.decodePublic(algorithm, encodedPublic)), algorithm.name());
            assertArrayEquals(encodedPrivate, kr.keys.encode(kr.keys.decodePrivate(algorithm, encodedPrivate)), algorithm.name());
            assertArrayEquals(encodedPublic, kr.keys.encode(kr.keys.decodePair(algorithm, encodedPrivate, encodedPublic).getPublic()), algorithm.name());
            assertArrayEquals(encodedPrivate, kr.keys.encode(kr.keys.decodePair(algorithm, encodedPrivate, encodedPublic).getPrivate()), algorithm.name());
        }
    }

    @Test
    public void generateX25519()
    {
        Kr kr = kr();
        assumeSupports(kr, KeyAlgorithm.X25519);
        KeyPair first = kr.keys.generateX25519();
        KeyPair second = kr.keys.generateX25519();
        assertEquals(32, kr.keys.encodeRaw(first.getPublic()).length, "RFC 7748 raw public keys are 32 bytes");
        assertFalse(Arrays.equals(kr.keys.encodeRaw(first.getPublic()), kr.keys.encodeRaw(second.getPublic())));

        //a raw public key is what arrives from the other side, so it must give
        //the very same key
        KeyPair withTheOtherPublic = kr.keys.decodeRawPair(KeyAlgorithm.X25519, new byte[32], kr.random.bytes(32));
        assertEquals(32, kr.keys.encodeRaw(withTheOtherPublic.getPublic()).length);

        assertThrows(IllegalArgumentException.class, () -> kr.keys.decodeRawPublic(KeyAlgorithm.X25519, new byte[31]));
        assertThrows(IllegalArgumentException.class, () -> kr.keys.decodeRawPrivate(KeyAlgorithm.X25519, new byte[31]));
        assumeSupports(kr, EcCurve.SECP256R1);
        assertThrows(IllegalArgumentException.class, () -> kr.keys.encodeRaw(kr.keys.generateEc(EcCurve.SECP256R1).getPublic()),
                "only Ed25519 and X25519 public keys have a raw encoding");
        assertThrows(IllegalArgumentException.class, () -> kr.keys.decodeRawPublic(KeyAlgorithm.EC, new byte[32]));
    }

    @Test
    public void generateWithExplicitSize()
    {
        Kr kr = kr();
        assumeSupports(kr, KeyAlgorithm.EC);
        assertEquals(384, keyBits(kr.keys.generate(KeyAlgorithm.EC, 384)));
        assertEquals(521, keyBits(kr.keys.generate(KeyAlgorithm.EC, 521)));
        assertThrows(IllegalArgumentException.class, () -> kr.keys.generate(KeyAlgorithm.EC, 512), "there is no curve of 512 bits");
        assertThrows(IllegalArgumentException.class, () -> kr.keys.generate(KeyAlgorithm.RSA, Kr.MINIMUM_RSA_BITS - 1));
        assumeSupports(kr, KeyAlgorithm.ED25519);
        assertThrows(IllegalArgumentException.class, () -> kr.keys.generate(KeyAlgorithm.ED25519, 512), "Ed25519 has a fixed size");
    }

    @Test
    public void generateEcChecksTheCurve()
    {
        Kr kr = kr();
        assumeSupports(kr, EcCurve.SECP256R1);
        assertEquals(256, keyBits(kr.keys.generateEc(EcCurve.SECP256R1)));
        assumeSupports(kr, EcCurve.SECP384R1);
        assertEquals(384, keyBits(kr.keys.generateEc(EcCurve.SECP384R1)));
        assumeSupports(kr, EcCurve.SECP256K1);
        assertEquals(256, keyBits(kr.keys.generateEc(EcCurve.SECP256K1)));
        assertThrows(NullPointerException.class, () -> kr.keys.generateEc(null));
    }

    @Test
    public void secretKeyChecksTheAlgorithm()
    {
        Kr kr = kr();
        SecretKey key = kr.keys.generateSecretKey(AeadAlgorithm.AES_128_GCM);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(16, key.getEncoded().length);
        assertEquals(32, kr.keys.generateSecretKey(AeadAlgorithm.AES_256_GCM).getEncoded().length);
        assertEquals(32, kr.keys.generateSecretKey(AeadAlgorithm.CHACHA20_POLY1305).getEncoded().length);
        assertFalse(Arrays.equals(key.getEncoded(), kr.keys.generateSecretKey(AeadAlgorithm.AES_128_GCM).getEncoded()));
    }

    @Test
    public void decodeRejectsGarbage()
    {
        Kr kr = kr();
        assumeSupports(kr, KeyAlgorithm.RSA);
        assertThrows(CryptoException.class, () -> kr.keys.decodePublic(KeyAlgorithm.RSA, new byte[]{1, 2, 3}));
        assertThrows(CryptoException.class, () -> kr.keys.decodePrivate(KeyAlgorithm.RSA, new byte[]{1, 2, 3}));
        assertThrows(CryptoException.class, () -> kr.keys.decodePublic(KeyAlgorithm.RSA, new byte[0]));
        assertThrows(NullPointerException.class, () -> kr.keys.decodePublic(KeyAlgorithm.RSA, null));
        assertThrows(NullPointerException.class, () -> kr.keys.decodePrivate(KeyAlgorithm.RSA, null));
        assertThrows(NullPointerException.class, () -> kr.keys.decodePublic(null, new byte[0]));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// random //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void randomIsRandom()
    {
        Kr kr = kr();
        assertEquals(32, kr.random.bytes(32).length);
        assertEquals(0, kr.random.bytes(0).length);
        assertThrows(IllegalArgumentException.class, () -> kr.random.bytes(-1));
        assertFalse(Arrays.equals(kr.random.bytes(32), kr.random.bytes(32)));
        assertNotNull(kr.random.secureRandom());
        assertSame(kr.random.secureRandom(), kr.random.secureRandom(), "the source must be shared, seeding is expensive");
    }

    @Test
    public void randomFill()
    {
        Kr kr = kr();
        byte[] destination = new byte[64];
        kr.random.fill(destination);
        assertFalse(kr.utils.timingSafeEql(destination, new byte[64]));

        byte[] part = new byte[10];
        kr.random.fill(part, 5, 4);
        for (int i = 0; i < 5; i++)
        {
            assertEquals(0, part[i], "the bytes before the slice must not be touched");
        }
        assertFalse(kr.utils.timingSafeEql(Arrays.copyOfRange(part, 5, 9), new byte[4]));
        kr.utils.wipe(part);
        assertArrayEquals(new byte[10], part);

        assertThrows(IndexOutOfBoundsException.class, () -> kr.random.fill(part, 5, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> kr.random.fill(part, 20, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> kr.random.fill(part, -1, 2));
        assertThrows(NullPointerException.class, () -> kr.random.fill(null));
        assertThrows(NullPointerException.class, () -> kr.random.fill(null, 0, 1));
    }

    @Test
    public void randomNextIntIsInRange()
    {
        Kr kr = kr();
        boolean[] seen = new boolean[7];
        for (int i = 0; i < 7000; i++)
        {
            int value = kr.random.nextInt(seen.length);
            assertTrue(value >= 0 && value < seen.length, value + " is out of range");
            seen[value] = true;
        }
        for (int value = 0; value < seen.length; value++)
        {
            assertTrue(seen[value], "the value " + value + " never came out");
        }
        assertEquals(0, kr.random.nextInt(1), "the only possible value is zero");
        assertThrows(IllegalArgumentException.class, () -> kr.random.nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> kr.random.nextInt(-1));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// utils ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void timingSafeEql()
    {
        Kr kr = kr();
        byte[] bytes = kr.random.bytes(64);
        byte[] same = bytes.clone();
        assertTrue(kr.utils.timingSafeEql(bytes, same));
        assertTrue(kr.utils.timingSafeEql(new byte[0], new byte[0]));
        same[63] ^= 0x80;
        assertFalse(kr.utils.timingSafeEql(bytes, same));
        assertFalse(kr.utils.timingSafeEql(bytes, kr.random.bytes(64)));
        assertFalse(kr.utils.timingSafeEql(bytes, new byte[63]), "arrays of different length are never equal");
        assertFalse(kr.utils.timingSafeEql(new byte[63], bytes));
        assertThrows(NullPointerException.class, () -> kr.utils.timingSafeEql(null, bytes));
        assertThrows(NullPointerException.class, () -> kr.utils.timingSafeEql(bytes, null));
        assertThrows(NullPointerException.class, () -> kr.utils.timingSafeEql(null, null));
    }

    @Test
    public void wipeZeroesAndConcat()
    {
        Kr kr = kr();
        byte[] secret = kr.random.bytes(32);
        kr.utils.wipe(secret);
        assertArrayEquals(new byte[32], secret);
        assertArrayEquals(new byte[16], kr.utils.zeroes(16));
        assertArrayEquals(new byte[0], kr.utils.zeroes(0));
        assertThrows(IllegalArgumentException.class, () -> kr.utils.zeroes(-1));
        assertThrows(NullPointerException.class, () -> kr.utils.wipe(null));

        assertArrayEquals(ascii("abc"), kr.utils.concat(ascii("a"), new byte[0], ascii("bc")));
        assertArrayEquals(new byte[0], kr.utils.concat());
        assertThrows(NullPointerException.class, () -> kr.utils.concat(ascii("a"), (byte[]) null));
        assertThrows(NullPointerException.class, () -> kr.utils.concat(ascii("a"), null));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// the backend itself //////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void nameAndProviderAreConsistent()
    {
        Kr kr = kr();
        assertNotNull(kr.name());
        assertFalse(kr.name().isEmpty());
        if (kr instanceof KrJdk)
        {
            assertNull(kr.provider(), "the JDK backend uses the default providers, it pins none");
            assertNull(kr.providerName(), "and it does not claim to use any");
            assertEquals(KrJdk.NAME, kr.name());
        }
        else
        {
            assertNotNull(kr.provider());
            assertEquals(kr.name(), kr.providerName());
            assertEquals(kr.providerName(), kr.provider().getName());
        }
        assertTrue(kr.toString().contains(kr.name()), kr.toString());
    }

    /**
     * The support report must not lie: everything it says is available is
     * available, and everything it says is not available throws.
     */
    @Test
    public void theSupportReportDoesNotLie()
    {
        Kr kr = kr();
        for (HashAlgorithm algorithm : HashAlgorithm.values())
        {
            assertEquals(!kr.supports(algorithm), unsupported(() -> kr.hash.digest(algorithm, HELLO)),
                    "hash " + algorithm);
        }
        for (HmacAlgorithm algorithm : HmacAlgorithm.values())
        {
            assertEquals(!kr.supports(algorithm), unsupported(() -> kr.hmac.hmac(algorithm, new byte[32], HELLO)),
                    "hmac " + algorithm);
        }
        for (AeadAlgorithm algorithm : AeadAlgorithm.values())
        {
            if (!kr.supports(algorithm))
            {
                continue;
            }
            Kr.Aead aead = kr.aeads.of(algorithm, kr.keys.generateSecretKey(algorithm));
            byte[] sealed = aead.seal(HELLO);
            assertArrayEquals(HELLO, aead.open(sealed), algorithm.name());
        }
        for (SignatureAlgorithm algorithm : SignatureAlgorithm.values())
        {
            if (!kr.supports(algorithm))
            {
                continue;
            }
            KeyPair keyPair = kr.keys.generate(algorithm.keyAlgorithm());
            assertTrue(kr.sign.verify(algorithm, keyPair.getPublic(), kr.sign.sign(algorithm, keyPair.getPrivate(), HELLO), HELLO),
                    algorithm.name());
        }
    }

    @Test
    public void unsupportedAlgorithmsExplainThemselves()
    {
        Kr kr = kr();
        for (HashAlgorithm algorithm : HashAlgorithm.values())
        {
            if (kr.supports(algorithm))
            {
                continue;
            }
            UnsupportedAlgorithmException exception = assertThrows(UnsupportedAlgorithmException.class,
                    () -> kr.hash.digest(algorithm, HELLO), algorithm.name());
            assertTrue(exception.getMessage().contains(algorithm.jcaName()), exception.getMessage());
            assertTrue(exception.getMessage().contains(kr.name()), exception.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// helpers /////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private void assumeSupports(Kr kr, HashAlgorithm algorithm)
    {
        assumeTrue(kr.supports(algorithm), algorithm + " is not available in " + kr.name());
    }

    private void assumeSupports(Kr kr, HmacAlgorithm algorithm)
    {
        assumeTrue(kr.supports(algorithm), algorithm + " is not available in " + kr.name());
    }

    private void assumeSupports(Kr kr, AeadAlgorithm algorithm)
    {
        assumeTrue(kr.supports(algorithm), algorithm + " is not available in " + kr.name());
    }

    private void assumeSupports(Kr kr, SignatureAlgorithm algorithm)
    {
        assumeTrue(kr.supports(algorithm), algorithm + " is not available in " + kr.name());
    }

    private void assumeSupports(Kr kr, KeyAlgorithm algorithm)
    {
        assumeTrue(kr.supports(algorithm), algorithm + " is not available in " + kr.name());
    }

    private void assumeSupports(Kr kr, EcCurve curve)
    {
        assumeTrue(kr.supports(curve), curve + " is not available in " + kr.name());
    }

    private void assumeSupports(Kr kr, Kr.Argon2Algorithm algorithm)
    {
        assumeTrue(kr.supports(algorithm), algorithm + " is not available in " + kr.name());
    }

    private void assumeSupports(Kr kr, Kr.PasswordAlgorithm algorithm)
    {
        assumeTrue(kr.supports(algorithm), algorithm + " is not available in " + kr.name());
    }

    private static boolean unsupported(Runnable action)
    {
        try
        {
            action.run();
            return false;
        }
        catch (UnsupportedAlgorithmException ex)
        {
            return true;
        }
    }

    private static byte[] hex(String hex)
    {
        return Hex.decode(hex);
    }

    private static byte[] ascii(String text)
    {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static char[] chars(String text)
    {
        return text.toCharArray();
    }

    /**
     * A byte array of the given size where every byte is the same, the shape
     * of the long keys of the RFC 4231 test vectors.
     *
     * @param value the byte to repeat
     * @param length how many times
     * @return the array
     */
    private static byte[] filled(int value, int length)
    {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    /**
     * The size in bits of the given key, for the algorithms that state it.
     * Ed25519 and X25519 have no parameters to read it from, their size is
     * fixed and their tests use the size of their encodings instead.
     *
     * @param keyPair the key pair
     * @return the size in bits
     */
    private static int keyBits(KeyPair keyPair)
    {
        java.security.PublicKey publicKey = keyPair.getPublic();
        if (publicKey instanceof RSAKey)
        {
            return ((RSAKey) publicKey).getModulus().bitLength();
        }
        if (publicKey instanceof ECKey)
        {
            return ((ECKey) publicKey).getParams().getCurve().getField().getFieldSize();
        }
        throw new IllegalArgumentException("the size of " + publicKey.getAlgorithm() + " keys is not a parameter, it is fixed");
    }
}
