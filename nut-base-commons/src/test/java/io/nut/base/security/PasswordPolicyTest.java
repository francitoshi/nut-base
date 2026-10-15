/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import io.nut.base.security.PasswordPolicy.Need;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class PasswordPolicyTest
{

    /**
     * Test of verifySafe method, of class PasswordPolicy.
     */
    @Test
    public void testVerifySafe()
    {
        PasswordPolicy policy;
        
        policy = new PasswordPolicy(8, Integer.MAX_VALUE, null, Need.Number,Need.Uppercase, Need.Lowercase,Need.Punctuation);
        assertFalse(policy.verifySafe("AbcdEf0."));
        assertTrue(policy.verifySafe("AbcdEf01"));
        assertTrue(policy.verifySafe("abcdef0."));
        assertTrue(policy.verifySafe("ABCDEF0."));
        assertTrue(policy.verifySafe("AbdEf0."));
        assertTrue(policy.verifySafe("AbcdEf0.", "abcd"));
        assertTrue(policy.verifySafe("AbcdEf0.", "abc","def"));
    }
    
}
