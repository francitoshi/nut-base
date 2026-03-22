/*
 *  ExecTest.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

public class ExecTest
{
    @Nested
    @DisplayName("isBinaryOnPath() – PATH lookup")
    class IsBinaryOnPathTests 
    {

        @Test
        @DisplayName("Returns true for 'java', which is always on PATH in a test JVM")
        void javaIsAlwaysOnPath() 
        {
            // isBinaryOnPath() already appends ".exe" on Windows internally,
            // so we pass just "java" on all platforms.
            assertTrue(Exec.isBinaryOnPath("java"), "'java' must be on PATH when running JUnit tests");
        }

        @Test
        @DisplayName("Returns false for a name that cannot possibly exist")
        void returnsFalseForNonExistentBinary() 
        {
            assertFalse(Exec.isBinaryOnPath("__nonexistent_binary_xyz_12345__"));
        }

        @Test
        @DisplayName("Returns false for an empty binary name")
        void returnsFalseForEmptyName() 
        {
            assertFalse(Exec.isBinaryOnPath(""));
        }
    }
    
}
