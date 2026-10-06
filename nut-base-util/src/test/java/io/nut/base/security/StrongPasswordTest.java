/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import io.nut.base.security.StrongPassword.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class StrongPasswordTest
{
    @Test
    public void testAnalyze()
    {
        StrongPassword instance = new StrongPassword(8);

        assertEquals(Level.VeryWeak, instance.getLevel(instance.analyze("password")));
        assertEquals(Level.Weak, instance.getLevel(instance.analyze("Password")));
        assertEquals(Level.Strong, instance.getLevel(instance.analyze("Password123")));
        assertEquals(Level.VeryStrong, instance.getLevel(instance.analyze("Password123!")));
        assertEquals(Level.VeryWeak, instance.getLevel(instance.analyze("12345678")));
        assertEquals(Level.TooShort, instance.getLevel(instance.analyze("abcdefgh")));
        assertEquals(Level.VeryWeak, instance.getLevel(instance.analyze("asdfghjkl")));
        assertEquals(Level.Weak, instance.getLevel(instance.analyze("asdfghjkl1")));
        assertEquals(Level.Strong, instance.getLevel(instance.analyze("asdfghjklA1!")));
        assertEquals(Level.TooShort, instance.getLevel(instance.analyze("aA1!")));
        assertEquals(Level.TooShort, instance.getLevel(instance.analyze("aaabbbccc")));
        assertEquals(Level.VeryStrong, instance.getLevel(instance.analyze("P@$$w0rd")));
    }

}
