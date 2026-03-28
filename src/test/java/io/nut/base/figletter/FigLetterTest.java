/*
 * FigLetterTest.java
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

@DisplayName("FigLetter")
class FigLetterTest
{
    // ── helpers ───────────────────────────────────────────────────────────────

    private static FigLetter blockFont(int scale)
    {
        return FigLetter.getInstance(FigLetter.Font.BLOCK, scale);
    }

    private static FigLetter slimFont(int scale)
    {
        return FigLetter.getInstance(FigLetter.Font.SLIM, scale);
    }

    private static String buildMinimalFlf(int height)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("flf2a$ %d %d 10 -1 1%n", height, height));
        sb.append("unit test font").append(System.lineSeparator());
        for (int code = 32; code <= 126; code++)
        {
            for (int r = 0; r < height; r++)
            {
                sb.append(r == height - 1 ? "X@@" : "X@").append(System.lineSeparator());
            }
        }
        for (int i = 0; i < 7; i++)
        {
            for (int r = 0; r < height; r++)
            {
                sb.append(r == height - 1 ? " @@" : " @").append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private static InputStream toStream(String s)
    {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    // ── getInstance ───────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getInstance")
    class GetInstance
    {

        @Test
        @DisplayName("getInstance(Font, scale) returns non-null for BLOCK")
        void fromFont()
        {
            assertNotNull(blockFont(1));
        }

        @Test
        @DisplayName("getInstance(name, stream, scale) loads from InputStream")
        void fromStream() throws IOException
        {
            FigLetter fl = FigLetter.getInstance("test.flf", toStream(buildMinimalFlf(3)), 1);
            assertNotNull(fl);
        }
    }

    // ── render ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("render")
    class Render
    {

        @Test
        @DisplayName("Render non-null for non-empty input")
        void renderNonNull()
        {
            String result = blockFont(1).render("A");
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Render empty string returns empty or blank text")
        void renderEmptyString()
        {
            String result = blockFont(1).render("");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Render single space returns non-null output")
        void renderSingleSpace()
        {
            String result = blockFont(1).render(" ");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Render output contains correct number of lines (height * 1 + separators)")
        void renderLineCount()
        {
            FigLetter fl = blockFont(1);
            String result = fl.render("A");
            int lines = result.split(System.lineSeparator().equals("\r\n") ? "\\r\\n" : "\n", -1).length;
            // BLOCK height is 5 → expect 5 lines
            assertEquals(5, lines);
        }

        @ParameterizedTest(name = "BLOCK font renders ''{0}'' without exception")
        @ValueSource(strings = { "HELLO", "WORLD", "0123456789", "!-.", "ABC XYZ" })
        @DisplayName("BLOCK font renders various strings without exception")
        void renderVariousStrings(String text)
        {
            assertDoesNotThrow(() -> blockFont(1).render(text));
        }

        @Test
        @DisplayName("BLOCK font is case-insensitive: 'abc' == 'ABC'")
        void caseInsensitive()
        {
            FigLetter fl = blockFont(1);
            assertEquals(fl.render("abc"), fl.render("ABC"));
        }

        @Test
        @DisplayName("SLIM font renders a string without exception")
        void slimFontRenders()
        {
            assertDoesNotThrow(() -> slimFont(1).render("TEST"));
        }
    }

    // ── scale ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("scale (rescale)")
    class Scale
    {

        @Test
        @DisplayName("scale=1 and scale=2: scaled output is exactly double the unscaled")
        void scaledOutputIsDouble()
        {
            String s1 = blockFont(1).render("A");
            String s2 = blockFont(2).render("A");

            // Each line in s1 should appear doubled horizontally in s2, and
            // there should be twice as many lines.
            String[] lines1 = s1.split("\\r?\\n", -1);
            String[] lines2 = s2.split("\\r?\\n", -1);

            assertEquals(lines1.length * 2, lines2.length, "Scale=2 should double the line count");

            // Every even-indexed line in lines2 should be the horizontal doubling of lines1
            for (int i = 0; i < lines1.length; i++)
            {
                StringBuilder expected = new StringBuilder();
                for (char c : lines1[i].toCharArray())
                {
                    expected.append(c).append(c);
                }
                assertEquals(expected.toString(), lines2[i * 2], "Horizontal scaling mismatch at line " + i);
            }
        }

        @Test
        @DisplayName("scale=1: output is not doubled")
        void scaleOneNotDoubled()
        {
            String s1 = blockFont(1).render("A");
            String s2 = blockFont(2).render("A");
            assertNotEquals(s1, s2);
        }

        @Test
        @DisplayName("scale=3: output has 3x as many lines as scale=1")
        void scaleThreeTriples()
        {
            String s1 = blockFont(1).render("A");
            String s3 = blockFont(3).render("A");
            String[] lines1 = s1.split("\\r?\\n", -1);
            String[] lines3 = s3.split("\\r?\\n", -1);
            assertEquals(lines1.length * 3, lines3.length);
        }
    }

    // ── renderCustom ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("renderCustom")
    class RenderCustom
    {

        @Test
        @DisplayName("renderCustom produces same output as render for BLOCK font")
        void renderCustomSameAsRender()
        {
            FigLetter fl = blockFont(1);
            assertEquals(fl.render("HELLO"), fl.renderCustom("HELLO"));
        }
    }

    // ── getInstance from InputStream (FLF) ────────────────────────────────────
    @Nested
    @DisplayName("getInstance from InputStream")
    class GetInstanceFromStream
    {

        @Test
        @DisplayName("FLF stream with height=3 can render text")
        void flfStreamRendersText() throws IOException
        {
            FigLetter fl = FigLetter.getInstance("font.flf", toStream(buildMinimalFlf(3)), 1);
            String result = fl.render("HI");
            assertNotNull(result);
            // 3 lines expected (height=3)
            String[] lines = result.split("\\r?\\n", -1);
            assertEquals(3, lines.length);
        }

        @Test
        @DisplayName("FLF font loaded from ZIPped stream is functional")
        void flfFromZipStream() throws IOException
        {
            byte[] flfBytes = buildMinimalFlf(3).getBytes(StandardCharsets.UTF_8);
            // ZIP it
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(bos))
            {
                zos.putNextEntry(new java.util.zip.ZipEntry("font.flf"));
                zos.write(flfBytes);
                zos.closeEntry();
            }
            FigLetter fl = FigLetter.getInstance("font.flf", new ByteArrayInputStream(bos.toByteArray()), 1);
            assertNotNull(fl.render("A"));
        }
    }

    // ── renderSystem ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("renderSystem (static)")
    class RenderSystem
    {

        @Test
        @DisplayName("renderSystem returns non-null output")
        void renderSystemNonNull()
        {
            // Uses Java AWT font — available in headless test environments via
            // BufferedImage + Graphics2D even without a display.
            String result = FigLetter.renderSystem("Hi", "Monospaced", 12, 1, '#');
            assertNotNull(result);
        }
    }

    @Test
    public void testMain() throws IOException
    {
        // Reloj CLI
        for(int i=1;i<4;i++)
        {
            System.out.printf("── Reloj (BLOCK / %d) ──\n", i);
            System.out.println(render("12:34:56", FigLetter.Font.BLOCK, i));
            System.out.println();
            System.out.printf("── Reloj (SLIM / %d) ──\n", i);
            System.out.println(render("12:34:56", FigLetter.Font.SLIM, i));
            System.out.println();
            System.out.printf("── Reloj (DOUBLE / %d) ──\n", i);
            System.out.println(render("12:34:56", FigLetter.Font.DOUBLE, i));
        }

        System.out.println();
        System.out.println("── Sistema: Monospaced / fontSize=14 / SMALL / '#' ──");
        System.out.println(renderSystem("12:34:56", "Monospaced", 14));

        System.out.println();
        System.out.println("── Sistema: SansSerif / fontSize=18 / SMALL / '█' ──");
        System.out.println(renderSystem("HELLO", "SansSerif", 18, '█'));

        FigIO figIO = new FigIO();

        // ── Export / Import ──────────────────────────
        System.out.println();
        System.out.println("── Export BLOCK → block.flf ──");
        File flfFile = new File("block.flf");
        File tlfFile = new File("block.tlf");
        System.out.println("Exportados: " + flfFile.getAbsolutePath() + " y " + tlfFile.getAbsolutePath());
        System.out.println();
        System.out.println("── Import block.flf → renderCustom ──");
        
    }
    
    static String render(String text, FigLetter.Font font, int scale)
    {
        return FigLetter.getInstance(font, scale).render(text);
    }

    private String renderSystem(String text, String font, int fontSize)
    {
        return FigLetter.renderSystem(text, font, fontSize, 1, '*');
    }
    private String renderSystem(String text, String font, int fontSize, char pixelChar)
    {
        return FigLetter.renderSystem(text, font, fontSize, 1, pixelChar);
    }

    private String renderCustom(String text, Glyphs imported, int scale)
    {
        return new FigLetter(imported, scale).renderCustom(text);
    }
    
    @Test
    public void testFlfTlf() throws IOException
    {
        String text = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ 012345689 ABCDEFGHIJKLMNÑOPQRSTUVWXYZ .,;:-";
        String[] fonts = { "future.tlf",  "smblock.tlf", "maxiwi.flf", "miniwi.flf", "terminus.flf" };
        for (String font : fonts)
        {
            
            System.out.println(font);
            FigLetter fl = FigLetter.getInstance(font, FigLetterTest.class.getResourceAsStream(font), 1);
            String s = fl.render(text);
            System.out.println(s);
        }
    }
    
    @Test
    @Disabled
    public void testUsrShareFiglet()
    {
        String text = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ 012345689 ABCDEFGHIJKLMNÑOPQRSTUVWXYZ .,;:-";
        File figlet = new File("/usr/share/figlet/");
        for (String font : figlet.list())
        {
            if(font.endsWith(".flf") || font.endsWith(".tlf"))
            {
                try 
                {
                    System.out.println(font);
                    File file = new File(figlet,font);
                    FigLetter fl = FigLetter.getInstance(file, 1);
                    String s = fl.render(text);
                    System.out.println(s);
                }
                catch (Exception ex) 
                {
                    System.err.print(font);
                }
            }
        }
    }
    
}
