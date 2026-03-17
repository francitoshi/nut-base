/*
 * Glyphs.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author franci
 */
public class Glyphs
{
    static final String EMBEDDED_FONT = "embedded font";
    static final int FLF_FIRST = 32;
    static final int FLF_LAST = 126;
    /**
     * 7 caracteres alemanes requeridos al final del bloque básico.
     */
    static final int[] FLF_GERMAN =
    {
        196, 214, 220, 228, 246, 252, 223
    };

    public static Glyphs getInstance(FigLetter.Font font)
    {
        switch (font)
        {
            case SLIM:
                return new SlimGlyphs();
            case DOUBLE:
                return new DoubleGlyphs();
            default:
                return new BlockGlyphs();
        }
    }

    public final String name;
    public final boolean caseSensitive;
    public final char hardblank;
    public final int height;
    public final int baseline;
    public final String comment;
    final Map<Character, String[]> glyphs;

    public Glyphs(String name, char hardblank, int height, int baseline, String comment, boolean caseSensitive, Map<Character, String[]> glyphs)
    {
        this.name = name;
        this.hardblank = hardblank;
        this.height = height;
        this.baseline = baseline;
        this.comment = comment;
        this.caseSensitive = caseSensitive;
        this.glyphs = glyphs;
    }

    public String[] get(char c)
    {
        return getOrDefault(c, null);
    }

    public String[] getOrDefault(char c, String[] defaultValue)
    {
        if (caseSensitive)
        {
            return glyphs.getOrDefault(Character.toUpperCase(c), defaultValue);
        }
        String[] value = glyphs.getOrDefault(c, null);
        if (value == null)
        {
            char u = Character.toUpperCase(c);
            return glyphs.getOrDefault(Character.toUpperCase(c), defaultValue);
        }
        return value;
    }

    public Iterable<String[]> values()
    {
        return glyphs.values();
    }

    boolean containsKey(char c)
    {
        return glyphs.containsKey(c);
    }

    private static void writeEmptyGlyph(PrintWriter pw, int height)
    {
        for (int i = 0; i < height; i++)
        {
            pw.println((i == height - 1) ? "@@" : "@");
        }
    }

    /**
     * Escribe un glifo en formato FLF: cada línea termina en '@', la última en
     * '@@'.
     */
    private void writeGlyph(PrintWriter pw, char c, int height)
    {
        String[] g = glyphs.containsKey(c) ? glyphs.get(c) : emptyGlyph(height);
        for (int i = 0; i < g.length; i++)
        {
            String endmark = (i == g.length - 1) ? "@@" : "@";
            pw.println(g[i] + endmark);
        }
    }

    /**
     * Escribe un glifo vacío (espacio) del alto indicado.
     */
    private static String[] emptyGlyph(int height)
    {
        String[] g = new String[height];
        Arrays.fill(g, "");
        return g;
    }

    private static List<String> readFontLines(File file) throws IOException
    {
        byte[] raw;
        try (InputStream is = new FileInputStream(file))
        {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int n;
            while ((n = is.read(tmp)) != -1)
            {
                buf.write(tmp, 0, n);
            }
            raw = buf.toByteArray();
        }

        // Detectar ZIP: magic bytes PK (0x50 0x4B)
        byte[] content = raw;
        if (raw.length > 2 && raw[0] == 0x50 && raw[1] == 0x4B)
        {
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(raw)))
            {
                ZipEntry entry = zis.getNextEntry();
                if (entry != null)
                {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    byte[] tmp = new byte[4096];
                    int n;
                    while ((n = zis.read(tmp)) != -1)
                    {
                        buf.write(tmp, 0, n);
                    }
                    content = buf.toByteArray();
                }
            }
        }

        List<String> lines = new ArrayList<>();
        for (String l : new String(content, java.nio.charset.StandardCharsets.UTF_8).split("\\r?\\n", -1))
        {
            lines.add(l);
        }
        return lines;
    }

    private void exportFontFile(File file, boolean tlf) throws IOException
    {
        String[] sample = glyphs.get('?');
        int height = sample.length;
        int baseline = height;          // baseline = última fila (sin descenders)
        char hardblank = '$';           // TLF lo ignora, pero lo incluimos igual

        // Calcular maxLen = ancho máximo de glifo + 2 (para los endmarks)
        int maxLen = 0;
        for (String[] g : glyphs.values())
        {
            for (String row : g)
            {
                maxLen = Math.max(maxLen, row.length());
            }
        }
        maxLen += 2; // endmarks

        // oldLayout = -1 → full width (sin smushing), lo más seguro para fuentes custom
        int oldLayout = -1;
        int numComments = 1;

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)))
        {
            pw.printf("%s2a%c %d %d %d %d %d 0 0 0%n", (tlf ? "tlf" : "flf"), hardblank, height, baseline, maxLen, oldLayout, numComments);
            pw.printf("Exported by Glyphs.java — font: %s%n", this.name);

            // Glifos ASCII 32..126
            for (int code = FLF_FIRST; code <= FLF_LAST; code++)
            {
                writeGlyph(pw, (char) code, height);
            }

            // 7 caracteres alemanes (vacíos, pero requeridos por el estándar)
            for (int code : FLF_GERMAN)
            {
                writeEmptyGlyph(pw, height);
            }
        }
    }
}

