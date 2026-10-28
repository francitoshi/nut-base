/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.figletter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Glyphs")
class GlyphsTest
{
    // ── factory ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getInstance(Font)")
    class GetInstance
    {

        @Test
        @DisplayName("BLOCK returns non-null BlockGlyphs instance")
        void blockFont()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            assertNotNull(g);
            assertEquals("Block", g.name);
        }

        @Test
        @DisplayName("SLIM returns non-null SlimGlyphs instance")
        void slimFont()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.SLIM);
            assertNotNull(g);
        }

        @Test
        @DisplayName("DOUBLE returns non-null DoubleGlyphs instance")
        void doubleFont()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.DOUBLE);
            assertNotNull(g);
        }

        @ParameterizedTest(name = "Font.{0} → height > 0")
        @EnumSource(FigLetter.Font.class)
        @DisplayName("Every font has positive height")
        void everyFontHasPositiveHeight(FigLetter.Font font)
        {
            Glyphs g = Glyphs.getInstance(font);
            assertTrue(g.height > 0);
        }
    }

    // ── constructor fields ────────────────────────────────────────────────────
    @Nested
    @DisplayName("Constructor / public fields")
    class ConstructorFields
    {

        @Test
        @DisplayName("All fields are stored correctly")
        void fieldsStoredCorrectly()
        {
            Map<Character, String[]> map = new LinkedHashMap<>();
            map.put('A', new String[]
            {
                "row1"
            });
            Glyphs g = new Glyphs("test", '$', 3, 2, "comment", true, map);

            assertEquals("test", g.name);
            assertEquals('$', g.hardblank);
            assertEquals(3, g.height);
            assertEquals(2, g.baseline);
            assertEquals("comment", g.comment);
            assertTrue(g.caseSensitive);
        }
    }

    // ── get / getOrDefault ────────────────────────────────────────────────────
    @Nested
    @DisplayName("get / getOrDefault")
    class GetMethods
    {

        @Test
        @DisplayName("get returns null for unknown char")
        void getUnknown()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            // BLOCK font maps uppercase letters; '\u0400' not in map
            assertNull(g.get('\u0400'));
        }

        @Test
        @DisplayName("getOrDefault returns default for unknown char")
        void getOrDefaultUnknown()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            String[] def = new String[]{ "fallback" };
            assertSame(def, g.getOrDefault('\u0400', def));
        }

        @Test
        @DisplayName("BLOCK font: lowercase 'a' resolves to uppercase 'A' (caseSensitive=false)")
        void blockFontCaseInsensitive()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            assertFalse(g.caseSensitive);
            String[] upper = g.get('A');
            String[] lower = g.getOrDefault('a', null);
            assertNotNull(upper);
            assertArrayEquals(upper, lower);
        }

        @Test
        @DisplayName("BLOCK font contains '?' glyph")
        void blockFontHasQuestionMark()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            assertNotNull(g.get('?'));
        }
    }

    // ── containsKey ──────────────────────────────────────────────────────────
    @Nested
    @DisplayName("containsKey")
    class ContainsKey
    {

        @Test
        @DisplayName("Known char returns true")
        void knownChar()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            assertTrue(g.containsKey('A'));
        }

        @Test
        @DisplayName("Unknown char returns false")
        void unknownChar()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            assertFalse(g.containsKey('\u0400'));
        }
    }

    // ── values ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("values")
    class Values
    {

        @Test
        @DisplayName("values() is non-null and non-empty for BLOCK font")
        void nonEmpty()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            Iterable<String[]> vals = g.values();
            assertNotNull(vals);
            assertTrue(vals.iterator().hasNext());
        }
    }

    // ── BlockGlyphs embedded font ─────────────────────────────────────────────
    @Nested
    @DisplayName("BlockGlyphs embedded font")
    class BlockGlyphsContent
    {

        private final Glyphs block = Glyphs.getInstance(FigLetter.Font.BLOCK);

        @ParameterizedTest(name = "char '{0}' present")
        @ValueSource(chars = { 'A', 'Z', '0', '9', ' ', '?', '-', '.', '!' })
        @DisplayName("Expected characters are present")
        void expectedCharsPresent(char c)
        {
            assertNotNull(block.get(c));
        }

        @Test
        @DisplayName("Every glyph row count equals declared height")
        void everyGlyphMatchesHeight()
        {
            int h = block.height;
            for (String[] g : block.values())
            {
                assertEquals(h, g.length, "Glyph has " + g.length + " rows but height=" + h);
            }
        }

        @Test
        @DisplayName("hardblank is '$'")
        void hardblankIsDollar()
        {
            assertEquals('$', block.hardblank);
        }

        @Test
        @DisplayName("height is 5")
        void heightIsFive()
        {
            assertEquals(5, block.height);
        }
    }
}
