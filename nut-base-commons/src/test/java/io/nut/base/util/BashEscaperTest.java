/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import static io.nut.base.util.BashEscaper.buildCommandLine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BashEscaperTest
{
    @Test
    public void testEscapeOrQuote()
    {
        assertEquals("simple", BashEscaper.escapeOrQuote("simple"));
        assertEquals("'with spaces'", BashEscaper.escapeOrQuote("with spaces"));
        assertEquals("\"with'simple-quote\"", BashEscaper.escapeOrQuote("with'simple-quote"));
        assertEquals("'with\"double-quote'", BashEscaper.escapeOrQuote("with\"double-quote"));
        assertEquals("'with$variable'", BashEscaper.escapeOrQuote("with$variable"));
        assertEquals("'mix'\\''ed\"quotes'", BashEscaper.escapeOrQuote("mix'ed\"quotes"));
        assertEquals("/path/to/file.txt", BashEscaper.escapeOrQuote("/path/to/file.txt"));
        assertEquals("user=admin", BashEscaper.escapeOrQuote("user=admin"));
        assertEquals("''", BashEscaper.escapeOrQuote(""));
        assertEquals("'with`backtick'", BashEscaper.escapeOrQuote("with`backtick"));
        assertEquals("'with\\backslash'", BashEscaper.escapeOrQuote("with\\backslash"));
        assertEquals("\"it's a test\"", BashEscaper.escapeOrQuote("it's a test"));
        assertEquals("'already \"quoted\"'", BashEscaper.escapeOrQuote("already \"quoted\""));
        assertEquals("normal-file_2024.txt", BashEscaper.escapeOrQuote("normal-file_2024.txt"));
    }

    @Test
    public void testBuildCommandLine()
    {
        
        String cmd = buildCommandLine("grep", "-r", "search term", "/path/to/dir", "--exclude=*.tmp");
        
        assertEquals("grep -r 'search term' /path/to/dir '--exclude=*.tmp'", cmd);
    }
    
}
