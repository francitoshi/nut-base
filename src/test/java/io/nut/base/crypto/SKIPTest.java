/*
 *  SKIPTest.java
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
package io.nut.base.crypto;

import io.nut.base.time.JavaTime;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.junit.jupiter.api.Test;

/**
 * Test suite for the {@link SKIP} (Shared Key Identity Protocol) class.
 * 
 * <p>This class verifies the protocol's ability to handle secure message exchanges
 * using both the default Key Derivation Function (HKDF) and specialized custom 
 * implementations such as Argon2id.</p>
 * 
 * @author franci
 */
public class SKIPTest
{
    /**
     * Tests the default protocol behavior using the built-in HKDF-based key derivation.
     * 
     * <p>The test simulates a message exchange between two parties (Alice and Bob):
     * <ol>
     *     <li>Alice encrypts a message containing her identity and GPG fingerprint.</li>
     *     <li>Bob decrypts the message and verifies Alice's identity.</li>
     *     <li>Bob responds with his own identity.</li>
     *     <li>Alice decrypts Bob's response.</li>
     *     <li>Both parties securely destroy their local instances of the shared secret.</li>
     * </ol>
     * </p>
     * 
     * @throws Exception If any cryptographic operation fails.
     */
    @Test
    public void testMain() throws Exception
    {
        char[] passphrase = "horse-blue-dog-big".toCharArray();

        Kripto kripto = Kripto.getInstance();
        
        // --- Usage 1: Default KDF (hkdfExpand) ---
        SKIP alice = new SKIP(kripto, passphrase);
        String msg1 = alice.buildMessage(1, "alice@example.com=0FC2FF07DD90113294B0C843FDD18FDBA1CC2773");
        System.out.println();
        System.out.println("msg1 : " + msg1);

        SKIP bob = new SKIP(kripto, passphrase);
        String[] id1 = bob.receiveMessage(1, msg1).split("=");
        System.out.println("Email : " + id1[0]);
        System.out.println("GPG   : " + id1[1]);
        
        String msg2 = bob.buildMessage(2, "bob@example.com=3F7BB578958C8342B4A78E8076AF7B20E01B52F1");
        bob.destroy();

        System.out.println();
        String[] id2 = alice.receiveMessage(3, msg2).split("=");
        System.out.println("msg2 : " + msg2);
        System.out.println("Email : " + id2[0]);
        System.out.println("GPG   : " + id2[1]);
        
        alice.destroy();

        Arrays.fill(passphrase, '\0');
    }

    /**
     * Tests the protocol's extensibility by injecting a custom Argon2id KDF.
     * 
     * <p>Argon2id is a memory-hard function that provides superior resistance 
     * against GPU/ASIC cracking attempts compared to standard HMAC-based KDFs.
     * This test ensures that {@link SKIP} correctly utilizes the externally 
     * provided derivation logic.</p>
     * 
     * @throws Exception If any cryptographic operation fails.
     */
    @Test
    public void testMain2() throws Exception
    {
        char[] passphrase = "horse-blue-dog-big".toCharArray();
        byte[] salt = "salt".getBytes(StandardCharsets.UTF_8);

        Kripto kripto = Kripto.getInstance();
        
        // Define a custom KDF using BouncyCastle's Argon2 implementation
        Function<char[], byte[]> argon2Kdf = pp -> 
        {
            //return ARGON2.rawHash(128, 64*1024, 1, passphrase, "this is a salt for an example".getBytes(StandardCharsets.UTF_8));

            Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13) // Version 1.3
                    .withIterations(3)           // t: iterations
                    .withMemoryAsKB(65536)       // m: memory usage (64 MB)
                    .withParallelism(4)          // p: parallelism/threads
                    .withSalt(salt);

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(builder.build());

            byte[] result = new byte[32]; // Output hash length
            generator.generateBytes(passphrase, result);
            return result; 
        };
        
        // --- Usage 2: Custom KDF (Argon2id) ---
        SKIP alice = new SKIP(kripto, passphrase, argon2Kdf);
        String msg1 = alice.buildMessage(1, "alice@example.com=0FC2FF07DD90113294B0C843FDD18FDBA1CC2773");
        System.out.println();
        System.out.println("msg1 : " + msg1);

        SKIP bob = new SKIP(kripto, passphrase, argon2Kdf);
        String[] id1 = bob.receiveMessage(2, msg1).split("=");
        System.out.println("Email : " + id1[0]);
        System.out.println("GPG   : " + id1[1]);
        
        String msg2 = bob.buildMessage(3, "bob@example.com=3F7BB578958C8342B4A78E8076AF7B20E01B52F1");
        bob.destroy();

        System.out.println();
        String[] id2 = alice.receiveMessage(4, msg2).split("=");
        System.out.println("msg2 : " + msg2);
        System.out.println("Email : " + id2[0]);
        System.out.println("GPG   : " + id2[1]);
        
        alice.destroy();

        Arrays.fill(passphrase, '\0');
    }
}
