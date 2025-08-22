/*
 *  EncryptedString.java
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
package io.nut.base.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * A secure string implementation that encrypts sensitive data using
 * AES/GCM/NoPadding. Implements AutoCloseable and CharSequence interfaces.
 * Automatically generates a random key and IV for each instance.
 */
public final class EncryptedString implements SecureString, CharSequence, AutoCloseable
{
    private static final int GCM_IV_LENGTH = 12; // Recommended IV length for GCM
    private static final int GCM_TAG_LENGTH = 16; // Authentication tag length
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final SecureRandom RANDOM = new SecureRandom();

    protected final byte[] encryptedData;
    private final SecretKey secretKey;
    private final byte[] iv;
    private volatile boolean closed;
    private final int length;

    /**
     * Constructor that encrypts the input char array
     *
     * @param input The sensitive data to encrypt
     */
    public EncryptedString(char[] input)
    {
        if (input == null)
        {
            this.encryptedData = null;
            this.secretKey = null;
            this.iv = null;
            this.closed = false;
            this.length = 0;
            return;
        }
        if (input.length == 0)
        {
            this.encryptedData = new byte[0];
            this.secretKey = null;
            this.iv = null;
            this.closed = false;
            this.length = 0;
            return;
        }

        this.length = input.length;

        // Generate random key and IV
        byte[] keyBytes = new byte[32]; // 256-bit key
        RANDOM.nextBytes(keyBytes);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");

        this.iv = new byte[GCM_IV_LENGTH];
        RANDOM.nextBytes(iv);

        // Convert char[] to byte[]
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(input));
        byte[] inputBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(inputBytes);
        try
        {
            // Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            this.encryptedData = cipher.doFinal(inputBytes);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
             throw new RuntimeException("Encryption failed", ex);
        }
        finally
        {
            // Securely clear sensitive data
            Arrays.fill(inputBytes, (byte) 0);
            Arrays.fill(keyBytes, (byte) 0);
            byteBuffer.clear();
        }
    }
    
    public static EncryptedString take(char[] password)
    {
        try
        {
            return new EncryptedString(password);
        }
        finally
        {
            if(password!=null && password.length>0)
            {
                Arrays.fill(password, '\0');
            }
        }    
    }

    @Override
    public int length()
    {
        return (closed || encryptedData ==null)? 0 : length;
    }

    @Override
    public char charAt(int index)
    {
        if (closed)
        {
            throw new IllegalStateException("SecureString is closed");
        }
        char[] decrypted = toCharArray();
        if (index < 0 || index >= decrypted.length)
        {
            throw new IndexOutOfBoundsException();
        }
        char result = decrypted[index];
        Arrays.fill(decrypted, '\0'); // Clear decrypted array
        return result;
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        if (closed)
        {
            throw new IllegalStateException("SecureString is closed");
        }
        char[] decrypted = toCharArray();
        if (start < 0 || end > decrypted.length || start > end)
        {
            throw new IndexOutOfBoundsException();
        }
        char[] result = Arrays.copyOfRange(decrypted, start, end);
        Arrays.fill(decrypted, '\0'); // Clear decrypted array
        return new EncryptedString(result);
    }

    @Override
    public void close()
    {
        if (!closed)
        {
            Arrays.fill(encryptedData, (byte) 0);
            Arrays.fill(secretKey.getEncoded(), (byte) 0);
            Arrays.fill(iv, (byte) 0);
            closed = true;
        }
    }

    /**
     * Checks if the SecureString is closed
     *
     * @return true if closed
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Decrypts the stored data
     *
     * @return Decrypted char array
     * @throws IllegalStateException if already closed
     */
    @Override
    public char[] toCharArray()
    {
        if (closed)
        {
            throw new IllegalStateException("SecureString is closed");
        }
        if (encryptedData == null)
        {
            return null;
        }
        if (encryptedData.length == 0)
        {
            return new char[0];
        }

        try
        {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedData);

            // Convert bytes to chars
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(decryptedBytes));
            char[] result = new char[charBuffer.remaining()];
            charBuffer.get(result);

            // Securely clear intermediate data
            Arrays.fill(decryptedBytes, (byte) 0);
            charBuffer.clear();

            return result;
        }
        catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex)
        {
            throw new RuntimeException("Decryption failed", ex);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            close(); // Ensure cleanup if not explicitly closed
        }
        finally
        {
            super.finalize();
        }
    }

    /**
     * Prevents accidental exposure in logs or debug messages. The contents are 
     * never revealed.
     */
    @Override
    public String toString()
    {
        return "[EncryptedString: content hidden]";
    }
    
}
