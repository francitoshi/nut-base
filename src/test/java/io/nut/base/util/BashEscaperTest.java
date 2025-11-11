/*
 *  BashEscaperTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.util;

import static io.nut.base.util.BashEscaper.buildCommandLine;
import static io.nut.base.util.BashEscaper.escapeOrQuote;
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
