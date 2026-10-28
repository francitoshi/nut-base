/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto;

public interface BytesCipher
{
    public byte[] encrypt(byte[] plaintext) throws Exception;
    public byte[] decrypt(byte[] ciphertext) throws Exception;
}
