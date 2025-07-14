/*
 *  KeyStoreManager.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.crypto.spec.SecretKeySpec;

/**
 * Manages operations on a {@link java.security.KeyStore} instance. This class
 * acts as a wrapper to simplify common tasks such as loading, storing, and
 * handling cryptographic keys and certificates.
 */
public class KeyStoreManager
{

    private final KeyStore keyStore;

    /**
     * Constructs a new KeyStoreManager with a given KeyStore instance. It
     * initializes the KeyStore by calling {@code load(null, null)}, preparing
     * it as an empty keystore in memory.
     *
     * @param keyStore The KeyStore instance to manage.
     * @throws RuntimeException if the keystore cannot be initialized.
     */
    public KeyStoreManager(KeyStore keyStore)
    {
        this.keyStore = keyStore;
        try
        {
            // Initializes an empty keystore
            this.keyStore.load(null, null);
        }
        catch (IOException | CertificateException | NoSuchAlgorithmException ex)
        {
            // This should not happen with null arguments, but rethrow as unchecked if it does.
            throw new RuntimeException("Failed to initialize empty keystore", ex);
        }
    }

    /**
     * Deletes the entry identified by the given alias from this keystore.
     *
     * @param alias the alias of the entry to delete.
     * @throws KeyStoreException if the entry cannot be deleted.
     */
    public final void deleteEntry(String alias) throws KeyStoreException
    {
        keyStore.deleteEntry(alias);
    }

    /**
     * Checks if this keystore contains an entry for the given alias.
     *
     * @param alias the alias to check.
     * @return true if the alias exists, false otherwise.
     * @throws KeyStoreException if there is an error accessing the keystore.
     */
    public final boolean containsAlias(String alias) throws KeyStoreException
    {
        return keyStore.containsAlias(alias);
    }

