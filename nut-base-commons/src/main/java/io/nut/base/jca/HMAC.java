/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.jca;

import io.nut.base.jca.Kr.Hmac;
import java.nio.charset.Charset;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HMAC
{

    final Kr kr;
    final Hmac algorithm;

    public HMAC(Kr kr, Hmac algorithm)
    {
        this.kr = kr==null ? Kr.getInstance() : kr;
        this.algorithm = algorithm;
    }
    
    public Mac get(SecretKey secretKey)
    {
        return kr.getMac(algorithm, secretKey);
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
