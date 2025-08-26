/*
 *  HKDF.java
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

import io.nut.base.crypto.Kripto.Hkdf;
import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import javax.crypto.SecretKey;

public abstract class HKDF
{
    final Hkdf algorithm;

    public HKDF(Hkdf algorithm)
    {
        this.algorithm = algorithm;
    }

    public abstract byte[] deriveBytes(byte[] ikm, byte[] salt, byte[] info, int keyBytes);
    public abstract SecretKey deriveSecretKey(byte[] ikm, byte[] salt, byte[] info, int keyBytes, SecretKeyAlgorithm keyAlgorithm); 

    public final SecretKey deriveSecretKeyAES(byte[] ikm, byte[] salt, byte[] info, int keyBytes) 
    {
        return deriveSecretKey(ikm, salt, info, keyBytes, SecretKeyAlgorithm.AES);
    }
    
}
