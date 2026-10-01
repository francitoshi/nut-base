/*
 * Flf2aIO.java
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Implementación de {@link FigIO} para el formato FIGlet (.flf).
 *
 * <h3>Reglas del formato FLF</h3>
 * <ul>
 * <li>Firma: {@code flf2a<hardblank>} — el hardblank está pegado a la
 * firma.</li>
 * <li>Endmark: <b>siempre {@code '@'}</b> para todos los glifos, sin
 * excepción.</li>
 * <li>Última fila de cada glifo: endmark doble {@code "@@"}.</li>
 * <li>Glifos en orden fijo: ASCII 32-126, luego 7 alemanes, luego glifos
 * etiquetados opcionales con su código decimal o hex.</li>
 * <li>Codificación: UTF-8 (aunque la especificación original solo define ASCII,
 * la práctica habitual es UTF-8).</li>
 * </ul>
 */
public class Flf2aIO extends FigIO
{

    /**
     * Endmark fijo del formato FLF.
     */
    private static final char ENDMARK = '@';
    private static final String ENDMARK_PATTERN = Pattern.quote("@") + "+$";

    // ── load ─────────────────────────────────────────────────────────────────
    public Glyphs load(String name, List<String> lines) throws IOException
    {
        String header = lines.get(0);
        if (!header.startsWith("flf2a"))
        {
            throw new IllegalArgumentException("Not a valid TLF file: " + header.substring(0, Math.min(8, header.length())));
        }

        // flf2a<hardblank> height baseline maxLen oldLayout numComments [printDir fullLayout codeTagCount]
        String[] parts = header.split("\\s+");
        char hardblank;
        int height, baseline, numComments, codeTagCount;
        try
        {
            hardblank = parts[0].charAt(5);
            height = Integer.parseInt(parts[1]);
            baseline = Integer.parseInt(parts[2]);
            numComments = Integer.parseInt(parts[5]);
            codeTagCount = parts.length > 8 ? Integer.parseInt(parts[8]) : 0;
        }
        catch (NumberFormatException | ArrayIndexOutOfBoundsException e)
        {
            throw new IllegalArgumentException("Cabecera FLF malformada: " + header, e);
        }

        int dataStart = 1 + numComments;
        if (dataStart >= lines.size())
        {
            throw new IllegalArgumentException("Sin datos de glifos tras los comentarios");
        }

        Map<Character, String[]> glyphs = new LinkedHashMap<>();
        int idx = dataStart;

        // Bloque ASCII 32-126
        for (int code = FIRST_CODE; code <= LAST_CODE && idx < lines.size(); code++)
        {
            char endmark = detectEndmark(lines, idx, height, (char) code); // detect instead of assuming '@'
            glyphs.put((char) code, readGlyph(lines, idx, height, endmark));
            idx += height;
        }

        // 7 glifos alemanes obligatorios
        for (int code : GERMAN_CODES)
        {
            if (idx + height > lines.size())
            {
                break;
            }
            char endmark = detectEndmark(lines, idx, height, (char) code); // detect instead of assuming '@'
            glyphs.put((char) code, readGlyph(lines, idx, height, endmark));
            idx += height;
        }
        
        //uppercase and lowercase detectors
        boolean upper = false;
        boolean lower = false;

        // Glifos etiquetados: "12345\n<glifo>" o "0x1F600\n<glifo>"
        int tagged = 0;
        while (idx < lines.size() && (codeTagCount == 0 || tagged < codeTagCount))
        {
            String tag = lines.get(idx).trim();
            if (tag.isEmpty())
            {
                idx++;
                continue;
            }
            try
            {
                int code = parseCode(tag.split("\\s+")[0]);
                idx++;
                if (idx + height > lines.size())
                {
                    break;
                }
                if (code >= 0 && code <= 0xFFFF)
                {
                    char endmark = detectEndmark(lines, idx, height, (char) code); // detect instead of assuming '@'
                    glyphs.put((char) code, readGlyph(lines, idx, height, endmark));
                    upper |= Character.isUpperCase(code);
                    lower |= Character.isLowerCase(code);
                }
                idx += height;
                tagged++;
            }
            catch (NumberFormatException e)
            {
                idx++;
            }
        }
        boolean caseSensitive = upper ^ lower;
       
        return new Glyphs(name, hardblank, height, baseline, "FLF loaded font", caseSensitive, glyphs);
    }

    // ── export ────────────────────────────────────────────────────────────────
    public void export(Glyphs glyphs, String comment, OutputStream os) throws IOException
    {
        // Calcular maxLen = ancho máximo + 2 (para "@@")
        int maxLen = computeMaxLen(glyphs);

        // Separar glifos etiquetados (código > 126 o < 32, excluidos los alemanes)
        Set<Integer> germanSet = new HashSet<>();
        for (int c : GERMAN_CODES)
        {
            germanSet.add(c);
        }

        Map<Character, String[]> tagged = new LinkedHashMap<>();
        for (Map.Entry<Character, String[]> e : glyphs.glyphs.entrySet())
        {
            int code = (int) e.getKey();
            if ((code < FIRST_CODE || code > LAST_CODE) && !germanSet.contains(code))
            {
                tagged.put(e.getKey(), e.getValue());
            }
        }

        String[] commentLines = buildCommentLines(comment);
        int numComments = commentLines.length;

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

        // Cabecera: flf2a<hardblank> height baseline maxLen oldLayout numComments printDir fullLayout codeTagCount
        pw.printf("flf2a%c %d %d %d -1 %d 0 0 %d%n", glyphs.hardblank, glyphs.height, glyphs.baseline, maxLen, numComments, tagged.size());

        for (String cl : commentLines)
        {
            pw.println(cl);
        }

        // Bloque ASCII 32-126
        for (int code = FIRST_CODE; code <= LAST_CODE; code++)
        {
            writeGlyph(pw, glyphs.getOrDefault((char) code, emptyGlyph(glyphs.height)), ENDMARK);
        }

        // 7 alemanes
        for (int code : GERMAN_CODES)
        {
            writeGlyph(pw, glyphs.getOrDefault((char) code, emptyGlyph(glyphs.height)), ENDMARK);
        }

        // Etiquetados
        for (Map.Entry<Character, String[]> e : tagged.entrySet())
        {
            pw.println((int) e.getKey());
            writeGlyph(pw, e.getValue(), ENDMARK);
        }

        pw.flush();
    }
}
