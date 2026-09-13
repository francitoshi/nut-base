/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.serializer;

import io.nut.base.crypto.AesSivCtrBytesCipher;
import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.Kripto.Hmac;
import io.nut.base.serializer.Serializer;
import io.nut.base.util.Exceptions;
import javax.crypto.SecretKey;
import java.util.logging.Logger;

/**
 * Serializador que cifra y descifra datos usando AES en modo CTR. El formato de
 * salida es [IV] + [Texto Cifrado]. Admite tanto IV aleatorios (no
 * determinista) como IV sintéticos generados con HMAC (determinista). Cuando se
 * usa un IV sintético, se realiza una verificación de integridad durante el
 * descifrado.
 */
public class AesSivCtrSerializer<T> extends AesSivCtrBytesCipher implements Serializer<T>
{
    private static final Logger LOG = Logger.getLogger(AesSivCtrSerializer.class.getName());

    private final Serializer<T> serializer;

    public AesSivCtrSerializer(Hmac hmac, SecretKey hmacKey, SecretKey encryptionKey, Serializer<T> serializer)
    {
        super(hmac, hmacKey, encryptionKey);
        this.serializer = serializer;
    }

    public AesSivCtrSerializer(Hmac hmac, SecretKey hmacKey, SecretKey encryptionKey, Serializer<T> serializer, Kripto kripto)
    {
        super(hmac, hmacKey, encryptionKey, kripto);
        this.serializer = serializer;
    }
    
    @Override
    public byte[] toBytes(T t)
    {
        try
        {
            return t != null ? encrypt(this.serializer.toBytes(t)) : null;
        }
        catch (Exception ex)
        {
            throw Exceptions.rethrow(LOG, ex);
        }
    }

    @Override
    public T fromBytes(byte[] bytes)
    {
        try
        {
            return bytes != null ? this.serializer.fromBytes(decrypt(bytes)) : null;
        }
        catch (Exception ex)
        {
            throw Exceptions.rethrow(LOG, ex);
        }
    }

}
