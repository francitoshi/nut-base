/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class BashEscaper
{

    /**
     * Escapa o añade comillas a un argumento para que sea seguro en bash.
     * Devuelve el string sin modificar si no contiene caracteres especiales.
     */
    public static String escapeOrQuote(String s)
    {
        if (s == null || s.isEmpty())
        {
            return "''";
        }

        // Si no tiene caracteres especiales, devolver tal cual
        if (isSafe(s))
        {
            return s;
        }

        // Si no contiene comillas simples, usar comillas simples (más simple)
        if (!s.contains("'"))
        {
            return "'" + s + "'";
        }

        // Si no contiene comillas dobles ni $ ni `, usar comillas dobles
        if (!s.contains("\"") && !s.contains("$") && !s.contains("`") && !s.contains("\\"))
        {
            return "\"" + s + "\"";
        }

        // Caso complejo: tiene comillas simples y otros caracteres problemáticos
        // Usamos comillas simples y escapamos las comillas simples internas
        return "'" + s.replace("'", "'\\''") + "'";
    }

    /**
     * Verifica si el string es seguro sin necesidad de escapar o quotear.
     */
    private static boolean isSafe(String s)
    {
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (!isSafeChar(c))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica si un carácter individual es seguro en bash sin escapar.
     */
    private static boolean isSafeChar(char c)
    {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_' || c == '-' || c == '.' || c == '/' || c == '=' || c == '+';
    }

    public static String buildCommandLine(List<String> args)
    {
        if (args == null || args.isEmpty())
        {
            return "";
        }

        StringJoiner sj = new StringJoiner(" ");
        for(String item : args)
        {
            sj.add(escapeOrQuote(item));
        }
        return sj.toString();
    }

    public static String buildCommandLine(String... args)
    {
        return buildCommandLine(Arrays.asList(args));
    }
}
