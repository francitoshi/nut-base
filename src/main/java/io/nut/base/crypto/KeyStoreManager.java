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

import io.nut.base.util.Byter;
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
    public interface Passphraser
    {
        char[] get(String seed);
    }
    private static final Passphraser NULL_PASSPHRASER = new Passphraser()
    {
        @Override
        public char[] get(String seed)
        {
            throw new ProviderException("no passphraser provided.");
        };
    };
    
    private final KeyStore keyStore;
    private final Passphraser passphraser;
    private volatile boolean modified;

    /**
     * Constructs a new KeyStoreManager with a given KeyStore instance. It
     * initializes the KeyStore by calling {@code load(null, null)}, preparing
     * it as an empty keystore in memory.
     *
     * @param keyStore The KeyStore instance to manage.
     * @param passphraser the passphraser that will get the passphrase
     * @throws RuntimeException if the keystore cannot be initialized.
     */
    public KeyStoreManager(KeyStore keyStore, Passphraser passphraser)
    {
        this.keyStore = keyStore;
        this.passphraser = passphraser;
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
     * Constructs a new KeyStoreManager with a given KeyStore instance. It
     * initializes the KeyStore by calling {@code load(null, null)}, preparing
     * it as an empty keystore in memory.
     *
     * @param keyStore The KeyStore instance to manage.
     * @throws RuntimeException if the keystore cannot be initialized.
     */
    public KeyStoreManager(KeyStore keyStore)
    {
        this(keyStore,NULL_PASSPHRASER);
    }

    public boolean isModified()
    {
        return modified;
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
        this.modified = true;
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
     * @param password the password to protect the keystore's integrity.
     * @throws KeyStoreException if the keystore has not been initialized.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the appropriate data integrity
     * algorithm cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be stored.
     */
    public final void store(OutputStream out, char[] password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStore.store(out, password);
        this.modified = false;
    }
    /**
     * Stores this keystore to the given output stream.
     *
     * @param out the output stream to write the keystore to.
     * @throws KeyStoreException if the keystore has not been initialized.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the appropriate data integrity
     * algorithm cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be stored.
     */
    public final void store(OutputStream out) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        this.store(out, passphraser.get(""));
    }

    /**
     * Stores this keystore to the given file. This is a convenience method that
     * wraps {@link #store(OutputStream, char[])}.
     *
     * @param file the file to write the keystore to.
     * @param password the password to protect the keystore's integrity.
     * @throws KeyStoreException if the keystore has not been initialized.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the appropriate data integrity
     * algorithm cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be stored.
     */
    public final void store(File file, char[] password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            this.store(fos, password);
            this.modified = false;
        }
    }
    /**
     * Stores this keystore to the given file. This is a convenience method that
     * wraps {@link #store(OutputStream, char[])}.
     *
     * @param file the file to write the keystore to.
     * @throws KeyStoreException if the keystore has not been initialized.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the appropriate data integrity
     * algorithm cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be stored.
     */
    public final void store(File file) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        this.store(file, passphraser.get("")); 
    }

    /**
     * Loads the keystore from the given input stream.
     *
     * @param in the input stream to read the keystore from.
     * @param password the password used to check the integrity of the keystore, the password used to unlock the keystore, or null
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the algorithm used to check the
     * integrity of the keystore cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be loaded.
     */
    public final void load(InputStream in, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStore.load(in, password);
        this.modified = false;
    }

    /**
     * Loads the keystore from the given input stream.
     *
     * @param in the input stream to read the keystore from.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the algorithm used to check the
     * integrity of the keystore cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be loaded.
     */
    public final void load(InputStream in) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        this.load(in, passphraser.get(""));
    }

    /**
     * Loads the keystore from the given file. This is a convenience method that
     * wraps {@link #load(InputStream, char[])}.
     *
     * @param file the file to read the keystore from.
     * @param password the password used to check the integrity of the keystore, the password used to unlock the keystore, or null
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the algorithm used to check the
     * integrity of the keystore cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be loaded.
     */
    public final void load(File file, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        try (FileInputStream fis = new FileInputStream(file))
        {
            this.load(fis, password);
            this.modified = false;
        }
    }
    /**
     * Loads the keystore from the given file. This is a convenience method that
     * wraps {@link #load(InputStream, char[])}.
     *
     * @param file the file to read the keystore from.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the algorithm used to check the
     * integrity of the keystore cannot be found.
     * @throws CertificateException if any of the certificates in the keystore
     * could not be loaded.
     */
    public final void load(File file) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        this.load(file, passphraser.get(""));
    }

    /**
     * Sets a secret key entry in the keystore, protecting it with a password.
     *
     * @param alias the alias to associate with the secret key.
     * @param secretKey the secret key to store.
     * @param protPass the password to protect the secret key entry.
     * @throws Exception if the entry cannot be set.
     */
    public void setSecretKey(String alias, SecretKey secretKey, char[] protPass) throws Exception
    {
        KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(protPass);
        keyStore.setEntry(alias, secretKeyEntry, protectionParam);
        this.modified = true;
    }

    /**
     * Sets a secret key entry in the keystore, protecting it with a password.
     *
     * @param alias the alias to associate with the secret key.
     * @param secretKey the secret key to store.
     * @throws Exception if the entry cannot be set.
     */
    public void setSecretKey(String alias, SecretKey secretKey) throws Exception
    {
        this.setSecretKey(alias, secretKey, passphraser.get(alias));
    }

    /**
     * Sets a secret key entry in the keystore, protecting it with a password.
     *
     * @param alias the alias to associate with the secret key.
     * @param secretKey the secret key to store.
     * @param protPass the password to protect the secret key entry.
     * @throws Exception if the entry cannot be set.
     */
    public void setSecretKeyRaw(String alias, byte[] secretKey, char[] protPass) throws Exception
    {
        SecretKey rawKey = new SecretKeySpec(secretKey, "RAW");
        setSecretKey(alias, rawKey, protPass);
        this.modified = true;
    }

    /**
     * Sets a secret key entry in the keystore, protecting it with a password.
     *
     * @param alias the alias to associate with the secret key.
     * @param secretKey the secret key to store.
     * @throws Exception if the entry cannot be set.
     */
    public void setSecretKeyRaw(String alias, byte[] secretKey) throws Exception
    {
        this.setSecretKeyRaw(alias, secretKey, passphraser.get(alias));
    }

    /**
     * Sets a secret key entry in the keystore, protecting it with a password.
     *
     * @param alias the alias to associate with the secret key.
     * @param passphrase the passphrase to store.
     * @param protPass the password to protect the secret key entry.
     * @throws Exception if the entry cannot be set.
     */
    public void setPassphrase(String alias, char[] passphrase, char[] protPass) throws Exception
    {
        byte[] bytes = Byter.bytes(passphrase);
        this.setSecretKeyRaw(alias, bytes, protPass);
    }

    /**
     * Sets a secret key entry in the keystore, protecting it with a password.
     *
     * @param alias the alias to associate with the secret key.
     * @param passphrase the passphrase to store.
     * @throws Exception if the entry cannot be set.
     */
    public void setPassphrase(String alias, char[] passphrase) throws Exception
    {
        this.setPassphrase(alias, passphrase, passphraser.get(alias));
    }

    /**
     * Retrieves a secret key from the keystore using the given alias and password.
     *
     * @param alias the alias of the secret key entry.
     * @param protPass the password to decrypt the secret key entry.
     * @return the retrieved {@link SecretKey}, or null if the entry is not
     * found or is not a SecretKeyEntry.
     * @throws Exception if the entry cannot be retrieved.
     */
    public SecretKey getSecretKey(String alias, char[] protPass) throws Exception
    {
        KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(protPass);
        KeyStore.Entry entry = keyStore.getEntry(alias, protectionParam);
        if (entry instanceof KeyStore.SecretKeyEntry)
        {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        return null;
    }

    /**
     * Retrieves a secret key from the keystore using the given alias and password.
     *
     * @param alias the alias of the secret key entry.
     * @return the retrieved {@link SecretKey}, or null if the entry is not
     * found or is not a SecretKeyEntry.
     * @throws Exception if the entry cannot be retrieved.
     */
    public SecretKey getSecretKey(String alias) throws Exception
    {
        return this.getSecretKey(alias, passphraser.get(alias)); 
    }

    /**
     * Retrieves a secret key from the keystore using the given alias and password.
     *
     * @param alias the alias of the secret key entry.
     * @param protPass the password to decrypt the secret key entry.
     * @return the retrieved {@link SecretKey}, or null if the entry is not
     * found or is not a SecretKeyEntry.
     * @throws Exception if the entry cannot be retrieved.
     */
    public byte[] getSecretKeyRaw(String alias, char[] protPass) throws Exception
    {
        SecretKey secretKey = getSecretKey(alias, protPass);
        return secretKey!=null ? secretKey.getEncoded() : null;
    }

    /**
     * Retrieves a secret key from the keystore using the given alias and password.
     *
     * @param alias the alias of the secret key entry.
     * @return the retrieved {@link SecretKey}, or null if the entry is not
     * found or is not a SecretKeyEntry.
     * @throws Exception if the entry cannot be retrieved.
     */
    public byte[] getSecretKeyRaw(String alias) throws Exception
    {
        return this.getSecretKeyRaw(alias, passphraser.get(alias)); 
    }

    /**
     * Retrieves a passphrase from the keystore using the given alias and password.
     *
     * @param alias the alias of the secret key entry.
     * @param protPass the password to decrypt the secret key entry.
     * @return the retrieved {@link SecretKey}, or null if the entry is not
     * found or is not a SecretKeyEntry.
     * @throws Exception if the entry cannot be retrieved.
     */
    public char[] getPassprhase(String alias, char[] protPass) throws Exception
    {
        byte[] bytes = getSecretKeyRaw(alias, protPass);
        return bytes!=null ? Byter.chars(bytes) : null;
    }

    /**
     * Retrieves a passphrase from the keystore using the given alias and password.
     *
     * @param alias the alias of the secret key entry.
     * @return the retrieved {@link SecretKey}, or null if the entry is not
     * found or is not a SecretKeyEntry.
     * @throws Exception if the entry cannot be retrieved.
     */
    public char[] getPassprhase(String alias) throws Exception
    {
        return this.getPassprhase(alias, passphraser.get(alias)); 
    }

    /**
     * Sets a private key entry, along with its associated certificate chain, in
     * the keystore.
     *
     * @param alias the alias to associate with the private key.
     * @param privateKey the private key to store.
     * @param chain the certificate chain for the corresponding public key.
     * @param protPass the password to protect the private key entry.
     * @throws Exception if the entry cannot be set.
     */
    public void setPrivateKey(String alias, PrivateKey privateKey, Certificate[] chain, char[] protPass) throws Exception
    {
        PrivateKeyEntry entry = new PrivateKeyEntry(privateKey, chain);
        ProtectionParameter prot = new PasswordProtection(protPass);
        keyStore.setEntry(alias, entry, prot);
        this.modified = true;
    }
    
    /**
     * Sets a private key entry, along with its associated certificate chain, in
     * the keystore.
     *
     * @param alias the alias to associate with the private key.
     * @param privateKey the private key to store.
     * @param chain the certificate chain for the corresponding public key.
     * @throws Exception if the entry cannot be set.
     */
    public void setPrivateKey(String alias, PrivateKey privateKey, Certificate[] chain) throws Exception
    {
        this.setPrivateKey(alias, privateKey, chain, passphraser.get(alias)); 
    }
    
    /**
     * Retrieves a private key from the keystore.
     *
     * @param alias the alias of the private key entry.
     * @param protPass the password to decrypt the private key.
     * @return the requested {@link PrivateKey}, or null if the key for the
     * given alias does not exist.
     * @throws Exception if the key cannot be retrieved.
     */
    public PrivateKey getPrivateKey(String alias, char[] protPass) throws Exception
    {
        return (PrivateKey) keyStore.getKey(alias, protPass);
    }
    
    /**
     * Retrieves a private key from the keystore.
     *
     * @param alias the alias of the private key entry.
     * @return the requested {@link PrivateKey}, or null if the key for the
     * given alias does not exist.
     * @throws Exception if the key cannot be retrieved.
     */
    public PrivateKey getPrivateKey(String alias) throws Exception
    {
        return this.getPrivateKey(alias, passphraser.get(alias)); 
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
        this.modified = true;
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
