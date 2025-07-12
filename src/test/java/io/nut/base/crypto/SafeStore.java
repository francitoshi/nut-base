/*
 * SafeStore.java
 *
 * Copyright (c) 2025 francitoshi@gmail.com
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

import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import io.nut.base.util.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

public class SafeStore
{
    private static final int KEY_LENGTH = 256;  // 256 bits para AES-256
    private static final int DEFAULT_ROUNDS = 600_000; // Iteraciones para PBKDF2

    private Kripto kripto;
    private Derive derive;
    private final File file;
    private final char[] charKey;  // Clave como char[] (puede ser null)
    private final byte[] iv;       // Vector de inicialización
    private final int rounds;
    private final byte[] salt;
    private final boolean atomic;  // Indica si la escritura debe ser atómica

    private SecretKey secretKey;
    private IvParameterSpec ivSpec;

    private static final char[] IV_SEED = "safestore.iv.".toCharArray();
    private static final char[] SALT_SEED = "safestore.salt.".toCharArray();
    
    // Constructor para char[] con IV explícito y opción atómica
    public SafeStore(File file, char[] key, String appName, boolean atomic, int rounds, Kripto kripto) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        this.kripto = kripto==null ? Kripto.getInstance() : kripto;
        this.derive = kripto.getDerivePBKDF2WithHmacSHA256();
        this.file = file;
        char[] app = appName.toCharArray();
        this.charKey = (key!=null) ? key : Utils.EMPTY_CHAR_ARRAY;
        this.iv = kripto.deriveSaltSHA256(IV_SEED, app, key);      //32 bytes
        this.salt =  kripto.deriveSaltSHA256(SALT_SEED, app, key); //32 bytes
        this.atomic = atomic;
        this.rounds = rounds;
    }

    public SafeStore(File file, char[] key, String appName, boolean atomic) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        this(file, key, appName, atomic, DEFAULT_ROUNDS, null);
    }
    
    // derive the key using PBKDF2
    private SecretKey getDerivedKey() throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        if(this.secretKey==null)
        {
            this.secretKey = derive.deriveSecretKey(this.charKey, salt, rounds, KEY_LENGTH, SecretKeyAlgorithm.AES);
        }
        return this.secretKey;
    }

    private IvParameterSpec getIvParameterSpec() throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        if(this.ivSpec==null)
        {
            this.ivSpec = kripto.getIv(iv, 128);
        }
        return this.ivSpec;
    }

    private Cipher getCipher(int mode) throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeySpecException
    {
        return kripto.getCipher(getDerivedKey(), SecretKeyTransformation.AES_CBC_PKCS5Padding, getIvParameterSpec(), mode);
    }
    
    interface StoreWriter
    {
        void write(OutputStream out, String comments)throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidAlgorithmParameterException, NoSuchPaddingException;
    }
    interface StoreReader
    {
        void read(InputStream in)throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidAlgorithmParameterException, NoSuchPaddingException;
    }
    
    boolean read(StoreReader sr) throws InvalidKeyException, IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeySpecException
    {
        if(!file.exists())
        {
            return false;
        }
        
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE);

        try (InputStream fis = new CipherInputStream(new FileInputStream(file), cipher))
        {
            sr.read(fis);
        }
        
        return true;
    }
    void write(StoreWriter sw) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, FileNotFoundException, InvalidKeySpecException
    {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
        
        File tmp = atomic ? File.createTempFile("temp_encrypted_", ".tmp") : file;
        
        try (OutputStream fos = new CipherOutputStream(new FileOutputStream(tmp), cipher))
        {
            sw.write(fos, "");
        }
        if(atomic)
        {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }
    }
    
    public boolean load(Properties properties) throws InvalidKeyException, IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeySpecException
    {
        return read(properties::load);
    }
    public boolean store(Properties properties) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IOException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, InvalidKeySpecException 
    {
        write(properties::store);
        return true;
    }
    
}
