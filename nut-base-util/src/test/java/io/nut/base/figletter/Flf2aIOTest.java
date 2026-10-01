/*
 * Flf2aIOTest.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.figletter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Flf2aIO")
class Flf2aIOTest
{
    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a minimal but valid FLF file in memory for a given height.
     */
    private static String buildMinimalFlf(int height, int numComments, String commentText)
    {
        // 95 ASCII glyphs (32..126) + 7 german = 102 glyphs total
        StringBuilder sb = new StringBuilder();
        // flf2a<hardblank> height baseline maxLen oldLayout numComments
        sb.append(String.format("flf2a$ %d %d 10 -1 %d%n", height, height, numComments));
        for (int i = 0; i < numComments; i++)
        {
            sb.append(commentText).append(System.lineSeparator());
        }
        // 95 ASCII glyphs
        for (int code = 32; code <= 126; code++)
        {
            writeSimpleGlyph(sb, height, (char) code);
        }
        // 7 german glyphs
        for (int i = 0; i < 7; i++)
        {
            writeSimpleGlyph(sb, height, 'X');
        }
        return sb.toString();
    }

    private static void writeSimpleGlyph(StringBuilder sb, int height, char fill)
    {
        for (int r = 0; r < height; r++)
        {
            if (r == height - 1)
            {
                sb.append(fill).append("@@").append(System.lineSeparator());
            }
            else
            {
                sb.append(fill).append("@").append(System.lineSeparator());
            }
        }
    }

    private static InputStream toStream(String s)
    {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    // ── load ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("load")
    class Load
    {

        private Flf2aIO io;

        @BeforeEach
        void setup()
        {
            io = new Flf2aIO();
        }

        @Test
        @DisplayName("Loads minimal FLF with height=3 and 1 comment")
        void loadMinimalFlf() throws IOException
        {
            String flf = buildMinimalFlf(3, 1, "A comment");
            Glyphs g = io.load("test.flf", toFlfLines(flf));
            assertNotNull(g);
            assertEquals(3, g.height);
        }

        @Test
        @DisplayName("Loads FLF with height=5 and multiple comments")
        void loadFlfMultipleComments() throws IOException
        {
            String flf = buildMinimalFlf(5, 3, "Line");
            Glyphs g = io.load("test.flf", toFlfLines(flf));
            assertNotNull(g);
            assertEquals(5, g.height);
        }

        @Test
        @DisplayName("Hardblank is extracted from header")
        void hardblankExtracted() throws IOException
        {
            String flf = buildMinimalFlf(3, 1, "comment");
            Glyphs g = io.load("test.flf", toFlfLines(flf));
            assertEquals('$', g.hardblank);
        }

        @Test
        @DisplayName("All 95 ASCII glyphs (32-126) are loaded")
        void allAsciiGlyphsLoaded() throws IOException
        {
            String flf = buildMinimalFlf(3, 1, "comment");
            Glyphs g = io.load("test.flf", toFlfLines(flf));
            for (int code = 32; code <= 126; code++)
            {
                assertNotNull(g.get((char) code), "Missing glyph for code " + code + " ('" + (char) code + "')");
            }
        }

        @Test
        @DisplayName("Glyph rows have endmark '@' stripped")
        void endmarkStripped() throws IOException
        {
            // Build FLF where space glyph rows are "   @" and last "   @@"
            String flf = buildMinimalFlf(3, 1, "comment");
            Glyphs g = io.load("test.flf", toFlfLines(flf));
            // Space glyph (code 32) - fill char was ' ' (space), so each row is just " "
            String[] space = g.get(' ');
            assertNotNull(space);
            for (String row : space)
            {
                assertFalse(row.endsWith("@"), "Endmark '@' was not stripped from row: " + row);
            }
        }

        @Test
        @DisplayName("Throws IllegalArgumentException for non-FLF header")
        void throwsOnBadHeader()
        {
            java.util.List<String> lines = java.util.Arrays.asList("not_a_valid_header rest");
            assertThrows(IllegalArgumentException.class, () -> io.load("bad.flf", lines));
        }

        @Test
        @DisplayName("Throws IllegalArgumentException for malformed numeric header field")
        void throwsOnMalformedHeader()
        {
            // Missing required fields after signature
            List<String> lines = Arrays.asList("flf2a$ badnum");
            assertThrows(IllegalArgumentException.class, () -> io.load("bad.flf", lines));
        }

        @Test
        @DisplayName("Loads tagged glyph beyond ASCII block")
        void loadsTaggedGlyph() throws IOException
        {
            // Build FLF + one tagged glyph for code 65536 → skipped (> 0xFFFF)
            // Use code 200 (within BMP) as a tagged glyph
            StringBuilder sb = new StringBuilder(buildMinimalFlf(3, 1, "comment"));
            sb.append("200").append(System.lineSeparator());
            writeSimpleGlyph(sb, 3, 'T');
            String flf = sb.toString();
            // Replace codeTagCount in header to 1
            flf = flf.replaceFirst("(flf2a\\$ \\d+ \\d+ \\d+ -1 \\d+ 0 0) 0", "$1 1");
            Glyphs g = io.load("test.flf", toFlfLines(flf));
            assertNotNull(g.get((char) 200));
        }

        // Helper: convert String to List<String> as the parser expects
        private java.util.List<String> toFlfLines(String s)
        {
            return new java.util.ArrayList<>(java.util.Arrays.asList(s.split("\\r?\\n", -1)));
        }
    }

    // ── export ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("export")
    class Export
    {

        private Flf2aIO io;

        @BeforeEach
        void setup()
        {
            io = new Flf2aIO();
        }

        @Test
        @DisplayName("Exported bytes start with FLF signature")
        void exportedBytesStartWithSignature() throws IOException
        {
            Glyphs glyphs = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            io.export(glyphs, "Test comment", bos);
            String out = bos.toString(StandardCharsets.UTF_8.name());
            assertTrue(out.startsWith("flf2a"), "Expected FLF signature, got: " + out.substring(0, Math.min(20, out.length())));
        }

        @Test
        @DisplayName("Exported FLF can be re-loaded")
        void exportedFlfCanBeReloaded() throws IOException
        {
            Glyphs glyphs = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            io.export(glyphs, "round-trip", bos);

            String exported = bos.toString(StandardCharsets.UTF_8.name());
            List<String> lines = new ArrayList<>(Arrays.asList(exported.split("\\r?\\n", -1)));

            Glyphs reloaded = io.load("round-trip.flf", lines);
            assertNotNull(reloaded);
            assertEquals(glyphs.height, reloaded.height);
        }

        @Test
        @DisplayName("Exported FLF preserves all 95 ASCII glyphs")
        void exportPreservesAllAsciiGlyphs() throws IOException
        {
            Glyphs glyphs = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            io.export(glyphs, null, bos);

            java.util.List<String> lines = new java.util.ArrayList<>(
                    java.util.Arrays.asList(bos.toString(StandardCharsets.UTF_8.name()).split("\\r?\\n", -1)));
            Glyphs reloaded = io.load("rt.flf", lines);

            for (int code = 32; code <= 126; code++)
            {
                assertNotNull(reloaded.get((char) code),
                        "Missing glyph for code " + code);
            }
        }

        @Test
        @DisplayName("Exported FLF with null comment uses default comment line")
        void nullCommentUsesDefault() throws IOException
        {
            Glyphs glyphs = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            io.export(glyphs, null, bos);
            String out = bos.toString(StandardCharsets.UTF_8.name());
            assertTrue(out.contains("Exported by"), "Expected default comment in: " + out.substring(0, Math.min(200, out.length())));
        }

        @Test
        @DisplayName("Each glyph row in export ends with '@' endmark")
        void glyphRowsEndWithEndmark() throws IOException
        {
            Glyphs glyphs = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            io.export(glyphs, "check endmarks", bos);
            String out = bos.toString(StandardCharsets.UTF_8.name());
            // After the header + comment lines, glyph data begins.
            // Every non-blank content line in glyph blocks must end in '@' or '@@'
            String[] lines = out.split("\\r?\\n", -1);
            // Skip header (1) + comment (1) = 2 lines
            boolean foundGlyphLine = false;
            for (int i = 2; i < lines.length; i++)
            {
                String line = lines[i];
                if (!line.isEmpty())
                {
                    assertTrue(line.endsWith("@"), "Line does not end with '@': " + line);
                    foundGlyphLine = true;
                }
            }
            assertTrue(foundGlyphLine, "No glyph lines found in export");
        }
    }

    // ── round-trip content fidelity ───────────────────────────────────────────
    @Nested
    @DisplayName("Round-trip content fidelity")
    class RoundTrip
    {

        @Test
        @DisplayName("Glyph row content is preserved after export→load")
        void glyphContentPreserved() throws IOException
        {
            Flf2aIO io = new Flf2aIO();
            Glyphs original = Glyphs.getInstance(FigLetter.Font.BLOCK);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            io.export(original, "round-trip test", bos);

            List<String> lines = new java.util.ArrayList<>(Arrays.asList(bos.toString(StandardCharsets.UTF_8.name()).split("\\r?\\n", -1)));
            Glyphs reloaded = io.load("rt.flf", lines);

            // Spot-check: glyph 'A'
            String[] origA = original.get('A');
            String[] rtA = reloaded.get('A');
            assertNotNull(rtA);
            assertEquals(origA.length, rtA.length);
            assertArrayEquals(origA, rtA);
        }
    }
}
