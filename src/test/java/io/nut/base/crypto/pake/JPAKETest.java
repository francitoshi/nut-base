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
    
}