    /**
     * Stores this keystore to the given output stream.
     *
     * @param out the output stream to write the keystore to.
     * @param chars the password to protect the keystore's integrity.
     * @throws KeyStoreException if the keystore has not been initialized.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the appropriate data integrity
     * algorithm cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be stored.
     */
    public final void store(OutputStream out, char[] chars) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStore.store(out, chars);
    }

    /**
     * Stores this keystore to the given file. This is a convenience method that
     * wraps {@link #store(OutputStream, char[])}.
     *
     * @param file the file to write the keystore to.
     * @param chars the password to protect the keystore's integrity.
     * @throws KeyStoreException if the keystore has not been initialized.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the appropriate data integrity
     * algorithm cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be stored.
     */
    public final void store(File file, char[] chars) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            this.store(fos, chars);
        }
    }

    /**
     * Loads the keystore from the given input stream.
     *
     * @param in the input stream to read the keystore from.
     * @param chars the password used to check the integrity of the keystore.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the algorithm used to check the
     * integrity of the keystore cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be loaded.
     */
    public final void load(InputStream in, char[] chars) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStore.load(in, chars);
    }

    /**
     * Loads the keystore from the given file. This is a convenience method that
     * wraps {@link #load(InputStream, char[])}.
     *
     * @param file the file to read the keystore from.
     * @param chars the password used to check the integrity of the keystore.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the algorithm used to check the
     * integrity of the keystore cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be loaded.
     */
    public final void load(File file, char[] chars) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        try (FileInputStream fis = new FileInputStream(file))
        {
            this.load(fis, chars);
        }
    }

    /**
     * Sets a secret key entry in the keystore, protecting it with a password.
     *
     * @param alias the alias to associate with the secret key.
     * @param secretKey the secret key to store.
     * @param entryPassphrase the password to protect the secret key entry.
     * @throws Exception if the entry cannot be set.
     */
    public void setSecretKey(String alias, SecretKey secretKey, char[] entryPassphrase) throws Exception
    {
        KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(entryPassphrase);
        keyStore.setEntry(alias, secretKeyEntry, protectionParam);
    }

    /**
     * Sets a secret key entry in the keystore, protecting it with a password.
     *
     * @param alias the alias to associate with the secret key.
     * @param secretKey the secret key to store.
     * @param entryPassphrase the password to protect the secret key entry.
     * @throws Exception if the entry cannot be set.
     */
    public void setSecretKeyRaw(String alias, byte[] secretKey, char[] entryPassphrase) throws Exception
    {
        SecretKey rawKey = new SecretKeySpec(secretKey, "RAW");
        setSecretKey(alias, rawKey, entryPassphrase);
    }

    /**
     * Retrieves a secret key from the keystore using the given alias and
     * password.
     *
     * @param alias the alias of the secret key entry.
     * @param entryPassphrase the password to decrypt the secret key entry.
     * @return the retrieved {@link SecretKey}, or null if the entry is not
     * found or is not a SecretKeyEntry.
     * @throws Exception if the entry cannot be retrieved.
     */
    public SecretKey getSecretKey(String alias, char[] entryPassphrase) throws Exception
    {
        KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(entryPassphrase);
        KeyStore.Entry entry = keyStore.getEntry(alias, protectionParam);
        if (entry instanceof KeyStore.SecretKeyEntry)
        {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        return null;
    }

    /**
     * Retrieves a secret key from the keystore using the given alias and
     * password.
     *
     * @param alias the alias of the secret key entry.
     * @param entryPassphrase the password to decrypt the secret key entry.
     * @return the retrieved {@link SecretKey}, or null if the entry is not
     * found or is not a SecretKeyEntry.
     * @throws Exception if the entry cannot be retrieved.
     */
    public byte[] getSecretKeyRaw(String alias, char[] entryPassphrase) throws Exception
    {
        SecretKey secretKey = getSecretKey(alias, entryPassphrase);
        return secretKey!=null ? secretKey.getEncoded() : null;
    }

    /**
     * Sets a private key entry, along with its associated certificate chain, in
     * the keystore.
     *
     * @param alias the alias to associate with the private key.
     * @param privateKey the private key to store.
     * @param chain the certificate chain for the corresponding public key.
     * @param entryPassphrase the password to protect the private key entry.
     * @throws Exception if the entry cannot be set.
     */
    public void setPrivateKey(String alias, PrivateKey privateKey, Certificate[] chain, char[] entryPassphrase) throws Exception
    {
        PrivateKeyEntry entry = new PrivateKeyEntry(privateKey, chain);
        ProtectionParameter prot = new PasswordProtection(entryPassphrase);
        keyStore.setEntry(alias, entry, prot);        
    }
    
    /**
     * Retrieves a private key from the keystore.
     *
     * @param alias the alias of the private key entry.
     * @param entryPassphrase the password to decrypt the private key.
     * @return the requested {@link PrivateKey}, or null if the key for the
     * given alias does not exist.
     * @throws Exception if the key cannot be retrieved.
     */
    public PrivateKey getPrivateKey(String alias, char[] entryPassphrase) throws Exception
    {
        return (PrivateKey) keyStore.getKey(alias, entryPassphrase);
    }
    
    /**
     * Assigns the given trusted certificate to the given alias.
     *
     * @param alias the alias of the certificate entry.
     * @param certificate the certificate to be stored.
     * @throws Exception if the certificate entry cannot be set.
     */
    public void setCertificate(String alias, X509Certificate certificate) throws Exception
    {
        keyStore.setCertificateEntry(alias, certificate);
    }

    /**
     * Retrieves the certificate associated with the given alias.
     *
     * @param alias the alias of the certificate entry.
     * @return the {@link Certificate}, or null if the alias does not exist.
     * @throws Exception if the certificate cannot be retrieved.
     */
    public Certificate getCertificate(String alias) throws Exception
    {
        return keyStore.getCertificate(alias);
    }

    /**
     * Retrieves the public key from the certificate associated with the given
     * alias.
     *
     * @param alias the alias of the certificate entry.
     * @return the {@link PublicKey}, or null if the alias does not exist or has
     * no certificate.
     * @throws Exception if the certificate or public key cannot be retrieved.
     */
    public PublicKey getCertificatePublicKey(String alias) throws Exception
    {
        Certificate certificate = keyStore.getCertificate(alias);
        return (certificate != null) ? certificate.getPublicKey() : null;
    }
}
