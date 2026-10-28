/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto;

public interface Passphraser
{
    byte[] bytes(byte[] info);
    
    byte[] bytes(String info);
    
    char[] chars(byte[] info);
    
    char[] chars(String info);
}
