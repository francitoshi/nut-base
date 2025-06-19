/*
 *  EncryptedMapWrapper.java
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
package io.nut.base.crypto;

import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import io.nut.base.crypto.Kripto.SecretKeyDerivation;
import io.nut.base.serializer.AesGcmSerializer;
import io.nut.base.serializer.Serializer;
import java.nio.charset.StandardCharsets;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Map;

public class EncryptedMapWrapper<K, V>
{
    private final Kripto kripto;
    private final Map<String, String> map;

    private final AesGcmSerializer<K> keySerializer;
    private final AesGcmSerializer<V> valSerializer;
    
    public EncryptedMapWrapper(Kripto kripto, Map<String, String> map, char[] passphrase, String saltSeed, int rounds, int keyBits, Serializer<K> ks, Serializer<V> vs) throws InvalidKeySpecException
    {
        this.kripto = kripto;
        this.map = map;
        
        byte[] macSalt = (saltSeed+"mac").getBytes(StandardCharsets.UTF_8);
        byte[] keySalt = (saltSeed+"key").getBytes(StandardCharsets.UTF_8);
        byte[] valSalt = (saltSeed+"val").getBytes(StandardCharsets.UTF_8);
        
        SecretKey macKey = kripto.deriveSecretKey(passphrase, macSalt, rounds, keyBits, SecretKeyDerivation.PBKDF2WithHmacSHA256, SecretKeyAlgorithm.AES);
        SecretKey keyKey = kripto.deriveSecretKey(passphrase, keySalt, rounds, keyBits, SecretKeyDerivation.PBKDF2WithHmacSHA256, SecretKeyAlgorithm.AES);
        SecretKey valKey = kripto.deriveSecretKey(passphrase, valSalt, rounds, keyBits, SecretKeyDerivation.PBKDF2WithHmacSHA256, SecretKeyAlgorithm.AES);
                
        this.keySerializer = new AesGcmSerializer<>(macKey, keyKey, ks, kripto);
        this.valSerializer = new AesGcmSerializer<>(valKey, vs, kripto);
    }

    public void put(K key, V value)
    {
        try
        {
            String k = encodeKey(key);
            String v = encodeVal(value);
                    
            map.put(k, v);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error encrypting data", ex);
        }
    }

    private String encodeKey(K key)
    {
        return Base64.getEncoder().encodeToString(keySerializer.toBytes(key));
    }

    private String encodeVal(V value)
    {
        return Base64.getEncoder().encodeToString(valSerializer.toBytes(value));
    }

    private V decodeVal(String v)
    {
        byte[] value = Base64.getDecoder().decode(v);
        return valSerializer.fromBytes(value);
    }

    public V get(K key)
    {
        try
        {
            String k = encodeKey(key);

            String v = map.get(k);
            if (v == null)
            {
                return null;
            }
            return decodeVal(v);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error encrypting data", ex);
        }
    }

    public boolean containsKey(K key)
    {
        try
        {
            String k = encodeKey(key);
            return map.containsKey(k);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error checking key", ex);
        }
    }

    public V remove(K key)
    {
        try
        {
            String k = encodeKey(key);
            String v = map.remove(k);

            if (v == null)
            {
                return null;
            }

            return decodeVal(v);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error removing data", ex);
        }
    }

    public int size()
    {
        return map.size();
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public void clear()
    {
        map.clear();
    }

}
