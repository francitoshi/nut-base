/*
 *  GPGTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GPGTest
{
    private static final boolean DEBUG = false;
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

        // Solo receptores
        String[] recipients1 = gpg.getEncryptionRecipients(encryptedSigned, passphrase);
        System.out.println("Receptores (solo lista): " + Arrays.deepToString(recipients1));

        String[] recipients2 = status.getRecipients();
        Arrays.sort(recipients2);
        Arrays.sort(recipients1);
        assertArrayEquals(recipients2, recipients1);
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

}
