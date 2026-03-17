/*
 * Tlf2aIO.java
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
 * Implementación de {@link FigIO} para el formato TOIlet (.tlf).
 *
 * <h3>Diferencias con FLF</h3>
 * <ul>
 * <li>Firma: {@code tlf2a<hardblank>} — el char pegado a la firma es el
 * <b>hardblank</b> (espacio protegido), típicamente {@code \x7f} (DEL).</li>
 * <li>Endmark: <b>variable por glifo</b>. La regla es:
 * <ol>
 * <li>El endmark de cada glifo es el último carácter de su última fila (la que
 * aparece repetido dos veces al final).</li>
 * <li>Por convención, el diseñador usa el propio código ASCII del glifo como
 * endmark cuando es posible (es decir, cuando ese carácter no aparece en el
 * contenido visual del glifo).</li>
 * <li>Cuando hay colisión (el carácter sí aparece en el contenido) o para el
 * glifo 32 (espacio), se usa {@code '@'} como fallback.</li>
 * </ol>
 * </li>
 * <li>Soporte Unicode nativo: los sub-caracteres pueden ser cualquier codepoint
 * UTF-8 (╔═║╗╚╝░▒▓█ etc.).</li>
 * <li>Compresión opcional: algunos ficheros .tlf están empaquetados como ZIP
 * (magic bytes {@code PK}). Se detectan y descomprimen automáticamente.</li>
 * </ul>
 */
public class Tlf2aIO extends FigIO
{

    /**
     * Endmark de fallback cuando el carácter propio no puede usarse (espacio, o
     * colisión con contenido visual).
     */
    private static final char FALLBACK_ENDMARK = '@';

