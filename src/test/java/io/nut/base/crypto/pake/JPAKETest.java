/*
 *  JPAKETest.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.crypto.pake;

import io.nut.base.encoding.Hex;
import io.nut.base.util.Strings;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class JPAKETest
{
    @Test
    public void testMain() throws Exception
    {

        char[] passphrase = "correo-tigre-nube-faro".toCharArray();
        String emailA = "alice@example.com";
        String emailB = "bob@example.com";
        byte[] fingerprintA = Hex.decode("4A7B3C9D1E2F8A0B5C6D7E8F9A0B1C2D3E4F5A6B");
        byte[] fingerprintB = Hex.decode("1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6E7F8A9B0C");

        long t0 = System.nanoTime();
        int num = 5;

        for(int i=0;i<num;i++)
        {
            run(passphrase, emailA, emailB, fingerprintA, fingerprintB);
        }
        long t1 = System.nanoTime();
        System.out.printf("avg = %d ms",TimeUnit.NANOSECONDS.toMillis(t1-t0)/num);
    }

    
    static void run(char[] passphrase, String emailA, String emailB, byte[] fingerprintA, byte[] fingerprintB) throws Exception
    {   
        System.out.println(Strings.repeat("=",66));
        System.out.println("  Type: NIST_3072");
        System.out.println(Strings.repeat("=",66));
        System.out.printf("  Passphrase  : %s%n", new String(passphrase));
        System.out.printf("  email_A     : %s%n", emailA);
        System.out.printf("  email_B     : %s%n", emailB);
        System.out.printf("  fingerprint_A : %s%n", Hex.encode(fingerprintA));
        System.out.printf("  fingerprint_B : %s%n", Hex.encode(fingerprintB));
        System.out.println(Strings.repeat("=",66));
        
        String idA = emailA+"="+Hex.encode(fingerprintA);
        String idB = emailB+"="+Hex.encode(fingerprintB);
        long t0 = System.currentTimeMillis();

        JPAKE A = JPAKE.getNIST3072(idA, passphrase.clone(), false);
        JPAKE B = JPAKE.getNIST3072(idB, passphrase.clone(), true);
        
        // ── A>B M1 r1 ────────────────────────────────────────────────────────
        String r1A = A.createRound1PayloadToSend();
        System.out.printf("r1A - A >> B : %s%n", r1A);
        System.out.println();

        // ── B>A M2 r1+r2 ─────────────────────────────────────────────────────
        String r1B = B.createRound1PayloadToSend();
        B.validateRound1PayloadReceived(r1A);
        String r2B = B.createRound2PayloadToSend();
        System.out.printf("r1B - A << B : %s%n", r1B);
        System.out.printf("r2B - A << B : %s%n", r2B);
        System.out.println();
        
        String backup = B.save();
        B = JPAKE.load(backup, passphrase.clone());

        // ── A>B M3 r2+r3 ─────────────────────────────────────────────────────
        A.validateRound1PayloadReceived(r1B);
        String r2A = A.createRound2PayloadToSend();
        A.validateRound2PayloadReceived(r2B);
        BigInteger keyMatA = A.calculateKeyingMaterial();
        String r3A = A.createRound3PayloadToSend(keyMatA);

        System.out.printf("r2A - A >> B : %s%n", r2A);
        System.out.printf("xa: %s%n", keyMatA);
        System.out.printf("r3A - A >> B : %s%n", r3A);
        System.out.println();

        backup = B.save();
        B = JPAKE.load(backup, passphrase.clone());
        
        // ── B>A M4 r3 ─────────────────────────────────────────────────────
        B.validateRound2PayloadReceived(r2A);
        BigInteger keyMatB = B.calculateKeyingMaterial();
        String r3B = B.createRound3PayloadToSend(keyMatB);
        B.validateRound3PayloadReceived(r3A, keyMatB);
        System.out.printf("xb: %s%n", keyMatB);
        System.out.printf("r3B - A << B : %s%n", r3B);
        System.out.println();
                
        A.validateRound3PayloadReceived(r3B, keyMatA);
        
        byte[] sessionA = JPAKE.deriveSessionKey(keyMatA);
        byte[] sessionB = JPAKE.deriveSessionKey(keyMatB);
        assertArrayEquals(sessionA, sessionB, "sessions must be equals");

        long elapsed = System.currentTimeMillis() - t0;

        System.out.printf("payload A: %s%n", JPAKE.getParticipantId(r3A));
        System.out.printf("payload b: %s%n", JPAKE.getParticipantId(r3B));

        System.out.printf("session A: %s%n", Hex.encode(sessionA));
        System.out.printf("session B: %s%n", Hex.encode(sessionB));

        System.out.printf("  Total time (CPU, without net) : %d ms%n", elapsed);
    }
    
    // ── Shared real payloads generated once for all tests ────────────────────
 
    private static String realR1;
    private static String realR2;
    private static String realR3;
 
    /**
     * Runs a full three-round J-PAKE exchange between two in-memory
     * participants so that {@link #realR1}, {@link #realR2} and
     * {@link #realR3} contain genuine wire-format strings.
     */
    @BeforeAll
    static void generatePayloads() throws Exception
    {
        char[] password = "test-password".toCharArray();
 
        JPAKE alice = JPAKE.getNIST2048("Alice", password);
        JPAKE bob   = JPAKE.getNIST2048("Bob",   password);
 
        // Round 1
        String aliceR1 = alice.createRound1PayloadToSend();
        String bobR1   = bob.createRound1PayloadToSend();
        alice.validateRound1PayloadReceived(bobR1);
        bob.validateRound1PayloadReceived(aliceR1);
 
        // Round 2
        String aliceR2 = alice.createRound2PayloadToSend();
        String bobR2   = bob.createRound2PayloadToSend();
        alice.validateRound2PayloadReceived(bobR2);
        bob.validateRound2PayloadReceived(aliceR2);
 
        // Round 3
        java.math.BigInteger aliceKey = alice.calculateKeyingMaterial();
        java.math.BigInteger bobKey   = bob.calculateKeyingMaterial();
        String aliceR3 = alice.createRound3PayloadToSend(aliceKey);
        String bobR3   = bob.createRound3PayloadToSend(bobKey);
        alice.validateRound3PayloadReceived(bobR3, aliceKey);
        bob.validateRound3PayloadReceived(aliceR3, bobKey);
 
        // Keep Alice's payloads as the reference set
        realR1 = aliceR1;
        realR2 = aliceR2;
        realR3 = aliceR3;
    }
 
    // ── findR1 ───────────────────────────────────────────────────────────────
 
    /** The raw payload produced by {@code createRound1PayloadToSend()} must be
     *  returned exactly as-is when passed alone. */
    @Test
    void findR1_exactPayload_returnsItself()
    {
        assertEquals(realR1, JPAKE.findRound1Payload(realR1));
    }
 
    /** The payload embedded in surrounding noise must be extracted correctly. */
    @Test
    void findR1_embeddedInText_extractsPayload()
    {
        String text = "BEGIN_MSG\n" + realR1 + "\nEND_MSG";
        assertEquals(realR1, JPAKE.findRound1Payload(text));
    }
 
    /** A string that starts with "r1/" but has no data field after the id
     *  (missing trailing slash and Base64 content) must not match. */
    @Test
    void findR1_missingDataField_returnsNull()
    {
        assertNull(JPAKE.findRound1Payload("r1/Alice"));
    }
 
    /** "r1/" followed only by the participant id and a trailing slash, but
     *  without any Base64 data field, must not match. */
    @Test
    void findR1_noBase64Field_returnsNull()
    {
        assertNull(JPAKE.findRound1Payload("r1/Alice/"));
    }
 
    /** A valid Round-2 payload must NOT be returned by findR1. */
    @Test
    void findR1_round2Payload_returnsNull()
    {
        assertNull(JPAKE.findRound1Payload(realR2));
    }
 
    /** A valid Round-3 payload must NOT be returned by findR1. */
    @Test
    void findR1_round3Payload_returnsNull()
    {
        assertNull(JPAKE.findRound1Payload(realR3));
    }
 
    /** An empty string must not match. */
    @Test
    void findR1_emptyString_returnsNull()
    {
        assertNull(JPAKE.findRound1Payload(""));
    }
 
    /** Completely unrelated text must not match. */
    @Test
    void findR1_unrelatedText_returnsNull()
    {
        assertNull(JPAKE.findRound1Payload("Hello, this is round one information but not a payload"));
    }
 
    // ── findR2 ───────────────────────────────────────────────────────────────
 
    /** The raw payload produced by {@code createRound2PayloadToSend()} must be
     *  returned exactly as-is when passed alone. */
    @Test
    void findR2_exactPayload_returnsItself()
    {
        assertEquals(realR2, JPAKE.findRound2Payload(realR2));
    }
 
    /** The payload embedded in surrounding noise must be extracted correctly. */
    @Test
    void findR2_embeddedInText_extractsPayload()
    {
        String text = "===START===\t" + realR2 + "\t===END===";
        assertEquals(realR2, JPAKE.findRound2Payload(text));
    }
 
    /** A string that starts with "r2/" but lacks a data field must not match. */
    @Test
    void findR2_missingDataField_returnsNull()
    {
        assertNull(JPAKE.findRound2Payload("r2/Bob"));
    }
 
    /** "r2/" with id and trailing slash only (no Base64 field) must not match. */
    @Test
    void findR2_noBase64Field_returnsNull()
    {
        assertNull(JPAKE.findRound2Payload("r2/Bob/"));
    }
 
    /** A valid Round-1 payload must NOT be returned by findR2. */
    @Test
    void findR2_round1Payload_returnsNull()
    {
        assertNull(JPAKE.findRound2Payload(realR1));
    }
 
    /** A valid Round-3 payload must NOT be returned by findR2. */
    @Test
    void findR2_round3Payload_returnsNull()
    {
        assertNull(JPAKE.findRound2Payload(realR3));
    }
 
    /** An empty string must not match. */
    @Test
    void findR2_emptyString_returnsNull()
    {
        assertNull(JPAKE.findRound2Payload(""));
    }
 
    /** Completely unrelated text must not match. */
    @Test
    void findR2_unrelatedText_returnsNull()
    {
        assertNull(JPAKE.findRound2Payload("round 2 data is pending"));
    }
 
    // ── findR3 ───────────────────────────────────────────────────────────────
 
    /** The raw payload produced by {@code createRound3PayloadToSend()} must be
     *  returned exactly as-is when passed alone. */
    @Test
    void findR3_exactPayload_returnsItself()
    {
        assertEquals(realR3, JPAKE.findRound3Payload(realR3));
    }
 
    /** The payload embedded in surrounding noise must be extracted correctly. */
    @Test
    void findR3_embeddedInText_extractsPayload()
    {
        String text = "[HEADER] some info " + realR3 + " [FOOTER] more info";
        assertEquals(realR3, JPAKE.findRound3Payload(text));
    }
 
    /** A string that starts with "r3/" but lacks a data field must not match. */
    @Test
    void findR3_missingDataField_returnsNull()
    {
        assertNull(JPAKE.findRound3Payload("r3/Alice"));
    }
 
    /** "r3/" with id and trailing slash only (no Base64 field) must not match. */
    @Test
    void findR3_noBase64Field_returnsNull()
    {
        assertNull(JPAKE.findRound3Payload("r3/Alice/"));
    }
 
    /** A valid Round-1 payload must NOT be returned by findR3. */
    @Test
    void findR3_round1Payload_returnsNull()
    {
        assertNull(JPAKE.findRound3Payload(realR1));
    }
 
    /** A valid Round-2 payload must NOT be returned by findR3. */
    @Test
    void findR3_round2Payload_returnsNull()
    {
        assertNull(JPAKE.findRound3Payload(realR2));
    }
 
    /** An empty string must not match. */
    @Test
    void findR3_emptyString_returnsNull()
    {
        assertNull(JPAKE.findRound3Payload(""));
    }
 
    /** Completely unrelated text must not match. */
    @Test
    void findR3_unrelatedText_returnsNull()
    {
        assertNull(JPAKE.findRound3Payload("this is the final round confirmation text"));
    }    
    
    // ── participantId ───────────────────────────────────────────────────────────
 
    @Test
    void participantId_cleanId_returnsSame()
    {
        assertEquals("Alice", JPAKE.getParticipantId(JPAKE.getNIST2048("Alice", PW).createRound1PayloadToSend()));
    }
 
    @Test
    void participantId_containsSemicolon_accepted()
    {
        assertEquals("Alice;Bob", JPAKE.getParticipantId(JPAKE.getNIST2048("Alice;Bob", PW).createRound1PayloadToSend()));
    }
 
    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void participantId_null_throwsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> JPAKE.getNIST2048(null, PW));
    }
 
    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void participantId_emptyString_throwsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> JPAKE.getNIST2048("", PW));
    }
 
    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void participantId_containsFieldSeparator_throwsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> JPAKE.getNIST2048("Alice/Bob", PW));
    }
 
    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void participantId_internalSpace_throwsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> JPAKE.getNIST2048("Alice Bob", PW));
    }
 
    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void participantId_leadingSpace_throwsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> JPAKE.getNIST2048(" Alice", PW));
    }
 
    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void participantId_tab_throwsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> JPAKE.getNIST2048("Alice\tBob", PW));
    }
 
    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void participantId_newline_throwsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> JPAKE.getNIST2048("Alice\nBob", PW));
    }
 
    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    void constructor_slashInId_throwsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> JPAKE.getNIST2048("Alice/Admin", PW));
    }    
    public static final char[] PW = "pw".toCharArray();
}
