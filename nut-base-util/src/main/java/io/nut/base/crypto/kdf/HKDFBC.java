/*
 *  HKDFBC.java
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
package io.nut.base.crypto.kdf;

import io.nut.base.crypto.Kripto.Hkdf;
import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

public class HKDFBC extends HKDF
{
    public HKDFBC(Hkdf algorithm)
    {
        super(algorithm);
    }

    private HKDFBytesGenerator get(Hkdf algorithm) 
    {
        switch (algorithm)
        {
            case HkdfWithSha256:
                return new HKDFBytesGenerator(new SHA256Digest());
            case HkdfWithSha384:
                return new HKDFBytesGenerator(new SHA384Digest());
            case HkdfWithSha512:
                return new HKDFBytesGenerator(new SHA512Digest());
            default:
                return null;
        }
    }   

    @Override
    public byte[] deriveBytes(byte[] ikm, byte[] salt, byte[] info, int keyBytes) 
    {
        HKDFBytesGenerator hkdf = get(this.algorithm);
        hkdf.init(new HKDFParameters(ikm, salt, info));
        byte[] okm = new byte[keyBytes]; // OKM = Output Keying Material
        hkdf.generateBytes(okm, 0, keyBytes);
        return okm;

    }
   
    @Override
    public SecretKey deriveSecretKey(byte[] ikm, byte[] salt, byte[] info, int keyBytes, SecretKeyAlgorithm keyAlgorithm)
    {
        return new SecretKeySpec(deriveBytes(ikm, salt, info, keyBytes), keyAlgorithm.name());
    }

}
