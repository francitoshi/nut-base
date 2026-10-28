/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto;

import io.github.francitoshi.nut.base.crypto.kdf.HKDF;
import io.github.francitoshi.nut.base.commons.encoding.Ascii85;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PassphraserHkdf implements Passphraser, Closeable
{
    private final HKDF hkdf;
    private final byte[] ikm;
    private final byte[] salt;

    public PassphraserHkdf(HKDF hkdf, byte[] ikm, byte[] salt)
    {
        this.hkdf = hkdf;
        this.ikm = ikm.clone();
        this.salt = salt;
    }
    
    @Override
    public byte[] bytes(byte[] info)
    {
        return this.hkdf.deriveBytes(ikm, salt, info, ikm.length);
    }

    @Override
    public byte[] bytes(String info)
    {
        return bytes(info.getBytes(StandardCharsets.UTF_8));
    }
    @Override
    public char[] chars(byte[] info)
    {
        return Ascii85.encode(bytes(info));
    }
    @Override
    public char[] chars(String info)
    {
        return chars(info.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException
    {
        Arrays.fill(this.ikm, (byte)0);
    }
    
}
