/*
 *  StrongPasswordTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