class SlimGlyphs extends Glyphs
{

    // ── SLIM (5 filas, 4 columnas, carácter '/') ─
    private static final Map<Character, String[]> GLYPHS = new LinkedHashMap<Character, String[]>()
    {
        {
            put('A', new String[]
            {
                " /\\ ",
                "/  \\",
                "/--\\",
                "/  \\",
                "/  \\"
            });
            put('B', new String[]
            {
                "|-- ",
                "|  \\",
                "|-- ",
                "|  \\",
                "|-- "
            });
            put('C', new String[]
            {
                " /--",
                "/   ",
                "/   ",
                "\\   ",
                " \\--"
            });
            put('D', new String[]
            {
                "|\\  ",
                "| \\ ",
                "|  |",
                "| / ",
                "|/  "
            });
            put('E', new String[]
            {
                "|---",
                "|   ",
                "|-- ",
                "|   ",
                "|---"
            });
            put('F', new String[]
            {
                "|---",
                "|   ",
                "|-- ",
                "|   ",
                "|   "
            });
            put('G', new String[]
            {
                " /--",
                "/   ",
                "/ --|",
                "\\  |",
                " \\-/"
            });
            put('H', new String[]
            {
                "|  |",
                "|  |",
                "|--|",
                "|  |",
                "|  |"
            });
            put('I', new String[]
            {
                "-+- ",
                " |  ",
                " |  ",
                " |  ",
                "-+- "
            });
            put('J', new String[]
            {
                "  |-",
                "  | ",
                "  | ",
                "  | ",
                "\\-/ "
            });
            put('K', new String[]
            {
                "| / ",
                "|/  ",
                "|\\  ",
                "| \\ ",
                "|  \\"
            });
            put('L', new String[]
            {
                "|   ",
                "|   ",
                "|   ",
                "|   ",
                "|---"
            });
            put('M', new String[]
            {
                "|\\/ ",
                "| \\ ",
                "|  |",
                "|  |",
                "|  |"
            });
            put('N', new String[]
            {
                "|\\  ",
                "| \\ ",
                "|  |",
                "|  |",
                "|  |"
            });
            put('O', new String[]
            {
                " /\\ ",
                "/  \\",
                "|  |",
                "\\  /",
                " \\/ "
            });
            put('P', new String[]
            {
                "|-- ",
                "|  \\",
                "|-- ",
                "|   ",
                "|   "
            });
            put('Q', new String[]
            {
                " /\\ ",
                "/  \\",
                "| \\|",
                "\\  \\",
                " \\-\\"
            });
            put('R', new String[]
            {
                "|-- ",
                "|  \\",
                "|--/",
                "| \\ ",
                "|  \\"
            });
            put('S', new String[]
            {
                " /--",
                "/   ",
                " \\-\\",
                "   \\",
                "\\--/"
            });
            put('T', new String[]
            {
                "-+--",
                " |  ",
                " |  ",
                " |  ",
                " |  "
            });
            put('U', new String[]
            {
                "|  |",
                "|  |",
                "|  |",
                "\\  /",
                " \\/ "
            });
            put('V', new String[]
            {
                "\\  /",
                "\\  /",
                " \\/ ",
                " /\\ ",
                "/  \\"
            });
            put('W', new String[]
            {
                "|  |",
                "|  |",
                "| /|",
                "|/ |",
                "/  \\"
            });
            put('X', new String[]
            {
                "\\  /",
                " \\/ ",
                " /\\ ",
                "/  \\",
                "\\  /"
            });
            put('Y', new String[]
            {
                "\\  /",
                " \\/ ",
                "  | ",
                "  | ",
                "  | "
            });
            put('Z', new String[]
            {
                "----",
                "  / ",
                " /  ",
                "/   ",
                "----"
            });
            put('0', new String[]
            {
                " /\\ ",
                "/ /\\",
                "| X |",
                "\\ \\/ ",
                " \\/ "
            });
            put('1', new String[]
            {
                " /| ",
                "  | ",
                "  | ",
                "  | ",
                " -+- "
            });
            put('2', new String[]
            {
                " /--",
                "/  /",
                " -- ",
                "/   ",
                "----"
            });
            put('3', new String[]
            {
                "----",
                "  /|",
                " -- ",
                "  \\|",
                "----"
            });
            put('4', new String[]
            {
                "| / ",
                "|/  ",
                "----",
                "  | ",
                "  | "
            });
            put('5', new String[]
            {
                "----",
                "|   ",
                "|---",
                "   |",
                "---/"
            });
            put('6', new String[]
            {
                " /--",
                "/   ",
                "|---",
                "|  |",
                " \\-/"
            });
            put('7', new String[]
            {
                "----",
                "   /",
                "  / ",
                " /  ",
                "/   "
            });
            put('8', new String[]
            {
                " /\\ ",
                "\\  /",
                " \\/ ",
                "/  \\",
                "\\  /"
            });
            put('9', new String[]
            {
                " /\\ ",
                "/  \\",
                " \\--|",
                "    |",
                " \\-/"
            });
            put(':', new String[]
            {
                "    ",
                " !! ",
                "    ",
                " !! ",
                "    "
            });
            put(' ', new String[]
            {
                "    ",
                "    ",
                "    ",
                "    ",
                "    "
            });
            put('?', new String[]
            {
                "????",
                "????",
                "????",
                "????",
                "????"
            });
            put('-', new String[]
            {
                "    ",
                "    ",
                "----",
                "    ",
                "    "
            });
            put('.', new String[]
            {
                "    ",
                "    ",
                "    ",
                "    ",
                " .  "
            });
            put('!', new String[]
            {
                " |  ",
                " |  ",
                " |  ",
                "    ",
                " *  "
            });
        }
    };

