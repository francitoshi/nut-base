/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.gpg;

import static io.nut.base.crypto.gpg.GPG.BRAINPOOLP256R1;
import static io.nut.base.crypto.gpg.GPG.BRAINPOOLP384R1;
import static io.nut.base.crypto.gpg.GPG.BRAINPOOLP512R1;
import static io.nut.base.crypto.gpg.GPG.CURVE25519;
import static io.nut.base.crypto.gpg.GPG.DSA2048;
import static io.nut.base.crypto.gpg.GPG.DSA3072;
import static io.nut.base.crypto.gpg.GPG.ELG4096;
import static io.nut.base.crypto.gpg.GPG.NISTP256;
import static io.nut.base.crypto.gpg.GPG.NISTP384;
import static io.nut.base.crypto.gpg.GPG.NISTP521;
import static io.nut.base.crypto.gpg.GPG.RSA1024;
import static io.nut.base.crypto.gpg.GPG.RSA2048;
import static io.nut.base.crypto.gpg.GPG.RSA3072;
import static io.nut.base.crypto.gpg.GPG.RSA4096;
import io.nut.base.encoding.Hex;
import io.nut.base.profile.Profiler;
import io.nut.base.time.JavaTime;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GPGTest
{
    private static final boolean DEBUG = true;
    private static final String EMAIL = "gpg.test@crypto.base.nut.io";
    private static final String PASSPHRASE = "PASSPHRASE";

    public static final String ESCA = GPG.ESCA;
    public static final String E    = GPG.E;
    public static final String ES   = GPG.ES;
    public static final String S    = GPG.S;
    public static final String SCA  = GPG.SCA;
    public static final String CA   = GPG.CA;
    public static final String SC   = GPG.SC;
    
    static final String THIS_IS_THE_COMMENT = "this is the comment";
    
    @Test
    @Order(1)
    public void testGenKey() throws Exception
    {
        GPG gpg = new GPG().setDebug(DEBUG);
        String name = GPG.class.getName()+System.nanoTime();
        
        assertEquals(0, gpg.genKey(RSA4096, SCA, NISTP521, E, name, "1", EMAIL, PASSPHRASE, "4y"));
        assertEquals(0, gpg.genKey(NISTP384, SCA, NISTP256, E, name, "1", EMAIL, PASSPHRASE, "4y"));

        assertEquals(0, gpg.genKey(RSA2048, S, DSA2048, S, name, "3", EMAIL, PASSPHRASE, "4y"));
        assertEquals(0, gpg.genKey(RSA3072, S, DSA3072, S, name, "3", EMAIL, PASSPHRASE, "4y"));
        assertEquals(0, gpg.genKey(RSA4096, S, ELG4096, E, name, "3", EMAIL, PASSPHRASE, "4y"));

//        assertEquals(0, gpg.genKey(ELG3072, E, name, "4", EMAIL, PASSPHRASE, "4y"));

        assertEquals(0, gpg.genKey(NISTP521, S, BRAINPOOLP256R1, E, name, "3", EMAIL, PASSPHRASE, "4y"));
        assertEquals(0, gpg.genKey(NISTP384, S, BRAINPOOLP384R1, E, name, "3", EMAIL, PASSPHRASE, "4y"));
        assertEquals(0, gpg.genKey(NISTP521, S, BRAINPOOLP512R1, E, name, "3", EMAIL, PASSPHRASE, "4y"));

        assertEquals(0, gpg.genKey(CURVE25519, S, CURVE25519, E, name, "3", EMAIL, PASSPHRASE, "4y"));

    }
    
    @Test
    @Order(2)
    public void testEditKeyAddKey() throws Exception
    {
        GPG gpg = new GPG().setDebug(DEBUG);
        String name = GPG.class.getName()+System.nanoTime();
        
        assertEquals(0, gpg.genKey(RSA1024, S, name, "1", EMAIL, "PASSPHRASE", "4y"));
        SecKey[] keys = gpg.getSecKeys(name);
        
        String id = keys[0].main.getFingerprint();

        assertEquals(0, gpg.addKeyRSA(id, 1024, true, true, true, "1y", PASSPHRASE));
        assertEquals(0, gpg.addKeyRSA(id, 1024, true, true, false, "2y", PASSPHRASE));
        assertEquals(0, gpg.addKeyRSA(id, 1024, true, false, true, "3y", PASSPHRASE));
        assertEquals(0, gpg.addKeyRSA(id, 1024, true, false, false, "4y", PASSPHRASE));
        assertEquals(0, gpg.addKeyRSA(id, 1024, false, true, true, "5y", PASSPHRASE));
        assertEquals(0, gpg.addKeyRSA(id, 1024, false, true, false, "6y", PASSPHRASE));
        assertEquals(0, gpg.addKeyRSA(id, 1024, false, false, true, "7y", PASSPHRASE));
        
        assertEquals(0, gpg.addKeyDSA(id, 1024, true, true, "1y", PASSPHRASE));
        assertEquals(0, gpg.addKeyDSA(id, 1024, true, false, "2y", PASSPHRASE));
        assertEquals(0, gpg.addKeyDSA(id, 1024, false, true, "3y", PASSPHRASE));    
        
        assertEquals(0, gpg.addKeyELG(id, 1024, "1y", PASSPHRASE));
        
        assertEquals(0, gpg.addKeyECC(id, true, false, true, 1, "3y", PASSPHRASE));
        assertEquals(0, gpg.addKeyECC(id, true, false, false, 2, "4y", PASSPHRASE));
        assertEquals(0, gpg.addKeyECC(id, false, true, false, 3, "6y", PASSPHRASE));
        assertEquals(0, gpg.addKeyECC(id, false, false, true, 4, "7y", PASSPHRASE));
        
        assertEquals(0, gpg.deleteSecKeys(id));
        
//        assertEquals(0, instance.addKeyECC(id, false, false, true, 4, "7y", null));
    }

    @Test
    @Order(3)
    public void testEncryptAndSign() throws Exception
    {
        GPG gpg = new GPG().setDebug(true).setArmor(true).setEmitVersion(true).setComment(THIS_IS_THE_COMMENT);
        // Datos de ejemplo
        String plaintext = "this is a secret message, for testing.";
        byte[] plaindata = plaintext.getBytes("UTF-8");
        char[] passphrase = PASSPHRASE.toCharArray();

        String signerId = "signer";
        String recipientId1 = "recipient1";
        String recipientId2 = "recipient2";

        assertEquals(0, gpg.genKey(NISTP521, S, NISTP521, E, signerId, "", EMAIL, PASSPHRASE, "4y"));
        assertEquals(0, gpg.genKey(NISTP521, S, NISTP521, E, recipientId1, "", EMAIL, PASSPHRASE, "4y"));
        assertEquals(0, gpg.genKey(NISTP521, S, NISTP521, E, recipientId2, "", EMAIL, PASSPHRASE, "4y"));

        // Cifrar y firmar
        byte[] encryptedSigned = gpg.encryptAndSign(plaindata, signerId, passphrase, recipientId1, recipientId2);
        System.out.println("Cifrado/firmado (base64): " + Base64.getEncoder().encodeToString(encryptedSigned));

        GPG.DecryptStatus status = new GPG.DecryptStatus();

        // Desencriptar y verificar
        byte[] deciphered = gpg.decryptAndVerify(encryptedSigned, passphrase, status);

        assertArrayEquals(plaindata, deciphered);
        assertEquals(THIS_IS_THE_COMMENT, status.comment);
        assertNotNull(status.version);

        System.out.println("Desencriptado: " + new String(deciphered, "UTF-8"));
        System.out.println("Firmante: " + (status.signer != null ? status.signer : "Ninguno"));
        System.out.println("Firma válida: " + status.validSignature);
        System.out.println("Receptores: " + Arrays.toString(status.getRecipients()));
        byte[] sigHash = Base64.getDecoder().decode(status.getSigHash());
        System.out.println("sigHash: " + status.getSigHash()+" => "+Hex.encode(sigHash));
        System.out.println("sigDate: " + status.getSigDate());
        System.out.println("sigTime: " + status.getSigTime());
        
        // Solo receptores
        String[] recipients1 = gpg.getEncryptionRecipients(encryptedSigned, passphrase);
        System.out.println("Receptores (solo lista): " + Arrays.deepToString(recipients1));

        String[] recipients2 = status.getRecipients();
        Arrays.sort(recipients2);
        Arrays.sort(recipients1);
        assertArrayEquals(recipients2, recipients1);
    }
    
    @Test
    @Order(4)
    public void testNullPointersExceptions() throws Exception
    {
        GPG gpg = new GPG().setDebug(true).setArmor(true).setEmitVersion(true).setComment(THIS_IS_THE_COMMENT);

        char[] chars = "hello".toCharArray();
        
        assertThrows(NullPointerException.class, () -> { gpg.decryptAndVerify((byte[])null, chars, null);});
        assertThrows(NullPointerException.class, () -> { gpg.decryptAndVerify((InputStream)null, chars, null);});
    }
    
    @Test
    @Order(99)
    public void testDeleteSecKeys() throws Exception
    {
        GPG gpg = new GPG().setDebug(DEBUG);
        SecKey[] keys = gpg.getSecKeys(EMAIL);
        
        for(SecKey item : keys)
        {
            String id = item.main.getFingerprint();
            assertEquals(0, gpg.deleteSecKeys(id));
        }

        PubKey[] pubs = gpg.getPubKeys(EMAIL);
        
        for(PubKey item : pubs)
        {
            String id = item.main.getFingerprint();
            assertEquals(0, gpg.deletePubKeys(id));
        }
    }

    @Test
    public void testListPackets() throws Exception
    {
        String msg = "-----BEGIN PGP MESSAGE-----\n" +
        "Version: ProtonMail\n" +
        "Comment: this is a comment\n" +
        "\n" +
        "wV4D8VjmbEu9wEESAQdA/g5QciIQyj/yuHLCm8jNHIvpW3/X70yfgfxRbd9B\n" +
        "pi0w22gjR/wlXDxqhqLIymPuEaKRR36AFflZxfeelO7cEdEVCtOwmiDCPusD\n" +
        "Fr07mxFK0sCEAZzHWjaEQXDhlgaoHRFBqH9RVJsfBK3gV6KgHySdKCZhQGs2\n" +
        "p8evtPTmT6HXoAu0CXzXKsTjUJf7oQcUnGZzsZfrq2hs4es6iPqChvLe+pEx\n" +
        "bjqA5Eu+23/+ctYvL63FQpKf2QgqmjH67oTg8KLSxJGrnUMdUbtHZW7GJuZ/\n" +
        "d7tgS19POj1R4LMJNQJipFFR9rcjrUiXHW3aCHv8w/H+3ANVnL88A4/nDG8N\n" +
        "sFyHm1Ft4zf58p33oeHNC9LLejkvD8+x/s5byEq6zJcl6ONoXErLqp2Picn7\n" +
        "Z9iyESMMAZjjOVvEQDdsyERsDENf3R/T3RGLHrU/doLrWZVZnZCmpQZc8ofC\n" +
        "VDyW8HK5EH3aWFDzfarKNvLYg2qrmYR6iOaRDI7XbfDxO/cli/Xnj1W5a85y\n" +
        "3z3InYbDyvSkkVq14fR1Bwf6\n" +
        "=9T15\n" +
        "-----END PGP MESSAGE-----\n";        
        
        byte[] cipherdata = msg.getBytes(StandardCharsets.UTF_8);
        char[] passphrase = null;
        
        GPG instance = new GPG().setDebug(DEBUG);
        GPG.PacketsInfo result = instance.listPackets(cipherdata, passphrase);
        
        assertEquals("ProtonMail", result.version);
        assertEquals("this is a comment", result.comment);
  
        if(DEBUG)
        {
            System.out.println(result);
        }

    }
    @Test
    public void testListPackets2() throws Exception
    {
        GPG gpg = new GPG().setDebug(DEBUG).setArmor(true).setEmitVersion(true).setComment(THIS_IS_THE_COMMENT);

        gpg.genKey(NISTP521, SCA, NISTP521, E, "alice", "1", "alice@gpgtest.io", PASSPHRASE, "4y");
        gpg.genKey(NISTP521, SCA, NISTP521, E, "bob", "1", "bob@gpgtest.io", PASSPHRASE, "4y");
        
        byte[] plaintext = "hello world!!!".getBytes(StandardCharsets.UTF_8);
        
        byte[] a = gpg.encryptAndSign(plaintext, "alice@gpgtest.io", PASSPHRASE.toCharArray(), "bob@gpgtest.io");
        byte[] b = gpg.encryptAndSign(plaintext, "bob@gpgtest.io", PASSPHRASE.toCharArray(), "alice@gpgtest.io");
        
        System.out.println(new String(a));
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(new String(b));
        System.out.println("--------------------------------------------------------------------------------");

        GPG.PacketsInfo r1 = gpg.listPackets(a, null);
        GPG.PacketsInfo r2 = gpg.listPackets(b, null);

        System.out.println(r1);
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(r2);
                
        GPG.PacketsInfo r3 = gpg.listPackets(a, PASSPHRASE.toCharArray());
        GPG.PacketsInfo r4 = gpg.listPackets(b, PASSPHRASE.toCharArray());

        System.out.println(r3);
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(r4);
        
        assertEquals(THIS_IS_THE_COMMENT, r4.comment);
        assertNotNull(r4.version);
        
        
        SecKey[] secs = gpg.getSecKeys("alice", "bob");
        
        for(SecKey sk : secs)
        {
            gpg.deleteSecAndPubKeys(sk.main.getFingerprint());
        }
    }

    /**
     * Test of refreshKeys method, of class GPG.
     */
    @Test
    public void testRefreshKeys() throws Exception
    {
        GPG instance = new GPG();
        int result = instance.refreshKeys();
        assertEquals(0, result);
    }


    // -------------------------------------------------------------------------
    // isValidKeyId
    // -------------------------------------------------------------------------
 
    @Nested
    @DisplayName("isValidShortKeyId")
    class IsValidGpgShortKeyId
    {
        @ParameterizedTest(name = "null or empty -> false: [{0}]")
        @NullAndEmptySource
        @DisplayName("returns false for null or empty input")
        void returnsFalseForNullOrEmpty(String input)
        {
            assertFalse(GPG.isValidShortKeyId(input));
        }
 
        @ParameterizedTest(name = "valid short key ID (8 hex): [{0}]")
        @ValueSource(strings = 
        {
            "DEADBEEF",
            "deadbeef",
            "DeAdBeEf",
            "00000000",
            "FFFFFFFF",
            "12345678",
            "ABCDEF01"
        })
        @DisplayName("returns true for valid short key IDs (8 hex chars)")
        void returnsTrueForValidShortKeyId(String input)
        {
            assertTrue(GPG.isValidShortKeyId(input));
        }
 
        @ParameterizedTest(name = "valid short key ID with 0x prefix: [{0}]")
        @ValueSource(strings = {
            "0xDEADBEEF",
            "0xdeadbeef",
            "0xDeAdBeEf",
            "0x00000000",
            "0xFFFFFFFF"
        })
        @DisplayName("returns true for valid short key IDs with '0x' prefix")
        void returnsTrueForValidShortKeyIdWithPrefix(String input)
        {
            assertTrue(GPG.isValidShortKeyId(input));
        }
 
        @ParameterizedTest(name = "invalid length: [{0}]")
        @ValueSource(strings = {
            "DEAD",           // 4 chars – too short
            "DEADBEE",        // 7 chars – one short
            "DEADBEEF0",      // 9 chars – one over short
            "DEADBEEF0123456",  // 15 chars – one short of long
            "DEADBEEF012345678" // 17 chars – one over long
        })
        @DisplayName("returns false for key IDs with invalid length")
        void returnsFalseForInvalidLength(String input)
        {
            assertFalse(GPG.isValidShortKeyId(input));
        }
 
        @ParameterizedTest(name = "invalid characters: [{0}]")
        @ValueSource(strings = {
            "DEADBEEG",       // 'G' is not hex
            "DEADBEE!",       // special char
            "DEAD BEEF",      // space inside
            "DEADBEEF\t",     // tab character
            "ZZZZZZZZ",
            "--------"
        })
        @DisplayName("returns false when input contains non-hex characters")
        void returnsFalseForNonHexCharacters(String input)
        {
            assertFalse(GPG.isValidShortKeyId(input), input);
        }
 
        @ParameterizedTest(name = "fingerprint-length strings are rejected: [{0}]")
        @ValueSource(strings = {
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E",  // 40 chars (v4 fingerprint)
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E1122334455667788990AABB" // 64 chars (v5)
        })
        @DisplayName("returns false for fingerprint-length inputs")
        void returnsFalseForFingerprintLengthInput(String input)
        {
            assertFalse(GPG.isValidShortKeyId(input));
        }
    }
 
    // -------------------------------------------------------------------------
    // isValidLongKeyId
    // -------------------------------------------------------------------------
 
    @Nested
    @DisplayName("isValidLongKeyId")
    class IsValidGpgLongKeyId
    {
        @ParameterizedTest(name = "null or empty -> false: [{0}]")
        @NullAndEmptySource
        @DisplayName("returns false for null or empty input")
        void returnsFalseForNullOrEmpty(String input)
        {
            assertFalse(GPG.isValidLongKeyId(input));
        }
 
        @ParameterizedTest(name = "valid short key ID (8 hex): [{0}]")
        @ValueSource(strings = 
        {
            "DEADBEEF",
            "deadbeef",
            "DeAdBeEf",
            "00000000",
            "FFFFFFFF",
            "12345678",
            "ABCDEF01"
        })
        @DisplayName("returns false for valid short key IDs (8 hex chars)")
        void returnsFalseForValidShortKeyId(String input)
        {
            assertFalse(GPG.isValidLongKeyId(input));
        }
 
        @ParameterizedTest(name = "valid short key ID with 0x prefix: [{0}]")
        @ValueSource(strings = {
            "0xDEADBEEF",
            "0xdeadbeef",
            "0xDeAdBeEf",
            "0x00000000",
            "0xFFFFFFFF"
        })
        @DisplayName("returns false for valid short key IDs with '0x' prefix")
        void returnsFalseForValidShortKeyIdWithPrefix(String input)
        {
            assertFalse(GPG.isValidLongKeyId(input));
        }
 
        @ParameterizedTest(name = "valid long key ID (16 hex): [{0}]")
        @ValueSource(strings = {
            "DEADBEEF01234567",
            "deadbeef01234567",
            "DeAdBeEf01234567",
            "0000000000000000",
            "FFFFFFFFFFFFFFFF",
            "ABCDEF0123456789"
        })
        @DisplayName("returns true for valid long key IDs (16 hex chars)")
        void returnsTrueForValidLongKeyId(String input)
        {
            assertTrue(GPG.isValidLongKeyId(input));
        }
 
        @ParameterizedTest(name = "valid long key ID with 0x prefix: [{0}]")
        @ValueSource(strings = {
            "0xDEADBEEF01234567",
            "0xdeadbeef01234567",
            "0x0000000000000000",
            "0xFFFFFFFFFFFFFFFF"
        })
        @DisplayName("returns true for valid long key IDs with '0x' prefix")
        void returnsTrueForValidLongKeyIdWithPrefix(String input)
        {
            assertTrue(GPG.isValidLongKeyId(input));
        }
 
        @ParameterizedTest(name = "invalid length: [{0}]")
        @ValueSource(strings = {
            "DEAD",           // 4 chars – too short
            "DEADBEE",        // 7 chars – one short
            "DEADBEEF0",      // 9 chars – one over short
            "DEADBEEF0123456",  // 15 chars – one short of long
            "DEADBEEF012345678" // 17 chars – one over long
        })
        @DisplayName("returns false for key IDs with invalid length")
        void returnsFalseForInvalidLength(String input)
        {
            assertFalse(GPG.isValidLongKeyId(input));
        }
 
        @ParameterizedTest(name = "invalid characters: [{0}]")
        @ValueSource(strings = {
            "DEADBEEG",       // 'G' is not hex
            "DEADBEE!",       // special char
            "DEAD BEEF",      // space inside
            "DEADBEEF\t",     // tab character
            "ZZZZZZZZ",
            "--------"
        })
        @DisplayName("returns false when input contains non-hex characters")
        void returnsFalseForNonHexCharacters(String input)
        {
            assertFalse(GPG.isValidLongKeyId(input), input);
        }
 
        @ParameterizedTest(name = "fingerprint-length strings are rejected: [{0}]")
        @ValueSource(strings = 
        {
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E",  // 40 chars (v4 fingerprint)
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E1122334455667788990AABB" // 64 chars (v5)
        })
        @DisplayName("returns false for fingerprint-length inputs")
        void returnsFalseForFingerprintLengthInput(String input)
        {
            assertFalse(GPG.isValidLongKeyId(input));
        }
    }
 
    // -------------------------------------------------------------------------
    // isValidFingerprint
    // -------------------------------------------------------------------------
 
    @Nested
    @DisplayName("isValidFingerprint")
    class IsValidGpgFingerprint
    {
        @ParameterizedTest(name = "null or empty -> false: [{0}]")
        @NullAndEmptySource
        @DisplayName("returns false for null or empty input")
        void returnsFalseForNullOrEmpty(String input)
        {
            assertFalse(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "valid v4 fingerprint (40 hex): [{0}]")
        @ValueSource(strings = {
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E",
            "d4c5e60f0a4a4b3e2f3a1b7c9d8e5f6a7b8c9d0e",
            "D4c5E60f0A4a4B3e2F3a1B7c9D8e5F6a7B8c9D0e",
            "0000000000000000000000000000000000000000",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        })
        @DisplayName("returns true for valid v4 fingerprints (40 hex chars)")
        void returnsTrueForValidV4Fingerprint(String input)
        {
            assertTrue(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "valid v4 fingerprint with 0x prefix: [{0}]")
        @ValueSource(strings = {
            "0xD4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E",
            "0xd4c5e60f0a4a4b3e2f3a1b7c9d8e5f6a7b8c9d0e",
            "0x0000000000000000000000000000000000000000"
        })
        @DisplayName("returns true for valid v4 fingerprints with '0x' prefix")
        void returnsTrueForValidV4FingerprintWithPrefix(String input)
        {
            assertTrue(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "valid v4 fingerprint in spaced format: [{0}]")
        @ValueSource(strings = {
            "D4C5 E60F 0A4A 4B3E 2F3A  1B7C 9D8E 5F6A 7B8C 9D0E",
            "d4c5 e60f 0a4a 4b3e 2f3a  1b7c 9d8e 5f6a 7b8c 9d0e",
            "D4C5E60F 0A4A4B3E 2F3A1B7C 9D8E5F6A 7B8C9D0E"
        })
        @DisplayName("returns true for valid v4 fingerprints in spaced display format")
        void returnsTrueForValidV4FingerprintSpaced(String input)
        {
            assertTrue(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "valid v5 fingerprint (64 hex): [{0}]")
        @ValueSource(strings = {
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E1A2B3C4D5E6F7A8B9C0D1E2F",
            "d4c5e60f0a4a4b3e2f3a1b7c9d8e5f6a7b8c9d0e1a2b3c4d5e6f7a8b9c0d1e2f",
            "0000000000000000000000000000000000000000000000000000000000000000",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        })
        @DisplayName("returns true for valid v5 fingerprints (64 hex chars)")
        void returnsTrueForValidV5Fingerprint(String input)
        {
            assertTrue(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "valid v5 fingerprint with 0x prefix: [{0}]")
        @ValueSource(strings = {
            "0xD4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E1A2B3C4D5E6F7A8B9C0D1E2F",
            "0x0000000000000000000000000000000000000000000000000000000000000000"
        })
        @DisplayName("returns true for valid v5 fingerprints with '0x' prefix")
        void returnsTrueForValidV5FingerprintWithPrefix(String input)
        {
            assertTrue(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "valid v5 fingerprint in spaced format: [{0}]")
        @ValueSource(strings = {
            "D4C5E60F 0A4A4B3E 2F3A1B7C 9D8E5F6A 7B8C9D0E 1A2B3C4D 5E6F7A8B 9C0D1E2F"
        })
        @DisplayName("returns true for valid v5 fingerprints in spaced display format")
        void returnsTrueForValidV5FingerprintSpaced(String input)
        {
            assertTrue(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "invalid length: [{0}]")
        @ValueSource(strings = {
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D",    // 39 chars
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E0",  // 41 chars
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E1A2B3C4D5E6F7A8B9C0D1E2",  // 63 chars
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9D0E1A2B3C4D5E6F7A8B9C0D1E2F0" // 65 chars
        })
        @DisplayName("returns false for fingerprints with invalid length")
        void returnsFalseForInvalidLength(String input)
        {
            assertFalse(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "invalid characters: [{0}]")
        @ValueSource(strings = {
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9G0",   // 'G' not hex
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9!0",   // '!' special char
            "D4C5E60F0A4A4B3E2F3A1B7C9D8E5F6A7B8C9\t0",  // tab
            "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"    // all invalid
        })
        @DisplayName("returns false when input contains non-hex characters")
        void returnsFalseForNonHexCharacters(String input)
        {
            assertFalse(GPG.isValidFingerprint(input));
        }
 
        @ParameterizedTest(name = "key-ID-length strings are rejected: [{0}]")
        @ValueSource(strings = {
            "DEADBEEF",           // 8 chars (short key ID)
            "DEADBEEF01234567"    // 16 chars (long key ID)
        })
        @DisplayName("returns false for key-ID-length inputs")
        void returnsFalseForKeyIdLengthInput(String input)
        {
            assertFalse(GPG.isValidFingerprint(input));
        }
    }    
    
    private static boolean gpgAvailable;

    @BeforeAll
    static void detectGpg()
    {
        gpgAvailable = new GPG().isInstalled();
    }
    
    @Nested
    @DisplayName("Tests requiring gpg binary")
    class RequiresGpg
    {

        // --- getVersion() --------------------------------------------------

        @Test
        @DisplayName("getVersion() returns a non-null, non-blank string")
        void getVersion_returnsNonBlank() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            String version = new GPG().getVersion();

            assertNotNull(version, "version must not be null");
            assertFalse(version.trim().isEmpty(), "version must not be blank");
        }

        @Test
        @DisplayName("getVersion() output starts with 'gpg'")
        void getVersion_startsWithGpg() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            String version = new GPG().getVersion();

            assertNotNull(version);
            assertTrue(version.toLowerCase().startsWith("gpg"),
                    "Expected first line to start with 'gpg', got: " + version);
        }

        @Test
        @DisplayName("getVersion() output contains a version number pattern (x.y.z)")
        void getVersion_containsVersionNumber() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            String version = new GPG().getVersion();

            assertNotNull(version);
            assertTrue(version.matches(".*\\d+\\.\\d+.*"),
                    "Expected a version number in output, got: " + version);
        }

        // --- enarmor() -----------------------------------------------------

        @Test
        @DisplayName("enarmor() wraps input in an ASCII-armor header and footer")
        void enarmor_producesArmorHeaderAndFooter() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            String result = new GPG().enarmor("hello gpg");

            assertNotNull(result);
            assertTrue(result.contains("-----BEGIN"),
                    "Armored output must contain '-----BEGIN'");
            assertTrue(result.contains("-----END"),
                    "Armored output must contain '-----END'");
        }

        @Test
        @DisplayName("enarmor() output contains a Base64-like body (alphanumeric/+/=)")
        void enarmor_bodyIsBase64Like() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            String result = new GPG().enarmor("test data 1234");

            assertNotNull(result);
            // At least one line that looks like Base64
            boolean hasBase64Line = false;
            for (String line : result.split("\n"))
            {
                if (line.matches("[A-Za-z0-9+/]+=*") && line.length() > 4)
                {
                    hasBase64Line = true;
                    break;
                }
            }
            assertTrue(hasBase64Line, "Expected at least one Base64 line in armored output");
        }

        @Test
        @DisplayName("enarmor() throws NullPointerException for null input")
        void enarmor_throwsOnNull()
        {
            assertThrows(NullPointerException.class, () -> new GPG().enarmor(null));
        }

        @Test
        @DisplayName("enarmor() handles empty string without exception")
        void enarmor_emptyString() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            String result = new GPG().enarmor("");

            // gpg may produce an armor block even for empty input; just must not throw
            assertNotNull(result);
        }

        // --- dearmor() -----------------------------------------------------

        @Test
        @DisplayName("dearmor() throws NullPointerException for null input")
        void dearmor_throwsOnNull()
        {
            assertThrows(NullPointerException.class, () -> new GPG().dearmor(null));
        }

        @Test
        @DisplayName("dearmor() inverts enarmor() and recovers original bytes")
        void dearmor_roundTrip() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            String original = "round-trip test payload";
            GPG gpg = new GPG();

            String armored   = gpg.enarmor(original);
            String recovered = gpg.dearmor(armored);

            assertNotNull(recovered);
            // The raw bytes of the original must appear inside the de-armored output
            // (gpg --dearmor returns binary; reading as UTF-8 may add trailing nulls)
            assertTrue(recovered.contains(original),
                    "De-armored output must contain the original string");
        }

        @Test
        @DisplayName("enarmor() then dearmor() preserves multi-line content")
        void enarmor_dearmor_multiLine() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            String original = "line1\nline2\nline3";
            GPG gpg = new GPG();

            String armored   = gpg.enarmor(original);
            String recovered = gpg.dearmor(armored);

            assertNotNull(recovered);
            assertTrue(recovered.contains("line1"), "Must contain 'line1'");
            assertTrue(recovered.contains("line2"), "Must contain 'line2'");
            assertTrue(recovered.contains("line3"), "Must contain 'line3'");
        }

        // --- cross-method --------------------------------------------------

        @Test
        @DisplayName("getVersion() is consistent across two consecutive calls")
        void getVersion_isConsistent() throws IOException, InterruptedException
        {
            Assumptions.assumeTrue(gpgAvailable, "gpg not found – skipping");

            GPG gpg = new GPG();
            String v1 = gpg.getVersion();
            String v2 = gpg.getVersion();

            assertEquals(v1, v2, "Repeated calls to getVersion() must return the same value");
        }
    }

    // -----------------------------------------------------------------------
    // Tests that do NOT need gpg (pure Java / null-guard logic)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Pure-Java / null-guard tests (no gpg required)")
    class NoGpgRequired
    {

        @Test
        @DisplayName("enarmor(null) throws NullPointerException regardless of gpg availability")
        void enarmor_null_throwsNPE()
        {
            assertThrows(NullPointerException.class, () -> new GPG().enarmor(null));
        }

        @Test
        @DisplayName("dearmor(null) throws NullPointerException regardless of gpg availability")
        void dearmor_null_throwsNPE()
        {
            assertThrows(NullPointerException.class, () -> new GPG().dearmor(null));
        }
    }
    
}
