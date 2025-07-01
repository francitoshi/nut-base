/*
 *  PasswordPolicyTest.java
 *
 *  Copyright (C) 2015-2025 francitoshi@gmail.com
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