    public SlimGlyphs()
    {
        super("Slim", '$', 5, 5, EMBEDDED_FONT, false, GLYPHS);
    }
}

class DoubleGlyphs extends Glyphs
{

    // ── DOUBLE (5 filas, 5 columnas, carácter '═') usando ASCII imprimible ─
    private static final Map<Character, String[]> GLYPHS = new LinkedHashMap<Character, String[]>()
    {
        {
            put('A', new String[]
            {
                " === ",
                "=   =",
                "=====",
                "=   =",
                "=   ="
            });
            put('B', new String[]
            {
                "==== ",
                "=   =",
                "==== ",
                "=   =",
                "==== "
            });
            put('C', new String[]
            {
                " ====",
                "=    ",
                "=    ",
                "=    ",
                " ===="
            });
            put('D', new String[]
            {
                "==== ",
                "=   =",
                "=   =",
                "=   =",
                "==== "
            });
            put('E', new String[]
            {
                "=====",
                "=    ",
                "==== ",
                "=    ",
                "====="
            });
            put('F', new String[]
            {
                "=====",
                "=    ",
                "==== ",
                "=    ",
                "=    "
            });
            put('G', new String[]
            {
                " ====",
                "=    ",
                "=  ==",
                "=   =",
                " ===="
            });
            put('H', new String[]
            {
                "=   =",
                "=   =",
                "=====",
                "=   =",
                "=   ="
            });
            put('I', new String[]
            {
                "=====",
                "  =  ",
                "  =  ",
                "  =  ",
                "====="
            });
            put('J', new String[]
            {
                "=====",
                "    =",
                "    =",
                "=   =",
                " === "
            });
            put('K', new String[]
            {
                "=   =",
                "=  = ",
                "===  ",
                "=  = ",
                "=   ="
            });
            put('L', new String[]
            {
                "=    ",
                "=    ",
                "=    ",
                "=    ",
                "====="
            });
            put('M', new String[]
            {
                "=   =",
                "== ==",
                "= = =",
                "=   =",
                "=   ="
            });
            put('N', new String[]
            {
                "=   =",
                "==  =",
                "= = =",
                "=  ==",
                "=   ="
            });
            put('O', new String[]
            {
                " === ",
                "=   =",
                "=   =",
                "=   =",
                " === "
            });
            put('P', new String[]
            {
                "==== ",
                "=   =",
                "==== ",
                "=    ",
                "=    "
            });
            put('Q', new String[]
            {
                " === ",
                "=   =",
                "= = =",
                "=  = ",
                " == ="
            });
            put('R', new String[]
            {
                "==== ",
                "=   =",
                "==== ",
                "=  = ",
                "=   ="
            });
            put('S', new String[]
            {
                " ====",
                "=    ",
                " === ",
                "    =",
                "==== "
            });
            put('T', new String[]
            {
                "=====",
                "  =  ",
                "  =  ",
                "  =  ",
                "  =  "
            });
            put('U', new String[]
            {
                "=   =",
                "=   =",
                "=   =",
                "=   =",
                " === "
            });
            put('V', new String[]
            {
                "=   =",
                "=   =",
                "=   =",
                " = = ",
                "  =  "
            });
            put('W', new String[]
            {
                "=   =",
                "=   =",
                "= = =",
                "== ==",
                "=   ="
            });
            put('X', new String[]
            {
                "=   =",
                " = = ",
                "  =  ",
                " = = ",
                "=   ="
            });
            put('Y', new String[]
            {
                "=   =",
                " = = ",
                "  =  ",
                "  =  ",
                "  =  "
            });
            put('Z', new String[]
            {
                "=====",
                "   = ",
                "  =  ",
                " =   ",
                "====="
            });
            put('0', new String[]
            {
                " === ",
                "=  ==",
                "= = =",
                "==  =",
                " === "
            });
            put('1', new String[]
            {
                "  =  ",
                " ==  ",
                "  =  ",
                "  =  ",
                "====="
            });
            put('2', new String[]
            {
                " === ",
                "=   =",
                "  == ",
                " =   ",
                "====="
            });
            put('3', new String[]
            {
                "=====",
                "   = ",
                " === ",
                "    =",
                "====="
            });
            put('4', new String[]
            {
                "=   =",
                "=   =",
                "=====",
                "    =",
                "    ="
            });
            put('5', new String[]
            {
                "=====",
                "=    ",
                "==== ",
                "    =",
                "==== "
            });
            put('6', new String[]
            {
                " === ",
                "=    ",
                "==== ",
                "=   =",
                " === "
            });
            put('7', new String[]
            {
                "=====",
                "    =",
                "   = ",
                "  =  ",
                "  =  "
            });
            put('8', new String[]
            {
                " === ",
                "=   =",
                " === ",
                "=   =",
                " === "
            });
            put('9', new String[]
            {
                " === ",
                "=   =",
                " ====",
                "    =",
                " === "
            });
            put(':', new String[]
            {
                "     ",
                "  =  ",
                "     ",
                "  =  ",
                "     "
            });
            put(' ', new String[]
            {
                "     ",
                "     ",
                "     ",
                "     ",
                "     "
            });
            put('?', new String[]
            {
                "?????",
                "?????",
                "?????",
                "?????",
                "?????"
            });
            put('-', new String[]
            {
                "     ",
                "     ",
                "=====",
                "     ",
                "     "
            });
            put('.', new String[]
            {
                "     ",
                "     ",
                "     ",
                "     ",
                "  =  "
            });
            put('!', new String[]
            {
                "  =  ",
                "  =  ",
                "  =  ",
                "     ",
                "  =  "
            });
        }
    };

