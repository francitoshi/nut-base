/*
 *  SecureBytes.java
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

import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import io.nut.base.crypto.Rand;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.security.auth.Destroyable;

public class SecureBytes implements AutoCloseable, Destroyable
{
    private enum Holder
    {
        INSTANCE;
        Kripto kripto = Kripto.getInstance();
    }

    private static final int IV_BYTES = 12; //96 bits
    private static final int TAG_BITS = 128;
    private static final Rand RAND = Kripto.getRand();
    
    private final Kripto kripto;
    private final byte[] iv;
    private final SecretKey key;
    private final byte[] encryptedData;
    private volatile boolean destroyed;

    public SecureBytes(byte[] data, Kripto kripto)
    {
        try
        {
            if(data == null)
            {
                this.kripto = null;
                this.iv = null;
                this.key = null;
                this.encryptedData = null;
                this.destroyed = true;
                return;
            }
            if(data.length == 0)
            {
                this.kripto = null;
                this.iv = null;
                this.key = null;
                this.encryptedData = new byte[0];
                this.destroyed = true;
                return;
            }

            this.kripto = kripto==null ? Holder.INSTANCE.kripto : kripto;
            this.key = this.kripto.keyGenAes256.generateKey();
            this.iv = RAND.nextBytes(new byte[IV_BYTES]);
            
            GCMParameterSpec spec = this.kripto.getIvGCM(iv, TAG_BITS);
            Cipher cipher = this.kripto.getCipher(this.key, SecretKeyTransformation.AES_GCM_NoPadding, spec, Cipher.ENCRYPT_MODE);
            this.encryptedData = cipher.doFinal(data);
            
            Arrays.fill(data, (byte) 0);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public SecureBytes(byte[] data)
    {
        this(data, Holder.INSTANCE.kripto);
    }

    public byte[] getBytes()
    {
        if(this.encryptedData==null)
        {
            return null;
        }
        if(this.encryptedData.length==0)
        {
            return this.encryptedData;
        }
        try
        {
            GCMParameterSpec spec = this.kripto.getIvGCM(iv, TAG_BITS);
            Cipher cipher = this.kripto.getCipher(this.key, SecretKeyTransformation.AES_GCM_NoPadding, spec, Cipher.DECRYPT_MODE);
            return cipher.doFinal(this.encryptedData);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void destroy()
    {
        if(!this.destroyed)
        {
            Arrays.fill(this.encryptedData, (byte) 0);
            Arrays.fill(this.iv, (byte) 0);
            Wiper.wipeSecretKey(this.key);
            this.destroyed=true;
        }
    }

    @Override
    public boolean isDestroyed()
    {
        return this.destroyed;
    }
    
    @Override
    public void close()
    {
        this.destroy();
    }
    
    public void consume(Consumer<byte[]> consumer)
    {
        byte[] tmp = getBytes();
        try
        {
            consumer.accept(tmp);
        }
        finally
        {
            if(tmp!=null && tmp.length!=0)
            {
                Arrays.fill(tmp, (byte)0);
            }
        }
    }
    
}
