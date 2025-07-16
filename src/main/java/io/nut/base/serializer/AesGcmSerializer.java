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

import io.nut.base.crypto.AesGcmBytesCipher;
import io.nut.base.crypto.Kripto;
import javax.crypto.SecretKey;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AesGcmSerializer<T> extends AesGcmBytesCipher implements Serializer<T>
{
    private final SecretKey key;
    private final Serializer<T> serializer;

    public AesGcmSerializer(SecretKey key, Serializer<T> serializer)
    {
        this(key, serializer, null);
    }
    public AesGcmSerializer(SecretKey key, Serializer<T> serializer, Kripto kripto)
    {
        super(key, kripto);
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

}
