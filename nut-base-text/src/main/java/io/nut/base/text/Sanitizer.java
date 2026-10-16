/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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

    // Secuencias de escape: CSI (colores, \u001B[2J...), OSC (enlaces OSC-8, titulo...),
    // DCS/SOS/PM/APC y escapes de un solo caracter (ESC 7, ESC =, ESC >...).
    private static final Pattern ANSI_PATTERN = Pattern.compile(
            "\u001B(?:"
            + "\\[[0-?]*[ -/]*[@-~]"                        // CSI
            + "|\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)?"  // OSC
            + "|[PX^_][^\u001B]*(?:\u001B\\\\)?"            // DCS/SOS/PM/APC
            + "|[\u0020-\u007E]"                            // escapes de un solo caracter
            + ")");

    // Expresión regular para detectar etiquetas <script> y su contenido
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", Pattern.CASE_INSENSITIVE);

    // Expresión regular para detectar otras etiquetas HTML
    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]+>");

    // Caracteres permitidos: letras, números, puntuación, separadores, '\n'/'\t' y
    // los símbolos explícitamente seguros (<>+=). Todo lo demás se elimina: es decir
    // controles (C0/C1/DEL), formato (Cf, p. ej. override RTL U+202E o ZWSP U+200B),
    // símbolos y emoji (S), marcas (M), uso privado (Co) y no asignados (Cn).
    private static final Pattern UNSAFE_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\p{P}\\p{Z}\\n\\t<>+=]");

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
        if (s == null || s.isEmpty())
        {
            return s;
        }

        // Elimina secuencias de escape ANSI/CSI/OSC/etc.
        if(ansi)
        {
            s = ANSI_PATTERN.matcher(s).replaceAll(harmless);
        }
        if(html)
        {
            // Elimina <script> y su contenido
            s = SCRIPT_PATTERN.matcher(s).replaceAll(harmless);

            // Elimina el resto de etiquetas HTML
            s = HTML_PATTERN.matcher(s).replaceAll(harmless);
        }

        // Normaliza CR/CRLF a '\n' para que nada pueda sobrescribir la línea.
        s = s.replace("\r\n", "\n").replace('\r', '\n');

        // Conserva solo texto seguro (letras, números, puntuación, separadores,
        // '\n'/'\t' y los símbolos permitidos). Elimina controles, formato, símbolos,
        // emoji, marcas, uso privado y no asignados.
        s = UNSAFE_PATTERN.matcher(s).replaceAll("");

        // Limitar longitud
        if (s.length() > sizeLimit)
        {
            s = s.substring(0, sizeLimit);
        }

        return s;
    }
}
