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
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author franci
 */
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
        try 
        {
            GPG gpg = new GPG().setArmor(true);
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
            
            System.out.println("Desencriptado: " + new String(status.plaintext, "UTF-8"));
            System.out.println("Firmante: " + (status.signer != null ? status.signer : "Ninguno"));
            System.out.println("Firma válida: " + status.validSignature);
            System.out.println("Receptores: " + Arrays.toString(status.recipients));

            // Solo receptores
            String[] recipientList = gpg.getEncryptionRecipients(encryptedSigned, passphrase);
            System.out.println("Receptores (solo lista): " + Arrays.deepToString(recipientList));

            Arrays.sort(status.recipients);
            Arrays.sort(recipientList);
            assertArrayEquals(status.recipients, recipientList);
        } 
        catch (IOException | InterruptedException e) 
        {
            e.printStackTrace();
        }    
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

}
