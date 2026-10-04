/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

/**
 *
 * @author franci
 */
public class PassphraseBuilderTest
{

    @Test
    public void testGenerate()
    {
        PassphraseBuilder instance = new PassphraseBuilder(0, 0, 0, 0);

        for (int i = 1; i < 100; i++)
        {
            char[] pass = instance.generate(i);
            assertEquals(i, pass.length);
        }

        for (int i = 1; i < 20; i++)
        {
            instance = new PassphraseBuilder(i, i, i, i);
            char[] pass = instance.generate(i * 4);
            String s = new String(pass);

            assertEquals(pass.length - i, s.replaceAll("[" + PassphraseBuilder.UPPERCASE + "]", "").length());
            assertEquals(pass.length - i, s.replaceAll("[" + PassphraseBuilder.LOWERCASE + "]", "").length());
            assertEquals(pass.length - i, s.replaceAll("[" + PassphraseBuilder.NUMBERS + "]", "").length());
            assertEquals(pass.length - i, s.replaceAll("[" + PassphraseBuilder.SPECIAL + "]", "").length());
        }
    }

    @Test
    @DisplayName("constructor with empty charset should throw IllegalArgumentException")
    void testGenerateWithEmptyCharsetThrowsIllegalArgument()
    {
        assertThrows(IllegalArgumentException.class, () -> new PassphraseBuilder(0, 0, 0, 0, ""),
                "constructor should throw IllegalArgumentException when allChars is empty and fill characters are needed");
    }

    @Test
    @DisplayName("generate() with a single-char charset and large size should work correctly")
    void testGenerateWithSingleCharCharsetAndLargeSize()
    {
        // allChars has exactly 1 character ("X").
        // No minimums, everything is fill from allChars.
        // This MUST work: should produce a string of repeated 'X'.
        // Verifies the bug does not affect the non-empty allChars path.
        PassphraseBuilder builder = new PassphraseBuilder(0, 0, 0, 0, "X");

        char[] result = assertDoesNotThrow(() -> builder.generate(10));
        assertNotNull(result);
        assertEquals(10, result.length);
        for (char c : result)
        {
            assertEquals('X', c, "Every character should be 'X'");
        }
    }

}
