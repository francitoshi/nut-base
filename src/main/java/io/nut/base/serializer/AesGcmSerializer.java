/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.serializer;

import io.nut.base.crypto.AesGcmBytesCipher;
import io.nut.base.crypto.Kripto;
import io.nut.base.util.Exceptions;
import javax.crypto.SecretKey;
import java.util.logging.Logger;

public class AesGcmSerializer<T> extends AesGcmBytesCipher implements Serializer<T>
{
    private static final Logger LOG = Logger.getLogger(AesGcmSerializer.class.getName());
    private final Serializer<T> serializer;

    public AesGcmSerializer(SecretKey key, Serializer<T> serializer)
    {
        this(key, serializer, null);
    }
    public AesGcmSerializer(SecretKey key, Serializer<T> serializer, Kripto kripto)
    {
        super(key, kripto);
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
            throw Exceptions.rethrow(LOG, ex);
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
            throw Exceptions.rethrow(LOG, ex);
        }
    }

}
