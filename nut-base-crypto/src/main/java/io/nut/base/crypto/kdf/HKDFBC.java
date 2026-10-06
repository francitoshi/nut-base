/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.kdf;

import io.nut.base.crypto.Kripto.Hkdf;
import io.nut.base.jca.Kr.SecretKeyAlgorithm;
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