    public DoubleGlyphs()
    {
        super("Double", '$', 5, 5, "embedded font", false, GLYPHS);
    }
}

class BlockGlyphs extends Glyphs
{

    // ── BLOCK (5 filas, 5 columnas) ──────────────
    private static final Map<Character, String[]> GLYPHS = new LinkedHashMap<Character, String[]>()
    {
        {
            put('A', new String[]
            {
                " ### ",
                "#   #",
                "#####",
                "#   #",
                "#   #"
            });
            put('B', new String[]
            {
                "#### ",
                "#   #",
                "#### ",
                "#   #",
                "#### "
            });
            put('C', new String[]
            {
                " ####",
                "#    ",
                "#    ",
                "#    ",
                " ####"
            });
            put('D', new String[]
            {
                "#### ",
                "#   #",
                "#   #",
                "#   #",
                "#### "
            });
            put('E', new String[]
            {
                "#####",
                "#    ",
                "#### ",
                "#    ",
                "#####"
            });
            put('F', new String[]
            {
                "#####",
                "#    ",
                "#### ",
                "#    ",
                "#    "
            });
            put('G', new String[]
            {
                " ####",
                "#    ",
                "#  ##",
                "#   #",
                " ####"
            });
            put('H', new String[]
            {
                "#   #",
                "#   #",
                "#####",
                "#   #",
                "#   #"
            });
            put('I', new String[]
            {
                "#####",
                "  #  ",
                "  #  ",
                "  #  ",
                "#####"
            });
            put('J', new String[]
            {
                "#####",
                "    #",
                "    #",
                "#   #",
                " ### "
            });
            put('K', new String[]
            {
                "#   #",
                "#  # ",
                "###  ",
                "#  # ",
                "#   #"
            });
            put('L', new String[]
            {
                "#    ",
                "#    ",
                "#    ",
                "#    ",
                "#####"
            });
            put('M', new String[]
            {
                "#   #",
                "## ##",
                "# # #",
                "#   #",
                "#   #"
            });
            put('N', new String[]
            {
                "#   #",
                "##  #",
                "# # #",
                "#  ##",
                "#   #"
            });
            put('O', new String[]
            {
                " ### ",
                "#   #",
                "#   #",
                "#   #",
                " ### "
            });
            put('P', new String[]
            {
                "#### ",
                "#   #",
                "#### ",
                "#    ",
                "#    "
            });
            put('Q', new String[]
            {
                " ### ",
                "#   #",
                "# # #",
                "#  # ",
                " ## #"
            });
            put('R', new String[]
            {
                "#### ",
                "#   #",
                "#### ",
                "#  # ",
                "#   #"
            });
            put('S', new String[]
            {
                " ####",
                "#    ",
                " ### ",
                "    #",
                "#### "
            });
            put('T', new String[]
            {
                "#####",
                "  #  ",
                "  #  ",
                "  #  ",
                "  #  "
            });
            put('U', new String[]
            {
                "#   #",
                "#   #",
                "#   #",
                "#   #",
                " ### "
            });
            put('V', new String[]
            {
                "#   #",
                "#   #",
                "#   #",
                " # # ",
                "  #  "
            });
            put('W', new String[]
            {
                "#   #",
                "#   #",
                "# # #",
                "## ##",
                "#   #"
            });
            put('X', new String[]
            {
                "#   #",
                " # # ",
                "  #  ",
                " # # ",
                "#   #"
            });
            put('Y', new String[]
            {
                "#   #",
                " # # ",
                "  #  ",
                "  #  ",
                "  #  "
            });
            put('Z', new String[]
            {
                "#####",
                "   # ",
                "  #  ",
                " #   ",
                "#####"
            });
            put('0', new String[]
            {
                " ### ",
                "#  ##",
                "# # #",
                "##  #",
                " ### "
            });
            put('1', new String[]
            {
                "  #  ",
                " ##  ",
                "  #  ",
                "  #  ",
                "#####"
            });
            put('2', new String[]
            {
                " ### ",
                "#   #",
                "  ## ",
                " #   ",
                "#####"
            });
            put('3', new String[]
            {
                "#####",
                "   # ",
                " ### ",
                "    #",
                "#####"
            });
            put('4', new String[]
            {
                "#   #",
                "#   #",
                "#####",
                "    #",
                "    #"
            });
            put('5', new String[]
            {
                "#####",
                "#    ",
                "#### ",
                "    #",
                "#### "
            });
            put('6', new String[]
            {
                " ### ",
                "#    ",
                "#### ",
                "#   #",
                " ### "
            });
            put('7', new String[]
            {
                "#####",
                "    #",
                "   # ",
                "  #  ",
                "  #  "
            });
            put('8', new String[]
            {
                " ### ",
                "#   #",
                " ### ",
                "#   #",
                " ### "
            });
            put('9', new String[]
            {
                " ### ",
                "#   #",
                " ####",
                "    #",
                " ### "
            });
            put(':', new String[]
            {
                "     ",
                "  #  ",
                "     ",
                "  #  ",
                "     "
            });
            put(' ', new String[]
            {
                "     ",
                "     ",
                "     ",
                "     ",
                "     "
            });
            put('?', new String[]
            {
                "?????",
                "?????",
                "?????",
                "?????",
                "?????"
            });
            put('-', new String[]
            {
                "     ",
                "     ",
                "#####",
                "     ",
                "     "
            });
            put('.', new String[]
            {
                "     ",
                "     ",
                "     ",
                "     ",
                "  #  "
            });
            put('!', new String[]
            {
                "  #  ",
                "  #  ",
                "  #  ",
                "     ",
                "  #  "
            });
        }
    };

    public BlockGlyphs()
    {
        super("Block", '$', 5, 5, "embedded font", false, GLYPHS);
    }
}
