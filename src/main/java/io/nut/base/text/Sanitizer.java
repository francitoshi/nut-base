/*
 * Sanitizer.java
 *
 * Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.text;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Sanitizer
{
    private final boolean ansi;
    private final boolean html;
    private final int sizeLimit;
    private final String harmless;

    public Sanitizer(boolean ansi, boolean html, int sizeLimit, String harmless)
    {
        this.ansi = ansi;
        this.html = html;
        this.sizeLimit = sizeLimit;
        this.harmless = harmless;
    }

    public Sanitizer(boolean ansi, boolean html, int sizeLimit)
    {
        this(ansi, html, sizeLimit, "");
    }
    public Sanitizer(boolean ansi, boolean html)
    {
        this(ansi, html, 1000, "");
    }
    
    // Expresión regular para detectar secuencias ANSI
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[\\d;]*[a-zA-Z]");

    // Expresión regular para detectar etiquetas <script> y su contenido
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", Pattern.CASE_INSENSITIVE);

    // Expresión regular para detectar otras etiquetas HTML
    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]+>");

    // Expresión regular para permitir solo caracteres seguros
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("[\\p{L}\\p{N}\\p{P}\\p{Z}\\n\\r\\t]*");

    public String sanitize(byte[] bytes)
    {
        if (bytes == null || bytes.length == 0)
        {
            return "";
        }

        try
        {
            return this.sanitize(new String(bytes, StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            return "";
        }
    }
    
    public String sanitize(String s)
    {
        // delete ANSI sequences
        if(ansi)
        {
            s = ANSI_PATTERN.matcher(s).replaceAll(harmless);
        }
        if(html)
        {
            // delete <script> mark and their content
            s = SCRIPT_PATTERN.matcher(s).replaceAll(harmless);

            // delete other HTML 
            s = HTML_PATTERN.matcher(s).replaceAll(harmless);
        }

        // Filtrar caracteres no imprimibles (excepto \n, \r, \t)
        s = s.replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", "");

        // Validar que el texto resultante solo contiene caracteres seguros
        if (!SAFE_TEXT_PATTERN.matcher(s).matches())
        {
            s = s.chars()
                    .filter(c -> isSafeCharacter((char) c))
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        }

        // Limitar longitud
        if (s.length() > sizeLimit)
        {
            s = s.substring(0, sizeLimit);
        }

        return s;
    }

    // Método auxiliar para determinar si un carácter es seguro
    private static boolean isSafeCharacter(char c)
    {
        return Character.isLetterOrDigit(c)
                || Character.isWhitespace(c)
                || isSafePunctuation(c)
                || isValidUnicode(c);
    }

    // Método para validar puntuación segura
    private static boolean isSafePunctuation(char c)
    {
        return ",.?!:;\"'()[]{}<>+-*/=".indexOf(c) >= 0;
    }

    // Método para validar caracteres Unicode
    private static boolean isValidUnicode(char c)
    {
        return Character.isDefined(c) && !Character.isISOControl(c);
    }
}
