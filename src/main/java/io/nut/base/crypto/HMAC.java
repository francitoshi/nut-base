/*
 *  HMAC.java
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

import io.nut.base.crypto.Kripto.Hmac;
import java.nio.charset.Charset;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HMAC
{

    final Kripto kripto;
    final Hmac algorithm;

    public HMAC(Kripto kripto, Hmac algorithm)
    {
        this.kripto = kripto==null ? Kripto.getInstance() : kripto;
        this.algorithm = algorithm;
    }
    
    public Mac get(SecretKey secretKey)
    {
        return kripto.getMac(algorithm, secretKey);
    }

    public byte[] digest(byte[] secretKey, byte[] bytes) 
    {
        return digest(new SecretKeySpec(secretKey, algorithm.name()), bytes);
    }
    public byte[] digest(byte[] secretKey, byte[] bytes, int offset, int length) 
    {
        return digest(new SecretKeySpec(secretKey, algorithm.name()), bytes, offset, length);
    }

    public byte[] digest(SecretKey secretKey, byte[] bytes) 
    {
        return digest(secretKey, bytes, 0, bytes.length);
    }

    public byte[] digest(SecretKey secretKey, byte[] bytes, int offset, int length) 
    {
        Mac mac = get(secretKey);
        mac.update(bytes, offset, length);
        return mac.doFinal();
    }

    public byte[] digest(SecretKey secretKey, byte[]... bytes) 
    {
        Mac mac = get(secretKey);
        for(byte[] item : bytes)
        {
            mac.update(item);
        }
        return mac.doFinal();
    }
    
    public byte[] digest(SecretKey secretKey, String s) 
    {
        return digest(secretKey, s.getBytes());
    }

    public byte[] digest(SecretKey secretKey, String s, Charset charset) 
    {
        return digest(secretKey, s.getBytes(charset));
    }
    
}
