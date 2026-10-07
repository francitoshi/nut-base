/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.kdf;

import io.nut.base.jca.Kr.Hkdf;
import io.nut.base.jca.Kr.SecretKeyAlgorithm;
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
