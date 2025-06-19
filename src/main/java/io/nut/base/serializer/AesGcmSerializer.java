/*
 *  AesGcmSerializer.java
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
package io.nut.base.serializer;

import io.nut.base.crypto.HMAC;
import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AesGcmSerializer<T> implements Serializer<T>
{
    private final Kripto kripto;
    private final HMAC hmac;
    private final SecretKey hmacKey;
    private final SecretKey key;
    private final Serializer<T> serializer;

    public AesGcmSerializer(SecretKey key, Serializer<T> serializer)
    {
        this(null, key, serializer, null);
    }
    public AesGcmSerializer(SecretKey hmacKey, SecretKey key, Serializer<T> serializer)
    {
        this(hmacKey, key, serializer, null);
    }
    public AesGcmSerializer(SecretKey key, Serializer<T> serializer, Kripto kripto)
    {
        this(null, key, serializer, kripto);
    }
    public AesGcmSerializer(SecretKey hmacKey, SecretKey key, Serializer<T> serializer, Kripto kripto)
    {
        this.kripto = kripto==null ? Kripto.getInstance() : kripto;
        this.hmacKey = hmacKey;
        this.hmac = hmacKey==null ? null : kripto.hmac();
        this.key = key;
        this.serializer = serializer;
    }

    @Override
    public byte[] toBytes(T t)
    {
        try
        {
            return t!=null ? encrypt(this.serializer.toBytes(t)) : null;
        }
        catch (Exception ex)
        {
            Logger.getLogger(AesGcmSerializer.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public T fromBytes(byte[] bytes)
    {
        try
        {
            return bytes!=null ? this.serializer.fromBytes(decrypt(bytes)) : null;
        }
        catch (Exception ex)
        {
            Logger.getLogger(AesGcmSerializer.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private static final int GCM_IV_BITS = 96;
    private static final int GCM_IV_LENGTH = GCM_IV_BITS/8;

    private byte[] encrypt(byte[] plaintext) throws Exception
    {
        byte[] iv = hmac==null ? Kripto.random(new byte[GCM_IV_LENGTH]) : hmac.hmacSHA256(hmacKey, new byte[GCM_IV_LENGTH]);

        GCMParameterSpec ivGCM = kripto.getIvGCM(iv, GCM_IV_BITS);
        byte[] encryptedData = kripto.encrypt(key, SecretKeyTransformation.AES_GCM_NoPadding, ivGCM, plaintext);

        byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
        System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);

        return encryptedWithIv;
    }

    private byte[] decrypt(byte[] ciphertext) throws Exception
    {
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(ciphertext, 0, iv, 0, iv.length);

        byte[] encryptedData = new byte[ciphertext.length - GCM_IV_LENGTH];
        System.arraycopy(ciphertext, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

        GCMParameterSpec ivGCM = kripto.getIvGCM(iv, GCM_IV_BITS);
        return kripto.decrypt(key, Kripto.SecretKeyTransformation.AES_GCM_NoPadding, ivGCM, encryptedData);
    }

}