    // ── load ─────────────────────────────────────────────────────────────────
    public Glyphs load(String name, List<String> lines) throws IOException
    {
        String header = lines.get(0);
        if (!header.startsWith("tlf2a"))
        {
            throw new IllegalArgumentException("Not a valid TLF file: " + header.substring(0, Math.min(8, header.length())));
        }

        // tlf2a<hardblank> height baseline maxLen oldLayout numComments [printDir fullLayout codeTagCount]
        String[] parts = header.split("\\s+");
        int height, baseline, numComments, codeTagCount;
        try
        {
            height = Integer.parseInt(parts[1]);
            baseline = Integer.parseInt(parts[2]);
            numComments = Integer.parseInt(parts[5]);
            codeTagCount = parts.length > 8 ? Integer.parseInt(parts[8]) : 0;
        }
        catch (NumberFormatException | ArrayIndexOutOfBoundsException e)
        {
            throw new IllegalArgumentException("Cabecera TLF malformada: " + header, e);
        }

        int dataStart = 1 + numComments;
        if (dataStart >= lines.size())
        {
            throw new IllegalArgumentException("Sin datos de glifos tras los comentarios");
        }

        Map<Character, String[]> glyphs = new LinkedHashMap<>();
        int idx = dataStart;

        // Bloque ASCII 32-126: el endmark se determina glifo a glifo
        for (int code = FIRST_CODE; code <= LAST_CODE && idx < lines.size(); code++)
        {
            char endmark = detectEndmark(lines, idx, height, (char) code);
            glyphs.put((char) code, readGlyph(lines, idx, height, endmark));
            idx += height;
        }

        // 7 alemanes
        for (int code : GERMAN_CODES)
        {
            if (idx + height > lines.size())
            {
                break;
            }
            char endmark = detectEndmark(lines, idx, height, (char) code);
            glyphs.put((char) code, readGlyph(lines, idx, height, endmark));
            idx += height;
        }

        //uppercase and lowercase detectors
        boolean upper = false;
        boolean lower = false;
        
        // Glifos etiquetados
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
                char endmark = detectEndmark(lines, idx, height, (char) code);
                if (code >= 0 && code <= 0xFFFF)
                {
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
        
        return new Glyphs(name, (char)0x7F, height, baseline, "TLF loaded font", caseSensitive, glyphs);
    }

    // ── export ────────────────────────────────────────────────────────────────
    /**
     * Exporta al formato TLF.
     *
     * <p>
     * El endmark de cada glifo se elige automáticamente siguiendo la convención
     * TLF: se usa el propio código ASCII del glifo si no aparece en su
     * contenido visual; en caso contrario se usa {@code '@'}.
     */

    public void export(Glyphs glyphs, String comment, OutputStream os) throws IOException
    {
        int maxLen = computeMaxLen(glyphs);

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

        // Cabecera: tlf2a<hardblank> height baseline maxLen oldLayout numComments printDir fullLayout codeTagCount
        pw.printf("tlf2a%c %d %d %d -1 %d 0 0 %d%n", glyphs.hardblank, glyphs.height, glyphs.baseline, maxLen, numComments, tagged.size());

        for (String cl : commentLines)
        {
            pw.println(cl);
        }

        // Bloque ASCII 32-126
        for (int code = FIRST_CODE; code <= LAST_CODE; code++)
        {
            String[] glyph = glyphs.getOrDefault((char) code, emptyGlyph(glyphs.height));
            char endmark = chooseEndmark(glyph, (char) code);
            writeGlyph(pw, glyph, endmark);
        }

        // 7 alemanes
        for (int code : GERMAN_CODES)
        {
            String[] glyph = glyphs.getOrDefault((char) code, emptyGlyph(glyphs.height));
            char endmark = chooseEndmark(glyph, (char) code);
            writeGlyph(pw, glyph, endmark);
        }

        // Etiquetados
        for (Map.Entry<Character, String[]> e : tagged.entrySet())
        {
            pw.println((int) e.getKey());
            char endmark = chooseEndmark(e.getValue(), e.getKey());
            writeGlyph(pw, e.getValue(), endmark);
        }

        pw.flush();
    }

    // ── helpers privados ──────────────────────────────────────────────────────
    /**
     * Detecta el endmark de un glifo leyendo el último carácter de su última
     * fila (donde aparece duplicado).
     *
     * <p>
     * Si la última fila está vacía o tiene menos de 2 caracteres, devuelve
     * {@link #FALLBACK_ENDMARK}.
     */
    private char detectEndmark(List<String> lines, int from, int height, char glyphCode)
    {
        int lastLine = from + height - 1;
        if (lastLine >= lines.size())
        {
            return FALLBACK_ENDMARK;
        }
        // Ignorar trailing whitespace: el endmark puede ir seguido de espacios
        String last = lines.get(lastLine).replaceAll("\\s+$", "");
        if (last.length() < 2)
        {
            return FALLBACK_ENDMARK;
        }
        char candidate = last.charAt(last.length() - 1);
        char prev = last.charAt(last.length() - 2);
        return (candidate == prev) ? candidate : FALLBACK_ENDMARK;
    }

    /**
     * Lee {@code height} líneas desde {@code from} y elimina el endmark
     * específico de este glifo del final de cada una.
     */
    private String[] readGlyph(List<String> lines, int from, int height, char endmark)
    {
        String escaped = Pattern.quote(String.valueOf(endmark));
        // Algunos ficheros TLF tienen trailing spaces DESPUÉS del endmark.
        // El patrón elimina primero el endmark (uno o más) y luego cualquier
        // espacio sobrante al final, o a la inversa si el endmark va después.
        // La forma más robusta: strip trailing whitespace, luego strip endmarks.
        String[] glyph = new String[height];
        for (int i = 0; i < height; i++)
        {
            String raw = (from + i < lines.size()) ? lines.get(from + i) : "";
            // 1. Eliminar trailing whitespace
            raw = raw.replaceAll("\\s+$", "");
            // 2. Eliminar endmark(s) al final
            raw = raw.replaceAll(escaped + "+$", "");
            glyph[i] = raw;
        }
        return glyph;
    }

    /**
     * Elige el endmark para la exportación de un glifo.
     *
     * <p>
     * Usa el propio código ASCII del glifo si éste no aparece en ninguna fila
     * del contenido visual. En caso contrario usa {@link #FALLBACK_ENDMARK}. El
     * glifo 32 (espacio) siempre usa el fallback porque su contenido
     * <em>es</em> espacio en blanco, indistinguible visualmente.
     */
    private char chooseEndmark(String[] glyph, char code)
    {
        if (code == ' ' || code == FALLBACK_ENDMARK)
        {
            return FALLBACK_ENDMARK;
        }
        String codeStr = String.valueOf(code);
        for (String row : glyph)
        {
            if (row != null && row.contains(codeStr))
            {
                return FALLBACK_ENDMARK;
            }
        }
        return code;
    }

    private void writeGlyph(PrintWriter pw, String[] glyph, char endmark)
    {
        for (int i = 0; i < glyph.length; i++)
        {
            String row = glyph[i] != null ? glyph[i] : "";
            pw.println(i == glyph.length - 1
                    ? row + endmark + endmark
                    : row + endmark);
        }
    }

    private String[] emptyGlyph(int height)
    {
        String[] g = new String[height];
        Arrays.fill(g, "");
        return g;
    }

    private String[] buildCommentLines(String comment)
    {
        if (comment == null || comment.trim().isEmpty())
        {
            return new String[]
            {
                "Exported by Tlf2aIO"
            };
        }
        return comment.split("\\r?\\n", -1);
    }    
}
