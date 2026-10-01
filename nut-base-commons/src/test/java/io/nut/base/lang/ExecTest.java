/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

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
