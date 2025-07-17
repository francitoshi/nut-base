/*
 *  KeyStoreManagerTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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

import static io.nut.base.util.Assert.assertTrue;
import java.nio.charset.StandardCharsets;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.security.cert.X509Certificate;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class KeyStoreManagerTest
{

    @Test
    public void testMain() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, Exception
    {
        test(Kripto.getInstance().getKeyStoreManagerJCEKS());
        test(Kripto.getInstance().getKeyStoreManagerBCFKS());
        test(Kripto.getInstance(true).getKeyStoreManagerBCFKS());
        test(Kripto.getInstance().getKeyStoreManagerPKCS12());
    }
    public void test(KeyStoreManager manager) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, Exception
    {
        String secretAlias = "secretAlias";
        char[] secretProtectionPass = "secretProtectionPass".toCharArray();

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey1 = keyGen.generateKey();

        manager.setSecretKey(secretAlias, secretKey1, secretProtectionPass);

        SecretKey secretKey2 = manager.getSecretKey(secretAlias, secretProtectionPass);

        assertEquals(secretKey1, secretKey2);

        String privateAlias = "privateAlias";
        char[] privateProtectionPass = "privateProtectionPass".toCharArray();

        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair0 = keyPairGen.generateKeyPair();
        PrivateKey privateKey1 = keyPair0.getPrivate();
        PublicKey publicKey1 = keyPair0.getPublic();

        X509Certificate[] certificateChain = new X509CertificateBuilerBC().buildCertificateChain(publicKey1, privateKey1, privateAlias);
        manager.setPrivateKey(privateAlias, privateKey1, certificateChain, privateProtectionPass);

        PrivateKey privateKey2 = manager.getPrivateKey(privateAlias, privateProtectionPass);

        assertEquals(privateKey1, privateKey2);

        String publicCertAlias = "publicCertAlias";

        manager.setCertificate(publicCertAlias, certificateChain[0]);

        PublicKey publicKey2 = manager.getCertificatePublicKey(publicCertAlias);

        assertEquals(publicKey1, publicKey2);

        byte[] secretKeyRaw1 = "this is a secreatKey".getBytes(StandardCharsets.UTF_8);
        manager.setSecretKeyRaw(secretAlias, secretKeyRaw1, secretProtectionPass);
        byte[] secretKeyRaw2 = manager.getSecretKeyRaw(secretAlias, secretProtectionPass);
        assertArrayEquals(secretKeyRaw1, secretKeyRaw2);

        byte[] secretKeyRaw3 = "this is a very very very long secret key to test a non standard key that should fail on some KeyStore types".getBytes(StandardCharsets.UTF_8);
        manager.setSecretKeyRaw(secretAlias, secretKeyRaw3, secretProtectionPass);
        byte[] secretKeyRaw4 = manager.getSecretKeyRaw(secretAlias, secretProtectionPass);
        assertArrayEquals(secretKeyRaw3, secretKeyRaw4);

        ////////// PASSPHRASES /////////////////////////////////////////////////
        
        String passphraseAlias = "passphraseAlias";
        char[] passphrase = "this is a passphrase".toCharArray();
        char[] protPassphrase = "entryPassphrase".toCharArray();
        manager.setPassphrase(passphraseAlias, passphrase, protPassphrase);
        char[] passphrase2 = manager.getPassprhase(passphraseAlias, protPassphrase);
        assertArrayEquals(passphrase, passphrase2);

        assertTrue(manager.isModified());
    }

    static final KeyStoreManager.Passphraser PASSPHRASER = new KeyStoreManager.Passphraser()
    {
        @Override
        public char[] get(String seed)
        {
            return ("dummy" + seed).toCharArray();
        }
    };

    @Test
    public void testMain2() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, Exception
    {
        test2(Kripto.getInstance().getKeyStoreManagerJCEKS(PASSPHRASER));
        test2(Kripto.getInstance().getKeyStoreManagerBCFKS(PASSPHRASER));
        test2(Kripto.getInstance(true).getKeyStoreManagerBCFKS(PASSPHRASER));
        test2(Kripto.getInstance().getKeyStoreManagerPKCS12(PASSPHRASER));
    }
    
    public void test2(KeyStoreManager manager) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, Exception
    {
        String secretAlias = "secretAlias";

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey1 = keyGen.generateKey();

        manager.setSecretKey(secretAlias, secretKey1);

        SecretKey secretKey2 = manager.getSecretKey(secretAlias);

        assertEquals(secretKey1, secretKey2);

        String privateAlias = "privateAlias";

        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair0 = keyPairGen.generateKeyPair();
        PrivateKey privateKey1 = keyPair0.getPrivate();
        PublicKey publicKey1 = keyPair0.getPublic();

        X509Certificate[] certificateChain = new X509CertificateBuilerBC().buildCertificateChain(publicKey1, privateKey1, privateAlias);
        manager.setPrivateKey(privateAlias, privateKey1, certificateChain);

        PrivateKey privateKey2 = manager.getPrivateKey(privateAlias);

        assertEquals(privateKey1, privateKey2);

        byte[] secretKeyRaw1 = "this is a secreatKey".getBytes(StandardCharsets.UTF_8);
        manager.setSecretKeyRaw(secretAlias, secretKeyRaw1);
        byte[] secretKeyRaw2 = manager.getSecretKeyRaw(secretAlias);
        Assertions.assertArrayEquals(secretKeyRaw1, secretKeyRaw2);

        byte[] secretKeyRaw3 = "this is a very very very long secret key to test a non standard key that should fail on some KeyStore types".getBytes(StandardCharsets.UTF_8);
        manager.setSecretKeyRaw(secretAlias, secretKeyRaw3);
        byte[] secretKeyRaw4 = manager.getSecretKeyRaw(secretAlias);
        assertArrayEquals(secretKeyRaw3, secretKeyRaw4);

        ////////// PASSPHRASES /////////////////////////////////////////////////
        
        String passphraseAlias = "passphraseAlias";
        char[] passphrase = "this is a passphrase".toCharArray();
        manager.setPassphrase(passphraseAlias, passphrase);
        char[] passphrase2 = manager.getPassprhase(passphraseAlias);
        assertArrayEquals(passphrase, passphrase2);

        assertTrue(manager.isModified());
    }
}
