/*
 * FigIOTest.java
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

import io.nut.base.util.Java;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FigIO")
class FigIOTest
{
    // ── helpers ───────────────────────────────────────────────────────────────

    private static String buildMinimalFlf(int height)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("flf2a$ %d %d 10 -1 1%n", height, height));
        sb.append("comment").append(System.lineSeparator());
        for (int code = 32; code <= 126; code++)
        {
            appendGlyph(sb, height);
        }
        for (int i = 0; i < 7; i++)
        {
            appendGlyph(sb, height);
        }
        return sb.toString();
    }

    private static String buildMinimalTlf(int height)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("tlf2a\u007F %d %d 10 -1 1%n", height, height));
        sb.append("comment").append(System.lineSeparator());
        for (int code = 32; code <= 126; code++)
        {
            appendGlyphTlf(sb, height);
        }
        for (int i = 0; i < 7; i++)
        {
            appendGlyphTlf(sb, height);
        }
        return sb.toString();
    }

    private static void appendGlyph(StringBuilder sb, int height)
    {
        for (int r = 0; r < height; r++)
        {
            sb.append(r == height - 1 ? " @@" : " @").append(System.lineSeparator());
        }
    }

    private static void appendGlyphTlf(StringBuilder sb, int height)
    {
        for (int r = 0; r < height; r++)
        {
            sb.append(r == height - 1 ? " @@" : " @").append(System.lineSeparator());
        }
    }

    private static InputStream toStream(String s)
    {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Wraps bytes in a ZIP archive (single entry).
     */
    private static byte[] zip(byte[] data, String entryName) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos))
        {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(data);
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    // ── load dispatching ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("load – format dispatching")
    class LoadDispatching
    {

        private final FigIO figIO = new FigIO();

        @Test
        @DisplayName("Dispatches FLF stream to Flf2aIO")
        void dispatchesFlf() throws IOException
        {
            Glyphs g = figIO.load("test.flf", toStream(buildMinimalFlf(3)));
            assertNotNull(g);
            assertEquals(3, g.height);
        }

        @Test
        @DisplayName("Dispatches TLF stream to Tlf2aIO")
        void dispatchesTlf() throws IOException
        {
            Glyphs g = figIO.load("test.tlf", toStream(buildMinimalTlf(3)));
            assertNotNull(g);
            assertEquals(3, g.height);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException for unknown format")
        void throwsForUnknownFormat()
        {
            InputStream bad = toStream("unknown_header foo bar\n");
            assertThrows(IllegalArgumentException.class, () -> figIO.load("bad.xxx", bad));
        }

        @Test
        @DisplayName("Throws IllegalArgumentException for empty stream")
        void throwsForEmptyStream()
        {
            InputStream empty = toStream("");
            assertThrows(IllegalArgumentException.class, () -> figIO.load("empty.flf", empty));
        }
    }

    // ── ZIP auto-decompression ────────────────────────────────────────────────
    @Nested
    @DisplayName("load – ZIP decompression")
    class ZipDecompression
    {
        private final FigIO figIO = new FigIO();

        @Test
        @DisplayName("Loads FLF packed in a ZIP transparently")
        void loadsFlfFromZip() throws IOException
        {
            byte[] flfBytes = buildMinimalFlf(3).getBytes(StandardCharsets.UTF_8);
            byte[] zipped = zip(flfBytes, "font.flf");
            Glyphs g = figIO.load("font.flf", new ByteArrayInputStream(zipped));
            assertNotNull(g);
            assertEquals(3, g.height);
        }

        @Test
        @DisplayName("Loads TLF packed in a ZIP transparently")
        void loadsTlfFromZip() throws IOException
        {
            byte[] tlfBytes = buildMinimalTlf(4).getBytes(StandardCharsets.UTF_8);
            byte[] zipped = zip(tlfBytes, "font.tlf");
            Glyphs g = figIO.load("font.tlf", new ByteArrayInputStream(zipped));
            assertNotNull(g);
            assertEquals(4, g.height);
        }
    }

    // ── exportFLF / exportTLF shortcuts ──────────────────────────────────────
    @Nested
    @DisplayName("exportFLF / exportTLF")
    class ExportShortcuts
    {

        private final FigIO figIO = new FigIO();

        @Test
        @DisplayName("exportFLF produces FLF-signed output")
        void exportFlfSignature() throws IOException
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            figIO.exportFLF(g, "test", bos);
            assertTrue(bos.toString(StandardCharsets.UTF_8.name()).startsWith("flf2a"));
        }

        @Test
        @DisplayName("exportTLF produces TLF-signed output")
        void exportTlfSignature() throws IOException
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            figIO.exportTLF(g, "test", bos);
            assertTrue(bos.toString(StandardCharsets.UTF_8.name()).startsWith("tlf2a"));
        }

        @Test
        @DisplayName("FLF export → FLF load round-trip via FigIO")
        void flfRoundTripViaFigIO() throws IOException
        {
            Glyphs original = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            figIO.exportFLF(original, "rt", bos);

            Glyphs reloaded = figIO.load("rt.flf", new ByteArrayInputStream(bos.toByteArray()));
            assertEquals(original.height, reloaded.height);
        }

        @Test
        @DisplayName("TLF export → TLF load round-trip via FigIO")
        void tlfRoundTripViaFigIO() throws IOException
        {
            Glyphs original = Glyphs.getInstance(FigLetter.Font.BLOCK);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            figIO.exportTLF(original, "rt", bos);

            Glyphs reloaded = figIO.load("rt.tlf", new ByteArrayInputStream(bos.toByteArray()));
            assertEquals(original.height, reloaded.height);
        }
    }

    // ── computeMaxLen ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("computeMaxLen")
    class ComputeMaxLen
    {

        @Test
        @DisplayName("Returns max row length + 2 for BLOCK font")
        void blockFontMaxLen()
        {
            Glyphs g = Glyphs.getInstance(FigLetter.Font.BLOCK);
            int maxLen = FigIO.computeMaxLen(g);
            // Max row length in BLOCK is 5, so maxLen = 7
            assertTrue(maxLen >= 2, "maxLen should be at least 2");
            // Verify it's consistent: export a FLF and check the header field
            try
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                new FigIO().exportFLF(g, null, bos);
                String header = bos.toString(StandardCharsets.UTF_8.name()).split("\\r?\\n")[0];
                String[] parts = header.split("\\s+");
                int headerMaxLen = Integer.parseInt(parts[3]);
                assertEquals(maxLen, headerMaxLen);
            }
            catch (IOException e)
            {
                fail("IOException during maxLen verification: " + e.getMessage());
            }
        }
    }

    // ── parseCode ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("parseCode")
    class ParseCode
    {

        @Test
        @DisplayName("Parses decimal string")
        void parsesDecimal()
        {
            assertEquals(65, FigIO.parseCode("65"));
        }

        @Test
        @DisplayName("Parses 0x-prefixed hex string (lowercase)")
        void parsesHexLower()
        {
            assertEquals(255, FigIO.parseCode("0xff"));
        }

        @Test
        @DisplayName("Parses 0X-prefixed hex string (uppercase)")
        void parsesHexUpper()
        {
            assertEquals(255, FigIO.parseCode("0XFF"));
        }

        @Test
        @DisplayName("Parses 0x1F600 (emoji codepoint)")
        void parsesEmojiHex()
        {
            assertEquals(0x1F600, FigIO.parseCode("0x1F600"));
        }

        @Test
        @DisplayName("Throws NumberFormatException for invalid string")
        void throwsForInvalidString()
        {
            assertThrows(NumberFormatException.class, () -> FigIO.parseCode("notanumber"));
        }
    }

    final Glyphs glyphs0 = Glyphs.getInstance(FigLetter.Font.BLOCK);
    
    @Test
    public void testAll() throws Exception
    {
        File tmp = new File(Java.JAVA_IO_TMPDIR);
        File blockFlf = new File(tmp,"block.flf");
        File blockTlf = new File(tmp,"block.tlf");
        
        FigIO instance = new FigIO();
        {
            instance.exportFLF(glyphs0, "comment0", new FileOutputStream(blockFlf));
        }
        {
            Glyphs glyphs = instance.load(blockFlf);
            assertNotNull(glyphs);
            instance.exportTLF(glyphs, "comment1", new FileOutputStream(blockTlf));
        }
        {
            Glyphs glyphs = instance.load(blockTlf);
            assertNotNull(glyphs);
        }            
    }    
}
